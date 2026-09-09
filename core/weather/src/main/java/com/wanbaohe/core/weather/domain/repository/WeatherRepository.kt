package com.wanbaohe.core.weather.domain.repository

import com.wanbaohe.core.weather.domain.model.DailyWeatherInfo
import com.wanbaohe.core.weather.domain.model.WeatherInfo
import com.wanbaohe.core.weather.domain.model.CityInfo

interface WeatherRepository {
    suspend fun getWeatherAtLocation(lat: Double, lon: Double): Result<WeatherInfo>
    suspend fun getCityAtLocation(lat: Double, lon: Double): Result<CityInfo>

    /** 按城市名(中文/英文/拼音或 LocationID)反查城市信息 */
    suspend fun getCityByName(name: String): Result<CityInfo>

    /** 指定城市的逐日天气预报,days 取 1-7 */
    suspend fun getDailyForecast(cityId: String, days: Int): Result<List<DailyWeatherInfo>>
}

