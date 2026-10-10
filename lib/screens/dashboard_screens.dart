import 'dart:math' as math;
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/design_widgets.dart';

typedef OpenPage = void Function(String page);
typedef ChangeTab = void Function(int tab);

class HomeScreen extends StatelessWidget {
  final OpenPage onOpenPage;
  final ChangeTab onChangeTab;
  const HomeScreen({super.key, required this.onOpenPage, required this.onChangeTab});

  @override
  Widget build(BuildContext context) {
    final hour = DateTime.now().hour;
    final greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    return ScreenPage(
      title: greeting,
      subtitle: 'Your Altroz, at a glance',
      trailing: const StatusPill('Offline', color: AppColors.muted, icon: Icons.bluetooth_disabled_rounded),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        _VehicleHero(onOpenPage: onOpenPage),
        const SectionHeading('Vehicle essentials'),
        const _MetricGrid(children: [
          MetricCard(icon: Icons.speed_rounded, label: 'Odometer', value: '—', unit: 'km', accent: AppColors.gold),
          MetricCard(icon: Icons.local_gas_station_rounded, label: 'Fuel level', value: '—', unit: '%', accent: AppColors.orange),
          MetricCard(icon: Icons.battery_charging_full_rounded, label: 'Range', value: '—', unit: 'km', accent: AppColors.green),
          MetricCard(icon: Icons.eco_rounded, label: 'Avg. economy', value: '—', unit: 'km/l', accent: AppColors.blue),
        ]),
        SectionHeading('Today’s trip', action: 'VIEW TRIPS', onAction: () => onChangeTab(2)),
        SurfaceCard(child: Row(children: [
          const IconTile(Icons.route_rounded, tint: AppColors.gold, size: 47),
          const SizedBox(width: 12),
          const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('No active trip', style: TextStyle(fontSize: 14, color: AppColors.ink, fontWeight: FontWeight.w800)),
            SizedBox(height: 4),
            Text('Connect your vehicle to begin tracking', style: TextStyle(fontSize: 10.5, color: AppColors.muted, height: 1.4)),
          ])),
          const Icon(Icons.chevron_right_rounded, color: AppColors.muted),
        ])),
        const SectionHeading('Recent trip days'),
        SurfaceCard(child: Column(children: [
          const Row(children: [Icon(Icons.calendar_month_rounded, size: 18, color: AppColors.gold), SizedBox(width: 8), Expanded(child: Text('Trip activity', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 13, color: AppColors.ink))), StatusPill('Last 7 days')]),
          const SizedBox(height: 15),
          SizedBox(height: 74, child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [for (final day in const ['M', 'T', 'W', 'T', 'F', 'S', 'S']) Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: Column(mainAxisAlignment: MainAxisAlignment.end, children: [Container(height: 3, decoration: BoxDecoration(color: AppColors.border, borderRadius: BorderRadius.circular(8))), const SizedBox(height: 8), Text(day, style: const TextStyle(fontSize: 10, color: AppColors.muted, fontWeight: FontWeight.w700))])))])),
          const SizedBox(height: 10),
          const Text('Trip history appears here once data is available.', style: TextStyle(fontSize: 10.5, color: AppColors.muted)),
        ])),
        const SectionHeading('Your drive, your way'),
        Row(children: [
          Expanded(child: _QuickAction(icon: Icons.speed_rounded, title: 'Live drive', subtitle: 'Vehicle gauges', tint: AppColors.blue, onTap: () => onChangeTab(1))),
          const SizedBox(width: 10),
          Expanded(child: _QuickAction(icon: Icons.insights_rounded, title: 'Insights', subtitle: 'Driving summary', tint: AppColors.green, onTap: () => onOpenPage('driving_insights'))),
        ]),
        const SizedBox(height: 10),
        _MusicMiniCard(onTap: () => onOpenPage('media')),
      ]),
    );
  }
}

