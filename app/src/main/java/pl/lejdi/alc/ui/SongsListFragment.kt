package pl.lejdi.alc.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.lejdi.alc.R
import pl.lejdi.alc.adapter.SongsListAdapter
import pl.lejdi.alc.databinding.FragmentSongsListBinding
import pl.lejdi.alc.viewmodel.SongsListViewModel
import java.io.File

@ExperimentalMaterialApi
class SongsListFragment : Fragment(), SongsListAdapter.OnListFragmentInteractionListener {
    private lateinit var binding: FragmentSongsListBinding
    private lateinit var callback: SongsToALCCallback
    private lateinit var callback2: ControlFragment.ControlToALCCallback
    private lateinit var adapter: SongsListAdapter

    private var buttonSemaphore = false

    private val viewModel: SongsListViewModel by viewModels()

    //interface for communication with control fragment
    interface SongsToALCCallback {
        fun sendSongsToControl(playlist: List<File>)
        fun startAlcFromFile(file: File)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //initialize communication with control fragment
        if (context is SongsToALCCallback) {
            callback = context
        } else {
            throw RuntimeException(context.toString() + "must implement SongsToALCCallback")
        }
        if (context is ControlFragment.ControlToALCCallback) {
            callback2 = context
        } else {
            throw RuntimeException(context.toString() + "must implement ControlToALCCallback")
        }
        //init Hawk in viewmodel
        viewModel.initHawk(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun notifyWidget() {
        val updateWidgetIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        context?.sendBroadcast(updateWidgetIntent)
    }

    override fun onStart() {
        super.onStart()
        viewModel.getFiles()
        notifyWidget()
        initRecyclerView()
        addFabListener()
    }

    private fun addFabListener() {
        binding.addSongsFab.setOnClickListener {
            if (!buttonSemaphore) {
                buttonSemaphore = true
                if (activity?.supportFragmentManager!!.backStackEntryCount == 0) {
                    val fileBrowserFragment = FileBrowserFragment()
                    callback2.setBackButton(fileBrowserFragment)
                    activity?.supportFragmentManager!!.beginTransaction()
                        .addToBackStack(null)
                        .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right
                        )
                        .replace(R.id.container, fileBrowserFragment)
                        .commit()
                }
                buttonSemaphore = false
            }

        }
    }

    //initialize recyclerview
    private fun initRecyclerView() {
        if (binding.songsRecyclerview.adapter == null) {
            adapter = SongsListAdapter(viewModel, this)
            val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
                ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                ) {

                private val paint = Paint()
                val icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_delete_outline_24
                )!!

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    val position = viewHolder.absoluteAdapterPosition
                    val file = viewModel.files.value?.get(position)
                    file?.let {
                        viewModel.deleteFile(file)
                        callback.sendSongsToControl(viewModel.files.value!!.toList())
                        notifyWidget()
                        val toast =
                            Toast.makeText(
                                context,
                                getString(R.string.deleted, file.name),
                                Toast.LENGTH_LONG
                            )
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
                    c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                    dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
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
                        paint.color = Color.parseColor("#D32F2F")
                        val top = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val left =
                            itemView.width - icon.intrinsicWidth - (itemView.height - icon.intrinsicHeight) / 2
                        val right = left + icon.intrinsicHeight
                        val bottom = top + icon.intrinsicHeight

                        icon.setTint(Color.WHITE)

                        if (dX < 0) {
                            val background = RectF(
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
                            val background = RectF(
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
            itemTouchHelper.attachToRecyclerView(binding.songsRecyclerview)
            binding.songsRecyclerview.adapter = adapter
            val layoutManager = LinearLayoutManager(activity)
            binding.songsRecyclerview.layoutManager = layoutManager
            //add divider between files
            val itemDecoration = DividerItemDecoration(
                activity,
                (binding.songsRecyclerview.layoutManager as LinearLayoutManager).orientation
            )
            itemDecoration.setDrawable(
                ContextCompat.getDrawable(
                    requireActivity().baseContext,
                    R.drawable.files_list_items_divider
                )!!
            )
            binding.songsRecyclerview.addItemDecoration(itemDecoration)
            binding.scrollbar.setRecyclerView(binding.songsRecyclerview)
            binding.songsRecyclerview.addOnScrollListener(binding.scrollbar.onScrollListener)
        }
    }

    //click on item - start ALC from file
    override fun onListItemClickListener(file: File) {
        callback.startAlcFromFile(file)
    }

    override fun onStop() {
        buttonSemaphore = false
        super.onStop()
    }
}