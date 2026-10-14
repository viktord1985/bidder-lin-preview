package com.linked.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.linked.bidder.appConfig
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.model.ChromeProfile
import com.linked.utils.sleep
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

fun analyticsInit() {

    appOneEvent(appConfig.botID)
}

fun initProfileAnalyticName(profile: ChromeProfile) {
    profileAnalyticName = getAnalyticNameByChromeProfile(profile)
}

fun getAnalyticNameByChromeProfile(profile: ChromeProfile): String {
    return appConfig.getProfileNameByChromeProfileName(profile.id).split(" ")[0].replace("#", "").toLowerCase()
}

// ------ FirebaseAnalyticsClient ------

val firebaseAnalytics = FirebaseAnalyticsClient()

class FirebaseAnalyticsEvent(
    val name: String, val params: MutableMap<String, Any>?
)

class FirebaseAnalyticsClient() {

    private val client: OkHttpClient =
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS)
            .build()

    private val messageQueue = LinkedBlockingQueue<FirebaseAnalyticsEvent>() // Message queue
    private val executor = Executors.newSingleThreadExecutor() // Single thread for sequential processing
    private val maxAttempts = 3

    @Volatile
    private var isSending = false // Sending flag, so messages are sent one after another

    init {
        executor.submit { processQueue() }
    }

    fun sendEvent(event: FirebaseAnalyticsEvent) {
        messageQueue.offer(event)
        processQueue()
    }

    // Processes the message queue
    @Synchronized
    private fun processQueue() {
        if (isSending) return

        val event = messageQueue.poll() ?: return
        isSending = true
        sendEventWithRetry(event, 0)
    }

    private fun sendEventWithRetry(event: FirebaseAnalyticsEvent, attempt: Int) {
        if (attempt >= maxAttempts) {
            Logger.logError("!!! Max retry attempts reached for event: ${event.name} !!!")
            isSending = false
            processQueue()
            return
        }

        try {

            Logger.logInfo("FirebaseAnalyticsEvent sendEvent: ${event.name} ${if (attempt > 0) "| Attempt ${attempt + 1}" else ""}")

            //Note: a too-short string causes glitches
            val clientId = "bot_bot_bot${appConfig.botID}"

            val measurementId = "G-0NQ5MZX51B" // Your Measurement ID
            val apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"  // Your API Secret

            val url = "https://www.google-analytics.com/mp/collect?measurement_id=$measurementId&api_secret=$apiSecret"

            val jsonBody = JsonObject().apply {

                addProperty("client_id", clientId)

                val eventsArray = JsonArray()
                val eventObject = JsonObject().apply {
                    addProperty("name", event.name)

                    val paramsObject = JsonObject()
                    event.params?.forEach { (key, value) ->
                        when (value) {
                            is Number -> paramsObject.addProperty(key, value)
                            is Boolean -> paramsObject.addProperty(key, value)
                            is String -> paramsObject.addProperty(key, value)
                        }
                    }
                    add("params", paramsObject)
                }

                eventsArray.add(eventObject)
                add("events", eventsArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Logger.logError("Error on FirebaseAnalyticsEvent: $e")
                    sleep(3000)
                    sendEventWithRetry(event, attempt + 1) // Retry sending
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Logger.logInfo("FirebaseAnalyticsEvent - Event sent successfully")
                        isSending = false
                        processQueue()
                    } else {
                        Logger.logError("FirebaseAnalyticsEvent - Failed to send event. Retrying... Attempt $attempt")
                        sleep(3000)
                        sendEventWithRetry(event, attempt + 1) // Retry sending on failure
                    }
                    response.close()
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
            Logger.logError("Error on FirebaseAnalyticsEvent: $e")
            sleep(3000)
            sendEventWithRetry(event, attempt + 1) // Retry sending
        }
    }
}

fun logFirebaseEvent(name: String, bot: String? = null, scanner: String? = null, count: Int? = null, counts: Map<String, Int>? = null) {
    val event = FirebaseAnalyticsEvent(
        name, mutableMapOf("platform" to "desktop", "version" to Config.appVersion.replace("version:", ""))
    )
    if (bot != null) {
        event.params?.set("bot", bot)
    }
    if (scanner != null) {
        event.params?.set("scanner", scanner)
    }
    if (count != null) {
        event.params?.set("count", count)
    }
    if (counts != null) {
        counts.forEach { it ->
            event.params?.set("${it.key}_count", it.value)
        }
    }
    firebaseAnalytics.sendEvent(event)
}

// ------ AmplitudeAnalyticsClient ------

val amplitudeAnalytics = AmplitudeAnalyticsClient()

data class AmplitudeEvent(
    val eventType: String, val eventProperties: MutableMap<String, Any?>? = null
)

class AmplitudeAnalyticsClient {

