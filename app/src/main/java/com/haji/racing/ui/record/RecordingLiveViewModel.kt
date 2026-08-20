package com.haji.racing.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.core.gps.GpsTracker
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.TrackRepository
import com.haji.racing.service.RecordingData
import com.haji.racing.service.RecordingResult
import com.haji.racing.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecordingLiveViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    gpsTracker: GpsTracker,
) : ViewModel() {

    val data: StateFlow<RecordingData> = RecordingService.recordingData
    val liveRoute: StateFlow<List<GpsPoint>> = RecordingService.liveRoute
    val result: StateFlow<RecordingResult?> = RecordingService.result
    val lastGps: StateFlow<GpsPoint?> = gpsTracker.lastPoint

    /** 当前赛道（自由跑为 null） */
    val track: StateFlow<Track?> = RecordingService.recordingData
        .map { d ->
            d.trackUid?.takeIf { it.isNotEmpty() }
                ?.let { trackRepository.getTrackByUid(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
