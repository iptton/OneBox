package com.shifenmiao.database.ai

/**
 * 极简 SKILL.md frontmatter 解析器。
 *
 * 项目无 YAML 依赖，且导入场景只需要元数据里的两个字段，因此只认
 * `name` / `description` 两个单行标量：
 * - 单行值的成对首尾引号（"..." / '...'）会被去除后再校验；
 * - 块标量写法（`description: |`、`>`、`|-`、`>-` 等多行形式）明确不支持，解析失败；
 * - 无 frontmatter 块或缺字段同样失败，返回 null，调用方应拒绝导入/入库。
 *
 * AppDatabase 的 bundled 技能预置、feature/ai 的 SkillRepository、
 * feature/settings 的技能管理页共用本解析器。
 */
object SkillFrontMatterParser {

    /** YAML 块标量标记（多行值），本解析器明确不支持 */
    private val BLOCK_SCALAR_MARKERS = setOf("|", ">", "|-", ">-", "|+", ">+")

    /**
     * @return `name to description`，解析失败返回 null
     */
    fun parse(body: String): Pair<String, String>? {
        if (!body.startsWith("---")) return null
        val end = body.indexOf("\n---", 3)
        if (end < 0) return null
        var name: String? = null
        var description: String? = null
        body.substring(3, end).lineSequence().forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                when (key) {
                    "name" -> name = value
                    "description" -> description = value
                }
            }
        }
        // 块标量（多行）明确拒绝，避免把字面量 | 当描述导入
        if (name in BLOCK_SCALAR_MARKERS || description in BLOCK_SCALAR_MARKERS) return null
        val cleanName = name?.unquote()
        val cleanDescription = description?.unquote()
        if (cleanName.isNullOrBlank() || cleanDescription.isNullOrBlank()) return null
        return cleanName to cleanDescription
    }

    /**
     * 改写 frontmatter 里的 name 行（另存副本后同步副本正文的 name，
     * 保证"导出副本再导入"命中副本）。找不到 frontmatter/name 行时原样返回。
     */
    fun rewriteName(body: String, newName: String): String {
        if (!body.startsWith("---")) return body
        val end = body.indexOf("\n---", 3)
        if (end < 0) return body
        val header = body.substring(3, end)
        val lines = header.lines().toMutableList()
        val nameIndex = lines.indexOfFirst { it.trimStart().startsWith("name:") }
        if (nameIndex < 0) return body
        lines[nameIndex] = "name: $newName"
        return lines.joinToString("\n") + body.substring(end)
    }

    /** 去除成对的首尾引号（"..." / '...'）；不成对或非引号包裹时原样返回 */
    private fun String.unquote(): String {
        if (length >= 2) {
            val first = first()
            val last = last()
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return substring(1, length - 1).trim()
            }
        }
        return this
    }
}
