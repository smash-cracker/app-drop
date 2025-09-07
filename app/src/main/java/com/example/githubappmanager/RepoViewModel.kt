package com.example.githubappmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubappmanager.data.GitHubRepo
import com.example.githubappmanager.data.RepoDataStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RepoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repoDataStore = RepoDataStore(application)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    val repos = repoDataStore.repos
    
    fun addRepo(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val repo = GitHubRepo.fromUrl(url)
                repoDataStore.addRepo(repo)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun removeRepo(url: String) {
        viewModelScope.launch {
            repoDataStore.removeRepo(url)
        }
    }
    
    fun clearAllRepos() {
        viewModelScope.launch {
            repoDataStore.clearAllRepos()
        }
    }
}