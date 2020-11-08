package boaventura.com.devel.br.flutteraudioquery.loaders

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask
import boaventura.com.devel.br.flutteraudioquery.sortingtypes.SongSortType
import io.flutter.plugin.common.MethodChannel
import java.util.*

class SongLoader(context: Context?) : AbstractLoader(context!!) {
    /**
     * This method is used to parse SongSortType object into a string
     * that will be used in SQL to query data in a specific sort mode.
     *
     * @param sortType SongSortType The type of sort desired.
     * @return A String for SQL language query usage.
     */
    private fun parseSortOrder(sortType: SongSortType): String {
        return when (sortType) {
            SongSortType.ALPHABETIC_COMPOSER -> MediaStore.Audio.Media.COMPOSER + " ASC"
            SongSortType.GREATER_DURATION -> MediaStore.Audio.Media.DURATION + " DESC"
            SongSortType.SMALLER_DURATION -> MediaStore.Audio.Media.DURATION + " ASC"
            SongSortType.RECENT_YEAR -> MediaStore.Audio.Media.YEAR + " DESC"
            SongSortType.OLDEST_YEAR -> MediaStore.Audio.Media.YEAR + " ASC"
            SongSortType.ALPHABETIC_ARTIST -> MediaStore.Audio.Media.ARTIST_KEY
            SongSortType.ALPHABETIC_ALBUM -> MediaStore.Audio.Media.ALBUM_KEY
            SongSortType.SMALLER_TRACK_NUMBER -> MediaStore.Audio.Media.TRACK + " ASC"
            SongSortType.GREATER_TRACK_NUMBER -> MediaStore.Audio.Media.TRACK + " DESC"
            SongSortType.DISPLAY_NAME -> MediaStore.Audio.Media.DISPLAY_NAME
            SongSortType.DEFAULT -> MediaStore.Audio.Media.DEFAULT_SORT_ORDER
            else -> MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        }
    }

