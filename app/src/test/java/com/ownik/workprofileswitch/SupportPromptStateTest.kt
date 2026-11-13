package com.ownik.workprofileswitch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportPromptStateTest {
    private val dueAt = 1_000L
    private val initial = SupportPromptState(nextPromptAt = dueAt)

    @Test
    fun promptWaitsUntilDueAndSetupComplete() {
        assertFalse(initial.shouldShow(dueAt - 1, setupComplete = true))
        assertFalse(initial.shouldShow(dueAt, setupComplete = false))
        assertTrue(initial.shouldShow(dueAt, setupComplete = true))
    }

    @Test
    fun shownPromptDoesNotRepeat() {
        assertFalse(initial.copy(promptShown = true).shouldShow(Long.MAX_VALUE, true))
    }

    @Test
    fun explicitOptOutAndSupportSuppressPrompts() {
        assertFalse(initial.copy(neverAsk = true).shouldShow(Long.MAX_VALUE, true))
        assertFalse(initial.copy(hasSupported = true).shouldShow(Long.MAX_VALUE, true))
    }

    @Test
    fun laterSchedulesAnotherPromptInThirtyDays() {
        val later = initial.copy(promptShown = true).remindLater(dueAt)
        val next = dueAt + SupportPromptState.REMINDER_DELAY
        assertFalse(later.shouldShow(next - 1, true))
        assertTrue(later.shouldShow(next, true))
    }

    @Test
    fun laterDoesNotOverrideOptOutOrSupport() {
        assertFalse(initial.copy(neverAsk = true).remindLater(dueAt)
            .shouldShow(Long.MAX_VALUE, true))
        assertFalse(initial.copy(hasSupported = true).remindLater(dueAt)
            .shouldShow(Long.MAX_VALUE, true))
    }
}
