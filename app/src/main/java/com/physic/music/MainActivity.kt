package com.physic.music

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(arrayOf("android.permission.READ_MEDIA_AUDIO", "android.permission.POST_NOTIFICATIONS"), 1)
        startService(android.content.Intent(this, MusicService::class.java))
        setContent { PhysicApp() }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                if (Prefs.updateCheck(this@MainActivity).first()) {
                    val conn = java.net.URL("https://api.github.com/repos/Tolepi/physic/releases/latest").openConnection() as java.net.HttpURLConnection
                    val body = conn.inputStream.bufferedReader().readText()
                    val tag = "\"tag_name\"\\s*:\\s*\"(v[^\"]+)\"".toRegex().find(body)?.groupValues?.get(1)
                    val current = packageManager.getPackageInfo(packageName, 0).versionName
                    if (tag != null && tag != "v$current") {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(this@MainActivity, "New Physic update: $tag at github.com/Tolepi/physic/releases", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

fun albumArtUri(song: Song): Uri = if (!song.coverOverride.isNullOrEmpty()) Uri.parse(song.coverOverride)
else ContentUris.withAppendedId(
    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, song.albumId
)

@Composable
fun PhysicApp() {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeName by Prefs.theme(c).collectAsState(initial = "System24")
    val rounded by Prefs.rounded(c).collectAsState(initial = false)
    val folders by Prefs.folders(c).collectAsState(initial = emptySet())
    val custom by Prefs.custom(c).collectAsState(initial = listOf(null, null, null, null, null, null))
    val fontName by Prefs.fontName(c).collectAsState(initial = "DM Mono")

    val theme = if (themeName == "Custom") {
        fun parse(s: String?, fallback: Color) = s?.removePrefix("#")?.let {
            try { Color((0xFF000000 or it.toLong(16)).toInt()) } catch (e: Exception) { fallback }
        } ?: fallback
        ThemeColors(
            "Custom",
            parse(custom.getOrNull(0), Color.Black),
            parse(custom.getOrNull(1), Color(0xFF111111)),
            parse(custom.getOrNull(2), Color.White),
            parse(custom.getOrNull(3), Color.Gray),
            parse(custom.getOrNull(4), Color.White),
            parse(custom.getOrNull(5), Color.Gray.copy(alpha = 0.35f)),
        )
    } else Themes.byName(themeName)

    val shape = RoundedCornerShape(if (rounded) 10.dp else 0.dp)
    val fontFamily = remember(fontName) {
        val bundled = mapOf(
            "DM Mono" to R.font.dm_mono_regular,
            "DM Mono Italic" to R.font.dm_mono_italic,
            "JetBrainsMono" to R.font.jetbrains_mono,
            "FiraCode" to R.font.fira_code,
            "CascadiaCode" to R.font.cascadia_code,
            "SpaceMono" to R.font.space_mono,
            "Iosevka" to R.font.iosevka,
            "MapleMono" to R.font.maple_mono,
            "MapleMono Italic" to R.font.maple_mono_italic,
        )
        when {
            bundled.containsKey(fontName) -> androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(bundled[fontName]!!)
            )
            else -> {
                val f = File(c.filesDir, "fonts/$fontName.ttf")
                if (f.exists()) androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(f)
                ) else androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.dm_mono_regular)
                )
            }
        }
    }
    val keepScreenOn by Prefs.keepScreenOn(c).collectAsState(initial = true)
    val activity = c as? android.app.Activity
    LaunchedEffect(keepScreenOn) {
        activity?.window?.let {
            if (keepScreenOn) it.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else it.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val onboarded by Prefs.onboarded(c).collectAsState(initial = false)
    val fontScale by Prefs.fontScale(c).collectAsState(initial = 1f)
    val lineHeightScale by Prefs.lineHeightScale(c).collectAsState(initial = 1f)
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        Density(baseDensity.density, baseDensity.fontScale * fontScale)
    }

    @Composable
    fun Wrap(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) { content() }
    }

    if (!onboarded) {
        Surface(Modifier.fillMaxSize(), color = theme.bg) {
            Wrap { ProvideTextStyle(TextStyle(fontFamily = fontFamily, lineHeight = (14 * lineHeightScale).sp)) { OnboardingScreen(theme) } }
        }
        return
    }

    val bgImg by Prefs.customBgImg(c).collectAsState(initial = "")
    val bgAlphaPct by Prefs.bgAlpha(c).collectAsState(initial = 45)
    val isLight = theme.bg.luminance() > 0.5f
    val m3Scheme = if (isLight) {
        androidx.compose.material3.lightColorScheme(
            primary = theme.accent, onPrimary = theme.bg, background = theme.bg, onBackground = theme.text,
            surface = theme.surface, onSurface = theme.text, surfaceVariant = theme.surface, onSurfaceVariant = theme.subtext,
            outline = theme.border, secondary = theme.accent
        )
    } else {
        androidx.compose.material3.darkColorScheme(
            primary = theme.accent, onPrimary = theme.bg, background = theme.bg, onBackground = theme.text,
            surface = theme.surface, onSurface = theme.text, surfaceVariant = theme.surface, onSurfaceVariant = theme.subtext,
            outline = theme.border, secondary = theme.accent
        )
    }
    androidx.compose.material3.MaterialTheme(colorScheme = m3Scheme, typography = androidx.compose.material3.Typography().run {
        copy(
            bodyLarge = bodyLarge.copy(fontFamily = fontFamily, color = theme.text),
            bodyMedium = bodyMedium.copy(fontFamily = fontFamily, color = theme.text),
            labelLarge = labelLarge.copy(fontFamily = fontFamily, color = theme.text),
            titleLarge = titleLarge.copy(fontFamily = fontFamily, color = theme.text),
            titleMedium = titleMedium.copy(fontFamily = fontFamily, color = theme.text),
            labelMedium = labelMedium.copy(fontFamily = fontFamily, color = theme.text)
        )
    }) {
    Wrap {
    Box(Modifier.fillMaxSize().background(theme.bg)) {
        if (bgImg.isNotEmpty()) AsyncImage(model = bgImg, contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(bgAlphaPct / 100f), contentScale = ContentScale.Fit)
        ProvideTextStyle(TextStyle(fontFamily = fontFamily, lineHeight = (14 * lineHeightScale).sp)) {
            val imageLoader = remember {
                coil.ImageLoader.Builder(c)
                    .components {
                        add(coil.decode.GifDecoder.Factory())
                        add(coil.decode.SvgDecoder.Factory())
                    }
                    .build()
            }
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
            val welcome by Prefs.welcome(c).collectAsState(initial = "Welcome back.")
            val profileName by Prefs.profileName(c).collectAsState(initial = "listener")
            val profilePic by Prefs.profilePic(c).collectAsState(initial = "")
            var tab by remember { mutableStateOf(0) }
            var songs by remember { mutableStateOf(listOf<Song>()) }
            val rescan = MusicIndex.rescanTick.intValue
            LaunchedEffect(folders, rescan) { songs = MusicIndex.songsInFolders(c, folders) }

            val title = Playback.title.collectAsState().value
            val artist = Playback.artist.collectAsState().value
            val playing = Playback.playing.collectAsState().value
            val shuffleOn by Prefs.shuffle(c).collectAsState(initial = false)
            val repeatOn by Prefs.repeat(c).collectAsState(initial = 0)
            var pos by remember { mutableStateOf(0f) }
            LaunchedEffect(playing) {
                while (playing) {
                    pos = Playback.player?.currentPosition?.toFloat() ?: 0f
                    kotlinx.coroutines.delay(400)
                }
            }

            Column(Modifier.fillMaxSize().padding(10.dp)) {
                // top tab bar — tui style
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    listOf("~/home", "~/songs", "~/albums", "~/artists", "~/stats", "~/playlists", "~/eq", "~/settings").forEachIndexed { i, t ->
                        Text(
                            (if (tab == i) "[$t]" else " $t "),
                            color = if (tab == i) theme.accent else theme.subtext,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { tab = i }.padding(end = 10.dp, bottom = 8.dp)
                        )
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        0 -> HomeTab(songs, theme, shape, welcome, profileName, profilePic)
                        1 -> SongsTab(songs, theme, shape)
                        2 -> AlbumsTab(songs, theme, shape)
                        3 -> ArtistsTab(songs, theme, shape)
                        4 -> StatsTab(theme)
                        5 -> PlaylistsTab(theme)
                        6 -> EqualizerTab(theme)
                        7 -> SettingsTab(theme, themeName, rounded, folders) { newTheme, newRounded, newFolders ->
                            scope.launch {
                                if (newTheme != null) Prefs.setTheme(c, newTheme)
                                if (newRounded != null) Prefs.setRounded(c, newRounded)
                                if (newFolders != null) Prefs.setFolders(c, newFolders)
                            }
                        }
                    }
                }

                // ---- now playing panel ----
                if (title.isNotEmpty()) {
                    val cur = Playback.queue.value.getOrNull(Playback.player?.currentMediaItemIndex ?: 0)
                    Column(Modifier.fillMaxWidth().heightIn(min = 140.dp).border(1.dp, theme.border, shape).background(theme.surface, shape).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Now playing!", color = theme.accent, fontSize = 12.sp)
                            val favSong by Prefs.favSong(c).collectAsState(initial = "")
                            val curSongTitle = Playback.queue.value.getOrNull(Playback.player?.currentMediaItemIndex ?: -1)?.title ?: ""
                            Text("★", color = if (favSong == curSongTitle && curSongTitle.isNotEmpty()) theme.accent else theme.subtext, fontSize = 28.sp,
                                modifier = Modifier.clickable { if (curSongTitle.isNotEmpty()) scope.launch { Prefs.setFavSong(c, if (favSong == curSongTitle) "" else curSongTitle) } })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (cur != null) {
                                val npSize by Prefs.npSize(c).collectAsState(initial = 52)
                                val customDisc by Prefs.customDisc(c).collectAsState(initial = "")
                                if (customDisc.isNotEmpty()) {
                                    val discAnim = rememberInfiniteTransition()
                                    val discRot by discAnim.animateFloat(0f, 360f, infiniteRepeatable(tween(3500, easing = LinearEasing)))
                                    androidx.compose.foundation.Image(painter = coil.compose.rememberAsyncImagePainter(model = customDisc), contentDescription = null, modifier = Modifier.size((npSize / 2).dp).clip(RoundedCornerShape(4.dp)).graphicsLayer { rotationZ = discRot }, contentScale = ContentScale.Crop)
                                } else {
                                    SpinningDisc(sizeDp = npSize / 2)
                                }
                                Spacer(Modifier.width(8.dp))
                                AsyncImage(
                                    model = albumArtUri(cur), contentDescription = null,
                                    modifier = Modifier.size(npSize.dp).clip(shape), contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee())
                                Text(artist, color = theme.subtext, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee())
                            }
                        }
                        val dur = (Playback.player?.duration?.toFloat() ?: 1f).coerceAtLeast(1f)
                        Slider(
                            value = pos.coerceIn(0f, dur), valueRange = 0f..dur,
                            onValueChange = { pos = it },
                            onValueChangeFinished = { Playback.player?.seekTo(pos.toLong()) },
                            colors = SliderDefaults.colors(
                                thumbColor = theme.accent, activeTrackColor = theme.accent,
                                inactiveTrackColor = theme.border
                            )
                        )
                        Box(Modifier.fillMaxWidth()) { Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shuffle, "shuffle", tint = if (shuffleOn) theme.accent else theme.subtext,
                                modifier = Modifier.clickable {
                                    scope.launch { Prefs.setShuffle(c, !shuffleOn); Playback.player?.shuffleModeEnabled = !shuffleOn }
                                })
                            Icon(Icons.Default.SkipPrevious, "prev", tint = theme.text, modifier = Modifier.clickable {
                                Playback.player?.seekToPreviousMediaItem()
                            })
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "playpause",
                                tint = theme.accent, modifier = Modifier.size(40.dp).clickable {
                                    Playback.player?.let { if (it.isPlaying) it.pause() else it.play() }
                                }
                            )
                            Icon(Icons.Default.SkipNext, "next", tint = theme.text, modifier = Modifier.clickable {
                                Playback.player?.seekToNextMediaItem()
                            })
                            Icon(
                                if (repeatOn == 2) Icons.Default.RepeatOne else Icons.Default.Repeat, "repeat",
                                tint = if (repeatOn > 0) theme.accent else theme.subtext,
                                modifier = Modifier.clickable {
                                    val next = (repeatOn + 1) % 3
                                    scope.launch {
                                        Prefs.setRepeat(c, next)
                                        Playback.player?.repeatMode = when (next) {
                                            1 -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                            2 -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                        }
                                    }
                                })
                        }
                        }
                        val upNext = Playback.queue.value.drop((Playback.player?.currentMediaItemIndex ?: 0) + 1).take(3).joinToString(" · ") { it.title }
                        if (upNext.isNotEmpty()) Text("up next: $upNext", color = theme.subtext, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        LyricsBlock(theme, pos)
                    }
                }
            }
        }
    }
}
}
}
}

