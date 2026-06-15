package com.haji.racing.ui.trackdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.RecordingPoint
import com.haji.racing.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
) : ViewModel() {

    private val recordingUid: String = savedStateHandle["recordingUid"] ?: ""

    private val _recording = MutableStateFlow<Recording?>(null)
    val recording: StateFlow<Recording?> = _recording.asStateFlow()

    private val _points = MutableStateFlow<List<RecordingPoint>>(emptyList())
    val points: StateFlow<List<RecordingPoint>> = _points.asStateFlow()

    init {
        loadRecording()
    }

    private fun loadRecording() {
        viewModelScope.launch {
            val rec = recordingRepository.getRecordingByUid(recordingUid)
            _recording.value = rec
            rec?.let {
                _points.value = it.points
            }
        }
    }
}
