package com.example.kt4_11

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Телефон перезагружен, восстанавливаем напоминание")


            if (ReminderManager.isReminderEnabled(context)) {
                ReminderManager.scheduleNextReminder(context)
                Log.d("BootReceiver", "Напоминание восстановлено")
            }
        }
    }
}