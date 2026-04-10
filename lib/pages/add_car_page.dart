import 'package:flutter/material.dart';
import '../models/car.dart';
import '../database/database_helper.dart';

class AddCarPage extends StatefulWidget {
  const AddCarPage({super.key});

  @override
  State<AddCarPage> createState() => _AddCarPageState();
}

class _AddCarPageState extends State<AddCarPage> {
  final _formKey = GlobalKey<FormState>();
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;

  String _name = '';
  TextEditingController _horsepowerController = TextEditingController(
    text: '300',
  );
  TextEditingController _weightController = TextEditingController(text: '1500');
  TextEditingController _frontTireWidthController = TextEditingController(
    text: '225',
  );
  TextEditingController _rearTireWidthController = TextEditingController(
    text: '225',
  );
  String _tireType = 'SH';

  @override
  void dispose() {
    _horsepowerController.dispose();
    _weightController.dispose();
    _frontTireWidthController.dispose();
    _rearTireWidthController.dispose();
    super.dispose();
  }

  double get _horsepower => double.tryParse(_horsepowerController.text) ?? 300;
  double get _weight => double.tryParse(_weightController.text) ?? 1500;
  double get _frontTireWidth =>
      double.tryParse(_frontTireWidthController.text) ?? 225;
  double get _rearTireWidth =>
      double.tryParse(_rearTireWidthController.text) ?? 225;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A1A1A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1A1A1A),
        elevation: 0,
        title: const Text(
          '添加车辆',
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
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 车辆名称
              _buildInputField(
                label: '车辆名称',
                hintText: '例如：GTR R35',
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return '请输入车辆名称';
                  }
                  return null;
                },
                onSaved: (value) => _name = value!,
              ),

              const SizedBox(height: 20),

              // 马力
              _buildInputNumberField(
                label: '马力 (HP)',
                controller: _horsepowerController,
                hintText: '例如：300',
              ),

              const SizedBox(height: 20),

              // 车重
              _buildInputNumberField(
                label: '车重 (KG)',
                controller: _weightController,
                hintText: '例如：1500',
              ),

              const SizedBox(height: 20),

              // 前轮轮胎宽度
              _buildInputNumberField(
                label: '前轮轮胎宽度 (MM)',
                controller: _frontTireWidthController,
                hintText: '例如：225',
              ),

              const SizedBox(height: 20),

              // 后轮轮胎宽度
              _buildInputNumberField(
                label: '后轮轮胎宽度 (MM)',
                controller: _rearTireWidthController,
                hintText: '例如：225',
              ),

              const SizedBox(height: 20),

              // 轮胎类型
              _buildTireTypeSelector(),

              const SizedBox(height: 30),

              // PP分预览
              _buildPPPreview(),

              const SizedBox(height: 30),

              // 保存按钮
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: _saveCar,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFF3D00),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  child: const Text(
                    '保存车辆',
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
        ),
      ),
    );
  }

  Widget _buildInputField({
    required String label,
    required String hintText,
    required String? Function(String?) validator,
    required void Function(String?) onSaved,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        const SizedBox(height: 8),
        TextFormField(
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            hintText: hintText,
            hintStyle: TextStyle(color: Colors.grey[600]),
            filled: true,
            fillColor: Colors.grey[900],
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: const BorderSide(color: Color(0xFFFF3D00), width: 2),
            ),
          ),
          validator: validator,
          onSaved: onSaved,
        ),
      ],
    );
  }

  Widget _buildSliderField({
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
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
            Text(
              format != null ? format(value) : value.toStringAsFixed(0),
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Color(0xFFFF3D00),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
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

  Widget _buildInputNumberField({
    required String label,
    required TextEditingController controller,
    required String hintText,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          keyboardType: TextInputType.number,
          controller: controller,
          style: const TextStyle(color: Colors.white, fontSize: 18),
          decoration: InputDecoration(
            hintText: hintText,
            hintStyle: TextStyle(color: Colors.grey[600]),
            filled: true,
            fillColor: Colors.grey[900],
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: const BorderSide(color: Color(0xFFFF3D00), width: 2),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTireTypeSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '轮胎等级',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(child: _buildTireTypeOption('SH', '硬胎', '+80~120')),
            const SizedBox(width: 12),
            Expanded(child: _buildTireTypeOption('SS', '软胎', '+160~220')),
          ],
        ),
      ],
    );
  }

  Widget _buildTireTypeOption(String type, String name, String bonus) {
    final isSelected = _tireType == type;
    return GestureDetector(
      onTap: () {
        setState(() {
          _tireType = type;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFFFF3D00) : Colors.grey[900],
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
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: isSelected ? Colors.white : Colors.grey[400],
              ),
            ),
            const SizedBox(height: 4),
            Text(
              name,
              style: TextStyle(
                fontSize: 12,
                color: isSelected ? Colors.white : Colors.grey[500],
              ),
            ),
            const SizedBox(height: 2),
            Text(
              bonus,
              style: TextStyle(
                fontSize: 10,
                color: isSelected
                    ? Colors.white.withOpacity(0.8)
                    : Colors.grey[600],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPPPreview() {
    final tempCar = Car(
      name: _name.isEmpty ? '未命名' : _name,
      horsepower: _horsepower,
      weight: _weight,
      frontTireWidth: _frontTireWidth,
      rearTireWidth: _rearTireWidth,
      tireType: _tireType,
    );
    final pp = tempCar.calculatePP();

    return Container(
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
            '预估PP分数',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          Text(
            pp.toStringAsFixed(0),
            style: const TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  void _saveCar() async {
    if (_formKey.currentState!.validate()) {
      _formKey.currentState!.save();

      final car = Car(
        name: _name,
        horsepower: _horsepower,
        weight: _weight,
        frontTireWidth: _frontTireWidth,
        rearTireWidth: _rearTireWidth,
        tireType: _tireType,
      );

      await _dbHelper.insertCar(car);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('车辆添加成功！'),
            backgroundColor: Color(0xFFFF3D00),
          ),
        );
        Navigator.pop(context);
      }
    }
  }
}
