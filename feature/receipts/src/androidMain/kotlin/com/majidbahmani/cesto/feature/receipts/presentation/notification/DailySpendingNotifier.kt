package com.majidbahmani.cesto.feature.receipts.presentation.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.majidbahmani.cesto.feature.receipts.R
import com.majidbahmani.cesto.feature.receipts.domain.model.DailySpending
import com.majidbahmani.cesto.feature.receipts.presentation.mapper.formatEuros
import com.majidbahmani.cesto.feature.receipts.resources.Res
import com.majidbahmani.cesto.feature.receipts.resources.daily_spending_channel_description
import com.majidbahmani.cesto.feature.receipts.resources.daily_spending_channel_name
import com.majidbahmani.cesto.feature.receipts.resources.daily_spending_text
import com.majidbahmani.cesto.feature.receipts.resources.daily_spending_title
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * "Yesterday you spent 23,45 € · From 3 receipts". Tapping it opens the app.
 *
 * Shows nothing without the notification permission (Android 13+): MainActivity asks for it.
 */
class DailySpendingNotifier(private val context: Context) {

    suspend fun show(spending: DailySpending) {
        if (!canNotify()) return
        createChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_receipt)
            .setContentTitle(getString(Res.string.daily_spending_title, formatEuros(spending.totalCents)))
            .setContentText(getPluralString(Res.plurals.daily_spending_text, spending.receiptCount, spending.receiptCount))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()

        // Same id every day: today's summary replaces yesterday's if it's still there.
        @Suppress("MissingPermission") // Checked in canNotify().
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    // Creating an existing channel does nothing, so this is safe before every notification.
    private suspend fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(Res.string.daily_spending_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
            .apply { description = getString(Res.string.daily_spending_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // The launcher Activity of whichever app hosts this feature, so the feature doesn't need MainActivity.
    private fun openAppIntent(): PendingIntent? = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
        PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private companion object {
        const val CHANNEL_ID = "daily_spending"
        const val NOTIFICATION_ID = 1
    }
}
