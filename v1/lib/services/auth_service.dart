import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';
import '../database/database_helper.dart';

class AuthService {
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  static const String baseUrl = 'https://haji-racing.online:8443';

  User? _currentUser;
  User? get currentUser => _currentUser;

  /// 初始化服务，从本地存储加载用户信息
  Future<void> init() async {
    await _loadUserFromStorage();
  }

  /// 从本地存储加载用户信息
  Future<void> _loadUserFromStorage() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final userJson = prefs.getString('current_user');
      if (userJson != null) {
        final userMap = jsonDecode(userJson);
        _currentUser = User.fromMap(userMap);
      }
    } catch (e) {
      print('加载用户信息失败: $e');
    }
  }

  /// 保存用户信息到本地存储
  Future<void> _saveUserToStorage(User user) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final userJson = jsonEncode(user.toMap());
      await prefs.setString('current_user', userJson);
    } catch (e) {
      print('保存用户信息失败: $e');
    }
  }

  /// 从本地存储清除用户信息
  Future<void> _clearUserFromStorage() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('current_user');

      // 清除数据库中的token
      if (_currentUser?.id != null) {
        await DatabaseHelper.instance.updateUserToken(_currentUser!.id!, null);
      }
    } catch (e) {
      print('清除用户信息失败: $e');
    }
  }

  /// 用户登录
  Future<Map<String, dynamic>> login(String account, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'account': account, 'password': password}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        _currentUser = User.fromLoginResponse(data);
        await _saveUserToStorage(_currentUser!);

        // 保存用户信息到数据库
        _currentUser = await DatabaseHelper.instance.saveUser(_currentUser!);

        // 强制将所有轨迹记录转移给当前登录用户
        debugPrint('正在转移所有轨迹记录给用户 ${_currentUser!.id}');
        if (_currentUser!.id != null) {
          await DatabaseHelper.instance.transferAllRecordsToUser(
            _currentUser!.id!,
          );
        }

        return {'success': true, 'message': '登录成功'};
      } else {
        final error = jsonDecode(response.body);
        return {'success': false, 'message': error['message'] ?? '登录失败'};
      }
    } catch (e) {
      return {'success': false, 'message': '网络错误: $e'};
    }
  }

  /// 用户注册
  Future<Map<String, dynamic>> register({
    required String email,
    required String account,
    required String password,
    required String username,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'account': account,
          'password': password,
          'username': username,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final data = jsonDecode(response.body);
        return {'success': true, 'message': data['message'] ?? '注册成功'};
      } else {
        final error = jsonDecode(response.body);
        return {'success': false, 'message': error['message'] ?? '注册失败'};
      }
    } catch (e) {
      return {'success': false, 'message': '网络错误: $e'};
    }
  }

  /// 退出登录
  Future<void> logout() async {
    _currentUser = null;
    await _clearUserFromStorage();
  }

  /// 设置为游客模式
  void setGuestMode() {
    _currentUser = User(
      username: '游客',
      createdAt: DateTime.now().toIso8601String(),
    );
  }

  /// 获取认证头（用于其他接口调用）
  Map<String, String>? getAuthHeaders() {
    if (_currentUser?.token != null) {
      return {
        'Authorization': 'Bearer ${_currentUser!.token}',
        'Content-Type': 'application/json',
      };
    }
    return null;
  }
}
