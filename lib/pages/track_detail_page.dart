import 'package:flutter/material.dart';
import '../models/track.dart';
import 'track_running_page.dart';

class TrackDetailPage extends StatelessWidget {
  final Track track;

  const TrackDetailPage({super.key, required this.track});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(track.name),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 赛道缩略图占位（后期使用高德静态图API）
            Container(
              width: double.infinity,
              height: 200,
              decoration: BoxDecoration(
                color: Colors.grey[300],
                borderRadius: BorderRadius.circular(8),
              ),
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.map, size: 48, color: Colors.grey[600]),
                    const SizedBox(height: 8),
                    Text('赛道地图', style: TextStyle(color: Colors.grey[600])),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // 赛道信息卡片
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.straighten, color: Colors.blue),
                        const SizedBox(width: 8),
                        Text(
                          '赛道长度',
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${(track.length / 1000).toStringAsFixed(2)} km',
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Divider(height: 32),

                    Row(
                      children: [
                        const Icon(Icons.description, color: Colors.blue),
                        const SizedBox(width: 8),
                        Text(
                          '赛道简介',
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      track.description ?? '暂无简介',
                      style: const TextStyle(fontSize: 16),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // 开始跟跑按钮
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => TrackRunningPage(track: track),
                    ),
                  );
                },
                icon: const Icon(Icons.play_arrow),
                label: const Text('开始跟跑', style: TextStyle(fontSize: 18)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).colorScheme.primary,
                  foregroundColor: Colors.white,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
