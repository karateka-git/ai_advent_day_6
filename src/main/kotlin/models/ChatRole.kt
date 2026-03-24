package models

enum class ChatRole(val apiValue: String, val displayName: String) {
    SYSTEM("system", "Система"),
    USER("user", "Пользователь"),
    ASSISTANT("assistant", "Ассистент")
}
