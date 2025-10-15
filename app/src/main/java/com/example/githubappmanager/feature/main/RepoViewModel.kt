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
import com.example.githubappmanager.domain.model.GitHubRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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

                val packageName = appInstallManager.guessPackageName(baseRepo.owner, baseRepo.name)
                val installStatus = release?.let {
                    appInstallManager.checkInstallStatus(it, packageName)
                } ?: AppInstallStatus.UNKNOWN

                val repoWithRelease = baseRepo.copy(
                    latestRelease = release,
                    packageName = packageName,
                    installStatus = installStatus
                )

                repoDataStore.addRepo(repoWithRelease)
                addRecentlyViewed(repoWithRelease)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshRepo(repo: GitHubRepo) {
        viewModelScope.launch {
            try {
                val release = apiService.getLatestRelease(repo.owner, repo.name)
                val packageName = repo.packageName ?: appInstallManager.guessPackageName(repo.owner, repo.name)
                val installStatus = appInstallManager.checkInstallStatus(release, packageName)

                val updatedRepo = repo.copy(
                    latestRelease = release,
                    packageName = packageName,
                    installStatus = installStatus
                )

                repoDataStore.updateRepo(updatedRepo)
                addRecentlyViewed(updatedRepo)
            } catch (e: Exception) {
                Log.e("RepoViewModel", "refreshRepo failed", e)
            }
        }
    }

    fun removeRepo(url: String) {
        viewModelScope.launch {
            repoDataStore.removeRepo(url)
        }
    }

    fun downloadAndInstallApk(repo: GitHubRepo) {
        viewModelScope.launch {
            addRecentlyViewed(repo)
            val release = repo.latestRelease ?: return@launch
            val apkAsset = release.preferredApk ?: return@launch

            val fileName = "${repo.name}-${release.tagName}.apk"

            apkDownloader.downloadApk(apkAsset.downloadUrl, fileName)
                .collect { progress ->
                    _downloadProgress.value = _downloadProgress.value + (repo.url to progress)

                    if (progress.isComplete && progress.error == null) {
                        val downloadedFile = apkDownloader.getDownloadedApk(fileName)
                        downloadedFile?.let { file ->
                            appInstallManager.installApk(file)
                        }
                        // Clear progress after installation
                        _downloadProgress.value = _downloadProgress.value - repo.url
                    } else if (progress.error != null) {
                        Log.e("RepoViewModel", "APK download failed: ${progress.error}")
                    }
                }
        }
    }

    fun uninstallApp(packageName: String) {
        Log.d("RepoViewModel", "uninstallApp requested for package=$packageName")
        appInstallManager.uninstallApp(packageName)
    }

    fun clearDownloadProgress(repoUrl: String) {
        _downloadProgress.value = _downloadProgress.value - repoUrl
    }

    fun clearAllRepos() {
        viewModelScope.launch {
            repoDataStore.clearAllRepos()
        }
    }
}
