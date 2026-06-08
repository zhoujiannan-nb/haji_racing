package com.haji.racing.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
) : ViewModel() {

    val allRecordings: StateFlow<List<Recording>> = recordingRepository.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRecording = MutableStateFlow<Recording?>(null)
    val selectedRecording: StateFlow<Recording?> = _selectedRecording

    private val _compareRecordings = MutableStateFlow<List<Recording>>(emptyList())
    val compareRecordings: StateFlow<List<Recording>> = _compareRecordings

    private val _selectedForCompare = MutableStateFlow<Set<String>>(emptySet())
    val selectedForCompare: StateFlow<Set<String>> = _selectedForCompare

    fun selectRecording(uid: String) {
        viewModelScope.launch {
            _selectedRecording.value = recordingRepository.getRecordingByUid(uid)
        }
    }

    fun toggleCompareSelection(uid: String) {
        val current = _selectedForCompare.value.toMutableSet()
        if (current.contains(uid)) {
            current.remove(uid)
        } else if (current.size < 2) {
            current.add(uid)
        }
        _selectedForCompare.value = current
    }

    fun startCompare() {
        viewModelScope.launch {
            val uids = _selectedForCompare.value.toList()
            if (uids.size == 2) {
                val rec1 = recordingRepository.getRecordingByUid(uids[0])
                val rec2 = recordingRepository.getRecordingByUid(uids[1])
                _compareRecordings.value = listOfNotNull(rec1, rec2)
            }
        }
    }

    fun clearCompare() {
        _selectedForCompare.value = emptySet()
        _compareRecordings.value = emptyList()
    }
}
