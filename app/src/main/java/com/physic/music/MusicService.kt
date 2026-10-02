package com.physic.music

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
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

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().also { Playback.player = it }
        session = MediaSession.Builder(this, player!!)
            .setSessionActivity(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            ).build()
        player!!.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                item?.mediaMetadata?.let { m ->
                    Playback.title.value = m.title?.toString() ?: "?"
                    Playback.artist.value = m.artist?.toString() ?: "?"
                    songUriToPath(item.localConfiguration?.uri?.toString())?.let { path ->
                        val song = Playback.queue.value.firstOrNull { it.uri.toString() == item.localConfiguration?.uri?.toString() }
                        if (song != null) { Stats.played(applicationContext, song.path, song.album, song.artist); }
                    }
                }
                updateWidget()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Playback.playing.value = isPlaying
                updateWidget()
            }
        })
    }

    private fun songUriToPath(s: String?) = s

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TOGGLE" -> player?.let { if (it.isPlaying) it.pause() else it.play() }
            "NEXT" -> player?.seekToNextMediaItem()
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
