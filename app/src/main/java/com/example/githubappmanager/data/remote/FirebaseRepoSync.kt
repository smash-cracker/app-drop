package com.example.githubappmanager.data.remote

import com.example.githubappmanager.data.local.SerializableGitHubRepo
import com.example.githubappmanager.domain.model.GitHubRepo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Helper for simple Firestore-based repo sync.
 * Stores all repos as a JSON string in document `users/{uid}` field `repos_json`.
 */
class FirebaseRepoSync {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    suspend fun fetchRemoteRepos(uid: String): List<SerializableGitHubRepo> = withContext(Dispatchers.IO) {
        suspendCoroutine { cont ->
            val docRef = firestore.collection("users").document(uid)
            docRef.get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val json = snapshot?.getString("repos_json") ?: "[]"
                        val list = Json.decodeFromString<List<SerializableGitHubRepo>>(json)
                        cont.resume(list)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    suspend fun pushRepos(uid: String, repos: List<SerializableGitHubRepo>) = withContext(Dispatchers.IO) {
        val json = Json.encodeToString(repos)
        suspendCoroutine<Unit> { cont ->
            val docRef = firestore.collection("users").document(uid)
            val data = mapOf("repos_json" to json)
            docRef.set(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    fun startListening(uid: String, onChange: (List<SerializableGitHubRepo>) -> Unit) {
        stopListening()
        val docRef = firestore.collection("users").document(uid)
        listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                try {
                    val json = snapshot.getString("repos_json") ?: "[]"
                    val list = Json.decodeFromString<List<SerializableGitHubRepo>>(json)
                    onChange(list)
                } catch (_: Exception) {
                }
            } else {
                onChange(emptyList())
            }
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
