package com.ownik.workprofileswitch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme

@Composable
internal fun SupportDialog(
    state: SupportUiState,
    onPurchase: (SupportTier) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onLater: () -> Unit,
    onNeverAsk: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.buy_developer_coffee)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.support_description))
                when {
                    state.thankYou -> Text(stringResource(R.string.support_thanks))
                    state.processing -> Text(stringResource(R.string.support_processing))
                    state.pending -> Text(stringResource(R.string.support_pending))
                }
                if (state.loading) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.support_loading))
                }
                state.error?.let { error ->
                    val message = when (error) {
                        SupportError.UNAVAILABLE -> R.string.support_unavailable
                        SupportError.PURCHASE -> R.string.support_error
                        SupportError.PROCESSING -> R.string.support_processing_error
                    }
                    Text(
                        text = stringResource(message),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry, enabled = !state.busy && !state.loading) {
                        Text(stringResource(R.string.support_retry))
                    }
                }
                if (!state.thankYou) {
                    state.options.forEach { option ->
                        val title = when (option.tier) {
                            SupportTier.SMALL -> R.string.support_small
                            SupportTier.MEDIUM -> R.string.support_medium
                            SupportTier.LARGE -> R.string.support_large
                        }
                        OutlinedButton(
                            onClick = { onPurchase(option.tier) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.canPurchase,
                        ) {
                            Text(stringResource(
                                R.string.support_option, stringResource(title), option.price
                            ))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = if (state.automaticPrompt) onLater else onDismiss,
                enabled = !state.busy,
            ) {
                Text(stringResource(
                    if (state.automaticPrompt) R.string.support_later else R.string.close
                ))
            }
        },
        dismissButton = {
            if (state.automaticPrompt) {
                TextButton(onClick = onNeverAsk, enabled = !state.busy) {
                    Text(stringResource(R.string.support_never))
                }
            }
        },
    )
}

@Preview
@Composable
private fun SupportDialogPreview() {
    WorkProfileSwitchTheme {
        SupportDialog(
            state = SupportUiState(
                dialogVisible = true,
                automaticPrompt = true,
                purchasesReady = true,
                options = listOf(
                    SupportOption(SupportTier.SMALL, "$1.00"),
                    SupportOption(SupportTier.MEDIUM, "$3.00"),
                    SupportOption(SupportTier.LARGE, "$5.00"),
                ),
            ),
            onPurchase = {},
            onRetry = {},
            onDismiss = {},
            onLater = {},
            onNeverAsk = {},
        )
    }
}
