package com.midroid.prayertimes.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.midroid.prayertimes.R
import com.midroid.prayertimes.data.SoundChoice

/** Opens a MediaPlayer for a sound choice. A lost/unreadable custom file falls back to the bundled chime. */
object Sounds {
    /** what will actually play for [choice] given whether the custom file is readable */
    fun effective(choice: SoundChoice, customReadable: Boolean) = if (choice == SoundChoice.CUSTOM && !customReadable) SoundChoice.CHIME else choice

    fun rawFor(c: SoundChoice) = if (c == SoundChoice.ADHAN) R.raw.adhan_hargeisa else R.raw.prayer_chime

    /** true if the persisted SAF uri can still be opened */
    fun customReadable(ctx: Context, uri: String?): Boolean = uri != null &&
        runCatching { ctx.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false }.getOrDefault(false)

    /** @return the choice actually used (CUSTOM may degrade to CHIME) and the prepared player */
    fun prepare(ctx: Context, choice: SoundChoice, customUri: String?, attrs: AudioAttributes): Pair<SoundChoice, MediaPlayer>? {
        if (choice == SoundChoice.CUSTOM) {
            val p = runCatching {
                MediaPlayer().apply { setAudioAttributes(attrs); setDataSource(ctx, Uri.parse(customUri!!)); prepare() }
            }.onFailure { Log.w("Mi5GPrayer", "custom sound unavailable, falling back to chime", it) }.getOrNull()
            if (p != null) return SoundChoice.CUSTOM to p
        }
        val used = if (choice == SoundChoice.CUSTOM) SoundChoice.CHIME else choice
        return runCatching {
            used to MediaPlayer().apply {
                setAudioAttributes(attrs)
                ctx.resources.openRawResourceFd(rawFor(used)).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                prepare()
            }
        }.getOrNull()
    }

    // ---- in-app preview ----
    private var preview: MediaPlayer? = null
    var previewing: SoundChoice? = null; private set
    fun playPreview(ctx: Context, choice: SoundChoice, customUri: String?, onEnd: () -> Unit = {}) {
        stopPreview()
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val (used, p) = prepare(ctx, choice, customUri, attrs) ?: return
        previewing = used
        p.setOnCompletionListener { stopPreview(); onEnd() }
        p.start(); preview = p
    }
    fun stopPreview() { preview?.runCatching { stop(); release() }; preview = null; previewing = null }
}
