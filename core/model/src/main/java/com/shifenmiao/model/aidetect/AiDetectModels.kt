package com.shifenmiao.model.aidetect

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/** 朱雀 AIGC 文本检测请求(go-proxy 代理 /ai-detect/text) */
@Keep
data class AiDetectTextRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("is_merge")
    val isMerge: Boolean = true,
)

@Keep
data class AiDetectTextResponse(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("softmax_confidence")
    val softmaxConfidence: Float = 0f,
    /** 疑似 AI 占比(0-1,越高越可能是 AI) */
    @SerializedName("ratio_confidence")
    val ratioConfidence: Float = 0f,
    /** key "0"=人类、"1"=AI、"2"=疑似 AI */
    @SerializedName("labels_ratio")
    val labelsRatio: Map<String, Float> = emptyMap(),
    @SerializedName("segment_labels")
    val segmentLabels: List<AiDetectSegmentLabel> = emptyList(),
    @SerializedName("msg")
    val msg: String? = null,
)

@Keep
data class AiDetectSegmentLabel(
    @SerializedName("text")
    val text: String = "",
    /** 0=人类、1=AI、2=疑似 AI */
    @SerializedName("label")
    val label: Int = 0,
    @SerializedName("conf")
    val conf: Float = 0f,
    @SerializedName("order")
    val order: Int = 0,
    @SerializedName("position")
    val position: List<Int> = emptyList(),
)

/** 朱雀 AIGC 图片检测请求(go-proxy 代理 /ai-detect/image) */
@Keep
data class AiDetectImageRequest(
    @SerializedName("imageBase64")
    val imageBase64: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
)

@Keep
data class AiDetectImageResponse(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("data")
    val data: AiDetectImageData? = null,
    @SerializedName("message")
    val message: String? = null,
)

@Keep
data class AiDetectImageData(
    /** AI 生成置信度(0-1,越高越可能是 AI 生成) */
    @SerializedName("confidence")
    val confidence: Float = 0f,
)
