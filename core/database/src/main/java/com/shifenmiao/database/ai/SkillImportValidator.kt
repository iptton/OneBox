package com.shifenmiao.database.ai

/**
 * SKILL.md 导入/保存校验规则（feature/ai 的 SkillRepository 与
 * feature/settings 的技能管理页共用，保证两处导入行为一致）。
 *
 * 规则：
 * - frontmatter 必须含单行 name/description（[SkillFrontMatterParser]）；
 * - name 必须是 kebab-case slug；
 * - body 体积不得超过 [MAX_BODY_BYTES]（与 use_skill 的 maxResultLength=16KB 对齐，
 *   避免"导得进但读不全"）；
 * - 与 BUNDLED 技能同名时拒绝（防止导入覆盖预置行）。
 */
object SkillImportValidator {

    /** SKILL.md 正文体积上限（UTF-8 字节） */
    const val MAX_BODY_BYTES = 16 * 1024

    private val SLUG_REGEX = Regex("[a-z0-9]+(-[a-z0-9]+)*")

    enum class Rejection {
        EMPTY_BODY,
        INVALID_FRONTMATTER,
        INVALID_SLUG,
        BODY_TOO_LARGE,
        BUNDLED_NAME_CONFLICT,
    }

    class SkillImportException(val rejection: Rejection) : Exception(rejection.name)

    data class Validated(
        val body: String,
        val slug: String,
        val description: String,
    )

    /**
     * 校验导入/保存文本。
     *
     * @param content 原始 SKILL.md 文本
     * @param existing 同 slug 的既有行（调用方先按 slug 查）；为 BUNDLED 时拒绝
     * @return 成功为 [Validated]；失败为 [SkillImportException]，按 rejection 映射提示文案
     */
    fun validate(content: String, existing: com.shifenmiao.database.ai.entity.SkillEntity? = null): Result<Validated> {
        val body = content.trim()
        if (body.isEmpty()) return failure(Rejection.EMPTY_BODY)
        val (slug, description) = SkillFrontMatterParser.parse(body)
            ?: return failure(Rejection.INVALID_FRONTMATTER)
        if (!SLUG_REGEX.matches(slug)) return failure(Rejection.INVALID_SLUG)
        if (body.toByteArray(Charsets.UTF_8).size > MAX_BODY_BYTES) {
            return failure(Rejection.BODY_TOO_LARGE)
        }
        if (existing?.source == com.shifenmiao.database.ai.entity.SkillEntity.SOURCE_BUNDLED) {
            return failure(Rejection.BUNDLED_NAME_CONFLICT)
        }
        return Result.success(Validated(body = body, slug = slug, description = description))
    }

    private fun failure(rejection: Rejection): Result<Validated> =
        Result.failure(SkillImportException(rejection))
}