class _VehicleHero extends StatelessWidget {
  final OpenPage onOpenPage;
  const _VehicleHero({required this.onOpenPage});
  @override
  Widget build(BuildContext context) => SurfaceCard(
        color: AppColors.navy,
        padding: const EdgeInsets.fromLTRB(18, 17, 18, 15),
        border: Border.all(color: AppColors.navy2),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6), decoration: BoxDecoration(color: Colors.white.withOpacity(.09), borderRadius: BorderRadius.circular(30), border: Border.all(color: Colors.white.withOpacity(.08))), child: const Text('TATA ALTROZ  •  XZA+', style: TextStyle(color: Color(0xFFE8D4AD), fontWeight: FontWeight.w800, fontSize: 9, letterSpacing: .85))),
            const Spacer(),
            IconButton(onPressed: () => onOpenPage('profile'), visualDensity: VisualDensity.compact, icon: const Icon(Icons.tune_rounded, color: Colors.white70, size: 20), tooltip: 'Vehicle profile'),
          ]),
          const SizedBox(height: 6),
          const Text('Every drive,\nmore informed.', style: TextStyle(fontSize: 27, height: 1.08, fontWeight: FontWeight.w800, letterSpacing: -.7, color: Colors.white)),
          const SizedBox(height: 5),
          const Text('Your journeys. Your car. Your data.', style: TextStyle(fontSize: 11, color: Color(0xFFBAC4CD))),
          const SizedBox(height: 12),
          SizedBox(height: 98, width: double.infinity, child: Stack(alignment: Alignment.center, children: [Positioned(left: 20, right: 20, bottom: 8, child: Container(height: 10, decoration: BoxDecoration(color: const Color(0xFFD6B875).withOpacity(.13), borderRadius: BorderRadius.circular(30), boxShadow: [BoxShadow(color: const Color(0xFFD6B875).withOpacity(.11), blurRadius: 16, spreadRadius: 5)]))), Image.asset('assets/images/altroz_cutout.png', fit: BoxFit.contain, errorBuilder: (_, __, ___) => CustomPaint(painter: _CarPainter())) ])),
          Divider(height: 24, color: Colors.white.withOpacity(.13)),
          Row(children: [
            const Icon(Icons.bluetooth_disabled_rounded, size: 16, color: Color(0xFF9EB8C5)),
            const SizedBox(width: 7),
            const Expanded(child: Text('ConnectNext status', style: TextStyle(fontSize: 10.5, color: Colors.white70))),
            const StatusPill('Not connected', color: Color(0xFFCAD2D8)),
          ]),
        ]),
      );
}

class _CarPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final outline = Paint()..color = const Color(0xFF9BA7B0)..style = PaintingStyle.stroke..strokeWidth = 2.3..strokeCap = StrokeCap.round..strokeJoin = StrokeJoin.round;
    final body = Path()
      ..moveTo(size.width * .05, size.height * .69)
      ..lineTo(size.width * .12, size.height * .65)
      ..lineTo(size.width * .22, size.height * .43)
      ..quadraticBezierTo(size.width * .27, size.height * .30, size.width * .42, size.height * .30)
      ..lineTo(size.width * .62, size.height * .30)
      ..quadraticBezierTo(size.width * .72, size.height * .30, size.width * .80, size.height * .49)
      ..lineTo(size.width * .92, size.height * .55)
      ..lineTo(size.width * .96, size.height * .65)
      ..lineTo(size.width * .95, size.height * .75)
      ..lineTo(size.width * .06, size.height * .75)
      ..close();
    canvas.drawPath(body, outline);
    final windows = Path()..moveTo(size.width * .29, size.height * .43)..quadraticBezierTo(size.width * .33, size.height * .36, size.width * .43, size.height * .36)..lineTo(size.width * .60, size.height * .36)..quadraticBezierTo(size.width * .67, size.height * .36, size.width * .73, size.height * .49)..lineTo(size.width * .25, size.height * .49)..close();
    canvas.drawPath(windows, Paint()..color = const Color(0xFF9BA7B0).withOpacity(.14));
    canvas.drawPath(windows, Paint()..color = const Color(0xFF9BA7B0)..style = PaintingStyle.stroke..strokeWidth = 1.3);
    final wheel = Paint()..color = const Color(0xFFD6B875)..style = PaintingStyle.stroke..strokeWidth = 2.7;
    canvas.drawCircle(Offset(size.width * .25, size.height * .75), size.height * .115, wheel);
    canvas.drawCircle(Offset(size.width * .76, size.height * .75), size.height * .115, wheel);
    canvas.drawOval(Rect.fromCenter(center: Offset(size.width * .5, size.height * .94), width: size.width * .62, height: 6), Paint()..color = const Color(0xFFD6B875).withOpacity(.22)..maskFilter = const MaskFilter.blur(BlurStyle.normal, 10));
    canvas.drawLine(Offset(size.width * .87, size.height * .60), Offset(size.width * .94, size.height * .60), Paint()..color = const Color(0xFFE6B28B)..strokeWidth = 2.8..strokeCap = StrokeCap.round);
  }
  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _MetricGrid extends StatelessWidget {
  final List<Widget> children;
  const _MetricGrid({required this.children});
  @override
  Widget build(BuildContext context) => LayoutBuilder(builder: (context, constraints) => GridView.count(
    crossAxisCount: 2,
    shrinkWrap: true,
    physics: const NeverScrollableScrollPhysics(),
    crossAxisSpacing: 10,
    mainAxisSpacing: 10,
    childAspectRatio: constraints.maxWidth / 2 / 118,
    children: children,
  ));
}

