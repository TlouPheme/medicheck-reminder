package com.example.medicheckreminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicheckreminder.MediCheckApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as MediCheckApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medications = app.container.medicationRepository.observeAll().first()
                val logs = app.container.doseLogRepository.observeAll().first()
                DoseAlarmScheduler.reschedule(
                    app,
                    medications,
                    logs,
                    app.container.settingsRepository.snapshot()
                )
            } finally {
                pending.finish()
            }
        }
    }
}
