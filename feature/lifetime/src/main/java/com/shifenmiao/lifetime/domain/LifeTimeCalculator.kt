package com.shifenmiao.lifetime.domain

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Immutable
data class LifeTimeData(
    val years: Long = 0,
    val months: Long = 0,
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
    val totalDays: Long = 0,
    val totalHours: Long = 0,
    val totalMinutes: Long = 0,
    val totalSeconds: Long = 0
)

@Immutable
data class RemainingLifeData(
    val years: Long = 0,
    val months: Long = 0,
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
    val progress: Float = 0f
)

class LifeTimeCalculator {

    companion object {
        private const val EXPECTED_LIFESPAN_YEARS = 100

        fun calculatePastTime(birthDate: LocalDate): LifeTimeData {
            val now = LocalDateTime.now()
            val birthDateTime = birthDate.atStartOfDay()

            val years = ChronoUnit.YEARS.between(birthDateTime, now)
            val months = ChronoUnit.MONTHS.between(birthDateTime, now)
            val days = ChronoUnit.DAYS.between(birthDateTime, now)
            val hours = ChronoUnit.HOURS.between(birthDateTime, now)
            val minutes = ChronoUnit.MINUTES.between(birthDateTime, now)
            val seconds = ChronoUnit.SECONDS.between(birthDateTime, now)

            return LifeTimeData(
                years = years,
                months = months,
                days = days,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                totalDays = days,
                totalHours = hours,
                totalMinutes = minutes,
                totalSeconds = seconds
            )
        }

        fun calculateRemainingLife(
            birthDate: LocalDate,
            expectedAge: Int = EXPECTED_LIFESPAN_YEARS
        ): RemainingLifeData {
            val now = LocalDateTime.now()
            val expectedDeathDate = birthDate.plusYears(expectedAge.toLong()).atStartOfDay()

            if (now.isAfter(expectedDeathDate)) {
                return RemainingLifeData()
            }

            val years = ChronoUnit.YEARS.between(now, expectedDeathDate)
            val months = ChronoUnit.MONTHS.between(now, expectedDeathDate)
            val days = ChronoUnit.DAYS.between(now, expectedDeathDate)
            val hours = ChronoUnit.HOURS.between(now, expectedDeathDate)
            val minutes = ChronoUnit.MINUTES.between(now, expectedDeathDate)
            val seconds = ChronoUnit.SECONDS.between(now, expectedDeathDate)

            val totalLifeSeconds = ChronoUnit.SECONDS.between(
                birthDate.atStartOfDay(),
                expectedDeathDate
            )
            val livedSeconds = ChronoUnit.SECONDS.between(
                birthDate.atStartOfDay(),
                now
            )
            val progress = if (totalLifeSeconds > 0) {
                (livedSeconds.toFloat() / totalLifeSeconds.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            return RemainingLifeData(
                years = years,
                months = months,
                days = days,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                progress = progress
            )
        }
    }
}