class _QuickAction extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final Color tint;
  final VoidCallback onTap;
  const _QuickAction({required this.icon, required this.title, required this.subtitle, required this.tint, required this.onTap});
  @override
  Widget build(BuildContext context) => InkWell(onTap: onTap, borderRadius: BorderRadius.circular(20), child: SurfaceCard(child: Row(children: [IconTile(icon, tint: tint, size: 39), const SizedBox(width: 9), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(fontWeight: FontWeight.w800, color: AppColors.ink, fontSize: 12)), const SizedBox(height: 3), Text(subtitle, style: const TextStyle(fontSize: 9.5, color: AppColors.muted))]))])));
}

class _MusicMiniCard extends StatelessWidget {
  final VoidCallback onTap;
  const _MusicMiniCard({required this.onTap});
  @override
  Widget build(BuildContext context) => InkWell(onTap: onTap, borderRadius: BorderRadius.circular(22), child: SurfaceCard(child: Row(children: [Container(width: 49, height: 49, decoration: BoxDecoration(color: AppColors.navy, borderRadius: BorderRadius.circular(15)), child: const Icon(Icons.graphic_eq_rounded, color: Color(0xFFE8D4AD), size: 24)), const SizedBox(width: 12), const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Music player', style: TextStyle(color: AppColors.ink, fontWeight: FontWeight.w800, fontSize: 13)), SizedBox(height: 4), Text('Open media controls', style: TextStyle(color: AppColors.muted, fontSize: 10.5))])), const Icon(Icons.chevron_right_rounded, color: AppColors.muted)])));
}

class DriveScreen extends StatelessWidget {
  final OpenPage onOpenPage;
  const DriveScreen({super.key, required this.onOpenPage});
  @override
  Widget build(BuildContext context) => ScreenPage(
    title: 'Drive', subtitle: 'Live vehicle telemetry', trailing: const StatusPill('Offline', color: AppColors.muted, icon: Icons.bluetooth_disabled_rounded),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      SurfaceCard(color: AppColors.navy, padding: const EdgeInsets.fromLTRB(17, 17, 17, 16), border: Border.all(color: AppColors.navy2), child: Column(children: [
        Row(children: [const Eyebrow('CURRENT SPEED', color: Color(0xFFBFC9D1)), const Spacer(), const Icon(Icons.circle, color: Color(0xFF9BA7B0), size: 7), const SizedBox(width: 5), const Text('OFFLINE', style: TextStyle(fontSize: 9, color: Colors.white60, fontWeight: FontWeight.w800, letterSpacing: .8))]),
        const SizedBox(height: 4),
        SizedBox(height: 190, width: double.infinity, child: CustomPaint(painter: _GaugePainter(value: 0, max: 220, accent: const Color(0xFFD6B875)), child: const Center(child: Padding(padding: EdgeInsets.only(top: 45), child: Column(mainAxisSize: MainAxisSize.min, children: [Text('—', style: TextStyle(fontSize: 53, color: Colors.white, fontWeight: FontWeight.w300, height: 1)), SizedBox(height: 2), Text('km/h', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 12, fontWeight: FontWeight.w600))]))))),
        Divider(color: Colors.white.withOpacity(.13), height: 18),
        Row(children: [Expanded(child: _DarkValue(label: 'ENGINE RPM', value: '—', unit: 'rpm', icon: Icons.rotate_right_rounded)), Container(height: 43, width: 1, color: Colors.white.withOpacity(.13)), Expanded(child: _DarkValue(label: 'ENGINE TEMP', value: '—', unit: '°C', icon: Icons.thermostat_rounded))]),
      ])),
      const SectionHeading('Live signals'),
      const _MetricGrid(children: [
        MetricCard(icon: Icons.speed_rounded, label: 'Speed', value: '—', unit: 'km/h', accent: AppColors.blue),
        MetricCard(icon: Icons.rotate_right_rounded, label: 'Engine RPM', value: '—', unit: 'rpm', accent: AppColors.gold),
        MetricCard(icon: Icons.local_gas_station_rounded, label: 'Fuel level', value: '—', unit: '%', accent: AppColors.orange),
        MetricCard(icon: Icons.thermostat_rounded, label: 'Coolant', value: '—', unit: '°C', accent: AppColors.green),
      ]),
      const SectionHeading('Trip computer'),
      SurfaceCard(child: Column(children: [
        const Row(children: [Expanded(child: _CompactValue(label: 'TRIP A', value: '—', unit: 'km')), Expanded(child: _CompactValue(label: 'TRIP B', value: '—', unit: 'km'))]),
        const Divider(height: 24),
        const Row(children: [Expanded(child: _CompactValue(label: 'AVG. ECONOMY', value: '—', unit: 'km/l')), Expanded(child: _CompactValue(label: 'TRIP TIME', value: '—', unit: ''))]),
        const SizedBox(height: 8),
        const Text('ConnectNext is offline. No live values are being displayed.', style: TextStyle(color: AppColors.muted, fontSize: 10.5)),
      ])),
      const SectionHeading('Shortcuts'),
      Row(children: [Expanded(child: _QuickAction(icon: Icons.map_outlined, title: 'Navigation', subtitle: 'Trip map', tint: AppColors.blue, onTap: () => onOpenPage('navigation'))), const SizedBox(width: 10), Expanded(child: _QuickAction(icon: Icons.music_note_rounded, title: 'Music', subtitle: 'Media controls', tint: AppColors.purple, onTap: () => onOpenPage('media')))]),
    ]),
  );
}

class _GaugePainter extends CustomPainter {
  final double value, max;
  final Color accent;
  _GaugePainter({required this.value, required this.max, required this.accent});
  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height * .82);
    final radius = math.min(size.width * .39, size.height * .73);
    const start = math.pi * .82;
    const sweep = math.pi * 1.36;
    final rect = Rect.fromCircle(center: center, radius: radius);
    canvas.drawArc(rect, start, sweep, false, Paint()..color = Colors.white.withOpacity(.13)..style = PaintingStyle.stroke..strokeWidth = 9..strokeCap = StrokeCap.round);
    if (value > 0) canvas.drawArc(rect, start, sweep * (value / max).clamp(0, 1), false, Paint()..color = accent..style = PaintingStyle.stroke..strokeWidth = 9..strokeCap = StrokeCap.round);
    final tickPaint = Paint()..color = Colors.white.withOpacity(.55)..strokeWidth = 1.4..strokeCap = StrokeCap.round;
    for (var i = 0; i <= 20; i++) {
      final angle = start + sweep * i / 20;
      final inner = radius - (i % 2 == 0 ? 12 : 7);
      canvas.drawLine(Offset(center.dx + math.cos(angle) * inner, center.dy + math.sin(angle) * inner), Offset(center.dx + math.cos(angle) * (radius + 1), center.dy + math.sin(angle) * (radius + 1)), tickPaint);
    }
  }
  @override
  bool shouldRepaint(covariant _GaugePainter oldDelegate) => oldDelegate.value != value || oldDelegate.accent != accent;
}

