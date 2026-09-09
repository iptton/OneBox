package com.wanbaohe.householditems.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shifenmiao.theme.AppTheme
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.icons.Delete
import com.t8rin.imagetoolbox.core.resources.icons.Edit
import com.t8rin.imagetoolbox.core.resources.icons.line.LineAddCircleOutline
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedAlertDialog
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedModalBottomSheet
import com.wanbaohe.householditems.R
import com.wanbaohe.householditems.component.HouseholdItemsComponent
import com.wanbaohe.householditems.model.LocationTreeNode
import com.wanbaohe.householditems.model.flattenLocationTree

// ─────────────────────────────────────────────────────────────────────────────
// 位置管理:树形展示,支持增加(选父级)/重命名/删除(有子级或物品时阻止)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LocationManageDialog(
    component: HouseholdItemsComponent,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val uiState by component.uiState.collectAsState()
    val flatNodes = remember(uiState.locationTree) { flattenLocationTree(uiState.locationTree) }

    /** 待输入名称的目标:null = 不在输入;first = 父位置 id(null = 顶级),second = 重命名目标 id */
    var nameInputParent by remember { mutableStateOf<String?>(null) }
    var nameInputRenameId by remember { mutableStateOf<String?>(null) }
    var showNameInput by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    EnhancedModalBottomSheet(
        nestedScrollEnabled = true,
        visible = visible,
        onDismiss = { onDismiss() },
        endConfirmButtonPadding = 16.dp,
        title = { Text(stringResource(R.string.household_locations_title)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.household_confirm))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            if (uiState.locationDeleteBlocked) {
                Text(
                    text = stringResource(R.string.household_location_delete_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
                if (flatNodes.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.household_locations_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                items(flatNodes, key = { it.location.id }) { node ->
                    LocationManageRow(
                        node = node,
                        onAddChild = {
                            nameInputParent = node.location.id
                            nameInputRenameId = null
                            nameInput = ""
                            showNameInput = true
                        },
                        onRename = {
                            nameInputParent = null
                            nameInputRenameId = node.location.id
                            nameInput = node.location.name
                            showNameInput = true
                        },
                        onDelete = { component.deleteLocation(node.location.id) },
                    )
                }
            }

            TextButton(
                onClick = {
                    nameInputParent = null
                    nameInputRenameId = null
                    nameInput = ""
                    showNameInput = true
                },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.household_location_add_root))
            }
        }
    }

    if (showNameInput) {
        val isRename = nameInputRenameId != null
        EnhancedAlertDialog(
            visible = true,
            onDismissRequest = { showNameInput = false },
            title = {
                Text(
                    stringResource(
                        if (isRename) R.string.household_location_rename
                        else R.string.household_location_add_root
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.household_location_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = AppTheme.colors.getOutlinedTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = nameInput.trim()
                        if (name.isNotEmpty()) {
                            val renameId = nameInputRenameId
                            if (renameId != null) {
                                component.renameLocation(renameId, name)
                            } else {
                                component.addLocation(name, nameInputParent)
                            }
                            showNameInput = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.household_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameInput = false }) {
                    Text(stringResource(R.string.household_cancel))
                }
            },
        )
    }
}

@Composable
private fun LocationManageRow(
    node: LocationTreeNode,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * 20).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = node.location.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onAddChild) {
            Icon(
                imageVector = Icons.Outlined.LineAddCircleOutline,
                contentDescription = stringResource(R.string.household_location_add_child),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.household_location_rename),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.household_location_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
