package com.haji.racing.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.core.gps.GpsTracker
import com.haji.racing.data.local.datastore.AppPreferences
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    trackRepository: TrackRepository,
    private val preferences: AppPreferences,
    private val gpsTracker: GpsTracker,
) : ViewModel() {

    /** 0 = 赛道跟跑, 1 = 自由跑 */
    private val _mode = MutableStateFlow(0)
    val mode: StateFlow<Int> = _mode.asStateFlow()

    val tracks: Flow<List<Track>> = trackRepository.getAllTracks()
    val allTracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTrackUid: StateFlow<String?> = preferences.selectedTrackUid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedTrack: StateFlow<Track?> = selectedTrackUid
        .map { uid -> uid?.let { trackRepository.getTrackByUid(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** GPS 预热：首页持续收集定位（1Hz），用于“GPS 已就绪”提示 */
    val lastGps: StateFlow<GpsPoint?> = gpsTracker.lastPoint

    init {
        // 进入首页即开始 GPS 预热（记录服务启动时会复用同一个 tracker）
        gpsTracker.startTracking()
    }

    fun setMode(mode: Int) {
        _mode.value = mode
    }

    fun selectTrack(track: Track) {
        viewModelScope.launch {
            preferences.setSelectedTrackUid(track.uid)
            gpsTracker.startTracking()
        }
    }

    fun clearSelectedTrack() {
        viewModelScope.launch { preferences.setSelectedTrackUid(null) }
    }
}
