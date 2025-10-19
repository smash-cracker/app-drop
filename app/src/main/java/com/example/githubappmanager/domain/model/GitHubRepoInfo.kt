package com.example.githubappmanager.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepoInfo(
    val name: String,
    val owner: Owner,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    @SerialName("watchers_count") val watchersCount: Int = 0,
    @SerialName("open_issues_count") val openIssuesCount: Int = 0,
    val description: String? = null
) {
    @Serializable
    data class Owner(
        val login: String,
        @SerialName("avatar_url") val avatarUrl: String
    )
}