@Composable
fun LyricsBlock(theme: ThemeColors, posMs: Float) {
    val c = LocalContext.current
    var showLyrics by remember { mutableStateOf(false) }
    val playing = Playback.playing.collectAsState().value
    val playerIndex = Playback.player?.currentMediaItemIndex ?: 0
    val curSong = Playback.queue.value.getOrNull(playerIndex)
    val doc = remember(curSong) { Lyrics.forSong(c, curSong?.path ?: "") }
    val pos = posMs.toLong()
    Text(if (showLyrics) "[hide lyrics]" else "[lyrics]", color = theme.accent, fontSize = 12.sp,
        modifier = Modifier.clickable { showLyrics = !showLyrics })
    if (showLyrics) {
        if (doc.isEmpty()) {
            Text(L.tr("no .lrc sidecar found for this song"), color = theme.subtext, fontSize = 12.sp)
        } else {
            val idx = doc.indexOfLast { it.first <= pos }.coerceAtLeast(0)
            Column(Modifier.height(180.dp).verticalScroll(rememberScrollState())) {
                doc.forEachIndexed { i, (t, text) ->
                    Text(
                        text,
                        color = if (i == idx) theme.accent else theme.subtext,
                        fontSize = if (i == idx) 14.sp else 12.sp,
                        fontWeight = if (i == idx) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpinningDisc(sizeDp: Int) {
    val anim = rememberInfiniteTransition()
    val rot by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(3500, easing = LinearEasing)))
    Canvas(Modifier.size(sizeDp.dp).padding(horizontal = 4.dp).graphicsLayer { rotationZ = rot }) {
        val rows = listOf(
            "..####..",
            ".######.",
            "########",
            "###..###",
            "###..###",
            "########",
            ".######.",
            "..####.."
        )
        val s = size.minDimension
        val px = s / 8f
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                if (ch == '#') drawRect(Color(0xFF141414), androidx.compose.ui.geometry.Offset(c * px, r * px), androidx.compose.ui.geometry.Size(px, px))
            }
        }
        // centre hole
        drawRect(Color.White, androidx.compose.ui.geometry.Offset(3 * px, 3 * px), androidx.compose.ui.geometry.Size(2 * px, 2 * px))
        // groove-highlight accent that shows rotation
        drawRect(Color(0xFF5A5A5A), androidx.compose.ui.geometry.Offset(6 * px, 1 * px), androidx.compose.ui.geometry.Size(px, px))
    }
}

@Composable
fun Panel(title: String, theme: ThemeColors, modifier: Modifier = Modifier, shape: RoundedCornerShape, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.border(1.dp, theme.border, shape).background(theme.surface, shape).padding(12.dp)) {
        Text(L.tr(title).uppercase(), color = theme.subtext, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun HomeTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape, welcome: String, profileName: String, profilePic: String) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var editWelcome by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var editBio by remember { mutableStateOf(false) }
    var editPronouns by remember { mutableStateOf(false) }
    var editLocation by remember { mutableStateOf(false) }
    var editFavAlbum by remember { mutableStateOf(false) }
    var welcomeField by remember(welcome) { mutableStateOf(welcome) }
    var nameField by remember(profileName) { mutableStateOf(profileName) }
    val topSongs = remember(songs) { Stats.topSongs(c) }
    val topSongList = remember(topSongs, songs) {
        topSongs.mapNotNull { s -> songs.firstOrNull { it.path == s.first } }
    }
    val recent = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(10) }
    val albums = remember(songs) { songs.groupBy { it.album }.entries.sortedByDescending { it.value.size } }
    val pickPfp = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            scope.launch { Prefs.setProfilePic(c, uri.toString()) }
        }
    }

    val banner by Prefs.profileBanner(c).collectAsState(initial = "")
    val bio by Prefs.profileBio(c).collectAsState(initial = "")
    val pronouns by Prefs.profilePronouns(c).collectAsState(initial = "")
    val fav by Prefs.favSong(c).collectAsState(initial = "")

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                scope.launch { Prefs.setProfileBanner(c, uri.toString()) }
            }
        }
        Panel("profile", theme, shape = shape) {
            if (banner.isNotEmpty()) {
                AsyncImage(model = banner, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(80.dp).clip(shape),
                    contentScale = ContentScale.Crop)
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profilePic.isNotEmpty()) {
                    AsyncImage(model = profilePic, contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(shape).border(1.dp, theme.border, shape), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(44.dp).background(theme.bg, shape).border(1.dp, theme.border, shape)) {
                        Text(profileName.firstOrNull()?.uppercase() ?: "?", color = theme.accent, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(profileName, color = theme.text, fontSize = 15.sp, modifier = Modifier.clickable { editName = true })
                    Text(L.tr("tap to edit"), color = theme.subtext, fontSize = 10.sp)
                }
                Text(L.tr("[set pfp]"), color = theme.accent, fontSize = 12.sp, modifier = Modifier.clickable { pickPfp.launch("image/*") })
            }
            if (pronouns.isNotEmpty()) Text("($pronouns)", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editPronouns = true })
            val location by Prefs.profileLocation(c).collectAsState(initial = "")
            val favAlbum by Prefs.profileFavAlbum(c).collectAsState(initial = "")
            var locationField by remember(location) { mutableStateOf(location) }
            var favAlbumField by remember(favAlbum) { mutableStateOf(favAlbum) }
            if (location.isNotEmpty()) Text("📍 $location", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editLocation = true })
            if (favAlbum.isNotEmpty()) Text("★ fav album: $favAlbum", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editFavAlbum = true })
            if (bio.isNotEmpty()) Text(bio, color = theme.text, fontSize = 13.sp, modifier = Modifier.clickable { editBio = true })
            else Text("tap to add a bio…", color = theme.subtext, fontSize = 13.sp, modifier = Modifier.clickable { editBio = true })
            val curIdx = Playback.player?.currentMediaItemIndex ?: -1
            val curSong = Playback.queue.value.getOrNull(curIdx)
            if (fav.isNotEmpty()) Text("★ favorite: $fav", color = theme.subtext, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("[banner]", color = theme.accent, fontSize = 12.sp, modifier = Modifier.clickable { pickBanner.launch("image/*") })
                Text("[pronouns]", color = theme.accent, fontSize = 12.sp, modifier = Modifier.clickable { editPronouns = true })
                Text("[location]", color = theme.accent, fontSize = 12.sp, modifier = Modifier.clickable { editLocation = true })
                Text("[fav album]", color = theme.accent, fontSize = 12.sp, modifier = Modifier.clickable { editFavAlbum = true })
            }
        }

        Panel("welcome — tap to edit", theme, shape = shape) {
            Text(welcome, color = theme.text, fontSize = 18.sp, modifier = Modifier.clickable { editWelcome = true })
        }

        Panel("most played", theme, shape = shape) {
            if (topSongList.isEmpty()) Text(L.tr("no plays yet"), color = theme.subtext, fontSize = 13.sp)
            Text("${songs.size} songs · ${albums.size} albums in library", color = theme.subtext, fontSize = 11.sp)
            topSongList.take(5).forEachIndexed { i, s ->
                Row(Modifier.fillMaxWidth().clickable { Playback.play(c, topSongList, i) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(34.dp).clip(shape))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${s.artist} · ${s.album}", color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Panel("recently added", theme, shape = shape) {
            recent.take(5).forEachIndexed { i, s ->
                Text("${s.title} — ${s.artist}", color = theme.text, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable { Playback.play(c, recent, i) }.padding(vertical = 3.dp))
            }
        }

        val lastPath = remember { Stats.lastPlayedPath(c) }
        val lastSong = remember(songs, lastPath) { songs.firstOrNull { it.path == lastPath } }
        if (lastSong != null) Panel("last played", theme, shape = shape) {
            Text("${lastSong.title} — ${lastSong.artist}", color = theme.text, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clickable { Playback.play(c, songs, songs.indexOf(lastSong)) }.padding(vertical = 3.dp))
        }

        Panel("albums", theme, shape = shape) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(albums) { (name, list) ->
                    Column(Modifier.width(110.dp).clickable { Playback.play(c, list, 0) }) {
                        AsyncImage(model = albumArtUri(list.first()), contentDescription = null,
                            modifier = Modifier.size(110.dp).clip(shape).background(theme.bg, shape), contentScale = ContentScale.Crop)
                        Text(name, color = theme.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${list.size} tracks", color = theme.subtext, fontSize = 10.sp)
                    }
                }
            }
        }

        Button(onClick = { if (songs.isNotEmpty()) Playback.play(c, songs.shuffled(), 0) },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent), shape = shape) {
            Text(L.tr("[ shuffle all ]"), color = theme.bg)
        }
        Spacer(Modifier.height(6.dp))
    }

    if (editWelcome) AlertDialog(
        onDismissRequest = { editWelcome = false },
        title = { Text(L.tr("edit welcome")) },
        text = { OutlinedTextField(value = welcomeField, onValueChange = { welcomeField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setWelcome(c, welcomeField); editWelcome = false } }) { Text(L.tr("ok")) } }
    )
    if (editName) AlertDialog(
        onDismissRequest = { editName = false },
        title = { Text(L.tr("edit name")) },
        text = { OutlinedTextField(value = nameField, onValueChange = { nameField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileName(c, nameField); editName = false } }) { Text(L.tr("ok")) } }
    )
    var bioField by remember(bio) { mutableStateOf(bio) }
    var pronounsField by remember(pronouns) { mutableStateOf(pronouns) }
    if (editBio) AlertDialog(
        onDismissRequest = { editBio = false },
        title = { Text("edit bio") },
        text = { OutlinedTextField(value = bioField, onValueChange = { bioField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileBio(c, bioField); editBio = false } }) { Text(L.tr("ok")) } }
    )
    if (editPronouns) AlertDialog(
        onDismissRequest = { editPronouns = false },
        title = { Text("edit pronouns") },
        text = { OutlinedTextField(value = pronounsField, onValueChange = { pronounsField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfilePronouns(c, pronounsField); editPronouns = false } }) { Text(L.tr("ok")) } }
    )
    val location by Prefs.profileLocation(c).collectAsState(initial = "")
    val favAlbum by Prefs.profileFavAlbum(c).collectAsState(initial = "")
    var locationField by remember(location) { mutableStateOf(location) }
    var favAlbumField by remember(favAlbum) { mutableStateOf(favAlbum) }
    if (editLocation) AlertDialog(
        onDismissRequest = { editLocation = false },
        title = { Text("edit location") },
        text = { OutlinedTextField(value = locationField, onValueChange = { locationField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileLocation(c, locationField); editLocation = false } }) { Text(L.tr("ok")) } }
    )
    if (editFavAlbum) AlertDialog(
        onDismissRequest = { editFavAlbum = false },
        title = { Text("edit favorite album") },
        text = { OutlinedTextField(value = favAlbumField, onValueChange = { favAlbumField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileFavAlbum(c, favAlbumField); editFavAlbum = false } }) { Text(L.tr("ok")) } }
    )
}

