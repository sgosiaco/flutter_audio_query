package boaventura.com.devel.br.flutteraudioquery.loaders

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask
import boaventura.com.devel.br.flutteraudioquery.sortingtypes.PlaylistSortType
import io.flutter.plugin.common.MethodChannel
import java.util.*

class PlaylistLoader(context: Context?) : AbstractLoader(context!!) {
    enum class PlayListMethodType {
        READ, WRITE
    }

    /**
     * This method get all playlists available on device storage
     * @param result  MethodChannel.Result object to send reply for dart
     * @param sortType PlaylistSortType object to define sort type for data queried.
     */
    fun getPlaylists(result: MethodChannel.Result?, sortType: PlaylistSortType) {
        createLoadTask(result, null, null,
                parseSortType(sortType), QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This method is used to parse PlaylistSortType object into a string
     * that will be used in SQL to query data in a specific sort mode.
     * @param sortType PlaylistSortType The type of sort desired.
     * @return A String for SQL language query usage.
     */
    private fun parseSortType(sortType: PlaylistSortType): String? {
        var sortOrder: String? = null
        when (sortType) {
            PlaylistSortType.DEFAULT -> sortOrder = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
            PlaylistSortType.NEWEST_FIRST -> sortOrder = MediaStore.Audio.Playlists.DATE_ADDED + " DESC"
            PlaylistSortType.OLDEST_FIRST -> sortOrder = MediaStore.Audio.Playlists.DATE_ADDED + " ASC"
            else -> {
            }
        }
        return sortOrder
    }

    /**
     * This method gets a specific playlist using it id.
     * @param result MethodChannel.Result object to send reply for dart.
     * @param playlistId Id of playlist.
     */
    private fun getPlaylistById(result: MethodChannel.Result, playlistId: String) {
        createLoadTask(result, MediaStore.Audio.Playlists._ID + " =?", arrayOf(playlistId),
                null, QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This method query playlist using name as qyery parameter.
     * @param results MethodChannel.Result object to send reply for dart.
     * @param namedQuery query param.
     * @param sortType PlaylistSortType The type of sort desired.
     */
    fun searchPlaylists(results: MethodChannel.Result?, namedQuery: String,
                        sortType: PlaylistSortType) {
        val args = arrayOf<String?>("$namedQuery%")
        createLoadTask(results, MediaStore.Audio.Playlists.NAME + " like ?", args,
                parseSortType(sortType), QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This method creates a new playlist.
     * @param results MethodChannel.Result object to send reply for dart.
     * @param name playlist desired name.
     */
    fun createPlaylist(results: MethodChannel.Result, name: String?) {
        if (name != null && name.length > 0) {
            val resolver: ContentResolver = getContentResolver()
            val selection = PLAYLIST_PROJECTION!![1].toString() + " =?"
            if (!verifyPlaylistExistence(arrayOf(PLAYLIST_PROJECTION[1]!!), selection, arrayOf(name))) {
                val values = ContentValues()
                values.put(PLAYLIST_PROJECTION[1], name)
                try {
                    val uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) updateResolver()
                    val cursor = resolver.query(
                            uri!!, PLAYLIST_PROJECTION, null, null,
                            MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER)
                    if (cursor != null) {
                        val data: MutableMap<String?, Any> = HashMap()
                        while (cursor.moveToNext()) {
                            try {
                                for (key in PLAYLIST_PROJECTION) {
                                    val dataValue = cursor.getString(cursor.getColumnIndex(key))
                                    data[key] = dataValue
                                }
                                data["memberIds"] = ArrayList<String>()
                            } catch (ex: Exception) {
                                results.error("PLAYLIST_READING_FAIL", ex.message, null)
                                cursor.close()
                            }
                        }
                        cursor.close()
                        results.success(data)
                    }
                } catch (ex: Exception) {
                    results.error("NAME_NOT_ACCEPTED", ex.message, null)
                }
            } else results.error("PLAYLIST_NAME_EXISTS", "Playlist $name already exists", null)
        } else results.error("INVALID PLAYLIST NAME", "Invalid name", null)
    }

    /**
     * This method is used to remove an entire playlist.
     * @param results MethodChannel.Result object to send reply for dart.
     * @param playlistId Playlist Id that will be removed.
     */
    fun removePlaylist(results: MethodChannel.Result, playlistId: String) {
        val resolver: ContentResolver = getContentResolver()
        try {
            val rows = resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Playlists._ID + "=?", arrayOf(playlistId))
            updateResolver()
            results.success("")
        } catch (ex: Exception) {
            results.error("PLAYLIST_DELETE_FAIL", "Was not possible remove playlist", null)
        }
    }

    /**
     * This method is used to add a song to playlist. After add song the updated playlist is
     * sent to dart side code.
     * @param results MethodChannel.Result object to send reply for dart.
     * @param playlistId Id of the playlist that we want add song
     * @param songId Id of the song that we will add to playlist..
     */
    fun addSongToPlaylist(results: MethodChannel.Result, playlistId: String,
                          songId: String?) {
        val playlistUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong())
        val base = getBase(playlistUri)
        if (base != -1) {
            val resolver: ContentResolver = getContentResolver()
            val values = ContentValues()
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songId)
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base)
            resolver.insert(playlistUri, values)
            //updateResolver();
            getPlaylistById(results, playlistId)
        } else {
            results.error("Error adding song to playlist", "base value $base", null)
        }
    }

    /**
     *
     * @param results MethodChannel.Result object to send reply for dart.
     * @param playlistId
     * @param from
     * @param to
     */
    fun moveSong(results: MethodChannel.Result,
                 playlistId: String, from: Int, to: Int) {
        if (from >= 0 && to >= 0) {
            val result = MediaStore.Audio.Playlists.Members.moveItem(getContentResolver(), playlistId.toLong(), from, to)
            if (result) {
                updateResolver()
                getPlaylistById(results, playlistId)
            } else results.error("SONG_SWAP_NO_SUCCESS", "Song swap operation was not success", null)
        } else {
            results.error("SONG_SWAP_NULL_ID", "Some song is null", null)
        }
    }

    private fun updateResolver() {
        getContentResolver().notifyChange(Uri.parse("content://media"), null)
    }

    /**
     * This method
     * @param results MethodChannel.Result object to send reply for dart.
     * @param playlistId
     * @param songId
     */
    fun removeSongFromPlaylist(results: MethodChannel.Result, playlistId: String?,
                               songId: String?) {
        if (playlistId != null && songId != null) {
            val selection = PLAYLIST_PROJECTION!![0].toString() + " = '" + playlistId + "'"
            if (!verifyPlaylistExistence(arrayOf(PLAYLIST_PROJECTION[0]!!), selection, null)) {
                results.error("Unavailable playlist", "", null)
                return
            }
            val resolver: ContentResolver = getContentResolver()
            val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong())
            val deletedRows = resolver.delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + " =?", arrayOf(songId))
            if (deletedRows > 0) {
                updateResolver()
                getPlaylistById(results, playlistId)
            } else results.error("Was not possible delete song data from this playlist", "", null)
        } else {
            results.error("Error removing song from playlist", "", null)
        }
    }

    /**
     *
     * @param playlistUri
     * @return
     */
    private fun getBase(playlistUri: Uri): Int {
        val col = arrayOf("count(*)")
        var base = -1
        val cursor: Cursor = getContentResolver().query(playlistUri, col, null, null, null)
        if (cursor != null) {
            cursor.moveToNext()
            base = cursor.getInt(0)
            base += 1
            cursor.close()
        }
        return base
    }

    /**
     * This method verify if a playlist already exists.
     * @param projection
     * @param selection
     * @param args
     * @return
     */
    private fun verifyPlaylistExistence(projection: Array<String>, selection: String, args: Array<String>?): Boolean {
        var flag = false
        val cursor: Cursor = getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                projection, selection, args, null)
        if (cursor != null && cursor.count > 0) {
            flag = true
            cursor.close()
        }
        return flag
    }

    override fun createLoadTask(
            result: MethodChannel.Result?, selection: String?, selectionArgs: Array<String?>?, sortOrder: String?, type: Int): PlaylistLoader.PlaylistLoadTask? {
        return PlaylistLoader.PlaylistLoadTask(result, getContentResolver(), selection, selectionArgs, sortOrder)
    }

    internal class PlaylistLoadTask
    /**
     * Constructor for AbstractLoadTask.
     *
     * @param selection     SQL selection param. WHERE clauses.
     * @param selectionArgs SQL Where clauses query values.
     * @param sortOrder     Ordering.
     */(private var m_result: MethodChannel.Result?, private var m_resolver: ContentResolver?,
        selection: String?, selectionArgs: Array<String?>?, sortOrder: String?) : AbstractLoadTask<List<Map<String?, Any?>?>?>(selection, selectionArgs, sortOrder) {
        override fun loadData(selection: String?, selectionArgs: Array<String?>?, sortOrder: String?): List<Map<String?, Any>> {
            val cursor = m_resolver!!.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    PLAYLIST_PROJECTION, selection, selectionArgs, sortOrder)
            val dataList: MutableList<Map<String?, Any?>?> = ArrayList()
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        val playlistData: MutableMap<String?, Any?> = HashMap()
                        for (key in PLAYLIST_PROJECTION!!) {
                            val data = cursor.getString(cursor.getColumnIndex(key))
                            //Log.d("MDBG"," READING " + key + " : " + data );
                            playlistData[key] = data
                        }
                        playlistData["memberIds"] = getPlaylistMembersId(playlistData[PLAYLIST_PROJECTION[0]] as String?. toLong ())
                        dataList.add(playlistData)
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                cursor.close()
            }
            return dataList
        }

        override fun onPostExecute(maps: List<Map<String?, Any?>?>?) {
            super.onPostExecute(maps)
            m_result!!.success(maps)
            m_result = null
            m_resolver = null
        }

        /**
         *
         * This method fetch member ids of a specific playlist.
         * @param playlistId Id of playlist
         * @return List of strings with members Ids or empty list if
         * the specified playlist has no members.
         */
        private fun getPlaylistMembersId(playlistId: Long): List<String?>? {
            val membersCursor = m_resolver!!.query(MediaStore.Audio.Playlists.Members.getContentUri(
                    "external", playlistId),
                    PLAYLIST_MEMBERS_PROJECTION,
                    null,
                    null,
                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER,
                    null)
            val memberIds: MutableList<String?> = ArrayList()
            if (membersCursor != null) {
                while (membersCursor.moveToNext()) {
                    try {
                        //for(String column : PLAYLIST_MEMBERS_PROJECTION)
                        // only getting member id yet.
                        memberIds.add(membersCursor.getString(
                                membersCursor.getColumnIndex(PLAYLIST_MEMBERS_PROJECTION!![0])))
                    } catch (ex: Exception) {
                        Log.e(TAG_ERROR, "PlaylistLoader::getPlaylistMembersId method exception")
                        Log.e(TAG_ERROR, ex.message)
                    }
                }
                membersCursor.close()
            }
            return memberIds
        }
    }

    companion object {
        private val PLAYLIST_PROJECTION: Array<String?>? = arrayOf(
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATA,
                MediaStore.Audio.Playlists.DATE_ADDED)
        private val PLAYLIST_MEMBERS_PROJECTION: Array<String?>? = arrayOf(
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER
        )
    }
}