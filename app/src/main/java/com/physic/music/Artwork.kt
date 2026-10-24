package com.physic.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/** Loads album art as compressed bytes so it can be embedded in media-session metadata. */
object Artwork {
    private val cache = ConcurrentHashMap<String, ByteArray>()

    fun bytes(context: Context, song: Song): ByteArray? {
        val key = song.coverOverride?.takeIf { it.isNotEmpty() } ?: "album:${song.albumId}"
        cache[key]?.let { return it }
        val loaded = load(context, song) ?: return null
        cache[key] = loaded
        return loaded
    }

    private fun load(context: Context, song: Song): ByteArray? {
        // First try the MediaStore album-art URI used by the UI.
        loadFromUri(context, song)?.let { return it }
        // Some libraries have no MediaStore cover but do have embedded artwork.
        loadEmbedded(context, song)?.let { return it }
        // Last resort: common cover files next to the song.
        return loadFolderCover(song)
    }

    private fun loadFromUri(context: Context, song: Song): ByteArray? = try {
        val input = context.contentResolver.openInputStream(albumArtUri(song)) ?: return null
        val bitmap = input.use { BitmapFactory.decodeStream(it) } ?: return null
        compress(bitmap)
    } catch (e: Exception) {
        null
    }

    private fun loadEmbedded(context: Context, song: Song): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, song.uri)
            val data = retriever.embeddedPicture ?: return null
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return data
            compress(bitmap)
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    private fun loadFolderCover(song: Song): ByteArray? {
        val dir = java.io.File(song.path).parentFile ?: return null
        val names = listOf("folder.jpg", "cover.jpg", "album.jpg", "front.jpg", "folder.png", "cover.png")
        for (name in names) {
            val file = java.io.File(dir, name)
            if (!file.isFile) continue
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                return compress(bitmap)
            } catch (e: Exception) { /* try the next name */ }
        }
        return null
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        val max = 512
        val scale = minOf(max.toFloat() / bitmap.width, max.toFloat() / bitmap.height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
