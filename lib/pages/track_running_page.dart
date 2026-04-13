import 'dart:async';
import 'package:flutter/material.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../models/track.dart';
import '../models/track_record.dart';
import '../models/track_point.dart';
import '../database/database_helper.dart';
import '../services/location_service.dart';
import '../utils/location_utils.dart';
import '../services/running_notification_service.dart';

class TrackRunningPage extends StatefulWidget {
  final Track track;

  const TrackRunningPage({super.key, required this.track});

  @override
  State<TrackRunningPage> createState() => _TrackRunningPageState();
}

class _TrackRunningPageState extends State<TrackRunningPage> {
  final LocationService _locationService = LocationService();
  final DatabaseHelper _db = DatabaseHelper.instance;
  final RunningNotificationService _notificationService =
      RunningNotificationService();

  bool _isStarted = false;
  bool _isTiming = false;
  bool _isInStartArea = false; // 是否已进入起点区域
  bool _isStopping = false; // 是否正在停止过程中（用于UI提示）
  double _elapsedTime = 0;
  Timer? _timer;
  StreamSubscription<LocationData>? _locationSubscription;

  int? _recordId;
  int _pointSequence = 0;
  double? _currentLatitude;
  double? _currentLongitude;
  double? _distanceToStart;
  double? _distanceToEnd;
  double? _currentSpeed;
  double _totalDistance = 0; // 累计距离（米）

  // 批量存储相关
  final List<TrackPoint> _pendingPoints = []; // 待存储的轨迹点缓存
  static const int _batchSize = 10; // 每10个点批量存储一次

  @override
  void initState() {
    super.initState();
    _enableWakeLock(); // 启用屏幕常亮
    _checkStartPosition();
  }

  /// 启用屏幕常亮
  Future<void> _enableWakeLock() async {
    try {
      await WakelockPlus.enable();
    } catch (e) {
      debugPrint('启用屏幕常亮失败: $e');
    }
  }

  /// 禁用屏幕常亮
  Future<void> _disableWakeLock() async {
    try {
      await WakelockPlus.disable();
    } catch (e) {
      debugPrint('禁用屏幕常亮失败: $e');
    }
  }

  /// 检查起始位置
  Future<void> _checkStartPosition() async {
    try {
      final location = await _locationService.getCurrentLocation();
      if (mounted) {
        // 检查组件是否仍然挂载
        setState(() {
          _currentLatitude = location.latitude;
          _currentLongitude = location.longitude;
          // 计算当前位置到起点多边形的最短距离（用于判断是否可以开始）
          _distanceToStart = LocationUtils.getDistanceToPolygon(
            pointLat: location.latitude,
            pointLon: location.longitude,
            polygon: widget.track.startPolygon,
          );
          // 计算当前位置到终点多边形的最短距离
          _distanceToEnd = LocationUtils.getDistanceToPolygon(
            pointLat: location.latitude,
            pointLon: location.longitude,
            polygon: widget.track.endPolygon,
          );
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('获取定位失败: $e')));
      }
    }
  }

  /// 开始跟跑
  Future<void> _startRunning() async {
    // 验证是否在起点附近（到起点多边形边500米内）
    if (_distanceToStart == null || _distanceToStart! > 500) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请前往起点附近再开始（500米内）')));
      return;
    }