    private val client: OkHttpClient =
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS)
            .build()

    private val messageQueue = LinkedBlockingQueue<AmplitudeEvent>()
    private val executor = Executors.newSingleThreadExecutor()
    private val maxAttempts = 3

    @Volatile
    private var isSending = false

    private val apiKey = "85f8b638ff7738a62aeeed33500bebaa"

    init {
        executor.submit { processQueue() }
    }

    fun sendEvent(event: AmplitudeEvent) {
        messageQueue.offer(event)
        processQueue()
    }

    @Synchronized
    private fun processQueue() {
        if (isSending) return

        val event = messageQueue.poll() ?: return
        isSending = true
        sendEventWithRetry(event, 0)
    }

    private fun sendEventWithRetry(event: AmplitudeEvent, attempt: Int) {
        if (attempt >= maxAttempts) {
            Logger.logError("!!! Max retry attempts reached for AmplitudeEvent: ${event.eventType} !!!")
            isSending = false
            processQueue()
            return
        }

        try {
            Logger.logInfo("AmplitudeEvent sendEvent: ${event.eventType} ${if (attempt > 0) "| Attempt ${attempt + 1}" else ""}")

            val url = "https://api2.amplitude.com/2/httpapi"

            // same as for Google Analytics, where a too-short string caused glitches
            val clientId = "bot_bot_bot${appConfig.botID}"

            val jsonBody = JsonObject().apply {
                addProperty("api_key", apiKey)

                val eventsArray = JsonArray()
                val eventObject = JsonObject().apply {
                    addProperty("device_id", clientId)
                    addProperty("event_type", event.eventType)

                    val eventProperties = JsonObject()
                    event.eventProperties?.forEach { (key, value) ->
                        when (value) {
                            is Number -> eventProperties.addProperty(key, value)
                            is Boolean -> eventProperties.addProperty(key, value)
                            is String -> eventProperties.addProperty(key, value)
                        }
                    }
                    add("event_properties", eventProperties)

                    val userProperties = JsonObject()

                    if (event.eventProperties?.containsKey("bot") == true) {
                        userProperties.addProperty("Bot ID", event.eventProperties["bot"].toString())
                    }

                    if (event.eventProperties?.containsKey("scanner") == true) {
                        userProperties.addProperty("Scanner ID", event.eventProperties["scanner"].toString())
                    }

                    if (!userProperties.isEmpty) {
                        add("user_properties", userProperties)
                    }

                }

                eventsArray.add(eventObject)
                add("events", eventsArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Logger.logError("Error on AmplitudeEvent: $e")
                    Thread.sleep(3000)
                    sendEventWithRetry(event, attempt + 1)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Logger.logInfo("AmplitudeEvent - Event sent successfully")
                        isSending = false
                        processQueue()
                    } else {
                        Logger.logError("AmplitudeEvent - Failed to send event. Retrying... Attempt $attempt")
                        Thread.sleep(3000)
                        sendEventWithRetry(event, attempt + 1)
                    }
                    response.close()
                }
            })

        } catch (e: Exception) {
            Logger.logError("Error on AmplitudeEvent: $e")
            Thread.sleep(3000)
            sendEventWithRetry(event, attempt + 1)
        }
    }
}

fun logAmplitudeEvent(name: String, bot: String? = null, scanner: String? = null, count: Int? = null, counts: Map<String, Int>? = null) {
    val event = AmplitudeEvent(
        name, mutableMapOf("platform" to "desktop", "version" to Config.appVersion.replace("version:", ""))
    )
    if (bot != null) {
        event.eventProperties?.set("bot", bot)
    }
    if (scanner != null) {
        event.eventProperties?.set("scanner", scanner)
    }
    if (count != null) {
        event.eventProperties?.set("count", count)
    }
    if (counts != null) {
        counts.forEach { it ->
            event.eventProperties?.set("${it.key}_count", it.value)
        }
    }
    amplitudeAnalytics.sendEvent(event)
}
// ------ analytics events ------

fun appOneEvent(bot: String) {
    logFirebaseEvent("app_open", bot)
    logAmplitudeEvent("app_open", bot)
}

fun sendConnectEvent(tag: String, bot: String, scanner: String) {
    logFirebaseEvent("${tag}_send_connect", bot, scanner)
    logAmplitudeEvent("${tag}_send_connect", bot, scanner)
}

fun getConnectEvent(tag: String, bot: String, scanner: String) {
    logFirebaseEvent("${tag}_get_connect", bot, scanner)
    logAmplitudeEvent("${tag}_get_connect", bot, scanner)
}

fun linkedPostEvent(tag: String, bot: String, scanner: String, count: Int) {
    logFirebaseEvent("${tag}_post", bot, scanner, count)
    logAmplitudeEvent("${tag}_post", bot, scanner, count)
}

fun bulkEvent(tag: String, bot: String, scanner: String, counts: Map<String, Int>) {
    logFirebaseEvent("${tag}_bulk", bot, scanner, null, counts)
    logAmplitudeEvent("${tag}_bulk", bot, scanner, null, counts)
}

fun profileViewersEvent(bot: String, scanner: String, count: Int) {
    logFirebaseEvent("profile_viewers", bot, scanner, count)
    logAmplitudeEvent("profile_viewers", bot, scanner, count)
}

fun postImpressionsEvent(bot: String, scanner: String, count: Int) {
    logFirebaseEvent("post_impressions", bot, scanner, count)
    logAmplitudeEvent("post_impressions", bot, scanner, count)
}

fun sendReferEvent(bot: String, scanner: String) {
    logFirebaseEvent("send_refer", bot, scanner, 1)
    logAmplitudeEvent("send_refer", bot, scanner, 1)
}