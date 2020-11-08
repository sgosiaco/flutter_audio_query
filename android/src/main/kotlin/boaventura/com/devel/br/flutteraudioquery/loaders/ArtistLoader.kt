package boaventura.com.devel.br.flutteraudioquery.loaders

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask
import boaventura.com.devel.br.flutteraudioquery.sortingtypes.ArtistSortType
import io.flutter.plugin.common.MethodChannel
import java.util.*

/**
 * ArtistLoader allows make queries for artists data info.
 */
class ArtistLoader(context: Context?) : AbstractLoader(context!!) {
    /**
     * This method is used to parse ArtistSortType object into a string
     * that will be used in SQL to query data in a specific sort mode
     * @param sortType ArtistSortType The type of sort desired.
     * @return A String for SQL language query usage.
     */
    private fun parseSortOrder(sortType: ArtistSortType): String {
        val sortOrder: String
        sortOrder = when (sortType) {
            ArtistSortType.MORE_ALBUMS_NUMBER_FIRST -> MediaStore.Audio.Artists.NUMBER_OF_ALBUMS + " DESC"
            ArtistSortType.LESS_ALBUMS_NUMBER_FIRST -> MediaStore.Audio.Artists.NUMBER_OF_ALBUMS + " ASC"
            ArtistSortType.MORE_TRACKS_NUMBER_FIRST -> MediaStore.Audio.Artists.NUMBER_OF_TRACKS + " DESC"
            ArtistSortType.LESS_TRACKS_NUMBER_FIRST -> MediaStore.Audio.Artists.NUMBER_OF_TRACKS + " ASC"
            ArtistSortType.DEFAULT -> MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
            else -> MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
        }
        return sortOrder
    }

