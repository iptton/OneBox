package com.wanbaohe.unitconverter.domain

import com.wanbaohe.unitconverter.R

object KinshipCalculator {

    fun resolve(
        gender: KinshipGender,
        steps: List<KinshipStep>,
    ): KinshipResult {
        if (steps.isEmpty()) {
            return KinshipResult(title = KinshipTitle.Empty, steps = emptyList())
        }
        val key = steps.joinToString(separator = ">") { it.route }
        val title = relationMap[key] ?: KinshipTitle.Descriptive(steps)
        return KinshipResult(title = title, steps = steps)
    }

    private val relationMap = mapOf<String, KinshipTitle>(
        "father" to KinshipTitle.Fixed(R.string.unit_kin_step_father),
        "mother" to KinshipTitle.Fixed(R.string.unit_kin_step_mother),
        "spouse" to KinshipTitle.ByGender(R.string.unit_kin_wife, R.string.unit_kin_husband),
        "brother" to KinshipTitle.Fixed(R.string.unit_kin_step_brother),
        "sister" to KinshipTitle.Fixed(R.string.unit_kin_step_sister),
        "son" to KinshipTitle.Fixed(R.string.unit_kin_step_son),
        "daughter" to KinshipTitle.Fixed(R.string.unit_kin_step_daughter),
        "father>father" to KinshipTitle.Fixed(R.string.unit_kin_paternal_grandfather),
        "father>mother" to KinshipTitle.Fixed(R.string.unit_kin_paternal_grandmother),
        "mother>father" to KinshipTitle.Fixed(R.string.unit_kin_maternal_grandfather),
        "mother>mother" to KinshipTitle.Fixed(R.string.unit_kin_maternal_grandmother),
        "father>brother" to KinshipTitle.Fixed(R.string.unit_kin_paternal_uncle),
        "father>sister" to KinshipTitle.Fixed(R.string.unit_kin_paternal_aunt),
        "mother>brother" to KinshipTitle.Fixed(R.string.unit_kin_maternal_uncle),
        "mother>sister" to KinshipTitle.Fixed(R.string.unit_kin_maternal_aunt),
        "brother>son" to KinshipTitle.Fixed(R.string.unit_kin_nephew_brother),
        "brother>daughter" to KinshipTitle.Fixed(R.string.unit_kin_niece_brother),
        "sister>son" to KinshipTitle.Fixed(R.string.unit_kin_nephew_sister),
        "sister>daughter" to KinshipTitle.Fixed(R.string.unit_kin_niece_sister),
        "son>son" to KinshipTitle.Fixed(R.string.unit_kin_grandson),
        "son>daughter" to KinshipTitle.Fixed(R.string.unit_kin_granddaughter),
        "daughter>son" to KinshipTitle.Fixed(R.string.unit_kin_maternal_grandson),
        "daughter>daughter" to KinshipTitle.Fixed(R.string.unit_kin_maternal_granddaughter),
        "spouse>father" to KinshipTitle.ByGender(
            R.string.unit_kin_father_in_law_wife_side,
            R.string.unit_kin_father_in_law_husband_side,
        ),
        "spouse>mother" to KinshipTitle.ByGender(
            R.string.unit_kin_mother_in_law_wife_side,
            R.string.unit_kin_mother_in_law_husband_side,
        ),
        "son>spouse" to KinshipTitle.Fixed(R.string.unit_kin_daughter_in_law),
        "daughter>spouse" to KinshipTitle.Fixed(R.string.unit_kin_son_in_law),
        "brother>spouse" to KinshipTitle.Fixed(R.string.unit_kin_brother_spouse),
        "sister>spouse" to KinshipTitle.Fixed(R.string.unit_kin_sister_spouse),
        "father>brother>son" to KinshipTitle.Fixed(R.string.unit_kin_paternal_male_cousin),
        "father>brother>daughter" to KinshipTitle.Fixed(R.string.unit_kin_paternal_female_cousin),
        "father>sister>son" to KinshipTitle.Fixed(R.string.unit_kin_cross_male_cousin),
        "father>sister>daughter" to KinshipTitle.Fixed(R.string.unit_kin_cross_female_cousin),
        "mother>brother>son" to KinshipTitle.Fixed(R.string.unit_kin_cross_male_cousin),
        "mother>brother>daughter" to KinshipTitle.Fixed(R.string.unit_kin_cross_female_cousin),
        "mother>sister>son" to KinshipTitle.Fixed(R.string.unit_kin_cross_male_cousin),
        "mother>sister>daughter" to KinshipTitle.Fixed(R.string.unit_kin_cross_female_cousin),
        "father>father>father" to KinshipTitle.Fixed(R.string.unit_kin_great_grandfather),
        "father>father>mother" to KinshipTitle.Fixed(R.string.unit_kin_great_grandmother),
        "mother>mother>father" to KinshipTitle.Fixed(R.string.unit_kin_maternal_great_grandfather),
        "mother>mother>mother" to KinshipTitle.Fixed(R.string.unit_kin_maternal_great_grandmother),
    )
}
