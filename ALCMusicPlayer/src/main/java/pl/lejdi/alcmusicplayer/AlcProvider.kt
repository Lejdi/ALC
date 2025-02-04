package pl.lejdi.alcmusicplayer

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.lejdi.alcmusicplayer.service.MusicService
import pl.lejdi.alcmusicplayer.util.Constants
import pl.lejdi.alcmusicplayer.util.Message
import pl.lejdi.alcmusicplayer.util.Mode
import java.io.File

class AlcProvider(private val context: Context) {

    //currently played song data
    val currentFile: MutableLiveData<File>
    val currentProgress: MutableLiveData<Int>
    val isMediaPlayerPaused: MutableLiveData<Boolean>

    companion object {
        //handle intents from outside the application (widgets, notifications)
        class PendingIntentsForwarder : BroadcastReceiver() {
            override fun onReceive(context: Context?, receivedIntent: Intent?) {
                if (receivedIntent != null) {
                    if (context != null) {
                        val message = receivedIntent.getSerializableExtra(Constants.intent_extra)
                        val intent = Intent(Constants.intent_action)
                        //if service is stopped, but broadcast is received, it means service should be started
                        if (!MusicService.isRunning) {
                            val source = receivedIntent.getStringExtra(Constants.intent_source)
                            if (source == Constants.intent_source) return
                            GlobalScope.launch {
                                val filesExtra = receivedIntent.getSerializableExtra(Constants.file)
                                if (filesExtra != null) {
                                    MusicService.setPlaylist(filesExtra as MutableList<File>)
                                }
                                startForegroundService(
                                    context,
                                    Intent(context, MusicService::class.java)
                                )
                                delay(200)
                                //support for widgets
                                val updateWidgetIntent =
                                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                                context.sendBroadcast(updateWidgetIntent)
                                //sending the actual command to service
                                intent.putExtra(Constants.intent_extra, Message.NEXT)
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                            }
                        } else {
                            intent.putExtra(Constants.intent_extra, message)
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        }
                    }
                }
            }
        }
    }

    init {
        Hawk.init(context).build()
        currentFile = MusicService.currentFile
        currentProgress = MusicService.currentProgress
        isMediaPlayerPaused = MusicService.isPaused
    }

    //sending messages from components using provider to service
    //region sendMessageToService
    private fun sendMessageToService(message: Message) {
        GlobalScope.launch {
            delay(200)
            val intent = Intent(Constants.intent_action)
            intent.putExtra(Constants.intent_extra, message)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    private fun sendMessageToService(message: Message, file: File?) {
        GlobalScope.launch {
            delay(200)
            val intent = Intent(Constants.intent_action)
            intent.putExtra(Constants.intent_extra, message)
            intent.putExtra(Constants.file, file)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    private fun sendMessageToService(message: Message, position: Int) {
        GlobalScope.launch {
            delay(200)
            val intent = Intent(Constants.intent_action)
            intent.putExtra(Constants.intent_extra, message)
            intent.putExtra(Constants.mediaplayer_position, position)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    //endregion

    //region setupALC

    //running service
    private fun startService() {
        startForegroundService(context, Intent(context, MusicService::class.java))
    }

    //setting ALC playlist
    fun setPlaylist(files: MutableList<File>) {
        MusicService.setPlaylist(files)
        if (MusicService.isRunning) {
            sendMessageToService(Message.NOTIFY_CHANGE)
        }
    }

    //setting ALC playback mode
    fun setMode(mode: Mode) {
        Hawk.put(Constants.HAWK_MODE_KEY, mode)
        if (MusicService.isRunning) {
            sendMessageToService(Message.NOTIFY_CHANGE)
        }
    }

    //getting currently set mode
    fun getMode(): Mode {
        if (Hawk.contains(Constants.HAWK_MODE_KEY)) {
            return Hawk.get(Constants.HAWK_MODE_KEY)
        }
        return Mode.ALPHABETICAL
    }

    fun setAssistant(){
        if (MusicService.isRunning) {
            sendMessageToService(Message.ASSISTANT_ENABLE)
        }
    }

    //endregion

    //region controlPlayback
    //start ALC from specific file
    fun start(file: File?) {
        if (!MusicService.isRunning) {
            startService()
        }
        sendMessageToService(Message.START, file)
    }

    //pausing or resuming ALC
    fun pauseOrResume() {
        if (MusicService.isRunning) {
            sendMessageToService(Message.PAUSE_OR_RESUME)
        } else {
            start(null)
        }
    }

    //stopping service
    fun stop() {
        if (MusicService.isRunning) {
            sendMessageToService(Message.STOP)
        }
    }

    //skipping song
    fun next() {
        if (!MusicService.isRunning) {
            startService()
        }
        sendMessageToService(Message.NEXT)
    }

    //previous song
    fun previous() {
        if (!MusicService.isRunning) {
            startService()
        }
        sendMessageToService(Message.PREVIOUS)
    }

    fun setSongProgress(progress: Int) {
        if (MusicService.isRunning) {
            sendMessageToService(Message.SEEK_TO, progress)
        }
    }

    fun getPlaylistSize(): Int {
        return MusicService.getPlaylistSize()
    }
    //endregion
}