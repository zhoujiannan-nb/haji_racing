package com.haji.racing.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.TrackStats
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TracksViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** trackUid -> 统计 */
    val statsByUid: StateFlow<Map<String, TrackStats>> = trackRepository.getTrackStats()
        .map { list -> list.associateBy { it.trackUid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun delete(track: Track) {
        viewModelScope.launch {
            trackRepository.deleteTrack(track.uid)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            tracks.value.forEach { trackRepository.deleteTrack(it.uid) }
        }
    }
}
