package com.example.githubappmanager.domain.model

/**
 * Merge a `GitHubRepoInfo` snapshot into the domain model while keeping existing
 * release/install metadata untouched.
 */
fun GitHubRepo.withInfo(info: GitHubRepoInfo): GitHubRepo = copy(
    name = info.name,
    owner = info.owner.login,
    stargazersCount = info.stargazersCount,
    forksCount = info.forksCount,
    watchersCount = info.watchersCount
)
