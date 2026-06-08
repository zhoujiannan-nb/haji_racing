class User {
  final int? id;
  final String username;
  final String? account;
  final String? email;
  final String? token;
  final String? role;
  final String createdAt;

  User({
    this.id,
    required this.username,
    this.account,
    this.email,
    this.token,
    this.role,
    required this.createdAt,
  });

  /// 判断用户是否已登录
  bool get isLoggedIn => token != null && token!.isNotEmpty;

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'username': username,
      'account': account,
      'email': email,
      'token': token,
      'role': role,
      'createdAt': createdAt,
    };
  }

  factory User.fromMap(Map<String, dynamic> map) {
    return User(
      id: map['id'],
      username: map['username'] ?? '',
      account: map['account'],
      email: map['email'],
      token: map['token'],
      role: map['role'],
      createdAt: map['createdAt'] ?? DateTime.now().toIso8601String(),
    );
  }

  /// 从登录响应创建用户对象
  factory User.fromLoginResponse(Map<String, dynamic> json) {
    return User(
      username: json['username'] ?? '',
      token: json['token'],
      role: json['role'],
      createdAt: DateTime.now().toIso8601String(),
    );
  }

  User copyWith({
    int? id,
    String? username,
    String? account,
    String? email,
    String? token,
    String? role,
    String? createdAt,
  }) {
    return User(
      id: id ?? this.id,
      username: username ?? this.username,
      account: account ?? this.account,
      email: email ?? this.email,
      token: token ?? this.token,
      role: role ?? this.role,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}
