package pl.lejdi.alc.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.orhanobut.hawk.Hawk
import pl.lejdi.alc.util.CurrentPlaylist
import pl.lejdi.alc.util.getHawkFiles
import java.io.File

class FileBrowserViewModel : ViewModel() {
    fun initHawk(context: Context) {
        Hawk.init(context).build()
    }

    //save files in shared preferences
    fun saveFiles(files: List<File>) {
        //add new files to new playlist
        val newFilesList = mutableListOf<File>()
        newFilesList.addAll(files)
        //add previously saved files to new playlist
        val oldFiles = getHawkFiles()
        if(oldFiles.isNotEmpty()){
            newFilesList.let { list -> oldFiles.let(list::addAll) }
        }
        //remove duplicates
        val dedupFiles = newFilesList.distinct().toMutableList().map { file -> file.absolutePath }
        //sort playlist
        dedupFiles.sortedBy { it }
        //save new files
        Hawk.put(
            pl.lejdi.alc.util.Constants.HAWK_PLAYLIST_PREFIX + CurrentPlaylist.name,
            dedupFiles
        )
    }
}