package com.devil.calendar.ui.onboarding

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.util.Permissions

private data class OnboardingStep(
    val title: String,
    val body: String,
    val granted: (android.content.Context) -> Boolean,
    val request: (android.content.Context) -> Intent,
    val runtime: String? = null,
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    // Bumped on every resume so each step's status refreshes after the user
    // returns from a Settings screen.
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

    val steps = remember {
        listOf(
            OnboardingStep(
                title = "Notifications",
                body = "Lets a timer or alarm tell you it is done.",
                granted = { Permissions.hasNotifications(it) },
                request = { Permissions.appDetails(it) },
                runtime = android.Manifest.permission.POST_NOTIFICATIONS,
            ),
            OnboardingStep(
                title = "Exact alarms",
                body = "Fires at the precise second, even in Doze.",
                granted = { Permissions.hasExactAlarm(it) },
                request = { Permissions.exactAlarmIntent(it) },
            ),
            OnboardingStep(
                title = "Display over other apps",
                body = "Shows the takeover on top of whatever you are doing.",
                granted = { Permissions.hasOverlay(it) },
                request = { Permissions.overlayIntent(it) },
            ),
            OnboardingStep(
                title = "Full-screen alerts",
                body = "Covers the lock screen and wakes the display.",
                granted = { Permissions.hasFullScreenIntent(it) },
                request = { Permissions.fullScreenIntentSettings(it) },
            ),
            OnboardingStep(
                title = "Battery optimization",
                body = "Stops the system from killing a running session.",
                granted = { Permissions.hasBatteryExemption(it) },
                request = { Permissions.batteryIntent(it) },
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DevilColors.Ink)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 32.dp),
    ) {
        Text(
            text = "Welcome to\nDevil Calendar",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "A few permissions keep your alarms alive. Grant what you can — you can change any of these later.",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))

        steps.forEachIndexed { index, step ->
            val granted = step.granted(context)
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                color = DevilColors.Card,
                cornerRadius = DevilRadius.card,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = step.title,
                            color = DevilColors.White,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (granted) DevilColors.Yellow else DevilColors.Red),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = step.body,
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    PillButton(
                        text = if (granted) "Granted" else "Grant",
                        onClick = {
                            if (granted) return@PillButton
                            if (step.runtime != null) {
                                notificationLauncher.launch(step.runtime)
                            } else {
                                runCatching { context.startActivity(step.request(context)) }
                            }
                        },
                        background = if (granted) DevilColors.Grey else DevilColors.Yellow,
                        height = 48.dp,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        PillButton(
            text = "CONTINUE",
            onClick = onDone,
            background = DevilColors.Red,
            textColor = DevilColors.White,
        )
    }
}
