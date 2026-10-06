package com.physic.music

import android.content.Context
import org.json.JSONObject
import java.io.File

object Meta {
    private const val FILE = "meta_overrides.json"
    private fun file(c: Context) = File(c.filesDir, FILE)
    private fun load(c: Context): JSONObject =
        try { JSONObject(file(c).readText()) } catch (e: Exception) { JSONObject() }
    private fun save(c: Context, j: JSONObject) = file(c).writeText(j.toString())

    fun get(c: Context, path: String): JSONObject? = load(c).optJSONObject(path)

    fun set(c: Context, path: String, title: String?, artist: String?, album: String?, year: String?, track: String?, cover: String?) {
        val j = load(c)
        val o = j.optJSONObject(path) ?: JSONObject()
        if (!title.isNullOrBlank()) o.put("title", title)
        if (!artist.isNullOrBlank()) o.put("artist", artist)
        if (!album.isNullOrBlank()) o.put("album", album)
        if (!year.isNullOrBlank()) o.put("year", year)
        if (!track.isNullOrBlank()) o.put("track", track)
        if (!cover.isNullOrBlank()) o.put("cover", cover)
        j.put(path, o)
        save(c, j)
    }

    fun apply(c: Context, song: Song): Song {
        val o = get(c, song.path) ?: return song
        return song.copy(
            title = o.optString("title", song.title),
            artist = o.optString("artist", song.artist),
            album = o.optString("album", song.album),
            year = o.optString("year", song.year),
            track = o.optString("track", "").toIntOrNull() ?: song.track,
        )
    }
}
