package com.example.githubappmanager.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.domain.model.GitHubRepo
import com.example.githubappmanager.feature.common.components.RepoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repos: List<GitHubRepo>,
    downloadProgress: Map<String, DownloadProgress>,
    onInstallApp: (GitHubRepo) -> Unit,
    onUninstallApp: (GitHubRepo) -> Unit,
    onClearProgress: (GitHubRepo) -> Unit,
    onRepoClick: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveRepo: ((String) -> Unit)? = null
) {
    var selectionMode by remember { mutableStateOf(false) }
    var selectedRepos by remember { mutableStateOf(setOf<String>()) }

    // Exit selection mode automatically if no repos selected
    LaunchedEffect(selectedRepos) {
        if (selectedRepos.isEmpty()) selectionMode = false
    }

    if (repos.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No repositories yet")
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {

            // ---------- Top Row (Visible only in selection mode) ----------
            if (selectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedRepos.size} selected",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val isAllSelected = selectedRepos.size == repos.size
                            val isPartialSelection = selectedRepos.isNotEmpty() && !isAllSelected

                            val clearButtonLabel = when {
                                isAllSelected -> "Clear All"
                                isPartialSelection -> "Clear"
                                else -> "Clear"
                            }

                            // --- Select All button (always visible) ---
                            TextButton(onClick = {
                                selectedRepos = repos.map { it.url }.toSet()
                                selectionMode = true
                            }) {
                                Text("Select All")
                            }

                            // --- Clear / Clear All button ---
                            TextButton(onClick = {
                                selectedRepos = emptySet()
                                selectionMode = false
                            }) {
                                Text(clearButtonLabel)
                            }

                            // --- Exit button ---
                            TextButton(onClick = {
                                selectedRepos = emptySet()
                                selectionMode = false
                            }) {
                                Text("Exit")
                            }
                        }
                    }
                }
            }

            // ---------- Repo List ----------
            LazyColumn(
                modifier = Modifier.weight(1f),
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
                            } else false
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
                                    .fillMaxSize()
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
                                onInstall = { onInstallApp(repo) },
                                onUninstall = { onUninstallApp(repo) },
                                onClearProgress = { onClearProgress(repo) },
                                onClick = {
                                    if (selectionMode) {
                                        selectedRepos = if (selectedRepos.contains(repo.url))
                                            selectedRepos - repo.url
                                        else
                                            selectedRepos + repo.url
                                    } else {
                                        onRepoClick(repo)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selectedRepos = setOf(repo.url)
                                    }
                                },
                                isSelectable = selectionMode,
                                isSelected = selectedRepos.contains(repo.url)
                            )
                        }
                    )
                }
            }
        }
    }
}
