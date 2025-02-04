package pl.lejdi.alc.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.widget.RemoteViews
import androidx.lifecycle.Observer
import pl.lejdi.alc.R
import java.io.File

class FileObserver(
    private val resourceTitle: Int,
    private val resourceArtist: Int,
    private val resourceName: Int
) : Observer<File> {
    private var views: RemoteViews? = null
    private var appWidgetIds: IntArray? = null
    private var appWidgetManager: AppWidgetManager? = null
    private var context: Context? = null

    fun setObserver(
        views: RemoteViews,
        appWidgetIds: IntArray,
        appWidgetManager: AppWidgetManager,
        context: Context
    ) {
        this.views = views
        this.appWidgetIds = appWidgetIds
        this.appWidgetManager = appWidgetManager
        this.context = context
    }

    override fun onChanged(file: File) {
        val metadataRetriever = MediaMetadataRetriever()
        if (file != null) {
            var title: String
            var author: String
            try {
                metadataRetriever.setDataSource(file.path)
                title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: context?.getString(R.string.unknown_title)!!
                author =
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: context?.getString(R.string.unknown_artist)!!
            } catch (e: Exception) {
                title = "-"
                author = "-"
            }
            val name = file.name

            metadataRetriever.release()
            views?.setTextViewText(resourceTitle, title)
            views?.setTextViewText(resourceArtist, author)
            views?.setTextViewText(resourceName, name)
        }
        for (appWidgetId in appWidgetIds!!) {
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }
}