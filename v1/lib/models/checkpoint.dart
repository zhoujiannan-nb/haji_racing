/// 检查点模型 - 支持多边形电子围栏
class CheckPoint {
  final int? id;
  final int trackId;
  final String name; // 检查点名称
  final int sequence; // 顺序号
  final List<LatLng> polygon; // 多边形顶点坐标
  final String? description; // 描述

  CheckPoint({
    this.id,
    required this.trackId,
    required this.name,
    required this.sequence,
    required this.polygon,
    this.description,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'trackId': trackId,
      'name': name,
      'sequence': sequence,
      'polygon': _polygonToString(polygon),
      'description': description,
    };
  }

  factory CheckPoint.fromMap(Map<String, dynamic> map) {
    return CheckPoint(
      id: map['id'],
      trackId: map['trackId'],
      name: map['name'],
      sequence: map['sequence'],
      polygon: _stringToPolygon(map['polygon']),
      description: map['description'],
    );
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

  CheckPoint copyWith({
    int? id,
    int? trackId,
    String? name,
    int? sequence,
    List<LatLng>? polygon,
    String? description,
  }) {
    return CheckPoint(
      id: id ?? this.id,
      trackId: trackId ?? this.trackId,
      name: name ?? this.name,
      sequence: sequence ?? this.sequence,
      polygon: polygon ?? this.polygon,
      description: description ?? this.description,
    );
  }
}

/// 经纬度坐标
class LatLng {
  final double latitude;
  final double longitude;

  LatLng(this.latitude, this.longitude);

  Map<String, dynamic> toMap() {
    return {'latitude': latitude, 'longitude': longitude};
  }

  factory LatLng.fromMap(Map<String, dynamic> map) {
    return LatLng(map['latitude'], map['longitude']);
  }

  @override
  String toString() {
    return 'LatLng($latitude, $longitude)';
  }
}
