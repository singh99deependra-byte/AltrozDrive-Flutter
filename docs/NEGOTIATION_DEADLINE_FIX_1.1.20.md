# AltrozDrive 1.1.20 — ConnectNext negotiation deadline hardening

## Scope
- Version: 1.1.20 (versionCode 12305)
- Functional source change limited to `BluetoothDiagnosticManager.kt`.
- UI, trip tracking, route/GPS, vehicle decoder, and database sources are unchanged.

## Fix
The earlier 1.1.19 deadline required `evidenceAppsSent == true`, so a stalled handshake could escape that deadline if the SupportedApps advertisement was never sent. It also lacked a fixed deadline before the first valid NETWORK_ID.

The updated watchdog now:
1. Starts a fixed session-accepted timestamp when the SppLink socket ownership is transferred.
2. If no valid NETWORK_ID is decoded, recovers after 45 seconds from accepted SppLink if the SupportedApps ACK has not been seen.
3. Once the first valid NETWORK_ID is decoded, requires the SupportedApps ACK within 20 seconds of that first NETWORK_ID, regardless of whether SupportedApps has been sent yet.
4. Does not refresh either deadline on repeated NETWORK_ID/heartbeat traffic.
5. Stops applying these negotiation deadlines once the SupportedApps ACK is seen. Existing generic stalled-session recovery remains.

## Validation status
- Source-level assertions and archive integrity checked.
- Android build and APK installation are NOT verified in this environment. Build with the project's supported Android SDK/Gradle environment before installing.
