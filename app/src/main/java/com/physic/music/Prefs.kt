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
    private val ROUNDED = booleanPreferencesKey("rounded")
    private val FOLDERS = stringPreferencesKey("folders")
    private val SHUFFLE = booleanPreferencesKey("shuffle")
    private val REPEAT = intPreferencesKey("repeat") // 0 off, 1 all, 2 one

    private val WELCOME = stringPreferencesKey("welcome")
    private val PROFILE_NAME = stringPreferencesKey("profile_name")
    private val PROFILE_PIC = stringPreferencesKey("profile_pic")
    private val FONT_NAME = stringPreferencesKey("font_name")
    private val ONBOARDED = booleanPreferencesKey("onboarded")
    private val PAUSE_UNPLUG = booleanPreferencesKey("pause_unplug")
    private val UPDATE_CHECK = booleanPreferencesKey("update_check")
    private val PLAY_COUNT_PCT = intPreferencesKey("play_count_pct")

    fun theme(c: Context) = c.ds.data.map { it[THEME] ?: "System24" }
    fun rounded(c: Context) = c.ds.data.map { it[ROUNDED] ?: false }
    fun folders(c: Context) = c.ds.data.map { (it[FOLDERS] ?: "").split("\n").filter{ f -> f.isNotBlank() }.toSet() }
    fun shuffle(c: Context) = c.ds.data.map { it[SHUFFLE] ?: false }
    fun repeat(c: Context) = c.ds.data.map { it[REPEAT] ?: 0 }
    fun custom(c: Context) = c.ds.data.map {
        Triple(it[CUSTOM_BG], it[CUSTOM_ACCENT], it[CUSTOM_TEXT])
    }
    fun welcome(c: Context) = c.ds.data.map { it[WELCOME] ?: "Welcome back." }
    fun profileName(c: Context) = c.ds.data.map { it[PROFILE_NAME] ?: "listener" }
    fun profilePic(c: Context) = c.ds.data.map { it[PROFILE_PIC] ?: "" }
    fun fontName(c: Context) = c.ds.data.map { it[FONT_NAME] ?: "DM Mono" }
    fun onboarded(c: Context) = c.ds.data.map { it[ONBOARDED] ?: false }
    suspend fun setOnboarded(c: Context, v: Boolean) = c.ds.edit { it[ONBOARDED] = v }
    fun pauseUnplug(c: Context) = c.ds.data.map { it[PAUSE_UNPLUG] ?: true }
    suspend fun setPauseUnplug(c: Context, v: Boolean) = c.ds.edit { it[PAUSE_UNPLUG] = v }
    fun updateCheck(c: Context) = c.ds.data.map { it[UPDATE_CHECK] ?: false }
    suspend fun setUpdateCheck(c: Context, v: Boolean) = c.ds.edit { it[UPDATE_CHECK] = v }
    fun playCountPct(c: Context) = c.ds.data.map { it[PLAY_COUNT_PCT] ?: 20 }
    suspend fun setPlayCountPct(c: Context, v: Int) = c.ds.edit { it[PLAY_COUNT_PCT] = v }
    suspend fun setWelcome(c: Context, v: String) = c.ds.edit { it[WELCOME] = v }
    suspend fun setProfileName(c: Context, v: String) = c.ds.edit { it[PROFILE_NAME] = v }
    suspend fun setProfilePic(c: Context, v: String) = c.ds.edit { it[PROFILE_PIC] = v }
    suspend fun setFontName(c: Context, v: String) = c.ds.edit { it[FONT_NAME] = v }

    suspend fun setTheme(c: Context, v: String) = c.ds.edit { it[THEME] = v }
    suspend fun setRounded(c: Context, v: Boolean) = c.ds.edit { it[ROUNDED] = v }
    suspend fun setFolders(c: Context, v: Set<String>) = c.ds.edit { it[FOLDERS] = v.joinToString("\n") }
    suspend fun setShuffle(c: Context, v: Boolean) = c.ds.edit { it[SHUFFLE] = v }
    suspend fun setRepeat(c: Context, v: Int) = c.ds.edit { it[REPEAT] = v }
    suspend fun setCustom(c: Context, bg: String?, accent: String?, text: String?) = c.ds.edit {
        if (bg != null) it[CUSTOM_BG] = bg
        if (accent != null) it[CUSTOM_ACCENT] = accent
        if (text != null) it[CUSTOM_TEXT] = text
    }

    fun artistImage(c: Context, artist: String) =
        c.ds.data.map { it[stringPreferencesKey("artist_img_$artist")] ?: "" }
    suspend fun setArtistImage(c: Context, artist: String, uri: String) =
        c.ds.edit { it[stringPreferencesKey("artist_img_$artist")] = uri }
}
