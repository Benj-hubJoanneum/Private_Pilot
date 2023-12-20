package at.privatepilot.ui.listView.base

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.privatepilot.model.INode
import at.privatepilot.model.nodeItem.NodeItem
import at.privatepilot.model.nodeItem.viewmodel.NodeItemViewModel

class RecyclerViewModel : ViewModel() {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val _imageResource = MutableLiveData<Int>()
    val imageResource: LiveData<Int> = _imageResource

    private val _itemList = MutableLiveData<List<NodeItemViewModel>>()
    val itemList: LiveData<List<NodeItemViewModel>> = _itemList

    fun loadFileList(displayedList: MutableList<INode>) {
        try {
            val newList = displayedList.map { NodeItemViewModel(it as NodeItem) }
            _itemList.postValue(newList) //probably can change the images to icons here
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error loading files: ${e.message}")
        }
    }

    fun setValues(newText: String, newImage: Int) {
        _text.value = newText
        _imageResource.value = newImage
    }
}
