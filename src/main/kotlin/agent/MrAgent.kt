package agent

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import models.ChatCompletionRequest
import models.ChatCompletionResponse
import models.ChatMessage
import models.ChatRole

private const val MODEL = "DeepSeek V3.2"
private const val MAX_TOKENS = 400
private const val TEMPERATURE = 0.7
private const val API_URL_TEMPLATE =
    "https://agent.timeweb.cloud/api/v1/cloud-ai/agents/%s/v1/chat/completions"

class MrAgent(
    private val httpClient: HttpClient,
    private val agentId: String,
    private val userToken: String,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) : Agent<String> {
    private val json = Json { ignoreUnknownKeys = true }

    override val responseFormat: ResponseFormat<String> = TextResponseFormat

    private val conversation = mutableListOf(
        ChatMessage(
            role = ChatRole.SYSTEM.apiValue,
            content = buildSystemPrompt()
        )
    )

    override val info = AgentInfo(
        name = "MrAgent",
        description = "CLI-агент для диалога с LLM через HTTP API.",
        model = MODEL
    )

    override fun ask(userPrompt: String): String {
        conversation += ChatMessage(role = ChatRole.USER.apiValue, content = userPrompt)

        val requestBody = json.encodeToString(
            ChatCompletionRequest(
                model = MODEL,
                messages = conversation,
                temperature = TEMPERATURE,
                maxTokens = MAX_TOKENS
            )
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL_TEMPLATE.format(agentId)))
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("Authorization", "Bearer $userToken")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))

        if (response.statusCode() !in 200..299) {
            error("API вернул статус ${response.statusCode()}: ${response.body()}")
        }

        val completion = json.decodeFromString<ChatCompletionResponse>(response.body())
        val rawContent = completion.choices.firstOrNull()?.message?.content
            ?: error("Ответ API не содержит choices[0].message.content")
        val content = responseFormat.parse(rawContent)

        conversation += ChatMessage(role = ChatRole.ASSISTANT.apiValue, content = content)
        return content
    }

    private fun buildSystemPrompt(): String =
        "$systemPrompt\n\nТребования к формату ответа:\n${responseFormat.formatInstruction}"

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT =
            "Ты полезный ассистент. Отвечай кратко, если пользователь не просит подробнее."
    }
}