class _DarkValue extends StatelessWidget {
  final String label, value, unit;
  final IconData icon;
  const _DarkValue({required this.label, required this.value, required this.unit, required this.icon});
  @override
  Widget build(BuildContext context) => Padding(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3), child: Row(children: [Icon(icon, size: 17, color: const Color(0xFFD6B875)), const SizedBox(width: 9), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(label, style: const TextStyle(fontSize: 8.5, color: Color(0xFFBFC9D1), fontWeight: FontWeight.w800, letterSpacing: .6)), const SizedBox(height: 5), Text('$value${unit.isEmpty ? '' : ' $unit'}', style: const TextStyle(fontSize: 17, color: Colors.white, fontWeight: FontWeight.w800))]))]));
}

class _CompactValue extends StatelessWidget {
  final String label, value, unit;
  const _CompactValue({required this.label, required this.value, required this.unit});
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(label, style: const TextStyle(fontSize: 9, color: AppColors.muted, fontWeight: FontWeight.w800, letterSpacing: .5)), const SizedBox(height: 6), Row(crossAxisAlignment: CrossAxisAlignment.end, children: [Text(value, style: const TextStyle(fontSize: 25, height: 1, fontWeight: FontWeight.w800, color: AppColors.ink)), if (unit.isNotEmpty) Padding(padding: const EdgeInsets.only(left: 4, bottom: 2), child: Text(unit, style: const TextStyle(fontSize: 10, color: AppColors.muted)))]),]);
}

