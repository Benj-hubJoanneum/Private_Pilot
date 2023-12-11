package at.privatepilot.server

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ServerViewModel : ViewModel() {

    private val _localAddress = MutableLiveData<String>()
    val localAddress: LiveData<String> get() = _localAddress

    private val _internetAddress = MutableLiveData<String>()
    val internetAddress: LiveData<String> get() = _internetAddress

    fun updateAddresses(localAddress: String, internetAddress: String) {
        _localAddress.value = localAddress
        _internetAddress.value = internetAddress
    }
}
