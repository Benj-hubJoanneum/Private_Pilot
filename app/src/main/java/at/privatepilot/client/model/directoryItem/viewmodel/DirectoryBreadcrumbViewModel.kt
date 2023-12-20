package at.privatepilot.client.model.directoryItem.viewmodel

import android.graphics.Color
import android.view.View
import androidx.lifecycle.ViewModel
import at.privatepilot.client.model.directoryItem.DirectoryItem

class DirectoryBreadcrumbViewModel(directoryItem: DirectoryItem) : ViewModel() {
    var name: String = directoryItem.name
    val path: String = directoryItem.path
    var color: Int = Color.BLACK
    var divider: Int = View.VISIBLE
}
