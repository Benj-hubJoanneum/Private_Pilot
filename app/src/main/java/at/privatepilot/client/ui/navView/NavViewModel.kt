package at.privatepilot.ui.navView

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.privatepilot.model.directoryItem.DirectoryItem
import at.privatepilot.model.directoryItem.viewmodel.DirectoryItemViewModel

class NavViewModel : ViewModel() {

    private val _itemList = MutableLiveData<List<DirectoryItemViewModel>>()
    val itemList: LiveData<List<DirectoryItemViewModel>> = _itemList

    private val _selectedFolder = MutableLiveData<DirectoryItemViewModel?>()
    val selectedFolder: LiveData<DirectoryItemViewModel?> = _selectedFolder

    fun loadFolderList(directoryList: MutableSet<DirectoryItem>) {
        try {
            directoryList.let { list ->
                list.sortedWith(compareBy { it.parentFolder })
                    .map {
                        DirectoryItemViewModel(it)
                        it.depth = it.path.count { it == '/' }// - 1
                    }

                // Add "HOME" item as the first item in the list
                val homeItem = DirectoryItem("HOME", "")

                _itemList.postValue(listOf(DirectoryItemViewModel(homeItem)) + list.map { DirectoryItemViewModel(it) })
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error loading folders: ${e.message}")
        }
    }

    fun setSelectedFolder(path: String) {
        try {
            if (_selectedFolder.value?.path != path) {
                val folder = _itemList.value?.find { it.path == path }
                _selectedFolder.postValue(folder)
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error selecting folder: ${e.message}")
        }
    }
}
