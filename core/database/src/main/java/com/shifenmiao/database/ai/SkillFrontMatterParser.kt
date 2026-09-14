package com.shifenmiao.database.ai

/**
 * 极简 SKILL.md frontmatter 解析器。
 *
 * 项目无 YAML 依赖，导入场景只需要元数据里的两个字段：
 * - `name`：只认单行标量（去成对首尾引号）；块标量/嵌套结构一律拒绝；
 * - `description`：支持三种写法——
 *   1. 单行标量（去成对首尾引号）；
 *   2. `>` 折叠块标量：消费后续比键行缩进更深的行，同段内换行折叠为空格、空行分段；
 *   3. `|` 字面块标量：同样的续行规则，换行原样保留；
 *   块标量头兼容 `|2` / `>-` / `|+` 等数字与 +- 修饰及尾随注释（近似处理）。
 *   其他复杂结构（嵌套 map / 列表）仍拒绝。
 * - 返回的 description 已单行化（空白折叠为单空格），注入侧另有 ≤200 字符截断。
 *
 * AppDatabase 的 bundled 技能预置、feature/ai 的 SkillRepository、
 * feature/settings 的技能管理页共用本解析器。
 */
object SkillFrontMatterParser {

    /** 块标量头：指示符 | 或 >，可带数字缩进修饰 / +- chomping 修饰 / 尾随注释 */
    private val BLOCK_SCALAR_HEADER = Regex("^([|>])[+-]?\\d*[+-]?\\s*(#.*)?$")

    /**
     * @return `name to description`（description 已单行化），解析失败返回 null
     */
    fun parse(body: String): Pair<String, String>? {
        if (!body.startsWith("---")) return null
        val end = body.indexOf("\n---", 3)
        if (end < 0) return null
        val lines = body.substring(3, end).lines()

        var name: String? = null
        var description: String? = null
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val idx = line.indexOf(':')
            // 非"键: 值"形态（续行/空行/注释）跳过；缩进行是上一键的块内容，由消费逻辑处理
            if (idx <= 0 || line.first().isWhitespace()) {
                i++
                continue
            }
            val key = line.substring(0, idx).trim()
            val rawValue = line.substring(idx + 1).trim()
            when (key) {
                "name" -> {
                    // name 只认单行标量，块标量写法直接判非法
                    if (BLOCK_SCALAR_HEADER.matches(rawValue)) return null
                    name = rawValue
                }

                "description" -> {
                    val blockMatch = BLOCK_SCALAR_HEADER.matchEntire(rawValue)
                    if (blockMatch == null) {
                        description = rawValue
                    } else {
                        val folded = blockMatch.groupValues[1] == ">"
                        val keyIndent = line.takeWhile { it == ' ' }.length
                        // 消费后续"比 frontmatter 键更深缩进"的续行（空行也算块内容）
                        val contentLines = mutableListOf<String>()
                        var j = i + 1
                        while (j < lines.size) {
                            val next = lines[j]
                            if (next.isNotBlank() && indentOf(next) <= keyIndent) break
                            contentLines.add(next)
                            j++
                        }
                        description = if (folded) foldLines(contentLines) else {
                            contentLines.joinToString("\n").trim()
                        }
                        i = j - 1
                    }
                }
            }
            i++
        }

        val cleanName = name?.unquote()
        // description 入库前单行化（块标量的换行也折叠掉）
        val cleanDescription = description?.unquote()?.singleLine()
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
        // 拼回时补回起始分隔符（substring(3, end) 把它吃掉了）
        return "---" + lines.joinToString("\n") + body.substring(end)
    }

    /** 折叠块标量：同段内换行折叠为空格，空行分段（段间保留一个换行） */
    private fun foldLines(lines: List<String>): String {
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        lines.forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) {
                if (current.isNotEmpty()) {
                    paragraphs.add(current.toString())
                    current.clear()
                }
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(line)
            }
        }
        if (current.isNotEmpty()) paragraphs.add(current.toString())
        return paragraphs.joinToString("\n")
    }

    private fun indentOf(line: String): Int = line.takeWhile { it == ' ' }.length

    /** 单行化：全部空白（含换行）折叠为单空格 */
    private fun String.singleLine(): String = replace(Regex("\\s+"), " ").trim()

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
