package com.devil.calendar.ui.coach

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.camera.CameraView
import com.devil.calendar.camera.CoachFrame
import com.devil.calendar.camera.LandmarkTransform
import com.devil.calendar.camera.PoseAnalyzer
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.theme.DevilColors
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Live form coaching, entirely on-device.
 *
 * Frames go from CameraX to ML Kit and are discarded. Nothing is recorded,
 * stored or uploaded, and Android blocks background camera use, so this only
 * runs while the screen is in front of you.
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

    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    val analyzer = remember(state.config) {
        state.config?.let { config -> PoseAnalyzer(config, viewModel::onFrame) }
    }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(previewView, analyzer, granted) {
        var provider: ProcessCameraProvider? = null
        var cancelled = false
        if (granted && analyzer != null) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (cancelled) return@addListener
                runCatching {
                    val bound = future.get()
                    provider = bound
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val resolution = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build()
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(resolution)
                        .build()
                        .also { it.setAnalyzer(analyzer.executor, analyzer.analyzer) }

                    val hasFront = bound.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    val selector = if (hasFront) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    bound.unbindAll()
                    bound.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    cameraError = if (hasFront) null else "No front camera - using the back camera."
                }.onFailure { error ->
                    cameraError = "Camera unavailable: ${error.javaClass.simpleName} ${error.message.orEmpty()}"
                }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            cancelled = true
            runCatching { provider?.unbindAll() }
            analyzer?.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (granted && analyzer != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize().onSizeChanged { viewSize = it },
                factory = { previewView },
            )
            state.frame?.let { frame ->
                SkeletonOverlay(frame, viewSize, state.ready, state.debug)
            }
        } else if (granted && analyzer == null) {
            Centered("Starting camera...")
        } else {
            Centered(
                "The coach needs the camera. Nothing is recorded - frames are " +
                    "analysed on this phone and thrown away.",
            )
        }

        cameraError?.let { Centered(it, DevilColors.RedText) }

        // Scrims so the text stays readable over a bright image.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(260.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))),
        )

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
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.pointerInput(Unit) {
                    // Hidden debug overlay: long-press the title.
                    detectTapGestures(onLongPress = { viewModel.toggleDebug() })
                },
            ) {
                Text(
                    text = state.exerciseName.ifBlank { "Coach" },
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = when (state.view) {
                        CameraView.FRONT -> "Head-on view"
                        CameraView.SIDE -> "Side-on view"
                        CameraView.UNKNOWN -> "Finding you..."
                    },
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (state.debug) {
            DebugPanel(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 90.dp, end = 12.dp),
                state = state,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (state.isHold) "${state.holdSeconds}s" else "${state.reps}",
                color = when {
                    !state.ready -> DevilColors.Muted
                    state.phase == com.devil.calendar.domain.coach.RepPhase.DOWN -> DevilColors.Yellow
                    else -> DevilColors.White
                },
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = state.hint,
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
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
private fun Centered(text: String, colour: Color = DevilColors.White) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = colour, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DebugPanel(modifier: Modifier, state: CoachUiState) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(10.dp),
    ) {
        listOf(
            "fps ${state.fps}",
            "view ${state.view}",
            "ready ${state.ready}",
            "signal ${state.signal?.let { "%.2f".format(it) } ?: "-"}",
            "low ${"%.2f".format(state.low)} high ${"%.2f".format(state.high)}",
            "phase ${state.phase}",
            "reps ${state.reps}",
        ).forEach {
            Text(text = it, color = DevilColors.Yellow, style = MaterialTheme.typography.bodySmall)
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

/**
 * Draws the skeleton with exactly the transform the preview uses, so points sit
 * on the body instead of drifting.
 */
@Composable
private fun SkeletonOverlay(
    frame: CoachFrame,
    viewSize: IntSize,
    ready: Boolean,
    debug: Boolean,
) {
    val colour = if (ready) DevilColors.Yellow else DevilColors.RedText
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
        if (viewSize.width == 0 || viewSize.height == 0) return@Canvas
        // The front preview is mirrored, the analysis image is not.
        val mapping = LandmarkTransform.ViewMapping.compute(
            viewW = size.width,
            viewH = size.height,
            imgW = frame.imgW,
            imgH = frame.imgH,
            fillCenter = true,
            mirror = true,
        )
        val stroke = 3.dp.toPx()

        bones.forEach { (from, to) ->
            val a = frame.points[from] ?: return@forEach
            val b = frame.points[to] ?: return@forEach
            drawLine(
                color = colour.copy(alpha = 0.9f),
                start = mapping.map(a.first, a.second, size.width),
                end = mapping.map(b.first, b.second, size.width),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        frame.points.forEach { (type, point) ->
            val centre = mapping.map(point.first, point.second, size.width)
            drawCircle(
                color = colour,
                radius = 4.dp.toPx(),
                center = centre,
                style = Stroke(width = 2.dp.toPx()),
            )
            if (debug && (type == PoseLandmark.NOSE || type == PoseLandmark.LEFT_SHOULDER ||
                    type == PoseLandmark.RIGHT_SHOULDER)
            ) {
                drawLine(
                    color = Color.White,
                    start = Offset(centre.x - 8f, centre.y),
                    end = Offset(centre.x + 8f, centre.y),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = Color.White,
                    start = Offset(centre.x, centre.y - 8f),
                    end = Offset(centre.x, centre.y + 8f),
                    strokeWidth = 1f,
                )
            }
        }
    }
}