    /**
     * This method queries all artists available on device storage.
     * @param result MethodChannel.Result object to send reply for dart
     * @param sortType ArtistSortType object to define sort type for data queried
     */
    fun getArtists(result: MethodChannel.Result?, sortType: ArtistSortType) {
        createLoadTask(result, null, null,
                parseSortOrder(sortType), QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * Fetch Artists by id.
     * @param result
     * @param ids
     * @param sortType
     */
    fun getArtistsById(result: MethodChannel.Result, ids: List<String?>?,
                       sortType: ArtistSortType) {
        val selectionArgs: Array<String?>
        var sortOrder: String? = null
        var selection = MediaStore.Audio.Artists._ID
        if (ids == null || ids.isEmpty()) {
            result.error("NO_ARTIST_IDS", "No Ids was provided", null)
            return
        }
        if (ids.size > 1) {
            selectionArgs = ids.toTypedArray()
            if (sortType == ArtistSortType.CURRENT_IDs_ORDER) sortOrder = prepareIDsSortOrder(ids)
        } else {
            sortOrder = parseSortOrder(sortType)
            selection = "$selection =?"
            selectionArgs = arrayOf(ids[0])
        }
        createLoadTask(result, selection, selectionArgs,
                sortOrder, QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This method makes a query that search artists by names with
     * nameQuery query String.
     *
     * @param nameQuery The query param for match artists name
     * @param result MethodChannel.Result object to send reply for dart
     * @param sortType ArtistSortType object to define sort type for data queried.
     */
    fun searchArtistsByName(result: MethodChannel.Result?,
                            nameQuery: String, sortType: ArtistSortType) {
        val args =  /*"%" +*/"$nameQuery%"
        createLoadTask(result, MediaStore.Audio.Artists.ARTIST +
                " like ?", arrayOf(args),
                parseSortOrder(sortType), QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This methods queries artists that appears in a specific genre
     * @param result MethodChannel.Result object to send reply for dart
     * @param genreName String with genre name that you want find artist
     * @param sortType ArtistSortType object to define sort type for data queried.
     */
    fun getArtistsFromGenre(result: MethodChannel.Result?, genreName: String?,
                            sortType: ArtistSortType) {
        createLoadTask(result, genreName, null,
                parseSortOrder(sortType), QUERY_TYPE_GENRE_ARTISTS)!!.execute()
    }

    /**
     * This method creates a new ArtistTaskLoader that is used to make
     * a background query for data.
     * @param result MethodChannel.Result object to send reply for dart
     * @param selection String with SQL selection
     * @param selectionArgs Values to match '?' wildcards in selection
     * @param sortOrder ArtistSortType object to define sort type for data queried.
     * @param type An integer number that can be used to identify what kind of task do you want
     * to create.
     * @return ArtistLoadTask task ready to be executed.
     */
    override fun createLoadTask(
            result: MethodChannel.Result?, selection: String?,
            selectionArgs: Array<String?>?, sortOrder: String?, type: Int): ArtistLoadTask? {
        return ArtistLoadTask(result, getContentResolver(), selection,
                selectionArgs, sortOrder, type)
    }
    /*protected ArtistLoadTask createLoadTask(
            final EventChannel eventChannel, final String selection,
            final String[] selectionArgs, final String sortOrder, final int type){
        return null;
    }*/
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

    internal class ArtistLoadTask(private var m_result: MethodChannel.Result?, private var m_resolver: ContentResolver?, selection: String?,
                                  selectionArgs: Array<String?>?, sortOrder: String?, private val m_queryType: Int) : AbstractLoadTask<List<Map<String?, Any?>?>?>(selection, selectionArgs, sortOrder) {
        protected override fun onPostExecute(maps: List<Map<String?, Any?>?>) {
            super.onPostExecute(maps)
            m_result!!.success(maps)
            m_result = null
            m_resolver = null
        }

        /**
         * This method is called in background. Here is where the query job
         * happens.
         *
         * @param selection Selection params [WHERE param = "?="].
         * @param selectionArgs Selection args [paramValue].
         * @param sortOrder SQL sort order.
         * @return List<Map></Map><String></String>, Object>> with the query results.
         */
        override fun loadData(selection: String?,
                              selectionArgs: Array<String?>?, sortOrder: String?): List<Map<String, Any?>> {
            when (m_queryType) {
                QUERY_TYPE_DEFAULT -> return basicDataLoad(selection, selectionArgs, sortOrder)
                QUERY_TYPE_GENRE_ARTISTS -> {
                    /// in this case the genre name comes from selection param
                    val artistIds = loadArtistIdsGenre(selection)
                    val idCount = artistIds.size
                    return if (idCount > 0) {
                        if (idCount > 1) {
                            val args = artistIds.toTypedArray()
                            val createdSelection = createMultipleValueSelectionArgs(
                                    MediaStore.Audio.Artists._ID, args)
                            basicDataLoad(createdSelection, args,
                                    MediaStore.Audio.Artists.DEFAULT_SORT_ORDER)
                        } else {
                            basicDataLoad(
                                    MediaStore.Audio.Artists._ID + " =?", arrayOf(artistIds[0]),
                                    MediaStore.Audio.Artists.DEFAULT_SORT_ORDER)
                        }
                    } else ArrayList()
                }
            }
            val artistCursor = m_resolver!!.query(
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    selection, selectionArgs, sortOrder)
            val list: MutableList<Map<String, Any?>> = ArrayList()
            if (artistCursor != null) {
                while (artistCursor.moveToNext()) {
                    try {
                        val map: MutableMap<String, Any?> = HashMap()
                        for (artistColumn in PROJECTION) {
                            val data = artistCursor.getString(artistCursor.getColumnIndex(artistColumn))
                            map[artistColumn] = data
                        }
                        // some album artwork of this artist that can be used
                        // as artist cover picture if there is one.
                        map["artist_cover"] = getArtistArtPath(map[PROJECTION[1]] as String?)
                        list.add(map)
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                artistCursor.close()
            }
            return list
        }

        /**
         * This method makes the query for artists using contentResolver and
         * can be utilized for many query variations.
         *
         * @param selection Selection params [WHERE param = "?="].
         * @param selectionArgs Selection args [paramValue].
         * @param sortOrder SQL sort order.
         * @return List<Map></Map><String></String>, Object>> with the query results.
         */
        private fun basicDataLoad(
                selection: String?, selectionArgs: Array<String?>?, sortOrder: String?): List<Map<String, Any?>> {
            val artistCursor = m_resolver!!.query(
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    PROJECTION,  /*where clause*/
                    selection,  /*where clause arguments */
                    selectionArgs,
                    sortOrder)
            val list: MutableList<Map<String, Any?>> = ArrayList()
            if (artistCursor != null) {
                while (artistCursor.moveToNext()) {
                    try {
                        val map: MutableMap<String, Any?> = HashMap()
                        for (artistColumn in PROJECTION) {
                            val data = artistCursor.getString(artistCursor.getColumnIndex(artistColumn))
                            map[artistColumn] = data
                        }
                        // some album artwork of this artist that can be used
                        // as artist cover picture if there is one.
                        map["artist_cover"] = getArtistArtPath(map[PROJECTION[1]] as String?)
                        //Log.i("MDGB", "getting: " +  (String) map.get(MediaStore.Audio.Media.ARTIST));
                        list.add(map)
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                artistCursor.close()
            }
            return list
        }

        /**
         * Method used to get some album artwork image path from an specific artist
         * and this image can be used as artist cover.
         *
         * @param artistName name of artist
         * @return Path String from some album from artist or null if there is no one.
         */
        private fun getArtistArtPath(artistName: String?): String? {
            var artworkPath: String? = null
            val artworkCursor = m_resolver!!.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, arrayOf(
                    MediaStore.Audio.AlbumColumns.ALBUM_ART,
                    MediaStore.Audio.AlbumColumns.ARTIST
            ),
                    MediaStore.Audio.AlbumColumns.ARTIST + "=?", arrayOf(artistName),
                    MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)
            if (artworkCursor != null) {
                //Log.i(TAG, "total paths " + artworkCursor.getCount());
                while (artworkCursor.moveToNext()) {
                    try {
                        artworkPath = artworkCursor.getString(
                                artworkCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
                        )

                        // breaks in first valid path founded.
                        if (artworkPath != null) break
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                //Log.i(TAG, "found path: " + artworkPath );
                artworkCursor.close()
            }
            return artworkPath
        }

        /**
         * This methods query artist Id's filtered by a specific genre
         * in Media "TABLE".
         *
         * @param genreName genre name to filter artists.
         * @return List of strings with artist Id's
         */
        private fun loadArtistIdsGenre(genreName: String?): List<String?> {
            //Log.i("MDBG",  "Genero: " + genreName +" Artistas: ");
            val artistsIds: MutableList<String?> = ArrayList()
            val artistNamesCursor = m_resolver!!.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf("Distinct " + MediaStore.Audio.Media.ARTIST_ID, "genre_name"),
                    "genre_name" + " =?", arrayOf(genreName), null)
            if (artistNamesCursor != null) {
                while (artistNamesCursor.moveToNext()) {
                    try {
                        val artistName = artistNamesCursor.getString(artistNamesCursor.getColumnIndex(
                                MediaStore.Audio.Media.ARTIST_ID))
                        artistsIds.add(artistName)
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                artistNamesCursor.close()
            }
            return artistsIds
        }
    }

    companion object {
        private const val QUERY_TYPE_GENRE_ARTISTS = 0x01

        //private static final int QUERY_TYPE_SEARCH_BY_NAME = 0x02;
        private val PROJECTION = arrayOf(
                MediaStore.Audio.AudioColumns._ID,  // row id
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS,
                MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS)
    }
}