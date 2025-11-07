package com.example.githubappmanager.feature.profile

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
                        googleSignInClient.signOut()
                        user = null
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
                        val signInIntent = googleSignInClient.signInIntent
                        signInLauncher.launch(signInIntent)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Login with Google")
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { /* TODO: GitHub OAuth flow */ },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Login with GitHub")
                }
            }
        }
    }
}
