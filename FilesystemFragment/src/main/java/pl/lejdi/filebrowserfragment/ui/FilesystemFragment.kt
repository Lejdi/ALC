package pl.lejdi.filebrowserfragment.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import pl.lejdi.filebrowserfragment.R
import pl.lejdi.filebrowserfragment.adapter.FilesAdapter
import pl.lejdi.filebrowserfragment.databinding.FilesFragmentBinding
import pl.lejdi.filebrowserfragment.model.ListItem
import pl.lejdi.filebrowserfragment.viewmodel.FileSystemViewModel
import java.io.File

abstract class FilesystemFragment : Fragment(), FilesAdapter.OnListFragmentInteractionListener {

    private val MAIN_PATH = "/storage/emulated/0" //root directory
    private lateinit var binding: FilesFragmentBinding
    private lateinit var viewModel: FileSystemViewModel
    private lateinit var adapter: FilesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FilesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //init viewmodel
        val factory: ViewModelProvider.Factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        viewModel = ViewModelProvider(this, factory).get(FileSystemViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        initRecyclerView()
        viewModel.setPath(MAIN_PATH)
        setButtonsClickListeners()
        registerObservers()
        initSearchView()
    }

    private fun registerObservers() {
        viewModel.currPath.observe(this, {
            binding.currentPathTextview.text =
                viewModel.currPath.value?.replaceFirst(MAIN_PATH, "/")?.replaceFirst("//", "/")
        })
        viewModel.filterString.observe(this, {
            viewModel.setPath(viewModel.currPath.value!!)
        })
        viewModel.files.observe(this, {
            adapter.setList(it)
        })
    }

    //initially search bar is not active
    private fun initSearchView() {
        binding.searchButton.visibility = View.VISIBLE
        binding.currentPathTextview.visibility = View.VISIBLE
        binding.cancelButton.visibility = View.GONE
        binding.searchEdittext.visibility = View.GONE
    }

    private fun setButtonsClickListeners() {
        binding.saveButton.setOnClickListener {
            val list = viewModel.saveFiles()
            onSave(list)
        }
        //if click on search container - switch to search mode
        binding.searchContainer.setOnClickListener {
            binding.searchButton.visibility = View.GONE
            binding.currentPathTextview.visibility = View.GONE
            binding.cancelButton.visibility = View.VISIBLE
            binding.searchEdittext.visibility = View.VISIBLE
            binding.searchEdittext.requestFocus()
        }
        //when click on cancel - back to displaying path
        binding.cancelButton.setOnClickListener {
            binding.searchButton.visibility = View.VISIBLE
            binding.currentPathTextview.visibility = View.VISIBLE
            binding.cancelButton.visibility = View.GONE
            binding.searchEdittext.visibility = View.GONE
            binding.searchEdittext.setText("")
        }
        //display soft keyboard when search field is focused
        binding.searchEdittext.setOnFocusChangeListener { view, b ->
            val inputMethodManager =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (b) {
                inputMethodManager.showSoftInput(view, 0)
            } else {
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        //filter values when search text is changed
        binding.searchEdittext.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                viewModel.filterString.value = p0.toString()
            }
        })

        binding.checkboxAll.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.files.value?.forEach {
                    it.isChosen = true
                    viewModel.addToCheckedList(it.file)
                }
                adapter.notifyDataSetChanged()
            } else {
                viewModel.files.value?.forEach {
                    it.isChosen = false
                    viewModel.removeFromCheckedList(it.file)
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun initRecyclerView() {
        //initialize recyclerview
        adapter = FilesAdapter(this)
        binding.filesRecyclerview.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        binding.filesRecyclerview.layoutManager = layoutManager

        //divider between items
        val itemDecoration = DividerItemDecoration(activity, layoutManager.orientation)
        itemDecoration.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.files_divider
            )!!
        )
        binding.filesRecyclerview.addItemDecoration(itemDecoration)
    }

    //fun to be implemented by child fragment - implement what to do with chosen files
    abstract fun onSave(file: List<File>)

    //on item click
    override fun onListFragmentClickInteraction(file: ListItem, position: Int) {
        //if it's directory, change path
        if (file.file.isDirectory) {
            viewModel.setPath(file.file.path)
        }
        //else, choose file
        else {
            val layoutManager = binding.filesRecyclerview.layoutManager as LinearLayoutManager
            val item = binding.filesRecyclerview.getChildAt(position - layoutManager.findFirstVisibleItemPosition())
            val checkbox = item.findViewById<CheckBox>(R.id.checkbox)
            checkbox.isChecked = !checkbox.isChecked
        }
    }

    //choose file on toggle checkbox
    override fun onCheckboxToggle(file: ListItem, position: Int, checked: Boolean) {
        if (checked) {
            file.isChosen = true
            viewModel.addToCheckedList(file.file)
        } else {
            file.isChosen = false
            viewModel.removeFromCheckedList(file.file)
        }
    }

    fun goBack(): Boolean {
        return if (viewModel.currPath.value == MAIN_PATH) {
            false
        } else {
            viewModel.goBackToParentDir()
            true
        }
    }
}