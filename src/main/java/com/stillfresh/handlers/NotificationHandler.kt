package com.stillfresh.handlers

import android.Manifest
import android.annotation.SuppressLint
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
import java.util.Calendar
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stillfresh.R
import com.stillfresh.activities.MainActivity
import java.time.LocalDate

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
    
    private const val TAG = "NotificationHandler"
    private const val CHANNEL_ID = "expiration_alerts"
    private const val CHANNEL_NAME = "Product Expiration Alerts"
    private const val NOTIFICATION_HOUR = 13
    private const val DEBUG_NOTIFICATION_REQUEST_CODE = 919_191

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification skipped because POST_NOTIFICATIONS is not granted")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.w(TAG, "Exact alarms unavailable, scheduled inexact alarm instead")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    @SuppressLint("ScheduleExactAlarm")
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

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                "$productName|$expirationDate".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set for 9:00 AM on the notification date
            val calendar = Calendar.getInstance().apply {
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
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(notificationDate.year, notificationDate.monthValue - 1, notificationDate.dayOfMonth, NOTIFICATION_HOUR, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            var triggerAtMillis = calendar.timeInMillis
            if (triggerAtMillis <= System.currentTimeMillis()) {
                triggerAtMillis = System.currentTimeMillis() + 10_000L
                Log.w(TAG, "Notification time already passed, scheduling fallback in 10 seconds for $productName")
            }

            scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
            Log.d(TAG, "Scheduled notification for $productName on $notificationDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification: ${e.message}")
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDebugTestNotification(context: Context, delaySeconds: Long = 10L) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + (delaySeconds.coerceAtLeast(1L) * 1000L)
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "StillFresh test")
            putExtra("body", "Test notification delivered successfully.")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DEBUG_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
        Log.d(TAG, "Scheduled debug test notification in ${delaySeconds.coerceAtLeast(1L)} seconds")
    }
}
