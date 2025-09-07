package com.example.githubappmanager.data

data class GitHubRepo(
    val url: String,
    val name: String,
    val owner: String,
    val addedAt: Long = System.currentTimeMillis(),
    val latestRelease: GitHubRelease? = null,
    val packageName: String? = null,
    val installStatus: AppInstallStatus = AppInstallStatus.UNKNOWN
) {
    companion object {
        fun fromUrl(url: String): GitHubRepo {
            val cleanUrl = url.trim().removePrefix("https://").removePrefix("http://")
                .removePrefix("www.").removeSuffix(".git").removeSuffix("/")
            
            val parts = cleanUrl.split("/")
            val owner = if (parts.size >= 2) parts[1] else "unknown"
            val name = if (parts.size >= 3) parts[2] else "unknown"
            
            return GitHubRepo(
                url = url.trim(),
                name = name,
                owner = owner
            )
        }
    }
    
    val displayName: String get() = "$owner/$name"
}