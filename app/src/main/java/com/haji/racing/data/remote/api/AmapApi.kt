package com.haji.racing.data.remote.api

import com.haji.racing.data.remote.dto.PoiSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AmapApi {
    @GET("v5/place/text")
    suspend fun searchPoi(
        @Query("key") key: String,
        @Query("keywords") keywords: String,
        @Query("location") location: String? = null, // "lng,lat" 格式
        @Query("sortrule") sortrule: String? = null,  // "distance" 按距离排序
        @Query("radius") radius: Int? = null,         // 搜索半径（米）
        @Query("region") region: String? = null,
        @Query("city_limit") cityLimit: Boolean = false,
    ): PoiSearchResponse
}
