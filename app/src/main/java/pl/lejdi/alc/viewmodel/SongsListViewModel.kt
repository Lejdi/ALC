package pl.lejdi.alc.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.hawk.Hawk
import pl.lejdi.alc.util.CurrentPlaylist
import pl.lejdi.alc.util.getHawkFiles
import java.io.File

class SongsListViewModel : ViewModel() {
    val files = MutableLiveData<MutableList<File>>()

    //initialize hawk
    fun initHawk(context: Context) {
        Hawk.init(context).build()
    }

    //retrieve saved files
    fun getFiles() {
        files.value = getHawkFiles()
    }

    //delete single file and update playlist
    fun deleteFile(file: File) {
        val savedFiles = getHawkFiles()
        if (savedFiles.contains(file)) {
            savedFiles.remove(file)
            Hawk.put(
                pl.lejdi.alc.util.Constants.HAWK_PLAYLIST_PREFIX + CurrentPlaylist.name,
                savedFiles.map { it.absolutePath }
            )
            files.value = savedFiles
        }
    }
}