package com.visceralfit.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationFormatTest {

    @Test
    fun `clock pads seconds and omits a zero hour`() {
        assertEquals("0:05", DurationFormat.clock(5.seconds))
        assertEquals("1:00", DurationFormat.clock(1.minutes))
        assertEquals("20:00", DurationFormat.clock(20.minutes))
    }

    @Test
    fun `clock switches to hours only when needed`() {
        assertEquals("59:59", DurationFormat.clock(59.minutes + 59.seconds))
        assertEquals("1:00:00", DurationFormat.clock(1.hours))
        assertEquals("1:30:05", DurationFormat.clock(1.hours + 30.minutes + 5.seconds))
    }

    @Test
    fun `clock never renders a negative countdown`() {
        assertEquals("0:00", DurationFormat.clock((-10).seconds))
    }

    @Test
    fun `spoken form pluralises correctly`() {
        assertEquals("1 second", DurationFormat.spoken(1.seconds))
        assertEquals("30 seconds", DurationFormat.spoken(30.seconds))
        assertEquals("1 minute", DurationFormat.spoken(1.minutes))
        assertEquals("20 minutes", DurationFormat.spoken(20.minutes))
        assertEquals("1 minute 30 seconds", DurationFormat.spoken(90.seconds))
    }
}
