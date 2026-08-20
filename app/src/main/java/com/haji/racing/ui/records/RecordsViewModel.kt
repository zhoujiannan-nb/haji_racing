package com.haji.racing.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.RecordingRepository
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordItem(
    val recording: Recording,
    val trackName: String?,
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    trackRepository: TrackRepository,
) : ViewModel() {

    val records: StateFlow<List<RecordItem>> = combine(
        recordingRepository.getAllRecordings(),
        trackRepository.getAllTracks(),
    ) { recordings, tracks ->
        val nameByUid = tracks.associate { it.uid to it.name }
        recordings.map { rec ->
            RecordItem(
                recording = rec,
                trackName = nameByUid[rec.trackUid],
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(item: RecordItem) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(item.recording.uid)
        }
    }
}
