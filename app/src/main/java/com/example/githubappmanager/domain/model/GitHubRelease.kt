package com.example.githubappmanager.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String?,
    @SerialName("body") val body: String?,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("assets") val assets: List<GitHubAsset>,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false
) {
    val androidAssets: List<GitHubAsset>
        get() = assets.filter { it.name.endsWith(".apk") }
    
    val preferredApk: GitHubAsset?
        get() = androidAssets.firstOrNull { 
            it.name.contains("universal", ignoreCase = true) || 
            it.name.contains("release", ignoreCase = true) 
        } ?: androidAssets.firstOrNull()
}

@Serializable
data class GitHubAsset(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("download_count") val downloadCount: Int,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("created_at") val createdAt: String
)

enum class AppInstallStatus {
    NOT_INSTALLED,
    INSTALLED_OUTDATED,
    INSTALLED_CURRENT,
    UNKNOWN
}

@Serializable
data class AppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean
)
