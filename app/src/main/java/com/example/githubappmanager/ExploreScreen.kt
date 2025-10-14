package com.example.githubappmanager.ui.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.data.GitHubRepo

/**
 * A completely separate Explore page — not tied to Home, Search, or Bookmarks.
 * You can later connect it to the GitHub API to show trending or popular repositories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    modifier: Modifier = Modifier,
    trendingRepos: List<GitHubRepo> = emptyList(),
    onRepoClick: (GitHubRepo) -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Explore") },
                navigationIcon = {
                    Icon(
                        Icons.Filled.Public,
                        contentDescription = "Explore",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        if (trendingRepos.isEmpty()) {
            EmptyExploreState(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding))
        } else {
            ExploreList(
                repos = trendingRepos,
                onRepoClick = onRepoClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun EmptyExploreState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Discover trending and popular repositories here!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExploreList(
    repos: List<GitHubRepo>,
    onRepoClick: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(repos) { repo ->
            ExploreRepoCard(repo = repo, onClick = { onRepoClick(repo) })
        }
    }
}

@Composable
private fun ExploreRepoCard(
    repo: GitHubRepo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
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
        }
    }
}
