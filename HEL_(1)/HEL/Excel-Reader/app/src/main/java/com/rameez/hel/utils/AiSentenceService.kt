package com.rameez.hel.utils


import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class AiSentenceService {

    private val TAG = "AI_SERVICE"

    // Google Apps Script Web App URL  (it looks something like: https://script.google.com/macros/s/abcabcabcabc_xyzxyzxyzxyzxyz-eeeeeeeeeexxxx/exec)
    private val scriptUrl =
        "https://script.google.com/macros/s/xxxx/exec"

    // OkHttp client with extended timeouts
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateNewsSentence(
        context: Context,
        word: String,
        apiKey: String,
        sources: List<String> = emptyList(),
        retryCount: Int = 2
    ): String? {
        if (!context.isInternetAvailable()) {
            Log.e(TAG, "No internet connection")
            return null
        }

        return withContext(Dispatchers.IO) {
            repeat(retryCount + 1) { attempt ->
                try {
                    Log.d(TAG, "Attempt ${attempt + 1}: Requesting sentence for word: '$word'")

                    val json = JSONObject().apply {
                        put("word", word)
                        put("apiKey", apiKey)
                        put("sources", JSONArray(sources))
                    }

                    val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder().url(scriptUrl).post(requestBody).build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.e(TAG, "HTTP error: ${response.code} ${response.message}")
                            return@use null
                        }

                        val body = response.body?.string()
                        Log.d(TAG, "RAW RESPONSE: $body")
                        if (body.isNullOrBlank()) {
                            Log.e(TAG, "Empty response from server")
                            return@use null
                        }

                        val result = extractSentenceWithSource(body)
                        if (result == null || result.first.isNullOrBlank()) {
                            Log.e(TAG, "Failed to extract sentence from response")
                            return@use null
                        }

                        // Format the response with source if available
                        val (sentence, sourceUrl, sourceName) = result
                        val formattedResponse = if (!sourceUrl.isNullOrBlank()) {
                            // Append URL naturally at the end
                            "$sentence\n\nSource: ${sourceName ?: "News Source"}\n$sourceUrl"
                        } else {
                            sentence
                        }

                        Log.d(TAG, "Generated sentence: $formattedResponse")
                        return@withContext formattedResponse
                    }

                } catch (e: java.net.SocketTimeoutException) {
                    Log.e(TAG, "Timeout on attempt ${attempt + 1}", e)
                    if (attempt == retryCount) return@withContext null
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during sentence generation on attempt ${attempt + 1}", e)
                    return@withContext null
                }
            }
            null
        }
    }

    private fun extractSentenceWithSource(responseJson: String): Triple<String?, String?, String?>? {
        if (responseJson.trim().startsWith("<!DOCTYPE", ignoreCase = true)) {
            Log.e(TAG, "Server returned HTML instead of JSON. Check Script permissions or API Key.")
            return null
        }

        return try {
            val root = JSONObject(responseJson)

            if (root.has("error")) {
                val errorMsg = root.optString("error")
                Log.e(TAG, "Google Apps Script Error: $errorMsg")
                return null
            }

            val text = root.optString("text").trim()
            val sourceUrl = root.optString("source").trim()
            val sourceName = root.optString("source_name").trim()

            // Filter out proxy/search URLs if the script missed them
            val isProxyUrl = sourceUrl.contains("vertexsearch.cloud.google.com", ignoreCase = true) ||
                    sourceUrl.contains("vertexaisearch.cloud.google.com", ignoreCase = true) ||
                    sourceUrl.contains("google.com/search", ignoreCase = true)

            val cleanSourceUrl = if (isProxyUrl) "" else sourceUrl

            if (text.isNotBlank()) {
                return Triple(text, cleanSourceUrl, sourceName)
            }

            val candidates = root.optJSONArray("candidates")
            val fallbackText = candidates
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.trim()

            if (fallbackText.isNullOrBlank()) {
                Log.e(TAG, "No usable text found in JSON response")
                return null
            }

            Triple(fallbackText, null, null)

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Parse error while extracting sentence. Response was: $responseJson",
                e
            )
            null
        }
    }

    suspend fun generateNewsArticle(
        context: Context,
        words: List<String>,
        apiKey: String,
        sources: List<String>
    ): String? {

        if (!context.isInternetAvailable()) return null

        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("type", "article")
                    put("apiKey", apiKey)
                    put("words", JSONArray(words))
                    put("sources", JSONArray(sources))
                }

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(scriptUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "HTTP error: ${response.code}")
                        return@use null
                    }

                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Article response: $responseBody")

                    val result = extractSentenceWithSource(responseBody)
                    if (result == null || result.first.isNullOrBlank()) {
                        return@use null
                    }

                    val (text, sourceUrl, sourceName) = result

                    // Format the article with source information
                    if (!sourceUrl.isNullOrBlank()) {
                        // Append URL naturally at the end
                        val formatted = "$text\n\nSource: ${sourceName ?: "News Source"}\n$sourceUrl"
                        Log.d(TAG, "Formatted article with source: $formatted")
                        formatted
                    } else {
                        text
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Article generation failed", e)
                null
            }
        }
    }
}
