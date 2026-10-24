package com.physic.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : MediaSessionService() {
    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val NOTY_ID = 1
        private const val CHANNEL_ID = "physic"
    }

    private var countedSong: String? = null
    private var currentCrossfadeMs: Int = 0

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.playback_channel), NotificationManager.IMPORTANCE_LOW))
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { Playback.player = it }
        val notificationProvider = object : DefaultMediaNotificationProvider(
            this,
            DefaultMediaNotificationProvider.NotificationIdProvider { NOTY_ID },
            CHANNEL_ID,
            R.string.playback_channel
        ) {
            override fun getNotificationContentText(metadata: MediaMetadata): CharSequence? {
                val artist = metadata.artist?.toString().orEmpty()
                val album = metadata.albumTitle?.toString().orEmpty()
                return when {
                    artist.isNotEmpty() && album.isNotEmpty() -> "$artist · $album"
                    artist.isNotEmpty() -> artist
                    album.isNotEmpty() -> album
                    else -> null
                }
            }
        }
        notificationProvider.setSmallIcon(android.R.drawable.ic_media_play)
        setMediaNotificationProvider(notificationProvider)
        scope.launch { Prefs.pauseUnplug(applicationContext).collect { player?.setHandleAudioBecomingNoisy(it) } }
        scope.launch { Prefs.skipSilence(applicationContext).collect { player?.setSkipSilenceEnabled(it) } }
        scope.launch { Prefs.crossfadeMs(applicationContext).collect { currentCrossfadeMs = it } }
        session = MediaSession.Builder(this, player!!)
            .setSessionActivity(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            ).build()
        // Register the session with the service ourselves. onGetSession() only adds it when a
        // controller connects, and this app drives the player directly without one. Without this,
        // media3 never posts the media notification or promotes the service to foreground, so
        // playback gets killed in the background and launchers see no media controls.
        addSession(session!!)
        player!!.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                item?.mediaMetadata?.let { m ->
                    Playback.title.value = m.title?.toString() ?: "?"
                    Playback.artist.value = m.artist?.toString() ?: "?"
                }
                countedSong = null
                updateWidget()
                ensureArtwork()
                val fade = currentCrossfadeMs
                if (fade > 0) {
                    player?.volume = 0f
                    scope.launch {
                        for (i in 1..10) {
                            kotlinx.coroutines.delay((fade / 10).toLong())
                            try { player?.volume = i / 10f } catch (_: Exception) {}
                        }
                        try { player?.volume = 1f } catch (_: Exception) {}
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Playback.playing.value = isPlaying
                updateWidget()
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

    /**
     * Media3 cannot always resolve the MediaStore album-art URI itself, which leaves the
     * notification/lock screen without a large icon. Load the cover and embed it in the current
     * media item so every media controller (notification, lock screen, launchers) gets it.
     */
    private fun ensureArtwork() {
        val p = player ?: return
        val index = p.currentMediaItemIndex
        if (index < 0) return
        val item = p.currentMediaItem ?: return
        if (item.mediaMetadata.artworkData != null) return
        val song = Playback.queue.value.getOrNull(index) ?: return
        scope.launch(Dispatchers.IO) {
            val art = Artwork.bytes(applicationContext, song) ?: return@launch
            withContext(Dispatchers.Main) {
                val current = player ?: return@withContext
                if (current.currentMediaItemIndex != index) return@withContext
                val cur = current.currentMediaItem ?: return@withContext
                if (cur.mediaMetadata.artworkData != null) return@withContext
                try {
                    current.replaceMediaItem(
                        index,
                        cur.buildUpon().setMediaMetadata(
                            cur.mediaMetadata.buildUpon()
                                .setArtworkData(art, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                .build()
                        ).build()
                    )
                } catch (e: Exception) { /* player can be released mid-load */ }
            }
        }
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
        p.setMediaItems(songs.mapIndexed { i, s ->
            val meta = MediaMetadata.Builder()
                .setTitle(s.title)
                .setArtist(s.artist)
                .setAlbumTitle(s.album)
                .setTrackNumber(s.track.takeIf { it > 0 })
                .setReleaseYear(s.year.toIntOrNull())
                .setArtworkUri(albumArtUri(s))
            if (i == startIndex) {
                Artwork.bytes(context, s)?.let {
                    meta.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
            MediaItem.fromUri(s.uri).buildUpon().setMediaMetadata(meta.build()).build()
        }, startIndex, 0)
        p.prepare()
        p.play()
        p.shuffleModeEnabled = kotlinx.coroutines.runBlocking { Prefs.shuffle(context).first() }
        p.repeatMode = when (kotlinx.coroutines.runBlocking { Prefs.repeat(context).first() }) {
            1 -> androidx.media3.common.Player.REPEAT_MODE_ALL
            2 -> androidx.media3.common.Player.REPEAT_MODE_ONE
            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }
}
