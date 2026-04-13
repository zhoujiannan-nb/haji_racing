import 'dart:async';
import 'package:flutter/material.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../models/track.dart';
import '../models/track_record.dart';
import '../models/track_point.dart';
import '../database/database_helper.dart';
import '../services/location_service.dart';
import '../utils/location_utils.dart';

class TrackRunningPage extends StatefulWidget {
  final Track track;

  const TrackRunningPage({super.key, required this.track});

  @override
  State<TrackRunningPage> createState() => _TrackRunningPageState();
}

class _TrackRunningPageState extends State<TrackRunningPage> {
  final LocationService _locationService = LocationService();
  final DatabaseHelper _db = DatabaseHelper.instance;

  bool _isStarted = false;
  bool _isTiming = false;
  double _elapsedTime = 0;
  Timer? _timer;
  StreamSubscription<LocationData>? _locationSubscription;

  int? _recordId;
  int _pointSequence = 0;
  double? _currentLatitude;
  double? _currentLongitude;
  double? _distanceToStart;
  double? _currentSpeed;

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
      setState(() {
        _currentLatitude = location.latitude;
        _currentLongitude = location.longitude;
        // 计算当前位置到起点多边形的最短距离
        _distanceToStart = LocationUtils.getDistanceToPolygon(
          pointLat: location.latitude,
          pointLon: location.longitude,
          polygon: widget.track.startPolygon,
        );
      });
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
    // 验证是否在起点附近（到多边形最近点200米内）
    if (_distanceToStart == null || _distanceToStart! > 200) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请前往起点附近再开始（200米内）')));
      return;
    }

    try {
      setState(() {
        _isStarted = true;
      });

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
      );
      _recordId = await _db.createTrackRecord(record);
      _pointSequence = 0;

      // 开始连续定位
      _locationSubscription = _locationService.startContinuousLocation().listen(
        (location) {
          _handleLocationUpdate(location);
        },
        onError: (error) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('定位错误: $error')));
          _stopRunning(saveRecord: false);
        },
      );

      // 启动计时器
      _startTimer();
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('启动失败: $e')));
      _stopRunning(saveRecord: false);
    }
  }

  /// 处理定位更新
  Future<void> _handleLocationUpdate(LocationData location) async {
    setState(() {
      _currentLatitude = location.latitude;
      _currentLongitude = location.longitude;
      _currentSpeed = location.speed;
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
        // 检查是否满足计时触发条件（在起点围栏内且速度>15km/h）
        // 使用射线法判断点是否在起点多边形内
        final isInStartArea = LocationUtils.isPointInPolygon(
          pointLat: location.latitude,
          pointLon: location.longitude,
          polygon: widget.track.startPolygon,
        );

        if (isInStartArea && (location.speed ?? 0) > 15) {
          setState(() {
            _isTiming = true;
          });
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
          _stopRunning(saveRecord: true);
        }
      }
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

  /// 启动计时器
  void _startTimer() {
    _timer = Timer.periodic(const Duration(milliseconds: 100), (timer) {
      setState(() {
        _elapsedTime += 0.1;
      });
    });
  }

  /// 停止跟跑
  Future<void> _stopRunning({required bool saveRecord}) async {
    // 先刷新所有缓存的轨迹点
    await _flushPoints();

    // 停止定位
    await _locationSubscription?.cancel();
    await _locationService.stopLocation();

    // 停止计时器
    _timer?.cancel();

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
            status: 'completed',
          ),
        );
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '完成！用时: ${LocationUtils.formatDuration(_elapsedTime)}',
            ),
          ),
        );
      }
    } else {
      // 删除不完整的记录
      if (_recordId != null) {
        await _db.deleteTrackRecord(_recordId!);
      }

      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('已停止，本次轨迹未保存')));
      }
    }

    setState(() {
      _isStarted = false;
      _isTiming = false;
      _elapsedTime = 0;
      _recordId = null;
      _pointSequence = 0;
      _pendingPoints.clear(); // 清空缓存
    });

    // 返回上一页
    if (mounted) {
      Navigator.pop(context);
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
    // 开始按钮的触发条件：距离起点多边形最近点200米内
    final canStart =
        !_isStarted && _distanceToStart != null && _distanceToStart! <= 200;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.track.name),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          if (_isStarted)
            IconButton(
              icon: const Icon(Icons.stop),
              onPressed: () => _stopRunning(saveRecord: false),
              tooltip: '停止',
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
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('距离起点'),
                        Text(
                          _distanceToStart != null
                              ? '${_distanceToStart!.toStringAsFixed(0)} 米'
                              : '计算中...',
                          style: TextStyle(
                            color:
                                _distanceToStart != null &&
                                    _distanceToStart! <= 200
                                ? Colors.green
                                : Colors.orange,
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

            // 开始按钮
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
              ),

            const SizedBox(height: 16),

            // 提示信息
            if (!_isStarted)
              Text(
                _distanceToStart != null && _distanceToStart! > 200
                    ? '您距离起点${_distanceToStart!.toStringAsFixed(0)}米，请前往起点附近（200米内）'
                    : '准备就绪，点击开始按钮开始跟跑',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey[600], fontSize: 14),
              ),
          ],
        ),
      ),
    );
  }
}
