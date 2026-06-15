package com.haji.racing.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    recordingRepository: RecordingRepository,
) : ViewModel() {

    val allRecordings: StateFlow<List<Recording>> = recordingRepository.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
