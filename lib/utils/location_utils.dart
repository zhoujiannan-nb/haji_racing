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

  /// 计算点到多边形中心的距离，并判断是否在多边形范围内
  /// 这里简化处理：使用多边形外接圆半径作为判断依据
  static bool isPointInPolygon({
    required double pointLat,
    required double pointLon,
    required List<LatLng> polygon,
  }) {
    if (polygon.isEmpty) {
      return false;
    }

    // 计算多边形中心
    final center = calculatePolygonCenter(polygon);

    // 计算多边形最大半径（从中心到最远顶点的距离）
    double maxRadius = 0;
    for (final vertex in polygon) {
      final distance = calculateDistance(
        center.latitude,
        center.longitude,
        vertex.latitude,
        vertex.longitude,
      );
      if (distance > maxRadius) {
        maxRadius = distance;
      }
    }

    // 判断当前点是否在多边形外接圆内
    final distanceToCenter = calculateDistance(
      pointLat,
      pointLon,
      center.latitude,
      center.longitude,
    );

    return distanceToCenter <= maxRadius;
  }
}
