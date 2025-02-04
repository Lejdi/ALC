package pl.lejdi.alc.util

import com.orhanobut.hawk.Hawk
import java.io.File

fun getHawkFiles() : MutableList<File>{
    if (Hawk.contains(Constants.HAWK_PLAYLIST_PREFIX + CurrentPlaylist.name)) {
        val filesList =
            Hawk.get<MutableList<String>>(Constants.HAWK_PLAYLIST_PREFIX + CurrentPlaylist.name)
        return filesList.map { path -> File(path) }.toMutableList()
    }
    return mutableListOf()
}