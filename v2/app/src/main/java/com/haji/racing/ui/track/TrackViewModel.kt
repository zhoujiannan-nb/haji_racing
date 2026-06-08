package com.haji.racing.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
) : ViewModel() {

    val allTracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val officialTracks: StateFlow<List<Track>> = trackRepository.getTracksByType("official")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customTracks: StateFlow<List<Track>> = trackRepository.getTracksByType("custom")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTrack(track: Track) {
        viewModelScope.launch { trackRepository.saveTrack(track) }
    }

    fun deleteTrack(uid: String) {
        viewModelScope.launch { trackRepository.deleteTrack(uid) }
    }

    suspend fun getTrackByUid(uid: String): Track? = trackRepository.getTrackByUid(uid)
}
