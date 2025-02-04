package pl.lejdi.alc.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import pl.lejdi.alc.databinding.PlaylistListItemBinding
import pl.lejdi.alc.ui.PlaylistListFragment
import pl.lejdi.alc.viewmodel.PlaylistListViewModel

class PlaylistListAdapter(
    private val viewModel: PlaylistListViewModel,
    private val listener: OnListFragmentInteractionListener
) : RecyclerView.Adapter<PlaylistListAdapter.ViewHolder>() {

    private lateinit var binding: PlaylistListItemBinding
    private val playlists = MutableLiveData<List<String>>()

    init {
        //observe data from viewmodel
        viewModel.playlists.observe(listener as PlaylistListFragment, Observer {
            playlists.value = it.toList()
            notifyDataSetChanged()
        })
    }

    //inflate view binding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding =
            PlaylistListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    //setup listitem view elements
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = playlists.value!![position]

        //handle clicks
        holder.binding.playlistsListItem.setOnClickListener {
            listener.onListItemClickListener(holder.name.text as String)
        }
    }

    //number of items in recyclerview
    override fun getItemCount(): Int {
        if (playlists.value?.size == null)
            return 0
        return playlists.value?.size!!
    }

    //holder for single item in recyclerview
    inner class ViewHolder(val binding: PlaylistListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val name = this.binding.playlistName
    }

    //functions declaration for controlling clicks on items
    interface OnListFragmentInteractionListener {
        fun onListItemClickListener(name: String)
    }
}