package pl.lejdi.alc.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.material.ExperimentalMaterialApi
import com.orhanobut.hawk.Hawk
import pl.lejdi.alc.MainActivity
import pl.lejdi.alc.R
import pl.lejdi.alc.util.CurrentPlaylist
import pl.lejdi.alc.util.getHawkFiles
import pl.lejdi.alcmusicplayer.AlcProvider
import pl.lejdi.alcmusicplayer.util.Constants
import pl.lejdi.alcmusicplayer.util.Message
import pl.lejdi.alcmusicplayer.util.Mode
import java.io.Serializable


@ExperimentalMaterialApi
class ALCControlWidget : AppWidgetProvider() {

    companion object {
        private val fileObserver = FileObserver(
            R.id.widget_songtitle, R.id.widget_artist, R.id.widget_filename
        )
        private val stateObserver = MediaStateObserver(R.id.widget_button_pauseresume)
    }

    private lateinit var alc: AlcProvider

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        //create remote views
        val views = RemoteViews(
            context.packageName,
            R.layout.widget_alc
        )
        //initialize alc
        alc = AlcProvider(context)
        //initialize hawk
        Hawk.init(context).build()
        //if mode wasn't saved yet, initialize it with default value
        if (!Hawk.contains(Constants.HAWK_MODE_KEY)) {
            Hawk.put(Constants.HAWK_MODE_KEY, Mode.ALPHABETICAL)
        }
        initButtons(context, views)

        observeCurrentSong(views, appWidgetIds, appWidgetManager, context)

        //update all widgets with new remoteviews object
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    //handle clicks on widget buttons
    private fun initButtons(context: Context, views: RemoteViews) {
        val intent = Intent(context, AlcProvider.Companion.PendingIntentsForwarder::class.java)
        intent.putExtra(
            Constants.file,
            getHawkFiles() as Serializable?
        )

        intent.putExtra(Constants.intent_extra, Message.PREVIOUS)
        var pendingIntent = PendingIntent.getBroadcast(
            context,
            20,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_button_prev, pendingIntent)

        intent.putExtra(Constants.intent_extra, Message.PAUSE_OR_RESUME)
        pendingIntent = PendingIntent.getBroadcast(
            context,
            21,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_button_pauseresume, pendingIntent)

        intent.putExtra(Constants.intent_extra, Message.NEXT)
        pendingIntent = PendingIntent.getBroadcast(
            context,
            22,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_button_next, pendingIntent)

        val startActivityIntent = Intent(context, MainActivity::class.java)
        startActivityIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        pendingIntent = PendingIntent.getActivity(
            context,
            23,
            startActivityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_alc_image, pendingIntent)
    }

    //update current song data on remoteviews
    private fun observeCurrentSong(
        views: RemoteViews,
        appWidgetIds: IntArray,
        appWidgetManager: AppWidgetManager,
        context: Context
    ) {
        if (alc.currentFile.hasObservers()) {
            alc.currentFile.removeObserver(fileObserver)
        }
        if (alc.isMediaPlayerPaused.hasObservers()) {
            alc.isMediaPlayerPaused.removeObserver(stateObserver)
        }
        fileObserver.setObserver(views, appWidgetIds, appWidgetManager, context)
        stateObserver.setObserver(views, appWidgetIds, appWidgetManager)

        alc.currentFile.observeForever(fileObserver)
        alc.isMediaPlayerPaused.observeForever(stateObserver)
    }

    //receive broadcast - if ACTION_APPWIDGET_UPDATE is received - update widgets
    override fun onReceive(context: Context?, receivedIntent: Intent?) {
        val strAction = receivedIntent!!.action
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == strAction && context != null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIDs = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    ALCControlWidget::class.java
                )
            )
            onUpdate(context, appWidgetManager, appWidgetIDs)
        }
        super.onReceive(context, receivedIntent)
    }
}