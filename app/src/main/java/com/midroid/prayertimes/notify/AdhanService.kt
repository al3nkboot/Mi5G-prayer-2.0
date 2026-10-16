package com.midroid.prayertimes.notify

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.app.PendingIntent
import com.midroid.prayertimes.R

/** Plays the bundled adhan once, with a Stop action. Stops on completion, on Stop, or when audio focus is lost (e.g. a call). */
class AdhanService : Service() {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopAll(); return START_NOT_STICKY }
        Notifier.channels(this)
        val stop = PendingIntent.getService(this, 1, Intent(this, AdhanService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = Notifier.base(this, Notifier.CH_ADHAN, intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name), getString(R.string.adhan_playing))
            .addAction(0, getString(R.string.stop), stop).setDeleteIntent(stop).setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        ServiceCompat.startForeground(this, Notifier.ID_ADHAN, n, if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
        play()
        return START_NOT_STICKY
    }

    private fun play() {
        player?.release()
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val am = getSystemService(AudioManager::class.java)
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { if (it == AudioManager.AUDIOFOCUS_LOSS || it == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stopAll() }.build()
        am.requestAudioFocus(focus!!)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attrs)
                setWakeMode(this@AdhanService, PowerManager.PARTIAL_WAKE_LOCK)
                resources.openRawResourceFd(R.raw.prayer_chime).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                setOnCompletionListener { stopAll() }
                prepare(); start()
            }
        }.getOrElse { stopAll(); null }
    }

    private fun stopAll() {
        player?.runCatching { stop(); release() }; player = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }; focus = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() { player?.runCatching { release() }; player = null; super.onDestroy() }

    companion object { const val ACTION_STOP = "com.midroid.prayertimes.STOP_ADHAN"; const val EXTRA_TITLE = "title" }
}
