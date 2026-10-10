# AltrozDrive 1.1.18 — Peer-ID, short-trip and route contamination fixes

## Changes made

### ConnectNext Peer 19 handshake
- Resolve the supported-app client ID from the currently decoded HU `PEER_DEVICE_ID` (the phone's own ID in this session), falling back to 20 only before an HU identity is known.
- Use the negotiated client ID consistently in the `Sl_Supported_Apps` length calculation, Supported Apps text and `Req_Remote_Client_Con` service requests.
- Recognize the exact HTS binary length-control signature on message type `0x13` as well as `0x14`. The captured Peer 19 HU used `0x13`; the Peer 20 session used `0x14`.
- Preserve the existing HTS/RFCOMM transport, framing, acknowledgement gate and recovery path.

### Short-trip start, distance and duration
- Mark ODO and Trip A/B values as fresh only when their corresponding X451 frame is actually decoded. A carried-forward ODO value can no longer mask the faster fractional Trip A/B updates.
- Detect movement from a fresh ODO increase or a validated Trip A/B increase of at least 0.05 km. This permits trip start before integer ODO advances by one kilometre.
- Persist current active-trip values/duration at most once per second even between movement-counter changes; only confirmed movement refreshes the inactivity timestamp.
- Prefer Trip A/B deltas for sub-kilometre distance, fall back to ODO when needed, and keep distance monotonic if a counter briefly lags/resets.
- Keep the existing DB schema and trip/history UI unchanged.

### Route contamination / GPS quality
- Validate fix timestamps, freshness, accuracy and coordinates before recording. Store the fix's own timestamp rather than callback time.
- Suppress network-provider fixes shortly after an accepted GPS fix to avoid alternating two different coordinate sources.
- Reject out-of-order points and implausible point-to-point jumps while recording and when rendering legacy route data.
- Separate 48-hour pending-point retention from trip association: only the five minutes immediately before a detected trip may be attached to that trip.
- Apply the same five-minute trip-boundary cut while rendering older stored routes. This does not delete or rewrite historical DB rows.

## Files changed
- `BluetoothDiagnosticManager.kt`
- `X451LiveSignalDecoder.kt`
- `VehicleSnapshotDb.kt`
- `VehicleConnectionService.kt`

No UI files, theme/layout assets, Gradle dependencies, Bluetooth transport selection, or X451 packet field offsets were changed.

## Validation and limitations
- Static regression checks are provided in `tools/verify_deep_fix.py` and were run before packaging.
- The uploaded 10 Oct 2026 log was inspected: Peer 19's app advertised client ID 20 despite the HU identifying Peer 19; the retry as Peer 20 received `Sl_Supported_App_Ack` and connected `VehicleInfoService`.
- A full Android build could not be run in this environment: `./gradlew :app:assembleDebug --offline` attempted to download Gradle 8.14.3 and failed with `UnknownHostException: services.gradle.org`; no Android SDK (`android.jar`) was present. Therefore this is a patched **source ZIP**, not a verified APK. Install/vehicle verification of the fix is still required.
