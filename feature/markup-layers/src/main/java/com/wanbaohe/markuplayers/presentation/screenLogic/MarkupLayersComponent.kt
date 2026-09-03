package com.wanbaohe.markuplayers.presentation.screenLogic

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.shifenmiao.common.recent.RecentAccessRepository
import com.shifenmiao.common.utils.BaseUtils
import com.shifenmiao.database.recent_access.entity.RecentAccessEntity
import com.shifenmiao.network.repository.BaiduImageProcessRepository
import com.shifenmiao.imagegeneration.loader.ImageGenerationLoader
import com.shifenmiao.imagegeneration.model.ImageGenerationRequest
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.domain.image.ImageCompressor
import com.t8rin.imagetoolbox.core.domain.image.ImageGetter
import com.t8rin.imagetoolbox.core.domain.image.ImageScaler
import com.t8rin.imagetoolbox.core.domain.image.ImageShareProvider
import com.t8rin.imagetoolbox.core.domain.image.model.ImageFormat
import com.t8rin.imagetoolbox.core.domain.image.model.ImageInfo
import com.t8rin.imagetoolbox.core.domain.model.IntegerSize
import com.t8rin.imagetoolbox.core.domain.saving.FileController
import com.t8rin.imagetoolbox.core.domain.saving.model.ImageSaveTarget
import com.t8rin.imagetoolbox.core.domain.saving.model.SaveResult
import com.t8rin.imagetoolbox.core.domain.transformation.Transformation
import com.t8rin.imagetoolbox.core.domain.utils.smartJob
import com.t8rin.imagetoolbox.core.filters.domain.FilterProvider
import com.t8rin.imagetoolbox.core.filters.presentation.model.UiFilter
import com.t8rin.imagetoolbox.core.filters.presentation.widget.FilterTemplateCreationSheetComponent
import com.t8rin.imagetoolbox.core.filters.presentation.widget.addFilters.AddFiltersSheetComponent
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.t8rin.imagetoolbox.core.ui.utils.helper.AppToastHost
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.t8rin.imagetoolbox.core.ui.utils.state.update
import com.t8rin.imagetoolbox.core.ui.widget.editor.StickerSource
import com.t8rin.imagetoolbox.core.utils.appContext
import com.t8rin.imagetoolbox.core.utils.fileProviderAuthority
import com.t8rin.imagetoolbox.core.utils.filename
import com.t8rin.logger.makeLog
import com.wanbaohe.markuplayers.R
import com.wanbaohe.markuplayers.data.render.DrawLayerExportRenderer
import com.wanbaohe.markuplayers.data.render.STICKER_EXPORT_BASE_RATIO
import com.wanbaohe.markuplayers.data.render.StickerLayerExportRenderer
import com.wanbaohe.markuplayers.domain.MarkupLayersApplier
import com.wanbaohe.markuplayers.domain.history.LayerHistory
import com.shifenmiao.base.utils.aiImageProcessPointsCost
import com.wanbaohe.markuplayers.domain.model.AiImageOp
import com.wanbaohe.markuplayers.domain.model.LayerBaseRemap
import com.wanbaohe.markuplayers.domain.model.LayerBlendMode
import com.wanbaohe.markuplayers.domain.model.LayerTransform
import com.wanbaohe.markuplayers.domain.model.LayerType
import com.wanbaohe.markuplayers.domain.model.MarkupLayer
import com.wanbaohe.markuplayers.domain.model.NormalizedRect
import com.wanbaohe.markuplayers.domain.model.ShapeSpec
import com.wanbaohe.markuplayers.domain.model.isBitmapFilterable
import com.wanbaohe.markuplayers.domain.model.toImageProcessRect
import com.wanbaohe.markuplayers.presentation.export.ExportSettings
import com.wanbaohe.markuplayers.presentation.export.toExportFormat
import com.wanbaohe.markuplayers.presentation.editor.CanvasBackground
import com.wanbaohe.markuplayers.presentation.render.IMAGE_LAYER_BASE_WIDTH_RATIO
import com.wanbaohe.markuplayers.presentation.tools.adjust.BaseAdjustments
import com.wanbaohe.markuplayers.presentation.tools.adjust.toColorMatrixValues
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 图层创作页面组件。
 *
 * 图层列表即唯一事实来源,所有修改操作(增/删/改/排序)都先向 [history]
 * 记录快照再变更,undo/redo 走 [LayerHistory] 的整体快照恢复。
 * 保存/分享不再走 Compose 截图,而是取全分辨率原图交由
 * [markupLayersApplier] 按原图尺寸重绘。
 */
class MarkupLayersComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted initialUri: Uri?,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    dispatchersHolder: DispatchersHolder,
    private val fileController: FileController,
    private val imageCompressor: ImageCompressor<Bitmap>,
    private val imageGetter: ImageGetter<Bitmap>,
    private val imageScaler: ImageScaler<Bitmap>,
    private val shareProvider: ImageShareProvider<Bitmap>,
    private val markupLayersApplier: MarkupLayersApplier<Bitmap>,
    private val drawLayerExportRenderer: DrawLayerExportRenderer,
    private val stickerLayerExportRenderer: StickerLayerExportRenderer,
    private val imageProcessRepository: BaiduImageProcessRepository,
    private val imageGenerationLoader: ImageGenerationLoader,
    private val filterProvider: FilterProvider<Bitmap>,
    private val recentAccessRepository: RecentAccessRepository,
    addFiltersSheetComponentFactory: AddFiltersSheetComponent.Factory,
    filterTemplateCreationSheetComponentFactory: FilterTemplateCreationSheetComponent.Factory
) : BaseComponent(dispatchersHolder, componentContext) {

    // AddFiltersSheet 子组件:随本组件创建,生命周期一致
    val addFiltersSheetComponent: AddFiltersSheetComponent = addFiltersSheetComponentFactory(
        componentContext = componentContext.childContext(key = "markupAddFiltersSheet")
    )

    val filterTemplateCreationSheetComponent: FilterTemplateCreationSheetComponent =
        filterTemplateCreationSheetComponentFactory(
            componentContext = componentContext.childContext(
                key = "markupFilterTemplateCreationSheet"
            )
        )

    init {
        observeRecentProjects()
        debounce {
            initialUri?.let {
                setUri(
                    uri = it,
                    onFailure = {}
                )
            }
        }
    }

    private val history = LayerHistory()

    private val _bitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val bitmap: Bitmap? by _bitmap

    private val _uri: MutableState<Uri?> = mutableStateOf(null)
    val uri: Uri? by _uri

    // 空白画布底图:无 uri 场景下的全分辨率底图,随 setUri/resetState/applyBaseTransform 清理
    private val _blankBaseBitmap: MutableState<Bitmap?> = mutableStateOf(null)

    val hasImage: Boolean get() = _bitmap.value != null

    private val _layers: MutableState<List<MarkupLayer>> = mutableStateOf(emptyList())
    val layers: List<MarkupLayer> by _layers

    private val _selectedLayerId: MutableState<String?> = mutableStateOf(null)
    val selectedLayerId: String? by _selectedLayerId

    private val _activeToolId: MutableState<String?> = mutableStateOf(null)
    val activeToolId: String? by _activeToolId

    private val _imageFormat = mutableStateOf(ImageFormat.Default)
    val imageFormat by _imageFormat

    private val _saveExif: MutableState<Boolean> = mutableStateOf(false)
    val saveExif: Boolean by _saveExif

    // 导出设置(格式/质量/分辨率/导出后分享),由导出设置面板编辑
    private val _exportSettings = mutableStateOf(ExportSettings())
    val exportSettings by _exportSettings

    // 原图全分辨率尺寸(供导出面板展示各档位实际像素)
    private val _sourceSize: MutableState<IntSize?> = mutableStateOf(null)
    val sourceSize: IntSize? by _sourceSize

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving: Boolean by _isSaving

    private val _canUndo: MutableState<Boolean> = mutableStateOf(false)
    val canUndo: Boolean by _canUndo

    private val _canRedo: MutableState<Boolean> = mutableStateOf(false)
    val canRedo: Boolean by _canRedo

    private var savingJob: Job? by smartJob {
        _isSaving.update { false }
    }

    private var mergeJob: Job? by smartJob {
        _isSaving.update { false }
    }

    private var baseTransformJob: Job? by smartJob {
        _isSaving.update { false }
    }

    // ---------------- 基础调节(亮度/对比度/饱和度) ----------------

    /**
     * 调节/滤镜的作用目标:当前选中图层;未选中任何图层时 = 背景图。
     * 调节(亮度/对比度/饱和度)所有图层类型与背景图都支持,实时生效;
     * 滤镜仅位图类图层([isBitmapFilterable])与背景图支持,文字/形状需先光栅化。
     */
    val selectedLayer: MarkupLayer?
        get() = _layers.value.firstOrNull { it.id == _selectedLayerId.value }

    /** 当前目标的调节参数(选中图层 → 图层自身;否则背景图) */
    val targetAdjustments: BaseAdjustments
        get() = selectedLayer?.adjustments ?: _baseAdjustments.value

    /** 当前目标的滤镜(选中图层 → 图层自身;否则背景图) */
    val targetFilter: UiFilter<*>?
        get() = selectedLayer?.filter ?: _selectedFilter.value

    /** 当前选中且需先光栅化才能用滤镜的图层(文字/形状);无或其他类型为 null */
    val rasterizeRequiredLayer: MarkupLayer?
        get() = selectedLayer?.takeIf { !it.type.isBitmapFilterable }

    /**
     * 背景图的调节参数,不进 undo 历史;预览经底图 Picture 的 colorFilter
     * 实时生效(仅底图),导出时烘焙进全分辨率底图。选中图层时调节走图层字段
     * (见 [updateBaseAdjustments] 分流),本状态保持不变。
     */
    private val _baseAdjustments: MutableState<BaseAdjustments> = mutableStateOf(BaseAdjustments())
    val baseAdjustments: BaseAdjustments by _baseAdjustments

    /**
     * 更新调节参数,按目标分流:选中图层 → 写入图层 adjustments(transient,
     * 拖动开始的快照由 [beginAdjustmentsChange] 记录,整段拖动 = 一步 undo);
     * 无选中 → 写背景图状态(不进 undo)。
     */
    fun updateBaseAdjustments(adjustments: BaseAdjustments) {
        val target = selectedLayer
        if (target != null) {
            updateLayerAdjustmentsTransient(target.id, adjustments)
            return
        }
        if (adjustments == _baseAdjustments.value) return
        _baseAdjustments.value = adjustments
        registerChanges()
    }

    /**
     * 调节拖动开始:目标是图层时记一次历史快照(配合 [updateBaseAdjustments]
     * 的 transient 写入,整段滑杆拖动 = 一步 undo);背景图不进 undo,无需快照。
     */
    fun beginAdjustmentsChange() {
        if (selectedLayer == null) return
        history.snapshot(_layers.value)
        onLayersChanged()
    }

    /** 图层调节拖动中:直接改值不入历史(快照已在 [beginAdjustmentsChange] 记录) */
    private fun updateLayerAdjustmentsTransient(
        id: String,
        adjustments: BaseAdjustments
    ) {
        if (_layers.value.none { it.id == id }) return
        _layers.update { list ->
            list.map { if (it.id == id) it.copy(adjustments = adjustments) else it }
        }
        registerChanges()
    }

    /** 重置当前目标的调节:图层走正常 updateLayer(入历史),背景图直接清零 */
    fun resetBaseAdjustments() {
        val target = selectedLayer
        if (target != null) {
            if (target.adjustments.isNeutral) return
            updateLayer(target.id) { it.copy(adjustments = BaseAdjustments()) }
        } else {
            updateBaseAdjustments(BaseAdjustments())
        }
    }

    // ---------------- 滤镜(目标分流:选中位图类图层 → 图层;无选中 → 背景图) ----------------

    /** 背景图的滤镜,不进 undo 历史;选中图层时滤镜走图层字段,本状态保持不变 */
    private val _selectedFilter: MutableState<UiFilter<*>?> = mutableStateOf(null)
    val selectedFilter: UiFilter<*>? by _selectedFilter

    // 背景滤镜预览缓存(仅底图):对展示底图应用滤镜后的结果,随底图/滤镜变化异步重算
    private val _filterPreviewBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val filterPreviewBitmap: Bitmap? by _filterPreviewBitmap

    private var filterPreviewJob: Job? by smartJob()

    /**
     * 图层滤镜位图缓存:layerId → 过滤后的图层内容位图(按底图全分辨率预算,
     * 预览缩放显示、导出直接复用,保证所见即所得且不重复过滤大图)。
     * 有效性按「内容(type)+ 滤镜」判定,任一变化即失效重算(见 [syncLayerFilterCache])。
     */
    private class LayerFilterCacheEntry(
        val type: LayerType,
        val filter: UiFilter<*>,
        val bitmap: Bitmap,
    )

    private val layerFilterCache = mutableStateMapOf<String, LayerFilterCacheEntry>()

    private var layerFilterJob: Job? by smartJob()

    /** 画布展示用底图(仅底图,不含图层):背景滤镜预览优先,背景调节经底图 colorFilter 叠加 */
    val displayBitmap: Bitmap? get() = _filterPreviewBitmap.value ?: _bitmap.value

    /**
     * 选择滤镜,按目标分流:选中位图类图层 → 写入图层 filter(经 [updateLayer] 入 undo
     * 历史);选中文字/形状图层时不直接赋值(应由 UI 先走光栅化确认,见
     * [rasterizeRequiredLayer]/[rasterizeLayer]),但清除滤镜(filter = null)仍然允许;
     * 无选中 → 写背景图滤镜状态(不进 undo,与历史行为一致)。
     */
    fun selectFilter(filter: UiFilter<*>?) {
        val target = selectedLayer
        if (target != null) {
            if (filter != null && !target.type.isBitmapFilterable) return
            if (filter === target.filter) return
            if (filter != null && target.filter != null &&
                filter::class == target.filter::class
            ) return
            updateLayer(target.id) { it.copy(filter = filter) }
            return
        }
        val current = _selectedFilter.value
        if (filter === current) return
        if (filter != null && current != null && filter::class == current::class) return
        _selectedFilter.value = filter
        registerChanges()
        updateFilterPreview()
    }

    /** 滤镜 → 变换(供缩略图 coil transformation 与导出烘焙复用同一入口) */
    fun filterTransformation(filter: UiFilter<*>): Transformation<Bitmap> =
        filterProvider.filterToTransformation(filter)

    private fun updateFilterPreview() {
        val filter = _selectedFilter.value
        val base = _bitmap.value
        if (filter == null || base == null) {
            _filterPreviewBitmap.value = null
            return
        }
        filterPreviewJob = componentScope.launch {
            _filterPreviewBitmap.value = runCatching {
                withContext(defaultDispatcher) {
                    filterTransformation(filter)
                        .transform(base, IntegerSize(base.width, base.height))
                }
            }.getOrNull()
        }
    }

    /** 把背景滤镜烘焙进位图;无滤镜或变换失败时返回原位图 */
    private suspend fun Bitmap.withSelectedFilter(): Bitmap {
        val filter = _selectedFilter.value ?: return this
        return runCatching {
            withContext(defaultDispatcher) {
                filterTransformation(filter)
                    .transform(this@withSelectedFilter, IntegerSize(width, height))
            }
        }.getOrNull() ?: this
    }

    /**
     * 取图层过滤后的内容位图(命中缓存且「内容+滤镜」均未变时返回),供预览渲染器使用;
     * 未命中返回 null,由 [syncLayerFilterCache] 异步补齐后触发重组。
     */
    fun layerFilteredBitmap(layer: MarkupLayer): Bitmap? {
        val filter = layer.filter ?: return null
        val entry = layerFilterCache[layer.id] ?: return null
        return entry
            .takeIf { it.filter::class == filter::class && it.type == layer.type }
            ?.bitmap
    }

    /**
     * 同步图层滤镜缓存:剔除失效条目(图层删除/内容变更/滤镜变更),
     * 串行(smartJob)补齐缺失条目。图层提交级变更([onLayersChanged])统一走这里,
     * 按条目有效性精确失效,undo/redo 后旧值不匹配同样会按需重算。
     */
    private fun syncLayerFilterCache() {
        val layers = _layers.value
        val byId = layers.associateBy { it.id }
        layerFilterCache.keys.toList().forEach { id ->
            val layer = byId[id]
            val entry = layerFilterCache[id]
            val valid = layer != null && entry != null && layer.filter != null &&
                entry.filter::class == layer.filter::class &&
                entry.type == layer.type
            if (!valid) layerFilterCache.remove(id)
        }
        val pending = layers.filter { layer ->
            layer.filter != null && layer.type.isBitmapFilterable &&
                layerFilteredBitmap(layer) == null
        }
        if (pending.isEmpty()) return
        layerFilterJob = componentScope.launch {
            pending.forEach { layer ->
                val bitmap = computeFilteredLayerBitmap(layer)
                // 计算期间图层可能已变(undo/编辑),回写前再校验一次内容与滤镜
                val current = _layers.value.firstOrNull { it.id == layer.id }
                val stillValid = current != null && current.type == layer.type &&
                    current.filter?.let { it::class } == layer.filter?.let { it::class }
                if (bitmap != null && isActive && stillValid) {
                    layerFilterCache[layer.id] = LayerFilterCacheEntry(
                        type = layer.type,
                        filter = layer.filter!!,
                        bitmap = bitmap
                    )
                }
            }
        }
    }

    /**
     * 预算图层「内容过滤镜后」位图(底图全分辨率,导出同尺寸复用):
     * 图片 = 解码源图过滤镜;贴纸 = 按导出基础尺寸解码过滤镜;涂鸦 = 笔画位图过滤镜。
     */
    private suspend fun computeFilteredLayerBitmap(layer: MarkupLayer): Bitmap? {
        val filter = layer.filter ?: return null
        val size = _sourceSize.value
            ?: _bitmap.value?.let { IntSize(it.width, it.height) }
            ?: return null
        if (size.width <= 0 || size.height <= 0) return null
        val decoded: Bitmap = when (val type = layer.type) {
            is LayerType.Image -> runCatching {
                imageGetter.getImage(data = type.imageData)
            }.getOrNull()

            is LayerType.Sticker -> runCatching {
                stickerLayerExportRenderer.decodeBitmap(
                    type = type,
                    baseSizePx = size.width * STICKER_EXPORT_BASE_RATIO
                )
            }.getOrNull()

            is LayerType.Draw -> drawLayerExportRenderer.renderStrokesBitmap(
                strokes = type.strokes,
                imageWidth = size.width,
                imageHeight = size.height
            ).takeIf { type.strokes.isNotEmpty() }

            else -> null
        } ?: return null
        // HARDWARE 配置位图无法参与像素级滤镜变换,统一转软件位图兜底(同 loadBaseBitmap)
        val content = decoded.takeIf { it.config == Bitmap.Config.HARDWARE }
            ?.copy(Bitmap.Config.ARGB_8888, false) ?: decoded
        return runCatching {
            withContext(defaultDispatcher) {
                filterTransformation(filter)
                    .transform(content, IntegerSize(content.width, content.height))
            }
        }.getOrNull()
    }

    /** 预算一组图层的过滤后位图(layerId → 位图),优先复用缓存,缺失即算并回写 */
    private suspend fun filteredBitmapsFor(layers: List<MarkupLayer>): Map<String, Bitmap> {
        val result = mutableMapOf<String, Bitmap>()
        layers.forEach { layer ->
            val filter = layer.filter
            if (filter == null || !layer.type.isBitmapFilterable) return@forEach
            val bitmap = layerFilteredBitmap(layer) ?: computeFilteredLayerBitmap(layer)
                ?.also { filtered ->
                    layerFilterCache[layer.id] = LayerFilterCacheEntry(
                        type = layer.type,
                        filter = filter,
                        bitmap = filtered
                    )
                }
            if (bitmap != null) result[layer.id] = bitmap
        }
        return result
    }

    // ---------------- 画布背景(操作台显示,会话级不持久化) ----------------

    /** 编辑器画布背景:默认(主题 surface 色)或纯色;仅影响画布显示,不参与导出,故不记 changes */
    private val _canvasBackground: MutableState<CanvasBackground> =
        mutableStateOf(CanvasBackground.Default)
    val canvasBackground: CanvasBackground by _canvasBackground

    fun setCanvasBackground(background: CanvasBackground) {
        _canvasBackground.value = background
    }

    // ---------------- 空白画布(无 uri 的内存底图) ----------------

    /**
     * 以纯色/透明底图开始创作:不产生 uri,预览/导出/分享全链路走内存底图
     * (见 [loadBaseBitmap])。[backgroundColor] 为 null 时透明,默认 PNG;
     * 否则以该颜色填充,默认 JPG。
     */
    fun startWithBlankCanvas(
        width: Int,
        height: Int,
        backgroundColor: Int?
    ) {
        val transparent = backgroundColor == null
        val safeWidth = width.coerceIn(1, MAX_CANVAS_SIDE)
        val safeHeight = height.coerceIn(1, MAX_CANVAS_SIDE)
        componentScope.launch {
            _layers.value = emptyList()
            _selectedLayerId.value = null
            _baseAdjustments.value = BaseAdjustments()
            _selectedFilter.value = null
            _filterPreviewBitmap.value = null
            layerFilterJob = null
            layerFilterCache.clear()
            history.clear()
            _canUndo.value = false
            _canRedo.value = false
            _isImageLoading.value = true

            val base = withContext(defaultDispatcher) {
                Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888).apply {
                    if (!transparent) eraseColor(backgroundColor)
                }
            }
            _uri.value = null
            _blankBaseBitmap.value = base
            _sourceSize.value = IntSize(safeWidth, safeHeight)
            val format = if (transparent) ImageFormat.Png.Lossless else ImageFormat.Jpg
            _imageFormat.update { format }
            _exportSettings.update { it.copy(format = format.toExportFormat()) }
            updateBitmap(base)
        }
    }

    /** 全分辨率底图:uri 场景解码原图;空白画布场景用内存底图 */
    private suspend fun loadBaseBitmap(): Bitmap? {
        val base = _uri.value?.let { imageGetter.getImage(data = it) } ?: _blankBaseBitmap.value
        // HARDWARE 配置位图无法绘制进软件 Canvas,会让矩阵变换/抠图直接抛
        // IllegalArgumentException;这里统一转软件位图兜底
        return base?.takeIf { it.config == Bitmap.Config.HARDWARE }
            ?.copy(Bitmap.Config.ARGB_8888, false)
            ?: base
    }

    // ---------------- 裁剪 / 旋转 / 翻转(作用于底图) ----------------

    /**
     * 确认裁剪旋转变换:把 90° 步进 + 自由旋转 + 翻转 + 裁剪框合成一次变换,
     * 作用于全分辨率原图,缓存后替换底图(保留图层与历史,不走 [setUri])。
     *
     * 图层结算:自由旋转角为 0 时按同一仿射精确重映射([LayerBaseRemap]);
     * 非 0 时先把可见图层烘焙进底图再变换并清空图层列表。图层变更只记一次
     * undo 快照(底图本身的变化不可撤销,快照用于恢复变换前的图层列表)。
     */
    fun applyBaseTransform(
        rotationSteps: Int,
        freeRotation: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        cropRect: NormalizedRect,
    ) {
        if (_uri.value == null && _blankBaseBitmap.value == null) {
            "applyBaseTransform skipped: no base image".makeLog(LOG_TAG)
            return
        }
        val oldSize = _sourceSize.value ?: run {
            "applyBaseTransform skipped: source size not ready".makeLog(LOG_TAG)
            return
        }
        val steps = ((rotationSteps % 4) + 4) % 4
        val isIdentity = steps == 0 && freeRotation == 0f &&
            !flipHorizontal && !flipVertical && cropRect == NormalizedRect.Full
        if (isIdentity) return

        baseTransformJob = componentScope.launch {
            _isSaving.value = true
            val outcome = runCatching {
                withContext(defaultDispatcher) {
                    var source = loadBaseBitmap()
                        ?: error("source image is null (uri=${_uri.value != null}, blank=${_blankBaseBitmap.value != null})")
                    // 自由旋转下精确重映射图层成本高:先烘焙可见图层(含各图层自身滤镜/调节),确认后清空
                    val bakeLayers = freeRotation != 0f && _layers.value.isNotEmpty()
                    if (bakeLayers) {
                        val visibleLayers = _layers.value.filter { it.transform.visible }
                        source = markupLayersApplier.applyToImage(
                            image = source,
                            layers = visibleLayers,
                            filteredLayers = filteredBitmapsFor(visibleLayers)
                        )
                    }
                    val transformed = source.transformed(
                        degrees = steps * 90f + freeRotation,
                        flipHorizontal = flipHorizontal,
                        flipVertical = flipVertical
                    )
                    transformed.cropped(cropRect) to bakeLayers
                }
            }
            val (result, baked) = outcome.getOrNull() ?: run {
                outcome.exceptionOrNull()?.makeLog(LOG_TAG)
                AppToastHost.showFailureToast(R.string.markup_base_transform_failed)
                _isSaving.value = false
                return@launch
            }
            val cachedUri = shareProvider.cacheImage(
                image = result,
                imageInfo = ImageInfo(
                    width = result.width,
                    height = result.height,
                    imageFormat = ImageFormat.Png.Lossless
                )
            )?.toUri()
            if (cachedUri == null) {
                "applyBaseTransform failed: cacheImage returned null, size=${result.width}x${result.height}"
                    .makeLog(LOG_TAG)
                AppToastHost.showFailureToast(R.string.markup_base_transform_failed)
                _isSaving.value = false
                return@launch
            }

            if (_layers.value.isNotEmpty()) history.snapshot(_layers.value)
            _uri.value = cachedUri
            _blankBaseBitmap.value = null
            _sourceSize.value = IntSize(result.width, result.height)
            _imageFormat.update { ImageFormat.Png.Lossless }
            _exportSettings.update {
                it.copy(format = ImageFormat.Png.Lossless.toExportFormat())
            }
            _layers.value = if (baked) {
                emptyList()
            } else {
                LayerBaseRemap.remap(
                    layers = _layers.value,
                    oldWidth = oldSize.width,
                    oldHeight = oldSize.height,
                    rotationSteps = steps,
                    flipHorizontal = flipHorizontal,
                    flipVertical = flipVertical,
                    cropRect = cropRect
                )
            }
            _selectedLayerId.value = null
            if (baked) AppToastHost.showToast(R.string.markup_layers_merged)
            updateBitmap(result)
            onLayersChanged()
            _isSaving.value = false
        }
    }

    /** 翻转 + 旋转合成一次矩阵变换;恒等时返回原位图 */
    private fun Bitmap.transformed(
        degrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
    ): Bitmap {
        val normalizedDegrees = ((degrees % 360f) + 360f) % 360f
        if (normalizedDegrees == 0f && !flipHorizontal && !flipVertical) return this
        val matrix = Matrix().apply {
            postScale(
                if (flipHorizontal) -1f else 1f,
                if (flipVertical) -1f else 1f,
                width / 2f,
                height / 2f
            )
            postRotate(normalizedDegrees)
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /**
     * 按归一化裁剪框抠图;框即整图时返回原位图。
     * 归一化坐标 × 位图尺寸四舍五入后分别钳制:left/top ∈ [0, 宽/高-1],
     * right ∈ [left+1, 宽],bottom ∈ [top+1, 高],保证 createBitmap
     * 的 w/h ≥ 1 且 x+w ≤ width,浮点 ±1 误差不会越界。
     */
    private fun Bitmap.cropped(rect: NormalizedRect): Bitmap {
        if (rect == NormalizedRect.Full) return this
        val left = (rect.left * width).roundToInt().coerceIn(0, width - 1)
        val top = (rect.top * height).roundToInt().coerceIn(0, height - 1)
        val right = (rect.right * width).roundToInt().coerceIn(left + 1, width)
        val bottom = (rect.bottom * height).roundToInt().coerceIn(top + 1, height)
        val cropWidth = right - left
        val cropHeight = bottom - top
        if (left == 0 && top == 0 && cropWidth == width && cropHeight == height) return this
        return Bitmap.createBitmap(this, left, top, cropWidth, cropHeight)
    }

    /** 把基础调节烘焙进位图;全 0 时返回原位图 */
    private fun Bitmap.withBaseAdjustments(adjustments: BaseAdjustments): Bitmap {
        if (adjustments.isNeutral) return this
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(adjustments.toColorMatrixValues()))
        }
        Canvas(output).drawBitmap(this, 0f, 0f, paint)
        return output
    }

    // ---------------- AI 图像处理(作用于底图,图层全部保留) ----------------

    private val _isAiProcessing: MutableState<Boolean> = mutableStateOf(false)
    val isAiProcessing: Boolean by _isAiProcessing

    private var aiProcessJob: Job? by smartJob {
        _isAiProcessing.update { false }
    }

    /**
     * AI 图像处理:取全分辨率底图([loadBaseBitmap])调 [BaiduImageProcessRepository],
     * 成功后结果图缓存并替换底图(与 [applyBaseTransform] 同路径直改 _uri/_sourceSize/
     * _bitmap,图层列表不动,归一化坐标天然适配;抠图结果为透明底 PNG)。
     * 处理中重复调用直接忽略;[cancelAiProcessing] 取消进行中的请求。
     * 底图变化与裁剪一样不可撤销,这里无需记图层快照。
     *
     * 登录与积分预检由调用方(UI 层 ActionUtils.ensureLoginAndCheckPoints)完成;
     * [pointsCost] > 0 时,仅处理成功后经 BaseUtils.consumePoints 扣积分,失败不扣。
     */
    fun processAiImage(
        op: AiImageOp,
        rect: NormalizedRect? = null,
        pointsCost: Int = 0,
    ) {
        if (_isAiProcessing.value) return
        if (_uri.value == null && _blankBaseBitmap.value == null) {
            "processAiImage skipped: no base image".makeLog(LOG_TAG)
            return
        }
        aiProcessJob = componentScope.launch {
            _isAiProcessing.value = true
            val outcome = runCatching {
                val source = loadBaseBitmap()
                    ?: error("source image is null (uri=${_uri.value != null}, blank=${_blankBaseBitmap.value != null})")
                imageProcessRepository.process(
                    op = op.processOp,
                    bitmap = source,
                    rect = rect?.toImageProcessRect()
                ).getOrThrow()
            }
            val result = outcome.getOrNull()
            if (result == null) {
                val failure = outcome.exceptionOrNull()
                // 用户取消:不提示,交由 smartJob 复位状态
                if (failure is CancellationException) throw failure
                failure?.makeLog(LOG_TAG)
                AppToastHost.showFailureToast(
                    failure?.message?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.markup_ai_process_failed)
                )
                _isAiProcessing.value = false
                return@launch
            }
            val cachedUri = shareProvider.cacheImage(
                image = result,
                imageInfo = ImageInfo(
                    width = result.width,
                    height = result.height,
                    imageFormat = ImageFormat.Png.Lossless
                )
            )?.toUri()
            if (cachedUri == null) {
                "processAiImage failed: cacheImage returned null, size=${result.width}x${result.height}"
                    .makeLog(LOG_TAG)
                AppToastHost.showFailureToast(R.string.markup_ai_process_failed)
                _isAiProcessing.value = false
                return@launch
            }
            _uri.value = cachedUri
            _blankBaseBitmap.value = null
            _sourceSize.value = IntSize(result.width, result.height)
            _imageFormat.update { ImageFormat.Png.Lossless }
            _exportSettings.update {
                it.copy(format = ImageFormat.Png.Lossless.toExportFormat())
            }
            updateBitmap(result)
            onLayersChanged()
            AppToastHost.showToast(R.string.markup_ai_process_success)
            // 处理成功才扣积分(失败/取消走到上面 return,不会扣)
            if (pointsCost > 0) {
                BaseUtils.consumePoints(
                    degree = pointsCost,
                    desc = appContext.getString(op.nameRes),
                    source = AI_POINTS_SOURCE,
                    showToast = true
                )
            }
            _isAiProcessing.value = false
        }
    }

    fun cancelAiProcessing() {
        aiProcessJob?.cancel()
        aiProcessJob = null
        _isAiProcessing.value = false
    }

    // ---------------- AI 生成图片(文生图;选中图片图层时图生图编辑) ----------------

    /**
     * AI 生成图片:登录与积分预检由调用方(UI 层)完成;复用 [isAiProcessing]
     * 防二次并驱动 LoadingDialog(不做占位图层)。
     * 新建:成功 addLayer(Image 图层,自动进 undo 历史);编辑:选中 Image 图层的
     * imageData 转 base64 data URI 作输入图,成功 updateLayer 换图(undo 即回退)。
     * 仅非缓存结果(!fromCache)扣积分,失败/取消不扣。
     */
    fun generateImageLayer(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isAiProcessing.value) return
        aiProcessJob = componentScope.launch {
            _isAiProcessing.value = true
            val editLayer = _layers.value.firstOrNull {
                it.id == _selectedLayerId.value && it.type is LayerType.Image
            }
            // 编辑模式:本地图片读成 base64 data URI 作为输入图(服务商不接受本地路径)
            val inputImages = if (editLayer != null) {
                val path = (editLayer.type as LayerType.Image).imageData.toString()
                runCatching {
                    withContext(defaultDispatcher) { encodeImageAsDataUri(path) }
                }.getOrNull()?.let(::listOf) ?: run {
                    AppToastHost.showFailureToast(R.string.markup_ai_process_failed)
                    _isAiProcessing.value = false
                    return@launch
                }
            } else emptyList()

            val (sourceWidth, sourceHeight) = _sourceSize.value
                ?.let { it.width to it.height }
                ?: _bitmap.value?.let { it.width to it.height }
                ?: (1080 to 1080)

            imageGenerationLoader.load(
                ImageGenerationRequest(
                    prompt = trimmed,
                    inputImages = inputImages,
                    outputSize = qwenOutputSize(sourceWidth, sourceHeight)
                )
            ).onSuccess { image ->
                val path = image.file.absolutePath
                if (editLayer != null) {
                    updateLayer(editLayer.id) { layer ->
                        layer.copy(type = LayerType.Image(imageData = path))
                    }
                } else {
                    addLayer(MarkupLayer(type = LayerType.Image(imageData = path)))
                }
                if (!image.fromCache) {
                    BaseUtils.consumePoints(
                        degree = aiImageProcessPointsCost(),
                        desc = appContext.getString(R.string.markup_ai_generate_title),
                        source = AI_POINTS_SOURCE,
                        showToast = true
                    )
                }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                failure.makeLog(LOG_TAG)
                AppToastHost.showFailureToast(
                    failure.message?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.markup_ai_process_failed)
                )
            }
            _isAiProcessing.value = false
        }
    }

    // ---------------- AI 生成贴纸(共享贴纸弹层 AI tab) ----------------

    /** AI 生成贴纸进行中(弹层内 loading;不复用 isAiProcessing,避免触发全屏 LoadingDialog) */
    private val _isGeneratingSticker: MutableState<Boolean> = mutableStateOf(false)
    val isGeneratingSticker: Boolean by _isGeneratingSticker

    private var generateStickerJob: Job? by smartJob {
        _isGeneratingSticker.update { false }
    }

    /**
     * AI 生成贴纸:文生图(提示词追加贴纸风格描述,正方形输出),成功落 Sticker 图层
     *(addLayer 自动进 undo 历史),[onSuccess] 供弹层关闭。
     * 登录与积分预检由调用方(UI 层 ActionUtils.ensureLoginAndCheckPoints)完成;
     * 仅非缓存结果(!fromCache)扣积分,失败/取消不扣。
     */
    fun generateSticker(prompt: String, onSuccess: () -> Unit = {}) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isGeneratingSticker.value) return
        generateStickerJob = componentScope.launch {
            _isGeneratingSticker.value = true
            imageGenerationLoader.load(
                ImageGenerationRequest(
                    prompt = "$trimmed, $STICKER_PROMPT_STYLE_SUFFIX",
                    outputSize = STICKER_OUTPUT_SIZE
                )
            ).onSuccess { image ->
                addLayer(
                    MarkupLayer(
                        type = LayerType.Sticker(StickerSource.Generated(image.file.absolutePath))
                    )
                )
                if (!image.fromCache) {
                    BaseUtils.consumePoints(
                        degree = aiImageProcessPointsCost(),
                        desc = appContext.getString(
                            com.t8rin.imagetoolbox.core.resources.R.string.sticker_category_ai
                        ),
                        source = AI_POINTS_SOURCE,
                        showToast = true
                    )
                }
                onSuccess()
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                failure.makeLog(LOG_TAG)
                AppToastHost.showFailureToast(
                    failure.message?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.markup_ai_process_failed)
                )
            }
            _isGeneratingSticker.value = false
        }
    }

    /** 本地图片文件 → base64 data URI(图生图输入) */
    private fun encodeImageAsDataUri(path: String): String {
        val file = File(path)
        val mime = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }
        return "data:$mime;base64," + Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
    }

    /** 生成输出尺寸(服务商限制:像素总数 ≤ 2048²、宽高比 ≤ 8:1),按底图等比收窄 */
    private fun qwenOutputSize(width: Int, height: Int): String {
        val maxPixels = 2048.0 * 2048.0
        val pixels = width.toDouble() * height
        val scale = if (pixels > maxPixels) sqrt(maxPixels / pixels) else 1.0
        return "${(width * scale).roundToInt()}*${(height * scale).roundToInt()}"
    }

    // ---------------- 图层操作(修改前一律先记快照) ----------------

    fun addLayer(layer: MarkupLayer) {
        history.snapshot(_layers.value)
        _layers.update { it + layer }
        _selectedLayerId.value = layer.id
        onLayersChanged()
    }

    fun updateLayer(
        id: String,
        transform: (MarkupLayer) -> MarkupLayer
    ) {
        if (_layers.value.none { it.id == id }) return
        history.snapshot(_layers.value)
        _layers.update { list -> list.map { if (it.id == id) transform(it) else it } }
        onLayersChanged()
    }

    fun updateLayerTransform(
        id: String,
        transform: LayerTransform
    ) = updateLayer(id) { it.copy(transform = transform) }

    /** 画布手势拖动开始:整段手势只记一次历史快照,配合 [updateLayerTransformTransient] 使用 */
    fun beginLayerTransformChange() {
        history.snapshot(_layers.value)
        onLayersChanged()
    }

    /** 画布手势拖动中:直接改值不入历史(快照已在 [beginLayerTransformChange] 记录) */
    fun updateLayerTransformTransient(
        id: String,
        transform: LayerTransform
    ) {
        if (_layers.value.none { it.id == id }) return
        _layers.update { list ->
            list.map { if (it.id == id) it.copy(transform = transform) else it }
        }
        registerChanges()
    }

    fun removeLayer(id: String) {
        if (_layers.value.none { it.id == id }) return
        history.snapshot(_layers.value)
        _layers.update { list -> list.filterNot { it.id == id } }
        if (_selectedLayerId.value == id) _selectedLayerId.value = null
        onLayersChanged()
    }

    fun duplicateLayer(id: String) {
        val source = _layers.value.find { it.id == id } ?: return
        history.snapshot(_layers.value)
        val copy = source.copy(id = java.util.UUID.randomUUID().toString())
        _layers.update { list ->
            list.toMutableList().apply { add(list.indexOf(source) + 1, copy) }
        }
        _selectedLayerId.value = copy.id
        onLayersChanged()
    }

    fun toggleLayerVisible(id: String) = updateLayer(id) {
        it.copy(transform = it.transform.copy(visible = !it.transform.visible))
    }

    fun toggleLayerLocked(id: String) = updateLayer(id) {
        it.copy(transform = it.transform.copy(locked = !it.transform.locked))
    }

    fun setLayerAlpha(
        id: String,
        alpha: Float
    ) = updateLayer(id) {
        it.copy(transform = it.transform.copy(alpha = alpha.coerceIn(0f, 1f)))
    }

    /** 透明度拖动开始:整段拖动只记一次历史快照,配合 [updateLayerAlphaTransient] 使用 */
    fun beginLayerAlphaChange() {
        history.snapshot(_layers.value)
        onLayersChanged()
    }

    /** 透明度拖动中:直接改值不入历史(快照已在 [beginLayerAlphaChange] 记录) */
    fun updateLayerAlphaTransient(
        id: String,
        alpha: Float
    ) {
        if (_layers.value.none { it.id == id }) return
        _layers.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(transform = it.transform.copy(alpha = alpha.coerceIn(0f, 1f)))
                } else it
            }
        }
        registerChanges()
    }

    /** 文字层框尺寸拖动手势中间帧:直接改 widthRatio 不入历史(快照已在 [beginLayerTransformChange] 记录) */
    fun updateTextWidthRatioTransient(
        id: String,
        widthRatio: Float
    ) {
        if (_layers.value.none { it.id == id }) return
        _layers.update { list ->
            list.map { layer ->
                if (layer.id == id && layer.type is LayerType.Text) {
                    layer.copy(
                        type = layer.type.copy(widthRatio = widthRatio.coerceIn(0.05f, 1f))
                    )
                } else layer
            }
        }
        registerChanges()
    }

    /** 形状图层参数更新(形状种类/样式/颜色等离散修改),正常入历史 */
    fun updateShapeSpec(
        id: String,
        spec: ShapeSpec
    ) = updateLayer(id) { layer ->
        (layer.type as? LayerType.Shape)
            ?.let { layer.copy(type = it.copy(spec = spec)) }
            ?: layer
    }

    /** 形状参数拖动开始:整段拖动只记一次历史快照,配合 [updateShapeSpecTransient] 使用 */
    fun beginShapeSpecChange() {
        history.snapshot(_layers.value)
        onLayersChanged()
    }

    /** 形状参数拖动中:直接改值不入历史(快照已在 [beginShapeSpecChange] 记录) */
    fun updateShapeSpecTransient(
        id: String,
        spec: ShapeSpec
    ) {
        if (_layers.value.none { it.id == id }) return
        _layers.update { list ->
            list.map { layer ->
                if (layer.id == id && layer.type is LayerType.Shape) {
                    layer.copy(type = LayerType.Shape(spec))
                } else layer
            }
        }
        registerChanges()
    }

    /** 混合模式,本期仅 [LayerBlendMode.Normal],先留接口 */
    fun setLayerBlendMode(
        id: String,
        blendMode: LayerBlendMode
    ) = updateLayer(id) {
        it.copy(transform = it.transform.copy(blendMode = blendMode))
    }

    /** z 序上移一位(列表尾部是最顶层) */
    fun moveLayerUp(id: String) = moveLayer(id, delta = 1)

    /** z 序下移一位 */
    fun moveLayerDown(id: String) = moveLayer(id, delta = -1)

    private fun moveLayer(
        id: String,
        delta: Int
    ) {
        val list = _layers.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = index + delta
        if (target !in list.indices) return
        history.snapshot(list)
        _layers.update { current ->
            current.toMutableList().apply { add(target, removeAt(index)) }
        }
        onLayersChanged()
    }

    /**
     * 向下合并:把该图层与其下方最近一个可见图层按原图尺寸烘焙成一张透明底
     * PNG,缓存后以图片图层替换原两个图层。烘焙图像素已含位置信息,故新图层
     * transform 保持居中;scale 取 1/[IMAGE_LAYER_BASE_WIDTH_RATIO] 抵消图片图层
     * 的基础宽度比例,使烘焙图恰好铺满画布。下方没有可合并图层时 toast 提示。
     * 各图层自身的滤镜/调节随合并烘焙进结果位图,合并后图层不再携带。
     */
    fun mergeLayerDown(id: String) {
        val list = _layers.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val layer = list[index]
        if (layer.transform.locked) return
        val lowerLayer = (index - 1 downTo 0)
            .map { list[it] }
            .firstOrNull { it.transform.visible }
        if (lowerLayer == null) {
            AppToastHost.showToast(R.string.markup_merge_no_layer_below)
            return
        }
        val size = _sourceSize.value
            ?: _bitmap.value?.let { IntSize(it.width, it.height) }
        if (size == null || size.width <= 0 || size.height <= 0) return

        mergeJob = componentScope.launch {
            _isSaving.value = true
            val cachedUri = runCatching {
                val base = Bitmap.createBitmap(
                    size.width,
                    size.height,
                    Bitmap.Config.ARGB_8888
                )
                // 走同一图层渲染路径:各图层自身的滤镜/调节一并烘焙进合并结果
                val merging = listOf(lowerLayer, layer)
                val baked = markupLayersApplier.applyToImage(
                    image = base,
                    layers = merging,
                    filteredLayers = filteredBitmapsFor(merging)
                )
                shareProvider.cacheImage(
                    image = baked,
                    imageInfo = ImageInfo(
                        width = baked.width,
                        height = baked.height,
                        imageFormat = ImageFormat.Png.Lossless
                    )
                )
            }.getOrNull()
            if (cachedUri == null) {
                AppToastHost.showFailureToast(R.string.markup_merge_failed)
                _isSaving.value = false
                return@launch
            }
            history.snapshot(_layers.value)
            val mergedLayer = MarkupLayer(
                type = LayerType.Image(imageData = cachedUri.toUri()),
                transform = LayerTransform(scale = 1f / IMAGE_LAYER_BASE_WIDTH_RATIO)
            )
            _layers.update { current ->
                val upperIndex = current.indexOfFirst { it.id == id }
                val lowerIndex = current.indexOfFirst { it.id == lowerLayer.id }
                if (upperIndex <= lowerIndex) {
                    current
                } else {
                    current.toMutableList().apply {
                        removeAt(upperIndex)
                        removeAt(lowerIndex)
                        add(lowerIndex, mergedLayer)
                    }
                }
            }
            _selectedLayerId.value = mergedLayer.id
            onLayersChanged()
            _isSaving.value = false
        }
    }

    private var rasterizeJob: Job? by smartJob {
        _isSaving.update { false }
    }

    /**
     * 光栅化图层:把该图层(文字/形状等矢量图层)按底图全分辨率画到等底图尺寸的
     * 透明位图(带其位置/缩放/旋转;alpha 不烘焙、由新图层 transform 沿用,避免双重
     * 生效;调节保留在图层字段上不烘焙),缓存后以图片图层原位替换,替换前记一次
     * 历史快照(可撤销)。与 [mergeLayerDown] 同理,scale 取
     * 1/[IMAGE_LAYER_BASE_WIDTH_RATIO] 抵消图片图层基础宽度比例,使烘焙图铺满画布。
     * 图层 id 保持不变,选中态/编辑会话引用不受影响。
     * 完成后回调 [onRasterized](如滤镜流程:光栅化后再应用滤镜);失败 toast 提示。
     */
    fun rasterizeLayer(
        layerId: String,
        onRasterized: () -> Unit = {}
    ) {
        val layer = _layers.value.find { it.id == layerId } ?: return
        // 位图类图层无需光栅化(该入口面向文字/形状),涂鸦虽可光栅化但无必要
        if (layer.type.isBitmapFilterable) return
        if (layer.transform.locked) return
        val size = _sourceSize.value
            ?: _bitmap.value?.let { IntSize(it.width, it.height) }
        if (size == null || size.width <= 0 || size.height <= 0) return

        rasterizeJob = componentScope.launch {
            _isSaving.value = true
            val cachedUri = runCatching {
                val base = Bitmap.createBitmap(
                    size.width,
                    size.height,
                    Bitmap.Config.ARGB_8888
                )
                // alpha 不烘焙(新图层 transform 沿用);调节不烘焙(保留在图层字段,
                // 光栅化后仍可实时调节),避免双重生效
                val bakeLayer = layer.copy(
                    transform = layer.transform.copy(alpha = 1f),
                    adjustments = BaseAdjustments()
                )
                val baked = markupLayersApplier.applyToImage(
                    image = base,
                    layers = listOf(bakeLayer)
                )
                shareProvider.cacheImage(
                    image = baked,
                    imageInfo = ImageInfo(
                        width = baked.width,
                        height = baked.height,
                        imageFormat = ImageFormat.Png.Lossless
                    )
                )
            }.getOrNull()
            if (cachedUri == null) {
                AppToastHost.showFailureToast(R.string.markup_rasterize_failed)
                _isSaving.value = false
                return@launch
            }
            history.snapshot(_layers.value)
            val imageLayer = layer.copy(
                type = LayerType.Image(imageData = cachedUri.toUri()),
                transform = layer.transform.copy(
                    centerX = 0.5f,
                    centerY = 0.5f,
                    scale = 1f / IMAGE_LAYER_BASE_WIDTH_RATIO,
                    rotation = 0f
                )
            )
            _layers.update { list ->
                list.map { if (it.id == layerId) imageLayer else it }
            }
            onLayersChanged()
            _isSaving.value = false
            onRasterized()
        }
    }

    fun reorderLayers(layers: List<MarkupLayer>) {
        if (layers == _layers.value) return
        history.snapshot(_layers.value)
        _layers.value = layers
        onLayersChanged()
    }

    fun clearLayers() {
        if (_layers.value.isEmpty()) return
        history.snapshot(_layers.value)
        _layers.value = emptyList()
        _selectedLayerId.value = null
        onLayersChanged()
    }

    fun selectLayer(id: String?) {
        if (_selectedLayerId.value == id) return
        _selectedLayerId.value = id
    }

    fun setActiveTool(id: String?) {
        _activeToolId.value = id
    }

    // ---------------- 图层编辑会话(文字工具页:整个会话 = 一次历史粒度) ----------------

    /**
     * 编辑会话快照。[preEditLayers] 为进入会话时的完整图层列表;
     * [createdLayerId] 非空表示「新建图层再编辑」流程(快照中已含该新图层)
     */
    private class LayerEditSession(
        val preEditLayers: List<MarkupLayer>,
        val createdLayerId: String?,
    )

    private var editSession: LayerEditSession? = null

    private val _editSessionLayerId: MutableState<String?> = mutableStateOf(null)

    /** 正在编辑会话中编辑的图层 id,会话外为 null */
    val editSessionLayerId: String? by _editSessionLayerId

    /** 当前会话是否为「新建文字图层」流程 */
    val isEditSessionNewLayer: Boolean get() = editSession?.createdLayerId != null

    /**
     * 开始文字图层编辑会话。[layerId] 为空时先经 [addLayer] 新建默认空文字图层
     * (自带一次历史快照)再记会话;非空时直接编辑该已有文字图层。
     * 会话期间的一切修改都应走 [updateLayerInEditSession] 等 transient 接口。
     */
    fun beginTextEditSession(layerId: String? = null) {
        val targetId: String
        if (layerId == null) {
            val layer = MarkupLayer(type = LayerType.Text.Default.copy(text = ""))
            addLayer(layer)
            targetId = layer.id
        } else {
            val layer = _layers.value.find { it.id == layerId }
            if (layer == null || layer.type !is LayerType.Text) return
            targetId = layerId
        }
        editSession = LayerEditSession(
            preEditLayers = _layers.value,
            createdLayerId = targetId.takeIf { layerId == null }
        )
        _editSessionLayerId.value = targetId
        _selectedLayerId.value = targetId
    }

    /** 会话内图层修改:直接改值不入历史,确认/取消时统一结算 */
    fun updateLayerInEditSession(
        id: String,
        transform: (MarkupLayer) -> MarkupLayer
    ) {
        if (_layers.value.none { it.id == id }) return
        _layers.update { list -> list.map { if (it.id == id) transform(it) else it } }
        registerChanges()
    }

    /** 会话内删除图层(不入历史):随后接 [commitLayerEditSession],undo 一次即可恢复 */
    fun removeLayerInEditSession(id: String) {
        if (_layers.value.none { it.id == id }) return
        _layers.update { list -> list.filterNot { it.id == id } }
        registerChanges()
    }

    /** 会话内复制图层(不入历史):副本插入原图层之上,编辑对象切换到副本 */
    fun duplicateLayerInEditSession(id: String) {
        val source = _layers.value.find { it.id == id } ?: return
        val copy = source.copy(id = java.util.UUID.randomUUID().toString())
        _layers.update { list ->
            list.toMutableList().apply { add(list.indexOf(source) + 1, copy) }
        }
        _editSessionLayerId.value = copy.id
        _selectedLayerId.value = copy.id
        registerChanges()
    }

    /**
     * 确认会话:整个会话的变更压成一次历史记录。
     * 新建图层文本为空(或已在会话内删除)时等同放弃:恢复会话前状态并
     * 移除该新图层,不产生新历史。
     */
    fun commitLayerEditSession() {
        val session = editSession ?: return
        val createdId = session.createdLayerId
        if (createdId != null) {
            val createdText = (_layers.value.find { it.id == createdId }
                ?.type as? LayerType.Text)?.text
            if (createdText.isNullOrBlank()) {
                _layers.value = session.preEditLayers.filterNot { it.id == createdId }
                endLayerEditSession()
                pruneSelection()
                onLayersChanged()
                return
            }
        }
        if (_layers.value != session.preEditLayers) {
            history.snapshot(session.preEditLayers)
        }
        endLayerEditSession()
        onLayersChanged()
    }

    /** 取消会话:恢复会话前状态(新建流程连新图层一并移除),不入历史 */
    fun cancelLayerEditSession() {
        val session = editSession ?: return
        _layers.value = session.preEditLayers.filterNot { it.id == session.createdLayerId }
        endLayerEditSession()
        pruneSelection()
        onLayersChanged()
    }

    private fun endLayerEditSession() {
        editSession = null
        _editSessionLayerId.value = null
    }

    // ---------------- undo / redo ----------------

    fun undo() {
        val previous = history.undo(_layers.value) ?: return
        _layers.value = previous
        pruneSelection()
        onLayersChanged()
    }

    fun redo() {
        val next = history.redo(_layers.value) ?: return
        _layers.value = next
        pruneSelection()
        onLayersChanged()
    }

    private fun pruneSelection() {
        if (_selectedLayerId.value !in _layers.value.map { it.id }) {
            _selectedLayerId.value = null
        }
    }

    private fun onLayersChanged() {
        _canUndo.value = history.canUndo
        _canRedo.value = history.canRedo
        registerChanges()
        // 提交级图层变更(增/删/改/排序/手势结束提交/undo/redo)联动图层滤镜缓存:
        // 按「内容+滤镜」精确失效,缺失条目异步补齐
        syncLayerFilterCache()
    }

    // ---------------- 首页「最近项目」(最近打开的图片文件) ----------------

    /**
     * 最近打开/保存的图片文件,供首页「最近项目」横排展示。
     * 数据来自公共最近访问表([RecentAccessRepository]):setUri 打开图片与
     * 保存成功时各记录一次,uri 为唯一键自动去重;失效 uri 由 UI 层加载失败时剔除。
     */
    private val _recentProjects: MutableState<List<RecentAccessEntity>> = mutableStateOf(emptyList())
    val recentProjects: List<RecentAccessEntity> by _recentProjects

    private fun observeRecentProjects() {
        componentScope.launch {
            recentAccessRepository
                .observeByType(RecentAccessRepository.TYPE_FILE, limit = RECENT_OBSERVE_LIMIT)
                .collect { entries ->
                    _recentProjects.update {
                        entries.filter { entity ->
                            IMAGE_EXTENSIONS.any {
                                entity.displayName.lowercase().endsWith(".$it")
                            }
                        }.take(RECENT_PROJECTS_LIMIT)
                    }
                }
        }
    }

    /**
     * 记录一次图片文件访问(最近打开/保存产物),uri 为唯一键自动去重。
     * 相机拍摄/粘贴缓存等临时 uri 直接跳过,其余失败静默(记录只是辅助行为)。
     */
    private fun recordRecentAccess(
        uri: Uri,
        displayName: String? = null
    ) {
        if (uri.isTransientUri()) return
        componentScope.launch(ioDispatcher) {
            runCatching {
                recentAccessRepository.recordAccess(
                    uri = uri.toString(),
                    displayName = displayName?.takeIf { it.isNotBlank() }
                        ?: uri.filename().orEmpty().ifBlank { FALLBACK_IMAGE_NAME },
                    accessType = RecentAccessRepository.TYPE_FILE,
                    pathHint = uri.path
                )
            }
        }
    }

    /** 相机拍摄(app FileProvider)与粘贴缓存(cacheDir 下 file://)等临时 uri 不进最近记录 */
    private fun Uri.isTransientUri(): Boolean =
        toString().contains(appContext.fileProviderAuthority) ||
            (scheme == ContentResolver.SCHEME_FILE &&
                path?.startsWith(appContext.cacheDir.absolutePath) == true)

    // ---------------- 图片加载 ----------------

    fun setUri(
        uri: Uri,
        onFailure: (Throwable) -> Unit,
    ) {
        componentScope.launch {
            _layers.value = emptyList()
            _selectedLayerId.value = null
            _baseAdjustments.value = BaseAdjustments()
            _selectedFilter.value = null
            _filterPreviewBitmap.value = null
            _blankBaseBitmap.value = null
            layerFilterJob = null
            layerFilterCache.clear()
            history.clear()
            _canUndo.value = false
            _canRedo.value = false
            _isImageLoading.value = true

            _uri.value = uri
            imageGetter.getImageAsync(
                uri = uri.toString(),
                originalSize = true,
                onGetImage = { data ->
                    _sourceSize.value = IntSize(data.image.width, data.image.height)
                    _imageFormat.update { data.imageInfo.imageFormat }
                    _exportSettings.update {
                        it.copy(format = data.imageInfo.imageFormat.toExportFormat())
                    }
                    updateBitmap(data.image)
                    // 「最近打开」记录:仅在图片确实加载成功后记
                    recordRecentAccess(uri)
                },
                onFailure = { throwable ->
                    // 失败路径也要复位全屏 Loading,否则 LoadingDialog 永久卡住
                    _isImageLoading.value = false
                    onFailure(throwable)
                }
            )
        }
    }

    private fun updateBitmap(bitmap: Bitmap?) {
        componentScope.launch {
            _isImageLoading.value = true
            _bitmap.value = imageScaler.scaleUntilCanShow(bitmap)
            _isImageLoading.value = false
            // 底图更换后背景滤镜预览缓存失效,按当前滤镜重算
            if (_selectedFilter.value != null) updateFilterPreview()
            // 底图(尺寸)变化后图层滤镜位图分辨率失效:保守全清,按需重算
            layerFilterJob = null
            layerFilterCache.clear()
            syncLayerFilterCache()
        }
    }

    override fun resetState() {
        _bitmap.value = null
        _uri.value = null
        _blankBaseBitmap.value = null
        _sourceSize.value = null
        aiProcessJob = null
        _layers.value = emptyList()
        _selectedLayerId.value = null
        _activeToolId.value = null
        _baseAdjustments.value = BaseAdjustments()
        _selectedFilter.value = null
        _filterPreviewBitmap.value = null
        layerFilterJob = null
        layerFilterCache.clear()
        rasterizeJob = null
        _canvasBackground.value = CanvasBackground.Default
        endLayerEditSession()
        history.clear()
        _canUndo.value = false
        _canRedo.value = false
        registerChangesCleared()
    }

    // ---------------- 导出(原图重绘,不再截图) ----------------

    private suspend fun renderResultBitmap(): Bitmap? {
        val source = loadBaseBitmap() ?: return null
        // 顺序:全分辨率底图 → 背景滤镜 → 背景调节 → 逐图层合成
        // (每个图层先应用自身滤镜(预算过滤后位图)/调节(调度器 colorFilter)),
        // 与画布预览「底图(带背景滤镜/调节)+ 全量图层 live 渲染」一致
        val preparedBase = source.withSelectedFilter()
            .withBaseAdjustments(_baseAdjustments.value)
        val visibleLayers = _layers.value.filter { it.transform.visible }
        return markupLayersApplier.applyToImage(
            image = preparedBase,
            layers = visibleLayers,
            filteredLayers = filteredBitmapsFor(visibleLayers)
        )
    }

    private fun resultImageInfo(bitmap: Bitmap): ImageInfo =
        _exportSettings.value.toImageInfo(
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        )

    fun updateExportSettings(settings: ExportSettings) {
        _exportSettings.value = settings
    }

    private fun onRenderFailed() {
        AppToastHost.showFailureToast(R.string.markup_export_failed_no_image)
    }

    fun saveBitmap(
        oneTimeSaveLocationUri: String?,
        onComplete: (saveResult: SaveResult) -> Unit,
    ) {
        // 快速连点防抖:保存/分享/复制共用 savingJob,进行中直接忽略新请求
        if (_isSaving.value) return
        savingJob = componentScope.launch {
            _isSaving.value = true
            val rendered = renderResultBitmap()
            if (rendered == null) {
                onRenderFailed()
                _isSaving.value = false
                return@launch
            }
            val imageInfo = resultImageInfo(rendered)
            val saveResult = fileController.save(
                saveTarget = ImageSaveTarget(
                    imageInfo = imageInfo,
                    originalUri = _uri.value?.toString().orEmpty(),
                    sequenceNumber = null,
                    data = imageCompressor.compressAndTransform(
                        image = rendered,
                        imageInfo = imageInfo
                    )
                ),
                keepOriginalMetadata = _saveExif.value,
                oneTimeSaveLocationUri = oneTimeSaveLocationUri
            ).onSuccess(::registerSave)
            onComplete(saveResult)
            if (saveResult is SaveResult.Success) {
                // 保存产物记入「最近项目」(覆盖原图时 fileUri 即原 uri,等于刷新其时间戳)
                saveResult.fileUri?.let { recordRecentAccess(it.toUri(), saveResult.fileName) }
                if (_exportSettings.value.shareAfterSave) {
                    shareProvider.shareImage(
                        image = rendered,
                        imageInfo = imageInfo,
                        onComplete = {}
                    )
                }
            }
            _isSaving.value = false
        }
    }

    fun shareBitmap(onComplete: () -> Unit) {
        if (_isSaving.value) return
        savingJob = componentScope.launch {
            _isSaving.value = true
            val rendered = renderResultBitmap()
            if (rendered == null) {
                onRenderFailed()
                _isSaving.value = false
                return@launch
            }
            shareProvider.shareImage(
                image = rendered,
                imageInfo = resultImageInfo(rendered),
                onComplete = onComplete
            )
            _isSaving.value = false
        }
    }

    fun cacheCurrentImage(onComplete: (Uri) -> Unit) {
        if (_isSaving.value) return
        savingJob = componentScope.launch {
            _isSaving.value = true
            val rendered = renderResultBitmap()
            if (rendered == null) {
                onRenderFailed()
                _isSaving.value = false
                return@launch
            }
            shareProvider.cacheImage(
                image = rendered,
                imageInfo = resultImageInfo(rendered)
            )?.let { uri ->
                onComplete(uri.toUri())
            }
            _isSaving.value = false
        }
    }

    fun cancelSaving() {
        savingJob?.cancel()
        savingJob = null
        _isSaving.value = false
    }

    fun setImageFormat(imageFormat: ImageFormat) {
        _imageFormat.value = imageFormat
        registerChanges()
    }

    fun setSaveExif(bool: Boolean) {
        _saveExif.value = bool
        registerChanges()
    }

    fun getFormatForFilenameSelection(): ImageFormat = imageFormat

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            initialUri: Uri?,
            onGoBack: () -> Unit,
            onNavigate: (Screen) -> Unit,
        ): MarkupLayersComponent
    }

}

/** 空白画布单边像素上限,防止自定义尺寸输入过大导致 OOM */
private const val MAX_CANVAS_SIDE = 8192

/** 首页「最近项目」展示条数上限 */
private const val RECENT_PROJECTS_LIMIT = 12

/** 观察最近访问表的拉取条数(过滤图片扩展名后再截取展示上限) */
private const val RECENT_OBSERVE_LIMIT = 50

/** 「最近项目」纳入展示的图片扩展名(小写比较) */
private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

/** 无法从 uri 解析文件名时的兜底显示名 */
private const val FALLBACK_IMAGE_NAME = "图片"

/** 底图变换(裁剪/旋转/翻转)链路日志 tag */
private const val LOG_TAG = "MarkupBaseTransform"

/** AI 图像处理积分消耗/预检的来源标识 */
private const val AI_POINTS_SOURCE = "markup_ai"

/** AI 生成贴纸:提示词追加的贴纸风格描述(文生图模型对英文风格词更稳定) */
private const val STICKER_PROMPT_STYLE_SUFFIX = "sticker style, clean simple solid background"

/** AI 生成贴纸输出尺寸(正方形,服务商像素限制内的固定值) */
private const val STICKER_OUTPUT_SIZE = "1024*1024"
