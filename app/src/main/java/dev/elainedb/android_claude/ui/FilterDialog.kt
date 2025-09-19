package dev.elainedb.android_claude.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.elainedb.android_claude.repository.FilterOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    isOpen: Boolean,
    currentFilter: FilterOptions,
    availableChannels: List<String>,
    availableCountries: List<String>,
    onDismiss: () -> Unit,
    onApplyFilter: (FilterOptions) -> Unit
) {
    if (!isOpen) return

    var selectedChannelName by remember(currentFilter) { mutableStateOf(currentFilter.channelName) }
    var selectedCountry by remember(currentFilter) { mutableStateOf(currentFilter.country) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter Videos",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Channel Filter Section
                if (availableChannels.isNotEmpty()) {
                    Text(
                        text = "Source Channel",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // None option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedChannelName == null,
                                onClick = { selectedChannelName = null }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedChannelName == null,
                            onClick = { selectedChannelName = null }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All Channels")
                    }

                    // Channel options
                    availableChannels.forEach { channelName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedChannelName == channelName,
                                    onClick = { selectedChannelName = channelName }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedChannelName == channelName,
                                onClick = { selectedChannelName = channelName }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(channelName)
                        }
                    }

                    HorizontalDivider()
                }

                // Country Filter Section
                if (availableCountries.isNotEmpty()) {
                    Text(
                        text = "Country",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // None option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedCountry == null,
                                onClick = { selectedCountry = null }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCountry == null,
                            onClick = { selectedCountry = null }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All Countries")
                    }

                    // Country options
                    availableCountries.forEach { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedCountry == country,
                                    onClick = { selectedCountry = country }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCountry == country,
                                onClick = { selectedCountry = country }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(country)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApplyFilter(
                        FilterOptions(
                            channelName = selectedChannelName,
                            country = selectedCountry
                        )
                    )
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