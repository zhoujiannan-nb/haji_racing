package com.haji.racing.core.kalman

class KalmanFilter(
    private val processNoise: Float = 0.01f,
    private val measurementNoise: Float = 1f,
    private val initialState: Float = 0f,
    private val initialVariance: Float = 1f,
) {
    private var x = initialState
    private var p = initialVariance

    fun update(measurement: Float): Float {
        val k = p / (p + measurementNoise)
        x = x + k * (measurement - x)
        p = (1f - k) * p + processNoise
        return x
    }

    fun reset(state: Float = 0f) {
        x = state
        p = initialVariance
    }
}

class KalmanFilter3D(
    processNoise: Float = 0.01f,
    measurementNoise: Float = 1f,
) {
    val filterX = KalmanFilter(processNoise, measurementNoise)
    val filterY = KalmanFilter(processNoise, measurementNoise)
    val filterZ = KalmanFilter(processNoise, measurementNoise)

    fun update(x: Float, y: Float, z: Float): FloatArray {
        return floatArrayOf(
            filterX.update(x),
            filterY.update(y),
            filterZ.update(z),
        )
    }

    fun reset() {
        filterX.reset()
        filterY.reset()
        filterZ.reset()
    }
}
