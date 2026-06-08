package com.haji.racing.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.RecordingRepository
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val recordingRepository: RecordingRepository,
) : ViewModel() {

    private val _selectedTrackUid = MutableStateFlow<String?>(null)
    val selectedTrackUid: StateFlow<String?> = _selectedTrackUid

    val tracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack: StateFlow<Track?> = _selectedTrack

    private val _latestRecording = MutableStateFlow<Recording?>(null)
    val latestRecording: StateFlow<Recording?> = _latestRecording

    private val _refRecordings = MutableStateFlow<List<Recording>>(emptyList())
    val refRecordings: StateFlow<List<Recording>> = _refRecordings

    private val _selectedRefRecordingUid = MutableStateFlow<String?>(null)
    val selectedRefRecordingUid: StateFlow<String?> = _selectedRefRecordingUid

    fun selectTrack(uid: String) {
        _selectedTrackUid.value = uid
        viewModelScope.launch {
            _selectedTrack.value = trackRepository.getTrackByUid(uid)
            _latestRecording.value = recordingRepository.getLatestRecordingForTrack(uid)
            _refRecordings.value = emptyList()
            trackRepository.getTrackByUid(uid)?.let { track ->
                recordingRepository.getRecordingsForTrack(track.uid).first().let { list ->
                    _refRecordings.value = list
                }
            }
        }
    }

    fun selectRefRecording(uid: String?) {
        _selectedRefRecordingUid.value = uid
    }
}
