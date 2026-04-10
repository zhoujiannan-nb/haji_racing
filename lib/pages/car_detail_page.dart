import 'package:flutter/material.dart';
import '../models/car.dart';
import '../database/database_helper.dart';

class CarDetailPage extends StatefulWidget {
  final Car car;

  const CarDetailPage({super.key, required this.car});

  @override
  State<CarDetailPage> createState() => _CarDetailPageState();
}

class _CarDetailPageState extends State<CarDetailPage> {
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;

  late String _name;
  late TextEditingController _horsepowerController;
  late TextEditingController _weightController;
  late TextEditingController _frontTireWidthController;
  late TextEditingController _rearTireWidthController;
  late String _tireType;

  @override
  void initState() {
    super.initState();
    _name = widget.car.name;
    _horsepowerController = TextEditingController(
      text: widget.car.horsepower.toStringAsFixed(0),
    );
    _weightController = TextEditingController(
      text: widget.car.weight.toStringAsFixed(0),
    );
    _frontTireWidthController = TextEditingController(
      text: widget.car.frontTireWidth.toStringAsFixed(0),
    );
    _rearTireWidthController = TextEditingController(
      text: widget.car.rearTireWidth.toStringAsFixed(0),
    );
    _tireType = widget.car.tireType;
  }

  @override
  void dispose() {
    _horsepowerController.dispose();
    _weightController.dispose();
    _frontTireWidthController.dispose();
    _rearTireWidthController.dispose();
    super.dispose();
  }

  double get _horsepower =>
      double.tryParse(_horsepowerController.text) ?? widget.car.horsepower;
  double get _weight =>
      double.tryParse(_weightController.text) ?? widget.car.weight;
  double get _frontTireWidth =>
      double.tryParse(_frontTireWidthController.text) ??
      widget.car.frontTireWidth;
  double get _rearTireWidth =>
      double.tryParse(_rearTireWidthController.text) ??
      widget.car.rearTireWidth;

  // 实时计算PP分
  double _calculateCurrentPP() {
    final tempCar = Car(
      name: _name,
      horsepower: _horsepower,
      weight: _weight,
      frontTireWidth: _frontTireWidth,
      rearTireWidth: _rearTireWidth,
      tireType: _tireType,
    );
    return tempCar.calculatePP();
  }

