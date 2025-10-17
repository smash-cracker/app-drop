package com.example.githubappmanager.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.domain.model.AppInstallStatus
import com.example.githubappmanager.domain.model.GitHubRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    repo: GitHubRepo,
    downloadProgress: DownloadProgress?,
    onRefresh: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(repo.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        RepoDetailContent(
            repo = repo,
            downloadProgress = downloadProgress,
            onInstall = onInstall,
            onUninstall = onUninstall,
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun RepoDetailContent(
    repo: GitHubRepo,
    downloadProgress: DownloadProgress?,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    contentPadding: PaddingValues
) {
    val release = repo.latestRelease

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = repo.url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        Text(
            text = "App Status",
            style = MaterialTheme.typography.titleMedium
        )

        when {
            downloadProgress?.error != null -> {
                Text(
                    text = "Download failed: ${downloadProgress.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            downloadProgress != null && !downloadProgress.isComplete -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Downloading latest APK…")
                    LinearProgressIndicator(
                        progress = if (downloadProgress.totalBytes > 0) {
                            downloadProgress.bytesDownloaded.toFloat() / downloadProgress.totalBytes.toFloat()
                        } else 0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${downloadProgress.bytesDownloaded} / ${downloadProgress.totalBytes} bytes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                StatusActions(
                    installStatus = repo.installStatus,
                    onInstall = onInstall,
                    onUninstall = onUninstall,
                    hasApk = release?.androidAssets?.isNotEmpty() == true
                )
            }
        }

        release?.let {
            Divider()
            Text(
                text = "Latest Release",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = it.tagName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            it.name?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Published at ${it.publishedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!it.body.isNullOrBlank()) {
                Text(
                    text = it.body,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } ?: run {
            Divider()
            Text(
                text = "No release information available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusActions(
    installStatus: AppInstallStatus,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    hasApk: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (installStatus) {
            AppInstallStatus.NOT_INSTALLED -> {
                PrimaryActionButton(
                    text = "Install",
                    icon = Icons.Filled.CloudDownload,
                    onClick = onInstall,
                    enabled = hasApk
                )
                if (!hasApk) {
                    Text(
                        text = "No APK assets found in the latest release.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppInstallStatus.INSTALLED_OUTDATED -> {
                PrimaryActionButton(
                    text = "Update",
                    icon = Icons.Filled.CloudDownload,
                    onClick = onInstall,
                    enabled = hasApk
                )
                if (!hasApk) {
                    Text(
                        text = "No APK assets found in the latest release.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onUninstall) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uninstall")
                    }
                }
            }

            AppInstallStatus.INSTALLED_CURRENT -> {
                Text(
                    text = "Latest version installed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                PrimaryActionButton(
                    text = "Uninstall",
                    icon = Icons.Filled.Delete,
                    onClick = onUninstall
                )
            }

            AppInstallStatus.UNKNOWN -> {
                if (hasApk) {
                    PrimaryActionButton(
                        text = "Install",
                        icon = Icons.Filled.CloudDownload,
                        onClick = onInstall
                    )
                } else {
                    Text(
                        text = "No installable artifacts detected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}
