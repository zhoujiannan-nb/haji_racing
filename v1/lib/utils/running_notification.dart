import 'package:flutter/material.dart';

/// 跑步状态通知工具类
/// 用于在锁屏/后台时提醒用户跑步记录仍在进行
class RunningNotification {
  static final RunningNotification _instance = RunningNotification._internal();
  factory RunningNotification() => _instance;
  RunningNotification._internal();

  bool _isShowing = false;

  /// 显示跑步中的提示（使用 SnackBar 或 Dialog）
  void showRunningNotification(BuildContext context) {
    if (_isShowing) return;

    _isShowing = true;

    // 显示持久化的 SnackBar 提示
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(Icons.run_circle, color: Colors.white),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '跟跑进行中',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '屏幕已保持常亮，请勿手动锁屏',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.white.withOpacity(0.9),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        backgroundColor: const Color(0xFF2196F3),
        duration: const Duration(hours: 1), // 长时间显示
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(8),
        action: SnackBarAction(
          label: '知道了',
          textColor: Colors.white,
          onPressed: () {
            _isShowing = false;
          },
        ),
      ),
    );
  }

  /// 隐藏通知
  void hideNotification(BuildContext context) {
    if (!_isShowing) return;

    _isShowing = false;
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
  }

  /// 是否正在显示
  bool get isShowing => _isShowing;
}
