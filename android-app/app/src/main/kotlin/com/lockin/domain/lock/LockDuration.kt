package com.lockin.domain.lock

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR
private const val MILLIS_PER_WEEK = 7L * MILLIS_PER_DAY

@JvmInline
value class LockDuration private constructor(val millis: Long) {
    init {
        require(millis > 0) { "Lock duration must be greater than zero." }
    }

    fun plus(other: LockDuration): LockDuration = fromMillis(millis + other.millis)

    companion object {
        fun fromMillis(millis: Long): LockDuration {
            require(millis > 0) { "Lock duration must be greater than zero." }
            return LockDuration(millis)
        }

        fun fromMinutes(minutes: Long): LockDuration = fromMillis(minutes * MILLIS_PER_MINUTE)

        fun fromHours(hours: Long): LockDuration = fromMillis(hours * MILLIS_PER_HOUR)

        fun fromDays(days: Long): LockDuration = fromMillis(days * MILLIS_PER_DAY)

        fun fromWeeks(weeks: Long): LockDuration = fromMillis(weeks * MILLIS_PER_WEEK)

        fun isValidMillis(millis: Long): Boolean = millis > 0
    }
}

enum class LockDurationUnit {
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    CUSTOM
}

data class LockDurationInput(
    val amount: Long,
    val unit: LockDurationUnit
) {
    fun toDuration(customMillis: Long? = null): LockDuration =
        when (unit) {
            LockDurationUnit.MINUTES -> LockDuration.fromMinutes(amount)
            LockDurationUnit.HOURS -> LockDuration.fromHours(amount)
            LockDurationUnit.DAYS -> LockDuration.fromDays(amount)
            LockDurationUnit.WEEKS -> LockDuration.fromWeeks(amount)
            LockDurationUnit.CUSTOM -> LockDuration.fromMillis(
                requireNotNull(customMillis) { "Custom duration requires milliseconds." }
            )
        }
}
