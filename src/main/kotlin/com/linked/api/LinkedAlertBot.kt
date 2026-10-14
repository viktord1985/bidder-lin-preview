package com.linked.api

import com.linked.bidder.utils.Logger
import okhttp3.*
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

// t.me/GmobyLinkedAlertBot
// send message to bot and get chatID -> https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/getUpdates
// if no result remove and invite again

class LinkedAlertBot(private val chatId: String) {

    private val botToken: String = System.getenv("TELEGRAM_BOT_TOKEN")
        ?: error("TELEGRAM_BOT_TOKEN environment variable is not set")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val messageQueue = LinkedBlockingQueue<String>()
    private val worker = Executors.newSingleThreadExecutor()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val started = AtomicBoolean(false)

    // Backoff settings
    private val baseDelayMs = 3_000L      // initial delay
    private val maxDelayMs = 60_000L      // maximum (1 min)
    private val backoffFactor = 2.0       // x2

    fun sendMessage(text: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        messageQueue.offer(escapeMarkdownV2("$timestamp | $text"))
        startWorkerIfNeeded()
    }

    private fun escapeMarkdownV2(text: String): String {
        // removed the asterisk {*} from this list because I use it
        // _ [ ] ( ) ~ ` > # + - = | { } . !
        val reserved = Regex("""([_\[\]\(\)~`>#+\-=|{}.!])""")
        return text.replace(reserved, """\\$1""")
    }

    private fun startWorkerIfNeeded() {
        if (started.compareAndSet(false, true)) {
            worker.submit { loop() }
        }
    }

    private fun loop() {
        while (!Thread.currentThread().isInterrupted) {
            val msg = messageQueue.take()
            sendUntilSuccessWithBackoff(msg)
        }
    }

    private fun sendUntilSuccessWithBackoff(text: String) {
        var attempt = 1
        var delayMs = baseDelayMs

        while (true) {
            try {
                Logger.logInfo("UpworkAlertBot sendMessage: $text | Attempt $attempt")

                val url = "https://api.telegram.org/bot$botToken/sendMessage"

                val requestBody = FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", text)
                    .add("parse_mode", "MarkdownV2")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val result = sendOnceBlocking(request)

                when (result.kind) {
                    SendResult.Kind.SUCCESS -> {
                        Logger.logInfo("Message sent successfully")
                        return
                    }

                    SendResult.Kind.RATE_LIMIT -> {
                        // Telegram asks to wait retry_after seconds
                        val waitMs = (result.retryAfterSec?.toLong() ?: 3L) * 1000L
                        val bounded = min(waitMs, maxDelayMs)
                        Logger.logError("429 Too Many Requests. retry_after=${result.retryAfterSec}s -> wait ${bounded}ms")
                        sleepNonBlocking(bounded)

                        // After a 429 the backoff is usually reset so delays don't grow for too long
                        delayMs = baseDelayMs
                    }

                    SendResult.Kind.PERMANENT_FAIL -> {
                        Logger.logError("Permanent error (${result.httpCode}), skipping message, body=${result.bodySnippet}")
                        return
                    }

                    SendResult.Kind.RETRYABLE_FAIL -> {
                        Logger.logError("Failed to send message. http=${result.httpCode} body=${result.bodySnippet}")
                        sleepNonBlocking(delayMs)
                        delayMs = nextBackoff(delayMs)
                    }
                }
            } catch (e: Exception) {
                Logger.logError("Error on UpworkAlertBot: $e")
                sleepNonBlocking(delayMs)
                delayMs = nextBackoff(delayMs)
            }

            attempt++
        }
    }

    private fun nextBackoff(current: Long): Long {
        val next = (current * backoffFactor).toLong()
        return min(next, maxDelayMs)
    }

    // -------------------- low-level sending --------------------

    private data class SendResult(
        val kind: Kind,
        val httpCode: Int? = null,
        val retryAfterSec: Int? = null,
        val bodySnippet: String? = null
    ) {
        enum class Kind { SUCCESS, RATE_LIMIT, PERMANENT_FAIL, RETRYABLE_FAIL }
    }

    private fun sendOnceBlocking(request: Request): SendResult {
        val lock = java.lang.Object()
        var result: SendResult? = null

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.logError("OkHttp failure: $e")
                synchronized(lock) {
                    result = SendResult(SendResult.Kind.RETRYABLE_FAIL, httpCode = null, bodySnippet = e.message)
                    lock.notify()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val code = resp.code
                    val body = runCatching { resp.body?.string() }.getOrNull()

                    val r = when {
                        resp.isSuccessful -> SendResult(SendResult.Kind.SUCCESS, httpCode = code)

                        code == 429 -> {
                            val retryAfter = parseRetryAfterSeconds(body)
                            SendResult(
                                kind = SendResult.Kind.RATE_LIMIT,
                                httpCode = code,
                                retryAfterSec = retryAfter,
                                bodySnippet = body?.take(200)
                            )
                        }

                        code in 400..499 ->
                            SendResult(
                                kind = SendResult.Kind.PERMANENT_FAIL,
                                httpCode = code,
                                bodySnippet = body?.take(200)
                            )

                        else -> SendResult(
                            kind = SendResult.Kind.RETRYABLE_FAIL,
                            httpCode = code,
                            bodySnippet = body?.take(200)
                        )
                    }

                    synchronized(lock) {
                        result = r
                        lock.notify()
                    }
                }
            }
        })

        synchronized(lock) {
            while (result == null) lock.wait()
            return result!!
        }
    }

    /**
     * On 429 Telegram usually returns JSON:
     * {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 12","parameters":{"retry_after":12}}
     * Extract retry_after with a simple regex, without external dependencies.
     */
    private fun parseRetryAfterSeconds(body: String?): Int? {
        if (body.isNullOrBlank()) return null
        val regex = Regex("\"retry_after\"\\s*:\\s*(\\d+)")
        val m = regex.find(body) ?: return null
        return m.groupValues.getOrNull(1)?.toIntOrNull()
    }

    private fun sleepNonBlocking(delayMs: Long) {
        val lock = java.lang.Object()
        scheduler.schedule({
            synchronized(lock) { lock.notify() }
        }, delayMs, TimeUnit.MILLISECONDS)

        synchronized(lock) {
            lock.wait()
        }
    }
}
