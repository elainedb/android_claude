package dev.elainedb.android_claude.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.elainedb.android_claude.repository.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDialog(
    isOpen: Boolean,
    currentSortOption: SortOption,
    onDismiss: () -> Unit,
    onApplySort: (SortOption) -> Unit
) {
    if (!isOpen) return

    var selectedSortOption by remember(currentSortOption) { mutableStateOf(currentSortOption) }

    val sortOptions = listOf(
        SortOption.PUBLICATION_DATE_NEWEST to "Publication Date (Newest First)",
        SortOption.PUBLICATION_DATE_OLDEST to "Publication Date (Oldest First)",
        SortOption.RECORDING_DATE_NEWEST to "Recording Date (Newest First)",
        SortOption.RECORDING_DATE_OLDEST to "Recording Date (Oldest First)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sort Videos",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortOptions.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSortOption == option,
                                onClick = { selectedSortOption = option }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSortOption == option,
                            onClick = { selectedSortOption = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApplySort(selectedSortOption)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}