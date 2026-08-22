package com.example.gasguard.domain.usecase

import com.example.gasguard.domain.model.DeviceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateDeviceStatusTest {

    private val calculateDeviceStatus = CalculateDeviceStatus()

    @Test
    fun `gasLevel below threshold returns SAFE`() {
        val result = calculateDeviceStatus(10.0f, 30.0)
        assertEquals(DeviceStatus.SAFE, result)
    }

    @Test
    fun `gasLevel just below threshold returns SAFE`() {
        val result = calculateDeviceStatus(29.9f, 30.0)
        assertEquals(DeviceStatus.SAFE, result)
    }

    @Test
    fun `gasLevel equal to threshold returns WARNING`() {
        val result = calculateDeviceStatus(30.0f, 30.0)
        assertEquals(DeviceStatus.WARNING, result)
    }

    @Test
    fun `gasLevel between threshold and 1_5x threshold returns WARNING`() {
        val result = calculateDeviceStatus(40.0f, 30.0)
        assertEquals(DeviceStatus.WARNING, result)
    }

    @Test
    fun `gasLevel equal to 1_5x threshold returns DANGER`() {
        val result = calculateDeviceStatus(45.0f, 30.0)
        assertEquals(DeviceStatus.DANGER, result)
    }

    @Test
    fun `gasLevel above 1_5x threshold returns DANGER`() {
        val result = calculateDeviceStatus(80.0f, 30.0)
        assertEquals(DeviceStatus.DANGER, result)
    }

    @Test
    fun `negative gasLevel returns SENSOR_ERROR`() {
        val result = calculateDeviceStatus(-5.0f, 30.0)
        assertEquals(DeviceStatus.SENSOR_ERROR, result)
    }

    @Test
    fun `zero threshold returns SENSOR_ERROR`() {
        val result = calculateDeviceStatus(10.0f, 0.0)
        assertEquals(DeviceStatus.SENSOR_ERROR, result)
    }

    @Test
    fun `negative threshold returns SENSOR_ERROR`() {
        val result = calculateDeviceStatus(10.0f, -30.0)
        assertEquals(DeviceStatus.SENSOR_ERROR, result)
    }
}
