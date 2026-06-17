package com.stillfresh.handlers

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.stillfresh.R
import com.stillfresh.activities.MainActivity
import java.time.LocalDate
import java.time.ZoneId

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Product Expiring"
        val body = intent.getStringExtra("body") ?: "One of your products is expiring tomorrow!"
        
        NotificationHandler.showNotification(context, title, body)
    }
}

object NotificationHandler {
    private const val CHANNEL_ID = "expiration_alerts"
    private const val CHANNEL_NAME = "Product Expiration Alerts"
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for products expiring soon"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun scheduleExpirationNotification(context: Context, productName: String, expirationDate: String) {
        try {
            val expiry = LocalDate.parse(expirationDate)
            val notificationDate = expiry.minusDays(1)
            
            // If it's already past the notification date, don't schedule
            if (notificationDate.isBefore(LocalDate.now())) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", "Expiration Alert")
                putExtra("body", "$productName is expiring tomorrow!")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                productName.hashCode(), // Use hashcode as unique request code for this product
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set for 9:00 AM on the notification date
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(notificationDate.year, notificationDate.monthValue - 1, notificationDate.dayOfMonth, 9, 0, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            Log.d("NotificationHandler", "Scheduled notification for $productName on $notificationDate")
        } catch (e: Exception) {
            Log.e("NotificationHandler", "Error scheduling notification: ${e.message}")
        }
    }
}
