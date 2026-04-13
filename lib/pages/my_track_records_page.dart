import 'package:flutter/material.dart';
import '../models/track_record.dart';
import '../models/track.dart';
import '../models/car.dart';
import '../database/database_helper.dart';
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
      appBar: AppBar(
        title: const Text('我的轨迹'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _records.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.route, size: 80, color: Colors.grey[400]),
                  const SizedBox(height: 16),
                  Text(
                    '暂无轨迹记录',
                    style: TextStyle(fontSize: 18, color: Colors.grey[600]),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '完成一次跟跑后即可查看轨迹',
                    style: TextStyle(fontSize: 14, color: Colors.grey[500]),
                  ),
                ],
              ),
            )
          : RefreshIndicator(
              onRefresh: _loadRecords,
              child: ListView.builder(
                itemCount: _records.length,
                itemBuilder: (context, index) {
                  final record = _records[index];
                  final track = _trackCache[record.trackId];
                  final car = record.carId != null
                      ? _carCache[record.carId]
                      : null;

                  return Card(
                    margin: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    child: ListTile(
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) =>
                                TrackRecordDetailPage(record: record),
                          ),
                        );
                      },
                      title: Row(
                        children: [
                          Expanded(
                            child: Text(
                              track?.name ?? '未知赛道',
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(
                                _formatDuration(record.duration),
                                style: TextStyle(
                                  fontSize: 18,
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
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Icon(
                                Icons.person,
                                size: 14,
                                color: Colors.grey[500],
                              ),
                              const SizedBox(width: 4),
                              Text(
                                'race',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.grey[600],
                                ),
                              ),
                              const SizedBox(width: 16),
                              Icon(
                                Icons.directions_car,
                                size: 14,
                                color: Colors.grey[500],
                              ),
                              const SizedBox(width: 4),
                              Text(
                                car?.name ?? '默认车辆',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.grey[600],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Row(
                            children: [
                              Icon(
                                Icons.access_time,
                                size: 14,
                                color: Colors.grey[500],
                              ),
                              const SizedBox(width: 4),
                              Expanded(
                                child: Text(
                                  _formatDateTime(
                                    record.endTime ?? record.startTime,
                                  ),
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey[600],
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete, color: Colors.red),
                        onPressed: () => _deleteRecord(record.id!),
                        tooltip: '删除',
                      ),
                    ),
                  );
                },
              ),
            ),
    );
  }
}