@Composable
fun SongsTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape) {
    val c = LocalContext.current
    var addSong by remember { mutableStateOf<Song?>(null) }
    var editSong by remember { mutableStateOf<Song?>(null) }
    var query by remember { mutableStateOf("") }
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    val shown = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) || it.album.contains(query, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(L.tr("search")) },
            modifier = Modifier.fillMaxWidth())
        val playsMap = remember(c) { Stats.topSongs(c, Int.MAX_VALUE).toMap() }
        var sortMode by remember { mutableStateOf(0) }
        val shownSorted = remember(shown, sortMode, playsMap) {
            when (sortMode) {
                1 -> shown.sortedBy { it.year.toIntOrNull() ?: Int.MAX_VALUE }
                2 -> shown.sortedByDescending { playsMap[it.path] ?: 0 }
                else -> shown
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("sort: ${listOf("title", "year", "plays")[sortMode]}", color = theme.accent, fontSize = 12.sp,
                modifier = Modifier.clickable { sortMode = (sortMode + 1) % 3 }.border(1.dp, theme.border).padding(6.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(shownSorted) { s ->
                val idx = songs.indexOf(s)
            Row(Modifier.fillMaxWidth().clickable { Playback.play(c, songs, idx) }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(coverSize.dp).clip(shape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${s.artist} · ${s.album}", color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                }
                Text(formatTime(s.durationMs), color = theme.subtext, fontSize = 11.sp)
                Text(L.tr(" +"), color = theme.accent, fontSize = 16.sp, modifier = Modifier.clickable { addSong = s }.padding(start = 8.dp))
                Text(L.tr(" ✎"), color = theme.accent, fontSize = 16.sp, modifier = Modifier.clickable { editSong = s }.padding(start = 8.dp))
            }
        }
    }
    addSong?.let { song ->
        AlertDialog(
            onDismissRequest = { addSong = null },
            title = { Text(L.tr("add to playlist")) },
            text = {
                Column {
                    Playlists.names(c).forEach { name ->
                        Text(name, modifier = Modifier.clickable {
                            Playlists.add(c, name, song.path); addSong = null
                        }.padding(vertical = 6.dp), color = theme.text)
                    }
                    var newPlaylistName by remember { mutableStateOf("") }
                    OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, label = { Text(L.tr("new playlist")) })
                    Button(onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            Playlists.create(c, newPlaylistName); Playlists.add(c, newPlaylistName, song.path); addSong = null
                        }
                    }) { Text(L.tr("create and add")) }
                }
            },
            confirmButton = {}
        )
    }
    }

    editSong?.let { song ->
        var tTitle by remember(song) { mutableStateOf(song.title) }
        var tArtist by remember(song) { mutableStateOf(song.artist) }
        var tAlbum by remember(song) { mutableStateOf(song.album) }
        var tYear by remember(song) { mutableStateOf(song.year) }
        var tTrack by remember(song) { mutableStateOf(song.track.toString()) }
        var tCover by remember(song) { mutableStateOf(song.coverOverride ?: "") }
        val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                tCover = uri.toString()
            }
        }
        AlertDialog(
            onDismissRequest = { editSong = null },
            title = { Text("metadata editor") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tCover.isNotEmpty()) AsyncImage(model = tCover, contentDescription = null, modifier = Modifier.size(96.dp).clip(shape))
                    Button(onClick = { pickCover.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) {
                        Text("pick cover", color = theme.bg)
                    }
                    OutlinedTextField(value = tTitle, onValueChange = { tTitle = it }, label = { Text("title") })
                    OutlinedTextField(value = tArtist, onValueChange = { tArtist = it }, label = { Text("artist") })
                    OutlinedTextField(value = tAlbum, onValueChange = { tAlbum = it }, label = { Text("album") })
                    OutlinedTextField(value = tYear, onValueChange = { tYear = it }, label = { Text("year") })
                    OutlinedTextField(value = tTrack, onValueChange = { tTrack = it }, label = { Text("track #") })
                    Text("delete", color = Color(0xFFEF5350), modifier = Modifier.clickable {
                        var deleted = false
                        try {
                            deleted = c.contentResolver.delete(song.uri, null, null) > 0
                        } catch (_: Exception) {}
                        if (!deleted) {
                            try { deleted = java.io.File(song.path).delete() } catch (_: Exception) {}
                        }
                        if (!deleted) {
                            try {
                                android.provider.MediaStore.createDeleteRequest(c.contentResolver, listOf(song.uri)).send(c, 0, null)
                            } catch (_: Exception) {}
                        }
                        MusicIndex.requestRescan()
                        editSong = null
                    })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Meta.set(c, song.path, tTitle, tArtist, tAlbum, tYear, tTrack, tCover)
                    MusicIndex.requestRescan()
                    editSong = null
                }) { Text(L.tr("ok")) }
            }
        )
    }
}

