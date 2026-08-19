package com.rudy.quizingo.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Restart discards a module's saved score/streak - shared by the Module List card
 *  and the Results screen so both restart entry points confirm with the same copy. */
@Composable
fun RestartConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restart this module?") },
        text = { Text("This clears your saved score and streak for this module and starts a fresh attempt. This can't be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Restart") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
