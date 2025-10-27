package com.example.githubappmanager.feature.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.domain.model.GitHubRepo
import com.example.githubappmanager.feature.home.HomeScreen
import com.example.githubappmanager.feature.main.components.AddRepoDialog
import com.example.githubappmanager.feature.search.SearchScreen
import com.example.githubappmanager.feature.explore.ExploreScreen
import com.example.githubappmanager.feature.details.RepoDetailScreen
import com.example.githubappmanager.ui.theme.GithubAppManagerTheme

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
        var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
        var selectedRepo by remember { mutableStateOf<GitHubRepo?>(null) }

        val activeDetailRepo = selectedRepo?.let { current ->
            repos.find { it.url == current.url } ?: current
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = { CenterAlignedTopAppBar(title = { Text("GitHub App Manager") }) },
                bottomBar = {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                },
            ) { innerPadding ->
                when (selectedTab) {
                    MainTab.HOME -> HomeScreen(
                        repos = repos,
                        downloadProgress = downloadProgress,
                        onRefreshRepo = { repo -> viewModel.refreshRepo(repo) },
                        onInstallApp = { repo -> viewModel.downloadAndInstallApk(repo) },
                        onUninstallApp = { repo ->
                            repo.packageName?.let { viewModel.uninstallApp(it) } ?: Log.w(
                                "MainActivity",
                                "Uninstall clicked but packageName is null for ${repo.url}"
                            )
                        },
                        onClearProgress = { repo -> viewModel.clearDownloadProgress(repo.url) },
                        onRepoClick = { repo -> selectedRepo = repo },
                        onRemoveRepo = { url -> viewModel.removeRepo(url) }, // ✅ new swipe-delete hook
                        modifier = Modifier.padding(innerPadding)
                    )

                    MainTab.EXPLORE -> ExploreScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        onRepoClick = { repo -> selectedRepo = repo }
                    )

                    MainTab.SEARCH -> SearchScreen(
                        repos = recentlyViewed,
                        downloadProgress = downloadProgress,
                        onRefreshRepo = { repo -> viewModel.refreshRepo(repo) },
                        onInstallApp = { repo -> viewModel.downloadAndInstallApk(repo) },
                        onUninstallApp = { repo -> repo.packageName?.let { viewModel.uninstallApp(it) } },
                        onAddRepo = { url -> viewModel.addRepo(url) },
                        onClearRecentlyViewed = { viewModel.clearRecentlyViewed() },
                        onRepoClick = { repo -> selectedRepo = repo },
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )

                    MainTab.BOOKMARK -> BookmarkPlaceholder(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }

            activeDetailRepo?.let { repo ->
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    RepoDetailScreen(
                        repo = repo,
                        downloadProgress = downloadProgress[repo.url],
                        onRefresh = { viewModel.refreshRepo(repo) },
                        onInstall = { viewModel.downloadAndInstallApk(repo) },
                        onUninstall = {
                            repo.packageName?.let { viewModel.uninstallApp(it) } ?: Log.w(
                                "MainActivity",
                                "Uninstall requested but packageName is null for ${repo.url}"
                            )
                        },
                        onBack = { selectedRepo = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("Bookmarks (placeholder)")
    }
}

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    EXPLORE("Explore", Icons.Filled.Public),
    SEARCH("Search", Icons.Filled.Search),
    BOOKMARK("Bookmark", Icons.Filled.Bookmark)
}
