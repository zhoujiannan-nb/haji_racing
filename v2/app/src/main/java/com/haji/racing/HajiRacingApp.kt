package com.haji.racing

import android.app.Application
import android.util.Log
import com.amap.api.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HajiRacingApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 高德地图SDK隐私合规设置
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        Log.d("HajiRacingApp", "MapsInitializer privacy done")
    }
}
