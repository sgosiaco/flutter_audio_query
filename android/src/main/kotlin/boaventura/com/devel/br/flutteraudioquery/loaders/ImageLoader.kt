package boaventura.com.devel.br.flutteraudioquery.loaders

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class ImageLoader(context: Context?) : AbstractLoader(context!!) {
    @Synchronized
    fun searchArtworkBytes(result: MethodChannel.Result, resourceType: Int,
                           id: String?, size: Size?) {
        if (id == null || id.isEmpty()) {
            result.error("NO_ID", "id is required", null)
            return
        }
        var args: Array<String>? = null
        var selection = ""
        val sortOrder: String? = null
        when (resourceType) {
            0 -> {
                selection = MediaStore.Audio.Media.ARTIST_ID + " = ? "
                args = arrayOf(id)
            }
            1 -> {
                selection = MediaStore.Audio.Media.ALBUM_ID + " = ? "
                args = arrayOf(id)
            }
            2 -> {
                selection = MediaStore.Audio.Media._ID + " = ? "
                args = arrayOf(id)
            }
        }
        ImageLoader.ImageLoadTask(result, getContentResolver(), selection, args, sortOrder, resourceType, size).execute()
    }

    override fun createLoadTask(result: MethodChannel.Result?,
                                selection: String?, selectionArgs: Array<String?>?, sortOrder: String?, type: Int): ImageLoader.ImageLoadTask? {
        return null
    }

    private class ImageLoadTask
    /**
     *
     * @param result
     * @param m_resolver
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     */ internal constructor(private var m_result: MethodChannel.Result?, private var m_resolver: ContentResolver?, selection: String?,
                             selectionArgs: Array<String?>?, sortOrder: String?, private val m_queryType: Int, private val size: Size) : AbstractLoadTask<Map<String?, Any?>?>(selection, selectionArgs, sortOrder) {
        protected override fun onPostExecute(map: Map<String?, Any?>) {
            super.onPostExecute(map)
            m_result!!.success(map)
            m_resolver = null
            m_result = null
        }

        // finds an image
        private fun findImage(cursor: Cursor?): Map<String, Any?> {
            val map: MutableMap<String, Any?> = HashMap()
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val uri = ContentUris.appendId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(),
                            cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)))
                            .build()
                    try {
                        val bitmap = m_resolver!!.loadThumbnail(uri, size, null)
                        map[key] = getBitmapBytes(bitmap)
                        bitmap.recycle()
                        break
                    } catch (ex: IOException) {
                        //Log.i("DBG_TEST", "A problem here " + ex.getMessage());
                    }
                }
            }
            if (map.isEmpty()) map[key] = null
            cursor?.close()
            return map
        }

        // extract bitmap raw bytes.
        private fun getBitmapBytes(bmp: Bitmap): ByteArray? {
            var imageBytes: ByteArray? = null
            try {
                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                imageBytes = baos.toByteArray()
                baos.close()
            } catch (ex: Exception) {
                Log.i("DBG_TEST", "Problem closing the native stream")
            }
            //String encodedImage = android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
            return imageBytes
        }

        override fun loadData(
                selection: String?, selectionArgs: Array<String?>?,
                sortOrder: String?): Map<String, Any?> {
            var cursor: Cursor? = null
            when (m_queryType) {
                0, 1 -> cursor = m_resolver!!.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media._ID), selection, selectionArgs, sortOrder)
                2 -> {
                    val map: MutableMap<String, Any?> = HashMap()
                    val uri = ContentUris.appendId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(), selectionArgs!![0]!!.toLong()).build()
                    try {
                        val bitmap = m_resolver!!.loadThumbnail(uri, size, null)
                        map[key] = getBitmapBytes(bitmap)
                    } catch (ex: IOException) {
                        //Log.i("DBG", "Problem reading song image " + ex.toString());
                    }
                    if (map.isEmpty()) map[key] = null
                    return map
                }
            }
            return findImage(cursor)
        }

        companion object {
            private const val key = "image"
        }
    }
}