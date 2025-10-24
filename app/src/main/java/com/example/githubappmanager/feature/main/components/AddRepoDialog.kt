package com.example.githubappmanager.feature.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepoDialog(
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

private fun validateGithubUrl(input: String): String? {
    val s = input.trim()
    if (s.isEmpty()) return "URL cannot be empty"

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
