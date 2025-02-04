package pl.lejdi.alc.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.hawk.Hawk
import pl.lejdi.alc.R
import pl.lejdi.alc.util.CurrentPlaylist

class PlaylistListViewModel : ViewModel() {
    val playlists = MutableLiveData<List<String>>()

    //initialize hawk
    fun initHawk(context: Context) {
        Hawk.init(context).build()
    }

    fun loadPlaylists() {
        if (playlists.value.isNullOrEmpty()) {
            playlists.value =
                Hawk.get<List<String>>(pl.lejdi.alc.util.Constants.HAWK_ALL_PLAYLISTS_KEY)
        }
    }

    fun addPlaylist(name: String) {
        val temp = Hawk.get<List<String>>(pl.lejdi.alc.util.Constants.HAWK_ALL_PLAYLISTS_KEY)
            .toMutableList()
        temp.add(name)
        Hawk.put(pl.lejdi.alc.util.Constants.HAWK_ALL_PLAYLISTS_KEY, temp.toList())
        playlists.value = temp
    }

    fun deletePlaylist(name: String, context: Context) {
        val temp = Hawk.get<List<String>>(pl.lejdi.alc.util.Constants.HAWK_ALL_PLAYLISTS_KEY)
            .toMutableList()
        temp.remove(name)
        if (Hawk.contains(pl.lejdi.alc.util.Constants.HAWK_PLAYLIST_PREFIX + name)) {
            Hawk.delete(pl.lejdi.alc.util.Constants.HAWK_PLAYLIST_PREFIX + name)
        }
        if (CurrentPlaylist.name.equals(name)) {
            if (temp.isEmpty()) {
                temp.add(context.getString(R.string.default_playlist))
            }
            CurrentPlaylist.name = temp[0]
        }
        Hawk.put(pl.lejdi.alc.util.Constants.HAWK_ALL_PLAYLISTS_KEY, temp.toList())
        Hawk.put(
            pl.lejdi.alc.util.Constants.HAWK_CURRENT_PLAYLIST_KEY,
            context.getString(R.string.default_playlist)
        )
        playlists.value = temp
    }
}