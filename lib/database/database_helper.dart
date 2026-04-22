import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/car.dart';
import '../models/user.dart';
import '../models/track.dart';
import '../models/track_record.dart';
import '../models/track_point.dart';
import '../models/checkpoint.dart';
import '../models/track_rule.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'cars.db');

    return await openDatabase(
      path,
      version: 10,
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  Future<void> _createDB(Database db, int version) async {
    const idType = 'INTEGER PRIMARY KEY AUTOINCREMENT';
    const textType = 'TEXT NOT NULL';
    const realType = 'REAL NOT NULL';
    const intType = 'INTEGER DEFAULT 0';

    // 车辆表
    await db.execute('''
      CREATE TABLE cars (
        id $idType,
        name $textType,
        horsepower $realType,
        weight $realType,
        frontTireWidth $realType,
        rearTireWidth $realType,
        tireType $textType,
        avatarUrl TEXT,
        isMain $intType
      )
    ''');

    // 用户表
    await db.execute('''
      CREATE TABLE users (
        id $idType,
        username $textType,
        account TEXT,
        email TEXT,
        token TEXT,
        role TEXT,
        createdAt TEXT NOT NULL DEFAULT (datetime('now'))
      )
    ''');

    // 赛道表 - 支持多边形电子围栏
    await db.execute('''
      CREATE TABLE tracks (
        id $idType,
        name $textType,
        description TEXT,
        length $realType,
        startPolygon TEXT NOT NULL,
        endPolygon TEXT NOT NULL,
        thumbnailUrl TEXT,
        publishedAt TEXT NOT NULL DEFAULT (datetime('now')),
        createdAt TEXT NOT NULL DEFAULT (datetime('now'))
      )
    ''');

    // 检查点表
    await db.execute('''
      CREATE TABLE checkpoints (
        id $idType,
        trackId INTEGER NOT NULL,
        name $textType,
        sequence INTEGER NOT NULL,
        polygon TEXT NOT NULL,
        description TEXT,
        FOREIGN KEY (trackId) REFERENCES tracks(id)
      )
    ''');

    // 赛道规则表
    await db.execute('''
      CREATE TABLE track_rules (
        id $idType,
        checkPointId INTEGER NOT NULL,
        ruleType $textType,
        parameters TEXT NOT NULL,
        description TEXT,
        FOREIGN KEY (checkPointId) REFERENCES checkpoints(id)
      )
    ''');

    // 轨迹记录表
    await db.execute('''
      CREATE TABLE track_records (
        id $idType,
        userId INTEGER NOT NULL,
        trackId INTEGER NOT NULL,
        carId INTEGER,
        startTime TEXT NOT NULL,
        endTime TEXT,
        duration REAL,
        status TEXT NOT NULL DEFAULT 'incomplete',
        manuallyStopped INTEGER DEFAULT 0,
        trajectoryJson TEXT,
        FOREIGN KEY (userId) REFERENCES users(id),
        FOREIGN KEY (trackId) REFERENCES tracks(id),
        FOREIGN KEY (carId) REFERENCES cars(id)
      )
    ''');

    // 轨迹点表
    await db.execute('''
      CREATE TABLE track_points (
        id $idType,
        recordId INTEGER NOT NULL,
        latitude $realType,
        longitude $realType,
        speed REAL,
        timestamp TEXT NOT NULL,
        sequence INTEGER NOT NULL,
        FOREIGN KEY (recordId) REFERENCES track_records(id)
      )
    ''');

    // 插入默认用户
    await db.insert('users', {
      'username': 'race',
      'createdAt': DateTime.now().toIso8601String(),
    });
  }

  Future<void> _upgradeDB(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      // 版本1升级到版本2：添加前后轮宽度字段
      await db.execute(
        'ALTER TABLE cars ADD COLUMN frontTireWidth REAL DEFAULT 200.0',
      );
      await db.execute(
        'ALTER TABLE cars ADD COLUMN rearTireWidth REAL DEFAULT 200.0',
      );

      // 将旧的 tireWidth 数据复制到新字段
      await db.execute(
        'UPDATE cars SET frontTireWidth = tireWidth WHERE frontTireWidth IS NULL',
      );
      await db.execute(
        'UPDATE cars SET rearTireWidth = tireWidth WHERE rearTireWidth IS NULL',
      );
    }

    if (oldVersion < 3) {
      // 版本2升级到版本3：添加 isMain 字段
      await db.execute('ALTER TABLE cars ADD COLUMN isMain INTEGER DEFAULT 0');

      // 将第一个车辆设为主车辆
      await db.execute(
        'UPDATE cars SET isMain = 1 WHERE id = (SELECT MIN(id) FROM cars)',
      );
    }

    if (oldVersion < 5) {
      // 版本4升级到版本5：重构赛道表结构，支持多边形电子围栏和检查点
      const idType = 'INTEGER PRIMARY KEY AUTOINCREMENT';
      const textType = 'TEXT NOT NULL';
      const realType = 'REAL NOT NULL';

      // 删除旧表
      await db.execute('DROP TABLE IF EXISTS tracks');

      // 创建新表
      await db.execute('''
        CREATE TABLE tracks (
          id $idType,
          name $textType,
          description TEXT,
          length $realType,
          startPolygon TEXT NOT NULL,
          endPolygon TEXT NOT NULL,
          thumbnailUrl TEXT,
          publishedAt TEXT NOT NULL DEFAULT (datetime('now')),
          createdAt TEXT NOT NULL DEFAULT (datetime('now'))
        )
      ''');

      // 创建检查点表
      await db.execute('''
        CREATE TABLE checkpoints (
          id $idType,
          trackId INTEGER NOT NULL,
          name $textType,
          sequence INTEGER NOT NULL,
          polygon TEXT NOT NULL,
          description TEXT,
          FOREIGN KEY (trackId) REFERENCES tracks(id)
        )
      ''');

      // 创建赛道规则表
      await db.execute('''
        CREATE TABLE track_rules (
          id $idType,
          checkPointId INTEGER NOT NULL,
          ruleType $textType,
          parameters TEXT NOT NULL,
          description TEXT,
          FOREIGN KEY (checkPointId) REFERENCES checkpoints(id)
        )
      ''');
    }

    if (oldVersion < 6) {
      // 版本5升级到版本6：为轨迹记录表添加carId字段
      await db.execute('ALTER TABLE track_records ADD COLUMN carId INTEGER');
    }

    if (oldVersion < 8) {
      // 版本7升级到版本8：确保manuallyStopped字段存在
      try {
        await db.execute(
          'ALTER TABLE track_records ADD COLUMN manuallyStopped INTEGER DEFAULT 0',
        );
      } catch (e) {
        // 如果字段已存在，忽略错误
        print('manuallyStopped字段可能已存在: $e');
      }
    }

    if (oldVersion < 9) {
      // 版本8升级到版本9：添加trajectoryJson字段用于存储JSON格式的轨迹数据
      try {
        await db.execute(
          'ALTER TABLE track_records ADD COLUMN trajectoryJson TEXT',
        );
      } catch (e) {
        // 如果字段已存在，忽略错误
        print('trajectoryJson字段可能已存在: $e');
      }
    }

    if (oldVersion < 10) {
      // 版本9升级到版本10：为users表添加token等字段
      try {
        await db.execute('ALTER TABLE users ADD COLUMN account TEXT');
        await db.execute('ALTER TABLE users ADD COLUMN email TEXT');
        await db.execute('ALTER TABLE users ADD COLUMN token TEXT');
        await db.execute('ALTER TABLE users ADD COLUMN role TEXT');
      } catch (e) {
        print('users表字段可能已存在: $e');
      }
    }
  }

  // 插入车辆
  Future<int> insertCar(Car car) async {
    final db = await database;
    return await db.insert('cars', car.toMap());
  }

  // 查询所有车辆
  Future<List<Car>> getAllCars() async {
    final db = await database;
    final result = await db.query('cars', orderBy: 'id DESC');
    return result.map((map) => Car.fromMap(map)).toList();
  }

  // 查询单个车辆
  Future<Car?> getCar(int id) async {
    final db = await database;
    final result = await db.query('cars', where: 'id = ?', whereArgs: [id]);
    if (result.isNotEmpty) {
      return Car.fromMap(result.first);
    }
    return null;
  }

  // 更新车辆
  Future<int> updateCar(Car car) async {
    final db = await database;
    return await db.update(
      'cars',
      car.toMap(),
      where: 'id = ?',
      whereArgs: [car.id],
    );
  }

  // 删除车辆
  Future<int> deleteCar(int id) async {
    final db = await database;
    return await db.delete('cars', where: 'id = ?', whereArgs: [id]);
  }

  // 获取主车辆
  Future<Car?> getMainCar() async {
    final db = await database;
    final result = await db.query(
      'cars',
      where: 'isMain = ?',
      whereArgs: [1],
      limit: 1,
    );
    if (result.isNotEmpty) {
      return Car.fromMap(result.first);
    }
    // 如果没有主车辆，返回第一个车辆
    return await getFirstCar();
  }

  // 设置主车辆
  Future<void> setMainCar(int carId) async {
    final db = await database;
    // 先将所有车辆的 isMain 设为 0
    await db.update('cars', {'isMain': 0});
    // 再将指定车辆的 isMain 设为 1
    await db.update('cars', {'isMain': 1}, where: 'id = ?', whereArgs: [carId]);
  }

  // 获取第一个车辆（作为主车辆）
  Future<Car?> getFirstCar() async {
    final db = await database;
    final result = await db.query('cars', orderBy: 'id ASC', limit: 1);
    if (result.isNotEmpty) {
      return Car.fromMap(result.first);
    }
    return null;
  }

  // ==================== 用户相关操作 ====================

  // 获取当前用户,如果已登录返回登录的用户
  Future<User?> getCurrentUser() async {
    final db = await database;
    // 优先获取已登录的用户（token不为空）
    var result = await db.query(
      'users',
      where: 'token IS NOT NULL AND token != ?',
      whereArgs: [''],
      orderBy: 'id DESC',
      limit: 1,
    );

    // 如果没有已登录的用户，则返回任意一个用户
    if (result.isEmpty) {
      result = await db.query('users', limit: 1);
    }

    if (result.isNotEmpty) {
      return User.fromMap(result.first);
    }
    return null;
  }

  // 更新用户token
  Future<int> updateUserToken(int userId, String? token) async {
    final db = await database;
    return await db.update(
      'users',
      {'token': token},
      where: 'id = ?',
      whereArgs: [userId],
    );
  }

  // 根据account查找用户
  Future<User?> getUserByAccount(String account) async {
    final db = await database;
    final result = await db.query(
      'users',
      where: 'account = ?',
      whereArgs: [account],
      limit: 1,
    );
    if (result.isNotEmpty) {
      return User.fromMap(result.first);
    }
    return null;
  }

  // 保存或更新用户信息
  Future<int> saveUser(User user) async {
    final db = await database;
    if (user.id != null) {
      // 更新现有用户
      return await db.update(
        'users',
        user.toMap(),
        where: 'id = ?',
        whereArgs: [user.id],
      );
    } else {
      // 插入新用户
      return await db.insert('users', user.toMap());
    }
  }

  // ==================== 赛道相关操作 ====================

  // 获取所有赛道
  Future<List<Track>> getAllTracks() async {
    final db = await database;
    final result = await db.query('tracks', orderBy: 'publishedAt DESC');
    return result.map((map) => Track.fromMap(map)).toList();
  }

  // 获取单个赛道
  Future<Track?> getTrack(int id) async {
    final db = await database;
    final result = await db.query('tracks', where: 'id = ?', whereArgs: [id]);
    if (result.isNotEmpty) {
      return Track.fromMap(result.first);
    }
    return null;
  }

  // 插入赛道
  Future<int> insertTrack(Track track) async {
    final db = await database;
    return await db.insert('tracks', track.toMap());
  }

  // 删除赛道（同时删除关联的检查点、规则和轨迹记录）
  Future<int> deleteTrack(int id) async {
    final db = await database;
    // 先获取该赛道的所有检查点
    final checkpoints = await getCheckPointsByTrack(id);

    // 删除每个检查点的规则
    for (var checkpoint in checkpoints) {
      await db.delete(
        'track_rules',
        where: 'checkPointId = ?',
        whereArgs: [checkpoint.id],
      );
    }

    // 删除检查点
    await db.delete('checkpoints', where: 'trackId = ?', whereArgs: [id]);

    // 删除轨迹记录（会级联删除轨迹点）
    await db.delete('track_records', where: 'trackId = ?', whereArgs: [id]);

    // 最后删除赛道
    return await db.delete('tracks', where: 'id = ?', whereArgs: [id]);
  }

  // ==================== 检查点相关操作 ====================

  // 获取赛道的所有检查点（按顺序）
  Future<List<CheckPoint>> getCheckPointsByTrack(int trackId) async {
    final db = await database;
    final result = await db.query(
      'checkpoints',
      where: 'trackId = ?',
      whereArgs: [trackId],
      orderBy: 'sequence ASC',
    );
    return result.map((map) => CheckPoint.fromMap(map)).toList();
  }

  // 插入检查点
  Future<int> insertCheckPoint(CheckPoint checkPoint) async {
    final db = await database;
    return await db.insert('checkpoints', checkPoint.toMap());
  }

  // 批量插入检查点
  Future<void> insertCheckPoints(List<CheckPoint> checkPoints) async {
    final db = await database;
    for (var checkPoint in checkPoints) {
      await db.insert('checkpoints', checkPoint.toMap());
    }
  }

  // 删除检查点（同时删除关联的规则）
  Future<int> deleteCheckPoint(int id) async {
    final db = await database;
    // 先删除关联的规则
    await db.delete('track_rules', where: 'checkPointId = ?', whereArgs: [id]);
    // 再删除检查点
    return await db.delete('checkpoints', where: 'id = ?', whereArgs: [id]);
  }

  // ==================== 赛道规则相关操作 ====================

  // 获取检查点的所有规则
  Future<List<TrackRule>> getRulesByCheckPoint(int checkPointId) async {
    final db = await database;
    final result = await db.query(
      'track_rules',
      where: 'checkPointId = ?',
      whereArgs: [checkPointId],
    );
    return result.map((map) => TrackRule.fromMap(map)).toList();
  }

  // 插入规则
  Future<int> insertRule(TrackRule rule) async {
    final db = await database;
    return await db.insert('track_rules', rule.toMap());
  }

  // 批量插入规则
  Future<void> insertRules(List<TrackRule> rules) async {
    final db = await database;
    for (var rule in rules) {
      await db.insert('track_rules', rule.toMap());
    }
  }

  // 删除规则
  Future<int> deleteRule(int id) async {
    final db = await database;
    return await db.delete('track_rules', where: 'id = ?', whereArgs: [id]);
  }

  // 获取完整的赛道信息（包含检查点和规则）
  Future<Track?> getTrackWithDetails(int id) async {
    final track = await getTrack(id);
    if (track == null) return null;

    final checkPoints = await getCheckPointsByTrack(id);
    final List<TrackRule> allRules = [];

    for (var checkPoint in checkPoints) {
      final rules = await getRulesByCheckPoint(checkPoint.id!);
      allRules.addAll(rules);
    }

    return track.copyWith(checkPoints: checkPoints, rules: allRules);
  }

  // ==================== 轨迹记录相关操作 ====================

  // 创建轨迹记录
  Future<int> createTrackRecord(TrackRecord record) async {
    final db = await database;
    return await db.insert('track_records', record.toMap());
  }

  // 更新轨迹记录
  Future<int> updateTrackRecord(TrackRecord record) async {
    final db = await database;
    return await db.update(
      'track_records',
      record.toMap(),
      where: 'id = ?',
      whereArgs: [record.id],
    );
  }

  // 删除轨迹记录（同时删除关联的轨迹点）
  Future<int> deleteTrackRecord(int id) async {
    final db = await database;
    // 先删除关联的轨迹点
    await db.delete('track_points', where: 'recordId = ?', whereArgs: [id]);
    // 再删除记录
    return await db.delete('track_records', where: 'id = ?', whereArgs: [id]);
  }

  // 获取轨迹记录
  Future<TrackRecord?> getTrackRecord(int id) async {
    final db = await database;
    final result = await db.query(
      'track_records',
      where: 'id = ?',
      whereArgs: [id],
    );
    if (result.isNotEmpty) {
      return TrackRecord.fromMap(result.first);
    }
    return null;
  }

  // ==================== 轨迹点相关操作 ====================

  // 批量插入轨迹点
  Future<void> insertTrackPoints(List<TrackPoint> points) async {
    final db = await database;
    final batch = db.batch();
    for (var point in points) {
      batch.insert('track_points', point.toMap());
    }
    await batch.commit(noResult: true);
  }

  // 插入单个轨迹点
  Future<int> insertTrackPoint(TrackPoint point) async {
    final db = await database;
    return await db.insert('track_points', point.toMap());
  }

  // 获取某次记录的所有轨迹点
  Future<List<TrackPoint>> getTrackPoints(int recordId) async {
    final db = await database;
    final result = await db.query(
      'track_points',
      where: 'recordId = ?',
      whereArgs: [recordId],
      orderBy: 'sequence ASC',
    );
    return result.map((map) => TrackPoint.fromMap(map)).toList();
  }

  // 获取赛道的圈速榜（按时间排序，只包含已完成的记录）
  Future<List<TrackRecord>> getTrackLeaderboard(int trackId) async {
    final db = await database;
    final result = await db.query(
      'track_records',
      where: 'trackId = ? AND status = ? AND duration IS NOT NULL',
      whereArgs: [trackId, 'completed'],
      orderBy: 'duration ASC',
    );
    return result.map((map) => TrackRecord.fromMap(map)).toList();
  }

  // 获取用户的所有轨迹记录（按开始时间倒序）
  Future<List<TrackRecord>> getUserTrackRecords(int userId) async {
    final db = await database;
    final result = await db.query(
      'track_records',
      where: 'userId = ? AND duration IS NOT NULL',
      whereArgs: [userId],
      orderBy: 'startTime DESC',
    );
    return result.map((map) => TrackRecord.fromMap(map)).toList();
  }

  // 关闭数据库
  Future close() async {
    final db = await database;
    db.close();
  }
}
