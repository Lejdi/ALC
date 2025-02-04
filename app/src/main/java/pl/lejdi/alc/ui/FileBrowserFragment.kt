package pl.lejdi.alc.ui

import android.content.Context
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.ViewModelProvider
import pl.lejdi.alc.viewmodel.FileBrowserViewModel
import pl.lejdi.filebrowserfragment.ui.FilesystemFragment
import pl.lejdi.filebrowserfragment.util.FileFormatChecker
import java.io.File

@ExperimentalMaterialApi
class FileBrowserFragment : FilesystemFragment() {
    private lateinit var callback: ControlFragment.ControlToALCCallback
    private lateinit var callback2: PlaylistListFragment.PlaylistsToALCCallback
    private lateinit var viewModel: FileBrowserViewModel

    //filter out not supported file formats
    init {
        FileFormatChecker.extensions.add(".mp3")
        FileFormatChecker.extensions.add(".ogg")
        FileFormatChecker.extensions.add(".opus")
        FileFormatChecker.extensions.add(".wav")
        FileFormatChecker.extensions.add(".flac")
        FileFormatChecker.extensions.add(".amr")
        FileFormatChecker.extensions.add(".aac")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ControlFragment.ControlToALCCallback) {
            callback = context
        } else {
            throw RuntimeException(context.toString() + "must implement SongsToALCCallback")
        }
        if (context is PlaylistListFragment.PlaylistsToALCCallback) {
            callback2 = context
        } else {
            throw RuntimeException(context.toString() + "must implement PlaylistsToALCCallback")
        }
        val factory: ViewModelProvider.Factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        viewModel = ViewModelProvider(this, factory).get(FileBrowserViewModel::class.java)
        //init Hawk in viewmodel
        viewModel.initHawk(context)
    }


    //send chosen files to list fragment
    override fun onSave(file: List<File>) {
        viewModel.saveFiles(file)

        callback.setBackButton(null)
        callback2.initializePlaylist()

        activity?.supportFragmentManager!!.popBackStack()
    }

}