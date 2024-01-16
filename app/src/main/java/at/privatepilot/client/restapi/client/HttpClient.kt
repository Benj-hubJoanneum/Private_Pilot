package at.privatepilot.client.restapi.client

import kotlinx.io.errors.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class HttpClient() {

    private val client: OkHttpClient = OkHttpClient()

    interface HttpCallback {
        fun onResponse(response: String)
        fun onFailure(error: Exception)
    }

    fun get(url: String, callback: HttpCallback) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                callback.onResponse(responseBody)
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }
        })
    }

    fun post(url: String, headerName: String, headerIP: String, requestBody: String, callback: HttpCallback) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = requestBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("name", headerName)
            .addHeader("ip", headerIP)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                callback.onResponse(responseBody)
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }
        })
    }
}