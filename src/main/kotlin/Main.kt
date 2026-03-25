import agent.Agent
import agent.MrAgent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.http.HttpClient
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import models.ChatRole

private const val CONFIG_FILE = "config/app.properties"

private val consoleReader = BufferedReader(
    InputStreamReader(System.`in`, detectConsoleCharset())
)
private val systemConsole = System.console()

fun main() {
    val config = loadConfig()
    val agent: Agent<String> = MrAgent(
        httpClient = HttpClient.newHttpClient(),
        agentId = config.getRequired("AGENT_ID"),
        userToken = config.getRequired("USER_TOKEN")
    )

    println("Чат готов. Введите 'exit' или 'quit', чтобы завершить работу.")
    println("Агент: ${agent.info.name}")
    println("Описание: ${agent.info.description}")
    println("Модель: ${agent.info.model}")

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

        try {
            val loading = LoadingIndicator()
            loading.start()

            val content = try {
                agent.ask(prompt)
            } finally {
                loading.stop()
            }

            println("${ChatRole.ASSISTANT.displayName}: $content")
        } catch (error: Exception) {
            println("Не удалось выполнить запрос: ${error.message}")
        }
    }
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
