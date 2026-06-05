package com.lockin.device

import android.os.UserManager

class TimeRestrictionPolicy(
    private val devicePolicyGateway: DevicePolicyGateway
) {
    fun reconcile(activeLocksExist: Boolean): DevicePolicyResult =
        if (activeLocksExist) {
            devicePolicyGateway.addUserRestriction(UserManager.DISALLOW_CONFIG_DATE_TIME)
        } else {
            devicePolicyGateway.clearUserRestriction(UserManager.DISALLOW_CONFIG_DATE_TIME)
        }
}
