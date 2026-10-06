package com.fancyfinery.mobile.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fancyfinery.mobile.core.platform.UrlOpener
import com.fancyfinery.mobile.core.theme.BrandWordmark
import com.fancyfinery.mobile.features.auth.AuthMode
import com.fancyfinery.mobile.features.auth.OnAuthSuccess
import com.fancyfinery.mobile.features.auth.presentation.components.AuthTextField
import com.fancyfinery.mobile.features.auth.presentation.components.PasswordTextField
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Sign in, or create an account.
 *
 * Carries every method the website offers, because an account made in a browser
 * has to be reachable from the app and vice versa — a customer who signed up
 * with Google and then cannot find that option here simply concludes the app is
 * broken:
 *
 *   · email + password
 *   · Google
 *   · a one-time sign-in link ("email me a link")
 *   · password reset
 */
@Composable
fun AuthScreen(
    initialMode: AuthMode = AuthMode.LOGIN,
    onAuthSuccess: OnAuthSuccess,
    onDismiss: (() -> Unit)? = null,
    viewModel: AuthViewModel = koinViewModel { org.koin.core.parameter.parametersOf(initialMode) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val urlOpener = koinInject<UrlOpener>()
    val isRegister = state.mode == AuthMode.REGISTER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        BrandWordmark(fontSize = 30.sp, tagline = "ELEGANCE REDEFINED")
        Spacer(Modifier.height(28.dp))

        Text(
            text = if (isRegister) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(20.dp))

        // "We've emailed you" — a success that is not a sign-in.
        state.notice?.let { notice ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (isRegister) {
            AuthTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Full name",
                error = state.nameError,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            error = state.emailError,
            // An email keyboard: no autocapitalise, and an @ on the primary
            // layer. Typing an address on a text keyboard is a small, constant
            // irritation that shows up as "invalid email" errors.
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        PasswordTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            error = state.passwordError,
            modifier = Modifier.fillMaxWidth(),
        )

        if (isRegister) {
            Spacer(Modifier.height(12.dp))
            PasswordTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Confirm password",
                error = state.confirmPasswordError,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.generalError?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { viewModel.onSubmit(onAuthSuccess) },
            enabled = !state.isLoading,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (isRegister) "Create account" else "Sign in")
            }
        }

        if (!isRegister) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = viewModel::onMagicLink,
                    enabled = !state.isLoading,
                ) { Text("Email me a link", style = MaterialTheme.typography.labelLarge) }

                TextButton(
                    onClick = viewModel::onForgotPassword,
                    enabled = !state.isLoading,
                ) { Text("Forgot password?", style = MaterialTheme.typography.labelLarge) }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  or  ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        /**
         * Google.
         *
         * The consent screen opens in a browser tab rather than a WebView —
         * Google refuses OAuth from embedded WebViews outright, so this is the
         * only flow that works at all, as well as the one that lets a customer
         * reuse a session they already have.
         */
        OutlinedButton(
            onClick = {
                viewModel.onGoogleSignIn(
                    callbackUrl = GOOGLE_CALLBACK,
                    onOpenUrl = urlOpener::open,
                )
            },
            enabled = !state.isGoogleLoading && !state.isLoading,
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            if (state.isGoogleLoading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text("Continue with Google")
            }
        }

        Spacer(Modifier.height(20.dp))

        TextButton(onClick = viewModel::onToggleMode) {
            Text(
                text = if (isRegister) {
                    "Already have an account? Sign in"
                } else {
                    "New here? Create an account"
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }

        // Shown only when auth was reached as a step inside another flow —
        // checkout, say — so the customer can back out and keep browsing.
        onDismiss?.let {
            TextButton(onClick = it) {
                Text("Continue browsing", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Where Google returns the customer after consent.
 *
 * Points at the website, not at the app. The server completes the OAuth
 * exchange and establishes the session there; the app picks the session up
 * afterwards. Pointing this at a custom scheme would need that scheme
 * registered as a trusted origin server-side first.
 */
private const val GOOGLE_CALLBACK = "/"
