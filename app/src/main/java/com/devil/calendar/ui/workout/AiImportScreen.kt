package com.devil.calendar.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.data.ai.AiPrompts
import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.data.local.workout.RoutineKind
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.bottomFadeHeight
import com.devil.calendar.ui.components.edgeFade
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.components.topFadeHeight
import com.devil.calendar.ui.theme.DevilColors

/**
 * Import a routine from any AI, or from your own notes. Parsing is offline; the
 * Sarvam generate box is the only networked path and degrades to a clear error.
 */
@Composable
fun AiImportScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AiImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hasKey by viewModel.hasKey.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

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
            text = "Import with AI",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )

        Spacer(Modifier.height(16.dp))
        PillButton(
            text = "COPY AI PROMPT",
            onClick = { clipboard.setText(AnnotatedString(AiPrompts.COPY_PROMPT)) },
            background = DevilColors.Card,
            textColor = DevilColors.White,
            height = 52.dp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Paste it into ChatGPT, Gemini or any chatbot, then paste the answer below.",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel("Paste a routine")
        Spacer(Modifier.height(8.dp))
        PasteBox(
            value = state.pasted,
            onValueChange = viewModel::onPastedChange,
            placeholder = "{\"name\":\"Push Day\",...}  or  Push-ups 4x12 rest 60s",
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = "READ IT",
            onClick = viewModel::parsePasted,
            enabled = state.pasted.isNotBlank(),
            height = 52.dp,
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel("Generate with Sarvam")
        Spacer(Modifier.height(8.dp))
        PasteBox(
            value = state.goal,
            onValueChange = viewModel::onGoalChange,
            placeholder = "e.g. 20 minute push day, no equipment",
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = if (state.busy) "ASKING SARVAM..." else "GENERATE",
            onClick = viewModel::generate,
            enabled = hasKey && !state.busy && state.goal.isNotBlank(),
            background = DevilColors.Card,
            textColor = DevilColors.White,
            height = 52.dp,
        )
        if (!hasKey) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add your Sarvam key in Profile to use this.",
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        state.message?.let { message ->
            Spacer(Modifier.height(14.dp))
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                color = DevilColors.Card,
                borderColor = if (state.isError) DevilColors.RedText else DevilColors.Yellow,
            ) {
                Text(
                    text = message,
                    color = if (state.isError) DevilColors.RedText else DevilColors.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        val draft = state.draft
        if (draft != null) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Preview - ${draft.name}")
            Spacer(Modifier.height(8.dp))
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    draft.exercises.forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = exercise.name,
                                color = DevilColors.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (exercise.mode == ExerciseMode.HOLD) {
                                    "${exercise.sets} x ${exercise.holdSeconds}s"
                                } else {
                                    "${exercise.sets} x ${exercise.reps}"
                                },
                                color = DevilColors.Yellow,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text(
                                text = "${exercise.restSeconds}s",
                                color = DevilColors.Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = "SAVE AS ROUTINE",
                onClick = { viewModel.save(RoutineKind.ROUTINE, onSaved) },
                height = 52.dp,
            )
            Spacer(Modifier.height(8.dp))
            PillButton(
                text = "SAVE AS SINGLE WORKOUT",
                onClick = { viewModel.save(RoutineKind.SINGLE, onSaved) },
                background = DevilColors.Card,
                textColor = DevilColors.White,
                height = 52.dp,
            )
        }
    }
}

@Composable
private fun PasteBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DevilColors.Card)
            .padding(14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = DevilColors.White),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(DevilColors.Yellow),
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        )
    }
}
