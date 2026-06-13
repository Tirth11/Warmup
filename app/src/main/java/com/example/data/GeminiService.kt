package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    suspend fun callGemini(systemInstruction: String, userPrompt: String, returnJson: Boolean = false): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is blank or placeholder")
            return@withContext "API_KEY_MISSING"
        }

        try {
            // Build the standard REST JSON body
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                }
                put("systemInstruction", systemInstructionObj)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.4)
                    if (returnJson) {
                        put("responseMimeType", "application/json")
                    }
                }
                put("generationConfig", generationConfig)
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(mediaTypeJson))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: $bodyStr"
                    Log.e(TAG, errMsg)
                    return@withContext "ERROR: $errMsg"
                }

                if (bodyStr.isNullOrEmpty()) {
                    return@withContext "ERROR: Empty response body"
                }

                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                text ?: "ERROR: No text returned in candidates"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API exception", e)
            "ERROR: ${e.message}"
        }
    }

    suspend fun generateMealPlan(scheduleStr: String, profileStr: String): String {
        val systemInstruction = """
            You are a nutrition-conscious, time-aware Chef AI assistant for Cal-Cook.
            You must analyze the user's daily Google/Outlook calendar events and generate a customized meal plan (Breakfast, Lunch, Dinner, Snack).
            
            ASSIGN MEALS BY TIME WINDOWS:
            - If free time is < 10 mins: Assign a "Desk Lunch" (grab-and-go/no-cooking, e.g., Protein bars/shakes, raw fruits).
            - If free time is 10-30 mins: Assign quick-prep meals (salads, simple wraps).
            - If free time is 30-60 mins: Assign moderate cooking (pasta, stir-fries).
            - If free time is 60+ mins: Assign complex items (roasts, salmon, slow cooker dishes).
            
            Consider these rules:
            - Respect dietary restriction (vegan, keto, gluten-free, etc.).
            - Respect list of kitchen equipment (appliances owned).
            - Exclude disliked ingredients.
            - Give budget adherence estimations and flags.
            
            Always respond with a VALID JSON object of a list of meals. Strictly return only the JSON array of objects without markdown headers:
            [
              {
                "mealType": "Breakfast" | "Lunch" | "Dinner" | "Snack",
                "recipeTitle": "Recipe Name",
                "description": "Short appetizing description",
                "prepCookTime": 15,
                "baseCost": 3.50,
                "calories": 420,
                "dietaryFlags": "comma-separated list of flags compatible",
                "appliancesRequired": "comma-separated appliances needed",
                "ingredients": "Ingredient1:Qty1:Unit1, Ingredient2:Qty2:Unit2",
                "instructions": "Step 1 text|Step 2 text|Step 3 text",
                "statusWarning": null or "Warning message like: Exceeds daily budget allocation"
              }
            ]
        """.trimIndent()

        val userPrompt = """
            Schedule Event Windows of Today:
            $scheduleStr

            User Profile preferences:
            $profileStr
            
            Generate today's delicious, optimized, budget-capped, and time-aware meal plan. Ensure it is strict JSON. Do not write anything outside the json array.
        """.trimIndent()

        return callGemini(systemInstruction, userPrompt, returnJson = true)
    }

    suspend fun generateRecipeAI(prompt: String, profileStr: String): String {
        val system = """
            You are a master culinary developer. Create a customized recipe matching:
             1. User request input.
             2. User dietary requirements and owned appliances.
            
            Return a JSON object:
            {
              "title": "Recipe Title",
              "description": "Appetizing description",
              "prepTime": 10,
              "cookTime": 15,
              "calories": 380,
              "dietaryFlags": "vegan,gluten-free",
              "appliancesRequired": "Oven",
              "baseCost": 4.50,
              "ingredients": "Double Tomato:2:pieces, Basil:5:leaves, Rice:1:cup",
              "instructions": "Instruction 1|Instruction 2|Instruction 3"
            }
        """.trimIndent()

        return callGemini(system, "Request: $prompt\nProfile: $profileStr", returnJson = true)
    }

    suspend fun getSmartSubstitution(ingredient: String, context: String): String {
        val system = """
            You are a smart kitchen substitute engine. Given an ingredient and a context (like "Traveling", "In a rush", "Working from home", "Allergy"), propose a direct substitute ingredient with a scientific reason (e.g. nutritional equivalence, flavor matching, or prep-time saving).
            
            Return JSON:
            {
              "originalIngredient": "$ingredient",
              "substituteIngredient": "Substitute name",
              "reason": "Specific expert explanation of why this works for $context"
            }
        """.trimIndent()

        return callGemini(system, "Ingredient: $ingredient, Context: $context", returnJson = true)
    }
}
