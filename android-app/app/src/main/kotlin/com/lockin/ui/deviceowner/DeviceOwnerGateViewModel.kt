package com.lockin.ui.deviceowner

import androidx.lifecycle.ViewModel
import com.lockin.device.DeviceOwnerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceOwnerGateUiState(
    val isDeviceOwner: Boolean = false,
    val packageName: String = "",
    val verificationCount: Int = 0,
    val lastVerificationMessage: String? = null,
    val guidance: String = DEVICE_OWNER_GUIDANCE
) {
    companion object {
        const val DEVICE_OWNER_GUIDANCE =
            "Lockin requires Android Device Owner status before locks can be created or viewed."
    }
}

class DeviceOwnerGateViewModel(
    private val deviceOwnerState: DeviceOwnerState
) : ViewModel() {
    private val _uiState = MutableStateFlow(readStatus(verificationCount = 0, lastMessage = null))
    val uiState: StateFlow<DeviceOwnerGateUiState> = _uiState.asStateFlow()

    fun verifyDeviceOwnerStatus() {
        val nextCount = _uiState.value.verificationCount + 1
        val nextState = readStatus(
            verificationCount = nextCount,
            lastMessage = _uiState.value.lastVerificationMessage
        )
        _uiState.value = nextState.copy(
            lastVerificationMessage = if (nextState.isDeviceOwner) {
                "Device Owner verified."
            } else {
                "Device Owner is still not active."
            }
        )
    }

    private fun readStatus(
        verificationCount: Int,
        lastMessage: String?
    ): DeviceOwnerGateUiState {
        val status = deviceOwnerState.currentStatus()
        return DeviceOwnerGateUiState(
            isDeviceOwner = status.isDeviceOwner,
            packageName = status.packageName,
            verificationCount = verificationCount,
            lastVerificationMessage = lastMessage
        )
    }
}
