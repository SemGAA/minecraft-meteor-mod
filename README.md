# Minecraft Meteor Mod

Forge-мод для Minecraft 1.21 с глобальным событием падения метеорита, кастомной сущностью, визуальными эффектами и серверной логикой обработки событий.

## Что внутри

- `MeteorWorldEventHandler` управляет жизненным циклом события через player/server/world events.
- `MeteorEntity` летит к рассчитанной точке удара, синхронизируется с клиентом и оставляет огненный след.
- `MeteorFlashS2CPacket` отправляет клиентский flash-эффект перед входом метеорита в атмосферу.
- Обработка удара создает кратер, остаток метеорита, огонь, звук, частицы и эффект опасной зоны.
- `MeteorEventSavedData` хранит состояние события между тиками мира.
- Команда `/meteor start` позволяет вручную запустить событие для проверки.
- `MeteorStaffItem` дает отдельный игровой способ вызвать механику.

## Что я сделал лично

- Реализовал механику падения метеорита как игровое событие, а не просто отдельный предмет или блок.
- Настроил обработку player/server/world events для запуска, обновления и завершения события.
- Добавил `MeteorEntity`, расчет траектории, точку удара и серверную синхронизацию поведения.
- Сделал клиентский flash packet, частицы, звук, кратер, остаток метеорита и эффект опасной зоны.
- Подготовил репозиторий под портфолио: чистый `.gitignore`, понятная структура и README для проверки кода.

## Почему это флагманский проект

Проект показывает не просто добавление предмета, а работу с игровой архитектурой Forge:

- подписка на события входа игрока, server tick и регистрацию команд;
- расчет траектории, точки обзора и позиции удара;
- кастомная entity с серверным и клиентским поведением;
- сетевой пакет для визуального feedback;
- генерация визуальных эффектов через частицы, звук и изменение блоков мира.

## Стек

- Java 21
- Minecraft Forge 1.21
- Gradle
- Parchment mappings

## Быстрый старт

```powershell
./gradlew compileJava
./gradlew runClient
```

Для Minecraft 1.21 используйте JDK 21.

## Основные файлы

- `src/main/java/net/kaupenjoe/tutorialmod/world/event/MeteorWorldEventHandler.java`
- `src/main/java/net/kaupenjoe/tutorialmod/entity/custom/MeteorEntity.java`
- `src/main/java/net/kaupenjoe/tutorialmod/network/packet/MeteorFlashS2CPacket.java`
- `src/main/java/net/kaupenjoe/tutorialmod/item/custom/MeteorStaffItem.java`
- `src/main/resources/META-INF/mods.toml`

## Проверка

```powershell
./gradlew compileJava
```

Команда проверяет Java-код, регистрацию типов, сетевых пакетов и зависимости Forge.
