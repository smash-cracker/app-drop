package com.example.githubappmanager.feature.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.domain.model.AppInstallStatus
import com.example.githubappmanager.domain.model.GitHubRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoCard(
    repo: GitHubRepo,
    downloadProgress: DownloadProgress?,
    onRefresh: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onClearProgress: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = modifier.fillMaxWidth()

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repo.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = repo.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    repo.latestRelease?.let { release ->
                        Text(
                            text = "Latest: ${release.tagName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    downloadProgress?.let { progress ->
                        when {
                            progress.error != null -> {
                                Text(
                                    text = "Download failed: ${progress.error}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            !progress.isComplete -> {
                                val progressPercent = if (progress.totalBytes > 0) {
                                    (progress.bytesDownloaded * 100 / progress.totalBytes).toInt()
                                } else {
                                    0
                                }
                                Text(
                                    text = "Downloading... $progressPercent%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                LinearProgressIndicator(
                                    progress = if (progress.totalBytes > 0) {
                                        progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()
                                    } else {
                                        0f
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    downloadProgress?.let { progress ->
                        when {
                            progress.error != null -> {
                                IconButton(onClick = onClearProgress) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Clear error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            !progress.isComplete -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    } ?: run {
                        when (repo.installStatus) {
                            AppInstallStatus.NOT_INSTALLED -> {
                                IconButton(onClick = onInstall) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDownload,
                                        contentDescription = "Install",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            AppInstallStatus.INSTALLED_OUTDATED -> {
                                IconButton(onClick = onInstall) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Update",
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            AppInstallStatus.INSTALLED_CURRENT -> {
                                IconButton(onClick = onUninstall) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Uninstall",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            AppInstallStatus.UNKNOWN -> {
                                if (repo.latestRelease?.androidAssets?.isNotEmpty() == true) {
                                    IconButton(onClick = onInstall) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudDownload,
                                            contentDescription = "Install",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (onClick != null) {
        Card(
            modifier = cardModifier,
            onClick = onClick,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            cardContent()
        }
    } else {
        Card(
            modifier = cardModifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            cardContent()
        }
    }
}
