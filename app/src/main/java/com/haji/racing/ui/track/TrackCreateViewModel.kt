package com.haji.racing.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.core.geo.GeoMath
import com.haji.racing.data.local.datastore.AppPreferences
import com.haji.racing.data.remote.api.AmapApi
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PoiResult(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val city: String,
)

@HiltViewModel
class TrackCreateViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val preferences: AppPreferences,
    private val amapApi: AmapApi,
) : ViewModel() {

    companion object {
        const val AMAP_WEB_KEY = "1ed9c4c5458688fca1141801bdbf8a21"
    }

    val nickname: StateFlow<String> = preferences.nickname
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Haji")

    private val _poiResults = MutableStateFlow<List<PoiResult>>(emptyList())
    val poiResults: StateFlow<List<PoiResult>> = _poiResults.asStateFlow()

    private val _poiSearching = MutableStateFlow(false)
    val poiSearching: StateFlow<Boolean> = _poiSearching.asStateFlow()

    fun searchPoi(keyword: String, center: Pair<Double, Double>? = null) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _poiSearching.value = true
            try {
                val centerStr = center?.let { "${it.second},${it.first}" } // "lng,lat"
                val resp = amapApi.searchPoi(
                    key = AMAP_WEB_KEY,
                    keywords = keyword.trim(),
                    location = centerStr,
                    sortrule = "distance",
                    radius = 30000,
                )
                _poiResults.value = resp.pois?.mapNotNull { poi ->
                    val (lng, lat) = parseLocation(poi.location) ?: return@mapNotNull null
                    PoiResult(
                        name = poi.name.orEmpty(),
                        address = buildString {
                            append(poi.cityname.orEmpty()).append(poi.adname.orEmpty())
                            poi.address?.let { append(" · ").append(it) }
                        },
                        lat = lat,
                        lng = lng,
                        city = poi.cityname.orEmpty(),
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                _poiResults.value = emptyList()
            } finally {
                _poiSearching.value = false
            }
        }
    }

    fun clearPoiResults() {
        _poiResults.value = emptyList()
    }

    fun saveTrack(
        name: String,
        description: String?,
        start: List<FencePoint>,
        end: List<FencePoint>,
    ) {
        val now = System.currentTimeMillis()
        val track = Track(
            uid = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description?.trim()?.ifEmpty { null },
            startFencePoints = start,
            endFencePoints = end,
            totalDistance = GeoMath.estimateTrackLength(start, end),
            creatorName = nickname.value,
            createdAt = now,
            updatedAt = now,
        )
        viewModelScope.launch {
            trackRepository.saveTrack(track)
        }
    }

    private fun parseLocation(location: String?): Pair<Double, Double>? {
        if (location == null) return null
        val parts = location.split(",")
        if (parts.size != 2) return null
        val lng = parts[0].toDoubleOrNull() ?: return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        return lng to lat
    }
}
