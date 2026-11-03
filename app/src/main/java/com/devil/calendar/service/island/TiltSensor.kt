package com.devil.calendar.service.island

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * A very cheap tilt reader for the island's glare.
 *
 * Deliberately minimal: SENSOR_DELAY_UI (not GAME), and values are only emitted
 * when they move by more than [THRESHOLD] so the composable is not recomposed
 * for noise. Stops the moment the island stops.
 */
@Singleton
class TiltSensor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _tilt = MutableStateFlow(0f to 0f)

    /** x and y in -1..1. */
    val tilt: StateFlow<Pair<Float, Float>> = _tilt.asStateFlow()

    private var registered = false
    private var lastX = 0f
    private var lastY = 0f

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val values = event?.values ?: return
            val x = (values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            val y = (values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            if (abs(x - lastX) < THRESHOLD && abs(y - lastY) < THRESHOLD) return
            lastX = x
            lastY = y
            _tilt.value = x to y
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        if (registered) return
        val manager = context.getSystemService<SensorManager>() ?: return
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        registered = true
    }

    fun stop() {
        if (!registered) return
        context.getSystemService<SensorManager>()?.unregisterListener(listener)
        registered = false
        _tilt.value = 0f to 0f
    }

    private companion object {
        const val THRESHOLD = 0.03f
    }
}
