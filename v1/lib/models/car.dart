class Car {
  int? id;
  String name;
  double horsepower; // 马力 hp
  double weight; // 车重 kg
  double frontTireWidth; // 前轮轮胎宽度 mm
  double rearTireWidth; // 后轮轮胎宽度 mm
  String tireType; // SH 或 SS
  String? avatarUrl; // 虚拟形象URL
  bool isMain; // 是否为主车辆

  Car({
    this.id,
    required this.name,
    required this.horsepower,
    required this.weight,
    required this.frontTireWidth,
    required this.rearTireWidth,
    required this.tireType,
    this.avatarUrl,
    this.isMain = false,
  });

  // 计算PP分
  double calculatePP() {
    // PP ≈ [马力(hp) × 1000 / 车重(kg)] × 基数 + (轮胎宽度（4条总和） - 20) × 2 + 轮胎等级加成
    const double baseMultiplier = 50;

    // 基础性能分
    double performanceScore = (horsepower * 1000 / weight) * baseMultiplier;

    // 轮胎宽度分（4条轮胎总宽度，从mm转换为cm）
    // 两条前轮 + 两条后轮
    double totalTireWidthCm =
        ((frontTireWidth * 2) + (rearTireWidth * 2)) / 10; // mm转cm
    double tireWidthScore = (totalTireWidthCm - 20) * 2;

    // 轮胎等级加成
    double tireTypeBonus = _getTireTypeBonus();

    return performanceScore + tireWidthScore + tireTypeBonus;
  }

  // 获取轮胎等级加成
  double _getTireTypeBonus() {
    if (tireType.toUpperCase() == 'SS') {
      // SS软胎：+160~220，取平均值190
      return 190;
    } else {
      // SH硬胎：+80~120，取平均值100
      return 100;
    }
  }

  // 从Map创建Car对象
  factory Car.fromMap(Map<String, dynamic> map) {
    return Car(
      id: map['id'],
      name: map['name'],
      horsepower: map['horsepower'].toDouble(),
      weight: map['weight'].toDouble(),
      frontTireWidth:
          map['frontTireWidth']?.toDouble() ??
          map['tireWidth']?.toDouble() ??
          200.0,
      rearTireWidth:
          map['rearTireWidth']?.toDouble() ??
          map['tireWidth']?.toDouble() ??
          200.0,
      tireType: map['tireType'],
      avatarUrl: map['avatarUrl'],
      isMain: map['isMain'] == 1 || map['isMain'] == true,
    );
  }

  // 转换为Map
  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'name': name,
      'horsepower': horsepower,
      'weight': weight,
      'frontTireWidth': frontTireWidth,
      'rearTireWidth': rearTireWidth,
      'tireType': tireType,
      'avatarUrl': avatarUrl,
      'isMain': isMain ? 1 : 0,
    };
  }

  // 复制并修改部分属性
  Car copyWith({
    int? id,
    String? name,
    double? horsepower,
    double? weight,
    double? frontTireWidth,
    double? rearTireWidth,
    String? tireType,
    String? avatarUrl,
    bool? isMain,
  }) {
    return Car(
      id: id ?? this.id,
      name: name ?? this.name,
      horsepower: horsepower ?? this.horsepower,
      weight: weight ?? this.weight,
      frontTireWidth: frontTireWidth ?? this.frontTireWidth,
      rearTireWidth: rearTireWidth ?? this.rearTireWidth,
      tireType: tireType ?? this.tireType,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      isMain: isMain ?? this.isMain,
    );
  }
}
