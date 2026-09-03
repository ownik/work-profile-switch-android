package com.ownik.workprofileswitch

import android.app.Activity
import android.os.Bundle

/**
 * No layout, translucent theme (see manifest) — this exists purely so
 * shortcuts / Bixby Routines have an Activity to target. It reads the
 * "quiet_mode_enabled" extra (1 = disable profile, 0 = enable),
 * performs the toggle, and finishes immediately.
 */
class ToggleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val quietModeEnabled = intent.getIntExtra("quiet_mode_enabled", 0) == 1
        WorkProfileHelper.setQuietMode(applicationContext, quietModeEnabled)

        finish()
    }
}
