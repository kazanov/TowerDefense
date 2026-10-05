# Tower Defense (Android / Kotlin)

MVP игры в жанре Tower Defense по ТЗ: 10 уровней, 4 башни × 3 уровня, 5 типов врагов,
10 волн на уровень, постоянные улучшения, сохранение прогресса, звук, вибрация, полностью офлайн.

## Сборка APK через GitHub Actions (без Android Studio)

1. Создайте репозиторий на GitHub и загрузите в него **содержимое** этой папки
   (так, чтобы `settings.gradle.kts` и папка `.github` лежали в корне репозитория).
2. Сборка запускается автоматически при каждом push (или вручную: Actions → Build APK → Run workflow).
3. Через 5–8 минут откройте завершившийся запуск → внизу раздел **Artifacts** → `TowerDefense-apk`.
4. В архиве два файла:
   - `app-release.apk` — **устанавливайте его**: он быстрее (Compose в debug-сборке заметно тормозит);
   - `app-debug.apk` — для отладки.

> Gradle Wrapper не нужен: workflow сам ставит Gradle 8.9. Если загружаете через веб-интерфейс GitHub,
> папку `.github` удобнее создать вручную: Add file → Create new file → `.github/workflows/build.yml`.

Release подписан отладочным ключом — подходит для установки, но для Google Play нужен свой ключ.

## Технологии

Kotlin 2.0, Jetpack Compose + Canvas (вариант из п. 23 ТЗ), DataStore Preferences, minSdk 26, landscape.
Звуки и музыка синтезируются в коде при первом запуске — аудиофайлы не нужны.

## Структура (MVVM + Clean Architecture)

```
app/src/main/
├── assets/config/          ← баланс игры: towers.json, enemies.json, levels.json, upgrades.json
└── java/com/game/towerdefense/
    ├── data/               config (загрузка JSON), local + repository (DataStore), audio, vibration,
    │                       analytics (Logcat), monetization (заглушка)
    ├── domain/             model, repository (интерфейс), usecase, service (Analytics, AdsProvider)
    ├── game/               engine (GameEngine, пул объектов), map, towers, enemies, entities, waves
    ├── presentation/       menu, levels, upgrades, settings, game (экран, HUD, рендер), ui
    └── di/AppContainer.kt  ручной DI
```

## Балансировка без изменения кода

- `towers.json` — урон/дальность/скорость/стоимость каждого уровня башни, радиус взрыва,
  замедление, пробитие брони, `sellRatio` (0.7 = 70% при продаже).
- `enemies.json` — HP, скорость (1.0 = `baseSpeed` px/с), награда, броня, урон базе.
- `levels.json` — маршруты (waypoints в клетках сетки 16×9; соседние точки — по горизонтали
  или вертикали; первая точка может быть за краем поля), препятствия, `hpMultiplier`,
  наборы волн (`waveSets`) или собственные `waves` у уровня.
- `upgrades.json` — постоянные улучшения (стартовые деньги, урон башен, HP базы).

## Ключевые решения по ТЗ

- Игровой цикл: фиксированный шаг 1/60 с, не зависит от FPS; ускорение x2 = два шага за кадр.
- Пауза: останавливает движение, атаки, таймеры волн; автопауза при сворачивании.
- Приоритет цели — враг, дальше всех продвинувшийся по маршруту.
- Урон: `damage × (1 − armor × (1 − armorPenetration))`; у Magic Tower пробитие 50%.
- Звёзды: 0–2 HP потеряно → 3★, 3–7 → 2★, 8+ → 1★.
  Награда за победу: 100 + 25 за каждую звезду сверх первой. За поражение — 5 монет за пройденную волну.
- Object pooling для врагов, снарядов и эффектов.
