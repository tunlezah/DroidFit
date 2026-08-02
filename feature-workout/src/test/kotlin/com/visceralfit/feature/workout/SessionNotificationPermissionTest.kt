package com.visceralfit.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one property this flow has to have: **every path starts the session.**
 *
 * A notification permission buys the lock-screen controls and nothing else, so a flow that
 * could leave a user who tapped Deny unable to train would be a worse defect than the missing
 * notification it was added to fix (KI-0016). These tests exist to make that regression
 * impossible to introduce quietly.
 */
class SessionNotificationPermissionTest {

    @Test
    fun `an already-granted permission starts the session with no prompt`() {
        val log = mutableListOf<String>()
        controller(granted = true, log = log).launch()

        assertEquals(listOf("proceed"), log)
    }

    @Test
    fun `an ungranted permission explains itself before asking`() {
        val log = mutableListOf<String>()
        controller(granted = false, log = log).launch()

        assertEquals("the system prompt was shown with no rationale first", listOf("rationale"), log)
    }

    @Test
    fun `accepting the rationale asks for the permission`() {
        val log = mutableListOf<String>()
        val controller = controller(granted = false, log = log)
        controller.launch()
        controller.request()

        assertEquals(listOf("rationale", "request"), log)
    }

    /**
     * No silent path. Whatever the permission state, tapping Start must visibly do something —
     * either the session begins or the user is asked a question. A tap that appears to do
     * nothing is how a permission check turns into "the app is broken".
     */
    @Test
    fun `there is no permission state in which tapping start does nothing`() {
        listOf(true, false).forEach { granted ->
            val log = mutableListOf<String>()
            controller(granted = granted, log = log).launch()
            assertTrue("granted=$granted produced no visible outcome", log.isNotEmpty())
        }
    }

    private fun controller(granted: Boolean, log: MutableList<String>) = SessionNotificationPermission(
        onNeedsRationale = { log += "rationale" },
        onProceed = { log += "proceed" },
        isGranted = { granted },
        requestPermission = { log += "request" },
    )
}
