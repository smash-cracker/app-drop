package com.example.githubappmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    
    val repos: Flow<List<GitHubRepo>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[reposKey] ?: "[]"
            try {
                val serializable = Json.decodeFromString<List<SerializableGitHubRepo>>(jsonString)
                serializable.map { 
                    GitHubRepo(it.url, it.name, it.owner, it.addedAt, it.latestRelease, it.packageName, it.installStatus) 
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
            
            val updatedRepos = currentRepos + SerializableGitHubRepo(
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
                } else {
                    existingRepo
                }
            }
            
            preferences[reposKey] = Json.encodeToString(updatedRepos)
        }
    }
    
    suspend fun clearAllRepos() {
        context.dataStore.edit { preferences ->
            preferences[reposKey] = "[]"
        }
    }
}