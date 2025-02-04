package pl.lejdi.alc.widget

import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import androidx.lifecycle.Observer
import pl.lejdi.alc.R

class MediaStateObserver(
    private val resourceButton: Int
) : Observer<Boolean> {
    private var views: RemoteViews? = null
    private var appWidgetIds: IntArray? = null
    private var appWidgetManager: AppWidgetManager? = null

    fun setObserver(
        views: RemoteViews,
        appWidgetIds: IntArray,
        appWidgetManager: AppWidgetManager
    ) {
        this.views = views
        this.appWidgetIds = appWidgetIds
        this.appWidgetManager = appWidgetManager
    }

    override fun onChanged(boolean: Boolean) {
        if (boolean) {
            views?.setImageViewResource(
                resourceButton,
                R.drawable.ic_baseline_play_circle_outline_24
            )
        } else {
            views?.setImageViewResource(
                resourceButton,
                R.drawable.ic_baseline_pause_circle_outline_24
            )
        }
        for (appWidgetId in appWidgetIds!!) {
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }
}