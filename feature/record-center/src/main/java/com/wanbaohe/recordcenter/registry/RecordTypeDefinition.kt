package com.wanbaohe.recordcenter.registry

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 健康记录类型定义 — 每种记录(心率/血压/血糖…)一份,描述其展示信息与字段契约。
 *
 * 新增类型:在 `registry/definitions/` 加一个 object 实现本接口,
 * 再在 `registry/di/RecordTypeModule` 里 @IntoMap 注册即可,
 * UI 与 Agent 工具都经 [RecordTypeCatalog] 动态感知。
 */
interface RecordTypeDefinition {
    /** 类型唯一标识,如 "blood_pressure",落库到 HealthRecordEntity.type */
    val key: String

    /** 类型名称字符串资源 */
    @get:StringRes
    val titleRes: Int

    /** 类型描述字符串资源 */
    @get:StringRes
    val descriptionRes: Int

    /** 列表/目录页展示图标 */
    val icon: ImageVector

    /** 字段定义列表(顺序即表单与图表的展示顺序) */
    val fields: List<RecordField>

    /** 参考范围字符串资源,无参考范围的类型(如体重)为 null */
    @get:StringRes
    val referenceRangeRes: Int?

    /** 趋势图上绘制的字段 key 列表 */
    val chartFieldKeys: List<String>

    /** 目录排序权重,越小越靠前 */
    val sortOrder: Int
}

/**
 * 单个数值字段定义。
 *
 * @param key      字段 key,如 "systolic",作为 fieldsJson 的键
 * @param labelRes 字段名称字符串资源
 * @param unit     展示单位(空串表示无单位,如 BMI)
 * @param required 是否必填(血脂的 ldl/hdl 等为选填)
 * @param min/max  合理范围校验,null 表示不校验该端
 */
data class RecordField(
    val key: String,
    @StringRes val labelRes: Int,
    val unit: String,
    val required: Boolean = true,
    val min: Float? = null,
    val max: Float? = null,
)
