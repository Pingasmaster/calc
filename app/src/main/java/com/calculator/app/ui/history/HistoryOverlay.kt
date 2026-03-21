package com.calculator.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calculator.app.domain.model.HistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    historyItems: List<HistoryItem>,
    onDismiss: () -> Unit,
    onExpressionClick: (String) -> Unit,
    onResultClick: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showClearDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

                if (historyItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No history yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(historyItems, key = { it.id }) { item ->
                            HistoryListItem(
                                item = item,
                                onExpressionClick = {
                                    onExpressionClick(item.expression)
                                    onDismiss()
                                },
                                onResultClick = {
                                    onResultClick(item.result)
                                    onDismiss()
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        // Bottom spacer for FAB clearance
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }

            // FAB for clearing history
            if (historyItems.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Clear all history",
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                onClearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }
}

@Composable
private fun HistoryListItem(
    item: HistoryItem,
    onExpressionClick: () -> Unit,
    onResultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Expression - tappable
        ListItem(
            headlineContent = {
                Text(
                    text = item.expression,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            supportingContent = {
                Text(
                    text = "= ${item.result}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onResultClick),
                )
            },
            modifier = Modifier.clickable(onClick = onExpressionClick),
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        },
        title = { Text("Clear history?") },
        text = { Text("This will permanently delete all calculation history.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Persistent history panel for tablet expanded layout.
 */
@Composable
fun HistoryPanel(
    historyItems: List<HistoryItem>,
    onExpressionClick: (String) -> Unit,
    onResultClick: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider()

            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No history yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        HistoryListItem(
                            item = item,
                            onExpressionClick = { onExpressionClick(item.expression) },
                            onResultClick = { onResultClick(item.result) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }

        if (historyItems.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showClearDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Clear all history",
                )
            }
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                onClearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }
}
