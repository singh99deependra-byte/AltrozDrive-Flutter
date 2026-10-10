import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'screens/dashboard_screens.dart';
import 'screens/detail_screens.dart';
import 'theme/app_theme.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key});
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _tab = 0;
  bool _checkingSetup = true;
  bool _showSetup = false;

  @override
  void initState() {
    super.initState();
    _checkFirstLaunch();
  }

  Future<void> _checkFirstLaunch() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() {
      _showSetup = !(prefs.getBool('first_launch_setup_complete') ?? false);
      _checkingSetup = false;
    });
  }

  Future<void> _finishSetup() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('first_launch_setup_complete', true);
    if (mounted) setState(() => _showSetup = false);
  }

  void _open(String key) {
    late final Widget screen;
    switch (key) {
      case 'setup': screen = FirstLaunchSetupScreen(onComplete: _finishSetup); break;
      case 'profile': screen = const VehicleProfileScreen(); break;
      case 'media': screen = const MediaScreen(); break;
      case 'navigation': screen = const NavigationScreen(); break;
      case 'data': screen = const DataHistoryScreen(); break;
      case 'settings': screen = const SettingsScreen(); break;
      case 'about': screen = const AboutScreen(); break;
      case 'reminders': screen = const RemindersScreen(); break;
      case 'driving_insights': screen = const DrivingInsightsScreen(); break;
      case 'fuel_intelligence': screen = const FuelIntelligenceScreen(); break;
      case 'vehicle_trends': screen = const VehicleTrendsScreen(); break;
      case 'since_last_drive': screen = const SinceLastDriveScreen(); break;
      case 'trip_detail': screen = TripDetailScreen(onOpenPage: _open); break;
      default: return;
    }
    Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => screen));
  }

  @override
  Widget build(BuildContext context) {
    if (_checkingSetup) {
      return const Scaffold(backgroundColor: AppColors.canvas, body: Center(child: CircularProgressIndicator(color: AppColors.gold)));
    }
    if (_showSetup) return FirstLaunchSetupScreen(onComplete: _finishSetup);

    final pages = <Widget>[
      HomeScreen(onOpenPage: _open, onChangeTab: (tab) => setState(() => _tab = tab)),
      DriveScreen(onOpenPage: _open),
      TripsScreen(onOpenPage: _open),
      FuelScreen(onOpenPage: _open),
      MoreScreen(onOpenPage: _open),
    ];
    return Scaffold(
      body: SafeArea(child: IndexedStack(index: _tab, children: pages)),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (index) => setState(() => _tab = index),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.directions_car_outlined), selectedIcon: Icon(Icons.directions_car_filled_rounded), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.speed_outlined), selectedIcon: Icon(Icons.speed_rounded), label: 'Drive'),
          NavigationDestination(icon: Icon(Icons.route_outlined), selectedIcon: Icon(Icons.route_rounded), label: 'Trips'),
          NavigationDestination(icon: Icon(Icons.local_gas_station_outlined), selectedIcon: Icon(Icons.local_gas_station_rounded), label: 'Fuel'),
          NavigationDestination(icon: Icon(Icons.tune_outlined), selectedIcon: Icon(Icons.tune_rounded), label: 'More'),
        ],
      ),
    );
  }
}
