plugins {
    alias(libs.plugins.image.toolbox.library)
    alias(libs.plugins.image.toolbox.feature)
    alias(libs.plugins.image.toolbox.hilt)
    alias(libs.plugins.image.toolbox.compose)
}

android.namespace = "com.wanbaohe.adwatch"

// AdMob 激励广告仅 google 渠道可用 (同 feature/login 的 flavor 隔离范式):
//   - google 渠道:        src/main + src/google (真实 GmsRewardedAdController) + play-services-ads
//   - 国内渠道 + foss:    src/main + src/nogms (同签名 NoopRewardedAdController stub), 不携带广告 SDK
afterEvaluate {
    android.sourceSets {
        listOf("onebox", "xiaomi", "yyb", "oppo", "vivo", "huawei", "foss").forEach { flavor ->
            getByName(flavor).kotlin.srcDir("src/nogms/java")
        }
    }
}

dependencies {
    implementation(libs.androidxCore)
    implementation(libs.appCompat)

    api(projects.core.base)
    api(projects.core.model)
    api(projects.core.theme)
    api(projects.core.storage)
    api(projects.feature.common)

    // MMKV (by ABI, 每日观看计数持久化)
    "arm64Api"(libs.com.tencent.mmkv)
    "universalApi"(libs.com.tencent.mmkv)

    // AdMob 激励广告仅 google 渠道, 其余渠道(含 foss)产物不含 ads SDK
    "googleApi"(libs.play.services.ads)
}
