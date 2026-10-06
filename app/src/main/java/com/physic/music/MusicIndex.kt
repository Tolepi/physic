package com.physic.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val uri: Uri,
    val path: String,
    val durationMs: Long,
    val dateAdded: Long = 0,
    val year: String = "",
    val track: Int = 0,
    val coverOverride: String? = null,
)

object MusicIndex {
    fun foldersOfLibrary(c: Context): Set<String> {
        val set = mutableSetOf<String>()
        val col = MediaStore.Audio.Media.RELATIVE_PATH
        c.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(col),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null, null
        )?.use { cur ->
            val i = cur.getColumnIndex(col)
            while (cur.moveToNext()) {
                val p = (cur.getString(i) ?: continue).trimEnd('/')
                if (p.isNotEmpty()) {
                    set.add(p)
                    val parts = p.split('/')
                    for (k in 1 until parts.size) set.add(parts.subList(0, k).joinToString("/"))
                }
            }
        }
        return set
    }

    fun songsInFolders(c: Context, folders: Set<String>): List<Song> {
        val out = mutableListOf<Song>()
        val cols = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
        )
        c.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cur ->
            while (cur.moveToNext()) {
                val rel = (cur.getString(6) ?: "").trimEnd('/')
                val match = folders.isEmpty() || folders.any { rel == it || rel.startsWith("$it/") }
                if (!match) continue
                val id = cur.getLong(0)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                out.add(
                    Meta.apply(c, Song(
                        id, cur.getString(1) ?: "?", cur.getString(2) ?: "?",
                        cur.getString(3) ?: "?", cur.getLong(7), uri, cur.getString(4) ?: "", cur.getLong(5),
                        cur.getLong(8), (cur.getLong(9)).let { if (it in 1L..9999L) it.toString() else "" },
                        cur.getInt(10),
                        Meta.get(c, cur.getString(4) ?: "")?.optString("cover", "")?.ifEmpty { null },
                    ) )
                )
            }
        }
        return out
    }
}
