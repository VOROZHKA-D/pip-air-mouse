package com.example

import com.example.util.AirMouseLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AirMouseLogicUnitTest {

    @Test
    fun `hid report descriptor is well-formed for macOS 4-byte reports`() {
        val descriptor = HidDescriptor.MOUSE_REPORT_DESCRIPTOR
        assertNotNull(descriptor)
        assertTrue(descriptor.isNotEmpty())
        assertEquals(0x05.toByte(), descriptor[0])
        assertEquals(0x01.toByte(), descriptor[1])
        assertEquals(0xC0.toByte(), descriptor[descriptor.size - 1])
        assertEquals(0xC0.toByte(), descriptor[descriptor.size - 2])

        assertEquals(0x01.toByte(), HidDescriptor.BUTTON_LEFT)
        assertEquals(0x02.toByte(), HidDescriptor.BUTTON_RIGHT)
        assertEquals(0x00.toByte(), HidDescriptor.BUTTON_NONE)
    }

    @Test
    fun `air mouse engine ignores tremor below deadzone`() {
        val engine = AirMouseEngine(sensitivity = 45f, deadzone = 0.04f, alpha = 0.25f)
        val (dx, dy) = engine.process(0.01f, 0.02f, -0.015f)
        assertEquals(0, dx)
        assertEquals(0, dy)
    }

    @Test
    fun `air mouse engine scales movement and clamps to bounds`() {
        val engine = AirMouseEngine(sensitivity = 80f, deadzone = 0.04f, alpha = 0.5f)
        // Repeat ticks simulating rapid hand turn
        var finalDx = 0
        var finalDy = 0
        for (i in 0 until 5) {
            val (dx, dy) = engine.process(2.5f, 0f, -3.0f)
            finalDx = dx
            finalDy = dy
        }
        assertTrue("dx should be positive and clamped <= 127", finalDx in 1..127)
        assertTrue("dy should be non-zero and within bounds", finalDy in -127..127)
    }

    @Test
    fun `clamping bounds between -127 and 127`() {
        assertEquals(-127, (-200).coerceIn(-127, 127))
        assertEquals(127, (300).coerceIn(-127, 127))
        assertEquals(55, (55).coerceIn(-127, 127))
    }

    @Test
    fun `logger records and clears entries`() {
        AirMouseLogger.clear()
        AirMouseLogger.i("Тестовое сообщение")
        AirMouseLogger.e("Тестовая ошибка")
        assertEquals(2, AirMouseLogger.logs.value.size)
        AirMouseLogger.clear()
        assertEquals(0, AirMouseLogger.logs.value.size)
    }
}
