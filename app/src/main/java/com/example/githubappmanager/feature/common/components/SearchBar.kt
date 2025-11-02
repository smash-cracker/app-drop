package com.example.githubappmanager.feature.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmitUrl: (String) -> Unit,
    onClearRecentlyViewed: () -> Unit,
    modifier: Modifier = Modifier,
    onError: ((String) -> Unit)? = null,
    hasRecentlyViewed: Boolean = false // ✅ added parameter
) {
    var inputText by rememberSaveable { mutableStateOf(query) }
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (inputText.isBlank()) return@KeyboardActions
                    if (isValid) {
                        onSubmitUrl(inputText.trim())
                        inputText = ""
                        onQueryChange("")
                    } else {
                        onQueryChange(inputText)
                        onError?.invoke(error ?: "Invalid URL")
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Recently Viewed + Clear All Row ---
        if (hasRecentlyViewed) { // ✅ only show if repos exist
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
                    onClick = onClearRecentlyViewed,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Clear All")
                }
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
