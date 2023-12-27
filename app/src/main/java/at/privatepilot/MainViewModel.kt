package at.privatepilot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _lan = MutableLiveData<String>()
    val lan: LiveData<String> get() = _lan

    private val _wan = MutableLiveData<String>()
    val wan: LiveData<String> get() = _wan

    fun updateCredentials(lan: String?, wan: String?) {
        _lan.value = lan ?: ""
        _wan.value = wan ?: ""
    }
}
