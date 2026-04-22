# Карта проекта

## Основной вход

- [TutorialMod.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/TutorialMod.java)
  - регистрация предметов, блоков, сущностей, сообщений и конфигов

## Контент

- [ModItems.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/item/ModItems.java)
  - обычные предметы

- [ModBlocks.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/block/ModBlocks.java)
  - регистрация блоков

- [AlexandriteSpreadingBlock.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/block/custom/AlexandriteSpreadingBlock.java)
  - поведение заражающего блока

## Сущности

- [ModEntityTypes.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/entity/ModEntityTypes.java)
  - регистрация типов сущностей

- [MeteorEntity.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/entity/custom/MeteorEntity.java)
  - сам летящий метеорит

- [CameraAnchorEntity.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/entity/custom/CameraAnchorEntity.java)
  - сущность-камера для катсцен

## Клиент

- [ClientModEvents.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/client/ClientModEvents.java)
  - подключение рендеров

- [MeteorCutsceneClient.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/client/MeteorCutsceneClient.java)
  - переключение камеры у клиента

- [MeteorRenderer.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/client/renderer/MeteorRenderer.java)
  - внешний вид метеорита

## Сеть

- [ModMessages.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/network/ModMessages.java)
  - регистрация пакетов

- [StartMeteorCutsceneS2CPacket.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/network/packet/StartMeteorCutsceneS2CPacket.java)
  - запуск катсцены у клиента

- [StopMeteorCutsceneS2CPacket.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/network/packet/StopMeteorCutsceneS2CPacket.java)
  - завершение катсцены

## Мир

- [MeteorWorldEventHandler.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/world/event/MeteorWorldEventHandler.java)
  - серверная логика глобального ивента

- [MeteorEventSavedData.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/world/data/MeteorEventSavedData.java)
  - сохранение состояния “ивент уже был / когда стартует / где ударил”

## Утилиты

- [MathUtil.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/util/MathUtil.java)
  - расчёт поворота камеры и easing-функции
