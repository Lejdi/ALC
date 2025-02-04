package pl.lejdi.alc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.app.ActivityCompat
import pl.lejdi.alc.databinding.ActivityMainBinding
import pl.lejdi.alc.ui.*
import java.io.File

@ExperimentalMaterialApi
class MainActivity : AppCompatActivity(),
    SongsListFragment.SongsToALCCallback,
    ControlFragment.ControlToALCCallback,
    PlaylistListFragment.PlaylistsToALCCallback,
    SettingsFragment.SettingsToALCCallback
{
    private lateinit var binding: ActivityMainBinding
    private var controlFragment: ControlFragment? = null
    private var songsListFragment: SongsListFragment? = null
    var fileBrowserFragment: FileBrowserFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        checkPermissions()
        controlFragment =
            supportFragmentManager.findFragmentById(R.id.control_fragment) as ControlFragment?
        if (controlFragment == null) {
            controlFragment = ControlFragment()
        }
        supportFragmentManager.beginTransaction().add(R.id.control_container, controlFragment!!)
            .commit()
        if (supportFragmentManager.fragments.isNullOrEmpty()) {
            setFragments()
        }
    }

    //setup fragments in order to enable communication between them
    private fun setFragments() {
        songsListFragment =
            supportFragmentManager.findFragmentById(R.id.songs_list_fragment) as SongsListFragment?
        if (songsListFragment == null) {
            songsListFragment = SongsListFragment()
        }
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, songsListFragment!!)
            .commit()
    }

    //check if all required permissions are granted
    private fun checkPermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.READ_PHONE_STATE,
                ),
                1
            )
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.READ_PHONE_STATE,
                ),
                1
            )
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.READ_PHONE_STATE,
                ),
                1
            )
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.READ_PHONE_STATE,
                    ),
                    1
                )
            }
        }
    }

    //implementation of messages to control interface - setting playlist
    override fun sendSongsToControl(playlist: List<File>) {
        controlFragment?.setPlaylist(playlist)
    }

    //implementation of messages to control interface - starting ALC from file
    override fun startAlcFromFile(file: File) {
        controlFragment?.startAlcFromFile(file)
    }

    override fun initializePlaylist() {
        controlFragment?.initializePlaylist()
    }

    override fun onBackPressed() {
        if (fileBrowserFragment != null && fileBrowserFragment!!.goBack()) {
            return
        }
        super.onBackPressed()
    }

    override fun setBackButton(fileBrowserFragment: FileBrowserFragment?) {
        this.fileBrowserFragment = fileBrowserFragment
    }

    override fun sendAssistantSetting() {
        controlFragment?.setAssistant()
    }
}