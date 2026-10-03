package com.physic.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicService : MediaSessionService() {
    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val NOTY_ID = 1
        private const val CHANNEL_ID = "physic"
    }

    private var countedSong: String? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW))
        player = ExoPlayer.Builder(this).build().also { Playback.player = it }
        scope.launch { Prefs.pauseUnplug(applicationContext).collect { player?.setHandleAudioBecomingNoisy(it) } }
        session = MediaSession.Builder(this, player!!)
            .setSessionActivity(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            ).build()
        player!!.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                item?.mediaMetadata?.let { m ->
                    Playback.title.value = m.title?.toString() ?: "?"
                    Playback.artist.value = m.artist?.toString() ?: "?"
                }
                countedSong = null
                updateWidget()
                updateNotification()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Playback.playing.value = isPlaying
                updateWidget()
                updateNotification()
            }
        })

        // Count a play only after playCountPct% of the current song has played.
        scope.launch {
            var currentPct = 20
            launch { Prefs.playCountPct(applicationContext).collect { currentPct = it } }
            while (true) {
                kotlinx.coroutines.delay(1000)
                try {
                    val p = player
                    if (p != null && p.playWhenReady && p.currentPosition > 0 && p.duration > 0) {
                        val pct = currentPct.toLong().coerceIn(1, 99)
                        if (p.currentPosition >= p.duration * pct / 100) {
                            val item = p.currentMediaItem
                            val uriStr = item?.localConfiguration?.uri?.toString()
                            val song = Playback.queue.value.firstOrNull { it.uri.toString() == uriStr }
                            if (song != null && countedSong != song.uri.toString()) {
                                countedSong = song.uri.toString()
                                Stats.played(applicationContext, song.path, song.album, song.artist)
                            }
                        }
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    private fun buildNotification(): Notification {
        val meta = player?.currentMediaItem?.mediaMetadata
        val playing = player?.isPlaying == true
        val toggle = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).setAction("TOGGLE"), PendingIntent.FLAG_IMMUTABLE)
        val prev = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).setAction("PREV"), PendingIntent.FLAG_IMMUTABLE)
        val next = PendingIntent.getService(this, 3, Intent(this, MusicService::class.java).setAction("NEXT"), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 4, Intent(this, MusicService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(meta?.title?.toString() ?: "Physic")
            .setContentText(meta?.artist?.toString() ?: "")
            .setSubText(meta?.albumTitle?.toString())
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(playing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prev)
            .addAction(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, "Play/Pause", toggle)
            .addAction(android.R.drawable.ic_media_next, "Next", next)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    private fun updateNotification() {
        val n = buildNotification()
        try {
            startForeground(NOTY_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } catch (e: Exception) {
            try {
                (getSystemService(NotificationManager::class.java)).notify(NOTY_ID, n)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?) = super.onBind(intent)

    private fun songUriToPath(s: String?) = s

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TOGGLE" -> player?.let { if (it.isPlaying) it.pause() else it.play() }
            "NEXT" -> player?.seekToNextMediaItem()
            "PREV" -> player?.seekToPreviousMediaItem()
            "STOP" -> { player?.pause(); player?.seekTo(0) }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        session?.run { player.release(); release() }
        Playback.player = null
        super.onDestroy()
    }

    private fun updateWidget() {
        scope.launch { MusicWidget().updateAll(applicationContext) }
    }
}

object Playback {
    var player: ExoPlayer? = null
    val title = MutableStateFlow("")
    val artist = MutableStateFlow("")
    val playing = MutableStateFlow(false)
    val queue = MutableStateFlow<List<Song>>(emptyList())
    val index = MutableStateFlow(0)

    fun play(context: Context, songs: List<Song>, startIndex: Int) {
        queue.value = songs
        index.value = startIndex
        val p = player
        if (p == null) {
            context.startForegroundService(Intent(context, MusicService::class.java))
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ play(context, songs, startIndex) }, 300)
            return
        }
        p.setMediaItems(songs.map { MediaItem.fromUri(it.uri).buildUpon().setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album)
                .setArtworkUri(ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, it.albumId))
                .build()
        ).build() }, startIndex, 0)
        p.prepare()
        p.play()
    }
}
