package com.visceralfit.app

import com.visceralfit.domain.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The onboarding sequence, which is a requirement rather than a layout detail.
 *
 * Two of these tests exist because of specific ways this has gone wrong or could: the safety
 * notice must not be skippable by any combination of the other flags (REQ-005), and the
 * notification step must not reappear for someone who has already declined — the defect the
 * first version of the permission flow shipped with, where the rationale dialog returned on
 * every single session start.
 */
class OnboardingStepTest {

    @Test
    fun `nothing is shown until preferences have loaded`() {
        assertEquals(OnboardingStep.WAITING, OnboardingStep.of(MainUiState.Loading, notificationsGranted = false))
        assertEquals(OnboardingStep.WAITING, OnboardingStep.of(MainUiState.Loading, notificationsGranted = true))
    }

    @Test
    fun `the safety notice comes first on a fresh install`() {
        assertEquals(
            OnboardingStep.SAFETY_NOTICE,
            OnboardingStep.of(ready(), notificationsGranted = false),
        )
    }

    /** REQ-005: no combination of the other flags may let a user past the notice. */
    @Test
    fun `the safety notice cannot be skipped by any other state`() {
        listOf(true, false).forEach { asked ->
            listOf(true, false).forEach { granted ->
                assertEquals(
                    "asked=$asked granted=$granted got past the safety notice",
                    OnboardingStep.SAFETY_NOTICE,
                    OnboardingStep.of(
                        ready(safetyAcknowledged = false, notificationAsked = asked),
                        notificationsGranted = granted,
                    ),
                )
            }
        }
    }

    @Test
    fun `the notification step follows the safety notice`() {
        assertEquals(
            OnboardingStep.NOTIFICATION_PERMISSION,
            OnboardingStep.of(ready(safetyAcknowledged = true), notificationsGranted = false),
        )
    }

    /** The defect this replaces: a declined permission asked again on every entry. */
    @Test
    fun `a declined permission is not asked for a second time`() {
        assertEquals(
            OnboardingStep.APP,
            OnboardingStep.of(
                ready(safetyAcknowledged = true, notificationAsked = true),
                notificationsGranted = false,
            ),
        )
    }

    /**
     * Covers both the granted-in-system-settings-first case and every device below API 33, where
     * `NotificationPermission.isGranted` reports true because the permission does not exist.
     */
    @Test
    fun `an already-granted permission is never asked for`() {
        assertEquals(
            OnboardingStep.APP,
            OnboardingStep.of(ready(safetyAcknowledged = true), notificationsGranted = true),
        )
    }

    @Test
    fun `both steps passed lands on the app`() {
        assertEquals(
            OnboardingStep.APP,
            OnboardingStep.of(
                ready(safetyAcknowledged = true, notificationAsked = true),
                notificationsGranted = true,
            ),
        )
    }

    private fun ready(
        safetyAcknowledged: Boolean = false,
        notificationAsked: Boolean = false,
    ) = MainUiState.Ready(
        theme = ThemePreference.SYSTEM,
        amoled = true,
        dynamicColour = true,
        safetyNoticeAcknowledged = safetyAcknowledged,
        notificationPermissionRequested = notificationAsked,
    )
}
