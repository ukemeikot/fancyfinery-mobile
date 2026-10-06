package com.fancyfinery.mobile.features.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.features.auth.AuthMode
import com.fancyfinery.mobile.features.auth.OnAuthSuccess
import com.fancyfinery.mobile.features.auth.data.remote.AuthApi
import com.fancyfinery.mobile.features.auth.presentation.components.AuthTextField
import com.fancyfinery.mobile.features.auth.presentation.components.PasswordTextField
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AuthScreen(
    onAuthSuccess: OnAuthSuccess,
    initialMode: AuthMode = AuthMode.LOGIN,
) {
    val viewModel = koinViewModel<AuthViewModel>(
        parameters = { parametersOf(initialMode) },
    )
    val state by viewModel.state.collectAsState()
    val isLogin = state.mode == AuthMode.LOGIN

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(state = rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = {
                    Spacer(modifier = Modifier.height(48.dp))

                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        content = {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                content = {
                                    Text(
                                        text = "K",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                            )
                        },
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = when {
                            isLogin -> "Welcome back"
                            else -> "Create account"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isLogin -> "Sign in to continue"
                            else -> "Sign up to get started"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        content = {
                            Column(
                                modifier = Modifier.padding(all = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                content = {
                                    AnimatedVisibility(
                                        visible = !isLogin,
                                        enter = expandVertically(),
                                        exit = shrinkVertically(),
                                        content = {
                                            AuthTextField(
                                                value = state.name,
                                                onValueChange = { viewModel.onNameChange(value = it) },
                                                label = "Full Name",
                                                placeholder = "Jane Doe",
                                                error = state.nameError,
                                                imeAction = ImeAction.Next,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        },
                                    )

                                    AuthTextField(
                                        value = state.email,
                                        onValueChange = { viewModel.onEmailChange(value = it) },
                                        label = "Email",
                                        placeholder = "demo@example.com",
                                        error = state.emailError,
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next,
                                        modifier = Modifier.fillMaxWidth(),
                                    )

                                    PasswordTextField(
                                        value = state.password,
                                        onValueChange = { viewModel.onPasswordChange(value = it) },
                                        label = "Password",
                                        error = state.passwordError,
                                        imeAction = when {
                                            isLogin -> ImeAction.Done
                                            else -> ImeAction.Next
                                        },
                                        onImeAction = {
                                            when {
                                                isLogin -> viewModel.onSubmit(onSuccess = onAuthSuccess)
                                                else -> {}
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )

                                    AnimatedVisibility(
                                        visible = !isLogin,
                                        enter = expandVertically(),
                                        exit = shrinkVertically(),
                                        content = {
                                            PasswordTextField(
                                                value = state.confirmPassword,
                                                onValueChange = {
                                                    viewModel.onConfirmPasswordChange(
                                                        value = it
                                                    )
                                                },
                                                label = "Confirm Password",
                                                error = state.confirmPasswordError,
                                                imeAction = ImeAction.Done,
                                                onImeAction = { viewModel.onSubmit(onSuccess = onAuthSuccess) },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )

                    state.generalError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.onSubmit(onSuccess = onAuthSuccess) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            when {
                                state.isLoading -> CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )

                                else -> Text(
                                    text = when {
                                        isLogin -> "Sign In"
                                        else -> "Create Account"
                                    },
                                )
                            }
                        },
                    )

                    TextButton(
                        onClick = { viewModel.onToggleMode() },
                        content = {
                            Text(
                                text = when {
                                    isLogin -> "Don't have an account? Sign up"
                                    else -> "Already have an account? Sign in"
                                },
                            )
                        },
                    )

                    if (isLogin) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            content = {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    content = {
                                        Text(
                                            text = "Demo credentials",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = AuthApi.DEMO_EMAIL,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Text(
                                            text = AuthApi.DEMO_PASSWORD,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    },
                                )
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                },
            )
        },
    )
}
