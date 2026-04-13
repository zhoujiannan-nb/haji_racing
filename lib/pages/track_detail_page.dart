import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../models/track.dart';
import '../models/checkpoint.dart' as models;
import 'track_running_page.dart';
import 'leaderboard_widget.dart';

class TrackDetailPage extends StatefulWidget {
  final Track track;

  const TrackDetailPage({super.key, required this.track});

  @override
  State<TrackDetailPage> createState() => _TrackDetailPageState();
}

class _TrackDetailPageState extends State<TrackDetailPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.track.name),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 圈速榜标题
            Row(
              children: [
                const Icon(Icons.emoji_events, color: Color(0xFFFFD700)),
                const SizedBox(width: 8),
                const Text(
                  '赛道圈速榜',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // 圈速榜组件
            LeaderboardWidget(trackId: widget.track.id!),
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
                      '${(widget.track.length / 1000).toStringAsFixed(2)} km',
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
                      widget.track.description ?? '暂无简介',
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
                      builder: (context) =>
                          TrackRunningPage(track: widget.track),
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

/// 赛道绘制器
class TrackPainter extends CustomPainter {
  final Track track;

  TrackPainter({required this.track});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFF2196F3)
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke;

    final pointPaint = Paint()..style = PaintingStyle.fill;

    // 收集所有点
    final allPoints = <models.LatLng>[];
    final labels = <String, models.LatLng>{};

    if (track.startPolygon.isNotEmpty) {
      final center = _calculateCenter(track.startPolygon);
      allPoints.add(center);
      labels['起点'] = center;
    }

    if (track.endPolygon.isNotEmpty) {
      final center = _calculateCenter(track.endPolygon);
      allPoints.add(center);
      labels['终点'] = center;
    }

    if (track.checkPoints != null) {
      for (var i = 0; i < track.checkPoints!.length; i++) {
        final checkPoint = track.checkPoints![i];
        if (checkPoint.polygon.isNotEmpty) {
          final center = _calculateCenter(checkPoint.polygon);
          allPoints.add(center);
          labels[checkPoint.name ?? '检查点${i + 1}'] = center;
        }
      }
    }

    if (allPoints.isEmpty) return;

    // 计算边界
    double minLat = allPoints[0].latitude;
    double maxLat = allPoints[0].latitude;
    double minLng = allPoints[0].longitude;
    double maxLng = allPoints[0].longitude;

    for (var point in allPoints) {
      if (point.latitude < minLat) minLat = point.latitude;
      if (point.latitude > maxLat) maxLat = point.latitude;
      if (point.longitude < minLng) minLng = point.longitude;
      if (point.longitude > maxLng) maxLng = point.longitude;
    }

    final bounds = {
      'minLat': minLat,
      'maxLat': maxLat,
      'minLng': minLng,
      'maxLng': maxLng,
    };

    // 转换为画布坐标
    final padding = 20.0;
    final canvasPoints = allPoints
        .map((p) => _latLngToCanvas(p, bounds, size, padding))
        .toList();

    // 绘制路径线
    if (canvasPoints.length > 1) {
      final path = Path();
      path.moveTo(canvasPoints[0].dx, canvasPoints[0].dy);
      for (var i = 1; i < canvasPoints.length; i++) {
        path.lineTo(canvasPoints[i].dx, canvasPoints[i].dy);
      }
      canvas.drawPath(path, paint);
    }

    // 绘制点和标签
    for (var i = 0; i < canvasPoints.length; i++) {
      final point = canvasPoints[i];
      final label = labels.entries.elementAt(i).key;

      // 确定颜色
      Color color;
      if (label == '起点') {
        color = Colors.green;
      } else if (label == '终点') {
        color = Colors.red;
      } else {
        color = Colors.blue;
      }

      pointPaint.color = color;
      canvas.drawCircle(point, 6, pointPaint);

      // 绘制标签
      final textPainter = TextPainter(
        text: TextSpan(
          text: label,
          style: const TextStyle(
            color: Colors.black87,
            fontSize: 12,
            fontWeight: FontWeight.bold,
          ),
        ),
        textDirection: TextDirection.ltr,
      );
      textPainter.layout();
      textPainter.paint(canvas, Offset(point.dx + 10, point.dy - 20));
    }
  }

  @override
  bool shouldRepaint(covariant TrackPainter oldDelegate) {
    return oldDelegate.track != track;
  }

  /// 将经纬度转换为画布坐标
  Offset _latLngToCanvas(
    models.LatLng latLng,
    Map<String, dynamic> bounds,
    Size canvasSize,
    double padding,
  ) {
    final latRange = bounds['maxLat'] - bounds['minLat'];
    final lngRange = bounds['maxLng'] - bounds['minLng'];

    final availableWidth = canvasSize.width - 2 * padding;
    final availableHeight = canvasSize.height - 2 * padding;

    // 保持纵横比
    final scale = math.min(
      availableWidth / (lngRange > 0 ? lngRange : 1),
      availableHeight / (latRange > 0 ? latRange : 1),
    );

    final x = padding + (latLng.longitude - bounds['minLng']) * scale;
    final y = padding + (bounds['maxLat'] - latLng.latitude) * scale; // Y轴翻转

    return Offset(x, y);
  }

  models.LatLng _calculateCenter(List<models.LatLng> polygon) {
    if (polygon.isEmpty) {
      return models.LatLng(0, 0);
    }

    double sumLat = 0;
    double sumLng = 0;

    for (var point in polygon) {
      sumLat += point.latitude;
      sumLng += point.longitude;
    }

    return models.LatLng(sumLat / polygon.length, sumLng / polygon.length);
  }
}
