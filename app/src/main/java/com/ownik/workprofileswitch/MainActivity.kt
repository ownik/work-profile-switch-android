package com.ownik.workprofileswitch

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var hasPermission by mutableStateOf(false)
    private var workProfileEnabled by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateState()

        setContent {
            WorkProfileSwitchTheme {
                Surface {
                    MainScreen(
                        hasPermission = hasPermission,
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
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    private fun updateState() {
        hasPermission = ContextCompat.checkSelfPermission(
            this,
            "android.permission.MODIFY_QUIET_MODE"
        ) == PackageManager.PERMISSION_GRANTED

        workProfileEnabled = !WorkProfileHelper.getQuietMode(this)
    }

    private suspend fun toggleWorkProfile() {
        workProfileEnabled = !workProfileEnabled
        withContext(Dispatchers.IO) {
            WorkProfileHelper.setQuietMode(
                this@MainActivity,
                quietModeEnabled = !workProfileEnabled
            )
        }
    }
}