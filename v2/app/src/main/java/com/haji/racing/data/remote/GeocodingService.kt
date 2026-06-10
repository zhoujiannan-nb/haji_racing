package com.haji.racing.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 通过高德 Web服务 API 进行地名搜索与地理编码。
 * 不再依赖 Android 搜索 SDK，只需一个开通了「Web服务」的 Key。
 */
object GeocodingService {

    /**
     * 高德 Web服务 API Key。
     * 请到高德开放平台 -> 应用管理 -> 添加一个「Web服务」类型的 Key 填入此处。
     * 与 AndroidManifest 中的 Android SDK Key 不是同一个！
     */
    private const val WEB_API_KEY = "1ed9c4c5458688fca1141801bdbf8a21"

    private const val BASE_URL = "https://restapi.amap.com/v3"
    private const val TAG = "GeocodingService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /** 经纬度点（替代高德 SDK 的 LatLonPoint） */
    data class GeoPoint(val latitude: Double, val longitude: Double)

    /** 搜索结果项 */
    data class SearchResult(
        val name: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
    )

    /**
     * 输入提示（相当于 Inputtips），输入文字实时联想。
     * @param query 搜索关键词
     * @param city 城市名称或城市编码（可选）
     */
    suspend fun searchSuggestions(
        query: String,
        city: String = "",
    ): Result<List<SearchResult>> = runCatching {
        val url = buildString {
            append("$BASE_URL/assistant/inputtips")
            append("?key=$WEB_API_KEY")
            append("&keywords=${java.net.URLEncoder.encode(query, "UTF-8")}")
            append("&output=json")
            if (city.isNotBlank()) append("&city=${java.net.URLEncoder.encode(city, "UTF-8")}")
        }

        val json = httpGet(url)
        val result = parseInputtipsResponse(json)
        android.util.Log.d(TAG, "searchSuggestions: query='$query', count=${result.size}")
        result
    }

    /**
     * 地理编码（地名→经纬度），用户按搜索键时调用。
     * @param address 地址名称
     * @param city 城市名称或城市编码（可选）
     */
    suspend fun geocode(
        address: String,
        city: String = "",
    ): Result<List<SearchResult>> = runCatching {
        val url = buildString {
            append("$BASE_URL/geocode/geo")
            append("?key=$WEB_API_KEY")
            append("&address=${java.net.URLEncoder.encode(address, "UTF-8")}")
            append("&output=json")
            if (city.isNotBlank()) append("&city=${java.net.URLEncoder.encode(city, "UTF-8")}")
        }

        val json = httpGet(url)
        val result = parseGeocodeResponse(json)
        android.util.Log.d(TAG, "geocode: address='$address', count=${result.size}")
        result
    }

    // ── 内部实现 ──────────────────────────────────────────────

    private suspend fun httpGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HajiRacing/1.0")
            .get()
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val body = response.body()?.string()
            if (!response.isSuccessful || body == null) {
                throw RuntimeException("HTTP ${response.code()}: $body")
            }
            body
        }
    }

    /**
     * 解析 inputtips 响应：
     * {"status":"1","info":"OK","tips":[{"name":"xxx","address":"xxx","location":"lng,lat"}]}
     */
    private fun parseInputtipsResponse(json: String): List<SearchResult> {
        val root = JSONObject(json)
        checkStatus(root)
        val tips = root.optJSONArray("tips") ?: return emptyList()
        val results = mutableListOf<SearchResult>()
        for (i in 0 until tips.length()) {
            val tip = tips.getJSONObject(i)
            val location = tip.optString("location", "")
            if (location.isBlank()) continue
            val parts = location.split(",")
            if (parts.size != 2) continue
            results.add(
                SearchResult(
                    name = tip.optString("name", ""),
                    address = tip.optString("address", null as String?),
                    latitude = parts[1].toDouble(),
                    longitude = parts[0].toDouble(),
                )
            )
        }
        return results
    }

    /**
     * 解析地理编码响应：
     * {"status":"1","info":"OK","geocodes":[{"formatted_address":"xxx","location":"lng,lat"}]}
     */
    private fun parseGeocodeResponse(json: String): List<SearchResult> {
        val root = JSONObject(json)
        checkStatus(root)
        val geocodes = root.optJSONArray("geocodes") ?: return emptyList()
        val results = mutableListOf<SearchResult>()
        for (i in 0 until geocodes.length()) {
            val geo = geocodes.getJSONObject(i)
            val location = geo.optString("location", "")
            if (location.isBlank()) continue
            val parts = location.split(",")
            if (parts.size != 2) continue
            results.add(
                SearchResult(
                    name = geo.optString("formatted_address", ""),
                    address = null,
                    latitude = parts[1].toDouble(),
                    longitude = parts[0].toDouble(),
                )
            )
        }
        return results
    }

    private fun checkStatus(root: JSONObject) {
        val status = root.optString("status")
        val info = root.optString("info", "")
        if (status != "1") {
            throw RuntimeException("高德API返回错误: $info (code=${root.optString("infocode")})")
        }
    }
}
