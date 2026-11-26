package com.unanimetv.android.core.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!UpdateNotifier.isEnabled(applicationContext) || !UpdateNotifier.canNotify(applicationContext)) {
            return Result.success()
        }
        return when (val outcome = UpdateChecker.pollForNotification(applicationContext)) {
            is UpdateChecker.Outcome.Available -> {
                UpdateNotifier.notify(applicationContext, outcome.update)
                Result.success()
            }
            UpdateChecker.Outcome.Unreachable -> Result.retry()
            UpdateChecker.Outcome.None -> Result.success()
        }
    }
}
