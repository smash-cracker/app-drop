package com.example.githubappmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "github_repos")

@Serializable
data class SerializableGitHubRepo(
    val url: String,
    val name: String,
    val owner: String,
    val addedAt: Long,
    val latestRelease: GitHubRelease? = null,
    val packageName: String? = null,
    val installStatus: AppInstallStatus = AppInstallStatus.UNKNOWN
)

class RepoDataStore(private val context: Context) {

    private val reposKey = stringPreferencesKey("github_repos_json")

    // ------------------------------
    // Persistent Repo List (DataStore)
    // ------------------------------
    val repos: Flow<List<GitHubRepo>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[reposKey] ?: "[]"
            try {
                val serializable = Json.decodeFromString<List<SerializableGitHubRepo>>(jsonString)
                serializable.map {
                    GitHubRepo(
                        it.url,
                        it.name,
                        it.owner,
                        it.addedAt,
                        it.latestRelease,
                        it.packageName,
                        it.installStatus
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun addRepo(repo: GitHubRepo) {
        context.dataStore.edit { preferences ->
            val currentJsonString = preferences[reposKey] ?: "[]"
            val currentRepos = try {
                Json.decodeFromString<List<SerializableGitHubRepo>>(currentJsonString)
            } catch (e: Exception) {
                emptyList()
            }

            // Prevent duplicates
            val updatedRepos = currentRepos.filter { it.url != repo.url } + SerializableGitHubRepo(
                url = repo.url,
                name = repo.name,
                owner = repo.owner,
                addedAt = repo.addedAt,
                latestRelease = repo.latestRelease,
                packageName = repo.packageName,
                installStatus = repo.installStatus
            )

            preferences[reposKey] = Json.encodeToString(updatedRepos)
        }
    }

    suspend fun removeRepo(url: String) {
        context.dataStore.edit { preferences ->
            val currentJsonString = preferences[reposKey] ?: "[]"
            val currentRepos = try {
                Json.decodeFromString<List<SerializableGitHubRepo>>(currentJsonString)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedRepos = currentRepos.filter { it.url != url }
            preferences[reposKey] = Json.encodeToString(updatedRepos)
        }
    }

    suspend fun updateRepo(repo: GitHubRepo) {
        context.dataStore.edit { preferences ->
            val currentJsonString = preferences[reposKey] ?: "[]"
            val currentRepos = try {
                Json.decodeFromString<List<SerializableGitHubRepo>>(currentJsonString)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedRepos = currentRepos.map { existingRepo ->
                if (existingRepo.url == repo.url) {
                    SerializableGitHubRepo(
                        url = repo.url,
                        name = repo.name,
                        owner = repo.owner,
                        addedAt = repo.addedAt,
                        latestRelease = repo.latestRelease,
                        packageName = repo.packageName,
                        installStatus = repo.installStatus
                    )
                } else existingRepo
            }

            preferences[reposKey] = Json.encodeToString(updatedRepos)
        }
    }

    suspend fun clearAllRepos() {
        context.dataStore.edit { preferences ->
            preferences[reposKey] = "[]"
        }
    }

    // ------------------------------
    // Recently Viewed (In-memory only)
    // ------------------------------
    private val _recentlyViewedRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val recentlyViewedRepos: Flow<List<GitHubRepo>> = _recentlyViewedRepos.asStateFlow()

    suspend fun addRecentlyViewed(repo: GitHubRepo) {
        val list = _recentlyViewedRepos.value.toMutableList()
        list.removeAll { it.url == repo.url } // avoid duplicates
        list.add(0, repo)
        _recentlyViewedRepos.value = list.take(10) // keep last 10
    }

    suspend fun clearRecentlyViewed() {
        _recentlyViewedRepos.value = emptyList()
    }
}