class TripsScreen extends StatefulWidget {
  final OpenPage onOpenPage;
  const TripsScreen({super.key, required this.onOpenPage});
  @override
  State<TripsScreen> createState() => _TripsScreenState();
}
class _TripsScreenState extends State<TripsScreen> {
  int _section = 0;
  @override
  Widget build(BuildContext context) => ScreenPage(title: 'Trips', subtitle: 'Your drives, routes and history', trailing: const StatusPill('Offline'), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    FilterSegment<int>(value: _section, values: const [0, 1], label: (v) => v == 0 ? 'Recent (5)' : 'History', onChanged: (v) => setState(() => _section = v)),
    if (_section == 0) ...[
      const SectionHeading('Recent trips'),
      const EmptyDataPanel(icon: Icons.route_rounded, title: 'Your recent trips will appear here', description: 'Once the vehicle data source is connected, the latest five trips will be shown here. Short drives must remain eligible for capture.', tag: 'Awaiting vehicle data'),
      const SectionHeading('Trip summaries'),
      const _MetricGrid(children: [MetricCard(icon: Icons.straighten_rounded, label: 'Distance today', value: '—', unit: 'km'), MetricCard(icon: Icons.timer_outlined, label: 'Drive time', value: '—', unit: ''), MetricCard(icon: Icons.eco_outlined, label: 'Avg. economy', value: '—', unit: 'km/l', accent: AppColors.green), MetricCard(icon: Icons.route_rounded, label: 'Trips today', value: '—', unit: '')]),
    ] else ...[
      const SectionHeading('Trip history'),
      const EmptyDataPanel(icon: Icons.history_rounded, title: 'No history loaded yet', description: 'The full history view will be backed by the migrated database. Existing records must remain intact during migration.', tag: 'Database integration pending'),
      SurfaceCard(child: ActionRow(icon: Icons.receipt_long_rounded, title: 'Trip detail screen', subtitle: 'Open the migrated trip-detail layout (data integration pending)', tint: AppColors.blue, onTap: () => widget.onOpenPage('trip_detail'))),
      const SectionHeading('History controls'),
      SurfaceCard(child: Column(children: [ActionRow(icon: Icons.calendar_month_rounded, title: 'Monthly trip history', subtitle: 'Browse trips by month', tint: AppColors.blue, onTap: () => widget.onOpenPage('data')), const Divider(height: 1), ActionRow(icon: Icons.insights_rounded, title: 'Driving insights', subtitle: 'Distance, duration and economy', tint: AppColors.green, onTap: () => widget.onOpenPage('driving_insights'))])),
    ],
    const SectionHeading('Related'),
    SurfaceCard(child: Column(children: [ActionRow(icon: Icons.map_outlined, title: 'Routes & map', subtitle: 'View trip route details', tint: AppColors.blue, onTap: () => widget.onOpenPage('navigation')), const Divider(height: 1), ActionRow(icon: Icons.timeline_rounded, title: 'Since last drive', subtitle: 'Changes since your previous journey', tint: AppColors.purple, onTap: () => widget.onOpenPage('since_last_drive'))])),
  ]));
}

