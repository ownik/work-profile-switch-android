package com.ownik.workprofileswitch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    private var hasPermission by mutableStateOf(false)
    private var workProfileHandle by mutableStateOf<UserHandle?>(null)
    private var workProfileEnabled by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)

    // Fires when the work profile's quiet mode is toggled by anything else -
    // Settings, Samsung Modes/Routines, another app, etc. - so the switch
    // reflects it immediately, without waiting for onResume.
    private val quietModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            workProfileEnabled = workProfileHandle?.let {
                !WorkProfileHelper.getQuietMode(this@MainActivity, it)
            } ?: false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateState()

        setContent {
            WorkProfileSwitchTheme {
                Surface {
                    MainScreen(
                        hasPermission = hasPermission,
                        workProfileFound = workProfileHandle != null,
                        workProfileEnabled = workProfileEnabled,
                        onToggleWorkProfile = {
                            toggleWorkProfile()
                        },
                        onShowPermissionDialog = {
                            showPermissionDialog = true
                        }
                    )

                    if (showPermissionDialog) {
                        PermissionGuideDialog(
                            onDismiss = {
                                showPermissionDialog = false
                            }
                        )
                    }
                }
            }
        }

        // MODIFY_QUIET_MODE has no grant/revoke broadcast, so poll it while the
        // activity is at least STARTED. Catches a permission change made
        // outside the app (adb, system Settings) while MainActivity stays open.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var lastKnownPermission = hasPermission
                while (true) {
                    val current = checkQuietModePermission()
                    if (current != lastKnownPermission) {
                        lastKnownPermission = current
                        hasPermission = current
                        workProfileHandle = WorkProfileHelper.findWorkProfileHandle(this@MainActivity)
                        workProfileEnabled = workProfileHandle?.let {
                            !WorkProfileHelper.getQuietMode(this@MainActivity, it)
                        } ?: false
                        Toast.makeText(
                            this@MainActivity,
                            if (current) {
                                getString(R.string.permission_granted)
                            } else {
                                getString(R.string.permission_revoked)
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    delay(PERMISSION_POLL_INTERVAL)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            quietModeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(quietModeReceiver)
    }

    private fun updateState() {
        hasPermission = checkQuietModePermission()
        workProfileHandle = WorkProfileHelper.findWorkProfileHandle(this)
        workProfileEnabled = workProfileHandle?.let {
            !WorkProfileHelper.getQuietMode(this, it)
        } ?: false
    }

    private fun checkQuietModePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            "android.permission.MODIFY_QUIET_MODE"
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun toggleWorkProfile() {
        workProfileHandle?.let {
            val newState = !workProfileEnabled
            withContext(Dispatchers.IO) {
                WorkProfileHelper.setQuietMode(
                    this@MainActivity,
                    workProfileHandle = it,
                    quietModeEnabled = !newState
                )
            }
            workProfileEnabled = newState
        }
    }

    private companion object {
        val PERMISSION_POLL_INTERVAL = 1.seconds
    }
}