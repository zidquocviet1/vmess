package com.mqv.vmess.util

import android.content.Context
import com.mqv.vmess.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

const val MONTH_DAY_FORMAT = "MMM dd"
const val MONTH_DAY_YEAR_FORMAT = "MMM dd, yyyy"
const val YEAR_MONTH_DAY_FORMAT = "yyyy, MMM dd"
const val DAY_MONTH_FORMAT = "dd MMM"
const val DAY_MONTH_YEAR_FORMAT = "dd MMM, yyyy"

const val TIME_PATTERN = "HH:mm"
const val WEEK_PATTERN = "EEE '%s' $TIME_PATTERN"
const val MONTH_PATTERN = "%s '%s' $TIME_PATTERN"
const val YEAR_PATTERN = "%s '%s' $TIME_PATTERN"

/*
* Helper class for handle all the datetime formatted in the app
* */
object DateTimeHelper {
    val TAG: String = DateTimeHelper::class.java.simpleName

    @JvmStatic
    @JvmOverloads
    fun getMessageDateTimeFormatted(
        context: Context,
        from: LocalDateTime,
        isShorter: Boolean = false,
        locale: Locale = Locale.getDefault()
    ): String {
        val now = LocalDateTime.now()
        val day = ChronoUnit.DAYS.between(from, now)
        val atLocalized = context.getString(R.string.msg_at_date_time)

        val pattern: String? = when {
            (day < 1 && from.dayOfMonth == now.dayOfMonth) -> {
                TIME_PATTERN
            }
            day <= 7 -> {
                WEEK_PATTERN.format(atLocalized)
            }
            (day < 365 && from.year == now.year) -> {
                formatDateTimePattern(MONTH_PATTERN, locale, atLocalized, true)
            }
            else -> {
                formatDateTimePattern(YEAR_PATTERN, locale, atLocalized, false)
            }
        }

        pattern?.let {
            var formatted = it
            if (isShorter) {
                formatted = it.replace(" '${atLocalized}' $TIME_PATTERN", "")
            }
            return from.format(DateTimeFormatter.ofPattern(formatted, locale))
        }
        return ""
    }

    // Only format the pattern of MONTH, YEAR,
    private fun formatDateTimePattern(
        pattern: String,
        locale: Locale,
        atLocalized: String,
        isMonth: Boolean
    ): String? {
        try {
            Constant.Country.findByCode(locale.country).let {
                val orderStyle = Constant.orders[it]

                when {
                    orderStyle?.contains(Constant.OrderStyle.DMY) == true -> {
                        return when (isMonth) {
                            true -> pattern.format(DAY_MONTH_FORMAT, atLocalized)
                            else -> pattern.format(DAY_MONTH_YEAR_FORMAT, atLocalized)
                        }
                    }
                    orderStyle?.contains(Constant.OrderStyle.MDY) == true -> {
                        return when (isMonth) {
                            true -> pattern.format(MONTH_DAY_FORMAT, atLocalized)
                            else -> pattern.format(MONTH_DAY_YEAR_FORMAT, atLocalized)
                        }
                    }
                    orderStyle?.contains(Constant.OrderStyle.YMD) == true -> {
                        return when (isMonth) {
                            true -> pattern.format(MONTH_DAY_FORMAT, atLocalized)
                            else -> pattern.format(YEAR_MONTH_DAY_FORMAT, atLocalized)
                        }
                    }
                    else -> {
                        return null
                    }
                }
            }
        } catch (e: IllegalArgumentException) {
            Logging.info(TAG, "${locale.country} not found")
            return ""
        }
    }

    class Constant {
        enum class Country(private val s: String) {
            VN("VN"),
            USA("US"),
            UK("GB"),
            SPAIN("ES"),
            SOUTH_KOREA("KR"),
            RUSSIA("RU"),
            CHINA("CN"),
            PHILIPPINES("PH"),
            SOUTH_AFRICA("ZA");

            companion object {
                val map = mapOf(
                    "VN" to VN,
                    "US" to USA,
                    "GB" to UK,
                    "ES" to SPAIN,
                    "KR" to SOUTH_KOREA,
                    "RU" to RUSSIA,
                    "CN" to CHINA,
                    "PH" to PHILIPPINES,
                    "ZA" to SOUTH_AFRICA
                )

                fun findByCode(code: String): Country? {
                    return map[code]
                }
            }
        }

        enum class OrderStyle {
            DMY,
            YMD,
            MDY
        }

        companion object {
            val orders = mapOf(
                Country.VN to listOf(OrderStyle.DMY),
                Country.RUSSIA to listOf(OrderStyle.DMY),
                Country.UK to listOf(OrderStyle.DMY),
                Country.SPAIN to listOf(OrderStyle.DMY),
                Country.USA to listOf(OrderStyle.MDY),
                Country.SOUTH_KOREA to listOf(OrderStyle.YMD),
                Country.CHINA to listOf(OrderStyle.YMD),
                Country.PHILIPPINES to listOf(OrderStyle.DMY, OrderStyle.MDY),
                Country.SOUTH_AFRICA to listOf(OrderStyle.MDY, OrderStyle.DMY, OrderStyle.YMD),
            )
        }
    }
}