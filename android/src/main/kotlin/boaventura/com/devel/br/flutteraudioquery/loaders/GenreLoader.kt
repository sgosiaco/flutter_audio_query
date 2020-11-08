package boaventura.com.devel.br.flutteraudioquery.loaders

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask
import boaventura.com.devel.br.flutteraudioquery.sortingtypes.GenreSortType
import io.flutter.plugin.common.MethodChannel
import java.util.*

class GenreLoader(context: Context?) : AbstractLoader(context!!) {
    override fun createLoadTask(
            result: MethodChannel.Result?, selection: String?,
            selectionArgs: Array<String?>?, sortOrder: String?, type: Int): GenreLoader.GenreLoadTask? {
        return GenreLoader.GenreLoadTask(result, getContentResolver(),
                selection, selectionArgs, sortOrder)
    }

    /**
     * This method is used to parse GenreSortType object into a string
     * that will be used in SQL to query data in a specific sort mode
     * @param sortType GenreSortType The type of sort desired.
     * @return A String for SQL language query usage.
     */
    private fun parseSortOrder(sortType: GenreSortType): String {
        val sortOrder: String
        sortOrder = when (sortType) {
            GenreSortType.DEFAULT -> GENRE_PROJECTION[0] + " ASC"
            else -> GENRE_PROJECTION[0] + " ASC"
        }
        return sortOrder
    }

    /**
     * This method queries for all genre available on device storage.
     * @param result MethodChannel.Result object to send reply for dart
     * @param sortType GenreSortType object to define sort type for data queried.
     */
    fun getGenres(result: MethodChannel.Result?, sortType: GenreSortType) {
        createLoadTask(result, null, null, parseSortOrder(sortType),
                QUERY_TYPE_DEFAULT)!!.execute()
    }

    /**
     * This method makes a query that search genre by name with
     * nameQuery as query String.
     * @param results MethodChannel.Result object to send reply for dart
     * @param namedQuery The query param that will match genre name
     * @param sortType GenreSortType object to define sort type for data queried.
     */
    fun searchGenres(results: MethodChannel.Result?, namedQuery: String,
                     sortType: GenreSortType) {
        val args = arrayOf<String?>("$namedQuery%")
        createLoadTask(results, GENRE_PROJECTION[0] + " like ?", args,
                parseSortOrder(sortType), QUERY_TYPE_DEFAULT)!!.execute()
    }

    internal class GenreLoadTask(
            /**
             * Constructor for AbstractLoadTask.
             *
             * @param selection     SQL selection param. WHERE clauses.
             * @param selectionArgs SQL Where clauses query values.
             * @param sortOrder     Ordering.
             */
            private var m_result: MethodChannel.Result?, private var m_resolver: ContentResolver?, selection: String?,
            selectionArgs: Array<String?>?, sortOrder: String?) : AbstractLoadTask<List<Map<String?, Any?>?>?>(selection, selectionArgs, sortOrder) {
        override fun loadData(selection: String?, selectionArgs: Array<String?>?,
                              sortOrder: String?): List<Map<String, Any>> {
            val dataList: MutableList<Map<String, Any>> = ArrayList()
            var genreCursor: Cursor? = null
            try {
                genreCursor = m_resolver!!.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, arrayOf("Distinct " + GENRE_PROJECTION[0]), selection,
                        selectionArgs, sortOrder)
                if (genreCursor != null) {
                    while (genreCursor.moveToNext()) {
                        val data: MutableMap<String, Any> = HashMap()
                        for (column in genreCursor.columnNames) {
                            val genreName = genreCursor.getString(
                                    genreCursor.getColumnIndex(column))
                            data[MediaStore.Audio.GenresColumns.NAME] = genreName
                        }
                        dataList.add(data)
                    }
                    genreCursor.close()
                }
            } catch (ex: RuntimeException) {
                Log.e(TAG_ERROR, "GenreLoader::loadData method exception")
                //Log.e(TAG_ERROR, ex.getMessage() );
            }
            return dataList
        }

        protected override fun onPostExecute(data: List<Map<String?, Any?>?>) {
            super.onPostExecute(data)
            m_result!!.success(data)
            m_result = null
            m_resolver = null
        }

        protected override fun createMultipleValueSelectionArgs(column: String, params: Array<String?>): String? {
            return null
        }
    }

    companion object {
        private val GENRE_PROJECTION = arrayOf( //            "name",
                MediaStore.Audio.GenresColumns.NAME)
    }
}