package com.lockin.domain.appcatalog

import com.lockin.data.entities.ApplicationEntity

sealed interface AppEligibility {
    data object Lockable : AppEligibility
    data class NotLockable(val reason: String) : AppEligibility
}

object AppEligibilityRules {
    fun evaluate(application: ApplicationEntity): AppEligibility =
        when {
            !application.isInstalled -> AppEligibility.NotLockable("App is not installed.")
            application.isLockinApp -> AppEligibility.NotLockable("Lockin cannot lock itself.")
            application.isPolicyExempt -> AppEligibility.NotLockable("App is required by device policy.")
            else -> AppEligibility.Lockable
        }

    fun requireLockable(application: ApplicationEntity) {
        val eligibility = evaluate(application)
        require(eligibility is AppEligibility.Lockable) {
            (eligibility as AppEligibility.NotLockable).reason
        }
    }
}
