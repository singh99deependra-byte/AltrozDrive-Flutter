import 'package:flutter/material.dart';

abstract final class AppColors {
  static const canvas = Color(0xFFF4F1EA);
  static const surface = Color(0xFFFCFAF6);
  static const surfaceSoft = Color(0xFFF0ECE3);
  static const ink = Color(0xFF222B35);
  static const muted = Color(0xFF77818A);
  static const gold = Color(0xFFB8893A);
  static const goldSoft = Color(0xFFEFE2C7);
  static const green = Color(0xFF31866A);
  static const greenSoft = Color(0xFFE5F1EA);
  static const blue = Color(0xFF4E82B9);
  static const blueSoft = Color(0xFFE6EEF7);
  static const orange = Color(0xFFD48641);
  static const orangeSoft = Color(0xFFF8EBDD);
  static const purple = Color(0xFF8771B4);
  static const purpleSoft = Color(0xFFEEE9F6);
  static const navy = Color(0xFF202C38);
  static const navy2 = Color(0xFF293949);
  static const border = Color(0xFFE4DED2);
  static const red = Color(0xFFC45D54);
}

abstract final class AppTheme {
  static ThemeData get light => ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
        scaffoldBackgroundColor: AppColors.canvas,
        colorScheme: ColorScheme.fromSeed(
          seedColor: AppColors.gold,
          brightness: Brightness.light,
          surface: AppColors.surface,
          primary: AppColors.gold,
          secondary: AppColors.green,
          onSurface: AppColors.ink,
        ),
        fontFamily: 'Roboto',
        appBarTheme: const AppBarTheme(
          backgroundColor: AppColors.canvas,
          foregroundColor: AppColors.ink,
          surfaceTintColor: Colors.transparent,
          elevation: 0,
          centerTitle: false,
        ),
        cardTheme: CardTheme(
          color: AppColors.surface,
          surfaceTintColor: Colors.transparent,
          elevation: 0,
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(22),
            side: const BorderSide(color: AppColors.border),
          ),
        ),
        dividerTheme: const DividerThemeData(color: AppColors.border, thickness: 1, space: 1),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppColors.surfaceSoft,
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 15),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: BorderSide.none),
          enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: const BorderSide(color: AppColors.border)),
          focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: const BorderSide(color: AppColors.gold, width: 1.4)),
        ),
        navigationBarTheme: NavigationBarThemeData(
          height: 72,
          backgroundColor: AppColors.surface,
          indicatorColor: AppColors.goldSoft,
          labelTextStyle: WidgetStateProperty.resolveWith((states) => TextStyle(
            fontSize: 11,
            letterSpacing: .1,
            fontWeight: states.contains(WidgetState.selected) ? FontWeight.w800 : FontWeight.w600,
            color: states.contains(WidgetState.selected) ? AppColors.ink : AppColors.muted,
          )),
        ),
        chipTheme: ChipThemeData(
          backgroundColor: AppColors.surfaceSoft,
          selectedColor: AppColors.goldSoft,
          side: const BorderSide(color: AppColors.border),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        ),
      );

  static ThemeData get dark => ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(seedColor: AppColors.gold, brightness: Brightness.dark),
        scaffoldBackgroundColor: const Color(0xFF141B22),
      );
}
