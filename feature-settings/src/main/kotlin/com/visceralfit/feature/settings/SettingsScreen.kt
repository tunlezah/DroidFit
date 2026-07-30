package com.visceralfit.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.core.designsystem.theme.VisceralFitTheme
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.UserPreferences

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onModalityToggled = viewModel::setModalityEnabled,
        onEquipmentToggled = viewModel::setEquipmentAvailable,
        onKeepScreenOnToggled = viewModel::setKeepScreenOn,
        onAmoledToggled = viewModel::setAmoledDarkMode,
        onSpeechToggled = viewModel::setSpeechEnabled,
        onAnnounceNextToggled = viewModel::setAnnounceNextExercise,
        onSpeakInstructionsToggled = viewModel::setSpeakFullInstructions,
        onAnnounceHalfwayToggled = viewModel::setAnnounceHalfway,
        onAnnounceCountdownToggled = viewModel::setAnnounceCountdown,
    )
}

/**
 * Stateless so it can be driven directly by preview and screenshot tests. Every
 * feature screen in this app follows the same Route/Screen split — see
 * /framework/10_screen_specs.md §Composable contract.
 */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onModalityToggled: (Modality, Boolean) -> Unit,
    onEquipmentToggled: (Modality, Boolean) -> Unit,
    onKeepScreenOnToggled: (Boolean) -> Unit,
    onAmoledToggled: (Boolean) -> Unit,
    onSpeechToggled: (Boolean) -> Unit,
    onAnnounceNextToggled: (Boolean) -> Unit,
    onSpeakInstructionsToggled: (Boolean) -> Unit,
    onAnnounceHalfwayToggled: (Boolean) -> Unit,
    onAnnounceCountdownToggled: (Boolean) -> Unit,
) {
    when (state) {
        SettingsUiState.Loading -> LoadingState()
        is SettingsUiState.Ready -> {
            val prefs = state.preferences
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item { SectionHeader("Exercise types") }
                items(Modality.entries) { modality ->
                    SettingRow(
                        title = modality.displayName,
                        subtitle = modality.settingSubtitle(prefs),
                        checked = modality in prefs.enabledModalities,
                        onCheckedChange = { onModalityToggled(modality, it) },
                    )
                }

                item { SectionHeader("Equipment I have access to") }
                items(Modality.entries.filter { it.requiresEquipment }) { modality ->
                    SettingRow(
                        title = modality.displayName,
                        subtitle = "Workouts using this are only generated when it is available.",
                        checked = modality in prefs.body.availableEquipment,
                        onCheckedChange = { onEquipmentToggled(modality, it) },
                    )
                }

                item { SectionHeader("Spoken coaching") }
                item {
                    SettingRow(
                        title = "Speak cues aloud",
                        subtitle = "Uses your device's text-to-speech voice. Works offline once " +
                            "voice data is installed.",
                        checked = prefs.coaching.speechEnabled,
                        onCheckedChange = onSpeechToggled,
                    )
                }
                item {
                    SettingRow(
                        title = "Announce the next exercise",
                        subtitle = "Names the upcoming movement before it starts.",
                        checked = prefs.coaching.announceNextExercise,
                        enabled = prefs.coaching.speechEnabled,
                        onCheckedChange = onAnnounceNextToggled,
                    )
                }
                item {
                    SettingRow(
                        title = "Read technique cues aloud",
                        subtitle = "Reads the full how-to, not just the exercise name.",
                        checked = prefs.coaching.speakFullInstructions,
                        enabled = prefs.coaching.speechEnabled,
                        onCheckedChange = onSpeakInstructionsToggled,
                    )
                }
                item {
                    SettingRow(
                        title = "Halfway reminder",
                        checked = prefs.coaching.announceHalfway,
                        enabled = prefs.coaching.speechEnabled,
                        onCheckedChange = onAnnounceHalfwayToggled,
                    )
                }
                item {
                    SettingRow(
                        title = "Count down the last seconds",
                        checked = prefs.coaching.announceCountdown,
                        enabled = prefs.coaching.speechEnabled,
                        onCheckedChange = onAnnounceCountdownToggled,
                    )
                }

                item { SectionHeader("Display") }
                item {
                    SettingRow(
                        title = "Keep the screen on during workouts",
                        subtitle = "Stops the display sleeping mid-interval.",
                        checked = prefs.display.keepScreenOn,
                        onCheckedChange = onKeepScreenOnToggled,
                    )
                }
                item {
                    SettingRow(
                        title = "True black dark mode",
                        subtitle = "Uses less battery on this phone's OLED screen.",
                        checked = prefs.display.amoledDarkMode,
                        onCheckedChange = onAmoledToggled,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column {
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        HorizontalDivider()
    }
}

/**
 * A single toggle.
 *
 * Accessibility notes that must survive refactors (see
 * /framework/12_accessibility_spec.md):
 *  - The whole row is the touch target, minimum 56 dp tall, not just the switch.
 *  - `toggleable` with `Role.Switch` on the row means TalkBack announces the row
 *    label and the on/off state together and the switch itself is not separately
 *    focusable, so there is one stop per setting rather than two.
 */
@Composable
private fun SettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.padding(horizontal = 8.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

private val Modality.displayName: String
    get() = when (this) {
        Modality.FLOOR_PILATES -> "Floor Pilates"
        Modality.REFORMER_PILATES -> "Reformer Pilates"
        Modality.ELLIPTICAL -> "Elliptical"
        Modality.SPIN_BIKE -> "Spin bike"
    }

private fun Modality.settingSubtitle(prefs: UserPreferences): String? = when {
    requiresEquipment && this !in prefs.body.availableEquipment ->
        "Enabled, but marked as equipment you do not have — no workouts will use it yet."
    else -> null
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    VisceralFitTheme {
        SettingsScreen(
            state = SettingsUiState.Ready(UserPreferences()),
            onModalityToggled = { _, _ -> },
            onEquipmentToggled = { _, _ -> },
            onKeepScreenOnToggled = {},
            onAmoledToggled = {},
            onSpeechToggled = {},
            onAnnounceNextToggled = {},
            onSpeakInstructionsToggled = {},
            onAnnounceHalfwayToggled = {},
            onAnnounceCountdownToggled = {},
        )
    }
}
