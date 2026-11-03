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

/** One analysed frame, everything the UI and the counter need. */
data class CoachFrame(
    val points: Map<Int, Pair<Float, Float>>,
    val likelihoods: Map<Int, Float>,
    val imgW: Float,
    val imgH: Float,
    val signal: Float?,
    val view: CameraView,
    val ready: Boolean,
    val fps: Int,
    val nowMillis: Long,
)

/**
 * CameraX → ML Kit in stream mode.
 *
 * Frames are dropped rather than queued, the ImageProxy is closed on every path
 * (a leak stops the camera within seconds), and analysis is capped at 640x480
 * so the detector keeps up on a cheap phone.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
class PoseAnalyzer(
    private val config: CoachConfig,
    private val onFrame: (CoachFrame) -> Unit,
) {
    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build(),
    )
    val executor = Executors.newSingleThreadExecutor()

    private val signals = SignalExtractor(config)
    private val smoother = PoseMath.OneEuro()

    private var readyFrames = 0
    private var frameCount = 0
    private var fpsWindowStart = 0L
    private var fps = 0

    val analyzer = ImageAnalysis.Analyzer { proxy: ImageProxy ->
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return@Analyzer
        }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener(executor) { pose -> onFrame(pose.toFrame(proxy)) }
            .addOnFailureListener {
                // A failed frame is not a zero: the counter holds its state.
                onFrame(emptyFrame())
            }
            .addOnCompleteListener { proxy.close() }
    }

    fun close() {
        runCatching { detector.close() }
        executor.shutdown()
    }

    private fun emptyFrame() = CoachFrame(
        points = emptyMap(),
        likelihoods = emptyMap(),
        imgW = 1f,
        imgH = 1f,
        signal = null,
        view = signals.view,
        ready = false,
        fps = fps,
        nowMillis = System.currentTimeMillis(),
    )

    private fun Pose.toFrame(proxy: ImageProxy): CoachFrame {
        val rotation = proxy.imageInfo.rotationDegrees
        val swapped = rotation == 90 || rotation == 270
        val imgW = (if (swapped) proxy.height else proxy.width).toFloat()
        val imgH = (if (swapped) proxy.width else proxy.height).toFloat()

        val wanted = listOf(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
        )

        val points = mutableMapOf<Int, Pair<Float, Float>>()
        val likelihoods = mutableMapOf<Int, Float>()
        var visible = 0

        wanted.forEach { type ->
            val landmark = getPoseLandmark(type) ?: return@forEach
            val likelihood = landmark.inFrameLikelihood
            likelihoods[type] = likelihood
            if (likelihood < MIN_LIKELIHOOD) return@forEach
            val nx = landmark.position.x / imgW
            val ny = landmark.position.y / imgH
            if (nx < -0.02f || nx > 1.02f || ny < -0.02f || ny > 1.02f) return@forEach
            points[type] = nx to ny
            visible++
        }

        // Readiness: the key landmarks must hold for ten frames before counting
        // starts, so a half-framed body never produces a rep.
        val keyVisible = points[PoseLandmark.LEFT_SHOULDER] != null &&
            points[PoseLandmark.RIGHT_SHOULDER] != null &&
            points[PoseLandmark.LEFT_ELBOW] != null &&
            points[PoseLandmark.RIGHT_ELBOW] != null &&
            points[PoseLandmark.LEFT_WRIST] != null &&
            points[PoseLandmark.RIGHT_WRIST] != null
        readyFrames = if (keyVisible) readyFrames + 1 else 0
        val ready = readyFrames >= READY_FRAMES

        val now = System.currentTimeMillis()
        frameCount++
        if (fpsWindowStart == 0L) fpsWindowStart = now
        if (now - fpsWindowStart >= 1_000L) {
            fps = frameCount
            frameCount = 0
            fpsWindowStart = now
        }

        val sample = PoseSample(points, likelihoods, now)
        val raw = signals.signal(sample)
        val signal = raw?.let { smoother.filter(it, now) }

        return CoachFrame(
            points = points,
            likelihoods = likelihoods,
            imgW = imgW,
            imgH = imgH,
            signal = signal,
            view = signals.view,
            ready = ready,
            fps = fps,
            nowMillis = now,
        )
    }

    private companion object {
        const val MIN_LIKELIHOOD = 0.5f
        const val READY_FRAMES = 10
    }
}
