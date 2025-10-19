package com.example.githubappmanager.feature.details

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
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
    BackHandler(onBack = onBack)

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
    val context = LocalContext.current
    val isInstalled = repo.installStatus == AppInstallStatus.INSTALLED_CURRENT ||
            repo.installStatus == AppInstallStatus.INSTALLED_OUTDATED

    val painter: Painter? = remember(repo.packageName, isInstalled) {
        if (isInstalled && repo.packageName != null) {
            try {
                val drawable: Drawable = context.packageManager.getApplicationIcon(repo.packageName)
                androidx.compose.ui.graphics.painter.BitmapPainter(drawable.toBitmap().asImageBitmap())
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App icon + name + owner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = "App icon",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = "https://avatars.githubusercontent.com/${repo.owner}",
                    contentDescription = "Owner avatar",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = repo.displayName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = repo.owner,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 🌟 Info row (Dynamic)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ⭐ Stars
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Stars",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${repo.stargazersCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 📦 Size
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = "App Size",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "50 MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

    // Vertical divider
            Divider(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 🔢 Version
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Version",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = release?.tagName ?: "v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Existing release/download UI ---
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

        // Latest Release Section with "See More"
        release?.let {
            Divider()
            Text(
                text = "Latest Release",
                style = MaterialTheme.typography.titleMedium
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
                var expanded by remember { mutableStateOf(false) }
                Column {
                    Text(
                        text = it.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Show less" else "See more")
                    }
                }
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

// --- StatusActions and PrimaryActionButton unchanged ---
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
    icon: ImageVector,
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
