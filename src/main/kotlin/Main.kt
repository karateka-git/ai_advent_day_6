import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.json.Json
import models.ChatCompletionRequest
import models.ChatCompletionResponse
import models.ChatMessage
import models.ChatRole

private const val CONFIG_FILE = "config/app.properties"
private const val MODEL = "DeepSeek V3.2"
private const val MAX_TOKENS = 400
private const val TEMPERATURE = 0.7
private const val API_URL_TEMPLATE =
    "https://agent.timeweb.cloud/api/v1/cloud-ai/agents/%s/v1/chat/completions"

private val json = Json { ignoreUnknownKeys = true }
private val consoleReader = BufferedReader(
    InputStreamReader(System.`in`, detectConsoleCharset())
)
private val systemConsole = System.console()

fun main() {
    val config = loadConfig()
    val agentId = config.getRequired("AGENT_ID")
    val userToken = config.getRequired("USER_TOKEN")
    val httpClient = HttpClient.newHttpClient()
    val conversation = mutableListOf(
        ChatMessage(
            role = ChatRole.SYSTEM.apiValue,
            content = buildSystemPrompt()
        )
    )

    println("Чат готов. Введите 'exit' или 'quit', чтобы завершить работу.")

    while (true) {
        print("${ChatRole.USER.displayName}: ")
        val prompt = readConsoleLine()?.trim() ?: break

        if (prompt.isEmpty()) {
            continue
        }

        if (prompt.equals("exit", ignoreCase = true) || prompt.equals("quit", ignoreCase = true)) {
            println("Чат завершён.")
            break
        }

        conversation += ChatMessage(role = ChatRole.USER.apiValue, content = prompt)

        try {
            val loading = LoadingIndicator()
            loading.start()

            val content = try {
                requestCompletion(
                    httpClient = httpClient,
                    agentId = agentId,
                    userToken = userToken,
                    conversation = conversation
                )
            } finally {
                loading.stop()
            }

            conversation += ChatMessage(role = ChatRole.ASSISTANT.apiValue, content = content)
            println("${ChatRole.ASSISTANT.displayName}: $content")
        } catch (error: Exception) {
            println("Не удалось выполнить запрос: ${error.message}")
        }
    }
}

private fun buildSystemPrompt(): String =
    "Ты полезный ассистент. Отвечай кратко, если пользователь не просит подробнее."

private fun requestCompletion(
    httpClient: HttpClient,
    agentId: String,
    userToken: String,
    conversation: List<ChatMessage>
): String {
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
    return completion.choices.firstOrNull()?.message?.content
        ?: error("Ответ API не содержит choices[0].message.content")
}

private fun detectConsoleCharset(): Charset {
    val nativeEncoding = System.getProperty("native.encoding")
    return if (nativeEncoding.isNullOrBlank()) {
        Charset.defaultCharset()
    } else {
        Charset.forName(nativeEncoding)
    }
}

private fun readConsoleLine(): String? = systemConsole?.readLine() ?: consoleReader.readLine()

private fun loadConfig(): Properties {
    val configPath = Path.of(CONFIG_FILE)
    require(Files.exists(configPath)) {
        "Файл конфигурации $CONFIG_FILE не найден. Создайте его на основе config/app.properties.example."
    }

    return Properties().apply {
        Files.newInputStream(configPath).use(::load)
    }
}

private fun Properties.getRequired(key: String): String =
    getProperty(key)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("В $CONFIG_FILE отсутствует обязательное свойство '$key'.")

private class LoadingIndicator {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    fun start() {
        running.set(true)
        thread = Thread {
            var step = 0
            while (running.get()) {
                val dots = ".".repeat(step % 4)
                val padding = " ".repeat(3 - dots.length)
                print("\rАссистент думает$dots$padding")
                Thread.sleep(350)
                step++
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        thread?.join(500)
        print("\r${" ".repeat(40)}\r")
    }
}
