package com.example.githubappmanager.feature.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubappmanager.data.download.ApkDownloader
import com.example.githubappmanager.data.download.DownloadProgress
import com.example.githubappmanager.data.install.AppInstallManager
import com.example.githubappmanager.data.local.RepoDataStore
import com.example.githubappmanager.data.remote.GitHubApiClient
import com.example.githubappmanager.domain.model.AppInstallStatus
import com.example.githubappmanager.domain.model.GitHubRelease
import com.example.githubappmanager.domain.model.GitHubRepo
import com.example.githubappmanager.domain.model.withInfo
import kotlinx.coroutines.Job // ✅ Added import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RepoViewModel(application: Application) : AndroidViewModel(application) {

    private val repoDataStore = RepoDataStore(application)
    private val appInstallManager = AppInstallManager(application)
    private val apkDownloader = ApkDownloader(application)
    private val apiService = GitHubApiClient.apiService

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>() // ✅ Added field

    val repos = repoDataStore.repos

    // ----------------------------
    // Recently Viewed Management
    // ----------------------------
    private val _recentlyViewed = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val recentlyViewed: StateFlow<List<GitHubRepo>> = _recentlyViewed.asStateFlow()

    init {
        // Load any stored recently viewed repos on startup
        viewModelScope.launch {
            repoDataStore.recentlyViewedRepos.collect { list ->
                _recentlyViewed.value = list
            }
        }

        // Refresh install status for all repos on app start
        refreshAllInstallStatus()
    }

    fun addRecentlyViewed(repo: GitHubRepo) {
        viewModelScope.launch {
            repoDataStore.addRecentlyViewed(repo)
        }
    }

    fun clearRecentlyViewed() {
        viewModelScope.launch {
            repoDataStore.clearRecentlyViewed()
        }
    }

    // ----------------------------
    // Install Status Management
    // ----------------------------
    private fun refreshAllInstallStatus() {
        viewModelScope.launch {
            val currentRepos = repos.first()
            currentRepos.forEach { repo ->
                try {
                    val updatedRepo = detectAndUpdateInstallStatus(repo)
                    if (updatedRepo.installStatus != repo.installStatus || updatedRepo.packageName != repo.packageName) {
                        repoDataStore.updateRepo(updatedRepo)
                    }
                } catch (e: Exception) {
                    Log.e("RepoViewModel", "Error refreshing install status for ${repo.name}", e)
                }
            }
        }
    }

    private suspend fun detectAndUpdateInstallStatus(repo: GitHubRepo): GitHubRepo {
        val foundPackageName = appInstallManager.findInstalledPackageForRepo(repo.owner, repo.name)
        val packageName = foundPackageName
            ?: repo.packageName
            ?: appInstallManager.guessPackageName(repo.owner, repo.name)

        val release = try {
            apiService.getLatestRelease(repo.owner, repo.name)
        } catch (e: Exception) {
            repo.latestRelease
        }

        val installStatus = appInstallManager.checkInstallStatus(release, packageName)

        val repoInfo = try {
            apiService.getRepoInfo(repo.owner, repo.name)
        } catch (e: Exception) {
            null
        }

        val apkSizeBytes = release?.extractApkSize()

        return repo.copy(
            latestRelease = release ?: repo.latestRelease,
            packageName = packageName,
            installStatus = installStatus,
            apkSizeBytes = apkSizeBytes ?: repo.apkSizeBytes
        ).let { current ->
            repoInfo?.let { current.withInfo(it) } ?: current
        }
    }

    // ----------------------------
    // Repo Management
    // ----------------------------
    fun addRepo(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val baseRepo = GitHubRepo.fromUrl(url)

                val release = try {
                    apiService.getLatestRelease(baseRepo.owner, baseRepo.name)
                } catch (e: Exception) {
                    null
                }

                val repoInfo = try {
                    apiService.getRepoInfo(baseRepo.owner, baseRepo.name)
                } catch (e: Exception) {
                    null
                }

                val foundPackageName =
                    appInstallManager.findInstalledPackageForRepo(baseRepo.owner, baseRepo.name)
                val packageName =
                    foundPackageName ?: appInstallManager.guessPackageName(baseRepo.owner, baseRepo.name)
                val installStatus = appInstallManager.checkInstallStatus(release, packageName)

                val apkSizeBytes = release?.extractApkSize()

                val repoWithRelease = baseRepo.copy(
                    latestRelease = release,
                    packageName = packageName,
                    installStatus = installStatus,
                    apkSizeBytes = apkSizeBytes
                )

                val enrichedRepo = repoInfo?.let { repoWithRelease.withInfo(it) } ?: repoWithRelease

                repoDataStore.addRepo(enrichedRepo)
                addRecentlyViewed(enrichedRepo)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshRepo(repo: GitHubRepo) {
        viewModelScope.launch {
            try {
                val foundPackageName =
                    appInstallManager.findInstalledPackageForRepo(repo.owner, repo.name)
                val packageName =
                    foundPackageName ?: repo.packageName ?: appInstallManager.guessPackageName(
                        repo.owner,
                        repo.name
                    )
                val release = try {
                    apiService.getLatestRelease(repo.owner, repo.name)
                } catch (e: Exception) {
                    repo.latestRelease
                }
                val installStatus = appInstallManager.checkInstallStatus(release, packageName)

                val repoInfo = try {
                    apiService.getRepoInfo(repo.owner, repo.name)
                } catch (e: Exception) {
                    null
                }

                val apkSizeBytes = release?.extractApkSize()

                val updatedRepo = repo.copy(
                    latestRelease = release ?: repo.latestRelease,
                    packageName = packageName,
                    installStatus = installStatus,
                    apkSizeBytes = apkSizeBytes ?: repo.apkSizeBytes
                ).let { current ->
                    repoInfo?.let { current.withInfo(it) } ?: current
                }

                repoDataStore.updateRepo(updatedRepo)
                addRecentlyViewed(updatedRepo)
            } catch (e: Exception) {
                Log.e("RepoViewModel", "refreshRepo failed", e)
                refreshInstallStatusLocal(repo)
            }
        }
    }

    private fun refreshInstallStatusLocal(repo: GitHubRepo) {
        viewModelScope.launch {
            try {
                val foundPackageName =
                    appInstallManager.findInstalledPackageForRepo(repo.owner, repo.name)
                val packageName = foundPackageName ?: repo.packageName
                val installStatus =
                    appInstallManager.checkInstallStatus(repo.latestRelease, packageName)

                val updatedRepo = repo.copy(
                    packageName = packageName,
                    installStatus = installStatus
                )

                if (updatedRepo.installStatus != repo.installStatus || updatedRepo.packageName != repo.packageName) {
                    repoDataStore.updateRepo(updatedRepo)
                }
            } catch (e: Exception) {
                Log.e("RepoViewModel", "refreshInstallStatusLocal failed", e)
            }
        }
    }

    fun removeRepo(url: String) {
        viewModelScope.launch {
            repoDataStore.removeRepo(url)
        }
    }

    // ✅ Updated version with cancel-safe logic
    fun downloadAndInstallApk(repo: GitHubRepo) {
        // Cancel any existing job for this repo
        downloadJobs[repo.url]?.cancel()
        downloadJobs.remove(repo.url)

        viewModelScope.launch {
            addRecentlyViewed(repo)
            val release = repo.latestRelease ?: return@launch
            val apkAsset = release.preferredApk ?: return@launch
            val fileName = "${repo.name}-${release.tagName}.apk"

            val job = launch {
                apkDownloader.downloadApk(apkAsset.downloadUrl, fileName)
                    .collect { progress ->
                        _downloadProgress.value = _downloadProgress.value + (repo.url to progress)

                        if (progress.isComplete && progress.error == null) {
                            val downloadedFile = apkDownloader.getDownloadedApk(fileName)
                            downloadedFile?.let { file ->
                                appInstallManager.installApk(file)
                                refreshInstallStatusLocal(repo)
                            }
                            _downloadProgress.value = _downloadProgress.value - repo.url
                        } else if (progress.error != null) {
                            Log.e("RepoViewModel", "APK download failed: ${progress.error}")
                        }
                    }
            }
            downloadJobs[repo.url] = job
        }
    }

    // ✅ New cancel function
    fun cancelDownload(repoUrl: String) {
        downloadJobs[repoUrl]?.cancel()
        downloadJobs.remove(repoUrl)
        _downloadProgress.value = _downloadProgress.value - repoUrl
    }

    fun uninstallApp(packageName: String) {
        Log.d("RepoViewModel", "uninstallApp requested for package=$packageName")
        appInstallManager.uninstallApp(packageName)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            refreshAllInstallStatus()
        }
    }

    fun clearDownloadProgress(repoUrl: String) {
        _downloadProgress.value = _downloadProgress.value - repoUrl
    }

    fun clearAllRepos() {
        viewModelScope.launch {
            repoDataStore.clearAllRepos()
        }
    }

    private fun GitHubRelease?.extractApkSize(): Long? {
        val release = this ?: return null
        return release.preferredApk?.size ?: release.androidAssets.firstOrNull()?.size
    }
}
