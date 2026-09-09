package com.wanbaohe.core.weather.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.wanbaohe.core.weather.domain.model.GeoCoordinates
import com.wanbaohe.core.weather.domain.repository.LocationProvider

/**
 * 基于系统 LocationManager 的最近已知位置实现。
 *
 * 只读系统缓存的 lastKnownLocation,不主动发起定位请求,
 * 避免在工具调用链路上引入长时间阻塞。
 */
class AndroidLocationProvider(
    private val context: Context,
) : LocationProvider {

    override fun lastKnownLocation(): GeoCoordinates? {
        if (!hasLocationPermission()) return null
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val location = runCatching {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull() ?: return null
        return GeoCoordinates(latitude = location.latitude, longitude = location.longitude)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
