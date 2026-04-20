import 'dart:async';
import 'dart:convert';
import 'dart:io';
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
  bool _showLocationIndicator = false; // 是否显示定位灯
  bool _isLocationReady = false; // GPS是否已就绪（预热完成）

  // Debug日志开关 - 设置为true可开启定位信息调试日志
  static const bool _debugLogEnabled = false;
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

  // JSON轨迹数据相关
  List<Map<String, dynamic>> _trajectoryPoints = []; // 轨迹点列表
  String? _trajectoryFilePath; // 临时JSON文件路径

  @override
  void initState() {
    super.initState();
    _enableWakeLock(); // 启用屏幕常亮
    _checkStartPosition();
    _startLocationPreheat(); // 启动GPS预热
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

  /// 启动GPS预热（提前开始连续定位，但不记录数据）
  Future<void> _startLocationPreheat() async {
    try {
      // 开始连续定位，但此时 _recordId 为 null，所以不会记录数据
      _locationSubscription = _locationService.startContinuousLocation().listen(
        (location) {
          if (!mounted) return;

          // Debug日志：打印每次获取到的定位信息
          if (_debugLogEnabled) {
            debugPrint(
              '📍 [DEBUG] 定位更新 - '
              '纬度: ${location.latitude.toStringAsFixed(6)}, '
              '经度: ${location.longitude.toStringAsFixed(6)}, '
              '速度: ${(location.speed ?? 0).toStringAsFixed(2)} km/h, '
              '时间戳: ${location.timestamp.toIso8601String()}',
            );
          }

          // 更新当前位置信息（用于显示和距离计算）
          setState(() {
            _currentLatitude = location.latitude;
            _currentLongitude = location.longitude;
            _currentSpeed = location.speed;
            _isLocationReady = true; // 标记GPS已就绪

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

            // 触发定位灯闪烁效果（让用户知道GPS在工作）
            _triggerLocationIndicator();
          });

          // 如果已经开始跟跑，则处理定位数据
          // 注意：只有当 _recordId != null 且满足条件时才会记录轨迹点
          if (_isStarted && _recordId != null) {
            _handleLocationUpdate(location);
          }
        },
        onError: (error) {
          if (mounted) {
            debugPrint('定位错误: $error');
          }
        },
      );
    } catch (e) {
      debugPrint('GPS预热失败: $e');
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
  ///
  /// 用户点击"开始跟跑"按钮时调用
  /// 此时会创建数据库记录，但不会立即开始计时和记录轨迹点
  /// 真正的计时和记录要等到满足条件（起点区域内 + 速度>15km/h）才开始
  Future<void> _startRunning() async {
    // 验证是否在起点附近（到起点多边形边500米内）
    if (_distanceToStart == null || _distanceToStart! > 500) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请前往起点附近再开始（500米内）')));
      return;
    }

    // 检查GPS是否已就绪
    if (!_isLocationReady) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('GPS信号准备中，请稍候...')));
      return;
    }

    try {
      if (mounted) {
        setState(() {
          _isStarted = true; // 标记为已开始状态
        });
      }

      // 获取当前用户
      final user = await _db.getCurrentUser();
      if (user == null) {
        throw Exception('用户信息不存在');
      }

      // 获取主车辆
      final mainCar = await _db.getMainCar();

      // 创建轨迹记录（初始状态为incomplete）
      // 注意：此时只是创建了记录，还没有开始计时和记录轨迹点
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

      debugPrint('📝 创建轨迹记录，ID: $_recordId');

      // 请求通知权限并显示通知
      if (mounted) {
        await _notificationService.requestPermissions();
        await _notificationService.showRunningNotification(
          elapsedTime: LocationUtils.formatDuration(_elapsedTime),
          distance: 0,
          speed: 0,
        );
      }

      // 注意：定位订阅已经在 _startLocationPreheat 中启动
      // 现在 _recordId 已设置，_handleLocationUpdate 会开始处理定位数据
      // 但只有当满足条件时才会真正开始计时和记录轨迹点
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
  ///
  /// 核心逻辑：
  /// 1. 持续接收GPS定位数据，用于UI显示和距离计算
  /// 2. 只有当满足以下条件时才开始计时并记录轨迹点到文件：
  ///    - 已进入起点区域（在起点多边形内）
  ///    - 速度 > 15 km/h
  /// 3. 到达终点区域时自动停止并保存数据
  Future<void> _handleLocationUpdate(LocationData location) async {
    if (!mounted || _isStopping) return; // 如果组件已销毁或正在停止则直接返回

    // Debug日志：打印每次获取到的定位信息
    if (_debugLogEnabled) {
      debugPrint(
        '📍 [DEBUG] 跟跑中定位更新 - '
        '纬度: ${location.latitude.toStringAsFixed(6)}, '
        '经度: ${location.longitude.toStringAsFixed(6)}, '
        '速度: ${(location.speed ?? 0).toStringAsFixed(2)} km/h, '
        '计时状态: $_isTiming, '
        '时间戳: ${location.timestamp.toIso8601String()}',
      );
    }

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

      // 触发定位灯闪烁效果
      _triggerLocationIndicator();
    });

    // 检查是否满足计时触发条件（在起点围栏内且速度>15km/h）
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
          setState(() {
            _isInStartArea = isInStartArea;
          });
        }
      }

      // 检查是否满足计时触发条件（在起点围栏内且速度>15km/h）
      if (isInStartArea && (location.speed ?? 0) > 15) {
        if (mounted) {
          setState(() {
            _isTiming = true;
          });
          _startTimer();
          debugPrint(
            '✅ 开始计时！位置: 起点区域内, 速度: ${location.speed?.toStringAsFixed(1)} km/h',
          );
        }
      }
    } else {
      // 已经在计时中，检查是否到达终点
      final isInEndArea = LocationUtils.isPointInPolygon(
        pointLat: location.latitude,
        pointLon: location.longitude,
        polygon: widget.track.endPolygon,
      );

      if (isInEndArea && _isStopping) {
        if (mounted) {
          debugPrint('🏁 到达终点区域，自动停止！');
          _stopRunning(saveRecord: true);
        }
      }
    }

    // 只有在计时中才保存轨迹点到JSON文件这样确保只记录有效比赛过程中的轨迹点
    if (_isTiming && _recordId != null) {
      final pointData = {
        'latitude': location.latitude,
        'longitude': location.longitude,
        'speed': location.speed,
        'timestamp': location.timestamp.toIso8601String(),
        'sequence': _pointSequence++,
      };

      _trajectoryPoints.add(pointData);

      // 实时追加写入到临时文件
      await _appendPointToFile(pointData);
    }

    // 更新通知（每秒更新一次）
    if (_isStarted && _isTiming && mounted) {
      await _updateNotification(location);
    }
  }

  /// 触发定位灯闪烁效果
  void _triggerLocationIndicator() {
    if (!mounted) return;

    setState(() {
      _showLocationIndicator = true;
    });

    // 300ms后隐藏定位灯
    Future.delayed(const Duration(milliseconds: 300), () {
      if (mounted) {
        setState(() {
          _showLocationIndicator = false;
        });
      }
    });
  }

  /// 追加轨迹点到临时JSON文件
  Future<void> _appendPointToFile(Map<String, dynamic> pointData) async {
    try {
      // 如果还没有创建临时文件，则创建
      if (_trajectoryFilePath == null) {
        final tempDir = Directory.systemTemp;
        _trajectoryFilePath =
            '${tempDir.path}/trajectory_${_recordId}_${DateTime.now().millisecondsSinceEpoch}.json';

        // 创建初始JSON文件
        final initialData = {
          'points': [pointData],
        };
        final file = File(_trajectoryFilePath!);
        await file.writeAsString(jsonEncode(initialData));
      } else {
        // 读取现有JSON数据
        final file = File(_trajectoryFilePath!);
        String content = await file.readAsString();
        Map<String, dynamic> jsonData = jsonDecode(content);

        // 添加新点
        List<dynamic> points = jsonData['points'] ?? [];
        points.add(pointData);
        jsonData['points'] = points;

        // 写回文件
        await file.writeAsString(jsonEncode(jsonData));
      }
    } catch (e) {
      debugPrint('追加轨迹点到文件失败: $e');
    }
  }

  /// 将JSON文件中的轨迹数据保存到数据库
  Future<void> _saveTrajectoryToDatabase() async {
    if (_trajectoryFilePath == null ||
        !File(_trajectoryFilePath!).existsSync()) {
      return;
    }

    try {
      // 读取JSON文件内容
      final file = File(_trajectoryFilePath!);
      String content = await file.readAsString();

      // 更新记录中的trajectoryJson字段
      if (_recordId != null) {
        final record = await _db.getTrackRecord(_recordId!);
        if (record != null) {
          await _db.updateTrackRecord(record.copyWith(trajectoryJson: content));
        }

        // 解析JSON并插入轨迹点到track_points表（混合存储）
        Map<String, dynamic> jsonData = jsonDecode(content);
        List<dynamic> points = jsonData['points'] ?? [];

        List<TrackPoint> trackPoints = points.map((pointData) {
          return TrackPoint(
            recordId: _recordId!,
            latitude: pointData['latitude'],
            longitude: pointData['longitude'],
            speed: pointData['speed'],
            timestamp: pointData['timestamp'],
            sequence: pointData['sequence'],
          );
        }).toList();

        // 批量插入轨迹点到数据库
        if (trackPoints.isNotEmpty) {
          await _db.insertTrackPoints(trackPoints);
          debugPrint('✅ 已保存 ${trackPoints.length} 个轨迹点到数据库（包含速度等结构化数据）');
        }
      }

      // 删除临时文件
      await file.delete();
      _trajectoryFilePath = null;
      _trajectoryPoints.clear();
    } catch (e) {
      debugPrint('保存轨迹数据到数据库失败: $e');
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
  ///
  /// 两种情况会调用此方法：
  /// 1. 自动停止：到达终点区域
  /// 2. 手动停止：用户点击"结束并保存"按钮
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

    debugPrint(manuallyStopped ? '🛑 手动停止' : '✅ 自动停止（到达终点）');

    // 立即取消订阅和计时器，避免继续接收数据和计时
    await _locationSubscription?.cancel();
    _timer?.cancel();

    // 显示停止提示
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              Icon(
                manuallyStopped ? Icons.stop_circle : Icons.check_circle,
                color: manuallyStopped ? Colors.orange : Colors.green,
                size: 24,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      manuallyStopped ? '已手动停止' : '已完成',
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '正在保存数据...',
                      style: TextStyle(fontSize: 13, color: Colors.grey[300]),
                    ),
                  ],
                ),
              ),
            ],
          ),
          backgroundColor: const Color(0xFF2C2C2C),
          duration: const Duration(seconds: 2),
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.only(top: 80, left: 16, right: 16),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(
              color: (manuallyStopped ? Colors.orange : Colors.green)
                  .withOpacity(0.5),
              width: 2,
            ),
          ),
          elevation: 10,
        ),
      );
    }

    // 在后台执行数据保存操作（不阻塞UI）
    _saveRunningData(saveRecord: saveRecord, manuallyStopped: manuallyStopped);

    // 重置距离
    _totalDistance = 0;
  }

  /// 后台保存轨迹数据
  ///
  /// 此方法在停止跟跑后异步执行，不阻塞UI
  /// 主要完成以下工作：
  /// 1. 将JSON轨迹文件中的数据保存到数据库
  /// 2. 更新记录状态（completed/incomplete）
  /// 3. 清理资源（通知、屏幕常亮等）
  /// 4. 返回上一页
  Future<void> _saveRunningData({
    required bool saveRecord,
    bool manuallyStopped = false,
  }) async {
    try {
      // 保存JSON轨迹数据到数据库
      await _saveTrajectoryToDatabase();

      // 停止定位服务
      await _locationService.stopLocation();

      // 隐藏通知
      if (mounted) {
        await _notificationService.hideNotification();
      }

      // 禁用屏幕常亮
      await _disableWakeLock();

      if (saveRecord && _recordId != null) {
        // 检查是否有实际的轨迹点数据
        final hasTrajectoryData =
            _trajectoryPoints.isNotEmpty ||
            (_trajectoryFilePath != null &&
                File(_trajectoryFilePath!).existsSync());

        // 只有当有实际轨迹数据时才保存记录
        if (hasTrajectoryData) {
          // 更新轨迹记录的状态和结束时间
          final record = await _db.getTrackRecord(_recordId!);
          if (record != null) {
            await _db.updateTrackRecord(
              record.copyWith(
                endTime: DateTime.now().toIso8601String(),
                duration: _elapsedTime,
                // 手动停止标记为incomplete，自动到达终点标记为completed
                status: manuallyStopped ? 'incomplete' : 'completed',
                manuallyStopped: manuallyStopped,
              ),
            );
          }

          debugPrint(
            manuallyStopped
                ? '⚠️ 已手动停止！用时: ${LocationUtils.formatDuration(_elapsedTime)}（未完成）'
                : '✅ 完成！用时: ${LocationUtils.formatDuration(_elapsedTime)}',
          );
        } else {
          // 如果没有轨迹数据，删除这条记录并提示用户
          await _db.deleteTrackRecord(_recordId!);
          debugPrint('❌ 无有效轨迹数据，已删除空记录');

          // 显示UI提示
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Row(
                  children: [
                    const Icon(
                      Icons.warning_amber_rounded,
                      color: Colors.orange,
                      size: 24,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            '记录未保存',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            '检测到你没有移动，无轨迹点，不保存',
                            style: TextStyle(
                              fontSize: 13,
                              color: Colors.grey[300],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                backgroundColor: const Color(0xFF2C2C2C),
                duration: const Duration(seconds: 5),
                behavior: SnackBarBehavior.floating,
                margin: const EdgeInsets.only(top: 80, left: 16, right: 16),
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 16,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(
                    color: Colors.orange.withOpacity(0.5),
                    width: 2,
                  ),
                ),
                elevation: 10,
              ),
            );
          }
        }
      } else {
        // 删除不完整的记录（例如用户未开始就退出）
        if (_recordId != null) {
          await _db.deleteTrackRecord(_recordId!);
        }

        debugPrint('❌ 已停止，本次轨迹未保存');
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

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;

        // 如果已经开始跟跑，显示确认对话框
        if (_isStarted) {
          final shouldPop = await showDialog<bool>(
            context: context,
            barrierDismissible: false,
            builder: (context) => AlertDialog(
              backgroundColor: const Color(0xFF2C2C2C),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(15),
              ),
              title: const Row(
                children: [
                  Icon(Icons.warning_amber_rounded, color: Colors.orange),
                  SizedBox(width: 8),
                  Text('确认退出？', style: TextStyle(color: Colors.white)),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '当前正在跟跑中，退出将：',
                    style: TextStyle(color: Colors.white, fontSize: 14),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    '• 停止计时（当前用时：${LocationUtils.formatDuration(_elapsedTime)})',
                    style: TextStyle(color: Colors.grey[300], fontSize: 13),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '• 自动保存已记录的轨迹数据',
                    style: TextStyle(color: Colors.grey[300], fontSize: 13),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '• 标记为“未完成”状态',
                    style: TextStyle(color: Colors.orange, fontSize: 13),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context, false),
                  child: const Text('取消', style: TextStyle(color: Colors.grey)),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pop(context, true),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                  ),
                  child: const Text('确认退出'),
                ),
              ],
            ),
          );

          if (shouldPop == true) {
            // 用户确认退出，执行停止并保存
            await _stopRunning(saveRecord: true, manuallyStopped: true);
          }
        } else {
          // 未开始时直接返回
          Navigator.pop(context);
        }
      },
      child: Scaffold(
        backgroundColor: const Color(0xFF1A1A1A),
        appBar: AppBar(
          backgroundColor: const Color(0xFF1A1A1A),
          elevation: 0,
          title: Text(
            widget.track.name,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
              fontSize: 20,
            ),
          ),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white),
            onPressed: () => Navigator.pop(context),
          ),
          actions: [
            if (_isStarted)
              IconButton(
                icon: const Icon(Icons.stop, color: Colors.red),
                onPressed: _isStopping
                    ? null
                    : () =>
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
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(32),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [Colors.grey[900]!, Colors.grey[850]!],
                  ),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: const Color(0xFFFF3D00).withOpacity(0.3),
                    width: 2,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFFFF3D00).withOpacity(0.2),
                      blurRadius: 20,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    const Text(
                      '用时',
                      style: TextStyle(
                        fontSize: 18,
                        color: Colors.grey,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      LocationUtils.formatDuration(_elapsedTime),
                      style: const TextStyle(
                        fontSize: 56,
                        fontWeight: FontWeight.bold,
                        fontFamily: 'monospace',
                        color: Color(0xFFFF3D00),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // 状态信息
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.grey[900],
                  borderRadius: BorderRadius.circular(15),
                  border: Border.all(color: Colors.grey[800]!, width: 1),
                ),
                child: Column(
                  children: [
                    // 定位灯指示器
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          '定位状态',
                          style: TextStyle(
                            color: Colors.grey[400],
                            fontSize: 14,
                          ),
                        ),
                        Row(
                          children: [
                            if (!_isLocationReady && !_isStarted)
                              Padding(
                                padding: const EdgeInsets.only(right: 8),
                                child: Text(
                                  'GPS预热中...',
                                  style: TextStyle(
                                    color: Colors.orange,
                                    fontSize: 12,
                                  ),
                                ),
                              ),
                            AnimatedOpacity(
                              opacity: _showLocationIndicator ? 1.0 : 0.3,
                              duration: const Duration(milliseconds: 150),
                              child: Container(
                                width: 12,
                                height: 12,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: _showLocationIndicator
                                      ? (_isLocationReady
                                            ? Colors.green
                                            : Colors.orange)
                                      : Colors.grey[600],
                                  boxShadow: _showLocationIndicator
                                      ? [
                                          BoxShadow(
                                            color:
                                                (_isLocationReady
                                                        ? Colors.green
                                                        : Colors.orange)
                                                    .withOpacity(0.6),
                                            blurRadius: 8,
                                            spreadRadius: 2,
                                          ),
                                        ]
                                      : null,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
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
                        Text(
                          _isStarted ? '距离终点' : '距离起点',
                          style: TextStyle(
                            color: Colors.grey[400],
                            fontSize: 14,
                          ),
                        ),
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
                            fontSize: 16,
                          ),
                        ),
                      ],
                    ),
                    if (_currentSpeed != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              '当前速度',
                              style: TextStyle(
                                color: Colors.grey[400],
                                fontSize: 14,
                              ),
                            ),
                            Text(
                              '${_currentSpeed!.toStringAsFixed(1)} km/h',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                      ),
                    if (_isInStartArea)
                      Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.check_circle, color: Colors.green),
                            const SizedBox(width: 8),
                            const Text(
                              '已进入起点区域',
                              style: TextStyle(
                                color: Colors.green,
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                              ),
                            ),
                          ],
                        ),
                      ),
                    if (_isTiming)
                      const Padding(
                        padding: EdgeInsets.only(top: 12),
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
                                fontSize: 14,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
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
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: canStart
                          ? const Color(0xFFFF3D00)
                          : Colors.grey[700],
                      foregroundColor: Colors.white,
                      elevation: canStart ? 8 : 0,
                      shadowColor: canStart
                          ? const Color(0xFFFF3D00).withOpacity(0.4)
                          : null,
                    ),
                  ),
                )
              else
                SizedBox(
                  width: double.infinity,
                  height: 56,
                  child: ElevatedButton.icon(
                    onPressed: _isStopping
                        ? null
                        : () => _stopRunning(
                            saveRecord: true,
                            manuallyStopped: true,
                          ),
                    icon: const Icon(Icons.stop, size: 28),
                    label: Text(
                      _isStopping ? '保存中...' : '结束并保存',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _isStopping
                          ? Colors.grey[700]
                          : Colors.red,
                      foregroundColor: Colors.white,
                      elevation: _isStopping ? 0 : 8,
                      shadowColor: _isStopping
                          ? null
                          : Colors.red.withOpacity(0.4),
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
                  style: TextStyle(color: Colors.grey[500], fontSize: 14),
                )
              else
                Column(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFF3D00).withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: const Color(0xFFFF3D00).withOpacity(0.3),
                        ),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.info_outline,
                            color: Color(0xFFFF3D00),
                            size: 20,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  '🔔 锁屏通知已启用',
                                  style: TextStyle(
                                    color: Color(0xFFFF3D00),
                                    fontSize: 13,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '锁屏后仍可查看计时器，下拉通知栏可控制',
                                  style: TextStyle(
                                    color: Colors.grey[400],
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
                            const Icon(
                              Icons.timer,
                              color: Colors.green,
                              size: 20,
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                '计时中...进入终点区域将自动停止',
                                style: TextStyle(
                                  color: Colors.green[300],
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
      ),
    );
  }
}
