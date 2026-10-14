package com.linked.api

import com.linked.bidder.utils.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class DetectAPI() {
    private var thread: Thread? = null

    fun start(label: String) {
        thread = Thread(Runnable {

            try {

                Logger.logInfo("DetectAPI sendMessage")

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                val url = "https://api.ipify.org/?format=text"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    Logger.logInfo("#DetectAPI label:$label, IP:${response.body!!.string()}")
                }

            } catch (e: Exception) {
                //e.printStackTrace()
                Logger.logError("Error on DetectAPI: Fail")
                DetectAPIV2().start(label)
            }
        })
        thread!!.start()
    }

    fun stop() {
        if (thread != null) {
            thread!!.stop()
        }
    }
}

