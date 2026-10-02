package com.physic.music

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object Lyrics {
    /** Returns list of (timeMs, text) lines parsed from a sidecar .lrc file. */
    fun forSong(c: Context, songPath: String): List<Pair<Long, String>> {
        val base = songPath.substringBeforeLast('.')
        val candidates = listOf(
            File("$base.lrc"),
            File("$base.LRC"),
            File("$base.txt"),
        )
        for (f in candidates) {
            if (f.exists()) return try { parseLrc(f.readText()) } catch (e: Exception) { emptyList() }
        }
        // also check by searching MediaStore for .lrc matching song filename
        val name = File(songPath).nameWithoutExtension.lowercase()
        c.contentResolver.query(
            MediaStore.Files.getContentUri("external"), arrayOf(MediaStore.Files.FileColumns.DATA),
            "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?",
            arrayOf("%lrc%", "%$name%"), null
        )?.use { cur ->
            val i = cur.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            while (cur.moveToNext()) {
                val p = cur.getString(i) ?: continue
                if (p.endsWith(".lrc", true) && File(p).nameWithoutExtension.lowercase().contains(name)) {
                    return try { parseLrc(File(p).readText()) } catch (e: Exception) { emptyList() }
                }
            }
        }
        return emptyList()
    }

    private val re = Regex("\\[(\\d+):(\\d+)(?:[.:](\\d+))?](.*)")
    fun parseLrc(text: String): List<Pair<Long, String>> {
        val out = mutableListOf<Pair<Long, String>>()
        for (line in text.lines()) {
            val m = re.matchEntire(line.trim()) ?: continue
            val min = m.groupValues[1].toLong()
            val sec = m.groupValues[2].toLong()
            val frac = m.groupValues[3]
            val ms = (frac.toLongOrNull() ?: 0).let { if (frac.length == 1) it * 100 else it }
            out.add((min * 60_000 + sec * 1000 + ms) to m.groupValues[4].trim())
        }
        return out.sortedBy { it.first }
    }
}
