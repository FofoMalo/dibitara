package com.dibitara.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dibitara.app.data.backup.BackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backups: BackupManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!backups.state.value.automatic) return Result.success()
        return try {
            backups.sauvegarder()
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
    companion object { const val NAME = "daily_external_backup" }
}
