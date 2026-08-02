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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.core.common.NotificationPermission
import com.visceralfit.core.designsystem.theme.VisceralFitTheme
import com.visceralfit.domain.model.CoachingPreferences
import com.visceralfit.domain.model.EffortCeiling
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.UserPreferences

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        state = state,
        // Read on every composition rather than held in state: the user may have changed it in
        // Android settings since this screen was last shown, which is the whole point of the row.
        notificationsAllowed = NotificationPermission.isGranted(context),
        onOpenNotificationSettings = {
            context.startActivity(NotificationPermission.settingsIntent(context))
        },
        onModalityToggled = viewModel::setModalityEnabled,
        onEquipmentToggled = viewModel::setEquipmentAvailable,
        onKeepScreenOnToggled = viewModel::setKeepScreenOn,
        onAmoledToggled = viewModel::setAmoledDarkMode,
        onCoachingChanged = viewModel::setCoaching,
        onEffortCeilingSelected = viewModel::setEffortCeiling,
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
    notificationsAllowed: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onModalityToggled: (Modality, Boolean) -> Unit,
    onEquipmentToggled: (Modality, Boolean) -> Unit,
    onKeepScreenOnToggled: (Boolean) -> Unit,
    onAmoledToggled: (Boolean) -> Unit,
    onCoachingChanged: (CoachingPreferences) -> Unit,
    onEffortCeilingSelected: (EffortCeiling) -> Unit,
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
                item {
                    Text(
                        "Turn on whichever you feel like using. Any combination works — one, " +
                            "all of them, or anything between.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                items(Modality.entries) { modality ->
                    SettingRow(
                        title = modality.displayName,
                        subtitle = modality.settingSubtitle(prefs),
                        checked = modality in prefs.enabledModalities,
                        onCheckedChange = { onModalityToggled(modality, it) },
                    )
                }
                // REQ-011: the refusal is explained rather than the switch just springing back.
                state.refusal?.let { message ->
                    item {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }

                item { SectionHeader("How hard sessions may get") }
                item { EffortCeilingPicker(prefs.effortCeiling, onEffortCeilingSelected) }

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
                items(CoachingToggle.entries) { toggle ->
                    SettingRow(
                        title = toggle.title,
                        subtitle = toggle.subtitle,
                        checked = toggle.read(prefs.coaching),
                        // Every spoken cue is gated by the master switch; tones and haptics are
                        // not, because they are what substitutes when speech is unavailable.
                        enabled = prefs.coaching.speechEnabled || !toggle.needsSpeech,
                        onCheckedChange = { onCoachingChanged(toggle.write(prefs.coaching, it)) },
                    )
                }

                // Only shown when it is actionable. A row saying "notifications: on" is noise;
                // a row explaining what is missing and offering the fix is not (D-0046). This is
                // the only route back once Android has stopped showing the system dialog.
                if (!notificationsAllowed) {
                    item { SectionHeader("Session controls") }
                    item { NotificationSettingsRow(onOpenNotificationSettings) }
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

/**
 * The way back for a user who declined the notification permission during onboarding.
 *
 * A button into Android settings rather than a second in-app request, because a runtime
 * permission can only be asked for a limited number of times — once the system stops showing
 * its dialog, an in-app request is a silent no-op, and a button that appears to do nothing is
 * worse than no button.
 */
@Composable
private fun NotificationSettingsRow(onOpen: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Notifications are off, so a running session has no lock-screen controls. Your " +
                "workouts are unaffected — you just have to reopen the app to pause or skip.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpen, modifier = Modifier.align(Alignment.Start)) {
            Text("Open notification settings")
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

/**
 * The effort ceiling (A-0007, D-0040).
 *
 * Worded in terms of what the user will feel and what it costs them, not in zone names: the
 * point of the control is that someone who has not been cleared for maximal work can say so,
 * and they will not do that if the options read as jargon. The trade-off is stated in both
 * directions rather than nudging toward the hardest option.
 */
@Composable
private fun EffortCeilingPicker(current: EffortCeiling, onSelected: (EffortCeiling) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            "Only you know what your body and your clinician are happy with. This is the " +
                "hardest any session will ask you to work.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EffortCeiling.entries.forEach { ceiling ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = ceiling == current, onClick = { onSelected(ceiling) })
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = ceiling == current, onClick = { onSelected(ceiling) })
                Column(Modifier.padding(start = 8.dp)) {
                    Text(ceiling.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        ceiling.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val EffortCeiling.title: String
    get() = when (this) {
        EffortCeiling.STEADY -> "Steady only"
        EffortCeiling.THRESHOLD -> "Moderately vigorous"
        EffortCeiling.VIGOROUS -> "Vigorous intervals"
    }

private val EffortCeiling.explanation: String
    get() = when (this) {
        EffortCeiling.STEADY ->
            "Conversational throughout. Still counts toward your weekly minutes, and still " +
                "supported by the evidence — just slower to accumulate."

        EffortCeiling.THRESHOLD ->
            "Hard but sustainable, where sentences get short. Interval sessions still happen; " +
                "they are held below maximal effort."

        EffortCeiling.VIGOROUS ->
            "Up to 85–95% of your maximum heart rate — the intensity the interval research " +
                "uses. Only choose this if you are confident that is safe for you."
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
        Modality.BODYWEIGHT -> "Bodyweight"
        Modality.MAT_PILATES -> "Mat Pilates"
        Modality.REFORMER_PILATES -> "Reformer Pilates"
        Modality.ELLIPTICAL -> "Elliptical"
        Modality.SPIN_BIKE -> "Spin bike"
    }

/**
 * What each category actually contains.
 *
 * Present on every row rather than only where something is wrong: "Bodyweight" and "Mat
 * Pilates" are both floor work with no equipment, so which is which is a fair question, and
 * the answer belongs next to the switch (D-0042). The equipment warning wins when it applies,
 * because that one is about the session the user is about to be denied.
 */
private fun Modality.settingSubtitle(prefs: UserPreferences): String = when {
    requiresEquipment && this !in prefs.body.availableEquipment ->
        "Enabled, but marked as equipment you do not have — no workouts will use it yet."

    else -> description
}

private val Modality.description: String
    get() = when (this) {
        Modality.BODYWEIGHT -> "Core, mobility and calisthenics cardio. Can carry hard intervals."
        Modality.MAT_PILATES -> "The classical mat repertoire. Strength and control, never cardio."
        Modality.REFORMER_PILATES -> "Spring-loaded reformer work. Strength and control, never cardio."
        Modality.ELLIPTICAL -> "Continuous machine cardio, from easy spins to vigorous intervals."
        Modality.SPIN_BIKE -> "Seated and standing, flat and climbing, easy through to sprints."
    }

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    VisceralFitTheme {
        SettingsScreen(
            state = SettingsUiState.Ready(UserPreferences()),
            notificationsAllowed = false,
            onOpenNotificationSettings = {},
            onModalityToggled = { _, _ -> },
            onEquipmentToggled = { _, _ -> },
            onKeepScreenOnToggled = {},
            onAmoledToggled = {},
            onCoachingChanged = {},
            onEffortCeilingSelected = {},
        )
    }
}
