import 'checkpoint.dart';
import 'track_rule.dart';

class Track {
  final int? id;
  final String name;
  final String? description;
  final double length;
  final List<LatLng> startPolygon; // 起点多边形电子围栏
  final List<LatLng> endPolygon; // 终点多边形电子围栏
  final String? thumbnailUrl;
  final String publishedAt;
  final String createdAt;

  // 关联的检查点和规则（不从数据库直接加载，需要单独查询）
  List<CheckPoint>? checkPoints;
  List<TrackRule>? rules;

  Track({
    this.id,
    required this.name,
    this.description,
    required this.length,
    required this.startPolygon,
    required this.endPolygon,
    this.thumbnailUrl,
    required this.publishedAt,
    required this.createdAt,
    this.checkPoints,
    this.rules,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'length': length,
      'startPolygon': _polygonToString(startPolygon),
      'endPolygon': _polygonToString(endPolygon),
      'thumbnailUrl': thumbnailUrl,
      'publishedAt': publishedAt,
      'createdAt': createdAt,
    };
  }

  /// 将多边形坐标列表转换为JSON字符串存储
  static String _polygonToString(List<LatLng> polygon) {
    return polygon
        .map((point) => '${point.latitude},${point.longitude}')
        .join(';');
  }

  /// 从JSON字符串解析多边形坐标列表
  static List<LatLng> _stringToPolygon(String polygonStr) {
    if (polygonStr == null || polygonStr.isEmpty) {
      return [];
    }
    return polygonStr.split(';').map((coord) {
      final parts = coord.split(',');
      return LatLng(double.parse(parts[0]), double.parse(parts[1]));
    }).toList();
  }

  factory Track.fromMap(Map<String, dynamic> map) {
    return Track(
      id: map['id'],
      name: map['name'],
      description: map['description'],
      length: map['length'],
      startPolygon: _stringToPolygon(map['startPolygon']),
      endPolygon: _stringToPolygon(map['endPolygon']),
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
    List<LatLng>? startPolygon,
    List<LatLng>? endPolygon,
    String? thumbnailUrl,
    String? publishedAt,
    String? createdAt,
    List<CheckPoint>? checkPoints,
    List<TrackRule>? rules,
  }) {
    return Track(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      length: length ?? this.length,
      startPolygon: startPolygon ?? this.startPolygon,
      endPolygon: endPolygon ?? this.endPolygon,
      thumbnailUrl: thumbnailUrl ?? this.thumbnailUrl,
      publishedAt: publishedAt ?? this.publishedAt,
      createdAt: createdAt ?? this.createdAt,
      checkPoints: checkPoints ?? this.checkPoints,
      rules: rules ?? this.rules,
    );
  }
}
