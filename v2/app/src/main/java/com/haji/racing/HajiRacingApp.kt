package com.haji.racing

import android.app.Application
import com.amap.api.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HajiRacingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 高德地图SDK隐私合规设置
        // 必须在调用任何SDK接口前设置
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
    }
}
