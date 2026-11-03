package com.devil.calendar.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.devil.calendar.domain.coach.CoachConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

/** Normalised landmark positions for the overlay, plus the tracked angle. */
data class SkeletonFrame(
    val points: Map<Int, Pair<Float, Float>>,
    val angle: Float?,
    val valid: Boolean,
)

/**
 * CameraX analyser feeding ML Kit pose detection in stream mode.
 *
 * Frames are dropped rather than queued (`STRATEGY_KEEP_ONLY_LATEST`), nothing
 * is ever written to disk, and the ImageProxy is always closed - a leaked
 * proxy stops the camera within a few seconds.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
class PoseAnalyzer(
    private val config: CoachConfig,
    private val onFrame: (SkeletonFrame) -> Unit,
) {
    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build(),
    )
    val executor = Executors.newSingleThreadExecutor()

    val analyzer = ImageAnalysis.Analyzer { proxy: ImageProxy ->
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return@Analyzer
        }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener(executor) { pose -> onFrame(pose.toSkeleton(proxy.width, proxy.height)) }
            .addOnFailureListener { onFrame(SkeletonFrame(emptyMap(), null, false)) }
            .addOnCompleteListener { proxy.close() }
    }

    fun close() {
        runCatching { detector.close() }
        executor.shutdown()
    }

    private fun Pose.toSkeleton(width: Int, height: Int): SkeletonFrame {
        if (width <= 0 || height <= 0) return SkeletonFrame(emptyMap(), null, false)

        val wanted = listOf(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
        )

        val points = mutableMapOf<Int, Pair<Float, Float>>()
        wanted.forEach { type ->
            val landmark = getPoseLandmark(type) ?: return@forEach
            // Low-confidence landmarks are noise; drop them rather than draw
            // a skeleton that jitters.
            if (landmark.inFrameLikelihood < MIN_LIKELIHOOD) return@forEach
            points[type] = (landmark.position.x / width) to (landmark.position.y / height)
        }

        val angle = when (config.primaryJoint.lowercase()) {
            "knee" -> sideAngle(points, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
            "hip" -> sideAngle(points, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ANKLE)
            "shoulder" -> sideAngle(points, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_WRIST,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_WRIST)
            else -> sideAngle(points, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
                PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        }

        return SkeletonFrame(points = points, angle = angle, valid = angle != null)
    }

    private fun sideAngle(
        points: Map<Int, Pair<Float, Float>>,
        la: Int, lb: Int, lc: Int,
        ra: Int, rb: Int, rc: Int,
    ): Float? {
        val left = tripleAngle(points, la, lb, lc)
        val right = tripleAngle(points, ra, rb, rc)
        // Prefer whichever side is visible; average when both are.
        return when {
            left != null && right != null -> (left + right) / 2f
            else -> left ?: right
        }
    }

    private fun tripleAngle(
        points: Map<Int, Pair<Float, Float>>,
        a: Int, b: Int, c: Int,
    ): Float? {
        val pa = points[a] ?: return null
        val pb = points[b] ?: return null
        val pc = points[c] ?: return null
        return PoseMath.angle(pa.first, pa.second, pb.first, pb.second, pc.first, pc.second)
    }

    private companion object {
        const val MIN_LIKELIHOOD = 0.5f
    }
}
