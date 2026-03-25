# ai_advent_day_6

Простой CLI-агент для работы с LLM по HTTP API.

## Что умеет проект

- принимает запрос пользователя из консоли;
- отправляет запрос в LLM через HTTP API;
- получает ответ и выводит его в CLI;
- инкапсулирует логику работы с моделью в отдельном агенте `MrAgent`.

## Архитектура

В проекте есть общий контракт агента и конкретная реализация:

- `Agent<T>` — общий интерфейс агента;
- `ResponseFormat<T>` — описание ожидаемого формата ответа;
- `TextResponseFormat` — текстовый формат ответа;
- `MrAgent` — агент, который реализует `Agent<String>`;
- `Main.kt` — CLI-обвязка, которая читает ввод пользователя и вызывает агента.

## Настройка

1. Скопируйте `config/app.properties.example` в `config/app.properties`.
2. Заполните `AGENT_ID` и `USER_TOKEN`.

## Сборка

```powershell
.\gradlew.bat build
```

## Запуск

Рекомендуемый запуск для Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat installDist
.\build\install\ai_advent_day_6\bin\ai_advent_day_6.bat
```

## Команды в чате

- вводите сообщения в консоли, чтобы продолжить диалог;
- введите `exit` или `quit`, чтобы завершить работу.

## IDE

Для просмотра и навигации по коду удобнее всего открыть проект в `IntelliJ IDEA Community Edition`.
