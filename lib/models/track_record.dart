class TrackRecord {
  final int? id;
  final int userId;
  final int trackId;
  final String startTime;
  final String? endTime;
  final double? duration;
  final String status; // incomplete or completed

  TrackRecord({
    this.id,
    required this.userId,
    required this.trackId,
    required this.startTime,
    this.endTime,
    this.duration,
    this.status = 'incomplete',
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'userId': userId,
      'trackId': trackId,
      'startTime': startTime,
      'endTime': endTime,
      'duration': duration,
      'status': status,
    };
  }

  factory TrackRecord.fromMap(Map<String, dynamic> map) {
    return TrackRecord(
      id: map['id'],
      userId: map['userId'],
      trackId: map['trackId'],
      startTime: map['startTime'],
      endTime: map['endTime'],
      duration: map['duration'],
      status: map['status'],
    );
  }

  TrackRecord copyWith({
    int? id,
    int? userId,
    int? trackId,
    String? startTime,
    String? endTime,
    double? duration,
    String? status,
  }) {
    return TrackRecord(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      trackId: trackId ?? this.trackId,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      duration: duration ?? this.duration,
      status: status ?? this.status,
    );
  }
}
