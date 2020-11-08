package boaventura.com.devel.br.flutteraudioquery.loaders.tasks

import android.os.AsyncTask

/**
 *
 * This is the base class for classes that will do load data job on
 * background thread.
 *
 * @param <T> return type data
</T> */
abstract class AbstractLoadTask<T>
/**
 * Constructor for AbstractLoadTask.
 * @param selection SQL selection param. WHERE clauses.
 * @param selectionArgs SQL Where clauses query values.
 * @param sortOrder Ordering.
 */(//private MethodChannel.Result m_result;
        private var m_selection: String?, private var m_selectionArgs: Array<String?>?,
        private var m_sortOrder: String?) : AsyncTask<Void?, Void?, T>() {
    /**
     * Interface of method that will make your background query
     * @param selection Selection params [WHERE param = "?="].
     * @param selectionArgs Selection args [paramValue].
     * @param sortOrder Your query sort order.
     * @return return you generic data type.
     */
    protected abstract fun loadData(selection: String?,
                                    selectionArgs: Array<String?>?, sortOrder: String?): T

    override fun doInBackground(vararg params: Void?): T {
        return loadData(m_selection, m_selectionArgs, m_sortOrder)
    }

    override fun onPostExecute(data: T) {
        m_selectionArgs = null
        m_selection = null
        m_sortOrder = null
    }

    /**
     * This method should implements if useful a creation of
     * a string that can be used as selection query argument
     * for multiple values for a specific table column.
     *
     *
     * something like:
     * SELECT column1, column2, columnN FROM myTable Where id in (1,2,3,4,5,6);
     *
     * @param column A specific table column
     * @param params Array with query param values.
     * @return
     */
    protected open fun createMultipleValueSelectionArgs(column: String, params: Array<String?>): String? {
        val stringBuilder = StringBuilder()
        stringBuilder.append("$column IN(?")
        for (i in 0 until params.size - 1) stringBuilder.append(",?")
        stringBuilder.append(')')
        return stringBuilder.toString()
    }
}