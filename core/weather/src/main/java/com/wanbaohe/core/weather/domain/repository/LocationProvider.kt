package com.wanbaohe.core.weather.domain.repository

import com.wanbaohe.core.weather.domain.model.GeoCoordinates

/**
 * 设备定位能力的抽象。
 *
 * 天气查询缺省城市时用它兜底取设备最近已知位置;
 * 具体系统 API(LocationManager 等)由 data 层实现注入。
 */
interface LocationProvider {

    /** 设备最近一次已知位置;未授权定位权限或系统无缓存位置时返回 null */
    fun lastKnownLocation(): GeoCoordinates?
}
