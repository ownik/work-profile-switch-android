package com.ownik.workprofileswitch

import android.app.Activity
import android.os.Bundle
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast

/**
 * No layout, translucent theme (see manifest) — this exists purely so
 * shortcuts / Bixby Routines have an Activity to target. It reads the
 * "quiet_mode_enabled" extra (1 = disable profile, 0 = enable),
 * performs the toggle, and finishes immediately.
 */
class ToggleActivity : Activity() {
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setQuietMode(false)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val quietModeEnabled = intent.getIntExtra("quiet_mode_enabled", 0) == 1
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        // if locked and disable quiet mode (enable work profile)
        if (keyguardManager.isKeyguardLocked && !quietModeEnabled) {
            // wait for unlock and enable profile
            registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        } else {
            // otherwise disable/enable quiet mode directly
            setQuietMode(quietModeEnabled)
            finish()
        }
    }

    private fun setQuietMode(quietModeEnabled: Boolean) {
        val workProfileHandle = WorkProfileHelper.findWorkProfileHandle(this)
        if (workProfileHandle == null) {
            Toast.makeText(applicationContext, "No work profile found", Toast.LENGTH_LONG).show()
            return
        }
        WorkProfileHelper.setQuietMode(applicationContext, workProfileHandle, quietModeEnabled)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(userPresentReceiver) }
    }
}
