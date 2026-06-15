import 'dart:convert';
import 'package:flutter/services.dart' show rootBundle;
import '../database/database_helper.dart';
import '../models/track.dart';
import '../models/checkpoint.dart';
import '../models/track_rule.dart';
import '../utils/location_utils.dart';

/// 赛道JSON导入服务
class TrackImportService {
  static final TrackImportService _instance = TrackImportService._internal();
  factory TrackImportService() => _instance;
  TrackImportService._internal();

  final DatabaseHelper _db = DatabaseHelper.instance;

  /// 从assets目录导入所有赛道JSON文件
  Future<void> importTracksFromAssets() async {
    try {
      print('开始从assets导入赛道数据...');

      // 读取tracks目录下的所有json文件
      // 注意：Flutter无法直接列出assets目录内容，需要预先知道文件名
      // 这里使用一个索引文件来管理所有赛道文件
      final indexFile = await _loadAssetFile('assets/tracks/tracks_index.json');

      if (indexFile == null) {
        print('未找到tracks_index.json，跳过导入');
        return;
      }

      final indexData = jsonDecode(indexFile) as Map<String, dynamic>;
      final trackFiles = indexData['tracks'] as List<dynamic>;

      print('找到 ${trackFiles.length} 个赛道文件');

      for (var trackFile in trackFiles) {
        final fileName = trackFile as String;
        await _importSingleTrack(fileName);
      }

      print('赛道数据导入完成');
    } catch (e) {
      print('导入赛道数据失败: $e');
      rethrow;
    }
  }

  /// 导入单个赛道文件
  Future<void> _importSingleTrack(String fileName) async {
    try {
      final jsonString = await _loadAssetFile('assets/tracks/$fileName');
      if (jsonString == null) {
        print('文件不存在: $fileName');
        return;
      }

      final jsonData = jsonDecode(jsonString) as Map<String, dynamic>;
      await _saveTrackFromJson(jsonData);
      print('成功导入赛道: ${jsonData['name']}');
    } catch (e) {
      print('导入赛道文件 $fileName 失败: $e');
    }
  }

  /// 从JSON数据保存赛道到数据库
  Future<void> _saveTrackFromJson(Map<String, dynamic> jsonData) async {
    // 检查赛道是否已存在（根据名称）
    final existingTracks = await _db.getAllTracks();
    final trackName = jsonData['name'] as String;
    if (existingTracks.any((t) => t.name == trackName)) {
      print('赛道 "$trackName" 已存在，跳过导入');
      return;
    }

    // 解析赛道基本信息
    final track = Track(
      name: trackName,
      description: jsonData['description'] as String?,
      length: (jsonData['length'] as num).toDouble(),
      startPolygon: _parsePolygon(jsonData['startPolygon'] as List<dynamic>),
      endPolygon: _parsePolygon(jsonData['endPolygon'] as List<dynamic>),
      thumbnailUrl: jsonData['thumbnailUrl'] as String?,
      publishedAt: jsonData['publishedAt'] ?? DateTime.now().toIso8601String(),
      createdAt: jsonData['createdAt'] ?? DateTime.now().toIso8601String(),
    );

    // 插入赛道
    final trackId = await _db.insertTrack(track);

    // 解析并插入检查点
    if (jsonData['checkPoints'] != null) {
      final checkPointsData = jsonData['checkPoints'] as List<dynamic>;
      for (var cpData in checkPointsData) {
        final checkPointMap = cpData as Map<String, dynamic>;
        final checkPoint = CheckPoint(
          trackId: trackId,
          name: checkPointMap['name'] as String,
          sequence: checkPointMap['sequence'] as int,
          polygon: _parsePolygon(checkPointMap['polygon'] as List<dynamic>),
          description: checkPointMap['description'] as String?,
        );

        final checkPointId = await _db.insertCheckPoint(checkPoint);

        // 解析并插入规则
        if (checkPointMap['rules'] != null) {
          final rulesData = checkPointMap['rules'] as List<dynamic>;
          for (var ruleData in rulesData) {
            final ruleMap = ruleData as Map<String, dynamic>;
            final rule = TrackRule(
              checkPointId: checkPointId,
              ruleType: ruleMap['ruleType'] as String,
              parameters: Map<String, dynamic>.from(ruleMap['parameters']),
              description: ruleMap['description'] as String,
            );
            await _db.insertRule(rule);
          }
        }
      }
    }
  }

  /// 解析多边形坐标（将GCJ-02坐标转换为WGS-84坐标）
  List<LatLng> _parsePolygon(List<dynamic> coords) {
    return coords.map((coord) {
      final coordMap = coord as Map<String, dynamic>;
      final gcjLat = (coordMap['latitude'] as num).toDouble();
      final gcjLon = (coordMap['longitude'] as num).toDouble();

      // 将GCJ-02坐标转换为WGS-84坐标
      final wgs84 = LocationUtils.gcj02ToWgs84(gcjLat, gcjLon);

      return LatLng(wgs84.latitude, wgs84.longitude);
    }).toList();
  }

  /// 加载asset文件
  Future<String?> _loadAssetFile(String path) async {
    try {
      return await rootBundle.loadString(path);
    } catch (e) {
      print('加载文件失败 $path: $e');
      return null;
    }
  }

  /// 清空所有赛道数据（用于重新导入）
  Future<void> clearAllTracks() async {
    try {
      final db = await _db.database;
      await db.delete('track_rules');
      await db.delete('checkpoints');
      await db.delete('track_records');
      await db.delete('track_points');
      await db.delete('tracks');
      print('已清空所有赛道数据');
    } catch (e) {
      print('清空赛道数据失败: $e');
      rethrow;
    }
  }
}
