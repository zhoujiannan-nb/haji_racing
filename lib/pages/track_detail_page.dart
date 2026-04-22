import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../models/track.dart';
import '../models/checkpoint.dart' as models;
import '../models/car.dart';
import '../database/database_helper.dart';
import 'track_running_page.dart';
import 'leaderboard_widget.dart';

class TrackDetailPage extends StatefulWidget {
  final Track track;

  const TrackDetailPage({super.key, required this.track});

  @override
  State<TrackDetailPage> createState() => _TrackDetailPageState();
}

class _TrackDetailPageState extends State<TrackDetailPage> {
  final DatabaseHelper _db = DatabaseHelper.instance;
  Car? _currentCar;
  bool _isLoadingCar = true;

  @override
  void initState() {
    super.initState();
    _loadCurrentCar();
  }

  Future<void> _loadCurrentCar() async {
    try {
      final car = await _db.getMainCar();
      if (mounted) {
        setState(() {
          _currentCar = car;
          _isLoadingCar = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoadingCar = false;
        });
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
        title: Text(
          widget.track.name,
          style: const TextStyle(
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
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 当前车辆信息卡片
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
                        Icons.directions_car,
                        color: Color(0xFFFF3D00),
                      ),
                      const SizedBox(width: 8),
                      const Text(
                        '当前车辆',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  if (_isLoadingCar)
                    const Center(
                      child: CircularProgressIndicator(
                        color: Color(0xFFFF3D00),
                      ),
                    )
                  else if (_currentCar != null)
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _currentCar!.name,
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'PP值: ${_currentCar!.calculatePP().toStringAsFixed(0)}',
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.grey[400],
                              ),
                            ),
                          ],
                        ),
                        const Icon(
                          Icons.check_circle,
                          color: Color(0xFFFF3D00),
                          size: 28,
                        ),
                      ],
                    )
                  else
                    Text(
                      '暂无车辆',
                      style: TextStyle(fontSize: 16, color: Colors.grey[500]),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // 圈速榜标题
            Row(
              children: [
                const Icon(Icons.emoji_events, color: Color(0xFFFFD700)),
                const SizedBox(width: 8),
                const Text(
                  '我的圈速榜',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // 圈速榜组件
            LeaderboardWidget(trackId: widget.track.id!),
            const SizedBox(height: 24),

            // 赛道信息卡片
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
                      const Icon(Icons.straighten, color: Color(0xFFFF3D00)),
                      const SizedBox(width: 8),
                      Text(
                        '赛道长度',
                        style: TextStyle(fontSize: 14, color: Colors.grey[400]),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '${(widget.track.length / 1000).toStringAsFixed(2)} km',
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const Divider(height: 32, color: Colors.grey),

                  Row(
                    children: [
                      const Icon(Icons.description, color: Color(0xFFFF3D00)),
                      const SizedBox(width: 8),
                      Text(
                        '赛道简介',
                        style: TextStyle(fontSize: 14, color: Colors.grey[400]),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    widget.track.description ?? '暂无简介',
                    style: const TextStyle(fontSize: 16, color: Colors.white),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // 开始跟跑按钮
            SizedBox(
              width: double.infinity,
              height: 56,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) =>
                          TrackRunningPage(track: widget.track),
                    ),
                  );
                },
                icon: const Icon(Icons.play_arrow, size: 28),
                label: const Text(
                  '开始跟跑',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFF3D00),
                  foregroundColor: Colors.white,
                  elevation: 8,
                  shadowColor: const Color(0xFFFF3D00).withOpacity(0.4),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