fun formatTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
           else "$m:${sec.toString().padStart(2, '0')}"
}

fun humanTime(ms: Long): String {
    val h = ms / 3600000
    val m = (ms % 3600000) / 60000
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun AlbumsTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape) {
    val c = LocalContext.current
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    val albums = remember(songs) { songs.groupBy { it.album } }
    val detailScroll = rememberScrollState()
    var selected by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    val sel = selected
    if (sel != null) BackHandler { selected = null }
    if (sel != null) {
        DetailView(theme, title = sel.first, subtitle = "${sel.second.first().artist} · ${sel.second.size} tracks",
            coverModel = albumArtUri(sel.second.first()), songs = sel.second, onBack = { selected = null }, scrollState = detailScroll)
    } else {
        LazyColumn {
            albums.entries.sortedBy { it.key }.forEach { (name, list) ->
                item {
                    Row(Modifier.fillMaxWidth().clickable { selected = name to list }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = albumArtUri(list.first()), contentDescription = null, modifier = Modifier.size(coverSize.dp + 16.dp).clip(shape))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val year = list.mapNotNull { it.year.toIntOrNull() }.minOrNull()
                            Text("${list.first().artist} · ${year ?: "?"} · ${list.size} tracks · ${humanTime(list.sumOf { it.durationMs })}", color = theme.subtext, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val artists = remember(songs) { songs.groupBy { it.artist } }
    val detailScroll = rememberScrollState()
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    if (selectedAlbum != null) BackHandler { selectedAlbum = null }
    else if (selectedArtist != null) BackHandler { selectedArtist = null }

    when {
        selectedAlbum != null && selectedArtist != null -> {
            val list = songs.filter { it.artist == selectedArtist && it.album == selectedAlbum }
            DetailView(theme, title = selectedAlbum!!, subtitle = selectedArtist!!,
                coverModel = list.firstOrNull()?.let { albumArtUri(it) }, songs = list,
                onBack = { selectedAlbum = null }, scrollState = detailScroll)
        }
        selectedArtist != null -> {
            val list = songs.filter { it.artist == selectedArtist }
            val byAlbum = list.groupBy { it.album }
            val customImg by Prefs.artistImage(c, selectedArtist!!).collectAsState(initial = "")
            val pickImg = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                    scope.launch { Prefs.setArtistImage(c, selectedArtist!!, uri.toString()) }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(L.tr("← back"), color = theme.accent, modifier = Modifier.clickable { selectedArtist = null }.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val imgModel: Any = if (customImg.isEmpty()) albumArtUri(list.first()) else customImg
                    AsyncImage(model = imgModel,
                        contentDescription = null, Modifier.size(72.dp).clip(shape).border(1.dp, theme.border, shape))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(selectedArtist!!, color = theme.text, fontSize = 18.sp)
                        Text("${list.size} songs · ${humanTime(list.sumOf { it.durationMs })} · [change img]", color = theme.subtext, fontSize = 12.sp,
                            modifier = Modifier.clickable { pickImg.launch("image/*") })
                    }
                }
                Spacer(Modifier.height(10.dp))
                byAlbum.entries.sortedBy { it.key }.forEach { (album, albumSongs) ->
                    Row(Modifier.fillMaxWidth().clickable { selectedAlbum = album }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = albumArtUri(albumSongs.first()), contentDescription = null, modifier = Modifier.size(coverSize.dp + 8.dp).clip(shape))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(album, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${albumSongs.mapNotNull { it.year.toIntOrNull() }.minOrNull() ?: "?"} · ${albumSongs.size} tracks · ${humanTime(albumSongs.sumOf { it.durationMs })}", color = theme.subtext, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        else -> {
            LazyColumn {
                artists.entries.sortedBy { it.key }.forEach { (name, list) ->
                    item {
                        Row(Modifier.fillMaxWidth().clickable { selectedArtist = name }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                            val customImg by Prefs.artistImage(c, name).collectAsState(initial = "")
                            AsyncImage(model = customImg.ifEmpty { albumArtUri(list.first()).toString() }, contentDescription = null,
                                Modifier.size(coverSize.dp + 8.dp).clip(shape).border(1.dp, theme.border, shape))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(name, color = theme.text)
                                Text("${list.size} songs · ${humanTime(list.sumOf { it.durationMs })}", color = theme.subtext, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailView(theme: ThemeColors, title: String, subtitle: String, coverModel: Any?, songs: List<Song>, onBack: () -> Unit, shape: RoundedCornerShape = RoundedCornerShape(0.dp), scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()) {
    val c = LocalContext.current
    var sortMode by remember { mutableStateOf(0) } // 0 = album order (track), 1 = title A-Z, 2 = year
    val c2 = LocalContext.current
    val rowPad by Prefs.rowPad(c2).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c2).collectAsState(initial = 40)
    val sorted = remember(songs, sortMode) {
        when (sortMode) {
            1 -> songs.sortedBy { it.title.lowercase() }
            2 -> songs.sortedBy { it.year.toIntOrNull() ?: Int.MAX_VALUE }
            else -> songs.sortedBy { it.track.let { t -> if (t == 0) Int.MAX_VALUE else t } }
        }
    }
    Column(Modifier.verticalScroll(scrollState)) {
        Text(L.tr("← back"), color = theme.accent, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (coverModel != null) {
                AsyncImage(model = coverModel, contentDescription = null, modifier = Modifier.size(72.dp).clip(shape))
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(title, color = theme.text, fontSize = 18.sp)
                Text(subtitle, color = theme.subtext, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("sort: ${listOf("track/album", "title", "year")[sortMode]}", color = theme.accent, fontSize = 12.sp,
                modifier = Modifier.clickable { sortMode = (sortMode + 1) % 3 }.border(1.dp, theme.border).padding(6.dp))
        }
        Spacer(Modifier.height(6.dp))
        sorted.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().clickable { Playback.play(c, sorted, i) }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(coverSize.dp).clip(shape))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artist, color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                }
                Text(formatTime(s.durationMs), color = theme.subtext, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun StatsTab(theme: ThemeColors) {
    val c = LocalContext.current
    var top by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var topArtists by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    LaunchedEffect(Unit) {
        top = Stats.topSongs(c)
        albums = Stats.topAlbums(c)
        total = Stats.total(c)
        topArtists = Stats.topArtists(c)
    }
    var fullView by remember { mutableStateOf<String?>(null) }

    if (fullView != null) BackHandler { fullView = null }

    when (fullView) {
        "songs" -> FullListView("most played songs", top.map { it.first.substringAfterLast('/') to it.second }, theme) { fullView = null }
        "albums" -> FullListView("top albums", albums, theme) { fullView = null }
        "artists" -> FullListView("top artists", topArtists, theme) { fullView = null }
        else -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel("total plays: $total", theme, shape = RoundedCornerShape(0.dp)) {
                top.let {
                    if (it.isNotEmpty()) Text("top: ${it.first().first.substringAfterLast('/')}", color = theme.text, fontSize = 13.sp)
                    else Text(L.tr("nothing yet — play something!"), color = theme.subtext, fontSize = 12.sp)
                }
            }
            StatCard("most played songs", top.take(5).map { it.first.substringAfterLast('/') to it.second }, theme) { fullView = "songs" }
            StatCard("top albums", albums.take(5), theme) { fullView = "albums" }
            StatCard("top artists", topArtists.take(5), theme) { fullView = "artists" }
            Text(L.tr("[import stats json]"), color = theme.accent, modifier = Modifier.clickable { showImport = !showImport })
            if (showImport) {
                OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text(L.tr("paste json")) },
                    modifier = Modifier.fillMaxWidth().height(160.dp))
                Button(onClick = { Stats.importJson(c, importText); showImport = false }) { Text(L.tr("import")) }
            }
        }
    }
}

@Composable
fun StatCard(title: String, items: List<Pair<String, Int>>, theme: ThemeColors, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, theme.border).background(theme.surface).clickable { onClick() }.padding(12.dp)) {
        Text(L.tr(title).uppercase(), color = theme.accent, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) Text(L.tr("no data"), color = theme.subtext, fontSize = 12.sp)
        items.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("${i + 1}. $name", color = theme.text, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count", color = theme.subtext, fontSize = 12.sp)
            }
        }
        Text(L.tr("tap for full list →"), color = theme.subtext, fontSize = 10.sp)
    }
}

@Composable
fun FullListView(title: String, items: List<Pair<String, Int>>, theme: ThemeColors, onBack: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(L.tr("← back"), color = theme.accent, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        Text(L.tr(title).uppercase(), color = theme.text, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth().border(1.dp, theme.border).padding(10.dp)) {
                Text("${i + 1}. $name", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count plays", color = theme.subtext, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    theme: ThemeColors, themeName: String, rounded: Boolean, folders: Set<String>,
    onUpdate: (String?, Boolean?, Set<String>?) -> Unit
) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var allFolders by remember { mutableStateOf(setOf<String>()) }
    val customState by Prefs.custom(c).collectAsState(initial = listOf(null, null, null, null, null, null))
    var customBg by remember(customState) { mutableStateOf(customState.getOrNull(0) ?: "#") }
    var customAccent by remember(customState) { mutableStateOf(customState.getOrNull(4) ?: "#") }
    var customText by remember(customState) { mutableStateOf(customState.getOrNull(2) ?: "#") }
    var customSurface by remember(customState) { mutableStateOf(customState.getOrNull(1) ?: "#") }
    var customSubtext by remember(customState) { mutableStateOf(customState.getOrNull(3) ?: "#") }
    var customBorder by remember(customState) { mutableStateOf(customState.getOrNull(5) ?: "#") }
    var fontUrl by remember { mutableStateOf("") }
    var fontMsg by remember { mutableStateOf("") }
    val fontName by Prefs.fontName(c).collectAsState(initial = "DM Mono")
    val downloaded = remember { File(c.filesDir, "fonts").apply { mkdirs() }.listFiles()?.map { it.nameWithoutExtension }?.toSet() ?: emptySet() }
    LaunchedEffect(Unit) { allFolders = MusicIndex.foldersOfLibrary(c) }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel("theme", theme, shape = RoundedCornerShape(0.dp)) {
            LazyRow {
                items(Themes.all.size + 1) { i ->
                    val name = if (i < Themes.all.size) Themes.all[i].name else "Custom"
                    Surface(
                        Modifier.padding(end = 8.dp, bottom = 4.dp).clickable { onUpdate(name, null, null) },
                        color = if (name == themeName) theme.accent else theme.bg,
                        shape = RoundedCornerShape(if (rounded) 8.dp else 0.dp)
                    ) { Text(name, color = if (name == themeName) theme.bg else theme.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp) }
                }
            }
            if (themeName == "Custom") {
                val existing by Prefs.savedCustomThemes(c).collectAsState(initial = "{}")
                val savedNames = remember(existing) {
                    try {
                        val obj = org.json.JSONObject(existing)
                        obj.keys().asSequence().toList()
                    } catch (e: Exception) { emptyList() }
                }
                Row(Modifier.fillMaxWidth()) {
                    Text("color picks", color = theme.subtext, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        scope.launch {
                            Prefs.setCustom(c, customBg, customSurface, customText, customSubtext, customAccent, customBorder)
                        }
                    }) { Text(L.tr("apply")) }
                }
                listOf("bg" to customBg, "surface" to customSurface, "text" to customText, "subtext" to customSubtext, "accent" to customAccent, "border" to customBorder).forEachIndexed { i, pair ->
                    val label = pair.first
                    val state = pair.second
                    Column(Modifier.fillMaxWidth().border(1.dp, theme.border).padding(8.dp)) {
                        Text(label, color = theme.text, fontSize = 12.sp)
                        ColorSliderPicker(
                            current = state,
                            onPick = { hex ->
                                val bg = if (label == "bg") hex else customBg
                                val surface = if (label == "surface") hex else customSurface
                                val text = if (label == "text") hex else customText
                                val subtext = if (label == "subtext") hex else customSubtext
                                val accent = if (label == "accent") hex else customAccent
                                val border = if (label == "border") hex else customBorder
                                when (label) {
                                    "bg" -> customBg = hex
                                    "surface" -> customSurface = hex
                                    "text" -> customText = hex
                                    "subtext" -> customSubtext = hex
                                    "accent" -> customAccent = hex
                                    else -> customBorder = hex
                                }
                                scope.launch { Prefs.setCustom(c, bg, surface, text, subtext, accent, border) }
                            }
                        )
                        OutlinedTextField(value = state, onValueChange = { v ->
                            val bg = if (label == "bg") v else customBg
                            val surface = if (label == "surface") v else customSurface
                            val text = if (label == "text") v else customText
                            val subtext = if (label == "subtext") v else customSubtext
                            val accent = if (label == "accent") v else customAccent
                            val border = if (label == "border") v else customBorder
                            when (label) {
                                "bg" -> customBg = v
                                "surface" -> customSurface = v
                                "text" -> customText = v
                                "subtext" -> customSubtext = v
                                "accent" -> customAccent = v
                                else -> customBorder = v
                            }
                            scope.launch { Prefs.setCustom(c, bg, surface, text, subtext, accent, border) }
                        }, label = { Text(L.tr("$label #hex")) }, modifier = Modifier.fillMaxWidth())
                    }
                }
                var newThemeName by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = newThemeName, onValueChange = { newThemeName = it }, label = { Text("save as…") }, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        if (newThemeName.isNotBlank()) {
                            val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                            j.put(newThemeName, "${customBg.trimStart('#')}|${customSurface.trimStart('#')}|${customText.trimStart('#')}|${customSubtext.trimStart('#')}|${customAccent.trimStart('#')}|${customBorder.trimStart('#')}")
                            scope.launch { Prefs.setSavedCustomThemes(c, j.toString()) }
                            newThemeName = ""
                        }
                    }) { Text("[ save ]") }
                }
                if (savedNames.isNotEmpty()) {
                    Text("saved themes:", color = theme.subtext, fontSize = 11.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        savedNames.forEach { name ->
                            var confirmDelete by remember { mutableStateOf(false) }
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    Modifier.padding(end = 4.dp, bottom = 4.dp).clickable {
                                        val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                                        val list = j.optString(name, "").split("|")
                                        if (list.size >= 6) {
                                            scope.launch {
                                                Prefs.setCustom(c, "#" + list[0], "#" + list[1], "#" + list[2], "#" + list[3], "#" + list[4], "#" + list[5])
                                            }
                                        }
                                    },
                                    color = theme.surface,
                                    shape = RoundedCornerShape(0.dp)
                                ) { Text(name, color = theme.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp) }
                                Text("✕", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { confirmDelete = true }.padding(end = 8.dp, bottom = 4.dp))
                            }
                            if (confirmDelete) {
                                AlertDialog(
                                    onDismissRequest = { confirmDelete = false },
                                    title = { Text("delete theme?") },
                                    text = { Text("Delete saved theme \"$name\"?") },
                                    confirmButton = { TextButton(onClick = {
                                        val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                                        j.remove(name)
                                        scope.launch { Prefs.setSavedCustomThemes(c, j.toString()) }
                                        confirmDelete = false
                                    }) { Text("delete") } },
                                    dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("cancel") } }
                                )
                            }
                        }
                    }
                }
                Button(onClick = { scope.launch { Prefs.setCustom(c, customBg, customSurface, customText, customSubtext, customAccent, customBorder) } }) { Text(L.tr("apply")) }
            }
        }

        Panel("font", theme, shape = RoundedCornerShape(0.dp)) {
            val bundledFonts = listOf("DM Mono", "DM Mono Italic", "JetBrainsMono", "FiraCode", "CascadiaCode", "SpaceMono", "Iosevka", "MapleMono", "MapleMono Italic")
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                bundledFonts.forEach { f ->
                    Text(f, color = if (fontName == f) theme.accent else theme.text,
                        modifier = Modifier.clickable { scope.launch { Prefs.setFontName(c, f) } }.padding(end = 14.dp), fontSize = 13.sp)
                }
                downloaded.filter { it != "dm_mono_regular" && it != "dm_mono_italic" && it != "space_mono" && it != "cascadia_code" && it != "fira_code" && it != "jetbrains_mono" && it != "iosevka" && it != "maple_mono" && it != "maple_mono_italic" }.forEach {
                    Text(it, color = if (fontName == it) theme.accent else theme.text,
                        modifier = Modifier.clickable { scope.launch { Prefs.setFontName(c, it) } }.padding(end = 14.dp), fontSize = 13.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = fontUrl, onValueChange = { fontUrl = it }, label = { Text(L.tr(".ttf url")) }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val conn = URL(fontUrl).openConnection() as HttpURLConnection
                                conn.inputStream.use { input ->
                                    val name = fontUrl.substringAfterLast('/').substringBefore('?')
                                    val file = File(c.filesDir, "fonts/$name").also { it.parentFile?.mkdirs() }
                                    FileOutputStream(file).use { out -> input.copyTo(out) }
                                }
                            }
                            val name = fontUrl.substringAfterLast('/').substringBefore('?').removeSuffix(".ttf")
                            withContext(Dispatchers.Main) {
                                Prefs.setFontName(c, name)
                                fontMsg = "installed $name"
                            }
                        } catch (e: Exception) { fontMsg = "download failed" }
                    }
                }) { Text(L.tr("dl")) }
            }
            if (fontMsg.isNotEmpty()) Text(fontMsg, color = theme.subtext, fontSize = 11.sp)
        }

        Panel("corners", theme, shape = RoundedCornerShape(0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (rounded) "rounded on" else "square (default)", color = theme.text, modifier = Modifier.weight(1f))
                Switch(checked = rounded, onCheckedChange = { onUpdate(null, it, null) })
            }
        }

        val pauseUnplug by Prefs.pauseUnplug(c).collectAsState(initial = true)
        val updateCheck by Prefs.updateCheck(c).collectAsState(initial = false)
        val playPct by Prefs.playCountPct(c).collectAsState(initial = 20)
        Panel("playback", theme, shape = RoundedCornerShape(0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("pause when headphones unplugged", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Switch(checked = pauseUnplug, onCheckedChange = { checked ->
                    scope.launch { Prefs.setPauseUnplug(c, checked); Playback.player?.setHandleAudioBecomingNoisy(checked) }
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("check for updates on launch", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Switch(checked = updateCheck, onCheckedChange = { scope.launch { Prefs.setUpdateCheck(c, it) } })
            }
            Column {
                Text("count a play after ${playPct}% of the song", color = theme.text, fontSize = 13.sp)
                Slider(value = playPct.toFloat(),
                    onValueChange = { v -> scope.launch { Prefs.setPlayCountPct(c, v.toInt().coerceIn(1, 99)) } },
                    valueRange = 1f..99f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("skip silence in tracks", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                val skipSilence by Prefs.skipSilence(c).collectAsState(initial = false)
                Switch(checked = skipSilence, onCheckedChange = { scope.launch { Prefs.setSkipSilence(c, it); Playback.player?.setSkipSilenceEnabled(it) } })
            }
            Column {
                val crossfadeMs by Prefs.crossfadeMs(c).collectAsState(initial = 0)
                Text("crossfade (fade-in): ${crossfadeMs}ms", color = theme.text, fontSize = 13.sp)
                Slider(value = crossfadeMs.toFloat(),
                    onValueChange = { v -> scope.launch { Prefs.setCrossfadeMs(c, v.toInt().coerceIn(0, 8000)) } },
                    valueRange = 0f..8000f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("keep screen on", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                val keepScreenOn by Prefs.keepScreenOn(c).collectAsState(initial = true)
                Switch(checked = keepScreenOn, onCheckedChange = { scope.launch { Prefs.setKeepScreenOn(c, it) } })
            }
        }

        Panel("index folders", theme, shape = RoundedCornerShape(0.dp)) {
            allFolders.sorted().forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = folders.contains(f), onCheckedChange = { checked ->
                        val new = folders.toMutableSet()
                        if (checked) new.add(f) else new.remove(f)
                        onUpdate(null, null, new)
                    })
                    Text(f, color = theme.subtext, fontSize = 12.sp)
                }
            }
        }

        Panel("about", theme, shape = RoundedCornerShape(0.dp)) {
            Text("physic v${LocalContext.current.packageManager.getPackageInfo(LocalContext.current.packageName, 0).versionName}", color = theme.text, fontSize = 13.sp)
            Text(L.tr("github.com/Tolepi/physic"), color = theme.accent, fontSize = 12.sp)
            Text(L.tr("vibecoded with love"), color = theme.subtext, fontSize = 11.sp)
        }

        val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
        val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
        val npSize by Prefs.npSize(c).collectAsState(initial = 52)
        val eqVertical by Prefs.eqVertical(c).collectAsState(initial = false)
        val bgAlphaPct by Prefs.bgAlpha(c).collectAsState(initial = 45)
        val fontScale by Prefs.fontScale(c).collectAsState(initial = 1f)
        val lineHeightScale by Prefs.lineHeightScale(c).collectAsState(initial = 1f)
        val backupMsg by remember { mutableStateOf("") }
        var backupText by remember { mutableStateOf("") }
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val json = Prefs.exportAll(c)
                        c.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    } catch (_: Exception) {}
                }
            }
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val text = c.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                        Prefs.importAll(c, text)
                        MusicIndex.requestRescan()
                    } catch (_: Exception) {}
                }
            }
        }
        Panel("backup / restore", theme, shape = RoundedCornerShape(0.dp)) {
            Text("export profile, stats, playlists, metadata edits and settings into one file", color = theme.subtext, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { exportLauncher.launch("physic-backup.json") }) { Text("[ export all ]") }
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("[ import ]") }
                TextButton(onClick = { scope.launch { backupText = Prefs.exportAll(c) } }) { Text("[ show json ]") }
            }
            if (backupText.isNotEmpty()) {
                Text(backupText, color = theme.subtext, fontSize = 10.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { backupText = "" }) { Text("[ close ]") }
            }
            Text("background image opacity: $bgAlphaPct%", color = theme.text, fontSize = 13.sp)
            Slider(value = bgAlphaPct.toFloat(), onValueChange = { v -> scope.launch { Prefs.setBgAlpha(c, v.toInt().coerceIn(0, 100)) } }, valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
        }
        Panel("customization", theme, shape = RoundedCornerShape(0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("vertical equalizer sliders", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = eqVertical, onCheckedChange = { scope.launch { Prefs.setEqVertical(c, it) } })
            }
            Text("font size: ${(fontScale * 100).toInt()}%", color = theme.text, fontSize = 13.sp)
            Slider(value = fontScale, onValueChange = { v -> scope.launch { Prefs.setFontScale(c, (v * 20).toInt() / 20f) } }, valueRange = 0.7f..2.0f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("line height: ${(lineHeightScale * 100).toInt()}%", color = theme.text, fontSize = 13.sp)
            Slider(value = lineHeightScale, onValueChange = { v -> scope.launch { Prefs.setLineHeightScale(c, (v * 20).toInt() / 20f) } }, valueRange = 0.8f..2.5f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("song row padding: ${rowPad}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = rowPad.toFloat(), onValueChange = { v -> scope.launch { Prefs.setRowPad(c, v.toInt().coerceIn(0, 24)) } }, valueRange = 0f..24f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("cover size in lists: ${coverSize}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = coverSize.toFloat(), onValueChange = { v -> scope.launch { Prefs.setCoverSize(c, v.toInt().coerceIn(24, 96)) } }, valueRange = 24f..96f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("now playing cover: ${npSize}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = npSize.toFloat(), onValueChange = { v -> scope.launch { Prefs.setNpSize(c, v.toInt().coerceIn(32, 96)) } }, valueRange = 32f..96f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))

            val customDisc by Prefs.customDisc(c).collectAsState(initial = "")
            val pickDisc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                    scope.launch { Prefs.setCustomDisc(c, uri.toString()) }
                }
            }
            Text("custom disc icon: ${if (customDisc.isEmpty()) "none (pixel-art default)" else "custom set"}", color = theme.text, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pickDisc.launch("image/*") }) { Text("[ pick svg/gif/png ]") }
                if (customDisc.isNotEmpty()) TextButton(onClick = { scope.launch { Prefs.setCustomDisc(c, "") } }) { Text("[ clear ]") }
            }

            Text("background image (custom theme)", color = theme.text, fontSize = 13.sp)
            val customBgImg by Prefs.customBgImg(c).collectAsState(initial = "")
            val pickBg = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                    scope.launch { Prefs.setCustomBgImg(c, uri.toString()) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pickBg.launch("image/*") }) { Text("[ pick bg ]") }
                if (customBgImg.isNotEmpty()) TextButton(onClick = { scope.launch { Prefs.setCustomBgImg(c, "") } }) { Text("[ clear ]") }
            }
        }
    }
}

