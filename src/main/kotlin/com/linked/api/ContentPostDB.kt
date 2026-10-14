package com.linked.api

import com.linked.bidder.appConfig
import com.linked.bidder.utils.Logger
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

//Note: the DB needs a unique file name (otherwise it won't download)

const val baserowToken = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

class ContentPostDB() {

    private val postTableId = 465634

    fun getList(): List<ContentPost> {

        Logger.logInfo("LinkedDB getPostList")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val pageSize = 200
        var currentPage = 1
        val allPosts = mutableListOf<ContentPost>()

        while (true) {
            val url = "https://api.baserow.io/api/database/rows/table/$postTableId/?user_field_names=true&page=$currentPage&size=$pageSize"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Token $baserowToken")
                .get()
                .build()

            Logger.logInfo("LinkedDB page: $currentPage")

            try {
                val response = client.newCall(request).execute()
                response.use {
                    if (!response.isSuccessful) {
                        //Logger.logInfo("LinkedDB - Unexpected code $response")
                        return allPosts
                    }

                    val responseData = response.body?.string()
                    if (responseData != null) {
                        val postsResult = parseJson(responseData)
                        val posts = postsResult.first
                        val next = postsResult.second
                        allPosts.addAll(posts)
                        if (posts.isEmpty() || !next) {
                            // No more data
                            return allPosts
                        }
                        currentPage++
                    } else {
                        return allPosts
                    }
                }
            } catch (e: IOException) {
                Logger.logInfo("LinkedDB - Request failed: ${e.message}")
                return emptyList()
            }
        }
    }

    private fun parseJson(jsonString: String): Pair<List<ContentPost>, Boolean> {
        val jsonObject = JSONObject(jsonString)

        val resultsArray = jsonObject.getJSONArray("results")
        val next = try {
            jsonObject.getString("next").isNotEmpty()
        } catch (e: Exception) {
            false
        }

        val contentPosts = mutableListOf<ContentPost>()

        val downloadDirectory = File(appConfig.exportFolderPath + "downloads")
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs()
        }

        for (i in 0 until resultsArray.length()) {
            val postObject = resultsArray.getJSONObject(i)
            val id = postObject.getInt("id")
            val name = try {
                postObject.getString("Name")
            } catch (e: Exception) {
                ""
            }
            val text = postObject.getString("Text")
            val personal = try {
                postObject.getString("Personal")
            } catch (e: Exception) {
                "No"
            }

            val filesArray = postObject.getJSONArray("File")
            val files = mutableListOf<FileAttachment>()
            for (j in 0 until filesArray.length()) {
                val fileObject = filesArray.getJSONObject(j)
                val url = fileObject.getString("url")
                val fileName = fileObject.getString("visible_name")
                val filePath = appConfig.exportFolderPath + "downloads/$fileName"
                val file = FileAttachment(
                    url = url,
                    visibleName = fileName,
                    mimeType = fileObject.getString("mime_type"),
                    size = fileObject.getInt("size"),
                    localPath = if (File(filePath).exists()) File(filePath).canonicalPath else downloadFileAttachment(url, filePath)
                )
                files.add(file)
            }

            contentPosts.add(ContentPost(id, name, text, personal, files))
        }

        return Pair(contentPosts, next)
    }

    private fun downloadFileAttachment(fileUrl: String, filePath: String): String? {
        return try {
            Logger.logInfo("PostDB - downloadFile: $filePath")
            val url = URL(fileUrl)
            val file = File(filePath)
            url.openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.canonicalPath
        } catch (e: Exception) {
            Logger.logInfo("PostDB - Failed to download file: ${e.message}")
            null
        }
    }

    fun updatePersonal(rowId: Int, newPersonal: String): Boolean {

        Logger.logInfo("PostDB updatePersonal")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val url = "https://api.baserow.io/api/database/rows/table/$postTableId/$rowId/?user_field_names=true"
        val json = """
            {
                "Personal": "$newPersonal",
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
            Logger.logInfo("PostDB - Request failed: ${e.message}")
            false
        }
    }
}

fun contentPostSelfTest(contentPost: ContentPost): Boolean {
    //note: removing {name} and {isVideo}
    //note: {name} stays as a hidden column for migration, switching to id
//    if (contentPost.name.isEmpty()) {
//        return false
//    }
    if (contentPost.text.isEmpty()) {
        return false
    }
    if (contentPost.personal.isEmpty()) {
        return false
    }
    if (contentPost.files.isEmpty()) {
        return false
    }
    if (contentPost.files.first().localPath == null) {
        return false
    }
    return true
}

// Data class for Post
class ContentPost(
    val id: Int,
    val name: String,
    val text: String,
    val personal: String,
    val files: List<FileAttachment>
) {
    fun isVideo(): Boolean {
        return files.first().isVideo()
    }

    fun isNotPersonal(): Boolean {
        return personal == "No"
    }

    fun isNotUsedPersonal(): Boolean {
        return personal.isEmpty()
    }

    fun getPostID(): String {
        if (name.isNotEmpty()) {
            return name
        } else {
            return id.toString()
        }
    }
}

class FileAttachment(
    val url: String,
    val visibleName: String,
    val mimeType: String,
    val size: Int,
    var localPath: String?
) {
    fun isVideo(): Boolean {
        return mimeType.contains("video")
    }

    fun isImage(): Boolean {
        return mimeType.contains("image")
    }
}
