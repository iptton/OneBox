package com.wanbaohe.recordcenter.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.t8rin.imagetoolbox.core.ui.utils.helper.AppToastHost
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedModalBottomSheet
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassFilterChip
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassOutlinedTextField
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassTonalButton
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.data.HealthProfile

/**
 * 基础信息编辑弹层:性别 + 年龄/身高/体重(全部可选)。
 * imePadding + 导航栏 padding 适配;非法输入 toast 拦截,不保存。
 */
@Composable
fun HealthProfileSheet(
    visible: Boolean,
    initial: HealthProfile,
    onSave: (HealthProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var gender by remember { mutableStateOf(initial.gender) }
    var age by remember { mutableStateOf(initial.age?.toString().orEmpty()) }
    var height by remember { mutableStateOf(initial.heightCm?.let(::formatProfileFloat).orEmpty()) }
    var weight by remember { mutableStateOf(initial.weightKg?.let(::formatProfileFloat).orEmpty()) }
    val invalidText = stringResource(R.string.record_center_profile_invalid)

    EnhancedModalBottomSheet(
        visible = true,
        dragHandle = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = stringResource(R.string.record_center_profile_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        },
        onDismiss = { onDismiss() },
        sheetContent = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.record_center_profile_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.record_center_profile_field_gender),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    GlassFilterChip(
                        selected = gender == HealthProfile.Gender.MALE,
                        onClick = {
                            gender = if (gender == HealthProfile.Gender.MALE) {
                                HealthProfile.Gender.UNSET
                            } else HealthProfile.Gender.MALE
                        },
                        label = { Text(text = stringResource(R.string.record_center_profile_gender_male)) },
                    )
                    GlassFilterChip(
                        selected = gender == HealthProfile.Gender.FEMALE,
                        onClick = {
                            gender = if (gender == HealthProfile.Gender.FEMALE) {
                                HealthProfile.Gender.UNSET
                            } else HealthProfile.Gender.FEMALE
                        },
                        label = { Text(text = stringResource(R.string.record_center_profile_gender_female)) },
                    )
                }
                GlassOutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.record_center_profile_field_age)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                GlassOutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.record_center_profile_field_height)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                GlassOutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.record_center_profile_field_weight)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                GlassTonalButton(
                    onClick = {
                        val parsedAge = age.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                        val parsedHeight = height.trim().takeIf { it.isNotEmpty() }?.toFloatOrNull()
                        val parsedWeight = weight.trim().takeIf { it.isNotEmpty() }?.toFloatOrNull()
                        val valid = (age.isBlank() || parsedAge in 1..150) &&
                            (height.isBlank() || (parsedHeight != null && parsedHeight in 50f..300f)) &&
                            (weight.isBlank() || (parsedWeight != null && parsedWeight in 2f..500f))
                        if (!valid) {
                            AppToastHost.showToast(invalidText)
                            return@GlassTonalButton
                        }
                        onSave(
                            HealthProfile(
                                gender = gender,
                                age = parsedAge,
                                heightCm = parsedHeight,
                                weightKg = parsedWeight,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.record_center_action_save))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
    )
}

/** 展示用浮点格式化:整数去小数点(175.0 → "175") */
private fun formatProfileFloat(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()
