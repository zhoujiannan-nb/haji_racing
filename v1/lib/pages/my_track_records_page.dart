import 'dart:convert';
import 'package:flutter/material.dart';
import '../models/track_record.dart';
import '../models/track.dart';
import '../models/car.dart';
import '../database/database_helper.dart';
import '../utils/location_utils.dart';
import 'track_record_detail_page.dart';

class MyTrackRecordsPage extends StatefulWidget {
  const MyTrackRecordsPage({super.key});

  @override
  State<MyTrackRecordsPage> createState() => _MyTrackRecordsPageState();
}

class _MyTrackRecordsPageState extends State<MyTrackRecordsPage> {
  final DatabaseHelper _db = DatabaseHelper.instance;
  List<TrackRecord> _records = [];
  final Map<int, Track?> _trackCache = {};
  final Map<int, Car?> _carCache = {};
  bool _isLoading = true;
  String? _currentUsername;

  @override
  void initState() {
    super.initState();
    _loadRecords();
  }

  Future<void> _loadRecords() async {
    setState(() {
      _isLoading = true;
    });

    try {
      // 获取当前用户
      final user = await _db.getCurrentUser();
      if (user == null) {
        setState(() {
          _isLoading = false;
        });
        return;
      }

      // 保存用户名
      _currentUsername = user.username;

      final records = await _db.getUserTrackRecords(user.id!);

      // 缓存赛道和车辆信息
      for (var record in records) {
        if (!_trackCache.containsKey(record.trackId)) {
          final track = await _db.getTrack(record.trackId);
          _trackCache[record.trackId] = track;
        }
        if (record.carId != null && !_carCache.containsKey(record.carId)) {
          final car = await _db.getCar(record.carId!);
          _carCache[record.carId!] = car;
        }
      }

      // 修复缺少时间的记录
      for (var record in records) {
        if (_needsTimeCalculation(record)) {
          final track = _trackCache[record.trackId];
          if (track != null) {
            debugPrint('🔧 检测到记录 ${record.id} 缺少时间信息，开始计算...');

            final times = await _calculateTimesFromTrajectory(
              record: record,
              track: track,
            );

            if (times != null) {
              await _updateRecordTimes(
                record: record,
                startTime: times['startTime'],
                endTime: times['endTime'],
                duration: times['duration'],
              );

              debugPrint(
                '✅ 记录 ${record.id} 时间计算完成: duration=${times['duration']}s',
              );

              // 更新内存中的记录
              final index = records.indexWhere((r) => r.id == record.id);
              if (index != -1) {
                records[index] = record.copyWith(
                  startTime: times['startTime'],
                  endTime: times['endTime'],
                  duration: times['duration'],
                );
              }
            } else {
              debugPrint('⚠️ 记录 ${record.id} 无法计算时间（无轨迹数据）');
            }
          }
        }
      }

      setState(() {
        _records = records;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('加载失败: $e')));
      }
    }
  }

  /// 格式化时间为 x分x秒
  String _formatDuration(double? duration) {
    if (duration == null) return '--';
    final minutes = duration ~/ 60;
    final seconds = (duration % 60).toInt();
    return '${minutes}分${seconds.toString().padLeft(2, '0')}秒';
  }

  /// 格式化日期时间
  String _formatDateTime(String? dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.isEmpty) return '--';
    try {
      final dateTime = DateTime.parse(dateTimeStr);
      return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} ${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
    } catch (e) {
      return dateTimeStr;
    }
  }

  /// 检查记录是否需要修复时间计算
  bool _needsTimeCalculation(TrackRecord record) {
    // 检查 duration 是否为 null 或 0
    final durationInvalid = record.duration == null || record.duration == 0;
    // 检查 endTime 是否为 null 或空字符串
    final endTimeInvalid = record.endTime == null || record.endTime!.isEmpty;

    return durationInvalid && endTimeInvalid;
  }

  /// 从 trajectoryJson 解析轨迹点
  List<Map<String, dynamic>>? _parseTrajectoryPoints(String? trajectoryJson) {
    if (trajectoryJson == null || trajectoryJson.isEmpty) {
      return null;
    }

    try {
      final jsonData = jsonDecode(trajectoryJson);
      if (jsonData is Map<String, dynamic> && jsonData['points'] is List) {
        return List<Map<String, dynamic>>.from(jsonData['points']);
      }
    } catch (e) {
      debugPrint('解析 trajectoryJson 失败: $e');
    }

    return null;
  }

  /// 从轨迹数据计算开始和结束时间
  Future<Map<String, dynamic>?> _calculateTimesFromTrajectory({
    required TrackRecord record,
    required Track track,
  }) async {
    // 解析轨迹点
    final points = _parseTrajectoryPoints(record.trajectoryJson);
    if (points == null || points.isEmpty) {
      return null;
    }

    // 按时间顺序排序（根据 timestamp 字段）
    points.sort((a, b) {
      final timeA = a['timestamp'] as String;
      final timeB = b['timestamp'] as String;
      return timeA.compareTo(timeB);
    });

    String? calculatedStartTime;
    String? calculatedEndTime;

    // 遍历所有轨迹点，查找起点和终点
    for (final point in points) {
      final lat = point['latitude'] as double;
      final lon = point['longitude'] as double;
      final speed = point['speed'] as double? ?? 0;
      final timestamp = point['timestamp'] as String;

      // 检查是否在起点围栏内且速度 >= 15 km/h
      if (calculatedStartTime == null) {
        final isInStartArea = LocationUtils.isPointInPolygon(
          pointLat: lat,
          pointLon: lon,
          polygon: track.startPolygon,
        );

        if (isInStartArea && speed >= 15) {
          calculatedStartTime = timestamp;
        }
      }

      // 检查是否在终点围栏内
      if (calculatedEndTime == null && calculatedStartTime != null) {
        final isInEndArea = LocationUtils.isPointInPolygon(
          pointLat: lat,
          pointLon: lon,
          polygon: track.endPolygon,
        );

        if (isInEndArea) {
          calculatedEndTime = timestamp;
        }
      }

      // 如果已经找到开始和结束时间，提前退出
      if (calculatedStartTime != null && calculatedEndTime != null) {
        break;
      }
    }

    // 如果没有找到有效的开始和结束时间，使用默认值
    if (calculatedStartTime == null || calculatedEndTime == null) {
      calculatedStartTime = '2026-01-01T00:00:00.000Z';
      calculatedEndTime = '2099-01-01T00:00:00.000Z';
      // duration = 999分钟 = 59940秒
      return {
        'startTime': calculatedStartTime,
        'endTime': calculatedEndTime,
        'duration': 59940.0,
      };
    }

    // 计算 duration（秒）
    final startDateTime = DateTime.parse(calculatedStartTime);
    final endDateTime = DateTime.parse(calculatedEndTime);
    final duration =
        endDateTime.difference(startDateTime).inMilliseconds / 1000.0;

    return {
      'startTime': calculatedStartTime,
      'endTime': calculatedEndTime,
      'duration': duration,
    };
  }

  /// 更新记录的时间信息到数据库
  Future<void> _updateRecordTimes({
    required TrackRecord record,
    required String startTime,
    required String endTime,
    required double duration,
  }) async {
    final updatedRecord = record.copyWith(
      startTime: startTime,
      endTime: endTime,
      duration: duration,
    );

    await _db.updateTrackRecord(updatedRecord);
  }

  Future<void> _deleteRecord(int recordId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除这条轨迹记录吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await _db.deleteTrackRecord(recordId);
        setState(() {
          _records.removeWhere((r) => r.id == recordId);
        });
        if (mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('删除成功')));
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('删除失败: $e')));
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A1A1A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1A1A1A),
        elevation: 0,
        title: const Text(
          '我的轨迹',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 24,
          ),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFFF3D00)),
            )
          : _records.isEmpty
          ? _buildEmptyState()
          : RefreshIndicator(
              onRefresh: _loadRecords,
              color: const Color(0xFFFF3D00),
              child: ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: _records.length,
                itemBuilder: (context, index) {
                  final record = _records[index];
                  final track = _trackCache[record.trackId];
                  final car = record.carId != null
                      ? _carCache[record.carId]
                      : null;

                  return _buildRecordCard(record, track, car);
                },
              ),
            ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.route, size: 80, color: Colors.grey[700]),
          const SizedBox(height: 20),
          Text(
            '暂无轨迹记录',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.grey[400],
            ),
          ),
          const SizedBox(height: 10),
          Text(
            '完成一次跟跑后即可查看轨迹',
            style: TextStyle(fontSize: 14, color: Colors.grey[600]),
          ),
        ],
      ),
    );
  }

  Widget _buildRecordCard(TrackRecord record, Track? track, Car? car) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Colors.grey[900]!, Colors.grey[850]!],
        ),
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: Colors.grey[800]!, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.3),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(15),
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => TrackRecordDetailPage(record: record),
              ),
            );
          },
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 顶部：赛道名称和用时
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        track?.name ?? '未知赛道',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          _formatDuration(record.duration),
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: record.manuallyStopped
                                ? Colors.orange
                                : const Color(0xFFFF3D00),
                          ),
                        ),
                        if (record.manuallyStopped)
                          Text(
                            '未完成',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.orange,
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 12),

                // 中部：车辆和用户信息
                Row(
                  children: [
                    Icon(
                      Icons.directions_car,
                      size: 16,
                      color: Colors.grey[500],
                    ),
                    const SizedBox(width: 4),
                    Text(
                      car?.name ?? '默认车辆',
                      style: TextStyle(fontSize: 13, color: Colors.grey[400]),
                    ),
                    const SizedBox(width: 16),
                    Icon(Icons.person, size: 16, color: Colors.grey[500]),
                    const SizedBox(width: 4),
                    Text(
                      _currentUsername ?? '游客',
                      style: TextStyle(fontSize: 13, color: Colors.grey[400]),
                    ),
                  ],
                ),
                const SizedBox(height: 8),

                // 底部：时间信息
                Row(
                  children: [
                    Icon(Icons.access_time, size: 16, color: Colors.grey[500]),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(
                        _formatDateTime(record.endTime ?? record.startTime),
                        style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    IconButton(
                      icon: const Icon(
                        Icons.delete,
                        color: Colors.red,
                        size: 20,
                      ),
                      onPressed: () => _deleteRecord(record.id!),
                      tooltip: '删除',
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
