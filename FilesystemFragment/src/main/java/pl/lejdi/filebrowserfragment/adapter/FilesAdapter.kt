package pl.lejdi.filebrowserfragment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import pl.lejdi.filebrowserfragment.R
import pl.lejdi.filebrowserfragment.databinding.FileItemBinding
import pl.lejdi.filebrowserfragment.model.ListItem


internal class FilesAdapter internal constructor(
    private val mListener: OnListFragmentInteractionListener
) : RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

    private lateinit var binding: FileItemBinding

    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //inflate binding
        binding = FileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    //set items' views
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    //get number of items
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun setList(list: List<ListItem>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ListItem>() {
            override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem.file.path == newItem.file.path
            }

            override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    //handle single item's fields
    inner class ViewHolder(val binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var listItem: ListItem

        fun bind(item: ListItem) {
            listItem = item
            if (!item.file.isDirectory) {
                binding.fileOrDirMarker.setBackgroundResource(R.drawable.file_icon_with_padding)
            } else {
                binding.fileOrDirMarker.setBackgroundResource(R.drawable.folder_icon_with_padding)
            }
            binding.fileName.text = item.file.name
            binding.checkbox.isChecked = item.isChosen
            binding.checkbox.setOnCheckedChangeListener { _, b ->
                mListener.onCheckboxToggle(listItem, absoluteAdapterPosition, b)
            }
            binding.listItem.setOnClickListener {
                mListener.onListFragmentClickInteraction(listItem, absoluteAdapterPosition)
            }
        }
    }

    //interface for items clicks
    internal interface OnListFragmentInteractionListener {
        fun onListFragmentClickInteraction(file: ListItem, position: Int)
        fun onCheckboxToggle(file: ListItem, position: Int, checked: Boolean)
    }
}