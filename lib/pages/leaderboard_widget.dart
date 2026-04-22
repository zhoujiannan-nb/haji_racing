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
  List<TrackRecord> _allRecords = [];
  Map<int, Car?> _carCache = {};
  String _currentUsername = 'race';
  bool _isLoading = true;

  // PP值分组相关
  int _currentPpGroup = 0; // 当前选中的PP组别索引
  List<int> _ppGroups = []; // PP值分组列表
  Map<int, List<TrackRecord>> _groupedRecords = {}; // 按PP分组的记录
  int _currentPage = 0; // 当前页码
  static const int _pageSize = 20; // 每页显示20条记录

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
      // 获取当前用户
      final currentUser = await _db.getCurrentUser();
      if (currentUser != null) {
        _currentUsername = currentUser.username;
      }

      final records = await _db.getTrackLeaderboard(widget.trackId);

      // 缓存车辆信息并计算PP值
      for (var record in records) {
        if (record.carId != null && !_carCache.containsKey(record.carId)) {
          final car = await _db.getCar(record.carId!);
          _carCache[record.carId!] = car;
        }
      }

      // 按PP值分组
      _groupRecordsByPP(records);

      setState(() {
        _allRecords = records;
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

  /// 按PP值分组记录
  void _groupRecordsByPP(List<TrackRecord> records) {
    _groupedRecords.clear();
    _ppGroups.clear();

    for (var record in records) {
      final car = record.carId != null ? _carCache[record.carId] : null;
      final pp = car?.calculatePP() ?? 0;

      // 计算PP组别（每3500分为一组）
      final ppGroup = (pp / 3500).floor();

      if (!_groupedRecords.containsKey(ppGroup)) {
        _groupedRecords[ppGroup] = [];
        _ppGroups.add(ppGroup);
      }

      _groupedRecords[ppGroup]!.add(record);
    }

    // 对PP组进行排序
    _ppGroups.sort();

    // 对每个组内的记录按时间排序
    for (var group in _ppGroups) {
      _groupedRecords[group]!.sort(
        (a, b) => (a.duration ?? 0).compareTo(b.duration ?? 0),
      );
    }

    // 重置当前选中的组和页码
    if (_ppGroups.isNotEmpty) {
      _currentPpGroup = _ppGroups.first;
      _currentPage = 0;
    }
  }

  /// 获取当前选中组的记录
  List<TrackRecord> _getCurrentGroupRecords() {
    if (!_groupedRecords.containsKey(_currentPpGroup)) {
      return [];
    }
    return _groupedRecords[_currentPpGroup] ?? [];
  }

  /// 获取当前页的记录
  List<TrackRecord> _getCurrentPageRecords() {
    final groupRecords = _getCurrentGroupRecords();
    final startIndex = _currentPage * _pageSize;
    final endIndex = startIndex + _pageSize;

    if (startIndex >= groupRecords.length) {
      return [];
    }

    return groupRecords.sublist(
      startIndex,
      endIndex > groupRecords.length ? groupRecords.length : endIndex,
    );
  }

  /// 获取总页数
  int _getTotalPages() {
    final groupRecords = _getCurrentGroupRecords();
    return (groupRecords.length / _pageSize).ceil();
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

  /// 获取PP范围文本
  String _getPpRangeText(int ppGroup) {
    final minPP = ppGroup * 3500;
    final maxPP = minPP + 3499;
    return '$minPP-$maxPP';
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32),
          child: CircularProgressIndicator(color: Color(0xFFFF3D00)),
        ),
      );
    }

    if (_allRecords.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.emoji_events, size: 64, color: Colors.grey[700]),
              const SizedBox(height: 16),
              Text(
                '暂无圈速记录',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.grey[400],
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '完成一次跟跑后即可查看圈速榜',
                style: TextStyle(fontSize: 14, color: Colors.grey[600]),
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      children: [
        // PP组别选择器
        if (_ppGroups.isNotEmpty)
          Container(
            height: 50,
            margin: const EdgeInsets.only(bottom: 16),
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: _ppGroups.length,
              itemBuilder: (context, index) {
                final ppGroup = _ppGroups[index];
                final isSelected = ppGroup == _currentPpGroup;

                return GestureDetector(
                  onTap: () {
                    setState(() {
                      _currentPpGroup = ppGroup;
                      _currentPage = 0; // 切换组别时重置页码
                    });
                  },
                  child: Container(
                    margin: const EdgeInsets.symmetric(horizontal: 4),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    decoration: BoxDecoration(
                      color: isSelected
                          ? const Color(0xFFFF3D00)
                          : Colors.grey[850],
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: isSelected
                            ? const Color(0xFFFF3D00)
                            : Colors.grey[700]!,
                        width: 1,
                      ),
                    ),
                    child: Center(
                      child: Text(
                        _getPpRangeText(ppGroup),
                        style: TextStyle(
                          color: isSelected ? Colors.white : Colors.grey[400],
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),

        // 圈速记录列表 - 使用固定高度的Container
        Container(
          height: MediaQuery.of(context).size.height * 0.25, // 占用屏幕高度的25%
          child: _getCurrentPageRecords().isEmpty
              ? Center(
                  child: Text(
                    '该PP组别暂无记录',
                    style: TextStyle(color: Colors.grey[500]),
                  ),
                )
              : ListView.builder(
                  shrinkWrap: true,
                  physics: const BouncingScrollPhysics(),
                  itemCount: _getCurrentPageRecords().length,
                  itemBuilder: (context, index) {
                    final records = _getCurrentPageRecords();
                    final record = records[index];
                    // 计算全局排名：当前页之前的记录数 + 当前索引 + 1
                    final globalRank = _currentPage * _pageSize + index + 1;
                    final car = record.carId != null
                        ? _carCache[record.carId]
                        : null;

                    // 排名颜色
                    Color rankColor;
                    IconData? medalIcon;
                    if (globalRank == 1) {
                      rankColor = const Color(0xFFFFD700); // 金色
                      medalIcon = Icons.emoji_events;
                    } else if (globalRank == 2) {
                      rankColor = const Color(0xFFC0C0C0); // 银色
                      medalIcon = Icons.emoji_events;
                    } else if (globalRank == 3) {
                      rankColor = const Color(0xFFCD7F32); // 铜色
                      medalIcon = Icons.emoji_events;
                    } else {
                      rankColor = Colors.grey[500]!;
                    }

                    return Container(
                      margin: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 6,
                      ),
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
                                    '$globalRank',
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
                                  color: Colors.white,
                                ),
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  car?.name ?? '默认车辆',
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: Colors.grey[400],
                                  ),
                                ),
                                Text(
                                  'PP: ${car?.calculatePP().toStringAsFixed(0) ?? 'N/A'}',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey[600],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                Icon(
                                  Icons.person,
                                  size: 14,
                                  color: Colors.grey[500],
                                ),
                                const SizedBox(width: 4),
                                Text(
                                  _currentUsername,
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey[600],
                                  ),
                                ),
                                const SizedBox(width: 16),
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
                      ),
                    );
                  },
                ),
        ),

        // 分页控件
        if (_getTotalPages() > 1)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  onPressed: _currentPage > 0
                      ? () {
                          setState(() {
                            _currentPage--;
                          });
                        }
                      : null,
                  icon: const Icon(
                    Icons.chevron_left,
                    color: Color(0xFFFF3D00),
                  ),
                ),
                Text(
                  '第 ${_currentPage + 1} / ${_getTotalPages()} 页',
                  style: const TextStyle(fontSize: 14, color: Colors.white),
                ),
                IconButton(
                  onPressed: _currentPage < _getTotalPages() - 1
                      ? () {
                          setState(() {
                            _currentPage++;
                          });
                        }
                      : null,
                  icon: const Icon(
                    Icons.chevron_right,
                    color: Color(0xFFFF3D00),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
