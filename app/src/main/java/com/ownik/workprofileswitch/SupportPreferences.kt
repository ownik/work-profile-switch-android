package com.ownik.workprofileswitch

import android.content.Context
import java.util.concurrent.TimeUnit

internal data class SupportPromptState(
    val nextPromptAt: Long,
    val promptShown: Boolean = false,
    val neverAsk: Boolean = false,
    val hasSupported: Boolean = false,
) {
    fun shouldShow(now: Long, setupComplete: Boolean): Boolean =
        setupComplete && !promptShown && !neverAsk && !hasSupported && now >= nextPromptAt

    fun remindLater(now: Long): SupportPromptState =
        copy(nextPromptAt = now + REMINDER_DELAY, promptShown = false)

    companion object {
        val INITIAL_DELAY: Long = TimeUnit.DAYS.toMillis(7)
        val REMINDER_DELAY: Long = TimeUnit.DAYS.toMillis(30)
    }
}

internal class SupportPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("developer_support", Context.MODE_PRIVATE)

    init {
        if (!preferences.contains("next_prompt_at")) {
            preferences.edit()
                .putLong("next_prompt_at", System.currentTimeMillis() + SupportPromptState.INITIAL_DELAY)
                .apply()
        }
    }

    fun read() = SupportPromptState(
        nextPromptAt = preferences.getLong("next_prompt_at", Long.MAX_VALUE),
        promptShown = preferences.getBoolean("prompt_shown", false),
        neverAsk = preferences.getBoolean("never_ask", false),
        hasSupported = preferences.getBoolean("has_supported", false),
    )

    fun write(state: SupportPromptState) {
        preferences.edit()
            .putLong("next_prompt_at", state.nextPromptAt)
            .putBoolean("prompt_shown", state.promptShown)
            .putBoolean("never_ask", state.neverAsk)
            .putBoolean("has_supported", state.hasSupported)
            .apply()
    }
}
