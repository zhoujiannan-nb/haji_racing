class Track {
  final int? id;
  final String name;
  final String? description;
  final double length;
  final double startLatitude;
  final double startLongitude;
  final double startRadius;
  final double endLatitude;
  final double endLongitude;
  final double endRadius;
  final String? thumbnailUrl;
  final String publishedAt;
  final String createdAt;

  Track({
    this.id,
    required this.name,
    this.description,
    required this.length,
    required this.startLatitude,
    required this.startLongitude,
    this.startRadius = 50.0,
    required this.endLatitude,
    required this.endLongitude,
    this.endRadius = 50.0,
    this.thumbnailUrl,
    required this.publishedAt,
    required this.createdAt,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'length': length,
      'startLatitude': startLatitude,
      'startLongitude': startLongitude,
      'startRadius': startRadius,
      'endLatitude': endLatitude,
      'endLongitude': endLongitude,
      'endRadius': endRadius,
      'thumbnailUrl': thumbnailUrl,
      'publishedAt': publishedAt,
      'createdAt': createdAt,
    };
  }

  factory Track.fromMap(Map<String, dynamic> map) {
    return Track(
      id: map['id'],
      name: map['name'],
      description: map['description'],
      length: map['length'],
      startLatitude: map['startLatitude'],
      startLongitude: map['startLongitude'],
      startRadius: map['startRadius'] ?? 50.0,
      endLatitude: map['endLatitude'],
      endLongitude: map['endLongitude'],
      endRadius: map['endRadius'] ?? 50.0,
      thumbnailUrl: map['thumbnailUrl'],
      publishedAt: map['publishedAt'],
      createdAt: map['createdAt'],
    );
  }

  Track copyWith({
    int? id,
    String? name,
    String? description,
    double? length,
    double? startLatitude,
    double? startLongitude,
    double? startRadius,
    double? endLatitude,
    double? endLongitude,
    double? endRadius,
    String? thumbnailUrl,
    String? publishedAt,
    String? createdAt,
  }) {
    return Track(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      length: length ?? this.length,
      startLatitude: startLatitude ?? this.startLatitude,
      startLongitude: startLongitude ?? this.startLongitude,
      startRadius: startRadius ?? this.startRadius,
      endLatitude: endLatitude ?? this.endLatitude,
      endLongitude: endLongitude ?? this.endLongitude,
      endRadius: endRadius ?? this.endRadius,
      thumbnailUrl: thumbnailUrl ?? this.thumbnailUrl,
      publishedAt: publishedAt ?? this.publishedAt,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}
