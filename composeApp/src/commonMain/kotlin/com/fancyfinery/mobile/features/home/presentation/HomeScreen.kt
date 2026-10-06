package com.fancyfinery.mobile.features.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.features.auth.domain.model.User
import com.fancyfinery.mobile.features.home.OnLogout
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: OnLogout) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsState()
    var menuExpanded by remember(calculation = { mutableStateOf(false) })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "KotlinStarter",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    IconButton(
                        onClick = { menuExpanded = true },
                        content = {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Menu",
                            )
                        },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        content = {
                            DropdownMenuItem(
                                text = { Text(text = "Sign Out") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.onLogout(onLogout = onLogout)
                                },
                            )
                        },
                    )
                },
            )
        },
        content = { paddingValues ->
            when {
                state.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = paddingValues),
                    contentAlignment = Alignment.Center,
                    content = { CircularProgressIndicator() },
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = paddingValues)
                        .verticalScroll(state = rememberScrollState())
                        .padding(all = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = {
                        state.user?.let { user ->
                            ProfileCard(user = user)
                        }
                        PlaceholderSection()
                    },
                )
            }
        },
    )
}

@Composable
private fun ProfileCard(user: User) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        content = {
            Row(
                modifier = Modifier.padding(all = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        content = {
                            Box(
                                contentAlignment = Alignment.Center,
                                content = {
                                    Text(
                                        text = user.name.firstOrNull()?.uppercaseChar()?.toString()
                                            ?: "?",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                            )
                        },
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        content = {
                            Text(
                                text = "Hello, ${user.name} 👋",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        },
                    )
                },
            )
        },
    )
}

@Composable
private fun PlaceholderSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        content = {
            Column(
                modifier = Modifier.padding(all = 20.dp),
                content = {
                    Text(
                        text = "Your feature goes here",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add new slices under features/ following the vertical slice pattern. Each slice owns its data, domain, and presentation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        },
    )
}
