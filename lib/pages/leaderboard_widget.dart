import 'package:flutter/material.dart';
import '../models/track_record.dart';
import '../models/car.dart';
import '../database/database_helper.dart';

/// 赛道圈速榜组件
class LeaderboardWidget extends StatefulWidget {
  final int trackId;

  const LeaderboardWidget({super.key, required this.trackId});

  @override
  State<LeaderboardWidget> createState() => _LeaderboardWidgetState();
}

class _LeaderboardWidgetState extends State<LeaderboardWidget> {
  final DatabaseHelper _db = DatabaseHelper.instance;
  List<TrackRecord> _records = [];
  final Map<int, Car?> _carCache = {};
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadLeaderboard();
  }

  Future<void> _loadLeaderboard() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final records = await _db.getTrackLeaderboard(widget.trackId);

      // 缓存车辆信息
      for (var record in records) {
        if (record.carId != null && !_carCache.containsKey(record.carId)) {
          final car = await _db.getCar(record.carId!);
          _carCache[record.carId!] = car;
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
        ).showSnackBar(SnackBar(content: Text('加载圈速榜失败: $e')));
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

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32),
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (_records.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.emoji_events, size: 64, color: Colors.grey[400]),
              const SizedBox(height: 16),
              Text(
                '暂无圈速记录',
                style: TextStyle(fontSize: 16, color: Colors.grey[600]),
              ),
              const SizedBox(height: 8),
              Text(
                '完成一次跟跑后即可查看圈速榜',
                style: TextStyle(fontSize: 14, color: Colors.grey[500]),
              ),
            ],
          ),
        ),
      );
    }

    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: _records.length,
      itemBuilder: (context, index) {
        final record = _records[index];
        final rank = index + 1;
        final car = record.carId != null ? _carCache[record.carId] : null;

        // 排名颜色
        Color rankColor;
        IconData? medalIcon;
        if (rank == 1) {
          rankColor = const Color(0xFFFFD700); // 金色
          medalIcon = Icons.emoji_events;
        } else if (rank == 2) {
          rankColor = const Color(0xFFC0C0C0); // 银色
          medalIcon = Icons.emoji_events;
        } else if (rank == 3) {
          rankColor = const Color(0xFFCD7F32); // 铜色
          medalIcon = Icons.emoji_events;
        } else {
          rankColor = Colors.grey[600]!;
        }

        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          child: ListTile(
            leading: Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: rankColor.withOpacity(0.2),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Center(
                child: medalIcon != null
                    ? Icon(medalIcon, color: rankColor, size: 24)
                    : Text(
                        '$rank',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: rankColor,
                        ),
                      ),
              ),
            ),
            title: Row(
              children: [
                Expanded(
                  child: Text(
                    _formatDuration(record.duration),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Text(
                  car?.name ?? '默认车辆',
                  style: TextStyle(fontSize: 14, color: Colors.grey[600]),
                ),
              ],
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.person, size: 14, color: Colors.grey[500]),
                    const SizedBox(width: 4),
                    Text(
                      'race',
                      style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                    ),
                    const SizedBox(width: 16),
                    Icon(Icons.access_time, size: 14, color: Colors.grey[500]),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(
                        _formatDateTime(record.endTime ?? record.startTime),
                        style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
