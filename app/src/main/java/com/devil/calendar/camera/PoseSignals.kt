package com.devil.calendar.camera

import com.devil.calendar.domain.coach.CoachConfig
import com.google.mlkit.vision.pose.PoseLandmark

enum class CameraView { FRONT, SIDE, UNKNOWN }

/** Landmarks the movement needs, per primary joint. */
data class PoseSample(
    val points: Map<Int, Pair<Float, Float>>,
    val likelihoods: Map<Int, Float>,
    val nowMillis: Long,
)

/**
 * Decides whether the phone is seeing you head-on or side-on, then picks a
 * signal that actually moves in that view.
 *
 * Head-on, the elbow angle barely changes because the arms point at the lens.
 * The ratio of shoulder width to torso length separates the two cases, and in a
 * front view the vertical gap between wrists and shoulders is the reliable
 * signal - it shrinks as the chest goes down.
 */
class SignalExtractor(private val config: CoachConfig) {

    private var viewSmooth: Float? = null
    var view: CameraView = CameraView.UNKNOWN
        private set

    /** Higher means deeper into the movement. */
    fun signal(sample: PoseSample): Float? {
        val p = sample.points
        val shoulderL = p[PoseLandmark.LEFT_SHOULDER]
        val shoulderR = p[PoseLandmark.RIGHT_SHOULDER]
        val hipL = p[PoseLandmark.LEFT_HIP]
        val hipR = p[PoseLandmark.RIGHT_HIP]

        if (shoulderL != null && shoulderR != null && hipL != null && hipR != null) {
            val shoulderWidth = kotlin.math.abs(shoulderR.first - shoulderL.first)
            val shoulderMidY = (shoulderL.second + shoulderR.second) / 2f
            val hipMidY = (hipL.second + hipR.second) / 2f
            val torso = kotlin.math.abs(hipMidY - shoulderMidY)
            if (torso > 1e-3f) {
                val ratio = shoulderWidth / torso
                viewSmooth = PoseMath.OneEuro().let { _ -> ratio } // raw; smoothed below
                view = if (ratio >= FRONT_RATIO) CameraView.FRONT else CameraView.SIDE
            }
        }

        return when (config.primaryJoint.lowercase()) {
            "knee", "hip", "shoulder" -> angleSignal(sample)
            else -> if (view == CameraView.FRONT) depthSignal(sample) else angleSignal(sample)
        }
    }

    /** Side view: elbow angle, larger when the arm bends. */
    private fun angleSignal(sample: PoseSample): Float? {
        val angle = when (config.primaryJoint.lowercase()) {
            "knee" -> armOrLegAngle(sample, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
            "hip" -> armOrLegAngle(sample, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ANKLE)
            "shoulder" -> armOrLegAngle(sample, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_WRIST,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_WRIST)
            else -> armOrLegAngle(sample, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
                PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        } ?: return null
        // Invert so the signal grows as the joint closes.
        return 180f - angle
    }

    /** Front view: (wristY - shoulderY) / shoulderWidth, inverted. */
    private fun depthSignal(sample: PoseSample): Float? {
        val p = sample.points
        val wrists = listOfNotNull(p[PoseLandmark.LEFT_WRIST], p[PoseLandmark.RIGHT_WRIST])
        val shoulders = listOfNotNull(p[PoseLandmark.LEFT_SHOULDER], p[PoseLandmark.RIGHT_SHOULDER])
        if (wrists.isEmpty() || shoulders.size < 2) return null
        val shoulderWidth = kotlin.math.abs(shoulders[1].first - shoulders[0].first)
        if (shoulderWidth < 1e-3f) return null
        val wristY = wrists.map { it.second }.average().toFloat()
        val shoulderY = shoulders.map { it.second }.average().toFloat()
        // Wrists are below shoulders; as the chest drops the gap closes.
        return -(wristY - shoulderY) / shoulderWidth
    }

    private fun armOrLegAngle(
        sample: PoseSample,
        la: Int, lb: Int, lc: Int,
        ra: Int, rb: Int, rc: Int,
    ): Float? {
        val left = triple(sample, la, lb, lc)
        val right = triple(sample, ra, rb, rc)
        // Prefer the arm the camera can actually see; do not average a good
        // reading with a guess.
        val leftScore = listOf(la, lb, lc).sumOf { (sample.likelihoods[it] ?: 0f).toDouble() }
        val rightScore = listOf(ra, rb, rc).sumOf { (sample.likelihoods[it] ?: 0f).toDouble() }
        return when {
            left != null && right != null -> if (leftScore >= rightScore) left else right
            else -> left ?: right
        }
    }

    private fun triple(sample: PoseSample, a: Int, b: Int, c: Int): Float? {
        val pa = sample.points[a] ?: return null
        val pb = sample.points[b] ?: return null
        val pc = sample.points[c] ?: return null
        return PoseMath.angle(pa.first, pa.second, pb.first, pb.second, pc.first, pc.second)
    }

    private companion object {
        /** Above this, the shoulders are wide relative to the torso: head-on. */
        const val FRONT_RATIO = 0.55f
    }
}
