package pl.lejdi.alcmusicplayer.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.*
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import pl.lejdi.alcmusicplayer.AlcProvider
import pl.lejdi.alcmusicplayer.R
import pl.lejdi.alcmusicplayer.util.Constants
import pl.lejdi.alcmusicplayer.util.Message
import pl.lejdi.alcmusicplayer.util.Mode
import pl.lejdi.alcmusicservice.util.PackageName
import java.io.File
import kotlin.math.abs

class MusicService : Service(), LifecycleOwner {
    companion object {
        var isRunning = false
            private set

        private var playlist = mutableListOf<File>()

        fun setPlaylist(playlist: MutableList<File>) {
            this.playlist = playlist
        }

        fun getPlaylistSize(): Int {
            return playlist.size
        }

        val currentFile = MutableLiveData<File>()
        val currentProgress = MutableLiveData<Int>()
        val isPaused = MutableLiveData<Boolean>()
    }

    //region BROADCASTS

    //receiving messages that control playback
    private val messagesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getSerializableExtra(Constants.intent_extra)
            val fileReceived = intent.getSerializableExtra(Constants.file)
            if (fileReceived != null) {
                startingFile = fileReceived as File
            }
            val position = intent.getIntExtra(Constants.mediaplayer_position, 0)
            if (message != null) {
                when (message) {
                    Message.START -> startMusic(startingFile)
                    Message.STOP -> {
                        stopService()
                    }
                    Message.NEXT -> nextSong()
                    Message.PREVIOUS -> previousSong()
                    Message.PAUSE_OR_RESUME -> pauseOrResumeMusic()
                    Message.NOTIFY_CHANGE -> {
                        startingFile =
                            if (playlist.contains(currentlyPlayed)) currentlyPlayed else null
                        initQueue(startingFile)
                        setMode()
                    }
                    Message.SEEK_TO -> {
                        setSongPosition(position)
                    }
                    Message.LAUNCH_ACTIVITY -> {
                        launchActivity()
                    }
                    Message.ASSISTANT_ENABLE -> {
                        setAssistant()
                    }
                }
            }
        }
    }

    //receive broadcasts sent when headphones are being unplugged
    private val unplugHeadphonesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (!isPaused.value!!) {
                    pauseOrResumeMusic()
                }
            }
        }
    }

    //endregion

    //region HANDLING PHONE CALLS

    //pausing music when phone is ringing
    private fun handleIncomingCalls(context: Context) {

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val phoneCallsHandler =
                object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        if (state == TelephonyManager.CALL_STATE_RINGING) {
                            if (!isPaused.value!!) {
                                pauseOrResumeMusic()
                            }
                        }
                    }
                }
            telephonyManager.registerTelephonyCallback(context.mainExecutor, phoneCallsHandler)
        } else {
            val phoneCallsHandler = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state == TelephonyManager.CALL_STATE_RINGING) {
                        if (!isPaused.value!!) {
                            pauseOrResumeMusic()
                        }
                    }
                    super.onCallStateChanged(state, phoneNumber)
                }
            }
            telephonyManager.listen(phoneCallsHandler, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    //endregion

    //region MEDIA BUTTONS

    private lateinit var mediaSession: MediaSession
    private var previousKeyDown: Long = 0L

    //handle pressing media button
    private val callback = object : MediaSession.Callback() {
        override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
            if (mediaButtonIntent.action == Intent.ACTION_MEDIA_BUTTON) {
                val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (event != null) {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (event.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                pauseOrResumeMusic()
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                nextSong()
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                previousSong()
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                pauseOrResumeMusic()
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                pauseOrResumeMusic()
                            }
                            KeyEvent.KEYCODE_HEADSETHOOK -> {
                                //if fast two clicks, then skip song, else pause or resume
                                if ((System.currentTimeMillis() - previousKeyDown) > 500) {
                                    pauseOrResumeMusic()
                                } else {
                                    nextSong()
                                }
                                previousKeyDown = System.currentTimeMillis()
                            }
                        }
                    }
                }
            }
            return super.onMediaButtonEvent(mediaButtonIntent)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(applicationContext, "ALC")
        mediaSession.setCallback(callback)
        mediaSession.isActive = true
    }

    //endregion

    //region MEDIA PLAYER

    private val queue = arrayListOf<File>()                 //songs to be played
    private val playedSongsList = mutableListOf<File>()     //songs already played
    private var currentlyPlayed: File? = null              //currently played song
    private var startingFile: File? = null                 //first file in queue
    private var mode = Mode.ALPHABETICAL                    //playback mode
    private var assistantEnabled = false
    private var mediaPlayer: MediaPlayer? = null           //media player
    private var nextSongSemaphore =
        false                   //handle too many button clicks in short time
    private var job: Job? = null

    //initialize queue
    private fun initQueue(file: File?) {
        //are there files on playlist?
        if (playlist.isNotEmpty()) {
            //cleanup previous queue
            queue.clear()
            //add files
            if (file == null || !playlist.contains(file)) {
                queue.addAll(playlist)
            } else {
                for (i in 0 until playlist.size) {
                    queue.add(playlist[(i + playlist.indexOf(file)) % playlist.size])
                }
            }
            //duplicate song in queue when added song is being currently played
            if (currentlyPlayed != null && file != null && file == currentlyPlayed) {
                queue.remove(currentlyPlayed)
            }
        }
        //remove played files - dedup
        if (playedSongsList.isNotEmpty()) {
            playedSongsList.clear()
        }
    }

    private fun setMode() {
        mode = Hawk.get(Constants.HAWK_MODE_KEY)
    }

    var assistantCoroutine: Job? = null
    var record: AudioRecord? = null

    private fun setAssistant() {
        assistantCoroutine?.let {
            record?.stop()
            record = null
            if (it.isActive) {
                it.cancel()
            }
            assistantCoroutine = null
        }

        try {
            startRecognising()
        } catch (e: java.lang.Exception) {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    launchActivity()
                }
                withContext(Dispatchers.Default) {
                    delay(500)
                    startRecognising()
                }
            }
        }
    }

    private fun startRecognising() {
        assistantEnabled = Hawk.get(Constants.HAWK_ASSISTANT)

        if (assistantEnabled) {
            val classifier = AudioClassifier.createFromFile(this@MusicService, "model.tflite")
            val audioTensor = classifier.createInputTensorAudio()
            record = classifier.createAudioRecord()
            assistantCoroutine = lifecycleScope.launch(Dispatchers.Default) {
                record?.startRecording()
                record?.let {
                    while (true) {
                        audioTensor.load(it)
                        val output = classifier.classify(audioTensor)
                        val categories = output.first().categories
                        categories.forEach { category ->
                            if (category.score > 0.9) {
                                val signal = audioTensor.tensorBuffer.floatArray
                                if (getAudioAmplitude(signal) > 0.25) {
                                    withContext(Dispatchers.Main) {
                                        handleRecognitionResult(category)
                                    }
                                    delay(1000)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getAudioAmplitude(buffer: FloatArray): Float {
        var max = 0.0f
        for (s in buffer) {
            if (abs(s) > max) {
                max = abs(s)
            }
        }
        return max
    }

    var lastOKAlice = 0L

    private suspend fun handleRecognitionResult(category: Category) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastOKAlice
        when (category.label) {
            "OK_Alice" -> {
                val soundEffectMediaPlayer: MediaPlayer = MediaPlayer.create(
                    applicationContext,
                    R.raw.putin
                )
                soundEffectMediaPlayer.setVolume(1.0f, 1.0f)
                soundEffectMediaPlayer.start()
                withContext(Dispatchers.Default) {
                    delay(500)
                    soundEffectMediaPlayer.stop()
                    soundEffectMediaPlayer.release()
                }
                lastOKAlice = currentTime
            }
            "Next" -> {
                if (timeDiff < 5000) {
                    lastOKAlice = currentTime
                    nextSong()
                }
            }
            "Start" -> {
                val isPaused = isPaused.value ?: true
                if (timeDiff < 5000 && isPaused) {
                    lastOKAlice = currentTime
                    pauseOrResumeMusic()
                }
            }
            "Stop" -> {
                val isPaused = isPaused.value ?: true
                if (timeDiff < 5000 && !isPaused) {
                    lastOKAlice = currentTime
                    pauseOrResumeMusic()
                }
            }
            "Back" -> {
                if (timeDiff < 5000) {
                    lastOKAlice = currentTime
                    previousSong()
                }
            }
            else -> {}
        }
    }

    //starting playback
    private fun startMusic(file: File?) {
        initQueue(file)
        when (mode) {
            Mode.ALPHABETICAL -> {
                nextSong()
            }
            Mode.RANDOM -> {
                //random but started not by specific song
                if (file == null) {
                    nextSong()
                } else {
                    playFile(file)
                    currentlyPlayed = file
                    if (queue.contains(file)) {
                        queue.remove(file)
                    }
                }
            }
        }
    }

    //play single file regardless of queueing logic
    private fun playFile(file: File) {
        releaseMediaPlayer()
        if (job != null) {
            job!!.cancel()
            job = null
        }
        currentProgress.value = 0
        try {
            if (!file.exists()) {
                nextSongSemaphore = false
                nextSong()
                return
            }
            mediaPlayer = MediaPlayer.create(this, file.toUri())
            updateLiveDatas(file)
            job = lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    while (true) {
                        delay(1000)
                        if (mediaPlayer != null && mediaPlayer?.isPlaying!!) {
                            currentProgress.value = mediaPlayer?.currentPosition
                        }
                    }
                }
            }
            mediaPlayer!!.setOnCompletionListener {
                nextSong()
            }
            mediaPlayer!!.setOnPreparedListener {
                it.start()
                isPaused.value = false
                nextSongSemaphore = false
            }
        } catch (e: Exception) {
        }
    }

    //do some cleanup when changing song or stopping service
    private fun releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    private fun setSongPosition(position: Int) {
        if (mediaPlayer != null) {
            if (isPaused.value!!) {
                isPaused.value = false
            }
            mediaPlayer!!.pause()
            mediaPlayer!!.seekTo(position)
            mediaPlayer!!.start()
        }
    }

    //play next song in queue
    private fun nextSong() {
        if (nextSongSemaphore)
            return
        nextSongSemaphore = true
        //maybe all songs in queue were already played?
        if (queue.size == 0) {
            initQueue(startingFile)
            //or maybe there are no files at all?
            if (queue.size == 0) {
                nextSongSemaphore = false
                stopService()
                launchActivity()
                return
            }
        }
        //if it's not starting but skipping - current song is previous now
        if (currentlyPlayed != null) {
            playedSongsList.add(currentlyPlayed!!)
        }
        //new currently played, remove it from queue
        currentlyPlayed = when (mode) {
            Mode.ALPHABETICAL -> {
                queue.removeAt(0)
            }
            Mode.RANDOM -> {
                val randomNumberTime = ((System.currentTimeMillis() % 8) + 1).toInt()
                val randomElement =
                    (((0 until queue.size).random()) * randomNumberTime) % queue.size
                queue.removeAt(randomElement)
            }
        }
        playFile(currentlyPlayed!!)
    }

    //play previous song
    private fun previousSong() {
        if (nextSongSemaphore)
            return

        nextSongSemaphore = true
        //is something is being played, add it to queue
        if (currentlyPlayed != null) {
            queue.add(0, currentlyPlayed!!)
            currentlyPlayed = null
        }

        //if something was played before, it is current song now, else simply play next
        if (playedSongsList.isNotEmpty()) {
            currentlyPlayed = playedSongsList.removeLast()
            playFile(currentlyPlayed!!)
            nextSongSemaphore = false
        } else {
            if (mode == Mode.ALPHABETICAL) {
                currentlyPlayed = queue.removeLast()
                playFile(currentlyPlayed!!)
            } else {
                nextSongSemaphore = false
                nextSong()
            }
        }
    }

    //pausing music
    private fun pauseOrResumeMusic() {
        if (mediaPlayer != null) {
            if (isPaused.value!!) {
                mediaPlayer!!.start()
            } else {
                mediaPlayer!!.pause()
            }
            isPaused.value = !isPaused.value!!
        }
    }


    //endregion

    //region DEFAULT NOTIFICATION

    private var notification: Notification? = null
    private var notificationID: Int? = null
    private lateinit var remoteViews: RemoteViews
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val defaultNotificationID = 1618
    private val defaultChannelID = "pl.lejdi.alcmusicplayer"
    private val defaultChannelName = "ALC"
    private fun initDefaultNotification() {
        //if other notification is provided, default won't be generated
        if (notification == null) {
            remoteViews = RemoteViews(packageName, R.layout.view_notification)

            notificationBuilder = NotificationCompat.Builder(
                this,
                defaultChannelID
            )
                .setOngoing(true)
                .setSmallIcon(R.drawable.alc_logo_foreground)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setNotificationSilent()
                .setContent(remoteViews)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val chan = NotificationChannel(
                    defaultChannelID,
                    defaultChannelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
                chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                notificationManager =
                    this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(chan)
            } else {
                notificationManager =
                    this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }

            notificationID = defaultNotificationID

            notification = notificationBuilder.build()


            initNotificationUpdates()

            initNotificationButtons()
        }
    }

    private fun initNotificationButtons() {
        val intent = Intent(this, AlcProvider.Companion.PendingIntentsForwarder::class.java)
        intent.putExtra(Constants.intent_source, Constants.intent_source)

        intent.putExtra(Constants.intent_extra, Message.PREVIOUS)
        var pendingIntent = PendingIntent.getBroadcast(
            this,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.notif_button_prev, pendingIntent)

        intent.putExtra(Constants.intent_extra, Message.PAUSE_OR_RESUME)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            11,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.notif_button_pauseresume, pendingIntent)

        intent.putExtra(Constants.intent_extra, Message.NEXT)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            12,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.notif_button_next, pendingIntent)

        intent.putExtra(Constants.intent_extra, Message.STOP)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            13,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.notif_button_stop_alc, pendingIntent)

        intent.putExtra(Constants.intent_extra, Message.LAUNCH_ACTIVITY)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            14,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.notif_alc_image, pendingIntent)

        notificationManager.notify(notificationID!!, notificationBuilder.build())
    }

    private fun initNotificationUpdates() {
        currentFile.observe(this, {
            remoteViews.setTextViewText(R.id.notif_filename, it.name)
            val metadataRetriever = MediaMetadataRetriever()

            var title: String
            var author: String
            try {
                metadataRetriever.setDataSource(it.path)
                title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: getString(
                        R.string.unknown_title
                    )
                author =
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: getString(
                            R.string.unknown_artist
                        )
            } catch (e: Exception) {
                title = "-"
                author = "-"
            }

            metadataRetriever.release()

            remoteViews.setTextViewText(R.id.notif_artist, author)

            remoteViews.setTextViewText(R.id.notif_songtitle, title)

            metadataRetriever.release()
            notificationManager.notify(notificationID!!, notificationBuilder.build())
        })
        isPaused.observe(this, {
            if (it) {
                remoteViews.setImageViewResource(
                    R.id.notif_button_pauseresume,
                    R.drawable.ic_baseline_play_circle_outline_24
                )
            } else {
                remoteViews.setImageViewResource(
                    R.id.notif_button_pauseresume,
                    R.drawable.ic_baseline_pause_circle_outline_24
                )
            }
            notificationManager.notify(notificationID!!, notificationBuilder.build())
        })
    }

    //endregion

    override fun onCreate() {
        super.onCreate()
        mDispatcher.onServicePreSuperOnCreate()
        initMediaSession() //media buttons
        registerBroadcastReceivers() //broadcasts
        handleIncomingCalls(applicationContext) //phone ringing
        Hawk.init(this).build() //shared prefs
        initDefaultNotification() //default notification
        setMode() //playbackmode
        setAssistant()
        startForeground(notificationID!!, notification) //running service in foreground
        isRunning = true
        isPaused.value = true
    }

    private fun registerBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                messagesReceiver,
                IntentFilter(Constants.intent_action)
            )
        registerReceiver(
            unplugHeadphonesReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    private fun stopService() {
        releaseMediaPlayer()
        queue.clear()
        playedSongsList.clear()
        currentlyPlayed = null
        isRunning = false
        isPaused.value = true
        currentFile.value = File(getString(R.string.alc_stopped))
        currentProgress.value = 0
        stopSelf()
    }

    override fun onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
        if (job != null) {
            job!!.cancel()
            job = null
        }
        assistantCoroutine?.let {
            record?.stop()
            record = null
            if (it.isActive) {
                it.cancel()
            }
            assistantCoroutine = null
        }
        notification = null
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messagesReceiver)
        unregisterReceiver(unplugHeadphonesReceiver)
    }

    override fun onStart(intent: Intent?, startId: Int) {
        mDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    private fun updateLiveDatas(file: File) {
        mediaPlayer?.currentPosition
        currentFile.value = file
    }

    private fun launchActivity() {
        val startActivityIntent: Intent? = packageManager.getLaunchIntentForPackage(
            PackageName.PACKAGE_NAME
        )
        if (startActivityIntent != null) {
            startActivityIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startActivityIntent)
        }
    }

    private val mDispatcher = ServiceLifecycleDispatcher(this)

    override fun onBind(p0: Intent?): IBinder? {
        mDispatcher.onServicePreSuperOnBind()
        return null
    }

    override val lifecycle: Lifecycle
        get() = mDispatcher.lifecycle

}