package com.bw.vrtnumm.androidApp.utils

import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.bw.vrtnumm.shared.utils.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

fun NotificationCompat.Builder.setImageUrl(imageUrl: String) = runBlocking {
    val url = URL(imageUrl)

    withContext(Dispatchers.IO) {
        try {
            val input = url.openStream()
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            DebugLog.e("failed fetching image for $url", e)
            null
        }
    }?.let { bitmap ->
        setLargeIcon(bitmap)
        .setStyle(
            NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null))
    }
}