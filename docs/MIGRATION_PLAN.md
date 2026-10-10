# Option C migration plan and behavior contract

## Source baseline
`AltrozDrive-1.1.20-CREAM-GOLD-UI-CONSISTENCY-source.zip` (available in the user's Library). The source archive contains Kotlin/Java Android code for the UI, vehicle connection, HTS mirror, packet codec/decoder, database, map bridge, reminders and backup scheduling. This prototype preserves a copy under `android/legacy_kotlin_reference/` for reference only; it is not linked into a Flutter runtime.

## Non-negotiable parity rules
- Do not change Bluetooth/RFCOMM/HTS framing, peer-19 control/negotiation, deadlines, reconnect rules or state machine as a side effect of UI migration.
- Do not change packet parsing, UID/VC routing, vehicle decoder mappings or units/rounding.
- Do not change trip start/end conditions, short-trip handling, route-point capture or route rendering semantics.
- Preserve the database file, schema, migrations, data retention, deduplication and backup/restore semantics.
- Preserve permissions, notifications, media integration, reminders and settings.
- No fake telemetry: until data source integration is complete, UI must say unavailable/offline.

## Recommended implementation sequence
1. UI shell and design tokens (this prototype).
2. Native bridge contract, initially read-only: connection state and a timestamped vehicle snapshot stream.
3. Read-only database access / repository parity and existing trip list.
4. Connection lifecycle, reconnect and packet regression tests.
5. Decoder parity test vectors (same raw frame -> same decoded values).
6. Trip lifecycle, short drive, GPS and route tests.
7. Media, notification, reminders, permissions, backup/restore.
8. Performance, background/foreground, process death, upgrade/install and data-preservation testing.

## Release gate
Do not declare Option C successful until the Flutter project compiles with the chosen SDK, the app installs, existing data is preserved, and each parity scenario is tested against the original app. If Option C cannot be made stable, Option B is the fallback: Flutter UI with the original Kotlin vehicle layer retained.
