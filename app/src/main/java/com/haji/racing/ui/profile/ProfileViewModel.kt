package com.haji.racing.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.data.local.datastore.AppPreferences
import com.haji.racing.domain.model.ProfileStats
import com.haji.racing.domain.repository.RecordingRepository
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val recordingRepository: RecordingRepository,
    private val trackRepository: TrackRepository,
) : ViewModel() {

    val nickname: StateFlow<String> = preferences.nickname
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Haji")

    /** 统计随记录变化实时刷新 */
    val stats: StateFlow<ProfileStats> = recordingRepository.getAllRecordings()
        .map { list ->
            val completed = list.filter { it.status == "completed" }
            ProfileStats(
                totalRecordings = list.size,
                completedRecordings = completed.size,
                totalDistance = list.sumOf { it.totalDistance },
                totalDurationMs = list.sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) },
                bestTimeMs = completed.minOfOrNull { it.endTime - it.startTime },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileStats())

    val trackCount: StateFlow<Int> = trackRepository.getAllTracks()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setNickname(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { preferences.setNickname(trimmed) }
    }

    fun clearAllRecords() {
        viewModelScope.launch { recordingRepository.clearAll() }
    }

    fun clearAllTracks() {
        viewModelScope.launch {
            trackRepository.getAllTracks().collect { tracks ->
                tracks.forEach { trackRepository.deleteTrack(it.uid) }
            }
        }
    }
}
