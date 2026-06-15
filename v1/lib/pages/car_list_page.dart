import 'package:flutter/material.dart';
import '../models/car.dart';
import '../database/database_helper.dart';
import 'car_detail_page.dart';
import 'add_car_page.dart';

class CarListPage extends StatefulWidget {
  const CarListPage({super.key});

  @override
  State<CarListPage> createState() => _CarListPageState();
}

class _CarListPageState extends State<CarListPage> {
  List<Car> _cars = [];
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;

  @override
  void initState() {
    super.initState();
    _loadCars();
  }

  Future<void> _loadCars() async {
    final cars = await _dbHelper.getAllCars();
    setState(() {
      _cars = cars;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A1A1A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1A1A1A),
        elevation: 0,
        title: const Text(
          '车辆列表',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 24,
          ),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add, color: Color(0xFFFF3D00)),
            onPressed: () async {
              await Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const AddCarPage()),
              );
              _loadCars();
            },
          ),
        ],
      ),
      body: _cars.isEmpty ? _buildEmptyState() : _buildCarList(),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.directions_car_outlined,
            size: 80,
            color: Colors.grey[700],
          ),
          const SizedBox(height: 20),
          Text(
            '暂无车辆',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.grey[400],
            ),
          ),
          const SizedBox(height: 10),
          Text(
            '点击右上角添加按钮创建车辆',
            style: TextStyle(fontSize: 14, color: Colors.grey[600]),
          ),
        ],
      ),
    );
  }

  Widget _buildCarList() {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _cars.length,
      itemBuilder: (context, index) {
        final car = _cars[index];
        return _buildCarCard(car);
      },
    );
  }

  Widget _buildCarCard(Car car) {
    final pp = car.calculatePP();
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Colors.grey[900]!, Colors.grey[850]!],
        ),
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: Colors.grey[800]!, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.3),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(15),
          onTap: () async {
            await Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => CarDetailPage(car: car)),
            );
            _loadCars();
          },
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                // 车辆图标
                Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    color: const Color(0xFFFF3D00).withOpacity(0.1),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(
                    Icons.directions_car_filled,
                    size: 40,
                    color: const Color(0xFFFF3D00),
                  ),
                ),
                const SizedBox(width: 16),

                // 车辆信息
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              car.name,
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: car.tireType.toUpperCase() == 'SS'
                                  ? const Color(0xFFFF3D00)
                                  : Colors.grey[700],
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              car.tireType.toUpperCase(),
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          _buildSmallInfo(
                            '马力',
                            '${car.horsepower.toStringAsFixed(0)} HP',
                          ),
                          const SizedBox(width: 12),
                          _buildSmallInfo(
                            '车重',
                            '${car.weight.toStringAsFixed(0)} KG',
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          _buildSmallInfo(
                            '前轮',
                            '${car.frontTireWidth.toStringAsFixed(0)} MM',
                          ),
                          const SizedBox(width: 12),
                          _buildSmallInfo(
                            '后轮',
                            '${car.rearTireWidth.toStringAsFixed(0)} MM',
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          _buildSmallInfo('PP', pp.toStringAsFixed(0)),
                        ],
                      ),
                    ],
                  ),
                ),

                // 箭头图标
                Icon(Icons.chevron_right, color: Colors.grey[600], size: 30),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSmallInfo(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: TextStyle(fontSize: 10, color: Colors.grey[500])),
        Text(
          value,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
      ],
    );
  }
}
