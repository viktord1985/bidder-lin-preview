package com.linked.api


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit


fun openAIRequest(text: String, prompt: String): String {
    try {
        Logger.logInfo("OpenAiRequest")
        Logger.logInfo("prompt: $prompt")

        var fullPrompt = prompt

        if (fullPrompt.contains("{Here is the text}")) {
            fullPrompt = fullPrompt.replace("{Here is the text}", "Here is the text: $text")
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", "You are a text analysis assistant.")
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty(
                    "content",
                    fullPrompt
                )
            })
        }

        val body = JsonObject().apply {
            addProperty("model", "gpt-4o")
            add("messages", messages)
        }.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${Config.openAIToken}")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val responseBody = response.body?.string()
        if (responseBody != null) {

            //Logger.logInfo("OpenAiRequest: $responseBody}")

            val jsonObj = JsonParser.parseString(responseBody).asJsonObject
            val responseText = jsonObj.getAsJsonArray("choices")
                .get(0).asJsonObject.getAsJsonObject("message")
                .get("content").asString
            val finishReason = jsonObj.getAsJsonArray("choices")
                .get(0).asJsonObject.get("finish_reason").asString

            if (finishReason != "stop") {
                Logger.logError("OpenAiRequest ---")
                Logger.logError("OpenAiRequest messages: ${messages.asString}")
                Logger.logError("OpenAiRequest ---")
                Logger.logError("OpenAiRequest finishReason: $finishReason")
                Logger.logError("OpenAiRequest ---")
                Logger.logError("OpenAiRequest responseBody: $responseBody")
            }

            Logger.logInfo("OpenAiRequest finishReason: $finishReason, responseText: $responseText")

            if (responseText.toLowerCase() == "nope" || responseText.toLowerCase() == "nope.") {
                return ""
            } else {
                return responseText
            }
        } else {
            return ""
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Logger.logError("Error on OpenAiRequest: $e")
        return ""
    }
}




