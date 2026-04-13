import 'package:flutter/material.dart';
import 'pages/home_page.dart';
import 'services/track_import_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 从JSON文件导入赛道数据
  final importService = TrackImportService();
  await importService.importTracksFromAssets();

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Haji Racing',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFFF3D00),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}
