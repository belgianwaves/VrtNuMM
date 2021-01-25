package com.bw.vrtnumm.androidApp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.bw.vrtnumm.androidApp.utils.sanitizedUrl
import com.bw.vrtnumm.androidApp.utils.setImageUrl
import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class CheckEpisodes(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        var id = 0
        withContext(Dispatchers.IO) {
            val repo = Repository()
                repo.init(Api("", ""))

            val programs = repo.getFavProgramsWithNewEpisodes()
            programs.forEach { p ->
                val intent = Intent(applicationContext, EntryActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(p.programUrl.sanitizedUrl())
                }

                val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                val notificationBuilder = NotificationCompat.Builder(applicationContext, "messages")
                    .setSmallIcon(R.drawable.exo_icon_play)
                    .setContentTitle(p.title)
                    .setContentText("New episodes are available")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setImageUrl(p.thumbnail.sanitizedUrl())

                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(id++, notificationBuilder?.build())
            }
        }

        scheduleDailyRequest(applicationContext)

        return Result.success()
    }

    companion object {
        fun scheduleDailyRequest(applicationContext: Context) {
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance()
                dueDate.set(Calendar.HOUR_OF_DAY, 7)
                dueDate.set(Calendar.MINUTE, 0)
                dueDate.set(Calendar.SECOND, 0)
                if (dueDate.before(currentDate)) {
                    dueDate.add(Calendar.HOUR_OF_DAY, 24)
                }
            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
            val dailyWorkRequest = OneTimeWorkRequestBuilder<CheckEpisodes>()
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "check_episodes",
                ExistingWorkPolicy.REPLACE,
                dailyWorkRequest
            )
        }
    }
}