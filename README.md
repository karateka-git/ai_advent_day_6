# ai_advent_day_6

Минимальный каркас чата для работы с LLM по HTTP API.

## Настройка

1. Скопируйте `config/app.properties.example` в `config/app.properties`.
2. Заполните `AGENT_ID` и `USER_TOKEN`.
3. Соберите или запустите приложение одним из способов ниже.

## Сборка

```powershell
.\gradlew.bat build
```

## Рекомендуемый запуск

Для интерактивного чата на Windows используйте launcher из `installDist`:

```powershell
.\gradlew.bat build
.\gradlew.bat installDist
.\build\install\ai_advent_day_6\bin\ai_advent_day_6.bat
```

## Команды

- Вводите сообщения в консоли, чтобы продолжить диалог.
- Введите `exit` или `quit`, чтобы остановить приложение.
