package com.example.githubappmanager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubappmanager.data.AppInstallStatus
import com.example.githubappmanager.data.GitHubRepo
import com.example.githubappmanager.ui.theme.GithubAppManagerTheme
import com.example.githubappmanager.utils.DownloadProgress
import com.example.githubappmanager.widgets.SearchBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent { GithubAppManagerApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GithubAppManagerApp() {
    GithubAppManagerTheme {
        val viewModel: RepoViewModel = viewModel()
        val repos by viewModel.repos.collectAsState(initial = emptyList())
        val recentlyViewed by viewModel.recentlyViewed.collectAsState(initial = emptyList())
        val downloadProgress by viewModel.downloadProgress.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        var showAddDialog by rememberSaveable { mutableStateOf(false) }
        var selectedTab by rememberSaveable { mutableStateOf("home") }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { CenterAlignedTopAppBar(title = { Text("GitHub App Manager") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == "home",
                        onClick = { selectedTab = "home" },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "search",
                        onClick = { selectedTab = "search" },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "bookmark",
                        onClick = { selectedTab = "bookmark" },
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Bookmark") },
                        label = { Text("Bookmark") }
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            when (selectedTab) {
                "home" -> RepoListScreen(
                    repos = repos,
                    downloadProgress = downloadProgress,
                    onDeleteRepo = { repo -> viewModel.removeRepo(repo.url) },
                    onRefreshRepo = { repo -> viewModel.refreshRepo(repo) },
                    onInstallApp = { repo -> viewModel.downloadAndInstallApk(repo) },
                    onUninstallApp = { repo ->
                        repo.packageName?.let { viewModel.uninstallApp(it) } ?: Log.w(
                            "MainActivity",
                            "Uninstall clicked but packageName is null for ${repo.url}"
                        )
                    },
                    onClearProgress = { repo -> viewModel.clearDownloadProgress(repo.url) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )

                "search" -> SearchScreen(
                    repos = recentlyViewed,
                    downloadProgress = downloadProgress,
                    onRefreshRepo = { repo -> viewModel.refreshRepo(repo) },
                    onInstallApp = { repo -> viewModel.downloadAndInstallApk(repo) },
                    onUninstallApp = { repo -> repo.packageName?.let { viewModel.uninstallApp(it) } },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )

                "bookmark" -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Bookmarks (placeholder)")
                }
            }
        }

        if (showAddDialog) {
            AddRepoDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { url ->
                    viewModel.addRepo(url)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun RepoListScreen(
    repos: List<GitHubRepo>,
    downloadProgress: Map<String, DownloadProgress>,
    onDeleteRepo: (GitHubRepo) -> Unit,
    onRefreshRepo: (GitHubRepo) -> Unit,
    onInstallApp: (GitHubRepo) -> Unit,
    onUninstallApp: (GitHubRepo) -> Unit,
    onClearProgress: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (repos.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("No repositories yet") }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(repos) { repo ->
                RepoCard(
                    repo = repo,
                    downloadProgress = downloadProgress[repo.url],
                    onDelete = { onDeleteRepo(repo) },
                    onRefresh = { onRefreshRepo(repo) },
                    onInstall = { onInstallApp(repo) },
                    onUninstall = { onUninstallApp(repo) },
                    onClearProgress = { onClearProgress(repo) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repos: List<GitHubRepo>,
    downloadProgress: Map<String, DownloadProgress>,
    onRefreshRepo: (GitHubRepo) -> Unit,
    onInstallApp: (GitHubRepo) -> Unit,
    onUninstallApp: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredRepos = remember(searchQuery, repos) {
        if (searchQuery.isBlank()) repos
        else repos.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.padding(16.dp)) {

        // ✅ Use SearchBar from widgets
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        if (filteredRepos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("No matching repositories found") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRepos) { repo ->
                    RepoCard(
                        repo = repo,
                        downloadProgress = downloadProgress[repo.url],
                        onDelete = {}, // No delete in search
                        onRefresh = { onRefreshRepo(repo) },
                        onInstall = { onInstallApp(repo) },
                        onUninstall = { onUninstallApp(repo) },
                        onClearProgress = {}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepoCard(
    repo: GitHubRepo,
    downloadProgress: DownloadProgress?,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onClearProgress: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                        if (progress.error != null) {
                            Text(
                                text = "Download failed: ${progress.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (!progress.isComplete) {
                            val progressPercent = if (progress.totalBytes > 0)
                                (progress.bytesDownloaded * 100 / progress.totalBytes).toInt() else 0
                            Text(
                                text = "Downloading... $progressPercent%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            LinearProgressIndicator(
                                progress = if (progress.totalBytes > 0)
                                    progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()
                                else 0f,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }

                    downloadProgress?.let { progress ->
                        if (progress.error != null) {
                            IconButton(onClick = onClearProgress) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Clear error", tint = MaterialTheme.colorScheme.error)
                            }
                        } else if (!progress.isComplete) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } ?: run {
                        when (repo.installStatus) {
                            AppInstallStatus.NOT_INSTALLED -> {
                                IconButton(onClick = onInstall) {
                                    Icon(Icons.Filled.CloudDownload, contentDescription = "Install", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            AppInstallStatus.INSTALLED_OUTDATED -> {
                                IconButton(onClick = onInstall) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Update", tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            AppInstallStatus.INSTALLED_CURRENT -> {
                                IconButton(onClick = onUninstall) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Uninstall", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            AppInstallStatus.UNKNOWN -> {
                                if (repo.latestRelease?.androidAssets?.isNotEmpty() == true) {
                                    IconButton(onClick = onInstall) {
                                        Icon(Icons.Filled.CloudDownload, contentDescription = "Install", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRepoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val error = remember(url) { validateGithubUrl(url) }
    val isValid = error == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add GitHub repository") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text("Paste repo URL (e.g., https://github.com/user/app)") },
                    supportingText = { if (!isValid && url.isNotBlank()) Text(error!!) },
                    isError = !isValid && url.isNotBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(url.trim()) }, enabled = isValid && url.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun validateGithubUrl(input: String): String? {
    val s = input.trim()
    if (s.isEmpty()) return "URL cannot be empty"

    val httpsRegex = Regex("""^(https?://)?(www\.)?github\.com/[^/\s]+/[^/\s]+/?(\.git)?$""", RegexOption.IGNORE_CASE)
    val sshRegex = Regex("""^git@github\.com:[^/\s]+/[^/\s]+(\.git)?$""", RegexOption.IGNORE_CASE)

    return if (httpsRegex.matches(s) || sshRegex.matches(s)) null
    else "Enter a valid GitHub repo URL (e.g., github.com/user/repo)"
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    GithubAppManagerApp()
}