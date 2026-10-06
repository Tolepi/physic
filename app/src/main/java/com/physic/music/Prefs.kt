package com.physic.music

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds: DataStore<Preferences> by preferencesDataStore("physic")

object Prefs {
    private val THEME = stringPreferencesKey("theme")
    private val CUSTOM_BG = stringPreferencesKey("custom_bg")
    private val CUSTOM_ACCENT = stringPreferencesKey("custom_accent")
    private val CUSTOM_TEXT = stringPreferencesKey("custom_text")
    private val CUSTOM_SURFACE = stringPreferencesKey("custom_surface")
    private val CUSTOM_SUBTEXT = stringPreferencesKey("custom_subtext")
    private val CUSTOM_BORDER = stringPreferencesKey("custom_border")
    private val ROUNDED = booleanPreferencesKey("rounded")
    private val FOLDERS = stringPreferencesKey("folders")
    private val SHUFFLE = booleanPreferencesKey("shuffle")
    private val REPEAT = intPreferencesKey("repeat") // 0 off, 1 all, 2 one

    private val WELCOME = stringPreferencesKey("welcome")
    private val PROFILE_NAME = stringPreferencesKey("profile_name")
    private val PROFILE_PIC = stringPreferencesKey("profile_pic")
    private val PROFILE_BANNER = stringPreferencesKey("profile_banner")
    private val PROFILE_BIO = stringPreferencesKey("profile_bio")
    private val PROFILE_PRONOUNS = stringPreferencesKey("profile_pronouns")
    private val FAV_SONG = stringPreferencesKey("fav_song")
    private val FONT_NAME = stringPreferencesKey("font_name")
    private val ONBOARDED = booleanPreferencesKey("onboarded")
    private val PAUSE_UNPLUG = booleanPreferencesKey("pause_unplug")
    private val UPDATE_CHECK = booleanPreferencesKey("update_check")
    private val PLAY_COUNT_PCT = intPreferencesKey("play_count_pct")
    private val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
    private val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
    private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val ROW_PAD = intPreferencesKey("row_pad")
    private val COVER_SIZE = intPreferencesKey("cover_size")
    private val NP_SIZE = intPreferencesKey("np_size")

