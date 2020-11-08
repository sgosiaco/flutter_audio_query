package boaventura.com.devel.br.flutteraudioquery.delegate

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import boaventura.com.devel.br.flutteraudioquery.loaders.*
import boaventura.com.devel.br.flutteraudioquery.sortingtypes.*
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener

///
// * AudioQueryDelegate makes a validation if a method call can be executed, permission validation and
// * requests and delegates the desired method call to a required loader class where the real call
// * happens in background
// *
// * <p>The work flow in this class is: </p>
// * <p>1) Verify if  already exists a call method to be executed. If there's we finish with a error if not
// *  we go to step 2.</p>
// *
// *  <p>2) Verify if we have system permissions to run a specific method. If permission is granted we go
// *  to step 3, if not, we make a system permission request and if permission is denied we finish with a
// *  permission_denial error other way we go to step 3.</p>
// *
// *  <p>3) After all validation process we delegate the current method call to a required Loader class
// *  to do a hard work in background. </p>
// *
// */
class AudioQueryDelegate : RequestPermissionsResultListener, AudioQueryDelegateInterface {
    private val m_permissionManager: PermissionManager?
    private var m_pendingCall: MethodCall? = null
    private var m_pendingResult: MethodChannel.Result? = null
    private val m_artistLoader: ArtistLoader?
    private val m_albumLoader: AlbumLoader?
    private val m_songLoader: SongLoader?
    private val m_genreLoader: GenreLoader?
    private val m_playlistLoader: PlaylistLoader?
    private val m_imageLoader: ImageLoader?

    private constructor(context: Context?, activity: Activity?) {
        m_artistLoader = ArtistLoader(context)
        m_albumLoader = AlbumLoader(context)
        m_songLoader = SongLoader(context)
        m_genreLoader = GenreLoader(context)
        m_playlistLoader = PlaylistLoader(context)
        m_imageLoader = ImageLoader(context)
        m_permissionManager = object : PermissionManager {
            override fun isPermissionGranted(permissionName: String?): Boolean {
                return (ContextCompat.checkSelfPermission(activity!!, permissionName!!)
                        == PackageManager.PERMISSION_GRANTED)
            }

            override fun askForPermission(permissionName: String?, requestCode: Int) {
                ActivityCompat.requestPermissions(activity!!, arrayOf(permissionName), requestCode)
            }
        }
    }

    private constructor(registrar: Registrar?) {
        m_artistLoader = ArtistLoader(registrar!!.context())
        m_albumLoader = AlbumLoader(registrar.context())
        m_songLoader = SongLoader(registrar.context())
        m_genreLoader = GenreLoader(registrar.context())
        m_playlistLoader = PlaylistLoader(registrar.context())
        m_imageLoader = ImageLoader(registrar.context())
        m_permissionManager = object : PermissionManager {
            override fun isPermissionGranted(permissionName: String?): Boolean {
                return (ActivityCompat.checkSelfPermission(registrar.activity(), permissionName!!)
                        == PackageManager.PERMISSION_GRANTED)
            }

            override fun askForPermission(permissionName: String?, requestCode: Int) {
                ActivityCompat.requestPermissions(registrar.activity(), arrayOf(permissionName), requestCode)
            }
        }
        registrar.addRequestPermissionsResultListener(this)
        registrar.addViewDestroyListener { // ideal
            Log.i("MDBG", "onViewDestroy")
            true
        }
    }

