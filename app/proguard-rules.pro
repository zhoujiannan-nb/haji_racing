# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.amap.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.haji.racing.data.local.db.entity.** { *; }

# Gson 序列化/反序列化（围栏点、POI 响应）
-keep class com.haji.racing.domain.model.** { *; }
-keep class com.haji.racing.data.remote.api.** { *; }

# Retrofit 服务接口
-keep interface com.haji.racing.data.remote.api.AmapApi { *; }
