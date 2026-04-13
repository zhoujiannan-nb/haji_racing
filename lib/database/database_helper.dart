import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/car.dart';
import '../models/user.dart';
import '../models/track.dart';
import '../models/track_record.dart';
import '../models/track_point.dart';

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
      version: 4,
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
        createdAt TEXT NOT NULL DEFAULT (datetime('now'))
      )
    ''');

    // 赛道表
    await db.execute('''
      CREATE TABLE tracks (
        id $idType,
        name $textType,
        description TEXT,
        length $realType,
        startLatitude $realType,
        startLongitude $realType,
        startRadius $realType DEFAULT 50.0,
        endLatitude $realType,
        endLongitude $realType,
        endRadius $realType DEFAULT 50.0,
        thumbnailUrl TEXT,
        publishedAt TEXT NOT NULL DEFAULT (datetime('now')),
        createdAt TEXT NOT NULL DEFAULT (datetime('now'))
      )
    ''');

    // 轨迹记录表
    await db.execute('''
      CREATE TABLE track_records (
        id $idType,
        userId INTEGER NOT NULL,
        trackId INTEGER NOT NULL,
        startTime TEXT NOT NULL,
        endTime TEXT,
        duration REAL,
        status TEXT NOT NULL DEFAULT 'incomplete',
        FOREIGN KEY (userId) REFERENCES users(id),
        FOREIGN KEY (trackId) REFERENCES tracks(id)
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

    if (oldVersion < 4) {
      // 版本3升级到版本4：添加用户表、赛道表、轨迹记录表、轨迹点表
      // 由于开发阶段直接重装app，这里暂不实现迁移逻辑
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

  // 获取当前用户（默认返回第一个用户）
  Future<User?> getCurrentUser() async {
    final db = await database;
    final result = await db.query('users', limit: 1);
    if (result.isNotEmpty) {
      return User.fromMap(result.first);
    }
    return null;
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

  // 关闭数据库
  Future close() async {
    final db = await database;
    db.close();
  }
}
