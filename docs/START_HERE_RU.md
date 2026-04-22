# Старт здесь

Этот проект уже подготовлен как основа под сюжетный Forge-мод, а не как пустой шаблон.

## Что уже есть

- `alexandrite` — первый предмет
- `alexandrite_block` — заражающее ядро метеорита
- глобальный world-event метеорита
- катсцена через `StartMeteorCutsceneS2CPacket` и `StopMeteorCutsceneS2CPacket`
- кастомные сущности:
  - `MeteorEntity`
  - `CameraAnchorEntity`

## Куда смотреть в первую очередь

- [TutorialMod.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/TutorialMod.java)
- [ModItems.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/item/ModItems.java)
- [ModBlocks.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/block/ModBlocks.java)
- [MeteorWorldEventHandler.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/world/event/MeteorWorldEventHandler.java)
- [MeteorCutsceneClient.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/client/MeteorCutsceneClient.java)

## Как это проверить

1. Запусти клиент:

```powershell
./gradlew runClient
```

2. Создай новый мир.
3. Зайди в мир первым игроком.
4. Через 1-5 минут должен сработать единоразовый ивент.
5. Все онлайн-игроки увидят катсцену полёта метеорита.

## Что править чаще всего

- тайминги события: [Config.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/Config.java)
- заражение блока: [AlexandriteSpreadingBlock.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/block/custom/AlexandriteSpreadingBlock.java)
- логика метеорита и камеры: [MeteorWorldEventHandler.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/world/event/MeteorWorldEventHandler.java)
- визуал метеорита: [MeteorRenderer.java](/D:/Main/Programing/Minecrat%20mods%20devlop/Forge-Tutorial-1.21.X-1-setup/src/main/java/net/kaupenjoe/tutorialmod/client/renderer/MeteorRenderer.java)
