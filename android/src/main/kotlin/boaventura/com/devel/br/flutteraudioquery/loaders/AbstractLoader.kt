package boaventura.com.devel.br.flutteraudioquery.loaders

import android.content.ContentResolver
import android.content.Context
import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask
import io.flutter.plugin.common.MethodChannel

abstract class AbstractLoader internal constructor(context: Context) {
    val contentResolver: ContentResolver = context.contentResolver

    /**
     * This method should create a new background task to run SQLite queries and return
     * the task.
     * @param result
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @param type An integer number that can be used to identify what kind of task do you want
     * to create.
     * @return
     */
    protected abstract fun createLoadTask(result: MethodChannel.Result?, selection: String?,
                                          selectionArgs: Array<String?>?, sortOrder: String?, type: Int): AbstractLoadTask<*>?

    companion object {
        const val TAG_ERROR = "ERROR"
        const val QUERY_TYPE_DEFAULT = 0x00
    }

}