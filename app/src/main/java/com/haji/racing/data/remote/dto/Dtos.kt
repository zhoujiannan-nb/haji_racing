package com.haji.racing.data.remote.dto

// 高德 POI 搜索相关 DTO

data class PoiSearchResponse(
    val status: String?,
    val info: String?,
    val count: String?,
    val pois: List<PoiItem>?,
)

data class PoiItem(
    val id: String?,
    val name: String?,
    val pname: String?,
    val cityname: String?,
    val adname: String?,
    val address: String?,
    val location: String?, // "lng,lat" 格式 (GCJ-02)
    val type: String?,
    val typecode: String?,
)
