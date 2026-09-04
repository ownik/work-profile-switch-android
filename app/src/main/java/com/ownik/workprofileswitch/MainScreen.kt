package com.ownik.workprofileswitch

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme

@Composable
fun MainScreen(
    hasPermission: Boolean,
    workProfileEnabled: Boolean,
    onToggleWorkProfile: suspend () -> Unit,
    onShowPermissionDialog: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            MainScreenBottomBar()
        }
    ) { innerPadding ->
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

@Composable
fun MainScreenBottomBar() {
    val context = LocalContext.current

    val githubUrl = stringResource(R.string.github_url)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        githubUrl.toUri()
                    )
                    context.startActivity(intent)
                }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        )
        {
            val textStyle = MaterialTheme.typography.bodyMedium
            val density = LocalDensity.current

            val iconSize = with(density) {
                textStyle.fontSize.toDp()
            }

            Text(
                text = "View source on Github ",
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Icon(
                painter = painterResource(id = R.drawable.github_invertocat_black),
                contentDescription = "",
                modifier = Modifier.size(iconSize * 1.3f)
            )
        }

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_COMMIT})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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