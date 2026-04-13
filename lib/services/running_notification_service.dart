import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// 跑步状态通知服务 - 实现类似 QQ 音乐的锁屏控制界面
class RunningNotificationService {
  static final RunningNotificationService _instance =
      RunningNotificationService._internal();
  factory RunningNotificationService() => _instance;
  RunningNotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();
  bool _isInitialized = false;

  // 通知 ID
  static const int _runningNotificationId = 1;

  /// 初始化通知服务
  Future<void> initialize() async {
    if (_isInitialized) return;

    // Android 初始化配置
    const androidSettings = AndroidInitializationSettings(
      '@mipmap/ic_launcher',
    );

    // iOS 初始化配置
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    const initializationSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _notificationsPlugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );

    _isInitialized = true;
  }

  /// 显示跑步中的通知（带计时器和控制按钮）
  Future<void> showRunningNotification({
    required String elapsedTime,
    required double distance,
    required double speed,
  }) async {
    if (!_isInitialized) await initialize();

    // Android 通知配置
    const androidChannel = AndroidNotificationChannel(
      'running_channel',
      '跑步状态',
      description: '显示跑步过程中的实时状态',
      importance: Importance.high,
    );

    await _notificationsPlugin
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >()
        ?.createNotificationChannel(androidChannel);

    final androidDetails = AndroidNotificationDetails(
      'running_channel',
      '跑步状态',
      channelDescription: '显示跑步过程中的实时状态',
      importance: Importance.high,
      priority: Priority.high,
      ongoing: true, // 持续通知，不能被滑动清除
      autoCancel: false,
      showWhen: true,
      when: DateTime.now().millisecondsSinceEpoch,
      styleInformation: BigTextStyleInformation(
        '',
        contentTitle: '🏃 跟跑进行中',
        summaryText: '点击查看详细信息',
      ),
      // 添加操作按钮
      actions: <AndroidNotificationAction>[
        AndroidNotificationAction('pause', '暂停', showsUserInterface: false),
        AndroidNotificationAction('stop', '停止', showsUserInterface: false),
      ],
    );

    // iOS 通知配置
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: false,
      badgeNumber: 1,
    );

    final notificationDetails = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    // 构建通知内容
    final title = '🏃 跟跑中';
    final body =
        '时间: $elapsedTime\n距离: ${distance.toStringAsFixed(2)} km\n速度: ${speed.toStringAsFixed(1)} km/h';

    await _notificationsPlugin.show(
      _runningNotificationId,
      title,
      body,
      notificationDetails,
      payload: 'running',
    );
  }

  /// 更新通知内容（用于更新计时器）
  Future<void> updateRunningNotification({
    required String elapsedTime,
    required double distance,
    required double speed,
  }) async {
    await showRunningNotification(
      elapsedTime: elapsedTime,
      distance: distance,
      speed: speed,
    );
  }

  /// 隐藏通知
  Future<void> hideNotification() async {
    await _notificationsPlugin.cancel(_runningNotificationId);
  }

  /// 通知点击事件处理
  void _onNotificationTapped(NotificationResponse response) {
    // 点击通知时可以在这里处理逻辑
    // 例如：返回应用、暂停/继续等
    debugPrint('通知被点击: ${response.payload}');
  }

  /// 请求通知权限
  Future<bool> requestPermissions() async {
    if (!_isInitialized) await initialize();

    // 请求 Android 通知权限
    final androidImplementation = _notificationsPlugin
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >();

    if (androidImplementation != null) {
      final granted = await androidImplementation
          .requestNotificationsPermission();
      return granted ?? false;
    }

    return true;
  }

  /// 销毁服务
  Future<void> dispose() async {
    await hideNotification();
  }
}
