class User {
  final int? id;
  final String username;
  final String createdAt;

  User({this.id, required this.username, required this.createdAt});

  Map<String, dynamic> toMap() {
    return {'id': id, 'username': username, 'createdAt': createdAt};
  }

  factory User.fromMap(Map<String, dynamic> map) {
    return User(
      id: map['id'],
      username: map['username'],
      createdAt: map['createdAt'],
    );
  }

  User copyWith({int? id, String? username, String? createdAt}) {
    return User(
      id: id ?? this.id,
      username: username ?? this.username,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}
