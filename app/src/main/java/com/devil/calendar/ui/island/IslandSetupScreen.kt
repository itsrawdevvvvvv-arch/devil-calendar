package com.devil.calendar.ui.island

import com.devil.calendar.ui.components.edgeFade
import com.devil.calendar.ui.components.topFadeHeight
import com.devil.calendar.ui.components.bottomFadeHeight
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.util.Permissions

/**
 * Step-by-step setup. Three of the four grants are special access, not runtime
 * dialogs, so each row deep-links into the right Settings screen and the state
 * is re-read when the user comes back.
 */
@Composable
fun IslandSetupScreen(
    onBack: () -> Unit,
    viewModel: IslandSetupViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val known by viewModel.known.collectAsStateWithLifecycle()
    val allowed by viewModel.allowed.collectAsStateWithLifecycle()
    val tiltGlare by viewModel.tiltGlare.collectAsStateWithLifecycle()
    var refresh by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }

    val steps = remember(refresh) {
        listOf(
            SetupStep(
                title = "Display over other apps",
                detail = "Lets the pill draw on top of other apps.",
                granted = Permissions.hasOverlay(context),
                action = { context.startActivity(Permissions.overlayIntent(context)) },
            ),
            SetupStep(
                title = "Notification access",
                detail = "Lets the island show timers, calls and music from other apps.",
                granted = Permissions.hasNotificationAccess(context),
                action = { context.startActivity(Permissions.notificationAccessIntent()) },
            ),
            SetupStep(
                title = "Notifications",
                detail = "Needed for the island's foreground service.",
                granted = Permissions.hasNotifications(context),
                action = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            ),
            SetupStep(
                title = "Ignore battery optimisation",
                detail = "Keeps the service alive when the screen is off.",
                granted = Permissions.hasBatteryExemption(context),
                action = { context.startActivity(Permissions.batteryIntent(context)) },
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .edgeFade(top = topFadeHeight(), bottom = bottomFadeHeight())
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = navAwareBottomPadding()),
    ) {
        Icon(
            imageVector = Icons.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = DevilColors.Muted,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .pressFeedback(haptic = false, onClick = onBack)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Dynamic Island setup",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${steps.count { it.granted }} of ${steps.size} granted",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        steps.forEachIndexed { index, step ->
            BentoCard(
                modifier = Modifier.fillMaxWidth().animatedEntrance(index + 1),
                color = DevilColors.Card,
                borderColor = if (step.granted) DevilColors.Yellow else DevilColors.CardBorder,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step.granted) DevilColors.Yellow else DevilColors.RingTrack,
                                ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = step.title,
                            color = DevilColors.White,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (step.granted) "Granted" else "Missing",
                            color = if (step.granted) DevilColors.Yellow else DevilColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = step.detail,
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!step.granted) {
                        Spacer(Modifier.height(10.dp))
                        PillButton(
                            text = "GRANT",
                            onClick = step.action,
                            height = 44.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        BentoCard(
            modifier = Modifier.fillMaxWidth().animatedEntrance(steps.size + 1),
            color = DevilColors.Card,
            borderColor = DevilColors.CardBorder,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .pressFeedback(haptic = false, onClick = { viewModel.setTiltGlare(!tiltGlare) })
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tilt glare",
                        color = DevilColors.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "The rim light follows how you hold the phone.",
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (tiltGlare) DevilColors.Yellow else DevilColors.RingTrack),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (known.isNotEmpty()) {
            BentoCard(
                modifier = Modifier.fillMaxWidth().animatedEntrance(steps.size + 2),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Which apps may use the island",
                        color = DevilColors.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (allowed.isEmpty()) {
                            "Nothing selected - every eligible notification shows."
                        } else {
                            "${allowed.size} selected"
                        },
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(10.dp))
                    known.sorted().forEach { packageName ->
                        val selected = packageName in allowed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .pressFeedback(haptic = false, onClick = { viewModel.toggle(packageName) })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) DevilColors.Yellow else DevilColors.RingTrack,
                                    ),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = appLabel(context, packageName),
                                color = DevilColors.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            color = DevilColors.Card,
            borderColor = DevilColors.CardBorder,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "On Xiaomi, Oppo, Vivo and Samsung",
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "These launchers kill background services aggressively. " +
                        "Enable autostart for Devil Calendar in your phone's settings, " +
                        "otherwise the island will disappear after a while.",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Falls back to the package name if the label cannot be read. */
private fun appLabel(context: android.content.Context, packageName: String): String =
    runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

private data class SetupStep(
    val title: String,
    val detail: String,
    val granted: Boolean,
    val action: () -> Unit,
)