  @override
  Widget build(BuildContext context) {
    final currentPP = _calculateCurrentPP();

    return Scaffold(
      backgroundColor: const Color(0xFF1A1A1A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1A1A1A),
        elevation: 0,
        title: const Text(
          '车辆详情',
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
          // 设为主车辆按钮
          if (!widget.car.isMain)
            IconButton(
              icon: const Icon(Icons.star_border, color: Colors.yellow),
              onPressed: _setAsMainCar,
              tooltip: '设为主车辆',
            ),
          if (widget.car.isMain)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: Icon(Icons.star, color: Colors.yellow),
            ),
          IconButton(
            icon: const Icon(Icons.delete, color: Colors.red),
            onPressed: _deleteCar,
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            // 虚拟车辆形象居中
            _buildCarAvatar(),

            // 马力和车重在虚拟车辆中间
            _buildCenterStats(),

            // 轮胎参数
            _buildTireInfo(),

            // 编辑表单
            _buildEditForm(),

            // PP分实时显示
            _buildLivePPScore(currentPP),
          ],
        ),
      ),
    );
  }

  Widget _buildCarAvatar() {
    return Container(
      height: 250,
      margin: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Colors.grey[900]!, Colors.grey[850]!],
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: const Color(0xFFFF3D00).withOpacity(0.3),
          width: 2,
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFFF3D00).withOpacity(0.2),
            blurRadius: 20,
            spreadRadius: 2,
          ),
        ],
      ),
      child: Stack(
        children: [
          Center(
            child: Icon(
              Icons.directions_car_filled,
              size: 120,
              color: const Color(0xFFFF3D00),
            ),
          ),
          // 装饰元素
          Positioned(
            top: 20,
            left: 20,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: const Color(0xFFFF3D00),
                borderRadius: BorderRadius.circular(15),
              ),
              child: Text(
                _tireType.toUpperCase(),
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 12,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCenterStats() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 40),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildStatBox(
            '马力',
            '${_horsepower.toStringAsFixed(0)} HP',
            Icons.speed,
          ),
          _buildStatBox(
            '车重',
            '${_weight.toStringAsFixed(0)} KG',
            Icons.monitor_weight,
          ),
        ],
      ),
    );
  }

  Widget _buildStatBox(String label, String value, IconData icon) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 15),
      decoration: BoxDecoration(
        color: Colors.grey[900],
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: Colors.grey[800]!, width: 1),
      ),
      child: Column(
        children: [
          Icon(icon, color: const Color(0xFFFF3D00), size: 30),
          const SizedBox(height: 8),
          Text(
            value,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
        ],
      ),
    );
  }

  Widget _buildTireInfo() {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          // 前轮
          Row(
            children: [
              Expanded(child: _buildTirePosition('左前', _frontTireWidth)),
              const SizedBox(width: 12),
              Expanded(child: _buildTirePosition('右前', _frontTireWidth)),
            ],
          ),
          const SizedBox(height: 12),
          // 后轮
          Row(
            children: [
              Expanded(child: _buildTirePosition('左后', _rearTireWidth)),
              const SizedBox(width: 12),
              Expanded(child: _buildTirePosition('右后', _rearTireWidth)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTirePosition(String position, double width) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.grey[900],
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.grey[800]!, width: 1),
      ),
      child: Column(
        children: [
          Text(
            position,
            style: TextStyle(fontSize: 12, color: Colors.grey[500]),
          ),
          const SizedBox(height: 4),
          Text(
            '${width.toStringAsFixed(0)} MM',
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEditForm() {
    return Container(
      margin: const EdgeInsets.all(20),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.grey[900],
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: Colors.grey[800]!, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '编辑参数',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 20),

          // 车辆名称
          TextFormField(
            initialValue: _name,
            style: const TextStyle(color: Colors.white),
            decoration: InputDecoration(
              labelText: '车辆名称',
              labelStyle: TextStyle(color: Colors.grey[500]),
              filled: true,
              fillColor: Colors.grey[850],
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: BorderSide.none,
              ),
            ),
            onChanged: (value) {
              setState(() {
                _name = value;
              });
            },
          ),

          const SizedBox(height: 16),

          // 马力输入
          _buildEditInputField(
            label: '马力 (HP)',
            controller: _horsepowerController,
            onChanged: (value) {
              setState(() {});
            },
          ),

          const SizedBox(height: 16),

          // 车重输入
          _buildEditInputField(
            label: '车重 (KG)',
            controller: _weightController,
            onChanged: (value) {
              setState(() {});
            },
          ),

          const SizedBox(height: 16),

          // 前轮轮胎宽度输入
          _buildEditInputField(
            label: '前轮轮胎宽度 (MM)',
            controller: _frontTireWidthController,
            onChanged: (value) {
              setState(() {});
            },
          ),

          const SizedBox(height: 16),

          // 后轮轮胎宽度输入
          _buildEditInputField(
            label: '后轮轮胎宽度 (MM)',
            controller: _rearTireWidthController,
            onChanged: (value) {
              setState(() {});
            },
          ),

          const SizedBox(height: 16),

          // 轮胎类型选择
          _buildTireTypeSelector(),

          const SizedBox(height: 20),

          // 保存按钮
          SizedBox(
            width: double.infinity,
            height: 50,
            child: ElevatedButton(
              onPressed: _saveChanges,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFF3D00),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              child: const Text(
                '保存修改',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEditSlider({
    required String label,
    required double value,
    required double min,
    required double max,
    required int divisions,
    String Function(double)? format,
    required void Function(double) onChanged,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              label,
              style: const TextStyle(fontSize: 14, color: Colors.white),
            ),
            Text(
              format != null ? format(value) : value.toStringAsFixed(0),
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Color(0xFFFF3D00),
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        SliderTheme(
          data: SliderTheme.of(context).copyWith(
            activeTrackColor: const Color(0xFFFF3D00),
            inactiveTrackColor: Colors.grey[800],
            thumbColor: const Color(0xFFFF3D00),
            overlayColor: const Color(0xFFFF3D00).withOpacity(0.2),
          ),
          child: Slider(
            value: value,
            min: min,
            max: max,
            divisions: divisions,
            onChanged: onChanged,
          ),
        ),
      ],
    );
  }

  Widget _buildEditInputField({
    required String label,
    required TextEditingController controller,
    required void Function(String) onChanged,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 14, color: Colors.white)),
        const SizedBox(height: 8),
        TextField(
          keyboardType: TextInputType.number,
          controller: controller,
          style: const TextStyle(color: Colors.white, fontSize: 18),
          decoration: InputDecoration(
            filled: true,
            fillColor: Colors.grey[850],
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: const BorderSide(color: Color(0xFFFF3D00), width: 2),
            ),
          ),
          onChanged: onChanged,
        ),
      ],
    );
  }

  Widget _buildTireTypeSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('轮胎等级', style: TextStyle(fontSize: 14, color: Colors.white)),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(child: _buildTireTypeOption('SH', '硬胎')),
            const SizedBox(width: 12),
            Expanded(child: _buildTireTypeOption('SS', '软胎')),
          ],
        ),
      ],
    );
  }

  Widget _buildTireTypeOption(String type, String name) {
    final isSelected = _tireType == type;
    return GestureDetector(
      onTap: () {
        setState(() {
          _tireType = type;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFFFF3D00) : Colors.grey[850],
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: isSelected ? const Color(0xFFFF3D00) : Colors.grey[800]!,
            width: 2,
          ),
        ),
        child: Column(
          children: [
            Text(
              type,
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: isSelected ? Colors.white : Colors.grey[400],
              ),
            ),
            const SizedBox(height: 2),
            Text(
              name,
              style: TextStyle(
                fontSize: 11,
                color: isSelected ? Colors.white : Colors.grey[500],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLivePPScore(double pp) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [const Color(0xFFFF3D00), const Color(0xFFFF6E40)],
        ),
        borderRadius: BorderRadius.circular(15),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFFF3D00).withOpacity(0.4),
            blurRadius: 15,
            spreadRadius: 2,
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const Text(
            '实时PP分数',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          Text(
            pp.toStringAsFixed(0),
            style: const TextStyle(
              fontSize: 36,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _setAsMainCar() async {
    await _dbHelper.setMainCar(widget.car.id!);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('已设为主车辆！'),
          backgroundColor: Color(0xFFFF3D00),
        ),
      );
      Navigator.pop(context, true);
    }
  }

  Future<void> _saveChanges() async {
    final updatedCar = widget.car.copyWith(
      name: _name,
      horsepower: _horsepower,
      weight: _weight,
      frontTireWidth: _frontTireWidth,
      rearTireWidth: _rearTireWidth,
      tireType: _tireType,
    );

    await _dbHelper.updateCar(updatedCar);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('修改已保存！'),
          backgroundColor: Color(0xFFFF3D00),
        ),
      );
      Navigator.pop(context, true);
    }
  }

  Future<void> _deleteCar() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: Colors.grey[900],
        title: const Text('删除车辆', style: TextStyle(color: Colors.white)),
        content: const Text(
          '确定要删除这辆车吗？此操作不可撤销。',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消', style: TextStyle(color: Colors.white70)),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      await _dbHelper.deleteCar(widget.car.id!);
      Navigator.pop(context, true);
    }
  }
}
