package com.example.medicheckreminder.reminder

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ActivityDoseAlarmBinding

/**
 * Full-screen alarm for a dose that was not answered within 10 minutes (user-defined feature 5).
 * It shows over the lock screen, plays the alarm sound with rising volume, vibrates, and offers
 * Taken, Snooze and Skip, which are handled by [DoseReminderReceiver] like the notification buttons.
 */
class DoseAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoseAlarmBinding
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var volume = START_VOLUME

    private val raiseVolume = object : Runnable {
        override fun run() {
            volume = (volume + VOLUME_STEP).coerceAtMost(1f)
            player?.setVolume(volume, volume)
            if (volume < 1f) handler.postDelayed(this, STEP_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        binding = ActivityDoseAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra(DoseReminderReceiver.EXTRA_NAME).orEmpty()
        val dosage = intent.getStringExtra(DoseReminderReceiver.EXTRA_DOSAGE).orEmpty()
        val time = intent.getStringExtra(DoseReminderReceiver.EXTRA_TIME).orEmpty()
        binding.textAlarmMedication.text = name
        binding.textAlarmDetails.text = getString(R.string.alarm_details, dosage, time)

        binding.btnAlarmTaken.setOnClickListener { answer(DoseReminderReceiver.ACTION_TAKEN) }
        binding.btnAlarmSnooze.setOnClickListener { answer(DoseReminderReceiver.ACTION_SNOOZE) }
        binding.btnAlarmSkip.setOnClickListener { answer(DoseReminderReceiver.ACTION_SKIP) }

        startSound()
        vibrate()
        Log.d(TAG, "Alarm screen shown for $name")
    }

    private fun answer(action: String) {
        Log.d(TAG, "Alarm answered: $action")
        sendBroadcast(
            Intent(this, DoseReminderReceiver::class.java).apply {
                this.action = action
                putExtras(intent.extras ?: Bundle())
            }
        )
        finish()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Escalation from Part 1: the alarm starts quietly and gets louder every couple of seconds. */
    private fun startSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                setDataSource(this@DoseAlarmActivity, uri)
                isLooping = true
                setVolume(START_VOLUME, START_VOLUME)
                prepare()
                start()
            }
            handler.postDelayed(raiseVolume, STEP_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Could not play the alarm sound", e)
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        val pattern = longArrayOf(0, 600, 400, 600, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(raiseVolume)
        player?.run {
            stop()
            release()
        }
        player = null
        getSystemService(Vibrator::class.java)?.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "DoseAlarmActivity"
        const val START_VOLUME = 0.15f
        const val VOLUME_STEP = 0.1f
        const val STEP_MS = 2_000L
    }
}
