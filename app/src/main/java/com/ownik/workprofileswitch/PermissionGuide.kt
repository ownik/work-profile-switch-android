package com.ownik.workprofileswitch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme


@Composable
fun PermissionGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(stringResource(R.string.how_to_grant_permission))
        },

        text = {
            PermissionGuideDialogContent()
        },

        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun PermissionGuideDialogContent() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = AnnotatedString
                .fromHtml(
                    stringResource(R.string.grant_permission_guide)
                        .replace("{appId}", context.packageName)
                )

        )
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionGuideDialogPreview() {
    WorkProfileSwitchTheme {
        Surface {
            PermissionGuideDialogContent()
        }
    }
}