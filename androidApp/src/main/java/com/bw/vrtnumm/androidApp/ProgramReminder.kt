package com.bw.vrtnumm.androidApp

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.bw.vrtnumm.androidApp.utils.sanitizedUrl
import com.bw.vrtnumm.androidApp.utils.setImageUrl

class ProgramReminder: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.apply {
            showNotification(
                context = this,
                title = intent?.getStringExtra("title") ?: "",
                streamId = intent?.getStringExtra("streamId") ?: "",
                imageUrl = intent?.getStringExtra("imageUrl") ?: "")
        }
    }

    fun showNotification(context: Context, title: String, streamId: String, imageUrl: String) {
        if (listOf(title, streamId, imageUrl).any { s -> s.isBlank() }) return

        val intent = Intent(context, EntryActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("channel://${streamId}")
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationBuilder = NotificationCompat.Builder(context, "messages")
            .setSmallIcon(R.drawable.exo_icon_play)
            .setContentTitle(title)
            .setContentText("Your program is about to start")
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setImageUrl(imageUrl.sanitizedUrl())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder?.build())
    }
}