package com.wanbaohe.period.ai.tool

/** 经期工具 markdown 输出的小工具 */
internal fun buildMarkdownLink(label: String, deeplink: String): String = "[$label]($deeplink)"

internal fun sanitizeMarkdownText(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("\n", " ")
}
