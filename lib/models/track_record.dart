class TrackRecord {
  final int? id;
  final int userId;
  final int trackId;
  final int? carId; // 车辆ID
  final String startTime;
  final String? endTime;
  final double? duration;
  final String status; // incomplete or completed
  final bool manuallyStopped; // 是否手动停止

  TrackRecord({
    this.id,
    required this.userId,
    required this.trackId,
    this.carId,
    required this.startTime,
    this.endTime,
    this.duration,
    this.status = 'incomplete',
    this.manuallyStopped = false,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'userId': userId,
      'trackId': trackId,
      'carId': carId,
      'startTime': startTime,
      'endTime': endTime,
      'duration': duration,
      'status': status,
      'manuallyStopped': manuallyStopped ? 1 : 0,
    };
  }

  factory TrackRecord.fromMap(Map<String, dynamic> map) {
    return TrackRecord(
      id: map['id'],
      userId: map['userId'],
      trackId: map['trackId'],
      carId: map['carId'],
      startTime: map['startTime'],
      endTime: map['endTime'],
      duration: map['duration'],
      status: map['status'],
      manuallyStopped:
          map['manuallyStopped'] == 1 || map['manuallyStopped'] == true,
    );
  }

  TrackRecord copyWith({
    int? id,
    int? userId,
    int? trackId,
    int? carId,
    String? startTime,
    String? endTime,
    double? duration,
    String? status,
    bool? manuallyStopped,
  }) {
    return TrackRecord(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      trackId: trackId ?? this.trackId,
      carId: carId ?? this.carId,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      duration: duration ?? this.duration,
      status: status ?? this.status,
      manuallyStopped: manuallyStopped ?? this.manuallyStopped,
    );
  }
}
