package com.example.githubappmanager.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.domain.model.GitHubRepo
import com.example.githubappmanager.feature.common.components.RepoCard
import com.example.githubappmanager.feature.common.components.SearchBar

@Composable
fun SearchScreen(
    repos: List<GitHubRepo>,
    downloadProgress: Map<String, DownloadProgress>,
    onRefreshRepo: (GitHubRepo) -> Unit,
    onInstallApp: (GitHubRepo) -> Unit,
    onUninstallApp: (GitHubRepo) -> Unit,
    onAddRepo: (String) -> Unit,
    onClearRecentlyViewed: () -> Unit,
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
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSubmitUrl = onAddRepo,
            onClearRecentlyViewed = onClearRecentlyViewed
        )

        if (filteredRepos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching repositories found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRepos) { repo ->
                    RepoCard(
                        repo = repo,
                        downloadProgress = downloadProgress[repo.url],
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
