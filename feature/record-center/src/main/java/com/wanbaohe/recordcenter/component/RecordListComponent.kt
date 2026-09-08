package com.wanbaohe.recordcenter.component

import com.arkivanov.decompose.ComponentContext
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import com.shifenmiao.database.recordcenter.repo.HealthRecordRepository
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.wanbaohe.recordcenter.model.RecordFieldsCodec
import com.wanbaohe.recordcenter.registry.RecordTypeCatalog
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition
import com.wanbaohe.recordcenter.service.RecordCenterService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 列表页时间范围筛选 */
enum class RecordRangeFilter { DAYS_7, DAYS_30, ALL }

/** 单个图表字段的统计数据(基于当前筛选范围) */
data class RecordFieldStats(
    val fieldKey: String,
    val latest: Float?,
    val average: Float?,
    val max: Float?,
    val min: Float?,
)

/**
 * 记录列表页 Component — 某一记录类型的趋势图、统计与记录明细。
 *
 * 记录列表统一按 happenedAt 倒序暴露,图表由界面层反转取正序。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordListComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted("recordType") val recordTypeKey: String,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    dispatchersHolder: DispatchersHolder,
    repository: HealthRecordRepository,
    catalog: RecordTypeCatalog,
    private val service: RecordCenterService,
) : BaseComponent(dispatchersHolder, componentContext) {

    /** 未知类型时为 null,界面层展示空态 */
    val definition: RecordTypeDefinition? = catalog.byKey(recordTypeKey)

    private val _rangeFilter = MutableStateFlow(RecordRangeFilter.DAYS_30)
    val rangeFilter: StateFlow<RecordRangeFilter> = _rangeFilter

    /** 当前范围内的记录,按 happenedAt 倒序 */
    val records: StateFlow<List<HealthRecordEntity>> = _rangeFilter
        .flatMapLatest { filter ->
            when (filter) {
                RecordRangeFilter.ALL -> repository.observeByType(recordTypeKey)
                else -> {
                    val to = System.currentTimeMillis()
                    val days = if (filter == RecordRangeFilter.DAYS_7) 7L else 30L
                    val from = to - TimeUnit.DAYS.toMillis(days)
                    repository.observeRange(recordTypeKey, from, to)
                }
            }
        }
        .map { list -> list.sortedByDescending { it.happenedAt } }
        .stateIn(componentScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    /** 每个图表字段的最新/平均/最高/最低 */
    val stats: StateFlow<List<RecordFieldStats>> = records
        .map { list ->
            val keys = definition?.chartFieldKeys.orEmpty()
            keys.map { key ->
                // 按时间正序取 latest,与展示无关,只看时间最新的一条
                val valuesByTime = list.sortedBy { it.happenedAt }
                    .mapNotNull { RecordFieldsCodec.decode(it.fieldsJson)[key] }
                RecordFieldStats(
                    fieldKey = key,
                    latest = valuesByTime.lastOrNull(),
                    average = valuesByTime.takeIf { it.isNotEmpty() }
                        ?.let { (it.sum() / it.size).toFloat() },
                    max = valuesByTime.maxOrNull(),
                    min = valuesByTime.minOrNull(),
                )
            }
        }
        .stateIn(componentScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun setRangeFilter(filter: RecordRangeFilter) {
        _rangeFilter.value = filter
    }

    fun deleteRecord(id: String) {
        componentScope.launch {
            service.deleteRecord(id, RecordCenterService.ACTOR_USER)
        }
    }

    fun navigateToAddRecord() {
        onNavigate(
            Screen.RecordCenter(Screen.RecordCenter.Type.AddRecord(recordType = recordTypeKey))
        )
    }

    fun navigateToEditRecord(recordId: String) {
        onNavigate(
            Screen.RecordCenter(
                Screen.RecordCenter.Type.AddRecord(
                    recordType = recordTypeKey,
                    editingRecordId = recordId,
                )
            )
        )
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            @Assisted("recordType") recordType: String,
            onGoBack: () -> Unit,
            onNavigate: (Screen) -> Unit,
        ): RecordListComponent
    }
}
