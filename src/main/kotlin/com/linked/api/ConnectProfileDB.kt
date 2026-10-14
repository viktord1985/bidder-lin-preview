package com.linked.api

import com.linked.bidder.utils.Logger
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

//todo - for posts: the DB is limited to 3000 records; when reached, use 2-3 DBs on different accounts and load them into one array

class ConnectProfileDB() {

    private val profileTableId = 469667

    fun getList(): List<ConnectProfile> {

        Logger.logInfo("LinkedDB getProfileList")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val pageSize = 200
        var currentPage = 1
        val allProfiles = mutableListOf<ConnectProfile>()


        while (true) {
            try {
                val url =
                    "https://api.baserow.io/api/database/rows/table/$profileTableId/?user_field_names=true&page=$currentPage&size=$pageSize"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Token $baserowToken")
                    .get()
                    .build()

                Logger.logInfo("LinkedDB page: $currentPage")

                val response = client.newCall(request).execute()
                response.use {
                    if (!response.isSuccessful) {
                        //Logger.logInfo("LinkedDB - Unexpected code $response")
                        return allProfiles
                    }

                    val responseData = response.body?.string()
                    if (responseData != null) {

                        val profilesResult = parseJson(responseData)
                        val profiles = profilesResult.first
                        val next = profilesResult.second
                        allProfiles.addAll(profiles)
                        if (profiles.isEmpty() || !next) {
                            // No more data
                            return allProfiles
                        }
                        currentPage++
                    } else {
                        return allProfiles
                    }
                }
            } catch (e: IOException) {
                Logger.logInfo("LinkedDB - Request failed: ${e.message}")
                return emptyList()
            }
        }
    }

    private fun parseJson(jsonString: String): Pair<List<ConnectProfile>, Boolean> {
        val jsonObject = JSONObject(jsonString)

        val resultsArray = jsonObject.getJSONArray("results")
        val next = try {
            jsonObject.getString("next").isNotEmpty()
        } catch (e: Exception) {
            false
        }

        val connectProfiles = mutableListOf<ConnectProfile>()

        for (i in 0 until resultsArray.length()) {
            val postObject = resultsArray.getJSONObject(i)
            val id = postObject.getInt("id")
            val name = postObject.getString("Name")
            val description = postObject.getString("Description")
            val url = postObject.getString("Url")
            val status = postObject.getString("Status")
            val profile = try {
                postObject.getString("Profile")
            } catch (e: Exception) {
                ""
            }
            connectProfiles.add(ConnectProfile(id, name, description, url, status, profile))
        }

        return Pair(connectProfiles, next)
    }

    fun updateStatus(rowId: Int, newStatus: String, newProfile: String): Boolean {

        Logger.logInfo("LinkedDB updateStatus")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val url = "https://api.baserow.io/api/database/rows/table/$profileTableId/$rowId/?user_field_names=true"
        val json = """
            {
                "Status": "$newStatus",
                "Profile": "$newProfile"
            }
        """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .addHeader("Authorization", "Token $baserowToken")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            Logger.logInfo("LinkedDB - Request failed: ${e.message}")
            false
        }
    }
}

fun connectProfileSelfTest(contentPost: ConnectProfile): Boolean {
    if (contentPost.name.isEmpty()) {
        return false
    }
    if (contentPost.description.isEmpty()) {
        return false
    }
    if (contentPost.url.isEmpty()) {
        return false
    }
    if (contentPost.status.isEmpty()) {
        return false
    }
    return true
}

// Data class for Profile
data class ConnectProfile(
    val id: Int,
    val name: String,
    val description: String,
    val url: String,
    val status: String,
    val profile: String,
)

