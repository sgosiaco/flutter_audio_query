package boaventura.com.devel.br.flutteraudioquery.loaders

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask
import boaventura.com.devel.br.flutteraudioquery.sortingtypes.AlbumSortType
import io.flutter.plugin.common.MethodChannel
import java.util.*

class AlbumLoader(private val context: Context?) : AbstractLoader(context!!) {
    /**
     * This method is used to parse AlbumSortType object into a string
     * that will be used in SQL to query data in a specific sort mode.
     * @param sortType AlbumSortType The type of sort desired.
     * @return A String for SQL language query usage.
     */
    private fun parseSortOrder(sortType: AlbumSortType): String {
        return when (sortType) {
            AlbumSortType.LESS_SONGS_NUMBER_FIRST -> MediaStore.Audio.Albums.NUMBER_OF_SONGS + " ASC"
            AlbumSortType.MORE_SONGS_NUMBER_FIRST -> MediaStore.Audio.Albums.NUMBER_OF_SONGS + " DESC"
            AlbumSortType.ALPHABETIC_ARTIST_NAME -> MediaStore.Audio.Albums.ARTIST
            AlbumSortType.MOST_RECENT_YEAR -> MediaStore.Audio.Albums.LAST_YEAR + " DESC"
            AlbumSortType.OLDEST_YEAR -> MediaStore.Audio.Albums.LAST_YEAR + " ASC"
            AlbumSortType.DEFAULT -> MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
            else -> MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
        }
    }