class FuelScreen extends StatefulWidget {
  final OpenPage onOpenPage;
  const FuelScreen({super.key, required this.onOpenPage});
  @override
  State<FuelScreen> createState() => _FuelScreenState();
}
class _FuelScreenState extends State<FuelScreen> {
  int _days = 7;
  @override
  Widget build(BuildContext context) => ScreenPage(title: 'Fuel', subtitle: 'Consumption, range and efficiency', trailing: const StatusPill('Offline'), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    SurfaceCard(color: AppColors.navy, border: Border.all(color: AppColors.navy2), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Eyebrow('FUEL OVERVIEW', color: Color(0xFFC1CAD1)),
      const SizedBox(height: 12),
      const Row(crossAxisAlignment: CrossAxisAlignment.end, children: [Text('—', style: TextStyle(color: Colors.white, fontSize: 43, height: 1, fontWeight: FontWeight.w800)), SizedBox(width: 6), Padding(padding: EdgeInsets.only(bottom: 4), child: Text('km/l', style: TextStyle(color: Color(0xFFC1CAD1), fontSize: 12, fontWeight: FontWeight.w700))), Spacer(), StatusPill('No data', color: Color(0xFFC1CAD1))]),
      const SizedBox(height: 7), const Text('Average fuel economy', style: TextStyle(color: Color(0xFFC1CAD1), fontSize: 11)),
      const SizedBox(height: 17),
      Row(children: [Expanded(child: _FuelDarkMetric(label: 'FUEL LEVEL', value: '— %', icon: Icons.local_gas_station_rounded)), const SizedBox(width: 8), Expanded(child: _FuelDarkMetric(label: 'EST. RANGE', value: '— km', icon: Icons.route_rounded))]),
    ])),
    const SectionHeading('Efficiency history', action: 'DETAILS', onAction: () => widget.onOpenPage('fuel_intelligence')),
    FilterSegment<int>(value: _days, values: const [7, 30, 90, 180], label: (v) => switch (v) { 7 => '7D', 30 => '30D', 90 => '90D', _ => '6M' }, onChanged: (v) => setState(() => _days = v)),
    const SizedBox(height: 10),
    SurfaceCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(_days == 180 ? 'Last 6 months' : 'Last $_days days', style: const TextStyle(fontSize: 13, color: AppColors.ink, fontWeight: FontWeight.w800)),
      const SizedBox(height: 5), const Text('Fuel history chart', style: TextStyle(fontSize: 10.5, color: AppColors.muted)),
      const SizedBox(height: 18),
      SizedBox(height: 155, child: BarChart(BarChartData(
        maxY: 10, minY: 0, barGroups: [],
        gridData: FlGridData(show: true, drawVerticalLine: false, getDrawingHorizontalLine: (value) => FlLine(color: AppColors.border.withOpacity(.7), strokeWidth: 1)),
        titlesData: FlTitlesData(show: true, leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)), rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)), topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)), bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 22, getTitlesWidget: (value, meta) => Padding(padding: const EdgeInsets.only(top: 7), child: Text(['M', 'T', 'W', 'T', 'F', 'S', 'S'][(value.toInt().clamp(0, 6)).toInt()], style: const TextStyle(fontSize: 9, color: AppColors.muted)))))),
        borderData: FlBorderData(show: false),
      ))),
      const SizedBox(height: 8), const Center(child: Text('No telemetry samples available for this period.', style: TextStyle(fontSize: 10.5, color: AppColors.muted))),
    ])),
    const SectionHeading('Fuel intelligence'),
    SurfaceCard(child: Column(children: [ActionRow(icon: Icons.local_gas_station_rounded, title: 'Fuel intelligence', subtitle: 'Consumption and cost insights', tint: AppColors.orange, onTap: () => widget.onOpenPage('fuel_intelligence')), const Divider(height: 1), ActionRow(icon: Icons.timeline_rounded, title: 'Vehicle trends', subtitle: 'Compare long-term signals', tint: AppColors.purple, onTap: () => widget.onOpenPage('vehicle_trends'))])),
  ]));
}

class _FuelDarkMetric extends StatelessWidget {
  final String label, value;
  final IconData icon;
  const _FuelDarkMetric({required this.label, required this.value, required this.icon});
  @override
  Widget build(BuildContext context) => Container(padding: const EdgeInsets.all(12), decoration: BoxDecoration(color: Colors.white.withOpacity(.07), borderRadius: BorderRadius.circular(15), border: Border.all(color: Colors.white.withOpacity(.08))), child: Row(children: [Icon(icon, color: const Color(0xFFE5C98C), size: 17), const SizedBox(width: 8), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(label, style: const TextStyle(color: Color(0xFFBBC5CC), fontSize: 8.5, fontWeight: FontWeight.w800, letterSpacing: .5)), const SizedBox(height: 4), Text(value, style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w800))]))]));
}

