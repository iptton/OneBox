package com.wanbaohe.recordcenter.component

import com.arkivanov.decompose.ComponentContext
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import com.shifenmiao.database.recordcenter.repo.HealthRecordRepository
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.wanbaohe.recordcenter.registry.RecordTypeCatalog
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
) : BaseComponent(dispatchersHolder, componentContext) {

    /** 全部记录类型定义,按 sortOrder 升序 */
    val recordTypes: List<RecordTypeDefinition> = catalog.all()

    /** 各类型最新一条记录,type → entity */
    val latestByType: StateFlow<Map<String, HealthRecordEntity>> = repository
        .observeLatestPerType()
        .map { list -> list.associateBy { it.type } }
        .stateIn(componentScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

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
