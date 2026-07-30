package com.visceralfit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.visceralfit.core.designsystem.theme.VisceralFitTheme

/**
 * The medical-safety notice, shown before the first session and acknowledged once
 * (REQ-005). Closes KI-0005.
 *
 * WHY IT GATES THE WHOLE APP RATHER THAN THE PLAYER: REQ-005 says "before the first
 * session", and the cheapest reliable reading of that is "before anything". Gating only
 * the player means a user who explores Settings, sets their level to advanced and enables
 * the bike has already made intensity decisions before being told to stop on chest pain.
 *
 * WHY IT IS NOT DISMISSIBLE BY BACK: acknowledgement is the mitigation for A-0007 — the
 * open assumption that the operator is cleared for vigorous exercise. A notice that can be
 * swiped away has not been acknowledged.
 *
 * The three points are exactly the ones REQ-005 enumerates, in the order it lists them,
 * and the copy follows `framework/02_evidence_base.md` §6: no outcome is promised and
 * "safe for everyone" is never implied.
 */
@Composable
internal fun SafetyNoticeScreen(onAcknowledge: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Before you start", style = MaterialTheme.typography.headlineSmall)

            Text(
                "This app builds interval and steady cardio sessions that can reach a hard " +
                    "effort — 85 to 95 percent of your maximum heart rate. Please read this once.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stop exercising immediately if you feel",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    listOf(
                        "chest pain or pressure",
                        "dizziness or light-headedness",
                        "breathlessness that is unusual for you",
                    ).forEach { symptom ->
                        Text(
                            "• $symptom",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Text(
                        "Seek medical help if a symptom does not settle with rest.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            NoticeSection(
                title = "Talk to a clinician first if",
                body = "you have a heart, lung or blood-pressure condition, you are pregnant or " +
                    "recently post-partum, you are recovering from an injury or operation, you take " +
                    "medication that affects your heart rate, or you are new to vigorous exercise. " +
                    "This app cannot know any of that about you.",
            )

            NoticeSection(
                title = "This app is not medical advice",
                body = "The sessions it builds follow published exercise-science guidance, and every " +
                    "programming rule in it traces to a citation you can read in Settings. That is " +
                    "not the same as advice for your body. Nothing here is personalised to your " +
                    "health, and no exercise is safe for everyone.",
            )

            NoticeSection(
                title = "What it does not do",
                body = "It does not measure or estimate your visceral fat, and no exercise in it " +
                    "targets fat in a particular place — that is not how the body works. Energy " +
                    "figures are estimates from population averages, shown as estimates.",
            )

            Text(
                "You can read this notice again at any time from Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = onAcknowledge, modifier = Modifier.fillMaxWidth()) {
                Text("I have read this")
            }
        }
    }
}

@Composable
private fun NoticeSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun SafetyNoticePreview() {
    VisceralFitTheme {
        SafetyNoticeScreen(onAcknowledge = {})
    }
}