    /**
     * Method used to handle all method calls that is about artist.
     * @param call Method call
     * @param result results input
     */
    override fun artistSourceHandler(call: MethodCall, result: MethodChannel.Result) {
        if (canIbeDependency(call, result)) {
            if (m_permissionManager!!.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                clearPendencies()
                handleReadOnlyMethods(call, result)
            } else m_permissionManager.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_PERMISSION_READ_EXTERNAL)
        } else finishWithAlreadyActiveError(result)
    }

    /**
     * Method used to handle all method calls that is about album data queries.
     * @param call Method call
     * @param result results input
     */
    override fun albumSourceHandler(call: MethodCall, result: MethodChannel.Result) {
        if (canIbeDependency(call, result)) {
            if (m_permissionManager!!.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                clearPendencies()
                handleReadOnlyMethods(call, result)
            } else m_permissionManager.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_PERMISSION_READ_EXTERNAL)
        } else finishWithAlreadyActiveError(result)
    }

    /**
     * Method used to handle all method calls that is about song data queries.
     * @param call Method call
     * @param result results input
     */
    override fun songSourceHandler(call: MethodCall, result: MethodChannel.Result) {
        if (canIbeDependency(call, result)) {
            if (m_permissionManager!!.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                clearPendencies()
                handleReadOnlyMethods(call, result)
            } else m_permissionManager.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_PERMISSION_READ_EXTERNAL)
        } else finishWithAlreadyActiveError(result)
    }

    fun artworkSourceHandler(call: MethodCall, result: MethodChannel.Result) {
        if (canIbeDependency(call, result)) {
            if (m_permissionManager!!.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                clearPendencies()
                handleReadOnlyMethods(call, result)
            } else m_permissionManager.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_PERMISSION_READ_EXTERNAL)
        } else finishWithAlreadyActiveError(result)
    }

    /**
     * Method used to handle all method calls that is about genre data queries.
     * @param call Method call
     * @param result results input
     */
    override fun genreSourceHandler(call: MethodCall, result: MethodChannel.Result) {
        if (canIbeDependency(call, result)) {
            if (m_permissionManager!!.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                clearPendencies()
                handleReadOnlyMethods(call, result)
            } else m_permissionManager.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_PERMISSION_READ_EXTERNAL)
        } else finishWithAlreadyActiveError(result)
    }

    /**
     * Method used to handle all method calls that is about playlist.
     * @param call Method call
     * @param result results input
     */
    override fun playlistSourceHandler(call: MethodCall, result: MethodChannel.Result) {
        val type = PlaylistLoader.PlayListMethodType.values()[call!!.argument<Any?>(PLAYLIST_METHOD_TYPE) as Int]
        when (type) {
            PlaylistLoader.PlayListMethodType.READ -> if (canIbeDependency(call, result)) {
                if (m_permissionManager!!.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    clearPendencies()
                    handleReadOnlyMethods(call, result)
                } else m_permissionManager.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                        REQUEST_CODE_PERMISSION_READ_EXTERNAL)
            } else finishWithAlreadyActiveError(result)
            PlaylistLoader.PlayListMethodType.WRITE -> if (canIbeDependency(call, result)) {
                if (m_permissionManager!!.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    clearPendencies()
                    handleWriteMethods(call, result)
                } else m_permissionManager.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        REQUEST_CODE_PERMISSION_WRITE_EXTERNAL)
            } else finishWithAlreadyActiveError(result)
            else -> result!!.notImplemented()
        }
    }

    /**
     * This method do the real delegate work. After all validation process this method
     * delegates the calls that are read only to a required loader class where all call happen in background.
     * @param call method to be called.
     * @param result results input object.
     */
    private fun handleReadOnlyMethods(call: MethodCall?, result: MethodChannel.Result?) {
        var idList: List<String?>? = null
        when (call!!.method) {
            "getArtists" -> m_artistLoader!!.getArtists(result, ArtistSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getArtistsById" -> {
                idList = call.argument("artist_ids")
                m_artistLoader!!.getArtistsById(result!!, idList,
                        ArtistSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            }
            "getArtistsFromGenre" -> m_artistLoader!!.getArtistsFromGenre(result, call.argument<Any?>("genre_name") as String?,
                    ArtistSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "searchArtistsByName" -> m_artistLoader!!.searchArtistsByName(result,
                    (call.argument<Any?>("query") as String?)!!,
                    ArtistSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getAlbums" -> m_albumLoader!!.getAlbums(result, AlbumSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getAlbumsById" -> {
                idList = call.argument("album_ids")
                m_albumLoader!!.getAlbumsById(result!!, idList,
                        AlbumSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            }
            "getAlbumsFromArtist" -> {
                val artist = call.argument<String?>("artist")
                m_albumLoader!!.getAlbumsFromArtist(result, artist,
                        AlbumSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            }
            "getAlbumsFromGenre" -> m_albumLoader!!.getAlbumFromGenre(result, call.argument<Any?>("genre_name") as String?,
                    AlbumSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "searchAlbums" -> m_albumLoader!!.searchAlbums(result, (call.argument<Any?>("query") as String?)!!,
                    AlbumSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getSongs" -> m_songLoader!!.getSongs(result, SongSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getSongsById" -> {
                idList = call.argument("song_ids")
                m_songLoader!!.getSongsById(result, idList,
                        SongSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            }
            "getSongsFromArtist" -> m_songLoader!!.getSongsFromArtist(result, call.argument<Any?>("artist") as String?,
                    SongSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getSongsFromAlbum" -> m_songLoader!!.getSongsFromAlbum(result,
                    call.argument<Any?>("album_id") as String?,
                    SongSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getSongsFromArtistAlbum" -> m_songLoader!!.getSongsFromArtistAlbum(result,
                    call.argument<Any?>("album_id") as String?,
                    call.argument<Any?>("artist") as String?,
                    SongSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getSongsFromGenre" -> m_songLoader!!.getSongsFromGenre(result, call.argument<Any?>("genre_name") as String?,
                    SongSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getSongsFromPlaylist" -> {
                val ids = call.argument<List<String?>?>("memberIds")
                m_songLoader!!.getSongsFromPlaylist(result, ids)
            }
            "searchSongs" -> m_songLoader!!.searchSongs(result, call.argument<Any?>("query") as String?,
                    SongSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getGenres" -> m_genreLoader!!.getGenres(result, GenreSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "searchGenres" -> m_genreLoader!!.searchGenres(result, (call.argument<Any?>("query") as String?)!!,
                    GenreSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getPlaylists" -> m_playlistLoader!!.getPlaylists(result,
                    PlaylistSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "searchPlaylists" -> m_playlistLoader!!.searchPlaylists(result, (call.argument<Any?>("query") as String?)!!,
                    PlaylistSortType.values()[call.argument<Any?>(SORT_TYPE) as Int])
            "getArtwork" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resourceType = call.argument<Any?>("resource") as Int
                val resourceId = call.argument<Any?>("id") as String?
                val width = call.argument<Any?>("width") as Int
                val height = call.argument<Any?>("height") as Int
                m_imageLoader!!.searchArtworkBytes(result!!, resourceType, resourceId,
                        Size(width, height))
            } else result!!.notImplemented()
            else -> result!!.notImplemented()
        }
    }

    /**
     * This method handle all methods calls that need write something on
     * device memory.
     * @param call
     * @param result
     */
    private fun handleWriteMethods(call: MethodCall?, result: MethodChannel.Result?) {
        val playlistId: String?
        val songId: String?
        val keyPlaylistName = "playlist_name"
        val keyPlaylistId = "playlist_id"
        val keySongId = "song_id"
        val keyFromPosition = "from"
        val keyToPosition = "to"
        when (call!!.method) {
            "createPlaylist" -> {
                val name = call.argument<String?>(keyPlaylistName)
                m_playlistLoader!!.createPlaylist(result!!, name)
            }
            "addSongToPlaylist" -> {
                playlistId = call.argument(keyPlaylistId)
                songId = call.argument(keySongId)
                m_playlistLoader!!.addSongToPlaylist(result!!, playlistId!!, songId)
            }
            "removeSongFromPlaylist" -> {
                playlistId = call.argument(keyPlaylistId)
                songId = call.argument(keySongId)
                m_playlistLoader!!.removeSongFromPlaylist(result!!, playlistId, songId)
            }
            "removePlaylist" -> {
                playlistId = call.argument(keyPlaylistId)
                m_playlistLoader!!.removePlaylist(result!!, playlistId!!)
            }
            "moveSong" -> {
                playlistId = call.argument(keyPlaylistId)
                m_playlistLoader!!.moveSong(result!!, playlistId!!,
                        call.argument<Any?>(keyFromPosition) as Int,
                        call.argument<Any?>(keyToPosition) as Int
                )
            }
            else -> result!!.notImplemented()
        }
    }

    private fun canIbeDependency(call: MethodCall?, result: MethodChannel.Result?): Boolean {
        return if (!setPendingMethodAndCall(call, result)) {
            false
        } else true
    }

    private fun setPendingMethodAndCall(call: MethodCall?, result: MethodChannel.Result?): Boolean {
        //There is something that needs to be delivered...
        if (m_pendingResult != null) return false
        m_pendingCall = call
        m_pendingResult = result
        return true
    }

    private fun clearPendencies() {
        m_pendingResult = null
        m_pendingCall = null
    }

    private fun finishWithAlreadyActiveError(result: MethodChannel.Result?) {
        result!!.error(ERROR_CODE_PENDING_RESULT,
                "There is some result to be delivered", null)
    }

    private fun finishWithError(errorKey: String?, errorMsg: String?, result: MethodChannel.Result?) {
        clearPendencies()
        result!!.error(errorKey, errorMsg, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray?): Boolean {
        val permissionGranted = (grantResults!!.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        when (requestCode) {
            REQUEST_CODE_PERMISSION_READ_EXTERNAL -> if (permissionGranted) {
                handleReadOnlyMethods(m_pendingCall, m_pendingResult)
                clearPendencies()
            } else {
                finishWithError(ERROR_CODE_PERMISSION_DENIED,
                        "READ EXTERNAL PERMISSION DENIED", m_pendingResult)
            }
            REQUEST_CODE_PERMISSION_WRITE_EXTERNAL -> if (permissionGranted) {
                handleWriteMethods(m_pendingCall, m_pendingResult)
                clearPendencies()
            } else {
                finishWithError(ERROR_CODE_PERMISSION_DENIED,
                        "WRITE EXTERNAL PERMISSION DENIED", m_pendingResult)
            }
            else -> return false
        }
        return true
    }

    internal interface PermissionManager {
        fun isPermissionGranted(permissionName: String?): Boolean
        fun askForPermission(permissionName: String?, requestCode: Int)
    }

    companion object {
        private var m_instance: AudioQueryDelegate? = null
        private val ERROR_CODE_PENDING_RESULT: String? = "pending_result"
        private val ERROR_CODE_PERMISSION_DENIED: String? = "PERMISSION DENIED"
        private val SORT_TYPE: String? = "sort_type"
        private val PLAYLIST_METHOD_TYPE: String? = "method_type"
        private const val REQUEST_CODE_PERMISSION_READ_EXTERNAL = 0x01
        private const val REQUEST_CODE_PERMISSION_WRITE_EXTERNAL = 0x02
        fun instance(context: Context?, activity: Activity?): AudioQueryDelegate? {
            if (m_instance == null) m_instance = AudioQueryDelegate(context!!, activity!!)
            return m_instance
        }

        fun instance(registrar: Registrar?): AudioQueryDelegate? {
            if (m_instance == null) m_instance = AudioQueryDelegate(registrar!!)
            return m_instance
        }
    }
}

internal interface AudioQueryDelegateInterface {
    /**
     * Interface method to handle artist queries related calls
     * @param call
     * @param result
     */
    fun artistSourceHandler(call: MethodCall, result: MethodChannel.Result)

    /**
     * Interface method to handle album queries related calls
     * @param call
     * @param result
     */
    fun albumSourceHandler(call: MethodCall, result: MethodChannel.Result)

    /**
     * Interface method to handle song queries related calls
     * @param call
     * @param result
     */
    fun songSourceHandler(call: MethodCall, result: MethodChannel.Result)

    /**
     * Interface method to handle genre queries related calls
     * @param call
     * @param result
     */
    fun genreSourceHandler(call: MethodCall, result: MethodChannel.Result)

    /**
     * Interface method to handle playlist related calls
     * @param call
     * @param result
     */
    fun playlistSourceHandler(call: MethodCall, result: MethodChannel.Result)
}