package com.wanbaohe.unitconverter.domain

import androidx.annotation.StringRes
import com.wanbaohe.unitconverter.R

enum class KinshipStep(
    val route: String,
    @StringRes val labelRes: Int,
) {
    Father("father", R.string.unit_kin_step_father),
    Mother("mother", R.string.unit_kin_step_mother),
    Spouse("spouse", R.string.unit_kin_step_spouse),
    Brother("brother", R.string.unit_kin_step_brother),
    Sister("sister", R.string.unit_kin_step_sister),
    Son("son", R.string.unit_kin_step_son),
    Daughter("daughter", R.string.unit_kin_step_daughter);

    companion object {
        fun fromRoute(route: String): KinshipStep? {
            return values().firstOrNull { it.route == route }
        }
    }
}
