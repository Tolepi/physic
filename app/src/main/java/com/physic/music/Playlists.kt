package com.physic.music

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object Playlists {
    private const val FILE = "playlists.json"
    private const val COVERS_FILE = "playlist_covers.json"
    private fun file(c: Context) = File(c.filesDir, FILE)
    private fun coversFile(c: Context) = File(c.filesDir, COVERS_FILE)

    private fun load(c: Context): JSONObject =
        try { JSONObject(file(c).readText()) } catch (e: Exception) { JSONObject() }

    private fun save(c: Context, j: JSONObject) = file(c).writeText(j.toString())

    private fun loadCovers(c: Context): JSONObject =
        try { JSONObject(coversFile(c).readText()) } catch (e: Exception) { JSONObject() }

    private fun saveCovers(c: Context, j: JSONObject) = coversFile(c).writeText(j.toString())

    fun names(c: Context): List<String> = load(c).keys().asSequence().sorted().toList()

    fun paths(c: Context, name: String): List<String> {
        val arr = load(c).optJSONArray(name) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i -> arr.optString(i, "").takeIf { it.isNotEmpty() } }
    }

    fun create(c: Context, name: String) {
        val j = load(c)
        if (!j.has(name)) { j.put(name, JSONArray()); save(c, j) }
    }

    fun add(c: Context, name: String, path: String) {
        val j = load(c)
        val arr = j.optJSONArray(name) ?: JSONArray().also { j.put(name, it) }
        for (i in 0 until arr.length()) if (arr.optString(i) == path) return
        arr.put(path)
        save(c, j)
    }

    fun move(c: Context, name: String, from: Int, to: Int) {
        val j = load(c)
        val arr = j.optJSONArray(name) ?: return
        if (from !in 0 until arr.length() || to !in 0 until arr.length()) return
        // JSONArray.put(index, value) replaces instead of inserting, which used to eat songs.
        val items = (0 until arr.length()).map { arr.getString(it) }.toMutableList()
        val moved = items.removeAt(from)
        items.add(to, moved)
        val out = JSONArray()
        items.forEach { out.put(it) }
        j.put(name, out)
        save(c, j)
    }

    fun remove(c: Context, name: String, index: Int) {
        val j = load(c)
        val arr = j.optJSONArray(name) ?: return
        if (index in 0 until arr.length()) { arr.remove(index); save(c, j) }
    }

    fun delete(c: Context, name: String) {
        val j = load(c)
        j.remove(name)
        save(c, j)
        removeCover(c, name)
    }

    fun rename(c: Context, old: String, new: String): Boolean {
        if (old == new) return true
        val j = load(c)
        if (!j.has(old) || j.has(new)) return false
        val arr = j.optJSONArray(old) ?: return false
        j.remove(old)
        j.put(new, arr)
        save(c, j)
        val covers = loadCovers(c)
        val coverName = covers.optString(old, "")
        if (coverName.isNotEmpty()) {
            covers.remove(old)
            covers.put(new, coverName)
            saveCovers(c, covers)
        }
        return true
    }

    /** Absolute path to the playlist cover, or "" when there is none. */
    fun cover(c: Context, name: String): String {
        val fileName = loadCovers(c).optString(name, "")
        if (fileName.isEmpty()) return ""
        val f = File(File(c.filesDir, "playlist_covers"), fileName)
        return if (f.exists()) f.absolutePath else ""
    }

    fun setCoverFromUri(c: Context, name: String, uri: Uri): String? {
        if (!load(c).has(name)) return null
        return try {
            val dir = File(c.filesDir, "playlist_covers").apply { mkdirs() }
            val file = File.createTempFile("cover_", ".img", dir)
            val input = c.contentResolver.openInputStream(uri)
            if (input == null) {
                file.delete()
                return null
            }
            input.use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }
            if (file.length() == 0L) {
                file.delete()
                return null
            }
            val old = loadCovers(c).optString(name, "")
            setCover(c, name, file.name)
            if (old.isNotEmpty()) deleteCoverFile(c, old)
            file.name
        } catch (e: Exception) {
            null
        }
    }

    fun setCover(c: Context, name: String, fileName: String) {
        val j = loadCovers(c)
        j.put(name, fileName)
        saveCovers(c, j)
    }

    fun removeCover(c: Context, name: String) {
        val j = loadCovers(c)
        val old = j.optString(name, "")
        j.remove(name)
        saveCovers(c, j)
        if (old.isNotEmpty()) deleteCoverFile(c, old)
    }

    private fun deleteCoverFile(c: Context, fileName: String) {
        try {
            val dir = File(c.filesDir, "playlist_covers").canonicalFile
            val f = File(dir, fileName).canonicalFile
            if (f.parentFile == dir) f.delete()
        } catch (_: Exception) {}
    }

    /** Export all playlists as one JSON string. */
    fun exportJson(c: Context): String = load(c).toString(2)

    /** Import JSON shaped like { "name": ["/path/a", ...] }. Merges into existing. */
    fun importJson(c: Context, text: String) {
        val src = try { JSONObject(text) } catch (e: Exception) { return }
        val dst = load(c)
        src.keys().forEach { name ->
            val arr = src.optJSONArray(name) ?: return@forEach
            val merged = dst.optJSONArray(name) ?: JSONArray()
            for (i in 0 until arr.length()) merged.put(arr.getString(i))
            dst.put(name, merged)
        }
        save(c, dst)
    }
}
