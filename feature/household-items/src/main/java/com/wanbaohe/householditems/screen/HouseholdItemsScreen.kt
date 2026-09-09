package com.wanbaohe.householditems.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shifenmiao.common.ui.BaseScreen
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.icons.Add
import com.t8rin.imagetoolbox.core.resources.icons.Delete
import com.t8rin.imagetoolbox.core.resources.icons.line.LineCalendar
import com.t8rin.imagetoolbox.core.resources.icons.line.LineChevronRight
import com.t8rin.imagetoolbox.core.resources.icons.line.LineFolder
import com.t8rin.imagetoolbox.core.resources.icons.line.LineSearch
import com.t8rin.imagetoolbox.core.resources.icons.line.LineStorage
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedFloatingActionButton
import com.wanbaohe.householditems.R
import com.wanbaohe.householditems.component.HouseholdItemsComponent
import com.wanbaohe.householditems.model.ExpiryStatus
import com.wanbaohe.householditems.model.HouseholdItemUi
import com.wanbaohe.householditems.model.HouseholdItemsUiState
import com.wanbaohe.householditems.model.HouseholdLocationUi
import com.wanbaohe.householditems.model.locationPathOf
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// 家庭物品主页:搜索 + 保质期提醒 + 按位置逐级浏览 + 物品列表
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HouseholdItemsScreen(
    component: HouseholdItemsComponent,
    onGoBack: () -> Unit,
) {
    val uiState by component.uiState.collectAsState()

    BaseScreen(
        title = stringResource(R.string.household_title),
        onGoBack = onGoBack,
        actions = {
            IconButton(onClick = component::openLocationManager) {
                Icon(
                    imageVector = Icons.Outlined.LineStorage,
                    contentDescription = stringResource(R.string.household_manage_locations),
                )
            }
        },
        supportGlassEffect = true,
        foreground = {
            Box(modifier = Modifier.fillMaxSize()) {
                EnhancedFloatingActionButton(
                    onClick = component::startAddItem,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.household_add_item),
                    )
                }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = component::onSearchQueryChange,
                )
                if (uiState.searchQuery.isNotBlank()) {
                    SearchResults(
                        items = uiState.items,
                        onItemClick = component::startEditItem,
                        onItemDelete = component::deleteItem,
                    )
                } else {
                    BrowseContent(
                        uiState = uiState,
                        onLocationClick = component::enterLocation,
                        onBreadcrumbClick = component::navigateToBreadcrumb,
                        onItemClick = component::startEditItem,
                        onItemDelete = component::deleteItem,
                    )
                }
            }
        },
        showNavigationBarsPadding = false,
    )

    ItemEditSheet(
        component = component,
        visible = uiState.showItemEditor,
        onDismiss = component::hideItemEditor,
    )

    LocationManageDialog(
        component = component,
        visible = uiState.showLocationManager,
        onDismiss = component::closeLocationManager,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 搜索框
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.household_search_hint)) },
        leadingIcon = {
            Icon(Icons.Outlined.LineSearch, contentDescription = null)
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 搜索结果
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchResults(
    items: List<HouseholdItemUi>,
    onItemClick: (HouseholdItemUi) -> Unit,
    onItemDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (items.isEmpty()) {
            item { EmptyHint(stringResource(R.string.household_empty_search)) }
        }
        items(items, key = { it.id }) { item ->
            ItemRow(item = item, onClick = { onItemClick(item) }, onDelete = { onItemDelete(item.id) })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 按位置浏览
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrowseContent(
    uiState: HouseholdItemsUiState,
    onLocationClick: (String) -> Unit,
    onBreadcrumbClick: (String?) -> Unit,
    onItemClick: (HouseholdItemUi) -> Unit,
    onItemDelete: (String) -> Unit,
) {
    val atRoot = uiState.currentLocationId == null
    val children = uiState.locations.filter { it.parentId == uiState.currentLocationId }
    val currentItems = uiState.items.filter { it.locationId == uiState.currentLocationId }
    val breadcrumb = locationPathOf(uiState.locations, uiState.currentLocationId)
    val expiryItems = uiState.expiredItems + uiState.expiringSoonItems

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── 保质期提醒(仅根层级展示,避免逐层重复) ──
        if (atRoot && expiryItems.isNotEmpty()) {
            item(key = "expiry_header") {
                Text(
                    text = stringResource(R.string.household_expiry_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(expiryItems, key = { "expiry_${it.id}" }) { item ->
                ItemRow(item = item, onClick = { onItemClick(item) }, onDelete = { onItemDelete(item.id) })
            }
        }

        // ── 面包屑 ──
        item(key = "breadcrumb") {
            BreadcrumbRow(
                breadcrumb = breadcrumb,
                onBreadcrumbClick = onBreadcrumbClick,
            )
        }

        // ── 子位置 ──
        items(children, key = { "loc_${it.id}" }) { location ->
            LocationRow(location = location, onClick = { onLocationClick(location.id) })
        }

        // ── 当前层级物品 ──
        items(currentItems, key = { it.id }) { item ->
            ItemRow(item = item, onClick = { onItemClick(item) }, onDelete = { onItemDelete(item.id) })
        }

        if (children.isEmpty() && currentItems.isEmpty() && (expiryItems.isEmpty() || !atRoot)) {
            item(key = "empty") {
                EmptyHint(stringResource(R.string.household_empty_items))
            }
        }
    }
}

@Composable
private fun BreadcrumbRow(
    breadcrumb: List<HouseholdLocationUi>,
    onBreadcrumbClick: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            BreadcrumbSegment(
                text = stringResource(R.string.household_breadcrumb_home),
                isLast = breadcrumb.isEmpty(),
                onClick = { onBreadcrumbClick(null) },
            )
        }
        items(breadcrumb, key = { it.id }) { location ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LineChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BreadcrumbSegment(
                    text = location.name,
                    isLast = location.id == breadcrumb.lastOrNull()?.id,
                    onClick = { onBreadcrumbClick(location.id) },
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbSegment(
    text: String,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (isLast) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 位置行 / 物品行
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocationRow(
    location: HouseholdLocationUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.LineFolder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Outlined.LineChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ItemRow(
    item: HouseholdItemUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemThumbnail(item = item)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.locationPath.ifBlank { stringResource(R.string.household_no_location) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.note.isNotBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ExpiryBadge(item = item)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.household_delete_item),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ItemThumbnail(item: HouseholdItemUi) {
    if (!item.photoPath.isNullOrBlank()) {
        AsyncImage(
            model = File(item.photoPath),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        // 无图:分类(或名称)首字占位
        val label = (item.category.ifBlank { item.name }).take(1)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxSize(),
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ExpiryBadge(item: HouseholdItemUi) {
    val days = item.daysToExpire ?: return
    val (textRes, textArg, color) = when (item.expiryStatus) {
        ExpiryStatus.EXPIRED -> Triple(
            R.string.household_expired_days,
            -days,
            MaterialTheme.colorScheme.error,
        )

        ExpiryStatus.EXPIRING_SOON -> {
            if (days == 0L) {
                Triple(R.string.household_expires_today, 0L, MaterialTheme.colorScheme.tertiary)
            } else {
                Triple(R.string.household_days_left, days, MaterialTheme.colorScheme.tertiary)
            }
        }

        else -> return
    }
    val text = if (textRes == R.string.household_expires_today) {
        stringResource(textRes)
    } else {
        stringResource(textRes, textArg)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.LineCalendar,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
