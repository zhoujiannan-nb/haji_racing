package com.haji.racing.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
) : ViewModel() {

    private val _selectedTrackUid = MutableStateFlow<String?>(null)
    val selectedTrackUid: StateFlow<String?> = _selectedTrackUid

    val tracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack: StateFlow<Track?> = _selectedTrack

    fun selectTrack(uid: String) {
        _selectedTrackUid.value = uid
        viewModelScope.launch {
            _selectedTrack.value = trackRepository.getTrackByUid(uid)
        }
    }
}
