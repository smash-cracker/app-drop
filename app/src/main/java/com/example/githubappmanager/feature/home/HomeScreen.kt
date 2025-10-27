package com.example.githubappmanager.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
// import androidx.compose.material3.DismissDirection
// import androidx.compose.material3.DismissState
// import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.domain.model.GitHubRepo
//import androidx.compose.foundation.layout.matchParentSize
import com.example.githubappmanager.feature.common.components.RepoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repos: List<GitHubRepo>,
    downloadProgress: Map<String, DownloadProgress>,
    onRefreshRepo: (GitHubRepo) -> Unit,
    onInstallApp: (GitHubRepo) -> Unit,
    onUninstallApp: (GitHubRepo) -> Unit,
    onClearProgress: (GitHubRepo) -> Unit,
    onRepoClick: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveRepo: ((String) -> Unit)? = null // ✅ optional callback for swipe-to-delete
) {
    if (repos.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No repositories yet")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(repos, key = { it.url }) { repo ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart ||
                            value == SwipeToDismissBoxValue.StartToEnd
                        ) {
                            onRemoveRepo?.invoke(repo.url)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                    val color = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart,
                    SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(alpha = 0.7f)
                    else -> Color.Transparent
                }
                 Box(
                    modifier = Modifier
                 .fillMaxSize() // replaces matchParentSize for full coverage
                .background(color),
                contentAlignment = Alignment.Center
         ) {
             Text("Deleting...", color = Color.White)
             }
        },
                    content = {
                        RepoCard(
                            repo = repo,
                            downloadProgress = downloadProgress[repo.url],
                            onRefresh = { onRefreshRepo(repo) },
                            onInstall = { onInstallApp(repo) },
                            onUninstall = { onUninstallApp(repo) },
                            onClearProgress = { onClearProgress(repo) },
                            onClick = { onRepoClick(repo) }
                        )
                    }
                )
            }
        }
    }
}
