package com.haji.racing.ui.track

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.STATUS_COMPLETED
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.TrackStats
import com.haji.racing.domain.repository.RecordingRepository
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val trackRepository: TrackRepository,
    recordingRepository: RecordingRepository,
) : ViewModel() {

    private val trackUid: String = checkNotNull(savedStateHandle["trackUid"])

    val track: StateFlow<Track?> = flow {
        emit(trackRepository.getTrackByUid(trackUid))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recordings: StateFlow<List<Recording>> = recordingRepository.getRecordingsForTrack(trackUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 排行榜：仅 completed，按用时升序，前 5（随记录流实时刷新） */
    val leaderboard: StateFlow<List<Recording>> = recordings
        .map { list ->
            list.asSequence()
                .filter { it.status == STATUS_COMPLETED }
                .sortedBy { it.endTime - it.startTime }
                .take(5)
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<TrackStats?> = trackRepository.getTrackStats()
        .map { list -> list.firstOrNull { it.trackUid == trackUid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete() {
        viewModelScope.launch {
            trackRepository.deleteTrack(trackUid)
        }
    }
}
