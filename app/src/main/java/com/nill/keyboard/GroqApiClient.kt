package com.nill.keyboard

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GroqApiClient(private val apiKey: String) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val BASE = "https://api.groq.com/openai/v1"

    /**
     * Convert Banglish (Bengali in Roman/English script) to proper Unicode Bengali.
     * Uses Groq's llama-3.1-8b-instant (free, fast).
     */
    fun convertBanglish(text: String, callback: (String) -> Unit) {
        val systemPrompt = """You are an expert Banglish to Bengali converter.
Banglish means Bengali language written using English/Roman letters.
Convert the given Banglish input to proper Unicode Bengali script.
Rules:
- Respond ONLY with the converted Bengali text. Nothing else.
- No explanation, no notes, no quotes, just the Bengali text.
- Preserve punctuation and sentence structure.
- Handle natural Bangladeshi/Bengali speech patterns.

Examples:
Input: amar naam Shahed → Output: আমার নাম শাহেদ
Input: ami bhalo achi → Output: আমি ভালো আছি
Input: tumi kemon acho → Output: তুমি কেমন আছো
Input: ami coding korte pocondo kori → Output: আমি কোডিং করতে পছন্দ করি
Input: apnar sathe kotha bolte valo lagche → Output: আপনার সাথে কথা বলতে ভালো লাগছে"""

        val body = JSONObject().apply {
            put("model", "llama-3.1-8b-instant")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            })
            put("max_tokens", 500)
            put("temperature", 0.1)
        }

        val request = Request.Builder()
            .url("$BASE/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(text) // Return original on network failure
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.has("error")) {
                        callback(text)
                        return
                    }
                    val result = json
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                    callback(result)
                } catch (e: Exception) {
                    callback(text)
                }
            }
        })
    }

    /**
     * Transcribe audio file using Groq Whisper (free, supports Bengali & English).
     * language: "bn" for Bengali, "en" for English
     */
    fun transcribeAudio(file: File, language: String, callback: (String) -> Unit) {
        val mimeType = "audio/m4a"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mimeType.toMediaType())
            )
            .addFormDataPart("model", "whisper-large-v3")
            .addFormDataPart("language", language)
            .addFormDataPart("response_format", "json")
            .build()

        val request = Request.Builder()
            .url("$BASE/audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.has("error")) {
                        callback("")
                        return
                    }
                    callback(json.optString("text", "").trim())
                } catch (e: Exception) {
                    callback("")
                }
            }
        })
    }
}
