import 'package:flutter/material.dart';
import '../models/track_record.dart';
import '../models/track_point.dart';
import '../models/track.dart';
import '../models/car.dart';
import '../database/database_helper.dart';

class TrackRecordDetailPage extends StatefulWidget {
  final TrackRecord record;

  const TrackRecordDetailPage({super.key, required this.record});

  @override
  State<TrackRecordDetailPage> createState() => _TrackRecordDetailPageState();
}

class _TrackRecordDetailPageState extends State<TrackRecordDetailPage> {
  final DatabaseHelper _db = DatabaseHelper.instance;
  Track? _track;
  Car? _car;
  List<TrackPoint> _points = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadDetails();
  }

  Future<void> _loadDetails() async {
    setState(() {
      _isLoading = true;
    });

    try {
      // 加载赛道信息
      final track = await _db.getTrack(widget.record.trackId);

      // 加载车辆信息
      Car? car;
      if (widget.record.carId != null) {
        car = await _db.getCar(widget.record.carId!);
      }

      // 加载轨迹点
      final points = await _db.getTrackPoints(widget.record.id!);

      setState(() {
        _track = track;
        _car = car;
        _points = points;
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

  /// 计算均速（米/秒）
  double? _calculateAverageSpeed() {
    if (_track == null ||
        widget.record.duration == null ||
        widget.record.duration! <= 0) {
      return null;
    }
    // 均速 = 赛道长度 / 时间
    return _track!.length / widget.record.duration!;
  }

  /// 获取最大速度
  double? _getMaxSpeed() {
    if (_points.isEmpty) return null;

    double maxSpeed = 0;
    for (var point in _points) {
      if (point.speed != null && point.speed! > maxSpeed) {
        maxSpeed = point.speed!;
      }
    }
    return maxSpeed > 0 ? maxSpeed : null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A1A1A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1A1A1A),
        elevation: 0,
        title: const Text(
          '轨迹详情',
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
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 基本信息卡片
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
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
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.flag, color: Color(0xFFFF3D00)),
                            const SizedBox(width: 8),
                            Text(
                              _track?.name ?? '未知赛道',
                              style: const TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                        const Divider(height: 32, color: Colors.grey),

                        // 用时
                        Row(
                          children: [
                            const Icon(Icons.timer, color: Color(0xFFFF3D00)),
                            const SizedBox(width: 12),
                            const Text(
                              '用时',
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.grey,
                              ),
                            ),
                            const Spacer(),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  _formatDuration(widget.record.duration),
                                  style: TextStyle(
                                    fontSize: 24,
                                    fontWeight: FontWeight.bold,
                                    color: widget.record.manuallyStopped
                                        ? Colors.orange
                                        : const Color(0xFFFF3D00),
                                  ),
                                ),
                                if (widget.record.manuallyStopped)
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
                        const SizedBox(height: 16),

                        // 达成时间
                        Row(
                          children: [
                            const Icon(
                              Icons.access_time,
                              color: Color(0xFFFF3D00),
                            ),
                            const SizedBox(width: 12),
                            const Text(
                              '达成时间',
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.grey,
                              ),
                            ),
                            const Spacer(),
                            Text(
                              _formatDateTime(
                                widget.record.endTime ??
                                    widget.record.startTime,
                              ),
                              style: const TextStyle(
                                fontSize: 14,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),

                        // 用户
                        Row(
                          children: [
                            const Icon(Icons.person, color: Color(0xFFFF3D00)),
                            const SizedBox(width: 12),
                            const Text(
                              '用户',
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.grey,
                              ),
                            ),
                            const Spacer(),
                            const Text(
                              'race',
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),

                        // 车辆
                        Row(
                          children: [
                            const Icon(
                              Icons.directions_car,
                              color: Color(0xFFFF3D00),
                            ),
                            const SizedBox(width: 12),
                            const Text(
                              '车辆',
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.grey,
                              ),
                            ),
                            const Spacer(),
                            Text(
                              _car?.name ?? '默认车辆',
                              style: const TextStyle(
                                fontSize: 14,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  // 统计数据卡片
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
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
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(
                              Icons.analytics,
                              color: Color(0xFFFF3D00),
                            ),
                            const SizedBox(width: 8),
                            const Text(
                              '统计数据',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                        const Divider(height: 32, color: Colors.grey),

                        // 均速
                        _buildStatItem(
                          Icons.speed,
                          '均速',
                          _calculateAverageSpeed() != null
                              ? '${(_calculateAverageSpeed()! * 3.6).toStringAsFixed(2)} km/h'
                              : '--',
                        ),
                        const SizedBox(height: 16),

                        // 最大速度
                        _buildStatItem(
                          Icons.trending_up,
                          '最大速度',
                          _getMaxSpeed() != null
                              ? '${(_getMaxSpeed()! * 3.6).toStringAsFixed(2)} km/h'
                              : '--',
                        ),
                        const SizedBox(height: 16),

                        // 轨迹点数
                        _buildStatItem(
                          Icons.location_on,
                          '轨迹点数',
                          '${_points.length} 个',
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildStatItem(IconData icon, String label, String value) {
    return Row(
      children: [
        Icon(icon, color: const Color(0xFFFF3D00), size: 24),
        const SizedBox(width: 12),
        Text(label, style: const TextStyle(fontSize: 14, color: Colors.grey)),
        const Spacer(),
        Text(
          value,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
      ],
    );
  }
}
