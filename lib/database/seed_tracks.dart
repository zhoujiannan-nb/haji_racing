import '../database/database_helper.dart';
import '../models/track.dart';

/// 初始化测试赛道数据
Future<void> initializeTestTracks() async {
  final db = DatabaseHelper.instance;

  // 检查是否已有赛道数据
  final existingTracks = await db.getAllTracks();
  if (existingTracks.isNotEmpty) {
    print('赛道数据已存在，跳过初始化');
    return;
  }

  print('开始初始化测试赛道数据...');

  // 测试赛道1：北京金港赛道（示例数据）
  final track1 = Track(
    name: '北京金港赛道',
    description: '位于北京市朝阳区，是一条FIA认证的三级赛道，全长2.063公里，拥有8个弯道，是华北地区知名的赛车场地。',
    length: 2063,
    startLatitude: 40.0079,
    startLongitude: 116.5119,
    startRadius: 50.0,
    endLatitude: 40.0085,
    endLongitude: 116.5125,
    endRadius: 50.0,
    publishedAt: DateTime.now().toIso8601String(),
    createdAt: DateTime.now().toIso8601String(),
  );

  // 测试赛道2：上海国际赛车场（示例数据）
  final track2 = Track(
    name: '上海国际赛车场',
    description:
        '中国第一条F1赛道，由著名设计师Hermann Tilke设计，单圈长度5.451公里，共有16个弯道，曾举办多年F1中国大奖赛。',
    length: 5451,
    startLatitude: 31.3389,
    startLongitude: 121.2197,
    startRadius: 50.0,
    endLatitude: 31.3395,
    endLongitude: 121.2203,
    endRadius: 50.0,
    publishedAt: DateTime.now().toIso8601String(),
    createdAt: DateTime.now().toIso8601String(),
  );

  // 测试赛道3：珠海国际赛车场（示例数据）
  final track3 = Track(
    name: '珠海国际赛车场',
    description: '中国第一个永久性的国际赛车场，建成于1996年，赛道全长4.3公里，共有14个弯道，是中国赛车运动的重要发源地之一。',
    length: 4300,
    startLatitude: 22.3681,
    startLongitude: 113.5590,
    startRadius: 50.0,
    endLatitude: 22.3687,
    endLongitude: 113.5596,
    endRadius: 50.0,
    publishedAt: DateTime.now().toIso8601String(),
    createdAt: DateTime.now().toIso8601String(),
  );

  await db.insertTrack(track1);
  await db.insertTrack(track2);
  await db.insertTrack(track3);

  print('测试赛道数据初始化完成，共添加 ${3} 条赛道');
}
