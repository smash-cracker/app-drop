package com.example.githubappmanager.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubappmanager.RepoViewModel

/**
 * A unified SearchBar for searching repos or adding a new GitHub repo directly.
 *
 * - If the input is a valid GitHub repo URL, pressing Enter will add it.
 * - Otherwise, pressing Enter will trigger a search via onQueryChange.
 *
 * @param query Current search query
 * @param onQueryChange Lambda to update search filter in parent
 * @param modifier Optional Modifier
 * @param onError Optional lambda to show validation errors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onError: ((String) -> Unit)? = null
) {
    var inputText by remember { mutableStateOf(query) }
    val viewModel: RepoViewModel = viewModel()
    val error = remember(inputText) { validateGithubUrl(inputText) }
    val isValid = error == null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // --- Search input box ---
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Paste GitHub repo URL or search") },
            singleLine = true,
            isError = !isValid && inputText.isNotBlank(),
            supportingText = { if (!isValid && inputText.isNotBlank()) Text(error!!) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (inputText.isBlank()) return@KeyboardActions
                    if (isValid) {
                        // Add repo directly if valid URL
                        viewModel.addRepo(inputText.trim())
                        inputText = ""
                    } else {
                        // Otherwise treat it as search query
                        onQueryChange(inputText)
                        onError?.invoke(error ?: "Invalid URL")
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Recently Viewed + Clear All Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Viewed",
                style = MaterialTheme.typography.bodyMedium
            )

            TextButton(
                onClick = { viewModel.clearRecentlyViewed() },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Clear All")
            }
        }
    }
}

/** Returns null if valid, else an error string. */
private fun validateGithubUrl(input: String): String? {
    val s = input.trim()
    if (s.isEmpty()) return null // empty input can still be used as search

    val httpsRegex = Regex(
        """^(https?://)?(www\.)?github\.com/[^/\s]+/[^/\s]+/?(\.git)?$""",
        RegexOption.IGNORE_CASE
    )
    val sshRegex = Regex(
        """^git@github\.com:[^/\s]+/[^/\s]+(\.git)?$""",
        RegexOption.IGNORE_CASE
    )

    return if (httpsRegex.matches(s) || sshRegex.matches(s)) null
    else "Enter a valid GitHub repo URL (e.g., github.com/user/repo)"
}
