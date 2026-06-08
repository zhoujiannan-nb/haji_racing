package com.haji.racing.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AccelData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
)

@Singleton
class AccelerometerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val _accelFlow = Channel<AccelData>(Channel.CONFLATED)
    val accelFlow: Flow<AccelData> = _accelFlow.receiveAsFlow()

    fun startListening() {
        accelerometer?.let {
            // 25Hz = 40000 microseconds
            sensorManager.registerListener(sensorListener, it, 40000)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                _accelFlow.trySend(
                    AccelData(
                        x = event.values[0],
                        y = event.values[1],
                        z = event.values[2],
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun getLinearAcceleration(rawAccel: FloatArray, gravity: FloatArray): FloatArray {
        return floatArrayOf(
            rawAccel[0] - gravity[0],
            rawAccel[1] - gravity[1],
            rawAccel[2] - gravity[2],
        )
    }

    fun getGValue(x: Float, y: Float, z: Float): Float {
        return kotlin.math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
    }
}
