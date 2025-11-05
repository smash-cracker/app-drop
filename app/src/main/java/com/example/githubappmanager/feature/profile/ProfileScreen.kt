package com.example.githubappmanager.feature.profile

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.githubappmanager.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

    // ✅ Configure Google Sign-In
    val googleSignInClient: GoogleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    user = auth.currentUser
                }
            }
        }
    }

    // ✅ UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (user != null) {
            // ✅ Logged-in view
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = rememberAsyncImagePainter(user?.photoUrl),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = user?.displayName ?: "Unknown User",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = user?.email ?: "",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    auth.signOut()
                    googleSignInClient.signOut()
                    user = null
                }) {
                    Text("Sign Out")
                }
            }
        } else {
            // 🔒 Not logged in view
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Login Required",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Sign in to access your profile",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    signInLauncher.launch(signInIntent)
                }) {
                    Text("Login with Google")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    // TODO: GitHub OAuth flow
                }) {
                    Text("Login with GitHub")
                }
                // Spacer(Modifier.height(8.dp))
                // OutlinedButton(onClick = {
                //     // TODO: Microsoft OAuth flow
                // }) {
                //     Text("Login with Microsoft")
                // }
            }
        }
    }
}