    /**
     * Fetch albums by id.
     * @param result
     * @param ids
     * @param sortType
     */
    fun getAlbumsById(result: MethodChannel.Result, ids: List<String?>?,
                      sortType: AlbumSortType) {
        val selectionArgs: Array<String?>
        var sortOrder: String? = null
        if (ids == null || ids.isEmpty()) {
            result.error("NO_ALBUM_IDS", "No Ids was provided", null)
            return
        }
        if (ids.size > 1) {
            selectionArgs = ids.toTypedArray()
            if (sortType == AlbumSortType.CURRENT_IDs_ORDER) sortOrder = prepareIDsSortOrder(ids)
        } else {
            sortOrder = parseSortOrder(sortType)
            selectionArgs = arrayOf(ids[0])
        }
        createLoadTask(result, MediaStore.Audio.Albums._ID, selectionArgs,
                sortOrder, QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This method queries all albums available on device storage
     * @param result MethodChannel.Result object to send reply for dart
     * @param sortType AlbumSortType object to define sort type for data queried.
     */
    fun getAlbums(result: MethodChannel.Result?, sortType: AlbumSortType) {
        createLoadTask(result, null, null,
                parseSortOrder(sortType), QUERY_TYPE_DEFAULT)
                ?.execute()
    }

    /**
     * Method used to query albums that appears in a specific genre
     * @param result MethodChannel.Result object to send reply for dart
     * @param genre String with genre name that you want find artist
     * @param sortType AlbumSortType object to define sort type for data queried.
     */
    fun getAlbumFromGenre(result: MethodChannel.Result?, genre: String?,
                          sortType: AlbumSortType) {
        createLoadTask(result, genre, null,
                parseSortOrder(sortType), QUERY_TYPE_GENRE_ALBUM)
                ?.execute()
    }

    /**
     *
     * This method makes a query that search album by name with
     * nameQuery as query String.
     *
     * @param results MethodChannel.Result object to send reply for dart
     * @param namedQuery The query param for match album title.
     * @param sortType AlbumSortType object to define sort type for data queried.
     */
    fun searchAlbums(results: MethodChannel.Result?, namedQuery: String,
                     sortType: AlbumSortType) {
        val args = arrayOf<String?>("$namedQuery%")
        createLoadTask(results, MediaStore.Audio.AlbumColumns.ALBUM + " like ?", args,
                parseSortOrder(sortType), QUERY_TYPE_DEFAULT)
                ?.execute()
    }

    /**
     *
     * Method used to query albums from a specific artist
     *
     * @param result MethodChannel.Result object to send reply for dart
     * @param artistName That artist id that you wanna fetch the albums.
     * @param sortType AlbumSortType object to define sort type for data queried.
     */
    fun getAlbumsFromArtist(result: MethodChannel.Result?, artistName: String?, sortType: AlbumSortType) {
        createLoadTask(result, ALBUM_PROJECTION[3] + " = ? ", arrayOf(artistName), parseSortOrder(sortType), QUERY_TYPE_ARTIST_ALBUM)
                ?.execute()
    }

    /**
     * This method creates a SQL CASE WHEN THEN in order to get specific elements
     * where the query results is sorted matching [IDs] list values order.
     *
     * @param idList Song IDs list
     * @return Sql String case when then or null if idList size is not greater then 1.
     */
    private fun prepareIDsSortOrder(idList: List<String?>): String? {
        if (idList.size == 1) return null
        val orderStr = StringBuilder("CASE ")
                .append(MediaStore.MediaColumns._ID)
                .append(" WHEN '")
                .append(idList[0])
                .append("'")
                .append(" THEN 0")
        for (i in 1 until idList.size) {
            orderStr.append(" WHEN '")
                    .append(idList[i])
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
     * This method creates a new AlbumTaskLoader that is used to make
     * a background query for data.
     *
     * @param result MethodChannel.Result object to send reply for dart.
     * @param selection String with SQL selection.
     * @param selectionArgs Values to match '?' wildcards in selection.
     * @param sortOrder AlbumSortType object to define sort type for data queried.
     * @param type An integer number that can be used to identify what kind of task do you want to create.
     *
     * @return AlbumLoadTask object ready to be executed.
     */
    override fun createLoadTask(
            result: MethodChannel.Result?, selection: String?,
            selectionArgs: Array<String?>?, sortOrder: String?, type: Int): AlbumLoadTask? {
        return AlbumLoadTask(result, context!!.contentResolver, selection, selectionArgs,
                sortOrder, type)
    }

    // removed internal
    class AlbumLoadTask(private var m_result: MethodChannel.Result?, private var m_resolver: ContentResolver?,
                                                      selection: String?, selectionArgs: Array<String?>?,
                                                      sortOrder: String?, private var m_queryType: Int) : AbstractLoadTask<List<Map<String?, Any?>?>?>(selection, selectionArgs, sortOrder) {
        /**
         * Utility method do create a multiple selection argument string.
         * By Example: "_id IN(?,?,?,?)".
         * @param params
         * @return String ready to multiple selection args matching.
         */
        private fun createMultipleValueSelectionArgs( /*String column */
                params: Array<String?>): String {
            val stringBuilder = StringBuilder()
            stringBuilder.append(MediaStore.Audio.Albums._ID + " IN(?")
            for (i in 0 until params.size - 1) stringBuilder.append(",?")
            stringBuilder.append(')')
            return stringBuilder.toString()
        }

        override fun loadData(
                selection: String?, selectionArgs: Array<String?>?,
                sortOrder: String?): List<Map<String?, Any?>?>? {
            when (m_queryType) {
                QUERY_TYPE_DEFAULT ->                     // In this case the selection will be always by id.
                    // used for fetch songs for playlist or songs by id.
                    return if (selectionArgs != null && selectionArgs.size > 1) {
                        basicDataLoad(
                                createMultipleValueSelectionArgs(selection, selectionArgs),
                                selectionArgs, sortOrder)
                    } else basicDataLoad(selection, selectionArgs, sortOrder)
                QUERY_TYPE_GENRE_ALBUM -> {
                    val albumsFromGenre = getAlbumNamesFromGenre(selection)
                    val idCount = albumsFromGenre.size
                    if (idCount > 0) {
                        return if (idCount > 1) {
                            val params = albumsFromGenre.toTypedArray()
                            val createdSelection = createMultipleValueSelectionArgs(params)
                            basicDataLoad(createdSelection, params,
                                    MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)
                        } else {
                            basicDataLoad(
                                    MediaStore.Audio.Albums._ID + " =?", arrayOf(albumsFromGenre[0]),
                                    MediaStore.Audio.Artists.DEFAULT_SORT_ORDER)
                        }
                    }
                }
                QUERY_TYPE_ARTIST_ALBUM -> return loadAlbumsInfoWithMediaSupport(selectionArgs!![0])
                else -> {
                }
            }
            return ArrayList()
        }

        private fun basicDataLoad(selection: String?, selectionArgs: Array<String?>?,
                                  sortOrder: String?): List<Map<String?, Any?>> {
            val dataList: MutableList<Map<String, Any?>> = ArrayList()
            val cursor = m_resolver!!.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    ALBUM_PROJECTION, selection, selectionArgs, sortOrder)
            if (cursor != null) {
                if (cursor.count == 0) {
                    cursor.close()
                    return dataList
                } else {
                    while (cursor.moveToNext()) {
                        try {
                            val dataMap: MutableMap<String, Any?> = HashMap()
                            for (albumColumn in ALBUM_PROJECTION) {
                                val value = cursor.getString(cursor.getColumnIndex(albumColumn))
                                dataMap[albumColumn] = value
                                //Log.i(TAG, albumColumn + ": " + value);
                            }
                            dataList.add(dataMap)
                        } catch (ex: Exception) {
                            Log.e("ERROR", "AlbumLoader::basicLoad", ex)
                            Log.e("ERROR", "while reading basic load cursor")
                        }
                    }
                }
                cursor.close()
            }
            return dataList
        }

        private fun getAlbumNamesFromGenre(genre: String?): List<String?> {
            val albumNames: MutableList<String?> = ArrayList()
            val albumNamesCursor = m_resolver!!.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf("Distinct " + MediaStore.Audio.Media.ALBUM_ID, "genre_name"),
                    "genre_name" + " =?", arrayOf(genre), null)
            if (albumNamesCursor != null) {
                while (albumNamesCursor.moveToNext()) {
                    try {
                        val albumName = albumNamesCursor.getString(albumNamesCursor.getColumnIndex(
                                MediaStore.Audio.Media.ALBUM_ID))
                        albumNames.add(albumName)
                    } catch (ex: Exception) {
                        Log.e("ERROR", "AlbumLoader::getAlbumNamesFromGenre", ex)
                    }
                }
                albumNamesCursor.close()
            }
            return albumNames
        }

        /**
         * This method is used to load albums from Media "Table" and not from Album "Table"
         * as basicDataLoad do.
         *
         * @param artistName The name of the artists that we can query for albums.
         */
        private fun loadAlbumsInfoWithMediaSupport(artistName: String?): List<Map<String, Any?>> {
            val dataList: MutableList<Map<String, Any?>> = ArrayList()

            // we get albums from an specific artist
            val artistAlbumsCursor = m_resolver!!.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    ALBUM_MEDIA_PROJECTION,
                    MediaStore.Audio.Albums.ARTIST + "=?" + " and "
                            + MediaStore.Audio.Media.IS_MUSIC + "=?"
                            + ") GROUP BY (" + MediaStore.Audio.Albums.ALBUM, arrayOf(artistName, "1"),
                    MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
            if (artistAlbumsCursor != null) {
                while (artistAlbumsCursor.moveToNext()) {
                    val albumId = artistAlbumsCursor.getString(
                            artistAlbumsCursor.getColumnIndex(ALBUM_MEDIA_PROJECTION[0]))
                    val albumDataCursor = m_resolver!!.query(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            ALBUM_PROJECTION,
                            MediaStore.Audio.Albums._ID + "=?", arrayOf(albumId),
                            MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)
                    if (albumDataCursor != null) {
                        val albumArtistSongsCountCursor = m_resolver!!.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.ARTIST,
                                MediaStore.Audio.Media.IS_MUSIC
                        ),
                                MediaStore.Audio.Artists.ARTIST + " =?" + " and " +
                                        MediaStore.Audio.Media.ALBUM_ID + " =?" + " and " +
                                        MediaStore.Audio.Media.IS_MUSIC + "=?", arrayOf(artistName, albumId, "1"), null)
                        var songsNumber = -1
                        if (albumArtistSongsCountCursor != null) {
                            songsNumber = albumArtistSongsCountCursor.count
                            albumArtistSongsCountCursor.close()
                        }
                        while (albumDataCursor.moveToNext()) {
                            try {
                                val albumData: MutableMap<String, Any?> = HashMap()

                                //MediaStore.Audio.AudioColumns._ID,
                                albumData[ALBUM_PROJECTION[0]] = albumDataCursor.getString(albumDataCursor.getColumnIndex(ALBUM_PROJECTION[0]))

                                //MediaStore.Audio.AlbumColumns.ALBUM,
                                albumData[ALBUM_PROJECTION[1]] = albumDataCursor.getString(albumDataCursor.getColumnIndex(ALBUM_PROJECTION[1]))

                                //MediaStore.Audio.AlbumColumns.ALBUM_ART,
                                albumData[ALBUM_PROJECTION[2]] = albumDataCursor.getString(albumDataCursor.getColumnIndex(ALBUM_PROJECTION[2]))

                                //MediaStore.Audio.AlbumColumns.ARTIST,
                                albumData[ALBUM_PROJECTION[3]] = artistName

                                //MediaStore.Audio.AlbumColumns.FIRST_YEAR,
                                albumData[ALBUM_PROJECTION[4]] = albumDataCursor.getString(albumDataCursor.getColumnIndex(ALBUM_PROJECTION[4]))

                                //MediaStore.Audio.AlbumColumns.LAST_YEAR,
                                albumData[ALBUM_PROJECTION[5]] = albumDataCursor.getString(albumDataCursor.getColumnIndex(ALBUM_PROJECTION[5]))

                                //MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS
                                albumData[ALBUM_PROJECTION[6]] = songsNumber.toString()

                                /*for(int i = 0; i < ALBUM_PROJECTION.length -1; i++)
                                    albumData.put(ALBUM_PROJECTION[i], albumDataCursor.
                                            getString(albumDataCursor.getColumnIndex(ALBUM_PROJECTION[i])));

                                albumData.put(ALBUM_PROJECTION[ALBUM_PROJECTION.length-1],
                                        String.valueOf(songsNumber));
                               */dataList.add(albumData)
                            } catch (ex: Exception) {
                                //TODO should I exit with results.error() here??
                                // think about it...
                                Log.e("ERROR", "AlbumLoader::loadAlbumsInfoWithMediaSupport", ex)
                            }
                        }
                        albumDataCursor.close()
                    }
                }
                artistAlbumsCursor.close()
            }
            return dataList
        }

        override fun onPostExecute(data: List<Map<String?, Any?>?>?) {
            super.onPostExecute(data)
            m_resolver = null
            m_result!!.success(data)
            m_result = null
        }
    }

    companion object {
        //private final ContentResolver m_resolver;
        private const val QUERY_TYPE_GENRE_ALBUM = 0x01
        private const val QUERY_TYPE_ARTIST_ALBUM = 0x02
        private val ALBUM_PROJECTION = arrayOf(
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.AlbumColumns.ALBUM_ART,
                MediaStore.Audio.AlbumColumns.ARTIST,
                MediaStore.Audio.AlbumColumns.FIRST_YEAR,
                MediaStore.Audio.AlbumColumns.LAST_YEAR,
                MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS /*, MediaStore.Audio.AlbumColumns.ALBUM_ID*/ //MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS_FOR_ARTIST,
        )
        private val ALBUM_MEDIA_PROJECTION = arrayOf(
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.IS_MUSIC
        )
    }
}