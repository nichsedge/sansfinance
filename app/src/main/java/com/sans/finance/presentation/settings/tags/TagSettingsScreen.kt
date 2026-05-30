package com.sans.finance.presentation.settings.tags

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.domain.model.Tag
import com.sans.finance.presentation.settings.TagEditDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagSettingsScreen(
    onBack: () -> Unit,
    viewModel: TagSettingsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    var tagToEdit by remember { mutableStateOf<Tag?>(null) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tags.isEmpty()) {
                item {
                    Text(
                        "No tags found. Tags are created when you add them to expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            itemsIndexed(tags, key = { _, item -> item.id }) { index, tag ->
                TagCardItem(
                    tag = tag,
                    onEdit = { tagToEdit = tag },
                    onDelete = { tagToDelete = tag },
                    onMoveUp = if (index > 0) {
                        {
                            val mutable = tags.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index - 1, item)
                            viewModel.onTagsReordered(mutable)
                        }
                    } else null,
                    onMoveDown = if (index < tags.size - 1) {
                        {
                            val mutable = tags.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index + 1, item)
                            viewModel.onTagsReordered(mutable)
                        }
                    } else null,
                    onVisibilityToggle = { isChecked ->
                        viewModel.updateTag(tag.copy(isVisible = isChecked))
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    tagToEdit?.let { tag ->
        TagEditDialog(
            tag = tag,
            onDismiss = { tagToEdit = null },
            onConfirm = { newName ->
                viewModel.updateTag(tag.copy(name = newName))
                tagToEdit = null
            }
        )
    }

    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag") },
            text = { Text("Are you sure you want to delete tag '${tag.name}'? This will remove the tag from all associated expenses.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagCardItem(
    tag: Tag,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onVisibilityToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                com.sans.finance.presentation.components.CategoryIcon(
                    icon = "🏷️",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (tag.isVisible) "Shown in transaction form" else "Hidden (Temporary tag)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = tag.isVisible,
                    onCheckedChange = onVisibilityToggle,
                    modifier = Modifier.scale(0.8f)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
