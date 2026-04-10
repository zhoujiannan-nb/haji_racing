import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/car.dart';

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
      version: 3,
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  Future<void> _createDB(Database db, int version) async {
    const idType = 'INTEGER PRIMARY KEY AUTOINCREMENT';
    const textType = 'TEXT NOT NULL';
    const realType = 'REAL NOT NULL';
    const intType = 'INTEGER DEFAULT 0';

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

  // 关闭数据库
  Future close() async {
    final db = await database;
    db.close();
  }
}
