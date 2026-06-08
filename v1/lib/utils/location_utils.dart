import 'dart:math';
import '../models/checkpoint.dart';

/// 地理位置工具类
class LocationUtils {
  /// 地球半径（米）
  static const double earthRadius = 6371000;

  // ==================== 坐标系转换相关常量 ====================
  static const double pi = 3.1415926535897932384626;
  static const double a = 6378245.0;
  static const double ee = 0.00669342162296594323;

  /// WGS-84 转 GCJ-02（火星坐标系）
  /// 用于将 GPS 原始坐标转换为高德/腾讯地图使用的坐标
  static LatLng wgs84ToGcj02(double lat, double lon) {
    if (_outOfChina(lat, lon)) {
      return LatLng(lat, lon);
    }

    final dlat = _transformLat(lon - 105.0, lat - 35.0);
    final dlon = _transformLon(lon - 105.0, lat - 35.0);
    final radlat = lat / 180.0 * pi;
    var magic = sin(radlat);
    magic = 1 - ee * magic * magic;
    final sqrtmagic = sqrt(magic);
    final mglat =
        lat + (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi);
    final mglon = lon + (dlon * 180.0) / (a / sqrtmagic * cos(radlat) * pi);

    return LatLng(mglat, mglon);
  }

  /// GCJ-02 转 WGS-84
  /// 用于将高德/腾讯地图坐标转换为 GPS 原始坐标
  static LatLng gcj02ToWgs84(double lat, double lon) {
    if (_outOfChina(lat, lon)) {
      return LatLng(lat, lon);
    }

    final dlat = _transformLat(lon - 105.0, lat - 35.0);
    final dlon = _transformLon(lon - 105.0, lat - 35.0);
    final radlat = lat / 180.0 * pi;
    var magic = sin(radlat);
    magic = 1 - ee * magic * magic;
    final sqrtmagic = sqrt(magic);
    final mglat =
        lat + (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi);
    final mglon = lon + (dlon * 180.0) / (a / sqrtmagic * cos(radlat) * pi);

    return LatLng(lat * 2 - mglat, lon * 2 - mglon);
  }

  /// 判断是否在中国境外
  static bool _outOfChina(double lat, double lon) {
    if (lon < 72.004 || lon > 137.8347) return true;
    if (lat < 0.8293 || lat > 55.8271) return true;
    return false;
  }

  static double _transformLat(double x, double y) {
    var ret =
        -100.0 +
        2.0 * x +
        3.0 * y +
        0.2 * y * y +
        0.1 * x * y +
        0.2 * sqrt(x.abs());
    ret += (20.0 * sin(6.0 * x * pi) + 20.0 * sin(2.0 * x * pi)) * 2.0 / 3.0;
    ret += (20.0 * sin(y * pi) + 40.0 * sin(y / 3.0 * pi)) * 2.0 / 3.0;
    ret += (160.0 * sin(y / 12.0 * pi) + 320 * sin(y * pi / 30.0)) * 2.0 / 3.0;
    return ret;
  }

  static double _transformLon(double x, double y) {
    var ret =
        300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(x.abs());
    ret += (20.0 * sin(6.0 * x * pi) + 20.0 * sin(2.0 * x * pi)) * 2.0 / 3.0;
    ret += (20.0 * sin(x * pi) + 40.0 * sin(x / 3.0 * pi)) * 2.0 / 3.0;
    ret +=
        (150.0 * sin(x / 12.0 * pi) + 300.0 * sin(x / 30.0 * pi)) * 2.0 / 3.0;
    return ret;
  }

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
    // 计算线段的长度平方（使用经纬度差值）
    final dx = x2 - x1;
    final dy = y2 - y1;
    final l2 = dx * dx + dy * dy;

    // 如果线段长度为0，返回点到端点的距离
    if (l2 == 0) {
      return calculateDistance(pointLat, pointLon, x1, y1);
    }

    // 计算投影参数 t
    final t = ((pointLat - x1) * dx + (pointLon - y1) * dy) / l2;

    // 限制 t 在 [0, 1] 范围内
    final tClamped = t.clamp(0.0, 1.0);

    // 计算线段上最近的点
    final closestLat = x1 + tClamped * dx;
    final closestLon = y1 + tClamped * dy;

    // 返回点到最近点的距离（使用Haversine公式精确计算）
    return calculateDistance(pointLat, pointLon, closestLat, closestLon);
  }
}
