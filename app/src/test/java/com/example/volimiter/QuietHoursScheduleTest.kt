package com.example.volimiter

import com.zjdavis.volimiter.QuietHoursSchedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursScheduleTest {
    @Test
    fun overnightWindowIncludesLateNightAndEarlyMorning() {
        assertTrue(QuietHoursSchedule.isActive(23 * 60, 22 * 60, 7 * 60))
        assertTrue(QuietHoursSchedule.isActive(6 * 60 + 59, 22 * 60, 7 * 60))
        assertFalse(QuietHoursSchedule.isActive(12 * 60, 22 * 60, 7 * 60))
    }

    @Test
    fun daytimeWindowIncludesStartAndExcludesEnd() {
        assertTrue(QuietHoursSchedule.isActive(9 * 60, 9 * 60, 17 * 60))
        assertFalse(QuietHoursSchedule.isActive(17 * 60, 9 * 60, 17 * 60))
    }

    @Test
    fun matchingTimesRepresentAllDay() {
        assertTrue(QuietHoursSchedule.isActive(0, 10 * 60, 10 * 60))
        assertTrue(QuietHoursSchedule.isActive(23 * 60 + 59, 10 * 60, 10 * 60))
    }
}
