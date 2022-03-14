package com.mqv.vmess

import android.content.Context
import com.mqv.vmess.util.DateTimeHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class DateTimeValidator {
    private val VIETNAMESE_LOCALE = Locale("vi", "VN")
    private val ENSLISH_LOCALE = Locale("en", "US")

    private val vietnameseAt = "lúc"
    private val englishAt = "at"

    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        `when`(mockContext.getString(R.string.msg_at_date_time)).thenReturn("at")
    }

    @Test
    fun whenInDayDateTimeValid_English() {
        val intervalHours = 10L
        val dateTime = LocalDateTime.now().minusHours(intervalHours)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, ENSLISH_LOCALE)
        val expectedHour = if (dateTime.hour < 10) "0${dateTime.hour}" else "${dateTime.hour}"
        val expectedMinute = if (dateTime.minute < 10) "0${dateTime.minute}" else "${dateTime.minute}"

        assertEquals("Date Time validation", result, "$expectedHour:$expectedMinute")
    }

    @Test
    fun whenInWeekDateTimeValid_English() {
        val dateTime = LocalDateTime.of(2022, 2, 1, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, ENSLISH_LOCALE)

        assertEquals("Date Time validation", result, "Tue $englishAt 23:10")
    }

    @Test
    fun whenInMonthDateTimeValid_English() {
        val dateTime = LocalDateTime.of(2022, 1, 30, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, ENSLISH_LOCALE)

        assertEquals("Date Time validation", result, "Jan 30 $englishAt 23:10")
    }

    @Test
    fun whenInYearDateTimeValid_English() {
        val dateTime = LocalDateTime.of(2020, 1, 7, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, ENSLISH_LOCALE)

        assertEquals("Date Time validation", result, "Jan 07, 2020 $englishAt 23:10")
    }

    @Test
    fun whenInDayDateTimeValid_Vietnamese() {
        val intervalHours = 10L
        val dateTime = LocalDateTime.now().minusHours(intervalHours)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime)
        val expectedHour = if (dateTime.hour < 10) "0${dateTime.hour}" else "${dateTime.hour}"
        val expectedMinute = if (dateTime.minute < 10) "0${dateTime.minute}" else "${dateTime.minute}"

        assertEquals("Date Time validation", result, "$expectedHour:$expectedMinute")
    }

    @Test
    fun whenInWeekDateTimeValid_Vietnamese() {
        val dateTime = LocalDateTime.of(2022, 2, 1, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, VIETNAMESE_LOCALE)
        val formatted = result.replace(englishAt, vietnameseAt)

        assertEquals("Date Time validation", formatted, "Th 3 $vietnameseAt 23:10")
    }

    @Test
    fun whenInMonthDateTimeValid_Vietnamese() {
        val dateTime = LocalDateTime.of(2022, 1, 30, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, VIETNAMESE_LOCALE)
        val formatted = result.replace(englishAt, vietnameseAt)

        assertEquals("Date Time validation", formatted, "30 thg 1 $vietnameseAt 23:10")
    }

    @Test
    fun whenInYearDateTimeValid_Vietnamese() {
        val dateTime = LocalDateTime.of(2020, 1, 7, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, VIETNAMESE_LOCALE)
        val formatted = result.replace(englishAt, vietnameseAt)

        assertEquals("Date Time validation", formatted, "07 thg 1, 2020 $vietnameseAt 23:10")
    }

    @Test
    fun conversationLastChatDateTime_Shorten_English_InDay() {
        val intervalHours = 10L
        val dateTime = LocalDateTime.now().minusHours(intervalHours)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, false, ENSLISH_LOCALE)
        val expectedHour = if (dateTime.hour < 10) "0${dateTime.hour}" else "${dateTime.hour}"
        val expectedMinute = if (dateTime.minute < 10) "0${dateTime.minute}" else "${dateTime.minute}"

        assertEquals("Date Time validation", result, "$expectedHour:$expectedMinute")
    }

    @Test
    fun conversationLastChatDateTime_Shorten_English_InWeek() {
        val dateTime = LocalDateTime.of(2022, 2, 1, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, true, ENSLISH_LOCALE)

        assertEquals("Date Time validation", result, "Tue")
    }

    @Test
    fun conversationLastChatDateTime_Shorten_English_InMonth() {
        val dateTime = LocalDateTime.of(2022, 1, 30, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, true, ENSLISH_LOCALE)

        assertEquals("Date Time validation", result, "Jan 30")
    }

    @Test
    fun conversationLastChatDateTime_Shorten_English_InYear() {
        val dateTime = LocalDateTime.of(2020, 1, 7, 23, 10)
        val result = DateTimeHelper.getMessageDateTimeFormatted(mockContext, dateTime, true, ENSLISH_LOCALE)

        assertEquals("Date Time validation", result, "Jan 07, 2020")
    }
}