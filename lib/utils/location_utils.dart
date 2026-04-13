import 'dart:math';

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
}
