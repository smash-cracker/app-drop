package com.example.githubappmanager.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.githubappmanager.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val serverClientId = remember(context) { context.getString(R.string.default_web_client_id) }
    val googleIdOption = remember(serverClientId) {
        GetSignInWithGoogleOption.Builder(serverClientId).build()
    }
    val credentialRequest = remember(googleIdOption) {
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    fun signInWithFirebase(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
            isLoading = false
            if (signInTask.isSuccessful) {
                user = auth.currentUser
            } else {
                errorMessage = signInTask.exception?.localizedMessage ?: "Failed to sign in with Firebase."
            }
        }
    }

    // ✅ UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalAlignment = Alignment.Start
        ) {
            if (user != null) {
                // ✅ Logged-in view
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(user?.photoUrl),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = user?.displayName ?: "Unknown User",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = user?.email ?: "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        auth.signOut()
                        user = null
                        errorMessage = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Sign Out")
                }
            } else {
                // 🔒 Not logged in view
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Guest User",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Not signed in",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val credentialResponse = credentialManager.getCredential(
                                    context = context,
                                    request = credentialRequest
                                )
                                val credential = credentialResponse.credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    signInWithFirebase(googleIdTokenCredential.idToken)
                                } else {
                                    isLoading = false
                                    errorMessage = "Unsupported credential type returned."
                                }
                            } catch (e: GetCredentialException) {
                                isLoading = false
                                errorMessage = e.message ?: "Google sign-in was canceled."
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "Google sign-in failed."
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (isLoading) "Loading..." else "Login with Google")
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { /* TODO: GitHub OAuth flow */ },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Login with GitHub")
                }
            }
            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
