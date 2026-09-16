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
import androidx.compose.runtime.LaunchedEffect
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
import com.shifenmiao.common.ui.BaseScreen
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import com.t8rin.imagetoolbox.core.resources.icons.line.LineFeatures
import com.t8rin.imagetoolbox.core.resources.icons.line.LineAvatarDefault
import com.t8rin.imagetoolbox.core.resources.icons.line.LineChevronRight
import com.t8rin.imagetoolbox.core.settings.presentation.provider.LocalSettingsState
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassCard
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.component.RecordCenterComponent
import com.wanbaohe.recordcenter.component.RecordCenterLayout
import com.wanbaohe.recordcenter.data.HealthProfile
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
    val profile by component.profile.collectAsState()
    var showProfileSheet by remember { mutableStateOf(false) }

    // 布局跟随 App 全局设置(单/双列),设置变化时重新对齐;页面内切换不回写设置
    val appGridMode = LocalSettingsState.current.groupOptionsByTypes
    LaunchedEffect(appGridMode) {
        component.syncLayoutWithAppSetting(appGridMode)
    }

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
                RecordCenterLayout.LIST -> RecordTypeList(
                    component = component,
                    latestByType = latestByType,
                    profile = profile,
                    onProfileClick = { showProfileSheet = true },
                )
                RecordCenterLayout.GRID -> RecordTypeGrid(
                    component = component,
                    latestByType = latestByType,
                    profile = profile,
                    onProfileClick = { showProfileSheet = true },
                )
            }
        },
    )

    HealthProfileSheet(
        visible = showProfileSheet,
        initial = profile,
        onSave = {
            component.saveProfile(it)
            showProfileSheet = false
        },
        onDismiss = { showProfileSheet = false },
    )
}

/** 列表布局:整行卡片 */
@Composable
private fun RecordTypeList(
    component: RecordCenterComponent,
    latestByType: Map<String, HealthRecordEntity>,
    profile: HealthProfile,
    onProfileClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HealthProfileCard(profile = profile, onClick = onProfileClick)
        }
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
    profile: HealthProfile,
    onProfileClick: () -> Unit,
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
        item(span = { GridItemSpan(maxLineSpan) }) {
            HealthProfileCard(profile = profile, onClick = onProfileClick)
        }
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

/** 置顶基础信息卡片:摘要展示,点击进入编辑弹层 */
@Composable
private fun HealthProfileCard(
    profile: HealthProfile,
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = com.t8rin.imagetoolbox.core.resources.Icons.Outlined.LineAvatarDefault,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.record_center_profile_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = profileSummary(profile),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profile.isSet) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = com.t8rin.imagetoolbox.core.resources.Icons.Outlined.LineChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 基础信息摘要:已设置 → "男 · 28 岁 · 175 cm · 70 kg";未设置 → 引导文案 */
@Composable
private fun profileSummary(profile: HealthProfile): String {
    if (!profile.isSet) return stringResource(R.string.record_center_profile_unset)
    val parts = mutableListOf<String>()
    when (profile.gender) {
        HealthProfile.Gender.MALE -> parts += stringResource(R.string.record_center_profile_gender_male)
        HealthProfile.Gender.FEMALE -> parts += stringResource(R.string.record_center_profile_gender_female)
        HealthProfile.Gender.UNSET -> {}
    }
    profile.age?.let { parts += stringResource(R.string.record_center_profile_age_years, it) }
    profile.heightCm?.let { parts += "${formatProfileNumber(it)} cm" }
    profile.weightKg?.let { parts += "${formatProfileNumber(it)} kg" }
    return parts.joinToString(" · ")
}

/** 摘要数字格式化:整数去小数点 */
private fun formatProfileNumber(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = definition.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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

/** 网格模式紧凑卡片:图标在上,标题 + 描述(最多两行) + 最新值(底对齐)。
 *  固定高度保证同行卡片上下对齐,无数据卡片中间留白。 */
@Composable
private fun RecordTypeGridCard(
    definition: RecordTypeDefinition,
    latestValues: String?,
    latestDate: String?,
    onClick: () -> Unit,
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(GRID_CARD_HEIGHT),
        shape = RoundedCornerShape(16.dp),
        borderWidth = 0.dp,
        containerAlpha = 0.4f,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 软色圆角图标容器
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = definition.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
            Spacer(modifier = Modifier.weight(1f))
            if (latestValues != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = latestValues,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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

/** 网格卡片固定高度:图标+标题+两行描述+最新值区块的内容高度 */
private val GRID_CARD_HEIGHT = 192.dp
