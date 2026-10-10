import 'package:flutter/material.dart';
import 'app_shell.dart';
import 'theme/app_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const AltrozDriveApp());
}

class AltrozDriveApp extends StatelessWidget {
  const AltrozDriveApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'AltrozDrive',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        darkTheme: AppTheme.dark,
        themeMode: ThemeMode.light,
        home: const AppShell(),
      );
}
