package com.devil.calendar.walk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Steps and heading from the phone's own sensors.
 *
 * `TYPE_STEP_COUNTER` is cumulative since boot; the detector gives per-step
 * events. If neither exists the walk cannot be tracked and the UI says so
 * rather than pretending.
 */
@Singleton
class StepSensor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: SensorManager? get() = context.getSystemService()

    val hasStepCounter: Boolean
        get() = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    val hasStepDetector: Boolean
        get() = manager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

    private var counterListener: SensorEventListener? = null
    private var detectorListener: SensorEventListener? = null
    private var rotationListener: SensorEventListener? = null

    fun start(
        onCounter: (Long) -> Unit,
        onStep: () -> Unit,
        onHeading: (Double) -> Unit,
    ) {
        stop()
        val m = manager ?: return

        m.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { sensor ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    onCounter(event.values.firstOrNull()?.toLong() ?: return)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            counterListener = listener
            m.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        m.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let { sensor ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) = onStep()
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            detectorListener = listener
            m.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val rotation = m.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: m.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotation?.let { sensor ->
            val listener = object : SensorEventListener {
                private val matrix = FloatArray(9)
                private val orientation = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    SensorManager.getOrientation(matrix, orientation)
                    onHeading(Math.toDegrees(orientation[0].toDouble()))
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            rotationListener = listener
            m.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        val m = manager ?: return
        counterListener?.let { m.unregisterListener(it) }
        detectorListener?.let { m.unregisterListener(it) }
        rotationListener?.let { m.unregisterListener(it) }
        counterListener = null
        detectorListener = null
        rotationListener = null
    }
}
