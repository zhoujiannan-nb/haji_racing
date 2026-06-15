import 'package:flutter/material.dart';
import '../services/auth_service.dart';

class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _accountController = TextEditingController();
  final _passwordController = TextEditingController();
  final _usernameController = TextEditingController();
  final _authService = AuthService();

  bool _isLoading = false;
  bool _obscurePassword = true;

  @override
  void dispose() {
    _emailController.dispose();
    _accountController.dispose();
    _passwordController.dispose();
    _usernameController.dispose();
    super.dispose();
  }

  Future<void> _handleRegister() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    final result = await _authService.register(
      email: _emailController.text.trim(),
      account: _accountController.text.trim(),
      password: _passwordController.text,
      username: _usernameController.text.trim(),
    );

    setState(() => _isLoading = false);

    if (mounted) {
      if (result['success']) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result['message']),
            backgroundColor: Colors.green,
          ),
        );
        // 注册成功后返回登录页
        Navigator.of(context).pop();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result['message']),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A1A1A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1A1A1A),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          '注册',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 24,
          ),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 30),

                // 标题
                const Text(
                  '创建新账号',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  '填写以下信息完成注册',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 14, color: Colors.grey[500]),
                ),

                const SizedBox(height: 40),

                // 用户名输入框
                TextFormField(
                  controller: _usernameController,
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(
                    labelText: '用户名',
                    hintText: '请输入用户名',
                    labelStyle: TextStyle(color: Colors.grey[400]),
                    hintStyle: TextStyle(color: Colors.grey[600]),
                    prefixIcon: const Icon(
                      Icons.person_outline,
                      color: Color(0xFFFF3D00),
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: const BorderSide(color: Color(0xFFFF3D00)),
                    ),
                    filled: true,
                    fillColor: Colors.grey[900],
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return '请输入用户名';
                    }
                    return null;
                  },
                ),

                const SizedBox(height: 20),

                // 邮箱输入框
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(
                    labelText: '邮箱',
                    hintText: '请输入邮箱地址',
                    labelStyle: TextStyle(color: Colors.grey[400]),
                    hintStyle: TextStyle(color: Colors.grey[600]),
                    prefixIcon: const Icon(
                      Icons.email,
                      color: Color(0xFFFF3D00),
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: const BorderSide(color: Color(0xFFFF3D00)),
                    ),
                    filled: true,
                    fillColor: Colors.grey[900],
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return '请输入邮箱';
                    }
                    if (!RegExp(
                      r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$',
                    ).hasMatch(value)) {
                      return '请输入有效的邮箱地址';
                    }
                    return null;
                  },
                ),

                const SizedBox(height: 20),

                // 账号输入框
                TextFormField(
                  controller: _accountController,
                  keyboardType: TextInputType.number,
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(
                    labelText: '账号',
                    hintText: '请输入6-20位数字',
                    labelStyle: TextStyle(color: Colors.grey[400]),
                    hintStyle: TextStyle(color: Colors.grey[600]),
                    prefixIcon: const Icon(
                      Icons.badge,
                      color: Color(0xFFFF3D00),
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: const BorderSide(color: Color(0xFFFF3D00)),
                    ),
                    filled: true,
                    fillColor: Colors.grey[900],
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return '请输入账号';
                    }
                    if (!RegExp(r'^\d{6,20}$').hasMatch(value)) {
                      return '账号必须是6-20位数字';
                    }
                    return null;
                  },
                ),

                const SizedBox(height: 20),

                // 密码输入框
                TextFormField(
                  controller: _passwordController,
                  obscureText: _obscurePassword,
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(
                    labelText: '密码',
                    hintText: '请设置密码',
                    labelStyle: TextStyle(color: Colors.grey[400]),
                    hintStyle: TextStyle(color: Colors.grey[600]),
                    prefixIcon: const Icon(
                      Icons.lock,
                      color: Color(0xFFFF3D00),
                    ),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _obscurePassword
                            ? Icons.visibility
                            : Icons.visibility_off,
                        color: Colors.grey[400],
                      ),
                      onPressed: () {
                        setState(() {
                          _obscurePassword = !_obscurePassword;
                        });
                      },
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide(color: Colors.grey[700]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: const BorderSide(color: Color(0xFFFF3D00)),
                    ),
                    filled: true,
                    fillColor: Colors.grey[900],
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return '请输入密码';
                    }
                    if (value.length < 6) {
                      return '密码至少6位';
                    }
                    return null;
                  },
                ),

                const SizedBox(height: 40),

                // 注册按钮
                ElevatedButton(
                  onPressed: _isLoading ? null : _handleRegister,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFF3D00),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor: AlwaysStoppedAnimation<Color>(
                              Colors.white,
                            ),
                          ),
                        )
                      : const Text(
                          '注册',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                ),

                const SizedBox(height: 15),

                // 已有账号？去登录
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: Text(
                    '已有账号？立即登录',
                    style: TextStyle(fontSize: 14, color: Colors.grey[500]),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
