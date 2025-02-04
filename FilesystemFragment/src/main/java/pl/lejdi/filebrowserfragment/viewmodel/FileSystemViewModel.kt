package pl.lejdi.filebrowserfragment.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pl.lejdi.filebrowserfragment.model.ListItem
import pl.lejdi.filebrowserfragment.util.FileFormatChecker
import java.io.File

internal class FileSystemViewModel : ViewModel() {

    private val checkedFiles = mutableListOf<File>() //files to save
    val files = MutableLiveData<MutableList<ListItem>>() //all files in current directory
    val currPath = MutableLiveData<String>() //current path
    val filterString = MutableLiveData<CharSequence>() //filtering files by this string

    fun setPath(newPath: String) {
        currPath.value = newPath
        if (filterString.value.isNullOrEmpty()) {
            getFilesInCurrDir()
        } else {
            files.value = mutableListOf()
            getAllFilesRecurrently(currPath.value!!)
            files.value =
                files.value!!.sortedWith(compareBy({ !it.file.isDirectory }, { it.file.name }))
                    .toMutableList()
        }
    }

    private fun getAllFilesRecurrently(path: String) {
        val f = File(path)
        f.listFiles()?.forEach {
            if (FileFormatChecker.isFileCorrect(it)) {
                if (it.isDirectory) {
                    getAllFilesRecurrently(it.path)
                }
                if (it.name.contains(filterString.value!!, ignoreCase = true)) {
                    if (it in checkedFiles) {
                        files.value!!.add(ListItem(it, true))
                    } else {
                        files.value!!.add(ListItem(it, false))
                    }
                }
            }
        }
    }

    private fun getFilesInCurrDir() {
        files.value = mutableListOf()
        //get directory
        val f = File(currPath.value!!)
        //get each child
        f.listFiles()!!.forEach {
            //if valid file extension
            if (FileFormatChecker.isFileCorrect(it)) {
                //if search is not filtered OR it's directory OR it's filtered but match
                if (filterString.value.isNullOrEmpty() || it.isDirectory || it.name.contains(
                        filterString.value!!,
                        ignoreCase = true
                    )
                ) {
                    //add to adapter with proper parameter Chosen
                    if (it in checkedFiles) {
                        files.value!!.add(ListItem(it, true))
                    } else {
                        files.value!!.add(ListItem(it, false))
                    }
                }

            }
        }
        //finally, sort items - directories first, then sort directories and files by name
        files.value =
            files.value!!.sortedWith(compareBy({ !it.file.isDirectory }, { it.file.name }))
                .toMutableList()
    }

    fun goBackToParentDir() {
        //regex to extract parent directory
        setPath(currPath.value!!.replaceFirst("/[^/]*(?!/)$".toRegex(), ""))
    }

    fun addToCheckedList(file: File) {
        //if it is already in checked list - abort
        if (file in checkedFiles)
            return
        checkedFiles.add(file)
    }

    fun removeFromCheckedList(file: File) {
        if (file in checkedFiles)
            checkedFiles.remove(file)
    }

    fun saveFiles(): List<File> {
        val resultList = mutableListOf<File>()
        //for each checked file, add to result list it or it's children
        checkedFiles.forEach {
            resultList.let { list -> retrieveMusicFiles(it).let(list::addAll) }
        }
        //deduplicate result
        val deduplicatedList = resultList.distinct().toMutableList()
        //sort result
        deduplicatedList.sortBy { it.name }

        return deduplicatedList
    }

    private fun retrieveMusicFiles(file: File): List<File> {
        val resultList = mutableListOf<File>()

        //if it's directory - recurrently get all children
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                resultList.let { list -> retrieveMusicFiles(it).let(list::addAll) }
            }
        }
        //else add it to result list
        else {
            if (FileFormatChecker.isFileCorrect(file)) {
                resultList.add(file)
            }
        }
        return resultList
    }
}