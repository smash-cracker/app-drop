package com.example.githubappmanager.feature.details

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    repo: GitHubRepo,
    downloadProgress: DownloadProgress?,
    onRefresh: suspend () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = repo.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        // ✅ Modern PullToRefreshBox API (Compose 1.7+)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    snackbarHostState.showSnackbar("Refreshing repo details...")

                    val refreshStart = System.currentTimeMillis()
                    try {
                        // Perform refresh logic
                        onRefresh()
                        // Always show success message
                        snackbarHostState.showSnackbar("✅ Refreshed successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Refresh failed: ${e.message ?: "Unknown error"}")
                    } finally {
                        // Ensure spinner stays visible for at least 2 seconds
                        val elapsed = System.currentTimeMillis() - refreshStart
                        val minVisibleTime = 2000L
                        if (elapsed < minVisibleTime) {
                            delay(minVisibleTime - elapsed)
                        }
                        isRefreshing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            RepoDetailContent(
                repo = repo,
                downloadProgress = downloadProgress,
                onInstall = onInstall,
                onUninstall = onUninstall,
                contentPadding = innerPadding
            )
        }
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
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val formattedApkSize = remember(repo.apkSizeBytes, release?.assets) {
        formatApkSize(repo)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- App icon + name + owner ---
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

        // --- Info Row: Stars, Size, Version ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoItem(Icons.Filled.Star, "${repo.stargazersCount}", "Stars", Color(0xFFFFD700))
            VerticalDivider()
            InfoItem(Icons.Filled.CloudDownload, formattedApkSize, "App Size", MaterialTheme.colorScheme.primary)
            VerticalDivider()
            InfoItem(Icons.Filled.Info, release?.tagName ?: "v1.0", "Version", MaterialTheme.colorScheme.primary)
        }

        // --- Download / Install section ---
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
                        progress = {
                            if (downloadProgress.totalBytes > 0) {
                                downloadProgress.bytesDownloaded.toFloat() / downloadProgress.totalBytes.toFloat()
                            } else 0f
                        },
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

        // --- Latest Release section ---
        release?.let {
            HorizontalDivider()
            Text(text = "Latest Release", style = MaterialTheme.typography.titleMedium)
            it.name?.let { name ->
                Text(text = name, style = MaterialTheme.typography.bodyMedium)
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
                    MarkdownText(
                        markdown = it.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(text = if (expanded) "Show less" else "See more", color = Color(0xFF005F73))
                    }
                }
            }
        } ?: run {
            HorizontalDivider()
            Text(
                text = "No release information available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
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
                PrimaryActionButton("Install", Icons.Filled.CloudDownload, onInstall, enabled = hasApk)
            }

            AppInstallStatus.INSTALLED_OUTDATED -> {
                PrimaryActionButton("Update", Icons.Filled.CloudDownload, onInstall, enabled = hasApk)
                TextButton(onClick = onUninstall) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uninstall")
                    }
                }
            }

            AppInstallStatus.INSTALLED_CURRENT -> {
                PrimaryActionButton("Uninstall", Icons.Filled.Delete, onUninstall)
            }

            AppInstallStatus.UNKNOWN -> {
                if (hasApk) {
                    PrimaryActionButton("Install", Icons.Filled.CloudDownload, onInstall)
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
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = enabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}

private fun formatApkSize(repo: GitHubRepo): String {
    val sizeBytes = repo.apkSizeBytes
        ?: repo.latestRelease?.preferredApk?.size
        ?: repo.latestRelease?.androidAssets?.firstOrNull()?.size
        ?: return "—"
    val megaBytes = sizeBytes.toDouble() / (1024 * 1024)
    return String.format(Locale.US, "%.1f MB", megaBytes)
}
