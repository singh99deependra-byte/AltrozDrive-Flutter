# Screen coverage matrix

| Original destination in AltrozDrive 1.1.20 | Flutter implementation | UI status | Behavioral status |
|---|---|---|---|
| FirstLaunchSetup | `FirstLaunchSetupScreen` | Implemented | Permission requests wired via `permission_handler`; media settings intent wired. Existing media service not migrated. |
| HomeScreen | `HomeScreen` | Implemented, includes transparent Altroz cutout | Telemetry/trips/media data source pending |
| DriveScreen | `DriveScreen` | Implemented with custom speed gauge and signal cards | Vehicle state/decoder stream pending |
| TripScreen | `TripsScreen` | Implemented with Recent (5) and History selectors | Database-backed records, trip details and calculation parity pending |
| FuelScreen | `FuelScreen` | Implemented with history periods and chart surface | Telemetry-backed bars/formulas pending |
| More | `MoreScreen` | Implemented, routes to all detail pages | Uses Flutter route navigation |
| VehicleProfileDetail | `VehicleProfileScreen` | Implemented | Stores Flutter-local preference values; not yet reconciled with original profile preferences |
| MediaDetail | `MediaScreen` | Visual layout implemented | Native media session integration pending |
| NavigationDetail | `NavigationScreen` | Map/route surface implemented | GPS + map provider + route data pending; placeholder is clearly labeled |
| DataHistoryDetail | `DataHistoryScreen` | Implemented | Original DB, CSV/JSON export, backup/restore and route service pending |
| Settings | `SettingsScreen` | Implemented | Values persist in Flutter preferences; theme/units not yet propagated to data calculations |
| AboutDetail | `AboutScreen` | Implemented | Reports migration status truthfully |
| VehicleRemindersScreen | `RemindersScreen` | Implemented; insurance/PUC date pickers work locally | Android scheduling, notification channels, boot/reboot handling pending |
| DrivingInsightsScreen | `DrivingInsightsScreen` | Implemented | Original calculations/records pending |
| FuelIntelligenceScreen | `FuelIntelligenceScreen` | Implemented | Original calculations/records pending |
| VehicleTrendsScreen | `VehicleTrendsScreen` | Implemented, signal/period selectors work | Telemetry repository pending |
| SinceLastDriveScreen | `SinceLastDriveScreen` | Implemented | Snapshot comparison logic pending |

## Non-screen subsystems that still require porting

The legacy source includes the ConnectNext transport/session stack, peer-19 negotiation/deadline handling, protocol stack and decoder, vehicle connection service, version-10 database, backup scheduler, media listener, reminder scheduler, and map bridge. Those are preserved as read-only source references, not represented as finished Dart migrations.
