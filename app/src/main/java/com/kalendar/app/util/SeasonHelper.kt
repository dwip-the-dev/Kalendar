package com.kalendar.app.util

import com.kalendar.app.R
import java.time.LocalDate
import java.time.Month

/**
 * Maps the current date to a seasonal hero image.
 * Uses the meteorological seasons (Northern Hemisphere).
 */
object SeasonHelper {

    enum class Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    fun getSeason(date: LocalDate = LocalDate.now()): Season {
        return when (date.month) {
            Month.MARCH, Month.APRIL, Month.MAY -> Season.SPRING
            Month.JUNE, Month.JULY, Month.AUGUST -> Season.SUMMER
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> Season.AUTUMN
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> Season.WINTER
            else -> Season.SUMMER
        }
    }

    fun getHeroImageRes(date: LocalDate = LocalDate.now()): Int {
        return when (getSeason(date)) {
            Season.SPRING -> R.drawable.hero_spring
            Season.SUMMER -> R.drawable.hero_summer
            Season.AUTUMN -> R.drawable.hero_autumn
            Season.WINTER -> R.drawable.hero_winter
        }
    }
}
