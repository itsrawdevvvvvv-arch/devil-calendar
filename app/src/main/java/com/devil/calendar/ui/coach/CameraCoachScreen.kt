package com.devil.calendar.ui.coach

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.camera.PoseAnalyzer
import com.devil.calendar.camera.SkeletonFrame
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.theme.DevilColors
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Live form coaching.
 *
 * Everything is on-device: frames go from CameraX to ML Kit and are thrown
 * away. Nothing is recorded, stored or uploaded. Android also blocks background
 * camera use, so this only runs while the screen is in front of the user.
 */
@Composable
fun CameraCoachScreen(
    onBack: () -> Unit,
    viewModel: CameraCoachViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    val analyzer = remember(state.config) {
        state.config?.let { config -> PoseAnalyzer(config, viewModel::onSkeleton) }
    }

    DisposableEffect(analyzer) {
        onDispose { analyzer?.close() }
    }

    Box(modifier = Modifier.fillMaxSize().background(DevilColors.Ink)) {
        if (granted && analyzer != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        runCatching {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { it.setAnalyzer(analyzer.executor, analyzer.analyzer) }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                analysis,
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            state.skeleton?.let { SkeletonOverlay(it, state.formValid) }
        } else if (!granted) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "The coach needs the camera. Nothing is recorded - " +
                        "frames are analysed on this phone and thrown away.",
                    color = DevilColors.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = DevilColors.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressFeedback(haptic = false, onClick = onBack)
                    .padding(8.dp),
            )
            Spacer(Modifier.size(8.dp))
            Column {
                Text(
                    text = state.exerciseName.ifBlank { "Coach" },
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                state.config?.let {
                    Text(
                        text = it.cameraAngle,
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // Counter
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isHold) {
                Text(
                    text = "${state.holdSeconds}s",
                    color = if (state.formValid) DevilColors.Yellow else DevilColors.RedText,
                    style = MaterialTheme.typography.headlineLarge,
                )
            } else {
                Text(
                    text = "${state.reps}",
                    color = if (state.formValid) DevilColors.Yellow else DevilColors.White,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            state.config?.tip?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                CounterButton("-1") { viewModel.adjustReps(-1) }
                Spacer(Modifier.size(10.dp))
                CounterButton("+1") { viewModel.adjustReps(1) }
                Spacer(Modifier.size(10.dp))
                CounterButton("RESET") { viewModel.reset() }
            }
            Spacer(Modifier.height(10.dp))
            PillButton(text = "FINISH", onClick = onBack, height = 52.dp)
        }
    }
}

@Composable
private fun CounterButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Card)
            .pressFeedback(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text = label, color = DevilColors.White, style = MaterialTheme.typography.bodySmall)
    }
}

/** Draws the tracked skeleton: green when the pose is usable, red when not. */
@Composable
private fun SkeletonOverlay(frame: SkeletonFrame, valid: Boolean) {
    val colour = if (valid) DevilColors.Yellow else DevilColors.RedText
    val bones = listOf(
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
        PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
        PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
        PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
        PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        bones.forEach { (from, to) ->
            val a = frame.points[from] ?: return@forEach
            val b = frame.points[to] ?: return@forEach
            drawLine(
                color = colour.copy(alpha = 0.85f),
                start = Offset(a.first * size.width, a.second * size.height),
                end = Offset(b.first * size.width, b.second * size.height),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        frame.points.values.forEach { point ->
            drawCircle(
                color = colour,
                radius = 5.dp.toPx(),
                center = Offset(point.first * size.width, point.second * size.height),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
