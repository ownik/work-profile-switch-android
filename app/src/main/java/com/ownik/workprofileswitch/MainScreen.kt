package com.ownik.workprofileswitch

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme

// PowerButton's fixed height, used to work out the offset that puts the
// button itself dead center — the state texts below it don't factor in,
// since their height can vary (localization, font scale) without moving
// the button.
private val PowerButtonHeight = 320.dp

@Composable
fun MainScreen(
    hasPermission: Boolean,
    workProfileFound: Boolean,
    workProfileEnabled: Boolean,
    onToggleWorkProfile: suspend () -> Unit,
    onShowPermissionDialog: () -> Unit,
    onSupportDeveloper: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            MainScreenBottomBar(onSupportDeveloper = onSupportDeveloper)
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),
        ) {
            // Center the button, but move the controls up in short windows
            // so their labels remain above the footer.
            var controlsHeight by remember { mutableIntStateOf(0) }
            val controlsHeightDp = with(LocalDensity.current) { controlsHeight.toDp() }
            val centeredOffset = ((maxHeight - PowerButtonHeight) / 2).coerceAtLeast(0.dp)
            val buttonTopOffset = minOf(
                centeredOffset,
                (maxHeight - controlsHeightDp).coerceAtLeast(0.dp),
            )

            // PowerButton stays fixed in place, independent of whether the
            // permission card above is shown.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = buttonTopOffset)
                    .onSizeChanged { controlsHeight = it.height }
            ) {
                PowerButton(
                    enabled = workProfileFound && hasPermission,
                    isOn = workProfileEnabled,
                    onClick = {
                        onToggleWorkProfile()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PowerButtonHeight)
                )

                Spacer(modifier = Modifier.padding(vertical = 4.dp))

                // Current state
                Text(
                    text = if (workProfileEnabled) {
                        stringResource(R.string.work_profile_enabled)
                    } else {
                        stringResource(R.string.work_profile_disabled)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (workProfileEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )

                Spacer(modifier = Modifier.padding(vertical = 2.dp))

                Text(
                    text = if (workProfileEnabled) {
                        stringResource(R.string.tap_to_disable)
                    } else {
                        stringResource(R.string.tap_to_enable)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (workProfileEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),

                )

                Spacer(modifier = Modifier.padding(vertical = 2.dp))
            }

            // Overlaid on top; appearing/disappearing never shifts PowerButton.
            if(!workProfileFound) {
                WorkProfileNotFound(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            } else if (!hasPermission) {
                PermissionCard(
                    onHowToGrantClick = {
                        onShowPermissionDialog()
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MainScreenBottomBar(onSupportDeveloper: () -> Unit) {
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
                .clickable(onClick = onSupportDeveloper),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.buy_developer_coffee),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
                text = stringResource(R.string.view_source_on_github),
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.padding(horizontal = 2.dp))

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
@Preview(name = "Short window", widthDp = 960, heightDp = 600, showBackground = true)
@Composable
fun MainScreenPreview() {
    WorkProfileSwitchTheme(darkTheme = true) {
        MainScreen(
            hasPermission = false,
            workProfileFound = false,
            workProfileEnabled = false,
            onToggleWorkProfile = {},
            onShowPermissionDialog = {},
            onSupportDeveloper = {},
        )
    }
}