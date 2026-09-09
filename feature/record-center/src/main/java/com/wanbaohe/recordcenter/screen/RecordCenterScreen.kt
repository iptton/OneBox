package com.wanbaohe.recordcenter.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shifenmiao.common.ui.BaseScreen
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import com.t8rin.imagetoolbox.core.resources.icons.line.LineFeatures
import com.t8rin.imagetoolbox.core.resources.icons.line.LineChevronRight
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassCard
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.component.RecordCenterComponent
import com.wanbaohe.recordcenter.component.RecordCenterLayout
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition
import com.wanbaohe.recordcenter.screen.util.formatRecordValues
import com.wanbaohe.recordcenter.screen.util.formatRelativeDate

/**
 * 记录中心聚合页 — 每种记录类型一张卡片,展示最新一条记录摘要。
 */
@Composable
fun RecordCenterScreen(component: RecordCenterComponent) {
    val latestByType by component.latestByType.collectAsState()
    val layoutMode by component.layoutMode.collectAsState()

    BaseScreen(
        title = {
            Text(
                text = stringResource(R.string.record_center_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        onGoBack = component.onGoBack,
        actions = {
            // 列表 / 网格布局切换(图标展示将要切换到的目标布局)
            IconButton(onClick = component::toggleLayoutMode) {
                Icon(
                    imageVector = when (layoutMode) {
                        RecordCenterLayout.GRID -> Icons.AutoMirrored.Filled.ViewList
                        RecordCenterLayout.LIST -> com.t8rin.imagetoolbox.core.resources.Icons.Outlined.LineFeatures
                    },
                    contentDescription = stringResource(
                        when (layoutMode) {
                            RecordCenterLayout.GRID -> R.string.record_center_cd_switch_to_list
                            RecordCenterLayout.LIST -> R.string.record_center_cd_switch_to_grid
                        }
                    ),
                )
            }
        },
        supportGlassEffect = true,
        content = {
            when (layoutMode) {
                RecordCenterLayout.LIST -> RecordTypeList(component = component, latestByType = latestByType)
                RecordCenterLayout.GRID -> RecordTypeGrid(component = component, latestByType = latestByType)
            }
        },
    )
}

/** 列表布局:整行卡片 */
@Composable
private fun RecordTypeList(
    component: RecordCenterComponent,
    latestByType: Map<String, HealthRecordEntity>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = component.recordTypes,
            key = { it.key },
        ) { definition ->
            RecordTypeCard(
                definition = definition,
                latestValues = latestByType[definition.key]?.let { entity ->
                    formatRecordValues(definition, entity.fieldsJson)
                },
                latestDate = latestByType[definition.key]?.let { entity ->
                    formatRelativeDate(entity.happenedAt)
                },
                onClick = { component.navigateToRecordList(definition.key) },
            )
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

/** 网格布局:双列紧凑卡片 */
@Composable
private fun RecordTypeGrid(
    component: RecordCenterComponent,
    latestByType: Map<String, HealthRecordEntity>,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = component.recordTypes,
            key = { it.key },
        ) { definition ->
            RecordTypeGridCard(
                definition = definition,
                latestValues = latestByType[definition.key]?.let { entity ->
                    formatRecordValues(definition, entity.fieldsJson)
                },
                latestDate = latestByType[definition.key]?.let { entity ->
                    formatRelativeDate(entity.happenedAt)
                },
                onClick = { component.navigateToRecordList(definition.key) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RecordTypeCard(
    definition: RecordTypeDefinition,
    latestValues: String?,
    latestDate: String?,
    onClick: () -> Unit,
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderWidth = 0.dp,
        containerAlpha = 0.4f,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 软色圆角图标容器
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = definition.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(definition.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(definition.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (latestValues != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = latestValues,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (latestDate != null) {
                            Text(
                                text = latestDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = com.t8rin.imagetoolbox.core.resources.Icons.Outlined.LineChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 网格模式紧凑卡片:图标在上,标题 + 描述(最多两行) + 最新值 */
@Composable
private fun RecordTypeGridCard(
    definition: RecordTypeDefinition,
    latestValues: String?,
    latestDate: String?,
    onClick: () -> Unit,
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderWidth = 0.dp,
        containerAlpha = 0.4f,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 软色圆角图标容器
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = definition.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(definition.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(definition.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (latestValues != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = latestValues,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (latestDate != null) {
                        Text(
                            text = latestDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
