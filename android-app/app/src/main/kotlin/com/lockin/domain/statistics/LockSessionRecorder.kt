package com.lockin.domain.statistics

import com.lockin.data.dao.LockWithApplications
import com.lockin.data.entities.LockSessionApplicationEntity
import com.lockin.data.entities.LockSessionEntity
import com.lockin.domain.repository.StatisticsRepository

class LockSessionRecorder(
    private val statisticsRepository: StatisticsRepository
) {
    suspend fun recordCompletedLock(
        lockWithApplications: LockWithApplications,
        completedAtWallTime: Long
    ) {
        val lock = lockWithApplications.lock
        val sessionId = statisticsRepository.insertSession(
            session = LockSessionEntity(
                lockId = lock.id,
                startedAtWallTime = lock.startedAtWallTime,
                completedAtWallTime = completedAtWallTime,
                totalCommittedDuration = (lock.committedEndWallTime - lock.startedAtWallTime)
                    .coerceAtLeast(0),
                sourceType = lock.sourceType,
                sourceId = lock.sourceId
            ),
            applications = lockWithApplications.applications.map { application ->
                LockSessionApplicationEntity(
                    sessionId = 0,
                    packageId = application.packageId
                )
            }
        )

        require(sessionId >= 0L)
    }
}
