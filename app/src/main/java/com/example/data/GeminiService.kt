package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateClassmateReply(
        classmateName: String,
        classmateNickname: String,
        classmateBio: String,
        classmatePersonality: String,
        chatHistory: List<ChatMessage>,
        newMessage: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallbackResponse(classmatePersonality, newMessage)
        }

        // Construct chat history context for Gemini
        val historyPrompt = chatHistory.takeLast(10).joinToString("\n") { msg ->
            if (msg.isFromMe) "Tú: ${msg.text}" else "$classmateName: ${msg.text}"
        }

        val prompt = """
            Eres $classmateName (apodo: "$classmateNickname"), un estudiante de colegio en un curso de 37 personas.
            Tu personalidad/estilo es: $classmatePersonality.
            Tu bio es: "$classmateBio".
            Responde de manera natural, informal (tipo mensaje de WhatsApp de adolescente de colegio latinoamericano, usa emojis apropiados, no seas demasiado formal ni un robot de ayuda, habla de tú/vos/che/parce/wey según corresponda, mantén tus respuestas cortas, de 1 o 2 oraciones, tal como respondería un amigo en chat).
            
            Historial reciente del chat:
            $historyPrompt
            Tú: $newMessage
            
            Genera la respuesta de $classmateName a continuación:
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = "Eres un compañero de curso adolescente. Responde de forma muy natural y corta por chat privado."
                    )
                )
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: getFallbackResponse(classmatePersonality, newMessage)
        } catch (e: Exception) {
            getFallbackResponse(classmatePersonality, newMessage)
        }
    }

    private fun getFallbackResponse(personality: String, userMessage: String): String {
        // High quality offline fallback responses matching teenager styles
        val lowerMsg = userMessage.lowercase()
        return when {
            lowerMsg.contains("hola") || lowerMsg.contains("que tal") -> {
                val waveResponses = listOf(
                    "¡Epa! ¿Qué tal? 😎",
                    "Holaaa, ¿cómo va todo?",
                    "Holi, ¿qué andas haciendo?",
                    "¡Buenas! ¿Todo bien?",
                    "¡Hola! ¿Qué se cuenta?"
                )
                waveResponses.random()
            }
            lowerMsg.contains("tarea") || lowerMsg.contains("deber") || lowerMsg.contains("clase") || lowerMsg.contains("colegio") -> {
                listOf(
                    "Uf, ni me hables de la tarea jaja 😭",
                    "Creo que tocaba para mañana, pero ni empecé",
                    "Pasen la tarea al grupo por faaa 🙏",
                    "Yo ya la hice, después te paso una foto de guía"
                ).random()
            }
            lowerMsg.contains("jaja") || lowerMsg.contains("xd") -> {
                listOf(
                    "Jajaja literal 😂",
                    "¡Siii, tal cual!",
                    "Jajajaja no puedo",
                    "Es buenísimo 😂"
                ).random()
            }
            lowerMsg.contains("gracias") -> "¡De nada! De una 👍"
            else -> {
                when {
                    personality.contains("deportista", true) || personality.contains("fútbol", true) -> {
                        listOf(
                            "Dale de una, más tarde entreno y hablamos ⚽",
                            "Jaja qué crack, hoy hay partido sí o sí",
                            "Buenísimo, ¿te sumás al fúbol mañana?"
                        ).random()
                    }
                    personality.contains("nerd", true) || personality.contains("estudioso", true) -> {
                        listOf(
                            "Eso está interesante. ¿Viste el material de física?",
                            "Claro, si necesitas ayuda me avisas y te explico",
                            "Oye, ¿estudiaste para el examen de química?"
                        ).random()
                    }
                    personality.contains("rebelde", true) || personality.contains("gracioso", true) -> {
                        listOf(
                            "Jajaja de una, no le digas al profe 🧠",
                            "Qué locura mano, tremendo plan jaja",
                            "¡Sapeee! Todo tranqui por acá"
                        ).random()
                    }
                    else -> {
                        listOf(
                            "¡Siii, totalmente de acuerdo! 😄",
                            "Uff de una, me avisas cualquier cosa",
                            "Oye qué cool, luego te cuento más",
                            "Qué buena onda. Hablamos al rato 👍"
                        ).random()
                    }
                }
            }
        }
    }
}
