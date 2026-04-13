import 'package:flutter_test/flutter_test.dart';
import '../lib/models/checkpoint.dart';
import '../lib/utils/location_utils.dart';

void main() {
  group('LocationUtils - 射线法多边形判断', () {
    test('点在多边形内部', () {
      // 创建一个正方形多边形
      final polygon = [
        LatLng(30.0, 120.0),
        LatLng(30.0, 120.1),
        LatLng(30.1, 120.1),
        LatLng(30.1, 120.0),
      ];

      // 测试中心点
      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.05,
          pointLon: 120.05,
          polygon: polygon,
        ),
        isTrue,
      );
    });

    test('点在多边形外部', () {
      final polygon = [
        LatLng(30.0, 120.0),
        LatLng(30.0, 120.1),
        LatLng(30.1, 120.1),
        LatLng(30.1, 120.0),
      ];

      // 测试外部点
      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.2,
          pointLon: 120.2,
          polygon: polygon,
        ),
        isFalse,
      );
    });

    test('点在多边形边上', () {
      final polygon = [
        LatLng(30.0, 120.0),
        LatLng(30.0, 120.1),
        LatLng(30.1, 120.1),
        LatLng(30.1, 120.0),
      ];

      // 测试边上的点（应该在多边形内）
      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.0,
          pointLon: 120.05,
          polygon: polygon,
        ),
        isTrue,
      );
    });

    test('点在多边形顶点上', () {
      final polygon = [
        LatLng(30.0, 120.0),
        LatLng(30.0, 120.1),
        LatLng(30.1, 120.1),
        LatLng(30.1, 120.0),
      ];

      // 测试顶点（应该在多边形内）
      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.0,
          pointLon: 120.0,
          polygon: polygon,
        ),
        isTrue,
      );
    });

    test('不规则多边形内部点', () {
      // 创建一个L形多边形
      final polygon = [
        LatLng(30.0, 120.0),
        LatLng(30.0, 120.2),
        LatLng(30.1, 120.2),
        LatLng(30.1, 120.1),
        LatLng(30.2, 120.1),
        LatLng(30.2, 120.0),
      ];

      // 测试L形内部的点
      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.05,
          pointLon: 120.1,
          polygon: polygon,
        ),
        isTrue,
      );

      // 测试L形外部的点（缺口处）
      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.15,
          pointLon: 120.15,
          polygon: polygon,
        ),
        isFalse,
      );
    });

    test('点数不足的多边形', () {
      final polygon = [LatLng(30.0, 120.0), LatLng(30.0, 120.1)];

      // 少于3个点应该返回false
      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.0,
          pointLon: 120.05,
          polygon: polygon,
        ),
        isFalse,
      );
    });

    test('空多边形', () {
      final polygon = <LatLng>[];

      expect(
        LocationUtils.isPointInPolygon(
          pointLat: 30.0,
          pointLon: 120.0,
          polygon: polygon,
        ),
        isFalse,
      );
    });
  });

  group('LocationUtils - 多边形中心计算', () {
    test('计算正方形中心', () {
      final polygon = [
        LatLng(30.0, 120.0),
        LatLng(30.0, 120.2),
        LatLng(30.2, 120.2),
        LatLng(30.2, 120.0),
      ];

      final center = LocationUtils.calculatePolygonCenter(polygon);

      expect(center.latitude, closeTo(30.1, 0.001));
      expect(center.longitude, closeTo(120.1, 0.001));
    });
  });

  group('LocationUtils - 距离计算', () {
    test('计算两点间距离', () {
      // 大约1度纬度差约111km
      final distance = LocationUtils.calculateDistance(
        30.0,
        120.0,
        31.0,
        120.0,
      );

      // 应该在110-112km之间
      expect(distance, greaterThan(110000));
      expect(distance, lessThan(112000));
    });

    test('相同点距离为0', () {
      final distance = LocationUtils.calculateDistance(
        30.0,
        120.0,
        30.0,
        120.0,
      );

      expect(distance, equals(0.0));
    });
  });
}
