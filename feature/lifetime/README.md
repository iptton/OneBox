# Lifetime Feature Module

"时光沙漏 (LifeTime)" — visualize your life journey: an animated hourglass of time lived vs.
remaining, plus frequency-event stats, personal milestones and holiday countdowns.

## Features

- **Hourglass hero card**: animated hourglass with time lived / time remaining, refreshed every second
- **Life progress**: battery-style progress toward the expected lifespan (configurable in Settings)
- **Frequency events**: track how often you do things (eating, sleeping, exercise, festivals…)
- **Personal milestones**: memorable days and goal dates with reached / days-left states
- **Countdown events**: one-time or annually recurring targets, including lunar-calendar recurrence
- **Preset holiday seeding**: on first launch, upcoming holidays (90-day window) are seeded as
  countdowns. The festival set is split by language (`LunarHolidayProvider`):
  - `zh`: Chinese festivals — 14 lunar (春节/端午/中秋…) + 21 solar
  - non-`zh`: international festivals only (New Year, Valentine's, Mother's/Father's Day,
    Halloween, Thanksgiving, Black Friday, Christmas…); lunar festivals are excluded
- **AI Agent tools**: `add_countdown_event`, `add_life_milestone`, `query_life_events`,
  `delete_lifetime_entry` (package `com.wanbaohe.lifetime.ai.tool`)

## Architecture

```
feature/lifetime/
├── component/          # Decompose components (business logic)
├── data/               # Repositories (DataStore + Room), holiday providers
├── domain/             # Pure calculators (LifeTime / Frequency / Countdown / PersonalMilestone)
├── screen/             # UI screens (main + add/detail per entry type, welcome, settings)
├── ui/                 # Reusable UI components (cards, hero hourglass)
└── util/               # PresetDisplayNames: preset Chinese keys → localized strings
```

- **Persistence**: birth date & expected age in DataStore (`LifeTimeRepository`);
  frequency events / countdowns / milestones in Room (`FeatureDatabase`)
- **Preset display names**: preset entries are seeded with Chinese `name` values that double as
  stable mapping keys; the UI maps them to string resources via `util/PresetDisplayNames.kt`
  (`presetEventNameRes` for non-Compose callers such as AI tools)
- **Holiday source**: solar/lunar festival tables come from the shared `feature/calendar`
  (`LunarCalendarCalculator`); the international-only additions (Thanksgiving etc.) are computed
  inside `LunarHolidayProvider` — `feature/calendar` is intentionally untouched

## Key Components

- **LifeTimeComponent**: root component, seeds presets on first entry, 1s real-time updates
- **CountdownSeedService / FrequencyEventRepository**: first-launch preset seeding (locale-split)
- **LunarHolidayProvider**: `HolidayProvider` implementation, zh / international festival sets
- **LifeTimeCalculator**: lived / remaining time arithmetic (pure functions)

## Technical Highlights

- ✅ Decompose navigation + Hilt DI
- ✅ Uses `AppTheme.colors` for all colors (no hardcoded values)
- ✅ Real-time countdown with 1-second refresh rate
- ✅ Immutable data classes for Compose optimization
- ✅ 12 locales; preset names localized via string-resource mapping
