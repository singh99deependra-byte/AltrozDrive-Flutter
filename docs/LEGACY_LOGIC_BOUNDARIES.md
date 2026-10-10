# Production logic map — AltrozDrive 1.1.20 reference

The original Kotlin/Java project is preserved byte-for-byte at source-file level under `android/legacy_kotlin_reference/` for read-only analysis. The list below maps the principal production responsibilities and makes explicit what cannot be changed accidentally during the Dart port.

## Connection and vehicle protocol

- `app/src/main/java/com/altrozdrive/app/VehicleConnectionService.kt`: Android service lifecycle and ongoing vehicle connection ownership. The Activity intentionally does not issue a second initial auto-connect attempt.
- `app/src/main/java/com/altrozdrive/connectnext/session/ConnectNextSessionController.kt`, `ConnectNextSessionState.kt`, `ConnectNextSessionStateMachine.kt`: session transitions and state-machine behavior.
- `app/src/main/java/com/altrozdrive/connectnext/connection/bluetooth/BTConnection.kt`, `BTTransportConnection.kt`, `BTConnectionReaderThread.kt`: Bluetooth transport and byte-stream reading.
- `app/src/main/java/com/altrozdrive/app/ConnectNextHtsMirror.kt`, `VehicleProtocolStack.kt`, and `connectnext/apptalk/*`: protocol framing, HTS/AppTalk message handling and response flow.
- `app/src/main/java/com/altrozdrive/connectnext/vehicle/client/utils/VehiclePacketParserRegistry.kt`: packet-parser selection; `VehiclePacketParser_X451.kt`, `X451PacketModel.kt`, `X451VehicleWrapper.kt`, and `X451PacketCodec.kt`: vehicle-specific packet decoding.
- `app/src/main/java/com/altrozdrive/app/X451LiveSignalDecoder.kt`: signal-to-value decoding consumed by the UI.
- `app/src/main/java/com/altrozdrive/app/BluetoothDiagnosticManager.kt`: diagnostics/negotiation lifecycle. Recent peer-19 negotiation/deadline fixes are part of the baseline and must not be weakened in a port.

Porting guardrails: preserve RFCOMM byte order and stream boundaries, HTS framing, request/response ordering, deadlines, peer-19 control/negotiation behavior, watchdog and reconnect policy, UID/VC routing, units and rounding, and which layer owns auto-connect. BLE-only transport libraries are not substitutes for RFCOMM.

## Persistence and trip behavior

- `app/src/main/java/com/altrozdrive/app/VehicleSnapshotDb.kt`: schema version 10, telemetry snapshots, trip rows and persistence logic.
- `VehicleDbBackup.kt`, `VehicleDbBackupScheduler.kt`, `VehicleDbBackupReceiver.kt`, `VehicleDbBootReceiver.kt`: backup, schedule and restore/startup lifecycle.
- `HomeScreen.kt`, `DriveScreen.kt`, `TripScreen.kt`, `FuelScreen.kt`, `FeatureScreens.kt`: how persisted trip and telemetry values are presented and aggregated.

Porting guardrails: do not change the on-device database file/schema without a migration plan; preserve existing records and migration versions; preserve trip start/stop thresholds and short-trip capture; don't add one row for every live signal change; preserve deduplication and six-month retention; preserve nightly backup behavior and one-backup-per-day limits.

## Maps, reminders and media

- `OpenRouteMap.kt`, `MapLibreBridge.java`, and `RouteMapProviderJava.java`: route map/provider integration.
- `VehicleReminderScheduler.kt`: insurance/PUC reminder scheduling and lifecycle behavior.
- `MediaNotificationListener.java` and the media-session helpers in `MainActivity.kt`: special notification-access permission, active media-session selection, metadata/artwork and transport controls.

Porting guardrails: use only recorded GPS/trip points; don't invent route geometry; preserve reminder timing and Android reboot handling; preserve media-session selection/playback/seek semantics. The Flutter screen layer includes visual destinations, but those subsystem implementations are not claimed complete yet.

## Safe migration approach

1. Treat 1.1.20 as the behavior baseline and keep the legacy reference untouched.
2. For each subsystem, write input/output fixtures from the original Kotlin implementation.
3. Port the logic to Dart (or use a narrowly scoped platform plugin for an Android-only OS primitive where pure Dart cannot access that API), with no semantic changes.
4. Compare old and new outputs for the same packet frames, database fixtures, trip journeys and route samples.
5. Only replace the runtime path after device-level parity tests pass. Keep debug install identity separate from the production package throughout migration.
