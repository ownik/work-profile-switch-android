package com.ownik.workprofileswitch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ownik.workprofileswitch.ui.theme.Failure
import com.ownik.workprofileswitch.ui.theme.Success
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme

@Composable
fun PermissionCard(
    hasPermission: Boolean,
    onHowToGrantClick: () -> Unit
) {
    val color = if (hasPermission) {
        Success
    } else {
        Failure
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Permission",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (hasPermission) {
                            "Permission granted"
                        } else {
                            "Permission required"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (hasPermission) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Error
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = color
                )
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "How to grant permission",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 56.dp)
                        .clickable(onClick = onHowToGrantClick)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionCardPreview() {
    WorkProfileSwitchTheme {
        PermissionCard(
            hasPermission = true,
            onHowToGrantClick = {}
        )
    }
}