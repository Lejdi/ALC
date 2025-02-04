package pl.lejdi.filebrowserfragment.util

import java.io.File

//class that contains extensions that should be displayed in adapter; if it's empty there is no filter
class FileFormatChecker {
    companion object {
        var extensions = mutableListOf<String>()
        internal fun isFileCorrect(file: File): Boolean {
            if (file.isDirectory || extensions.isEmpty())
                return true
            extensions.forEach {
                if (file.name.endsWith(it))
                    return true
            }
            return false
        }
    }
}