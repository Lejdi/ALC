package pl.lejdi.alc.ui

import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.orhanobut.hawk.Hawk
import pl.lejdi.alc.R
import pl.lejdi.alc.databinding.FragmentControlBinding
import pl.lejdi.alc.util.CurrentPlaylist
import pl.lejdi.alc.util.DarkMode
import pl.lejdi.alc.util.getHawkFiles
import pl.lejdi.alcmusicplayer.AlcProvider
import pl.lejdi.alcmusicplayer.util.Constants
import pl.lejdi.alcmusicplayer.util.Constants.Companion.HAWK_ASSISTANT
import pl.lejdi.alcmusicplayer.util.Mode
import java.io.File

@ExperimentalMaterialApi
class ControlFragment : Fragment() {
    private lateinit var binding: FragmentControlBinding
    private lateinit var alc: AlcProvider
    private var buttonSemaphore = false

    interface ControlToALCCallback {
        fun setBackButton(fileBrowserFragment: FileBrowserFragment?)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentControlBinding.inflate(inflater, container, false)
        setALC()
        retrieveDarkMode()
        observeALC()
        setButtons()
        return binding.root
    }

    private fun initializeHawk() {
        Hawk.init(requireActivity().baseContext).build()
        if (!Hawk.contains(HAWK_ASSISTANT)) {
            Hawk.put(HAWK_ASSISTANT, false)
        }
        if (!Hawk.contains(pl.lejdi.alc.util.Constants.HAWK_DARK_MODE)) {
            Hawk.put(pl.lejdi.alc.util.Constants.HAWK_DARK_MODE, DarkMode.OFF)
        }
        if (!Hawk.contains(pl.lejdi.alc.util.Constants.HAWK_ALL_PLAYLISTS_KEY)) {
            if (!Hawk.contains(Constants.HAWK_MODE_KEY)) {
                Hawk.put(Constants.HAWK_MODE_KEY, Mode.ALPHABETICAL)
            }
            Hawk.put(
                pl.lejdi.alc.util.Constants.HAWK_ALL_PLAYLISTS_KEY,
                listOf(getString(R.string.default_playlist))
            )
            Hawk.put(
                pl.lejdi.alc.util.Constants.HAWK_CURRENT_PLAYLIST_KEY,
                getString(R.string.default_playlist)
            )
        }
        CurrentPlaylist.name = Hawk.get(pl.lejdi.alc.util.Constants.HAWK_CURRENT_PLAYLIST_KEY)
    }

    private fun retrieveDarkMode() {
        when (Hawk.get<DarkMode>(pl.lejdi.alc.util.Constants.HAWK_DARK_MODE)) {
            DarkMode.OFF -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            DarkMode.ON -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            DarkMode.AUTO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    //setup ALC provider
    private fun setALC() {
        alc = AlcProvider(requireActivity().baseContext)

        initializeHawk()

        if (alc.getPlaylistSize() == 0) {
            initializePlaylist()
        }
    }

    //observe live data from ALC and actualize UI
    private fun observeALC() {
        alc.currentFile.observe(viewLifecycleOwner, {
            binding.currentSongName.text = it.name

            val metadataRetriever = MediaMetadataRetriever()
            try {
                metadataRetriever.setDataSource(it.path)
                val duration =
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                metadataRetriever.release()
                if (duration != null) {
                    binding.songProgress.max = duration.toInt()
                    binding.songDuration.text = durationToString(duration.toInt())
                }
            } catch (e: Exception) {
                binding.songProgress.max = 0
                binding.songDuration.text = "0:00"
            }
        })
        alc.currentProgress.observe(viewLifecycleOwner, {
            binding.songProgress.progress = it
            binding.currentProgress.text = durationToString(it)
        })

        binding.songProgress.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                //allow to change song progress from seekbar
                override fun onProgressChanged(
                    seekbar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        alc.setSongProgress(progress)
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            }
        )
        alc.isMediaPlayerPaused.observe(viewLifecycleOwner, {
            if (it) {
                binding.resumePauseButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else {
                binding.resumePauseButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }

        })
    }

    private fun durationToString(it: Int): String {
        val minutes = (it / 60000)
        val seconds = (it / 1000) % 60
        return if (seconds < 10) {
            "$minutes:0$seconds"
        } else {
            "$minutes:$seconds"
        }
    }

    //change image when mode is changed
    private fun setChangeModeButtonImage() {
        when (alc.getMode()) {
            Mode.ALPHABETICAL -> {
                binding.changeModeButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.blocked_dice
                    )
                )
            }
            Mode.RANDOM -> {
                binding.changeModeButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.unblocked_dice
                    )
                )
            }
        }
    }

    //setup control buttons clicks
    private fun setButtons() {
        setChangeModeButtonImage()
        binding.changeModeButton.setOnClickListener {
            when (alc.getMode()) {
                Mode.ALPHABETICAL -> {
                    alc.setMode(Mode.RANDOM)
                }
                Mode.RANDOM -> {
                    alc.setMode(Mode.ALPHABETICAL)
                }
            }
            setChangeModeButtonImage()
        }
        binding.prevButton.setOnClickListener {
            if (alc.getPlaylistSize() > 0) alc.previous()
        }
        binding.resumePauseButton.setOnClickListener {
            if (alc.getPlaylistSize() > 0) alc.pauseOrResume()
        }
        binding.nextButton.setOnClickListener {
            if (alc.getPlaylistSize() > 0) alc.next()
        }

        binding.settingsButton.setOnClickListener {
            if (!buttonSemaphore) {
                buttonSemaphore = true
                if (!SettingsFragment.isDisplayed) {
                    val settingsFragment = SettingsFragment()
                    activity?.supportFragmentManager!!
                        .beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.container, settingsFragment)
                        .commit()
                } else {
                    if (PlaylistListFragment.isDisplayed) {
                        activity?.supportFragmentManager!!.popBackStack()
                    }
                }
                buttonSemaphore = false
            }
        }

        binding.changePlaylistButton.setOnClickListener {
            if (!buttonSemaphore) {
                buttonSemaphore = true
                if (!PlaylistListFragment.isDisplayed) {
                    val playlistListFragment = PlaylistListFragment()
                    activity?.supportFragmentManager!!
                        .beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_left, R.anim.slide_out_right,
                            R.anim.slide_in_right, R.anim.slide_out_left
                        )
                        .addToBackStack(null)
                        .replace(R.id.container, playlistListFragment)
                        .commit()
                } else {
                    if (SettingsFragment.isDisplayed) {
                        activity?.supportFragmentManager!!.popBackStack()
                    }
                }
                buttonSemaphore = false
            }
        }
    }

    //public function for other fragments - setting playlist
    fun setPlaylist(files: List<File>) {
        alc.setPlaylist(files.toMutableList())
    }

    fun setAssistant(){
        alc.setAssistant()
    }

    fun initializePlaylist() {
        val files = getHawkFiles()
        setPlaylist(files)
    }

    //public function for other fragments - starting alc from chosen file
    fun startAlcFromFile(file: File) {
        alc.start(file)
    }

    override fun onStop() {
        buttonSemaphore = false
        super.onStop()
    }
}