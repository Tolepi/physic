package com.physic.music

import android.content.Context
import org.json.JSONObject
import java.io.File

object Playlists {
    private const val FILE = "playlists.json"
    private fun file(c: Context) = File(c.filesDir, FILE)

    private fun load(c: Context): JSONObject =
        try { JSONObject(file(c).readText()) } catch (e: Exception) { JSONObject() }

    private fun save(c: Context, j: JSONObject) = file(c).writeText(j.toString())

    fun names(c: Context): List<String> = load(c).keys().asSequence().sorted().toList()

    fun paths(c: Context, name: String): List<String> {
        val arr = load(c).optJSONArray(name) ?: return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun create(c: Context, name: String) {
        val j = load(c)
        if (!j.has(name)) { j.put(name, org.json.JSONArray()); save(c, j) }
    }

    fun add(c: Context, name: String, path: String) {
        val j = load(c)
        val arr = j.optJSONArray(name) ?: org.json.JSONArray().also { j.put(name, it) }
        arr.put(path)
        save(c, j)
    }

    fun move(c: Context, name: String, from: Int, to: Int) {
        val j = load(c)
        val arr = j.optJSONArray(name) ?: return
        if (from !in 0 until arr.length() || to !in 0 until arr.length()) return
        val v = arr.getString(from)
        arr.remove(from)
        arr.put(to, v)
        save(c, j)
    }

    fun remove(c: Context, name: String, index: Int) {
        val j = load(c)
        val arr = j.optJSONArray(name) ?: return
        if (index in 0 until arr.length()) { arr.remove(index); save(c, j) }
    }

    fun delete(c: Context, name: String) { val j = load(c); j.remove(name); save(c, j) }

    /** Export all playlists as one JSON string. */
    fun exportJson(c: Context): String = load(c).toString(2)

    /** Import JSON shaped like { "name": ["/path/a", ...] }. Merges into existing. */
    fun importJson(c: Context, text: String) {
        val src = try { JSONObject(text) } catch (e: Exception) { return }
        val dst = load(c)
        src.keys().forEach { name ->
            val arr = src.optJSONArray(name) ?: return@forEach
            val merged = dst.optJSONArray(name) ?: org.json.JSONArray()
            for (i in 0 until arr.length()) merged.put(arr.getString(i))
            dst.put(name, merged)
        }
        save(c, dst)
    }
}
