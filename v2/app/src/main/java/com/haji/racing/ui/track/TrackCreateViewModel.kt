package com.haji.racing.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TrackCreateViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
) : ViewModel() {

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun saveTrack(
        name: String,
        description: String,
        creatorName: String,
        startFencePoints: List<FencePoint>,
        endFencePoints: List<FencePoint>,
    ) {
        viewModelScope.launch {
            val track = Track(
                uid = UUID.randomUUID().toString(),
                name = name,
                description = description,
                creatorName = creatorName,
                startFencePoints = startFencePoints,
                endFencePoints = endFencePoints,
                totalDistance = 0.0,
                type = "user",
            )
            trackRepository.saveTrack(track)
            _isSaved.value = true
        }
    }
}
