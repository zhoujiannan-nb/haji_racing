package com.haji.racing.ui.records

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
) : ViewModel() {

    private val recordingUid: String = checkNotNull(savedStateHandle["recordingUid"])

    val recording: StateFlow<Recording?> = flow {
        emit(recordingRepository.getRecordingByUid(recordingUid))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 速度曲线采样（km/h） */
    val speedSamples: StateFlow<List<Float>> = recording
        .map { rec -> rec?.points?.map { p -> (p.speed * 3.6).toFloat() } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recordingUid)
            onDone()
        }
    }
}