@Composable
fun PlaylistsTab(theme: ThemeColors) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var names by remember { mutableStateOf(Playlists.names(c)) }
    var openPlaylist by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var pickPlaylistTarget by remember { mutableStateOf<Song?>(null) }

    val refresh = { names = Playlists.names(c) }

    if (openPlaylist != null) BackHandler { openPlaylist = null }

    when (val open = openPlaylist) {
        null -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel("your playlists", theme, shape = RoundedCornerShape(0.dp)) {
                if (names.isEmpty()) Text(L.tr("no playlists yet — make one!"), color = theme.subtext, fontSize = 13.sp)
                names.forEach { name ->
                    Row(Modifier.fillMaxWidth().clickable { openPlaylist = name }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, color = theme.text, modifier = Modifier.weight(1f))
                        Text(L.tr("[delete]"), color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { Playlists.delete(c, name); refresh() })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(onClick = { showNew = !showNew }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(L.tr("[ new playlist ]"), color = theme.bg) }
                if (showNew) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(L.tr("name")) })
                    Button(onClick = { if (newName.isNotBlank()) { Playlists.create(c, newName); refresh(); newName = ""; showNew = false } }) { Text(L.tr("create")) }
                }
            }
            Row {
                Text(L.tr("[export]"), color = theme.accent, modifier = Modifier.clickable { exportText = Playlists.exportJson(c) }.padding(end = 16.dp))
                Text(L.tr("[import]"), color = theme.accent, modifier = Modifier.clickable { showImport = !showImport })
            }
            exportText?.let { text ->
                Panel("export (copy this into a file to share)", theme, shape = RoundedCornerShape(0.dp)) {
                    Text(text, color = theme.subtext, fontSize = 10.sp, maxLines = 20, overflow = TextOverflow.Ellipsis)
                }
            }
            if (showImport) {
                OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text(L.tr("paste playlist json")) }, modifier = Modifier.fillMaxWidth().height(160.dp))
                Button(onClick = { Playlists.importJson(c, importText); refresh(); showImport = false }) { Text(L.tr("import")) }
            }
        }
        else -> {
            val paths = Playlists.paths(c, open)
            var removeTick by remember(open) { mutableStateOf(0) }
            val fullIndex = remember(removeTick) {
                MusicIndex.songsInFolders(c, emptySet()).associateBy { it.path }
            }
            val songs = remember(paths, fullIndex) {
                paths.mapNotNull { p -> fullIndex[p] }
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(L.tr("← back"), color = theme.accent, modifier = Modifier.clickable { openPlaylist = null; refresh() }.padding(bottom = 8.dp))
                Text(open, color = theme.text, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                if (songs.isEmpty()) Text(L.tr("empty — add songs from the songs tab (+ icon)"), color = theme.subtext, fontSize = 12.sp)
                songs.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f).clickable { Playback.play(c, songs, i) }) {
                            Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${s.artist} · ${s.album}", color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                        }
                        Text("▲", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable {
                            if (i > 0) { Playlists.move(c, open, i, i - 1); removeTick++ }
                        }.padding(horizontal = 6.dp))
                        Text("▼", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable {
                            if (i < songs.lastIndex) { Playlists.move(c, open, i, i + 1); removeTick++ }
                        }.padding(horizontal = 6.dp))
                        Text("[x]", color = theme.subtext, modifier = Modifier.clickable { Playlists.remove(c, open, i); removeTick++ }.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSliderPicker(current: String, onPick: (String) -> Unit) {
    val initialHsv = remember(current) {
        try {
            val parsed = android.graphics.Color.parseColor(if (current.startsWith("#")) current else "#$current")
            val out = FloatArray(3)
            android.graphics.Color.colorToHSV(parsed, out)
            out
        } catch (e: Exception) { FloatArray(3) }
    }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var sat by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }
    LaunchedEffect(current) {
        // keep the slider synced when hex is typed manually
        hue = initialHsv[0]; sat = initialHsv[1]; value = initialHsv[2]
    }
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val hex = String.format("#%02X%02X%02X", (rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(Color(rgb)).border(1.dp, Color.Gray))
            Spacer(Modifier.width(8.dp))
            Text(hex, color = Color.Gray, fontSize = 12.sp)
        }
        Text("hue", fontSize = 10.sp, color = Color.Gray)
        Slider(value = hue, onValueChange = { hue = it; onPick(stringColor(hue, sat, value)) }, valueRange = 0f..360f)
        Text("saturation", fontSize = 10.sp, color = Color.Gray)
        Slider(value = sat, onValueChange = { sat = it; onPick(stringColor(hue, sat, value)) }, valueRange = 0f..1f)
        Text("value", fontSize = 10.sp, color = Color.Gray)
        Slider(value = value, onValueChange = { value = it; onPick(stringColor(hue, sat, value)) }, valueRange = 0f..1f)
    }
}

private fun stringColor(hue: Float, sat: Float, value: Float): String {
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    return String.format("#%02X%02X%02X", (rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
}

@Composable
fun OnboardingScreen(theme: ThemeColors) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var welcome by remember { mutableStateOf("Welcome back.") }
    var name by remember { mutableStateOf("listener") }
    var foldersOn by remember { mutableStateOf(setOf<String>()) }
    var allFolders by remember { mutableStateOf(setOf<String>()) }
    var themeName by remember { mutableStateOf("System24") }
    var rounded by remember { mutableStateOf(false) }
    var fontName by remember { mutableStateOf("DM Mono") }
    LaunchedEffect(Unit) { allFolders = MusicIndex.foldersOfLibrary(c) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(L.tr("welcome to Physic"), color = theme.text, fontSize = 24.sp)
        Text(L.tr("you can change all of this later in ~/settings"), color = theme.subtext, fontSize = 12.sp)

        Panel("profile", theme, shape = RoundedCornerShape(0.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(L.tr("your name")) })
            OutlinedTextField(value = welcome, onValueChange = { welcome = it }, label = { Text(L.tr("welcome message")) })
        }
        Panel("theme", theme, shape = RoundedCornerShape(0.dp)) {
            LazyRow {
                items(Themes.all.size) { i ->
                    val t = Themes.all[i]
                    Surface(Modifier.padding(end = 8.dp).clickable { themeName = t.name }, color = if (themeName == t.name) theme.accent else theme.bg) {
                        Text(t.name, color = if (themeName == t.name) theme.bg else theme.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L.tr("rounded corners"), color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = rounded, onCheckedChange = { rounded = it })
            }
        }
        Panel("font", theme, shape = RoundedCornerShape(0.dp)) {
            listOf("DM Mono", "JetBrainsMono", "FiraCode", "CascadiaCode", "SpaceMono", "Iosevka", "MapleMono", "MapleMono Italic").forEach {
                Text(it, color = if (fontName == it) theme.accent else theme.subtext,
                    modifier = Modifier.clickable { fontName = it }.padding(vertical = 3.dp))
            }
        }
        Panel("index your music folders", theme, shape = RoundedCornerShape(0.dp)) {
            allFolders.sorted().forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = foldersOn.contains(f), onCheckedChange = { checked ->
                        foldersOn = if (checked) foldersOn + f else foldersOn - f
                    })
                    Text(f, color = theme.subtext, fontSize = 12.sp)
                }
            }
        }
        Button(onClick = {
            scope.launch {
                Prefs.setProfileName(c, name)
                Prefs.setWelcome(c, welcome)
                Prefs.setTheme(c, themeName)
                Prefs.setRounded(c, rounded)
                Prefs.setFontName(c, fontName)
                Prefs.setFolders(c, foldersOn)
                Prefs.setOnboarded(c, true)
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(L.tr("[ finish ]"), color = theme.bg) }
    }
}