    try {
      if (mounted) {
        // 检查组件是否仍然挂载
        setState(() {
          _isStarted = true;
        });
      }

      // 获取当前用户
      final user = await _db.getCurrentUser();
      if (user == null) {
        throw Exception('用户信息不存在');
      }

      // 获取主车辆
      final mainCar = await _db.getMainCar();

      // 创建轨迹记录
      final record = TrackRecord(
        userId: user.id!,
        trackId: widget.track.id!,
        carId: mainCar?.id,
        startTime: DateTime.now().toIso8601String(),
        status: 'incomplete',
        manuallyStopped: false,
      );
      _recordId = await _db.createTrackRecord(record);
      _pointSequence = 0;

      // 请求通知权限并显示跑步通知
      if (mounted) {
        await _notificationService.requestPermissions();
        await _notificationService.showRunningNotification(
          elapsedTime: LocationUtils.formatDuration(_elapsedTime),
          distance: 0,
          speed: 0,
        );
      }

      // 开始连续定位
      _locationSubscription = _locationService.startContinuousLocation().listen(
        (location) {
          _handleLocationUpdate(location);
        },
        onError: (error) {
          if (mounted) {
            ScaffoldMessenger.of(
              context,
            ).showSnackBar(SnackBar(content: Text('定位错误: $error')));
            _stopRunning(saveRecord: false);
          }
        },
      );

      // 注意：计时器不在这里启动，而是在真正进入起点区域且速度>15km/h时才启动
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('启动失败: $e')));
        _stopRunning(saveRecord: false);
      }
    }
  }

  /// 处理定位更新
  Future<void> _handleLocationUpdate(LocationData location) async {
    if (!mounted || _isStopping) return; // 如果组件已销毁或正在停止则直接返回

    setState(() {
      _currentLatitude = location.latitude;
      _currentLongitude = location.longitude;
      _currentSpeed = location.speed;

      // 更新到起点的距离
      _distanceToStart = LocationUtils.getDistanceToPolygon(
        pointLat: location.latitude,
        pointLon: location.longitude,
        polygon: widget.track.startPolygon,
      );

      // 更新到终点的距离
      _distanceToEnd = LocationUtils.getDistanceToPolygon(
        pointLat: location.latitude,
        pointLon: location.longitude,
        polygon: widget.track.endPolygon,
      );
    });

    // 保存轨迹点到缓存
    if (_recordId != null) {
      final point = TrackPoint(
        recordId: _recordId!,
        latitude: location.latitude,
        longitude: location.longitude,
        speed: location.speed,
        timestamp: location.timestamp.toIso8601String(),
        sequence: _pointSequence++,
      );

      _pendingPoints.add(point);

      // 批量存储：当缓存达到一定数量时写入数据库
      if (_pendingPoints.length >= _batchSize) {
        await _flushPoints();
      }

      // 检查是否到达终点
      if (!_isTiming) {
        // 检查是否在起点区域内
        final isInStartArea = LocationUtils.isPointInPolygon(
          pointLat: location.latitude,
          pointLon: location.longitude,
          polygon: widget.track.startPolygon,
        );

        // 更新进入起点区域的状态
        if (isInStartArea != _isInStartArea) {
          if (mounted) {
            // 检查组件是否仍然挂载
            setState(() {
              _isInStartArea = isInStartArea;
            });
          }
        }

        // 检查是否满足计时触发条件（在起点围栏内且速度>15km/h）
        if (isInStartArea && (location.speed ?? 0) > 15) {
          if (mounted) {
            // 检查组件是否仍然挂载
            setState(() {
              _isTiming = true;
            });
            // 真正开始计时
            _startTimer();
          }
        }
      } else {
        // 已经在计时中，检查是否到达终点
        // 使用射线法判断点是否在终点多边形内
        final isInEndArea = LocationUtils.isPointInPolygon(
          pointLat: location.latitude,
          pointLon: location.longitude,
          polygon: widget.track.endPolygon,
        );

        if (isInEndArea) {
          await _flushPoints(); // 停止前刷新所有缓存点
          if (mounted) {
            _stopRunning(saveRecord: true);
          }
        }
      }
    }

    // 更新通知（每秒更新一次）
    if (_isStarted && _isTiming && mounted) {
      await _updateNotification(location);
    }
  }

  /// 批量写入轨迹点到数据库
  Future<void> _flushPoints() async {
    if (_pendingPoints.isEmpty) return;

    try {
      // 批量插入
      for (final point in _pendingPoints) {
        await _db.insertTrackPoint(point);
      }
      _pendingPoints.clear();
    } catch (e) {
      debugPrint('批量存储轨迹点失败: $e');
    }
  }

  /// 更新通知内容
  Future<void> _updateNotification(LocationData location) async {
    try {
      // 计算距离（使用简单的累加方式）
      if (_currentLatitude != null && _currentLongitude != null) {
        final distance = LocationUtils.calculateDistance(
          _currentLatitude!,
          _currentLongitude!,
          location.latitude,
          location.longitude,
        );
        _totalDistance += distance;
      }

      // 更新通知
      await _notificationService.updateRunningNotification(
        elapsedTime: LocationUtils.formatDuration(_elapsedTime),
        distance: _totalDistance / 1000, // 转换为公里
        speed: location.speed ?? 0,
      );
    } catch (e) {
      debugPrint('更新通知失败: $e');
    }
  }

  /// 启动计时器
  void _startTimer() {
    _timer = Timer.periodic(const Duration(milliseconds: 100), (timer) {
      if (mounted) {
        // 检查组件是否仍然挂载
        setState(() {
          _elapsedTime += 0.1;
        });
      }
    });
  }

  /// 停止跟跑
  Future<void> _stopRunning({
    required bool saveRecord,
    bool manuallyStopped = false,
  }) async {
    // 标记为停止状态，阻止后续定位数据处理
    if (mounted) {
      setState(() {
        _isStopping = true;
      });
    }

    // 立即取消订阅和计时器，避免继续接收数据和计时
    await _locationSubscription?.cancel();
    _timer?.cancel();

    // 显示停止提示
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(manuallyStopped ? '已手动停止，正在保存数据...' : '已完成，正在保存数据...'),
          duration: const Duration(seconds: 2),
        ),
      );
    }

    // 在后台执行数据保存操作（不阻塞UI）
    _saveRunningData(saveRecord: saveRecord, manuallyStopped: manuallyStopped);

    // 重置距离
    _totalDistance = 0;
  }

  /// 后台保存跑步数据
  Future<void> _saveRunningData({
    required bool saveRecord,
    bool manuallyStopped = false,
  }) async {
    try {
      // 刷新所有缓存的轨迹点
      await _flushPoints();

      // 停止定位服务
      await _locationService.stopLocation();

      // 隐藏通知
      if (mounted) {
        await _notificationService.hideNotification();
      }

      // 禁用屏幕常亮
      await _disableWakeLock();

      if (saveRecord && _recordId != null) {
        // 更新轨迹记录为完成状态
        final record = await _db.getTrackRecord(_recordId!);
        if (record != null) {
          await _db.updateTrackRecord(
            record.copyWith(
              endTime: DateTime.now().toIso8601String(),
              duration: _elapsedTime,
              status: manuallyStopped ? 'incomplete' : 'completed',
              manuallyStopped: manuallyStopped,
            ),
          );
        }

        debugPrint(
          manuallyStopped
              ? '已手动停止！用时: ${LocationUtils.formatDuration(_elapsedTime)}（未完成）'
              : '完成！用时: ${LocationUtils.formatDuration(_elapsedTime)}',
        );
      } else {
        // 删除不完整的记录
        if (_recordId != null) {
          await _db.deleteTrackRecord(_recordId!);
        }

        debugPrint('已停止，本次轨迹未保存');
      }
    } catch (e) {
      debugPrint('保存跑步数据失败: $e');
    } finally {
      // 保存完成后返回上一页
      if (mounted) {
        Navigator.pop(context);
      }
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    _locationSubscription?.cancel();
    _locationService.stopLocation();
    _disableWakeLock(); // 确保禁用屏幕常亮
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // 开始按钮的触发条件：距离起点多边形边500米内
    final canStart =
        !_isStarted && _distanceToStart != null && _distanceToStart! <= 500;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.track.name),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          if (_isStarted)
            IconButton(
              icon: const Icon(Icons.stop),
              onPressed: () =>
                  _stopRunning(saveRecord: true, manuallyStopped: true),
              tooltip: '手动停止',
            ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // 用时显示
            Card(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  children: [
                    const Text(
                      '用时',
                      style: TextStyle(fontSize: 18, color: Colors.grey),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      LocationUtils.formatDuration(_elapsedTime),
                      style: const TextStyle(
                        fontSize: 48,
                        fontWeight: FontWeight.bold,
                        fontFamily: 'monospace',
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // 状态信息
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    // 停止中提示
                    if (_isStopping)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(
                              Icons.hourglass_empty,
                              color: Colors.orange,
                            ),
                            const SizedBox(width: 8),
                            Text(
                              '正在保存数据...',
                              style: TextStyle(
                                color: Colors.orange,
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                              ),
                            ),
                          ],
                        ),
                      ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(_isStarted ? '距离终点' : '距离起点'),
                        Text(
                          _isStarted
                              ? (_distanceToEnd != null
                                    ? '${_distanceToEnd!.toStringAsFixed(0)} 米'
                                    : '计算中...')
                              : (_distanceToStart != null
                                    ? '${_distanceToStart!.toStringAsFixed(0)} 米'
                                    : '计算中...'),
                          style: TextStyle(
                            color: _isStarted
                                ? (_distanceToEnd != null &&
                                          _distanceToEnd! <= 100
                                      ? Colors.green
                                      : Colors.orange)
                                : (_distanceToStart != null &&
                                          _distanceToStart! <= 500
                                      ? Colors.green
                                      : Colors.orange),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    if (_currentSpeed != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text('当前速度'),
                            Text(
                              '${_currentSpeed!.toStringAsFixed(1)} km/h',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    if (_isInStartArea)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.check_circle, color: Colors.green),
                            SizedBox(width: 8),
                            Text(
                              '已进入起点区域',
                              style: TextStyle(
                                color: Colors.green,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    if (_isTiming)
                      const Padding(
                        padding: EdgeInsets.only(top: 8),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.timer, color: Colors.green),
                            SizedBox(width: 8),
                            Text(
                              '计时中...',
                              style: TextStyle(
                                color: Colors.green,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 32),

            // 开始/停止按钮
            if (!_isStarted)
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton.icon(
                  onPressed: canStart ? _startRunning : null,
                  icon: const Icon(Icons.play_arrow, size: 28),
                  label: Text(
                    canStart ? '开始跟跑' : '请前往起点',
                    style: const TextStyle(fontSize: 18),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: canStart
                        ? Theme.of(context).colorScheme.primary
                        : Colors.grey,
                    foregroundColor: Colors.white,
                  ),
                ),
              )
            else
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton.icon(
                  onPressed: () =>
                      _stopRunning(saveRecord: true, manuallyStopped: true),
                  icon: const Icon(Icons.stop, size: 28),
                  label: const Text('结束并保存', style: TextStyle(fontSize: 18)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                  ),
                ),
              ),

            const SizedBox(height: 16),

            // 提示信息
            if (!_isStarted)
              Text(
                _distanceToStart != null && _distanceToStart! > 500
                    ? '您距离起点${_distanceToStart!.toStringAsFixed(0)}米，请前往起点附近（500米内）'
                    : '准备就绪，点击开始按钮开始跟跑',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey[600], fontSize: 14),
              )
            else
              Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.blue.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.blue.withOpacity(0.3)),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.blue, size: 20),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '🔔 锁屏通知已启用',
                                style: TextStyle(
                                  color: Colors.blue[900],
                                  fontSize: 13,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                '锁屏后仍可查看计时器，下拉通知栏可控制',
                                style: TextStyle(
                                  color: Colors.blue[900],
                                  fontSize: 11,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),
                  if (_isTiming)
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.green.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: Colors.green.withOpacity(0.3),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.timer, color: Colors.green, size: 20),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              '计时中...进入终点区域将自动停止',
                              style: TextStyle(
                                color: Colors.green[900],
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
