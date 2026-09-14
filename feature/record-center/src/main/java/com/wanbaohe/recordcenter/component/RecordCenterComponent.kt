package com.wanbaohe.recordcenter.component

import com.arkivanov.decompose.ComponentContext
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import com.shifenmiao.database.recordcenter.repo.HealthRecordRepository
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.wanbaohe.recordcenter.data.HealthProfile
import com.wanbaohe.recordcenter.data.HealthProfileStore
import com.wanbaohe.recordcenter.registry.RecordTypeCatalog
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 聚合页布局模式:列表 / 双列网格 */
enum class RecordCenterLayout { LIST, GRID }

/**
 * 记录中心聚合页 Component — 展示全部记录类型卡片及各类型最新一条记录。
 *
 * 只读,写操作在列表页/录入页走 RecordCenterService。
 */
class RecordCenterComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    dispatchersHolder: DispatchersHolder,
    repository: HealthRecordRepository,
    catalog: RecordTypeCatalog,
    private val profileStore: HealthProfileStore,
) : BaseComponent(dispatchersHolder, componentContext) {

    /** 全部记录类型定义,按 sortOrder 升序 */
    val recordTypes: List<RecordTypeDefinition> = catalog.all()

    /** 基础信息(性别/年龄/身高/体重),置顶卡片展示与编辑 */
    val profile: StateFlow<HealthProfile> = profileStore.profile

    fun saveProfile(profile: HealthProfile) {
        profileStore.save(profile)
    }

    /** 各类型最新一条记录,type → entity */
    val latestByType: StateFlow<Map<String, HealthRecordEntity>> = repository
        .observeLatestPerType()
        .map { list -> list.associateBy { it.type } }
        .stateIn(componentScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    /**
     * 布局模式,内存级状态(重启不持久化):
     * 初始值与 App 全局布局设置一致并跟随其变化(见 RecordCenterScreen 的同步逻辑),
     * 页面内切换只改本地状态,不回写全局设置。
     */
    private val _layoutMode = MutableStateFlow(RecordCenterLayout.LIST)
    val layoutMode: StateFlow<RecordCenterLayout> = _layoutMode

    fun toggleLayoutMode() {
        _layoutMode.value = when (_layoutMode.value) {
            RecordCenterLayout.LIST -> RecordCenterLayout.GRID
            RecordCenterLayout.GRID -> RecordCenterLayout.LIST
        }
    }

    /** 跟随 App 全局布局设置(true=双列网格) */
    fun syncLayoutWithAppSetting(isGridMode: Boolean) {
        _layoutMode.value = if (isGridMode) RecordCenterLayout.GRID else RecordCenterLayout.LIST
    }

    fun navigateToRecordList(recordType: String) {
        onNavigate(
            Screen.RecordCenter(Screen.RecordCenter.Type.RecordList(recordType = recordType))
        )
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            onGoBack: () -> Unit,
            onNavigate: (Screen) -> Unit,
        ): RecordCenterComponent
    }
}
