package com.wanbaohe.decisionwheel.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.arkivanov.decompose.ComponentContext
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.wanbaohe.com.color.ColorGenerator
import com.wanbaohe.decisionwheel.data.WheelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 转盘编辑页状态。
 *
 * 全程只改内存草稿，[save] 时一次性落库 —— 之前是"加选项立即写库、标题等保存才写"，
 * 用户中途放弃会留下脏数据。
 */
@Immutable
data class WheelEditorUiState(
    val title: String = "",
    val options: List<WheelOption> = emptyList(),
    val loading: Boolean = true,
    val dirty: Boolean = false,
    val canSave: Boolean = false
)

class DecisionWheelEditorComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted private val wheelId: String,
    @Assisted private val onGoBack: () -> Unit,
    private val repository: WheelRepository,
    dispatchersHolder: DispatchersHolder
) : BaseComponent(dispatchersHolder, componentContext) {

    private val _uiState = MutableStateFlow(WheelEditorUiState())
    val uiState = _uiState.asStateFlow()

    /** 最近一次删除，供 Snackbar 撤销 */
    private var lastRemoved: Pair<Int, WheelOption>? = null

    private var original: DecisionWheel? = null

    init {
        componentScope.launch {
            val wheel = repository.getWheelById(wheelId)
            original = wheel
            _uiState.update {
                WheelEditorUiState(
                    title = wheel?.title.orEmpty(),
                    options = wheel?.options.orEmpty(),
                    loading = false,
                    dirty = false,
                    canSave = wheel != null && wheel.title.isNotBlank() && wheel.options.size >= 2
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, dirty = true).validated() }
    }

    fun addOption(name: String) {
        if (name.isBlank()) return
        val current = _uiState.value.options
        val baseColor = current.firstOrNull { it.color != Color.Unspecified }?.color
            ?: ColorGenerator.generateSegmentBackgrounds(Color(0xFFA3B7F6), 1).first()
        val palette = ColorGenerator.generateSegmentBackgrounds(
            baseColor = baseColor,
            count = current.size + 1
        )
        val used = current.map { it.color }.toMutableSet()
        val color = palette.firstOrNull { c ->
            used.none { u -> closeEnough(u, c) }
        } ?: palette.getOrNull(current.size) ?: baseColor

        _uiState.update {
            it.copy(
                options = current + WheelOption(name = name.trim(), color = color),
                dirty = true
            ).validated()
        }
    }

    fun renameOption(optionId: String, name: String) {
        if (name.isBlank()) return
        _uiState.update {
            it.copy(
                options = it.options.map { o -> if (o.id == optionId) o.copy(name = name.trim()) else o },
                dirty = true
            )
        }
    }

    fun setWeight(optionId: String, weight: Float) {
        _uiState.update {
            it.copy(
                options = it.options.map { o ->
                    if (o.id == optionId) o.copy(weight = weight.coerceIn(0.25f, 10f)) else o
                },
                dirty = true
            )
        }
    }

    fun setOptionColor(optionId: String, color: Color) {
        _uiState.update {
            it.copy(
                options = it.options.map { o -> if (o.id == optionId) o.copy(color = color) else o },
                dirty = true
            )
        }
    }

    /** 返回 false 表示已达下限（至少保留 2 项），UI 应提示而不是弹确认框 */
    fun removeOption(optionId: String): Boolean {
        val current = _uiState.value.options
        if (current.size <= 2) return false
        val index = current.indexOfFirst { it.id == optionId }
        if (index < 0) return false
        lastRemoved = index to current[index]
        _uiState.update {
            it.copy(options = current.filterNot { o -> o.id == optionId }, dirty = true).validated()
        }
        return true
    }

    fun undoRemove() {
        val (index, option) = lastRemoved ?: return
        lastRemoved = null
        _uiState.update {
            val restored = it.options.toMutableList().apply {
                add(index.coerceIn(0, size), option)
            }
            it.copy(options = restored, dirty = true).validated()
        }
    }

    fun consumeUndo() {
        lastRemoved = null
    }

    fun moveOption(from: Int, to: Int) {
        val current = _uiState.value.options
        if (from !in current.indices || to !in current.indices) return
        _uiState.update {
            val moved = current.toMutableList().apply { add(to, removeAt(from)) }
            it.copy(options = moved, dirty = true)
        }
    }

    /** 整体换配色：以 baseColor 为基色重新生成一整套扇区色 */
    fun applyPalette(baseColor: Color) {
        val current = _uiState.value.options
        if (current.isEmpty()) return
        val backgrounds = ColorGenerator.generateSegmentBackgrounds(
            baseColor = baseColor,
            count = current.size
        )
        _uiState.update {
            it.copy(
                options = current.mapIndexed { i, o ->
                    o.copy(color = backgrounds.getOrNull(i) ?: o.color)
                },
                dirty = true
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        componentScope.launch {
            val base = original ?: return@launch
            repository.updateWheel(
                base.copy(title = state.title.trim(), options = state.options)
            )
            onGoBack()
        }
    }

    fun goBack() = onGoBack()

    private fun WheelEditorUiState.validated(): WheelEditorUiState = copy(
        canSave = title.isNotBlank() && options.size >= 2
    )

    private fun closeEnough(a: Color, b: Color): Boolean =
        kotlin.math.abs(a.red - b.red) < 0.01f &&
            kotlin.math.abs(a.green - b.green) < 0.01f &&
            kotlin.math.abs(a.blue - b.blue) < 0.01f

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            wheelId: String,
            onGoBack: () -> Unit
        ): DecisionWheelEditorComponent
    }
}
