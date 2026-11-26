package com.unanimetv.android.core.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val update = UpdateChecker.pollForNotification(applicationContext) ?: return Result.success()
        UpdateNotifier.notify(applicationContext, update)
        return Result.success()
    }
}
