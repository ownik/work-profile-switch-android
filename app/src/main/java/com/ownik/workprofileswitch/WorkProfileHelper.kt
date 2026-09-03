package com.ownik.workprofileswitch

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

object WorkProfileHelper {

    private const val TAG = "WorkProfileHelper"

    /**
     * Toggles the work profile's quiet mode.
     * quietModeEnabled = true  -> work profile apps are PAUSED (profile off)
     * quietModeEnabled = false -> work profile apps are ACTIVE (profile on)
     *
     * Requires android.permission.MODIFY_QUIET_MODE to already be granted
     * (via `adb shell pm grant ...` or a Shizuku-mediated grant). Without
     * it, requestQuietModeEnabled throws SecurityException — there is no
     * runtime permission dialog to fall back on, since this is a
     * signature|privileged permission, not a dangerous one.
     */
    fun setQuietMode(context: Context, quietModeEnabled: Boolean) {
        if (ContextCompat.checkSelfPermission(
                context, "android.permission.MODIFY_QUIET_MODE"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "Permission not granted yet — see setup instructions",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

        val workProfileHandle = findWorkProfileHandle(userManager)
        if (workProfileHandle == null) {
            Log.w(TAG, "No work profile found on this device")
            Toast.makeText(context, "No work profile found", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val ok = userManager.requestQuietModeEnabled(quietModeEnabled, workProfileHandle)
            if (!ok) {
                Log.w(TAG, "requestQuietModeEnabled returned false")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing MODIFY_QUIET_MODE at call time", e)
            Toast.makeText(context, "Permission denied by system", Toast.LENGTH_LONG).show()
        }
    }


    /**
     * Get the work profile's quiet mode state.
     */
    fun getQuietMode(context: Context): Boolean {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

        val workProfileHandle = findWorkProfileHandle(userManager)
        if (workProfileHandle == null) {
            Log.w(TAG, "No work profile found on this device")
            Toast.makeText(context, "No work profile found", Toast.LENGTH_LONG).show()
            return false
        }

        return userManager.isQuietModeEnabled(workProfileHandle)
    }

    /**
     * The app must be running in the PERSONAL profile for this to work.
     * getUserProfiles() returns every profile associated with the current
     * user group (personal + any work profiles); we pick the one that
     * isn't our own handle.
     */
    private fun findWorkProfileHandle(userManager: UserManager): UserHandle? {
        val myHandle = android.os.Process.myUserHandle()
        return userManager.userProfiles.firstOrNull { it != myHandle }
    }
}
