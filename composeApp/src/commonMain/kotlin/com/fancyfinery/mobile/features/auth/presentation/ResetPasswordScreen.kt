package com.fancyfinery.mobile.features.auth.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fancyfinery.mobile.core.theme.BrandWordmark
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.features.auth.presentation.components.PasswordTextField
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Set a new password, reached by following the emailed link.
 *
 * The token in the link is the only credential involved — there is no session,
 * and the customer could not create one if asked, which is the whole reason
 * they are here. Nothing on this screen requires signing in.
 */
@Composable
fun ResetPasswordScreen(
    token: String,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ResetPasswordViewModel = koinViewModel { parametersOf(token) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        BrandWordmark(fontSize = 28.sp, tagline = "ACCOUNT SECURITY")
        Spacer(Modifier.height(28.dp))

        if (state.isDone) {
            Text(
                text = "Password updated",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You can sign in with it on any device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Sign in") }
            return@Column
        }

        Text(
            text = "Set a new password",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(20.dp))

        PasswordTextField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = "New password",
            error = state.passwordError,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PasswordTextField(
            value = state.confirm,
            onValueChange = viewModel::onConfirm,
            label = "Confirm new password",
            error = state.confirmError,
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::onSubmit,
            enabled = !state.isSaving,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Update password")
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel) { Text("Back to the shop", color = Gold) }
    }
}
