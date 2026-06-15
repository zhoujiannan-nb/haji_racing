/// 赛道规则模型 - 用于检查点的验证规则
class TrackRule {
  final int? id;
  final int checkPointId; // 关联的检查点ID
  final String ruleType; // 规则类型: speed_limit, min_points, time_limit等
  final Map<String, dynamic> parameters; // 规则参数
  final String description; // 规则描述

  TrackRule({
    this.id,
    required this.checkPointId,
    required this.ruleType,
    required this.parameters,
    required this.description,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'checkPointId': checkPointId,
      'ruleType': ruleType,
      'parameters': _parametersToString(parameters),
      'description': description,
    };
  }

  factory TrackRule.fromMap(Map<String, dynamic> map) {
    return TrackRule(
      id: map['id'],
      checkPointId: map['checkPointId'],
      ruleType: map['ruleType'],
      parameters: _stringToParameters(map['parameters']),
      description: map['description'],
    );
  }

  /// 将参数Map转换为JSON字符串存储
  static String _parametersToString(Map<String, dynamic> params) {
    // 简单的键值对转换，实际可以使用dart:convert的jsonEncode
    return params.entries.map((e) => '${e.key}=${e.value}').join(';');
  }

  /// 从JSON字符串解析参数Map
  static Map<String, dynamic> _stringToParameters(String paramStr) {
    if (paramStr == null || paramStr.isEmpty) {
      return {};
    }
    final result = <String, dynamic>{};
    paramStr.split(';').forEach((pair) {
      final parts = pair.split('=');
      if (parts.length == 2) {
        // 尝试转换为数字
        final value = parts[1];
        if (value.contains('.')) {
          result[parts[0]] = double.tryParse(value) ?? value;
        } else {
          result[parts[0]] = int.tryParse(value) ?? value;
        }
      }
    });
    return result;
  }

  TrackRule copyWith({
    int? id,
    int? checkPointId,
    String? ruleType,
    Map<String, dynamic>? parameters,
    String? description,
  }) {
    return TrackRule(
      id: id ?? this.id,
      checkPointId: checkPointId ?? this.checkPointId,
      ruleType: ruleType ?? this.ruleType,
      parameters: parameters ?? this.parameters,
      description: description ?? this.description,
    );
  }

  /// 验证是否满足规则
  bool validate(Map<String, dynamic> context) {
    switch (ruleType) {
      case 'speed_limit':
        return _validateSpeedLimit(context);
      case 'min_points':
        return _validateMinPoints(context);
      case 'time_limit':
        return _validateTimeLimit(context);
      default:
        return true;
    }
  }

  /// 验证速度限制规则
  bool _validateSpeedLimit(Map<String, dynamic> context) {
    final maxSpeed = parameters['max_speed'] as double?;
    final minSpeed = parameters['min_speed'] as double?;
    final currentSpeed = context['speed'] as double?;

    if (currentSpeed == null) return false;
    if (maxSpeed != null && currentSpeed > maxSpeed) return false;
    if (minSpeed != null && currentSpeed < minSpeed) return false;
    return true;
  }

  /// 验证最少点数规则（比如在检查点内至少有N个点的速度低于某值）
  bool _validateMinPoints(Map<String, dynamic> context) {
    final minPoints = parameters['min_points'] as int?;
    final speedThreshold = parameters['speed_threshold'] as double?;
    final pointsInZone = context['points_in_zone'] as List<dynamic>?;

    if (minPoints == null || pointsInZone == null) return false;

    int countBelowThreshold = 0;
    for (var point in pointsInZone) {
      final speed = point is Map ? point['speed'] as double? : null;
      if (speed != null && speedThreshold != null && speed < speedThreshold) {
        countBelowThreshold++;
      }
    }

    return countBelowThreshold >= minPoints;
  }

  /// 验证时间限制规则
  bool _validateTimeLimit(Map<String, dynamic> context) {
    final maxTime = parameters['max_time'] as double?;
    final elapsedTime = context['elapsed_time'] as double?;

    if (maxTime == null || elapsedTime == null) return false;
    return elapsedTime <= maxTime;
  }
}
