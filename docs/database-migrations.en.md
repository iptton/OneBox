# Database migrations follow app releases

Starting with app 140 (1.4.0), a database whose schema changed takes that app release's `versionCode` as its target version. Database versions are no longer bumped per feature. The version is declared explicitly and is never read at runtime from `BuildConfig`.

## Release baselines

Take the `@Database` declaration on a published Git tag as the baseline, not the highest schema number you can find in the working tree:

| App release | AppDatabase | FeatureDatabase |
| --- | --- | --- |
| 139 / tag `1.3.9` | 2 | 4 |
| 140 / not yet released | 140 | 140 |

All of release 140's increments are collected in `core/database/src/main/java/com/shifenmiao/database/Release140Migrations.kt`:

- **AppDatabase**: memory, skills, per-session memory policy, and theme glass-border alpha.
- **FeatureDatabase**: AI detection, health records, household item/location icons, cycle records, and backfill for wheel fields, indexes and history titles.
- Historical migrations from before the published release are kept. An old install first reaches the database baseline of 139, then upgrades to 140.

## Development rules

1. **Before 140 ships**, new features keep appending to this release's migration and re-export both `140.json` schemas. Do not add a 140→141 step or per-feature version numbers.
2. **After 140 ships**, freeze that release's migration and schemas. The next schema change opens a new migration entry keyed to the app release it will ship in. A database with no structural change does not need a version bump.
3. Never edit a published schema, never fake the DB 139 starting point, and never paper over a missed migration with a destructive migration or a version downgrade that wipes data.
4. AppDatabase 3/4 and FeatureDatabase 5–9 were development versions during the unreleased cycle. They upgrade through the same set of idempotent SQL as 140. Those schemas are kept only as development-version compatibility references — **they are not a release policy**.
5. New tables, added columns, indexes and defaults must match the Room entities. Backfill history titles only on the first addition of `wheelTitle`, so snapshots already saved by a development build are not overwritten.
6. When you change an existing release draft, verify all three starting points: the published baseline, the development version, and a fresh database. If a development database is already stamped 140 and the entity structure changed again before 140 shipped, handle the development data backup/rebuild separately — do not stack another production version number on top of unreleased features.

## Verification

From the repository root:

```sh
./gradlew :core:database:compileOneboxArm64DebugKotlin
```

Compiling makes Room's KSP export the actual target schema. Before a release, do a real upgrade test from an old APK and check the published baseline and the development-version upgrade for tables, columns, defaults, foreign keys, indexes, and user data retention. Throwaway verification scripts belong outside the repo; migration tests that need to be maintained long-term belong in the project's Android/Room test setup.