    fun theme(c: Context) = c.ds.data.map { it[THEME] ?: "System24" }
    fun rounded(c: Context) = c.ds.data.map { it[ROUNDED] ?: false }
    fun folders(c: Context) = c.ds.data.map { (it[FOLDERS] ?: "").split("\n").filter{ f -> f.isNotBlank() }.toSet() }
    fun shuffle(c: Context) = c.ds.data.map { it[SHUFFLE] ?: false }
    fun repeat(c: Context) = c.ds.data.map { it[REPEAT] ?: 0 }
    fun custom(c: Context) = c.ds.data.map {
        listOf(it[CUSTOM_BG], it[CUSTOM_SURFACE], it[CUSTOM_TEXT], it[CUSTOM_SUBTEXT], it[CUSTOM_ACCENT], it[CUSTOM_BORDER])
    }
    fun welcome(c: Context) = c.ds.data.map { it[WELCOME] ?: "Welcome back." }
    fun profileName(c: Context) = c.ds.data.map { it[PROFILE_NAME] ?: "listener" }
    fun profilePic(c: Context) = c.ds.data.map { it[PROFILE_PIC] ?: "" }
    fun profileBanner(c: Context) = c.ds.data.map { it[PROFILE_BANNER] ?: "" }
    fun profileBio(c: Context) = c.ds.data.map { it[PROFILE_BIO] ?: "" }
    fun profilePronouns(c: Context) = c.ds.data.map { it[PROFILE_PRONOUNS] ?: "" }
    fun favSong(c: Context) = c.ds.data.map { it[FAV_SONG] ?: "" }
    fun fontName(c: Context) = c.ds.data.map { it[FONT_NAME] ?: "DM Mono" }
    fun onboarded(c: Context) = c.ds.data.map { it[ONBOARDED] ?: false }
    suspend fun setOnboarded(c: Context, v: Boolean) = c.ds.edit { it[ONBOARDED] = v }
    fun pauseUnplug(c: Context) = c.ds.data.map { it[PAUSE_UNPLUG] ?: true }
    suspend fun setPauseUnplug(c: Context, v: Boolean) = c.ds.edit { it[PAUSE_UNPLUG] = v }
    fun updateCheck(c: Context) = c.ds.data.map { it[UPDATE_CHECK] ?: false }
    suspend fun setUpdateCheck(c: Context, v: Boolean) = c.ds.edit { it[UPDATE_CHECK] = v }
    fun playCountPct(c: Context) = c.ds.data.map { it[PLAY_COUNT_PCT] ?: 20 }
    suspend fun setPlayCountPct(c: Context, v: Int) = c.ds.edit { it[PLAY_COUNT_PCT] = v }
    fun skipSilence(c: Context) = c.ds.data.map { it[SKIP_SILENCE] ?: false }
    suspend fun setSkipSilence(c: Context, v: Boolean) = c.ds.edit { it[SKIP_SILENCE] = v }
    fun crossfadeMs(c: Context) = c.ds.data.map { it[CROSSFADE_MS] ?: 0 }
    suspend fun setCrossfadeMs(c: Context, v: Int) = c.ds.edit { it[CROSSFADE_MS] = v }
    fun keepScreenOn(c: Context) = c.ds.data.map { it[KEEP_SCREEN_ON] ?: true }
    suspend fun setKeepScreenOn(c: Context, v: Boolean) = c.ds.edit { it[KEEP_SCREEN_ON] = v }
    fun rowPad(c: Context) = c.ds.data.map { it[ROW_PAD] ?: 8 }
    suspend fun setRowPad(c: Context, v: Int) = c.ds.edit { it[ROW_PAD] = v }
    fun coverSize(c: Context) = c.ds.data.map { it[COVER_SIZE] ?: 40 }
    suspend fun setCoverSize(c: Context, v: Int) = c.ds.edit { it[COVER_SIZE] = v }
    fun npSize(c: Context) = c.ds.data.map { it[NP_SIZE] ?: 52 }
    suspend fun setNpSize(c: Context, v: Int) = c.ds.edit { it[NP_SIZE] = v }
    suspend fun setWelcome(c: Context, v: String) = c.ds.edit { it[WELCOME] = v }
    suspend fun setProfileName(c: Context, v: String) = c.ds.edit { it[PROFILE_NAME] = v }
    suspend fun setProfilePic(c: Context, v: String) = c.ds.edit { it[PROFILE_PIC] = v }
    suspend fun setProfileBanner(c: Context, v: String) = c.ds.edit { it[PROFILE_BANNER] = v }
    suspend fun setProfileBio(c: Context, v: String) = c.ds.edit { it[PROFILE_BIO] = v }
    suspend fun setProfilePronouns(c: Context, v: String) = c.ds.edit { it[PROFILE_PRONOUNS] = v }
    suspend fun setFavSong(c: Context, v: String) = c.ds.edit { it[FAV_SONG] = v }
    suspend fun setFontName(c: Context, v: String) = c.ds.edit { it[FONT_NAME] = v }

    suspend fun setTheme(c: Context, v: String) = c.ds.edit { it[THEME] = v }
    suspend fun setRounded(c: Context, v: Boolean) = c.ds.edit { it[ROUNDED] = v }
    suspend fun setFolders(c: Context, v: Set<String>) = c.ds.edit { it[FOLDERS] = v.joinToString("\n") }
    suspend fun setShuffle(c: Context, v: Boolean) = c.ds.edit { it[SHUFFLE] = v }
    suspend fun setRepeat(c: Context, v: Int) = c.ds.edit { it[REPEAT] = v }
    suspend fun setCustom(c: Context, bg: String?, surface: String?, text: String?, subtext: String?, accent: String?, border: String?) = c.ds.edit {
        if (bg != null) it[CUSTOM_BG] = bg
        if (surface != null) it[CUSTOM_SURFACE] = surface
        if (text != null) it[CUSTOM_TEXT] = text
        if (subtext != null) it[CUSTOM_SUBTEXT] = subtext
        if (accent != null) it[CUSTOM_ACCENT] = accent
        if (border != null) it[CUSTOM_BORDER] = border
    }

    fun artistImage(c: Context, artist: String) =
        c.ds.data.map { it[stringPreferencesKey("artist_img_$artist")] ?: "" }
    suspend fun setArtistImage(c: Context, artist: String, uri: String) =
        c.ds.edit { it[stringPreferencesKey("artist_img_$artist")] = uri }
}
