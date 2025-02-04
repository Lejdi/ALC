package pl.lejdi.alc.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.lejdi.alc.R
import pl.lejdi.alc.adapter.PlaylistListAdapter
import pl.lejdi.alc.databinding.FragmentPlaylistsListBinding
import pl.lejdi.alc.util.CurrentPlaylist
import pl.lejdi.alc.viewmodel.PlaylistListViewModel


class PlaylistListFragment : Fragment(), PlaylistListAdapter.OnListFragmentInteractionListener {

    interface PlaylistsToALCCallback {
        fun initializePlaylist()
    }

    companion object {
        var isDisplayed = false
    }

    private lateinit var callback: PlaylistsToALCCallback
    private lateinit var binding: FragmentPlaylistsListBinding
    private lateinit var adapter: PlaylistListAdapter
    private lateinit var viewModel: PlaylistListViewModel

    private var buttonSemaphore = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is PlaylistsToALCCallback) {
            callback = context
        } else {
            throw RuntimeException(context.toString() + "must implement PlaylistsToALCCallback")
        }

        isDisplayed = true

        val factory: ViewModelProvider.Factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        viewModel = ViewModelProvider(this, factory).get(PlaylistListViewModel::class.java)
        //init Hawk in viewmodel
        viewModel.initHawk(context)
    }

    override fun onDetach() {
        isDisplayed = false
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistsListBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        initRecyclerView()
        viewModel.loadPlaylists()
        addFABClickListener()
    }

    //initialize recyclerview
    private fun initRecyclerView() {
        adapter = PlaylistListAdapter(viewModel, this)
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {

            private val paint = android.graphics.Paint()
            val icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_delete_outline_24
            )!!

            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                swipeDir: Int
            ) {
                val position = viewHolder.absoluteAdapterPosition
                val item = viewModel.playlists.value?.get(position)
                item?.let {
                    viewModel.deletePlaylist(it, requireContext())
                    val toast =
                        Toast.makeText(context, getString(R.string.deleted, it), Toast.LENGTH_LONG)
                    toast.setGravity(
                        Gravity.BOTTOM,
                        0,
                        binding.root.rootView.findViewById<FragmentContainerView>(R.id.control_container).height + 20
                    )
                    toast.show()
                    adapter.notifyItemRemoved(position)
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )

                if (dX != 0f && isCurrentlyActive) {
                    val itemView = viewHolder.itemView
                    paint.color = android.graphics.Color.parseColor("#D32F2F")
                    val top = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                    val left =
                        itemView.width - icon.intrinsicWidth - (itemView.height - icon.intrinsicHeight) / 2
                    val right = left + icon.intrinsicHeight
                    val bottom = top + icon.intrinsicHeight

                    icon.setTint(android.graphics.Color.WHITE)

                    if (dX < 0) {
                        val background = android.graphics.RectF(
                            itemView.right.toFloat() + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat()
                        )
                        c.drawRect(background, paint)
                        icon.setBounds(
                            left,
                            top,
                            right,
                            bottom
                        )
                    } else if (dX > 0) {
                        val background = android.graphics.RectF(
                            itemView.left.toFloat() + dX, itemView.top.toFloat(),
                            itemView.left.toFloat(), itemView.bottom.toFloat()
                        )
                        c.drawRect(background, paint)
                        icon.setBounds(
                            icon.intrinsicWidth,
                            top,
                            2 * icon.intrinsicWidth,
                            top + icon.intrinsicHeight
                        )
                    }
                    icon.draw(c)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.playlistsRecyclerview)
        binding.playlistsRecyclerview.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        binding.playlistsRecyclerview.layoutManager = layoutManager
        val itemDecoration = DividerItemDecoration(activity, layoutManager.orientation)
        itemDecoration.setDrawable(
            ContextCompat.getDrawable(
                requireActivity().baseContext,
                R.drawable.files_list_items_divider
            )!!
        )
        binding.playlistsRecyclerview.addItemDecoration(itemDecoration)
    }

    private lateinit var customAlertDialogView: View

    private fun launchCustomAlertDialog() {
        val nameTextField: TextInputLayout = customAlertDialogView.findViewById(R.id.playlist_name)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(getString(R.string.playlist_name))
            .setView(customAlertDialogView)
            .setBackground(
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.color.dialogBackgroundColor
                )
            )
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                val newName = nameTextField.editText?.text.toString()

                if (newName.isEmpty()) {
                    dialog.dismiss()
                } else {
                    if (viewModel.playlists.value!!.contains(newName)) {
                        dialog.dismiss()
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                delay(100)
                                val toast = Toast.makeText(
                                    context,
                                    getString(
                                        R.string.playlists_already_exists,
                                        newName
                                    ),
                                    Toast.LENGTH_LONG
                                )
                                toast.setGravity(
                                    Gravity.BOTTOM,
                                    0,
                                    binding.root.rootView.findViewById<FragmentContainerView>(R.id.control_container).height + 20
                                )
                                toast.show()
                            }
                        }
                    } else {
                        viewModel.addPlaylist(newName)
                    }
                    dialog.dismiss()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addFABClickListener() {
        binding.addPlaylistFab.setOnClickListener {
            if (!buttonSemaphore) {
                buttonSemaphore = true

                customAlertDialogView = LayoutInflater.from(requireActivity())
                    .inflate(R.layout.new_playlist_dialog, null, false)

                launchCustomAlertDialog()
                buttonSemaphore = false
            }
        }
    }

    override fun onListItemClickListener(name: String) {
        CurrentPlaylist.name = name
        Hawk.put(pl.lejdi.alc.util.Constants.HAWK_CURRENT_PLAYLIST_KEY, name)
        callback.initializePlaylist()
        activity?.supportFragmentManager!!.popBackStack()
    }

    override fun onStop() {
        buttonSemaphore = false
        super.onStop()
    }
}
