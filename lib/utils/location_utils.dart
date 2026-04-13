import 'dart:math';
import '../models/checkpoint.dart';

/// 地理位置工具类
class LocationUtils {
  /// 地球半径（米）
  static const double earthRadius = 6371000;

  /// 计算两点之间的距离（单位：米）
  /// 使用 Haversine 公式
  static double calculateDistance(
    double lat1,
    double lon1,
    double lat2,
    double lon2,
  ) {
    final dLat = _toRadians(lat2 - lat1);
    final dLon = _toRadians(lon2 - lon1);

    final a =
        sin(dLat / 2) * sin(dLat / 2) +
        cos(_toRadians(lat1)) *
            cos(_toRadians(lat2)) *
            sin(dLon / 2) *
            sin(dLon / 2);

    final c = 2 * atan2(sqrt(a), sqrt(1 - a));

    return earthRadius * c;
  }

  /// 判断点是否在圆形电子围栏内
  /// 返回距离（米），如果 <= radius 则在围栏内
  static bool isPointInCircle({
    required double pointLat,
    required double pointLon,
    required double centerLat,
    required double centerLon,
    required double radius,
  }) {
    final distance = calculateDistance(
      pointLat,
      pointLon,
      centerLat,
      centerLon,
    );
    return distance <= radius;
  }

  /// 获取点到圆心的距离
  static double getDistanceToCenter({
    required double pointLat,
    required double pointLon,
    required double centerLat,
    required double centerLon,
  }) {
    return calculateDistance(pointLat, pointLon, centerLat, centerLon);
  }

  /// 角度转弧度
  static double _toRadians(double degrees) {
    return degrees * pi / 180;
  }

  /// 格式化时间为 MM:SS.ms
  static String formatDuration(double seconds) {
    final minutes = (seconds / 60).floor();
    final secs = (seconds % 60).floor();
    final ms = ((seconds % 1) * 100).floor();

    return '${minutes.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}.${ms.toString().padLeft(2, '0')}';
  }

  /// 使用射线法判断点是否在多边形内（包括边界上）
  /// 适用于首尾相连的不交叉多边形
  static bool isPointInPolygon({
    required double pointLat,
    required double pointLon,
    required List<LatLng> polygon,
  }) {
    if (polygon.length < 3) {
      return false;
    }

    int n = polygon.length;
    bool inside = false;

    // 射线法：从点向右发射一条水平射线，计算与多边形边的交点数
    // 奇数个交点表示在内部，偶数个表示在外部
    for (int i = 0, j = n - 1; i < n; j = i++) {
      double xi = polygon[i].latitude;
      double yi = polygon[i].longitude;
      double xj = polygon[j].latitude;
      double yj = polygon[j].longitude;

      // 检查点是否在边上（包括顶点）
      if (_isPointOnSegment(pointLat, pointLon, xi, yi, xj, yj)) {
        return true;
      }

      // 判断射线是否与边相交
      bool intersect =
          ((yi > pointLon) != (yj > pointLon)) &&
          (pointLat < (xj - xi) * (pointLon - yi) / (yj - yi) + xi);

      if (intersect) {
        inside = !inside;
      }
    }

    return inside;
  }

  /// 判断点是否在线段上（包括端点）
  static bool _isPointOnSegment(
    double px,
    double py,
    double x1,
    double y1,
    double x2,
    double y2,
  ) {
    // 首先检查点是否在线段的包围盒内
    if (px < min(x1, x2) ||
        px > max(x1, x2) ||
        py < min(y1, y2) ||
        py > max(y1, y2)) {
      return false;
    }

    // 检查点是否在直线上（使用叉积）
    // 如果叉积接近0，则点在直线上
    double crossProduct = (py - y1) * (x2 - x1) - (px - x1) * (y2 - y1);
    return crossProduct.abs() < 1e-9;
  }

  /// 计算多边形的中心点（质心）
  static LatLng calculatePolygonCenter(List<LatLng> polygon) {
    if (polygon.isEmpty) {
      throw ArgumentError('多边形不能为空');
    }

    double sumLat = 0;
    double sumLon = 0;

    for (final point in polygon) {
      sumLat += point.latitude;
      sumLon += point.longitude;
    }

    final centerLat = sumLat / polygon.length;
    final centerLon = sumLon / polygon.length;

    return LatLng(centerLat, centerLon);
  }

  /// 计算点到多边形的最短距离（单位：米）
  /// 返回点到多边形边界上最近点的距离
  static double getDistanceToPolygon({
    required double pointLat,
    required double pointLon,
    required List<LatLng> polygon,
  }) {
    if (polygon.isEmpty) {
      return double.infinity;
    }

    // 如果点在多边形内部，距离为0
    if (isPointInPolygon(
      pointLat: pointLat,
      pointLon: pointLon,
      polygon: polygon,
    )) {
      return 0;
    }

    double minDistance = double.infinity;

    // 遍历多边形的每条边，计算点到线段的最短距离
    for (int i = 0; i < polygon.length; i++) {
      final p1 = polygon[i];
      final p2 = polygon[(i + 1) % polygon.length];

      final distance = _getDistanceToSegment(
        pointLat,
        pointLon,
        p1.latitude,
        p1.longitude,
        p2.latitude,
        p2.longitude,
      );

      if (distance < minDistance) {
        minDistance = distance;
      }
    }

    return minDistance;
  }

  /// 计算点到线段的最短距离（单位：米）
  static double _getDistanceToSegment(
    double pointLat,
    double pointLon,
    double x1,
    double y1,
    double x2,
    double y2,
  ) {
    // 将经纬度转换为平面坐标进行计算（小范围内近似）
    // 计算线段的长度平方
    final l2 = _squaredDistance(x1, y1, x2, y2);

    // 如果线段长度为0，返回点到端点的距离
    if (l2 == 0) {
      return calculateDistance(pointLat, pointLon, x1, y1);
    }

    // 计算投影参数 t
    final t = ((pointLat - x1) * (x2 - x1) + (pointLon - y1) * (y2 - y1)) / l2;

    // 限制 t 在 [0, 1] 范围内
    final tClamped = t.clamp(0.0, 1.0);

    // 计算线段上最近的点
    final closestLat = x1 + tClamped * (x2 - x1);
    final closestLon = y1 + tClamped * (y2 - y1);

    // 返回点到最近点的距离
    return calculateDistance(pointLat, pointLon, closestLat, closestLon);
  }

  /// 计算两点之间的平方距离（用于快速比较）
  static double _squaredDistance(double x1, double y1, double x2, double y2) {
    final dx = x2 - x1;
    final dy = y2 - y1;
    return dx * dx + dy * dy;
  }
}
