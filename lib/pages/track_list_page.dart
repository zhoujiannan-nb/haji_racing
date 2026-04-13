import 'package:flutter/material.dart';
import '../models/track.dart';
import '../database/database_helper.dart';
import 'track_detail_page.dart';

class TrackListPage extends StatefulWidget {
  const TrackListPage({super.key});

  @override
  State<TrackListPage> createState() => _TrackListPageState();
}

class _TrackListPageState extends State<TrackListPage> {
  List<Track> _tracks = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadTracks();
  }

  Future<void> _loadTracks() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final tracks = await DatabaseHelper.instance.getAllTracks();
      setState(() {
        _tracks = tracks;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('加载失败: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('赛道列表'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _tracks.isEmpty
          ? const Center(child: Text('暂无赛道数据'))
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _tracks.length,
              itemBuilder: (context, index) {
                final track = _tracks[index];
                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: Theme.of(context).colorScheme.primary,
                      child: const Icon(Icons.flag, color: Colors.white),
                    ),
                    title: Text(
                      track.name,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 4),
                        Text(
                          '长度: ${(track.length / 1000).toStringAsFixed(2)} km',
                        ),
                        Text(
                          '发布时间: ${_formatDate(track.publishedAt)}',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => TrackDetailPage(track: track),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
    );
  }

  String _formatDate(String dateStr) {
    try {
      final date = DateTime.parse(dateStr);
      return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
    } catch (e) {
      return dateStr;
    }
  }
}
