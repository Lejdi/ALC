package pl.lejdi.filebrowserfragment.model

import java.io.File

data class ListItem(
    val file: File, //file represented by item on recyclerview
    var isChosen: Boolean //info if file is chosen
)