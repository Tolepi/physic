package com.physic.music

import android.content.Context
import org.json.JSONObject
import java.io.File

object Stats {
    private const val FILE = "stats.json"
    private fun file(c: Context) = File(c.filesDir, FILE)

    private fun load(c: Context): JSONObject {
        return try { JSONObject(file(c).readText()) } catch (e: Exception) { JSONObject() }
    }

    private fun save(c: Context, j: JSONObject) { file(c).writeText(j.toString()) }

    fun played(c: Context, songPath: String, album: String, artist: String = "") {
        val root = load(c)
        val songs = root.optJSONObject("songs") ?: JSONObject().also { root.put("songs", it) }
        songs.put(songPath, songs.optInt(songPath, 0) + 1)
        val albums = root.optJSONObject("albums") ?: JSONObject().also { root.put("albums", it) }
        albums.put(album, albums.optInt(album, 0) + 1)
        val artists = root.optJSONObject("artists") ?: JSONObject().also { root.put("artists", it) }
        if (artist.isNotEmpty()) artists.put(artist, artists.optInt(artist, 0) + 1)
        root.put("total", root.optInt("total", 0) + 1)
        save(c, root)
    }

    fun topArtists(c: Context, n: Int = 10): List<Pair<String, Int>> {
        val a = load(c).optJSONObject("artists") ?: return emptyList()
        return a.keys().asSequence().map { it to a.optInt(it) }.sortedByDescending { it.second }.take(n).toList()
    }

    fun topSongs(c: Context, n: Int = 10): List<Pair<String, Int>> {
        val s = load(c).optJSONObject("songs") ?: return emptyList()
        return s.keys().asSequence().map { it to s.optInt(it) }.sortedByDescending { it.second }.take(n).toList()
    }

    fun topAlbums(c: Context, n: Int = 10): List<Pair<String, Int>> {
        val a = load(c).optJSONObject("albums") ?: return emptyList()
        return a.keys().asSequence().map { it to a.optInt(it) }.sortedByDescending { it.second }.take(n).toList()
    }

    fun total(c: Context) = load(c).optInt("total", 0)

    /** Import Namida-style JSON: either { "songPath": count } or { "tracks": {path: {"plays": n}} } or our own export format. */
    fun importJson(c: Context, text: String) {
        val src = try { JSONObject(text) } catch (e: Exception) { return }
        val root = load(c)
        val songs = root.optJSONObject("songs") ?: JSONObject().also { root.put("songs", it) }
        val walk: (JSONObject) -> Unit = { obj ->
            obj.keys().forEach { k ->
                val v = obj.opt(k)
                val count = when (v) {
                    is Number -> v.toInt()
                    is JSONObject -> v.optInt("plays", v.optInt("count", v.optInt("play_count", 0)))
                    else -> 0
                }
                if (count > 0 && (k.endsWith(".mp3", true) || k.endsWith(".flac", true) || k.endsWith(".m4a", true) || k.endsWith(".ogg", true) || k.contains('/'))) {
                    songs.put(k, songs.optInt(k, 0) + count)
                    root.put("total", root.optInt("total", 0) + count)
                }
            }
        }
        if (src.has("tracks")) walk(src.getJSONObject("tracks"))
        else if (src.has("songs")) walk(src.getJSONObject("songs"))
        else walk(src)
        save(c, root)
    }

    fun exportJson(c: Context): String = load(c).toString(2)
}
