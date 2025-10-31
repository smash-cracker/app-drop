package com.example.githubappmanager.feature.common.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
    // onRefresh: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onClearProgress: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelectable: Boolean = false,
    isSelected: Boolean = false
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox only when in selection mode
            if (isSelectable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick?.invoke() },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .align(Alignment.CenterVertically)
                )
            }

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
                            } else 0
                            Text(
                                text = "Downloading... $progressPercent%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            LinearProgressIndicator(
                                progress = if (progress.totalBytes > 0) {
                                    progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()
                                } else 0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // IconButton(onClick = onRefresh) {
                    //     Icon(
                    //         imageVector = Icons.Filled.Refresh,
                    //         contentDescription = "Refresh",
                    //         tint = MaterialTheme.colorScheme.primary
                    //     )
                    // }

                downloadProgress?.let { progress ->
                    when {
                        progress.error != null -> {
                            IconButton(onClick = onClearProgress) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
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
