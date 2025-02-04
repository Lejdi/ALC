package pl.lejdi.alc.adapter

import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import pl.lejdi.alc.R
import pl.lejdi.alc.databinding.SongsListItemBinding
import pl.lejdi.alc.ui.SongsListFragment
import pl.lejdi.alc.viewmodel.SongsListViewModel
import java.io.File

@ExperimentalMaterialApi
class SongsListAdapter(
    private val viewModel: SongsListViewModel,
    private val listener: OnListFragmentInteractionListener
) : RecyclerView.Adapter<SongsListAdapter.ViewHolder>() {

    private lateinit var binding: SongsListItemBinding
    private val songs = MutableLiveData<List<File>>()

    init {
        //observe data from viewmodel
        viewModel.files.observe(listener as SongsListFragment, Observer {
            songs.value = it.toList()
            notifyDataSetChanged()
        })
    }

    //inflate view binding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = SongsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    //get duration of song from metadata
    private suspend fun getDuration(file: File): String {
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(file.path)
        val metadataDuration =
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        metadataRetriever.release()
        return if (metadataDuration != null) {
            val minutes = (metadataDuration.toInt()) / 60000
            val seconds = ((metadataDuration.toInt()) / 1000) % 60
            val resString: String
            resString = if (seconds < 10) {
                "$minutes:0$seconds"
            } else {
                "$minutes:$seconds"
            }
            resString
        } else {
            "0:00"
        }
    }

    //get author of song from metadata
    private suspend fun getArtist(file: File): String {
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(file.path)
        val artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: (listener as Fragment).getString(R.string.unknown_artist)
        metadataRetriever.release()
        return artist
    }

    //get title of song from metadata
    private suspend fun getTitle(file: File): String {
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(file.path)
        val title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?: (listener as Fragment).getString(R.string.unknown_title)
        metadataRetriever.release()
        return title
    }

    //setup listitem view elements
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //set item
        val item = songs.value!![position]
        holder.mItem = item

        //duration
        liveData(context = Dispatchers.Default) {
            try {
                val duration = getDuration(item)
                emit(duration)
            } catch (e: Exception) {
                if (e is IllegalArgumentException) {
                    Log.e("ALC", "File ${item.name} does not exist!")
                }
            }
        }.observe(listener as SongsListFragment, Observer {
            holder.duration.text = it
        })

        //artist
        liveData(context = Dispatchers.Default) {
            try {
                val artist = getArtist(item)
                emit(artist)
            } catch (e: Exception) {
                if (e is IllegalArgumentException) {
                    Log.e("ALC", "File ${item.name} does not exist!")
                }
            }
        }.observe(listener, {
            holder.artist.text = it
        })

        //title
        liveData(context = Dispatchers.Default) {
            try {
                val title = getTitle(item)
                emit(title)
            } catch (e: Exception) {
                if (e is IllegalArgumentException) {
                    Log.e("ALC", "File ${item.name} does not exist!")
                }
            }
        }.observe(listener, {
            holder.title.text = it
        })

        //file name
        holder.filename.text = item.name

        //handle clicks
        holder.binding.songsListItem.setOnClickListener {
            listener.onListItemClickListener(holder.mItem!!)
        }
    }

    //number of items in recyclerview
    override fun getItemCount(): Int {
        if (songs.value?.size == null)
            return 0
        return songs.value?.size!!
    }

    //holder for single item in recyclerview
    inner class ViewHolder(val binding: SongsListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val filename = this.binding.fileName
        val artist = this.binding.artist
        val title = this.binding.title
        val duration = this.binding.duration
        var mItem: File? = null
    }

    //functions declaration for controlling clicks on items
    interface OnListFragmentInteractionListener {
        fun onListItemClickListener(file: File)
    }
}