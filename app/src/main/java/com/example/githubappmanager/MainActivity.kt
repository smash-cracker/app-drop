package com.example.githubappmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.githubappmanager.ui.theme.GithubAppManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent { GithubAppManagerApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GithubAppManagerApp() {
    GithubAppManagerTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        var showAddDialog by rememberSaveable { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("GitHub App Manager") }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add repository")
                }
            },
            // Apply system bar insets when using edge-to-edge
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            RepoListScreen(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        }

        if (showAddDialog) {
            AddRepoDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { url ->
                    // TODO: hand off to ViewModel (e.g., add repo, refresh list)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun RepoListScreen(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("No repositories yet")
    }
}

@Composable
private fun AddRepoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val error = remember(url) { validateGithubUrl(url) }
    val isValid = error == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add GitHub repository") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text("Paste repo URL (e.g., https://github.com/user/app)") },
                    supportingText = { if (!isValid && url.isNotBlank()) Text(error!!) },
                    isError = !isValid && url.isNotBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(url.trim()) }, enabled = isValid && url.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Returns null if valid, else an error string. */
private fun validateGithubUrl(input: String): String? {
    val s = input.trim()
    if (s.isEmpty()) return "URL cannot be empty"

    val httpsRegex = Regex("""^(https?://)?(www\.)?github\.com/[^/\s]+/[^/\s]+/?(\.git)?$""", RegexOption.IGNORE_CASE)
    val sshRegex = Regex("""^git@github\.com:[^/\s]+/[^/\s]+(\.git)?$""", RegexOption.IGNORE_CASE)

    return if (httpsRegex.matches(s) || sshRegex.matches(s)) null
    else "Enter a valid GitHub repo URL (e.g., github.com/user/repo)"
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    GithubAppManagerApp()
}