    /**
     * This method query for all songs available on device storage
     * @param result MethodChannel.Result object to send reply for dart
     * @param sortType SongSortType object to define sort type for data queried.
     */
    fun getSongs(result: MethodChannel.Result?, sortType: SongSortType) {
        createLoadTask(result, null, null,
                parseSortOrder(sortType), QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     *
     * This method makes a query that search genre by name with
     * nameQuery as query String.
     *
     * @param result MethodChannel.Result object to send reply for dart.
     * @param namedQuery Query param to match song title.
     * @param sortType SongSortType object to define sort type for data queried.
     */
    fun searchSongs(result: MethodChannel.Result?, namedQuery: String?,
                    sortType: SongSortType) {
        val args = arrayOf<String?>("$namedQuery%")
        createLoadTask(result, MediaStore.Audio.Media.TITLE + " like ?",
                args, parseSortOrder(sortType), QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This method fetch songs by Ids. Here it is used to fetch
     * songs that appears on specific playlist.
     *
     * @param result MethodChannel.Result object to send reply for dart.
     * @param songIds Ids of songs that will be fetched.
     */
    fun getSongsFromPlaylist(result: MethodChannel.Result?, songIds: List<String?>?) {
        val values: Array<String?>
        if (songIds != null && songIds.size > 0) {
            values = songIds.toTypedArray()
            createLoadTask(result, SONG_PROJECTION!![0].toString() + " =?", values, prepareIDsSongsSortOrder(songIds), QUERY_TYPE_DEFAULT)
                    .execute()
        } else result.success(ArrayList<Map<String, Any>>())
    }

    /**
     * This method creates a SQL CASE WHEN THEN in order to get specific songs
     * from Media table where the query results is sorted matching [songIds] list values order.
     *
     * @param songIds Song ids list
     * @return Sql String case when then or null if songIds size is not greater then 1.
     */
    private fun prepareIDsSongsSortOrder(songIds: List<String?>): String? {
        if (songIds.size == 1) return null
        val orderStr = StringBuilder("CASE ")
                .append(MediaStore.MediaColumns._ID)
                .append(" WHEN '")
                .append(songIds[0])
                .append("'")
                .append(" THEN 0")
        for (i in 1 until songIds.size) {
            orderStr.append(" WHEN '")
                    .append(songIds[i])
                    .append("'")
                    .append(" THEN ")
                    .append(i)
        }
        orderStr.append(" END, ")
                .append(MediaStore.MediaColumns._ID)
                .append(" ASC")
        return orderStr.toString()
    }

    /**
     * This method queries for all songs that appears on specific album.
     *
     * @param result MethodChannel.Result object to send reply for dart.
     * @param albumId Album id that we want fetch songs
     * @param sortType SongSortType object to define sort type for data queried.
     */
    fun getSongsFromAlbum(result: MethodChannel.Result?, albumId: String?,
                          sortType: SongSortType) {

        // Log.i("MFBG", "Art: " + artist + " album: " + albumId);
        val selection = MediaStore.Audio.Media.ALBUM_ID + " =?"
        createLoadTask(result, selection, arrayOf(albumId),
                parseSortOrder(sortType), QUERY_TYPE_ALBUM_SONGS)!!.execute()
    }

    /**
     * This method queries for songs from specific artist that appears on specific album.
     *
     * @param result MethodChannel.Result object to send reply for dart.
     * @param albumId Album id that we want fetch songs
     * @param artist Artist name that appears in album
     * @param sortType SongSortType object to define sort type for data queried.
     */
    fun getSongsFromArtistAlbum(result: MethodChannel.Result?, albumId: String?,
                                artist: String?, sortType: SongSortType) {
        val selection = (MediaStore.Audio.Media.ALBUM_ID + " =?"
                + " and " + MediaStore.Audio.Media.ARTIST + " =?")
        createLoadTask(result, selection, arrayOf(albumId, artist),
                parseSortOrder(sortType), QUERY_TYPE_ALBUM_SONGS)!!.execute()
    }

    /**
     * This method queries songs from a specific artist.
     * @param result MethodChannel.Result object to send reply for dart.
     * @param artistId Artist name that we want fetch songs.
     * @param sortType SongSortType object to define sort type for data queried.
     */
    fun getSongsFromArtist(result: MethodChannel.Result?, artistId: String?,
                           sortType: SongSortType) {
        createLoadTask(result, MediaStore.Audio.Media.ARTIST_ID + " =?", arrayOf(artistId), parseSortOrder(sortType), QUERY_TYPE_DEFAULT)
                .execute()
    }

    /**
     * This method queries songs that appears on specific genre.
     *
     * @param result MethodChannel.Result object to send reply for dart.
     * @param genre Genre name that we want songs.
     * @param sortType SongSortType object to define sort type for data queried.
     */
    fun getSongsFromGenre(result: MethodChannel.Result?, genre: String?,
                          sortType: SongSortType) {
        createLoadTask(result, genre, null,
                parseSortOrder(sortType), QUERY_TYPE_GENRE_SONGS)
                .execute()
    }

    /**
     * This method fetch songs with specified Ids.
     * @param result MethodChannel.Result object to send reply for dart.
     * @param ids Songs Ids.
     * @param sortType SongSortType object to define sort type for data queried.
     */
    fun getSongsById(result: MethodChannel.Result?, ids: List<String?>?,
                     sortType: SongSortType) {
        val selectionArgs: Array<String?>
        var sortOrder: String? = null
        var selection = MediaStore.Audio.Media._ID
        if (ids == null || ids.isEmpty()) {
            result.error("NO_SONG_IDS", "No Ids was provided", null)
            return
        }
        if (ids.size > 1) {
            selectionArgs = ids.toTypedArray()
            if (sortType == SongSortType.CURRENT_IDs_ORDER) sortOrder = prepareIDsSongsSortOrder(ids)
        } else {
            sortOrder = parseSortOrder(sortType)
            selection = "$selection =?"
            selectionArgs = arrayOf(ids[0])
        }
        createLoadTask(result, selection, selectionArgs,
                sortOrder, QUERY_TYPE_DEFAULT)!!.execute()
    }

    override fun createLoadTask(result: MethodChannel.Result?, selection: String?, selectionArgs: Array<String?>?,
                                sortOrder: String?, type: Int): SongLoader.SongTaskLoad? {
        return SongLoader.SongTaskLoad(result, getContentResolver(), selection, selectionArgs, sortOrder, type)
    }

    private class SongTaskLoad
    /**
     *
     * @param result
     * @param m_resolver
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     */ internal constructor(private var m_result: MethodChannel.Result?, private var m_resolver: ContentResolver?, selection: String?,
                             selectionArgs: Array<String?>?, sortOrder: String?, private val m_queryType: Int) : AbstractLoadTask<List<Map<String?, Any?>?>?>(selection, selectionArgs, sortOrder) {
        protected override fun onPostExecute(map: List<Map<String?, Any?>?>) {
            super.onPostExecute(map)
            m_result!!.success(map)
            m_resolver = null
            m_result = null
        }

        override fun loadData(
                selection: String?, selectionArgs: Array<String?>?,
                sortOrder: String?): List<Map<String, Any?>> {
            when (m_queryType) {
                QUERY_TYPE_DEFAULT ->                     // In this case the selection will be always by id.
                    // used for fetch songs for playlist or songs by id.
                    return if (selectionArgs != null && selectionArgs.size > 1) {
                        basicLoad(createMultipleValueSelectionArgs(MediaStore.Audio.Media._ID,
                                selectionArgs), selectionArgs, sortOrder)
                    } else basicLoad(selection, selectionArgs, sortOrder)
                QUERY_TYPE_ALBUM_SONGS ->                     //Log.i("MDBG", "new way");
                    return basicLoad(selection, selectionArgs, sortOrder)
                QUERY_TYPE_GENRE_SONGS -> {
                    val songIds = getSongIdsFromGenre(selection)
                    val idCount = songIds.size
                    if (idCount > 0) {
                        return if (idCount > 1) {
                            val args = songIds.toTypedArray()
                            val createdSelection = createMultipleValueSelectionArgs(
                                    MediaStore.Audio.Media._ID, args)
                            basicLoad(
                                    createdSelection,
                                    args, sortOrder)
                        } else {
                            basicLoad(MediaStore.Audio.Media._ID + " =?", arrayOf(songIds[0]),
                                    sortOrder)
                        }
                    }
                }
                else -> {
                }
            }
            return ArrayList()
        }

        /**
         * This method fetch song ids that appears on specific genre.
         * @param genre genre name
         * @return List of ids in string.
         */
        private fun getSongIdsFromGenre(genre: String?): List<String?> {
            val songIdsCursor = m_resolver!!.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf("Distinct " + MediaStore.Audio.Media._ID, "genre_name"),
                    "genre_name" + " =?", arrayOf(genre), null)
            val songIds: MutableList<String?> = ArrayList()
            if (songIdsCursor != null) {
                while (songIdsCursor.moveToNext()) {
                    try {
                        val id = songIdsCursor.getString(songIdsCursor.getColumnIndex(MediaStore.Audio.Media._ID))
                        songIds.add(id)
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, "SongLoader::getSonIdsFromGenre method exception")
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                songIdsCursor.close()
            }
            return songIds
        }

        private fun basicLoad(selection: String?, selectionArgs: Array<String?>?,
                              sortOrder: String?): List<Map<String, Any?>> {
            val dataList: MutableList<Map<String, Any?>> = ArrayList()
            var songsCursor: Cursor? = null
            try {
                songsCursor = m_resolver!!.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        SONG_PROJECTION, selection, selectionArgs, sortOrder)
            } catch (ex: RuntimeException) {
                println("SongLoader::basicLoad $ex")
                m_result!!.error("SONG_READ_ERROR", ex.message, null)
            }
            if (songsCursor != null) {
                val albumArtMap: MutableMap<String, String?> = HashMap()
                while (songsCursor.moveToNext()) {
                    try {
                        val songData: MutableMap<String, Any?> = HashMap()
                        for (column in songsCursor.columnNames) {
                            when (column) {
                                MediaStore.Audio.Media._ID -> {
                                    val id = songsCursor.getString(songsCursor.getColumnIndex(column))
                                    val uri = ContentUris.appendId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(), id.toLong()).build()
                                    songData["uri"] = uri.toString()
                                    songData[column] = id
                                }
                                MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.IS_PODCAST, MediaStore.Audio.Media.IS_RINGTONE, MediaStore.Audio.Media.IS_ALARM, MediaStore.Audio.Media.IS_NOTIFICATION -> songData[column] = songsCursor.getInt(songsCursor.getColumnIndex(column)) != 0
                                else -> songData[column] = songsCursor.getString(songsCursor.getColumnIndex(column))
                            }
                        }
                        val albumKey = songsCursor.getString(
                                songsCursor.getColumnIndex(SONG_PROJECTION!![4]))
                        var artPath: String?
                        if (!albumArtMap.containsKey(albumKey)) {
                            artPath = getAlbumArtPathForSong(albumKey)
                            albumArtMap[albumKey] = artPath

                            //Log.i("MDBG", "song for album  " + albumKey + "adding path: " + artPath);
                        }
                        artPath = albumArtMap[albumKey]
                        songData["album_artwork"] = artPath
                        dataList.add(songData)
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, "SongLoader::basicLoad method exception")
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                songsCursor.close()
            }
            return dataList
        }

        /**
         * This method the image of the album if exists. If there is no album artwork
         * null is returned
         * @param album Album name that we want the artwork
         * @return String with image path or null if there is no image.
         */
        private fun getAlbumArtPathForSong(album: String): String? {
            val artCursor = m_resolver!!.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    SONG_ALBUM_PROJECTION, SONG_ALBUM_PROJECTION!![0].toString() + " =?", arrayOf(album),
                    null)
            var artPath: String? = null
            if (artCursor != null) {
                while (artCursor.moveToNext()) {
                    try {
                        artPath = artCursor.getString(artCursor.getColumnIndex(SONG_ALBUM_PROJECTION[1]))
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, "SongLoader::getAlbumArtPathForSong method exception")
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                artCursor.close()
            }
            return artPath
        }
    }

    companion object {
        private const val TAG = "MDBG"
        private const val QUERY_TYPE_GENRE_SONGS = 0x01
        private const val QUERY_TYPE_ALBUM_SONGS = 0x02

        //private static final String MOST_PLAYED = "most_played"; //undocumented column
        //private static final String RECENTLY_PLAYED = "recently_played"; // undocumented column
        private val SONG_ALBUM_PROJECTION = arrayOf(
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.AlbumColumns.ALBUM_ART
        )
        private val SONG_PROJECTION = arrayOf(
                MediaStore.Audio.Media._ID,  // row id
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.IS_PODCAST,
                MediaStore.Audio.Media.IS_RINGTONE,
                MediaStore.Audio.Media.IS_ALARM,
                MediaStore.Audio.Media.IS_NOTIFICATION,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.COMPOSER,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,  // duration of the audio file in ms
                MediaStore.Audio.Media.BOOKMARK,  // position, in ms, where playback was at in last stopped
                MediaStore.Audio.Media.DATA,  // file data path
                MediaStore.Audio.Media.SIZE)
    }
}