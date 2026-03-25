package agent

interface Agent<T> {
    val info: AgentInfo
    val responseFormat: ResponseFormat<T>

    fun ask(userPrompt: String): T
}