class MoreScreen extends StatelessWidget {
  final OpenPage onOpenPage;
  const MoreScreen({super.key, required this.onOpenPage});
  @override
  Widget build(BuildContext context) => ScreenPage(title: 'More', subtitle: 'Vehicle tools and app preferences', child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    SurfaceCard(color: AppColors.navy, border: Border.all(color: AppColors.navy2), child: Row(children: [
      Container(width: 54, height: 54, decoration: BoxDecoration(color: Colors.white.withOpacity(.1), borderRadius: BorderRadius.circular(17)), child: const Icon(Icons.directions_car_filled_rounded, color: Color(0xFFE8D4AD), size: 29)),
      const SizedBox(width: 13),
      const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Tata Altroz', style: TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.w800)), SizedBox(height: 4), Text('XZA+  •  Vehicle profile', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 11))])),
      IconButton(onPressed: () => onOpenPage('profile'), icon: const Icon(Icons.edit_outlined, color: Colors.white70)),
    ])),
    const SectionHeading('Vehicle'),
    SurfaceCard(child: Column(children: [
      ActionRow(icon: Icons.directions_car_filled_rounded, title: 'Vehicle profile', subtitle: 'Model, trim and vehicle preferences', tint: AppColors.gold, onTap: () => onOpenPage('profile')),
      const Divider(height: 1), ActionRow(icon: Icons.notifications_active_outlined, title: 'Vehicle reminders', subtitle: 'Insurance and PUC expiry', tint: AppColors.orange, onTap: () => onOpenPage('reminders')),
      const Divider(height: 1), ActionRow(icon: Icons.map_outlined, title: 'Navigation', subtitle: 'GPS and route map', tint: AppColors.blue, onTap: () => onOpenPage('navigation')),
      const Divider(height: 1), ActionRow(icon: Icons.storage_outlined, title: 'Data & history', subtitle: 'Telemetry, trips and storage', tint: AppColors.purple, onTap: () => onOpenPage('data')),
    ])),
    const SectionHeading('Insights'),
    SurfaceCard(child: Column(children: [
      ActionRow(icon: Icons.insights_rounded, title: 'Driving insights', subtitle: 'Driving summary and efficiency', tint: AppColors.green, onTap: () => onOpenPage('driving_insights')),
      const Divider(height: 1), ActionRow(icon: Icons.local_gas_station_rounded, title: 'Fuel intelligence', subtitle: 'Consumption and range analysis', tint: AppColors.orange, onTap: () => onOpenPage('fuel_intelligence')),
      const Divider(height: 1), ActionRow(icon: Icons.show_chart_rounded, title: 'Vehicle trends', subtitle: 'Historical signals over time', tint: AppColors.purple, onTap: () => onOpenPage('vehicle_trends')),
      const Divider(height: 1), ActionRow(icon: Icons.history_toggle_off_rounded, title: 'Since last drive', subtitle: 'Changes since your previous trip', tint: AppColors.blue, onTap: () => onOpenPage('since_last_drive')),
    ])),
    const SectionHeading('App'),
    SurfaceCard(child: Column(children: [
      ActionRow(icon: Icons.music_note_rounded, title: 'Music player', subtitle: 'Media session and playback controls', tint: AppColors.purple, onTap: () => onOpenPage('media')),
      const Divider(height: 1), ActionRow(icon: Icons.settings_outlined, title: 'Settings', subtitle: 'Appearance, accents and units', tint: AppColors.gold, onTap: () => onOpenPage('settings')),
      const Divider(height: 1), ActionRow(icon: Icons.info_outline_rounded, title: 'About AltrozDrive', subtitle: 'App version and migration details', tint: AppColors.muted, onTap: () => onOpenPage('about')),
    ])),
    const SizedBox(height: 16), const Center(child: Text('ALTROZDRIVE  •  OPTION C FLUTTER MIGRATION', textAlign: TextAlign.center, style: TextStyle(fontSize: 8.5, letterSpacing: 1, color: AppColors.muted, fontWeight: FontWeight.w800))),
  ]));
}
