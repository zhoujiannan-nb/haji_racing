class TrackPoint {
  final int? id;
  final int recordId;
  final double latitude;
  final double longitude;
  final double? speed;
  final String timestamp;
  final int sequence;

  TrackPoint({
    this.id,
    required this.recordId,
    required this.latitude,
    required this.longitude,
    this.speed,
    required this.timestamp,
    required this.sequence,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'recordId': recordId,
      'latitude': latitude,
      'longitude': longitude,
      'speed': speed,
      'timestamp': timestamp,
      'sequence': sequence,
    };
  }

  factory TrackPoint.fromMap(Map<String, dynamic> map) {
    return TrackPoint(
      id: map['id'],
      recordId: map['recordId'],
      latitude: map['latitude'],
      longitude: map['longitude'],
      speed: map['speed'],
      timestamp: map['timestamp'],
      sequence: map['sequence'],
    );
  }

  TrackPoint copyWith({
    int? id,
    int? recordId,
    double? latitude,
    double? longitude,
    double? speed,
    String? timestamp,
    int? sequence,
  }) {
    return TrackPoint(
      id: id ?? this.id,
      recordId: recordId ?? this.recordId,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      speed: speed ?? this.speed,
      timestamp: timestamp ?? this.timestamp,
      sequence: sequence ?? this.sequence,
    );
  }
}
