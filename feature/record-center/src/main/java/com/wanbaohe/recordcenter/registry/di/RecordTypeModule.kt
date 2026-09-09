package com.wanbaohe.recordcenter.registry.di

import com.wanbaohe.recordcenter.registry.RecordTypeDefinition
import com.wanbaohe.recordcenter.registry.definitions.BloodGlucoseRecordType
import com.wanbaohe.recordcenter.registry.definitions.BloodLipidRecordType
import com.wanbaohe.recordcenter.registry.definitions.BloodOxygenRecordType
import com.wanbaohe.recordcenter.registry.definitions.BloodPressureRecordType
import com.wanbaohe.recordcenter.registry.definitions.BmiRecordType
import com.wanbaohe.recordcenter.registry.definitions.BodyFatRecordType
import com.wanbaohe.recordcenter.registry.definitions.BodyTemperatureRecordType
import com.wanbaohe.recordcenter.registry.definitions.HeartRateRecordType
import com.wanbaohe.recordcenter.registry.definitions.SleepDurationRecordType
import com.wanbaohe.recordcenter.registry.definitions.UricAcidRecordType
import com.wanbaohe.recordcenter.registry.definitions.WaistCircumferenceRecordType
import com.wanbaohe.recordcenter.registry.definitions.WeightRecordType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * 记录类型注册模块 — 通过 @IntoMap + @StringKey 把 12 种类型注册进 RecordTypeCatalog。
 */
@Module
@InstallIn(SingletonComponent::class)
object RecordTypeModule {

    @Provides
    @IntoMap
    @StringKey("heart_rate")
    fun provideHeartRateType(): RecordTypeDefinition = HeartRateRecordType

    @Provides
    @IntoMap
    @StringKey("blood_pressure")
    fun provideBloodPressureType(): RecordTypeDefinition = BloodPressureRecordType

    @Provides
    @IntoMap
    @StringKey("blood_glucose")
    fun provideBloodGlucoseType(): RecordTypeDefinition = BloodGlucoseRecordType

    @Provides
    @IntoMap
    @StringKey("weight")
    fun provideWeightType(): RecordTypeDefinition = WeightRecordType

    @Provides
    @IntoMap
    @StringKey("bmi")
    fun provideBmiType(): RecordTypeDefinition = BmiRecordType

    @Provides
    @IntoMap
    @StringKey("body_fat")
    fun provideBodyFatType(): RecordTypeDefinition = BodyFatRecordType

    @Provides
    @IntoMap
    @StringKey("uric_acid")
    fun provideUricAcidType(): RecordTypeDefinition = UricAcidRecordType

    @Provides
    @IntoMap
    @StringKey("blood_lipid")
    fun provideBloodLipidType(): RecordTypeDefinition = BloodLipidRecordType

    @Provides
    @IntoMap
    @StringKey("blood_oxygen")
    fun provideBloodOxygenType(): RecordTypeDefinition = BloodOxygenRecordType

    @Provides
    @IntoMap
    @StringKey("body_temperature")
    fun provideBodyTemperatureType(): RecordTypeDefinition = BodyTemperatureRecordType

    @Provides
    @IntoMap
    @StringKey("sleep_duration")
    fun provideSleepDurationType(): RecordTypeDefinition = SleepDurationRecordType

    @Provides
    @IntoMap
    @StringKey("waist_circumference")
    fun provideWaistCircumferenceType(): RecordTypeDefinition = WaistCircumferenceRecordType
}
