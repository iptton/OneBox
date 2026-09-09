package com.wanbaohe.householditems.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.icons.Delete
import com.t8rin.imagetoolbox.core.resources.icons.Edit
import com.t8rin.imagetoolbox.core.resources.icons.line.LineAddCircleOutline
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedAlertDialog
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassOutlinedTextField
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassTonalButton
import com.wanbaohe.householditems.R
import com.wanbaohe.householditems.component.HouseholdItemsComponent
import com.wanbaohe.householditems.model.LocationTreeNode
import com.wanbaohe.householditems.model.flattenLocationTree
import com.wanbaohe.householditems.model.locationIcon

// ─────────────────────────────────────────────────────────────────────────────
// 位置管理(设置 tab):缩进树,一次性展示全部层级
// 层级感:竖向引导线 + 逐级递减的图标/字号/颜色;顶级行加 primary 淡色底突出
// ─────────────────────────────────────────────────────────────────────────────

private val INDENT_PER_DEPTH = 20.dp
private val ROW_HEIGHT = 52.dp

@Composable
fun LocationManageTab(
    component: HouseholdItemsComponent,
    modifier: Modifier = Modifier,
) {
    val uiState by component.uiState.collectAsState()
    val flatNodes = remember(uiState.locationTree) { flattenLocationTree(uiState.locationTree) }

    /** 待输入名称的目标:nameInputParent = 父位置 id(null = 顶级);nameInputRenameId 非空表示重命名 */
    var nameInputParent by remember { mutableStateOf<String?>(null) }
    var nameInputRenameId by remember { mutableStateOf<String?>(null) }
    var showNameInput by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (uiState.locationDeleteBlocked) {
            item(key = "blocked") {
                Text(
                    text = stringResource(R.string.household_location_delete_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

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
            LocationTreeRow(
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

        item(key = "add_root") {
            GlassTonalButton(
                onClick = {
                    nameInputParent = null
                    nameInputRenameId = null
                    nameInput = ""
                    showNameInput = true
                },
                modifier = Modifier.padding(top = 4.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.household_location_add_root),
                    style = MaterialTheme.typography.labelLarge,
                )
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
                GlassOutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.household_location_name_hint)) },
                    singleLine = true,
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

/**
 * 树形行:引导线区(每级一段 20dp,内含 1dp 竖线)+ 图标 + 名称 + 操作按钮。
 * 容器走轻量路线:顶级行 primary 淡色圆角底,子级行透明底,避免密集树形下卡片感过重。
 */
@Composable
private fun LocationTreeRow(
    node: LocationTreeNode,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val depth = node.depth
    val isTop = depth == 0

    // 逐级递减的图标/字号/颜色
    val iconSize = when (depth) {
        0 -> 22.dp
        1 -> 20.dp
        else -> 18.dp
    }
    val textStyle = if (depth >= 2) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val textColor = if (depth >= 2) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val textWeight = if (isTop) FontWeight.SemiBold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isTop) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                } else {
                    Modifier
                }
            )
            .height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── 引导线:每级一段,竖线居中于该段,同级多行自然连续 ──
        repeat(depth) {
            Box(
                modifier = Modifier
                    .width(INDENT_PER_DEPTH)
                    .fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        ),
                )
            }
        }

        Icon(
            imageVector = locationIcon(node.location.id),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (isTop) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        )
        Text(
            text = node.location.name,
            style = textStyle,
            fontWeight = textWeight,
            color = textColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // 紧凑操作按钮:图标 18dp,触摸区域 36dp
        IconButton(onClick = onAddChild, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.LineAddCircleOutline,
                contentDescription = stringResource(R.string.household_location_add_child),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.household_location_rename),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.household_location_delete),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
