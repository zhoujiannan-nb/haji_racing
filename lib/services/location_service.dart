import 'dart:async';
import 'package:geolocator/geolocator.dart';
import 'package:permission_handler/permission_handler.dart';

/// 定位数据模型
class LocationData {
  final double latitude;
  final double longitude;
  final double? speed; // km/h
  final DateTime timestamp;

  LocationData({
    required this.latitude,
    required this.longitude,
    this.speed,
    required this.timestamp,
  });
}

/// 定位服务类
class LocationService {
  static final LocationService _instance = LocationService._internal();
  factory LocationService() => _instance;
  LocationService._internal();

  StreamSubscription<Position>? _positionSubscription;

  bool _isInitialized = false;
  bool _isListening = false;

  /// 初始化定位服务
  Future<void> initialize() async {
    if (_isInitialized) return;

    // 请求定位权限
    final status = await Permission.location.request();
    if (!status.isGranted) {
      throw Exception('定位权限被拒绝');
    }

    // 检查定位服务是否启用
    final isEnabled = await Geolocator.isLocationServiceEnabled();
    if (!isEnabled) {
      throw Exception('定位服务未启用，请在设置中开启');
    }

    _isInitialized = true;
  }

  /// 开始连续定位
  Stream<LocationData> startContinuousLocation() async* {
    if (!_isInitialized) {
      await initialize();
    }

    if (_isListening) {
      throw Exception('定位服务已在运行');
    }

    _isListening = true;

    // 设置定位选项
    const locationSettings = LocationSettings(
      accuracy: LocationAccuracy.best,
      distanceFilter: 0,
    );

    try {
      await for (final position in Geolocator.getPositionStream(
        locationSettings: locationSettings,
      )) {
        yield LocationData(
          latitude: position.latitude,
          longitude: position.longitude,
          speed: position.speed * 3.6, // m/s 转 km/h
          timestamp: position.timestamp,
        );
      }
    } catch (e) {
      throw Exception('定位失败: $e');
    } finally {
      _isListening = false;
    }
  }

  /// 获取单次定位
  Future<LocationData> getCurrentLocation() async {
    if (!_isInitialized) {
      await initialize();
    }

    try {
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.best,
      );

      return LocationData(
        latitude: position.latitude,
        longitude: position.longitude,
        speed: position.speed * 3.6, // m/s 转 km/h
        timestamp: position.timestamp,
      );
    } catch (e) {
      throw Exception('获取定位失败: $e');
    }
  }

  /// 停止定位
  Future<void> stopLocation() async {
    if (!_isListening) return;

    await _positionSubscription?.cancel();
    _isListening = false;
  }

  /// 销毁服务
  Future<void> dispose() async {
    await stopLocation();
    _isInitialized = false;
  }

  /// 是否正在监听
  bool get isListening => _isListening;
}
