package com.ownik.workprofileswitch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme

@Composable
fun MainScreen(
    hasPermission: Boolean,
    workProfileEnabled: Boolean,
    onToggleWorkProfile: suspend () -> Unit,
    onShowPermissionDialog: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),
        ) {

            PermissionCard(
                hasPermission = hasPermission,
                onHowToGrantClick = {
                    onShowPermissionDialog()
                }
            )

            Spacer(modifier = Modifier.padding(vertical = 16.dp))

            PowerButton(
                enabled = hasPermission,
                isOn = workProfileEnabled,
                onClick = onToggleWorkProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )

            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            // Current state
            Text(
                text = if (workProfileEnabled) {
                    "Work profile is enabled"
                } else {
                    "Work profile is disabled"
                },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.padding(vertical = 2.dp))

            Text(
                text = if (workProfileEnabled) {
                    "Tap to disable"
                } else {
                    "Tap to enable"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )


            Spacer(modifier = Modifier.padding(vertical = 2.dp))

        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    WorkProfileSwitchTheme(darkTheme = true) {
        MainScreen(
            hasPermission = false,
            workProfileEnabled = false,
            onToggleWorkProfile = {},
            onShowPermissionDialog = {},
        )
    }
}