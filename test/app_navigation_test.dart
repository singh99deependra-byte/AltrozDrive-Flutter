import 'package:altroz_drive/main.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({'first_launch_setup_complete': true});
  });

  testWidgets('main dashboard renders without telemetry placeholders being fabricated', (tester) async {
    await tester.pumpWidget(const AltrozDriveApp());
    await tester.pumpAndSettle();

    expect(find.text('Your Altroz, at a glance'), findsOneWidget);
    expect(find.text('TATA ALTROZ  •  XZA+'), findsOneWidget);
    expect(find.text('NOT CONNECTED'), findsWidgets);
    expect(find.text('ODOMETER'), findsOneWidget);
  });

  testWidgets('More menu opens vehicle reminders page', (tester) async {
    await tester.pumpWidget(const AltrozDriveApp());
    await tester.pumpAndSettle();

    await tester.tap(find.text('More').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Vehicle reminders'));
    await tester.pumpAndSettle();

    expect(find.text('Documents'), findsOneWidget);
    expect(find.text('Vehicle insurance'), findsOneWidget);
    expect(find.text('PUC certificate'), findsOneWidget);
  });

  testWidgets('Trips view keeps Recent and History as separate sections', (tester) async {
    await tester.pumpWidget(const AltrozDriveApp());
    await tester.pumpAndSettle();
    await tester.tap(find.text('Trips').last);
    await tester.pumpAndSettle();

    expect(find.text('Recent (5)'), findsOneWidget);
    expect(find.text('History'), findsWidgets);
    await tester.tap(find.text('History').first);
    await tester.pumpAndSettle();
    expect(find.text('No history loaded yet'), findsOneWidget);
  });
  testWidgets('trip detail route has a dedicated Flutter screen', (tester) async {
    await tester.pumpWidget(const AltrozDriveApp());
    await tester.pumpAndSettle();
    await tester.tap(find.text('Trips').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('History').first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Trip detail screen'));
    await tester.pumpAndSettle();
    expect(find.text('Trip record unavailable'), findsOneWidget);
    expect(find.text('Trip metrics'), findsOneWidget);
  });

}
