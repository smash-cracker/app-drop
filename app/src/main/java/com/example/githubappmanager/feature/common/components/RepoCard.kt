package com.example.githubappmanager.feature.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.domain.model.AppInstallStatus
import com.example.githubappmanager.domain.model.GitHubRepo
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoCard(
    repo: GitHubRepo,
    downloadProgress: DownloadProgress?,
    // onRefresh: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onClearProgress: () -> Unit,
    onCancelDownload: () -> Unit, // ✅ Added here
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelectable: Boolean = false,
    isSelected: Boolean = false
) {
    val context = LocalContext.current

    // --- Try to get installed app icon if available ---
    val appIconPainter: Painter? = remember(repo.packageName) {
        if (repo.installStatus == AppInstallStatus.INSTALLED_CURRENT ||
            repo.installStatus == AppInstallStatus.INSTALLED_OUTDATED
        ) {
            try {
                val drawable = context.packageManager.getApplicationIcon(repo.packageName!!)
                BitmapPainter(drawable.toBitmap().asImageBitmap())
            } catch (e: Exception) {
                null
            }
        } else null
    }

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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Checkbox (only in selection mode) ---
            if (isSelectable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick?.invoke() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // --- App Icon, Avatar, or Placeholder ---
            when {
                appIconPainter != null -> {
                    Image(
                        painter = appIconPainter,
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFEFEF)),
                        contentScale = ContentScale.Crop
                    )
                }

                !repo.owner.isNullOrBlank() -> {
                    AsyncImage(
                        model = "https://avatars.githubusercontent.com/${repo.owner}",
                        contentDescription = "Owner Avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFEFEF)),
                        contentScale = ContentScale.Crop,
                        error = painterResourcePlaceholder()
                    )
                }

                else -> {
                    // --- Default placeholder icon ---
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "Placeholder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // --- Repo Info & Progress ---
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

            // --- Action icons (Install/Uninstall/etc.) ---
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
                            // ✅ Cancel Download Button
                            IconButton(onClick = onCancelDownload) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel download",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
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

/**
 * Provides a simple painter for placeholder/error cases.
 */
@Composable
private fun painterResourcePlaceholder(): Painter {
    return rememberVectorPainter(Icons.Default.Android)
}
