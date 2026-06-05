package com.lockin.device

data class DeviceOwnerStatus(
    val isDeviceOwner: Boolean,
    val packageName: String
)

interface DeviceOwnerState {
    fun currentStatus(): DeviceOwnerStatus
}
