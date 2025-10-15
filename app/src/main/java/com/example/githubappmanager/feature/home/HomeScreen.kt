package com.example.githubappmanager.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.domain.model.GitHubRepo
import com.example.githubappmanager.feature.common.components.RepoCard

@Composable
fun HomeScreen(
    repos: List<GitHubRepo>,
    downloadProgress: Map<String, DownloadProgress>,
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
                    onRefresh = { onRefreshRepo(repo) },
                    onInstall = { onInstallApp(repo) },
                    onUninstall = { onUninstallApp(repo) },
                    onClearProgress = { onClearProgress(repo) }
                )
            }
        }
    }
}
