package com.shifenmiao.database.ai

/**
 * 极简 SKILL.md frontmatter 解析器。
 *
 * 项目无 YAML 依赖，且导入场景只需要元数据里的两个字段，因此只认
 * `name` / `description` 两个单行标量，明确不支持多行/嵌套/列表；
 * 解析失败（无 frontmatter 块或缺字段）返回 null，调用方应拒绝导入/入库。
 *
 * AppDatabase 的 bundled 技能预置与 feature/ai 的 SkillRepository 导入共用本解析器。
 */
object SkillFrontMatterParser {

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
        if (name.isNullOrBlank() || description.isNullOrBlank()) return null
        return name to description
    }
}
