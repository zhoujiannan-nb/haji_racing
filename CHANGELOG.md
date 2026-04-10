# 更新日志

## 2026-04-10 更新

### 主要变更

#### 1. 轮胎宽度单位更改
- **从 CM（厘米）改为 MM（毫米）**
- 所有页面中的轮胎宽度显示单位已更新为 MM
- PP分计算逻辑已调整，自动将mm转换为cm进行计算

#### 2. 输入方式改进
- **移除滑动条控件**
- **改用手动输入框**，支持精确输入数值
- 解决了滑动条手抖和无法输入个位数的问题
- 输入框设置为数字键盘类型，方便输入

#### 3. 影响的文件

##### 数据模型
- `lib/models/car.dart`
  - 注释更新：轮胎宽度单位从 cm 改为 mm
  - PP分计算：添加单位转换逻辑 `(tireWidth * 4) / 10`

##### 添加车辆页面
- `lib/pages/add_car_page.dart`
  - 状态变量从 `double` 改为 `String` 类型
  - 添加 getter 方法自动解析字符串为 double
  - 新增 `_buildInputNumberField` 方法替代滑动条
  - 默认值调整：轮胎宽度从 22.5cm 改为 225mm

##### 车辆详情页面
- `lib/pages/car_detail_page.dart`
  - 状态变量从 `double` 改为 `String` 类型
  - 添加 getter 方法自动解析字符串为 double
  - 新增 `_buildEditInputField` 方法替代滑动条
  - 轮胎位置显示单位从 CM 改为 MM

##### 主页
- `lib/pages/home_page.dart`
  - 轮胎宽度显示格式从 `.toStringAsFixed(1) CM` 改为 `.toStringAsFixed(0) MM`

##### 车辆列表页
- `lib/pages/car_list_page.dart`
  - 轮胎宽度显示格式从 `.toStringAsFixed(1) CM` 改为 `.toStringAsFixed(0) MM`

### PP分计算说明

计算公式保持不变，但内部进行了单位转换：

```dart
// 轮胎宽度分（4条轮胎总宽度，从mm转换为cm）
double totalTireWidthCm = (tireWidth * 4) / 10; // mm转cm
double tireWidthScore = (totalTireWidthCm - 20) * 2;
```

**示例：**
- 输入：单条轮胎宽度 225mm
- 计算：4条总宽度 = 225 × 4 = 900mm = 90cm
- 得分：(90 - 20) × 2 = 140分

### 用户体验改进

1. **精确输入**：可以直接输入任意数值，不受滑动条精度限制
2. **支持个位数**：可以输入任何有效数字，包括个位数
3. **数字键盘**：自动弹出数字键盘，方便输入
4. **实时反馈**：输入时PP分会实时更新

### 注意事项

- 数据库中存储的数值单位现在是 **mm（毫米）**
- 如果已有旧数据（单位为cm），需要进行数据迁移或重新录入
- PP分计算会自动处理单位转换，用户无需关心
