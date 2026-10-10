import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../theme/app_theme.dart';
import '../widgets/design_widgets.dart';

typedef OpenPage = void Function(String page);


class TripDetailScreen extends StatelessWidget {
  final OpenPage onOpenPage;
  const TripDetailScreen({super.key, required this.onOpenPage});

  @override
  Widget build(BuildContext context) => DetailScaffold(
    title: 'Trip details',
    subtitle: 'Journey summary and vehicle snapshots',
    child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
      const SurfaceCard(color: AppColors.navy, border: Border.fromBorderSide(BorderSide(color: AppColors.navy2)), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Eyebrow('JOURNEY OVERVIEW', color: Color(0xFFBBC5CC)),
        SizedBox(height: 13),
        Text('Trip record unavailable', style: TextStyle(fontSize: 22, height: 1.15, color: Colors.white, fontWeight: FontWeight.w800)),
        SizedBox(height: 8),
        Text('Once the original trip repository is migrated, this screen will show the selected trip without changing its calculations or stored values.', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 11.5, height: 1.45)),
      ])),
      const SectionHeading('Trip metrics'),
      const _MetricGrid(children: [
        MetricCard(icon: Icons.route_rounded, label: 'Distance', value: '—', unit: 'km', accent: AppColors.blue),
        MetricCard(icon: Icons.timer_outlined, label: 'Duration', value: '—', accent: AppColors.purple),
        MetricCard(icon: Icons.eco_rounded, label: 'Avg. economy', value: '—', unit: 'km/l', accent: AppColors.green),
        MetricCard(icon: Icons.local_gas_station_rounded, label: 'Fuel change', value: '—', unit: '%', accent: AppColors.orange),
      ]),
      const SectionHeading('Vehicle snapshots'),
      const SurfaceCard(child: Column(children: [
        _StatusDetailRow(icon: Icons.speed_rounded, title: 'Start / end odometer', value: '— / — km', color: AppColors.blue),
        Divider(height: 1),
        _StatusDetailRow(icon: Icons.swap_horiz_rounded, title: 'Trip A', value: '— → — km', color: AppColors.gold),
        Divider(height: 1),
        _StatusDetailRow(icon: Icons.swap_horiz_rounded, title: 'Trip B', value: '— → — km', color: AppColors.purple),
        Divider(height: 1),
        _StatusDetailRow(icon: Icons.local_gas_station_rounded, title: 'Fuel level', value: '— → — %', color: AppColors.orange),
      ])),
      const SizedBox(height: 12),
      SurfaceCard(child: ActionRow(icon: Icons.map_outlined, title: 'Open route map', subtitle: 'Show recorded GPS points for this trip', tint: AppColors.blue, onTap: () => onOpenPage('navigation'))),
      const SizedBox(height: 10),
      const EmptyDataPanel(icon: Icons.verified_outlined, title: 'Waiting for database integration', description: 'This screen intentionally shows unavailable values. It does not generate demo trips or estimate values. The selected trip ID, route, and metrics must be supplied by the migrated repository.', tag: 'Not ready for live trip testing'),
    ]),
  );
}

class VehicleProfileScreen extends StatefulWidget {
  const VehicleProfileScreen({super.key});
  @override
  State<VehicleProfileScreen> createState() => _VehicleProfileScreenState();
}
class _VehicleProfileScreenState extends State<VehicleProfileScreen> {
  String _model = 'Altroz';
  String _trim = 'XZA+';
  String _fuel = 'Petrol';
  String _transmission = 'DCA';
  final _nickname = TextEditingController(text: 'My Altroz');
  @override
  void initState() { super.initState(); _loadProfile(); }
  Future<void> _loadProfile() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() {
      _model = prefs.getString('vehicle_model') ?? _model;
      _trim = prefs.getString('vehicle_trim') ?? _trim;
      _fuel = prefs.getString('vehicle_fuel') ?? _fuel;
      _transmission = prefs.getString('vehicle_transmission') ?? _transmission;
      _nickname.text = prefs.getString('vehicle_nickname') ?? _nickname.text;
    });
  }
  @override
  void dispose() { _nickname.dispose(); super.dispose(); }
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Vehicle profile', subtitle: 'Your vehicle configuration', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    SurfaceCard(color: AppColors.navy, border: Border.all(color: AppColors.navy2), child: Row(children: [Container(width: 56, height: 56, decoration: BoxDecoration(color: Colors.white.withOpacity(.09), borderRadius: BorderRadius.circular(17)), child: const Icon(Icons.directions_car_filled_rounded, color: Color(0xFFE8D4AD), size: 30)), const SizedBox(width: 13), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('$_model $_trim', style: const TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w800)), const SizedBox(height: 5), Text('$_fuel  •  $_transmission', style: const TextStyle(color: Color(0xFFBAC4CD), fontSize: 11)), const SizedBox(height: 9), const StatusPill('Profile only', color: Color(0xFFC8D1D8))]))])),
    const SectionHeading('Vehicle details'),
    SurfaceCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const _FieldLabel('DISPLAY NAME'), const SizedBox(height: 7), TextField(controller: _nickname, decoration: const InputDecoration(hintText: 'Name this vehicle')),
      const SizedBox(height: 16), const _FieldLabel('MODEL'), const SizedBox(height: 7), DropdownButtonFormField<String>(value: _model, isExpanded: true, items: const [DropdownMenuItem(value: 'Altroz', child: Text('Tata Altroz')), DropdownMenuItem(value: 'Other', child: Text('Other model'))], onChanged: (v) => setState(() => _model = v ?? _model)),
      const SizedBox(height: 16), const _FieldLabel('TRIM'), const SizedBox(height: 7), DropdownButtonFormField<String>(value: _trim, isExpanded: true, items: const ['XE', 'XM', 'XZ', 'XZ+', 'XZA+', 'Other'].map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(), onChanged: (v) => setState(() => _trim = v ?? _trim)),
      const SizedBox(height: 16), const _FieldLabel('FUEL TYPE'), const SizedBox(height: 7), DropdownButtonFormField<String>(value: _fuel, isExpanded: true, items: const ['Petrol', 'Diesel', 'CNG', 'Electric'].map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(), onChanged: (v) => setState(() => _fuel = v ?? _fuel)),
      const SizedBox(height: 16), const _FieldLabel('TRANSMISSION'), const SizedBox(height: 7), DropdownButtonFormField<String>(value: _transmission, isExpanded: true, items: const ['Manual', 'DCA', 'AMT', 'Automatic', 'Other'].map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(), onChanged: (v) => setState(() => _transmission = v ?? _transmission)),
      const SizedBox(height: 18), FilledButton.icon(onPressed: () async { final p = await SharedPreferences.getInstance(); await p.setString('vehicle_nickname', _nickname.text); await p.setString('vehicle_model', _model); await p.setString('vehicle_trim', _trim); await p.setString('vehicle_fuel', _fuel); await p.setString('vehicle_transmission', _transmission); if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Vehicle profile saved on this device.'))); }, icon: const Icon(Icons.check_rounded), label: const Text('Save vehicle profile'), style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(50), backgroundColor: AppColors.navy, foregroundColor: Colors.white)),
    ])),
    const SizedBox(height: 12), const Text('Vehicle configuration is stored locally in this migration build. It does not affect the existing vehicle decoder or protocol.', style: TextStyle(color: AppColors.muted, fontSize: 10.5, height: 1.45)),
  ]));
}

class MediaScreen extends StatelessWidget {
  const MediaScreen({super.key});
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Music player', subtitle: 'Media session and playback', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    Container(height: 390, decoration: BoxDecoration(color: AppColors.navy, borderRadius: BorderRadius.circular(28), boxShadow: [BoxShadow(color: AppColors.ink.withOpacity(.08), blurRadius: 20, offset: const Offset(0, 8))]), clipBehavior: Clip.antiAlias, child: Stack(children: [
      Positioned.fill(child: DecoratedBox(decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [AppColors.navy2, AppColors.navy, const Color(0xFF121A21)])))),
      Positioned(top: 60, left: 50, right: 50, child: Container(height: 210, decoration: BoxDecoration(shape: BoxShape.circle, gradient: RadialGradient(colors: [AppColors.gold.withOpacity(.23), AppColors.gold.withOpacity(.04), Colors.transparent])))),
      Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Row(children: [StatusPill('Not connected', color: Color(0xFFD5DCE1), icon: Icons.music_off_rounded), Spacer(), Eyebrow('MUSIC', color: Color(0xFFBFC9D1))]),
        const Spacer(),
        Center(child: Container(width: 172, height: 172, decoration: BoxDecoration(borderRadius: BorderRadius.circular(30), gradient: const LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [Color(0xFF354758), Color(0xFF202C38)]), border: Border.all(color: Colors.white.withOpacity(.15)), boxShadow: [BoxShadow(color: Colors.black.withOpacity(.25), blurRadius: 30, offset: const Offset(0, 15))]), child: const Center(child: Icon(Icons.graphic_eq_rounded, color: Color(0xFFE8D4AD), size: 72)))),
        const Spacer(),
        const Text('No active media', style: TextStyle(color: Colors.white, fontSize: 21, fontWeight: FontWeight.w800)),
        const SizedBox(height: 5), const Text('Enable media access to display phone playback.', style: TextStyle(color: Color(0xFFBAC4CD), fontSize: 11)),
        const SizedBox(height: 16),
        Row(children: List.generate(28, (i) => Expanded(child: Container(margin: const EdgeInsets.symmetric(horizontal: 1.5), height: 4 + ((i * 11) % 22).toDouble(), decoration: BoxDecoration(color: i < 8 ? AppColors.gold : Colors.white.withOpacity(.22), borderRadius: BorderRadius.circular(3)))))),
        const SizedBox(height: 19),
        Row(mainAxisAlignment: MainAxisAlignment.center, children: [IconButton(onPressed: null, icon: const Icon(Icons.skip_previous_rounded, size: 31)), const SizedBox(width: 10), Container(width: 58, height: 58, decoration: BoxDecoration(color: Colors.white.withOpacity(.1), shape: BoxShape.circle, border: Border.all(color: Colors.white.withOpacity(.15))), child: const Icon(Icons.play_arrow_rounded, color: Colors.white, size: 34)), const SizedBox(width: 10), IconButton(onPressed: null, icon: const Icon(Icons.skip_next_rounded, size: 31))]),
      ])),
    ])),
    const SizedBox(height: 16),
    const SurfaceCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Media access', style: TextStyle(color: AppColors.ink, fontWeight: FontWeight.w800, fontSize: 14)), SizedBox(height: 6), Text('This Flutter screen preserves the media-player layout. Playback, seek, metadata and Android notification-listener integration still need to be connected to the original media session behavior.', style: TextStyle(color: AppColors.muted, fontSize: 11, height: 1.5)), SizedBox(height: 12), StatusPill('Integration pending', color: AppColors.orange)])),
  ]));
}

class NavigationScreen extends StatelessWidget {
  const NavigationScreen({super.key});
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Navigation', subtitle: 'GPS position and trip routes', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    SurfaceCard(color: AppColors.navy, padding: EdgeInsets.zero, border: Border.all(color: AppColors.navy2), child: ClipRRect(borderRadius: BorderRadius.circular(22), child: SizedBox(height: 340, child: Stack(children: [
      Positioned.fill(child: CustomPaint(painter: _MapPlaceholderPainter())),
      Positioned(left: 16, top: 16, child: Container(padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8), decoration: BoxDecoration(color: AppColors.surface, borderRadius: BorderRadius.circular(12)), child: const Row(children: [Icon(Icons.location_searching_rounded, color: AppColors.blue, size: 15), SizedBox(width: 6), Text('GPS unavailable', style: TextStyle(color: AppColors.ink, fontSize: 10, fontWeight: FontWeight.w800))]))),
      const Positioned(right: 16, top: 16, child: StatusPill('Map offline')),
      const Positioned(left: 18, right: 18, bottom: 18, child: SurfaceCard(child: Row(children: [Icon(Icons.route_rounded, color: AppColors.gold), SizedBox(width: 9), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('No active route', style: TextStyle(color: AppColors.ink, fontSize: 13, fontWeight: FontWeight.w800)), SizedBox(height: 3), Text('Connect GPS and begin a trip to record route points.', style: TextStyle(color: AppColors.muted, fontSize: 10))]))]))),
    ])))),
    const SizedBox(height: 14),
    const _StatusDetailRow(icon: Icons.my_location_rounded, title: 'Location permission', value: 'Not verified in this build', color: AppColors.orange),
    const SizedBox(height: 8),
    const _StatusDetailRow(icon: Icons.route_rounded, title: 'Route recording', value: 'Integration pending', color: AppColors.muted),
    const SizedBox(height: 16),
    const EmptyDataPanel(icon: Icons.map_outlined, title: 'Routes will appear here', description: 'The native route recording and MapLibre/OpenRouteMap behavior must be ported and checked against the original before route data can be shown.', tag: 'No simulated route data'),
  ]));
}

class _MapPlaceholderPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = const Color(0xFF263745));
    final minor = Paint()..color = const Color(0xFF354B5B)..strokeWidth = 1.2;
    final major = Paint()..color = const Color(0xFF435868)..strokeWidth = 2.5;
    for (var i = -4; i < 12; i++) {
      final y = i * 44.0;
      canvas.drawLine(Offset(0, y), Offset(size.width, y + 35), minor);
      final x = i * 47.0;
      canvas.drawLine(Offset(x, 0), Offset(x + 90, size.height), minor);
    }
    final road = Path()..moveTo(size.width * .08, size.height * .84)..cubicTo(size.width * .33, size.height * .68, size.width * .27, size.height * .38, size.width * .59, size.height * .40)..cubicTo(size.width * .76, size.height * .41, size.width * .78, size.height * .23, size.width * 1.1, size.height * .18);
    canvas.drawPath(road, major);
    canvas.drawPath(road, Paint()..color = const Color(0xFFD7BD82)..strokeWidth = 1.1..style = PaintingStyle.stroke..strokeCap = StrokeCap.round);
  }
  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class DataHistoryScreen extends StatefulWidget {
  const DataHistoryScreen({super.key});
  @override
  State<DataHistoryScreen> createState() => _DataHistoryScreenState();
}
class _DataHistoryScreenState extends State<DataHistoryScreen> {
  bool _routeRecording = true;
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Data & history', subtitle: 'Vehicle data, telemetry and storage', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    const SurfaceCard(color: AppColors.navy, border: Border.fromBorderSide(BorderSide(color: AppColors.navy2)), child: Row(children: [Icon(Icons.storage_rounded, color: Color(0xFFE8D4AD), size: 28), SizedBox(width: 13), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Vehicle database', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 15)), SizedBox(height: 5), Text('Original schema: version 10', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 10.5)), SizedBox(height: 8), StatusPill('Migration not connected', color: Color(0xFFD4DDE3))]))])),
    const SectionHeading('Storage status'),
    const _MetricGrid(children: [MetricCard(icon: Icons.route_rounded, label: 'Saved trips', value: '—'), MetricCard(icon: Icons.sensors_rounded, label: 'Telemetry rows', value: '—'), MetricCard(icon: Icons.backup_rounded, label: 'Last backup', value: '—'), MetricCard(icon: Icons.calendar_month_rounded, label: 'Retention', value: '6', unit: 'months')]),
    const SectionHeading('Recording preferences'),
    SurfaceCard(child: Column(children: [SwitchListTile.adaptive(contentPadding: EdgeInsets.zero, value: _routeRecording, activeColor: AppColors.gold, onChanged: (v) => setState(() => _routeRecording = v), title: const Text('Route recording', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: AppColors.ink)), subtitle: const Text('Preference UI only; native route service is not connected yet.', style: TextStyle(fontSize: 10.5, color: AppColors.muted))), const Divider(height: 1), const ListTile(contentPadding: EdgeInsets.zero, leading: Icon(Icons.backup_outlined, color: AppColors.blue), title: Text('Nightly backup', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: AppColors.ink)), subtitle: Text('Must retain the existing Android backup schedule.', style: TextStyle(fontSize: 10.5, color: AppColors.muted)), trailing: StatusPill('Pending'))])),
    const SectionHeading('Trip and telemetry history'),
    const EmptyDataPanel(icon: Icons.history_rounded, title: 'Existing records are not imported yet', description: 'The original version-10 database must be migrated with schema and data parity checks. This build does not create fake history or replace your existing records.', tag: 'No data has been migrated'),
    const SectionHeading('Export and backup'),
    SurfaceCard(child: Column(children: [
      ActionRow(icon: Icons.file_download_outlined, title: 'Export trip history (CSV)', subtitle: 'Exports the selected trip list when data access is connected', tint: AppColors.blue, onTap: () => ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('CSV export will be enabled after the existing trip repository is migrated.')))),
      const Divider(height: 1),
      ActionRow(icon: Icons.data_object_rounded, title: 'Export vehicle data (JSON)', subtitle: 'Snapshot and telemetry export', tint: AppColors.purple, onTap: () => ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('JSON export will be enabled after database parity is complete.')))),
      const Divider(height: 1),
      ActionRow(icon: Icons.backup_outlined, title: 'Backup status', subtitle: 'Nightly backup and restore state', tint: AppColors.green, onTap: () => ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Backup scheduling is not active in this Flutter test build. Keep the production app installed.')))),
    ])),
    const SizedBox(height: 12),
  ]));
}

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}
class _SettingsScreenState extends State<SettingsScreen> {
  String _appearance = 'sandstone';
  String _accent = 'gold';
  String _distance = 'km';
  String _economy = 'km/l';
  String _temperature = '°C';
  bool _loaded = false;
  final _appearances = const {'auto': 'Auto • System', 'amoled': 'AMOLED', 'daylight': 'Daylight', 'pearl': 'Pearl', 'sandstone': 'Sandstone', 'graphite': 'Graphite', 'obsidian': 'Obsidian', 'titanium': 'Titanium', 'carbon': 'Carbon'};
  final _accents = const {'gold': AppColors.gold, 'cyan': Color(0xFF37A6B8), 'emerald': AppColors.green, 'amber': AppColors.orange, 'turquoise': Color(0xFF38A9A2), 'jade': Color(0xFF4D927D), 'olive_gold': Color(0xFF93803B)};
  @override
  void initState() { super.initState(); _load(); }
  Future<void> _load() async { final p = await SharedPreferences.getInstance(); if (!mounted) return; setState(() { _appearance = p.getString('appearance') ?? _appearance; _accent = p.getString('accent') ?? _accent; _distance = p.getString('distance_unit') ?? _distance; _economy = p.getString('economy_unit') ?? _economy; _temperature = p.getString('temperature_unit') ?? _temperature; _loaded = true; }); }
  Future<void> _save(String key, String value) async { final p = await SharedPreferences.getInstance(); await p.setString(key, value); }
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Settings', subtitle: 'Customize your AltrozDrive experience', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    if (!_loaded) const LinearProgressIndicator(minHeight: 2, color: AppColors.gold),
    const SectionHeading('Appearance'),
    SurfaceCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('Theme', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: AppColors.ink)), const SizedBox(height: 5), const Text('Select an appearance preference.', style: TextStyle(fontSize: 10.5, color: AppColors.muted)), const SizedBox(height: 13),
      ..._appearances.entries.toList().chunked(2).map((row) => Padding(padding: const EdgeInsets.only(bottom: 8), child: Row(children: [for (final option in row) Expanded(child: Padding(padding: const EdgeInsets.only(right: 7), child: _ThemeOption(label: option.value, selected: _appearance == option.key, keyName: option.key, onTap: () { setState(() => _appearance = option.key); _save('appearance', option.key); }))), if (row.length == 1) const Expanded(child: SizedBox())]))),
    ])),
    const SectionHeading('Accent color'),
    SurfaceCard(child: Wrap(spacing: 15, runSpacing: 13, children: _accents.entries.map((entry) => GestureDetector(onTap: () { setState(() => _accent = entry.key); _save('accent', entry.key); }, child: SizedBox(width: 62, child: Column(children: [Container(width: _accent == entry.key ? 31 : 26, height: _accent == entry.key ? 31 : 26, decoration: BoxDecoration(color: entry.value, shape: BoxShape.circle, border: _accent == entry.key ? Border.all(color: AppColors.ink, width: 2.1) : null)), const SizedBox(height: 5), Text(_accentLabel(entry.key), textAlign: TextAlign.center, style: TextStyle(fontSize: 9.5, fontWeight: _accent == entry.key ? FontWeight.w800 : FontWeight.w500, color: _accent == entry.key ? AppColors.ink : AppColors.muted))])))).toList())),
    const SectionHeading('Units'),
    SurfaceCard(child: Column(children: [
      _SettingDropdown(icon: Icons.speed_rounded, label: 'Distance & speed', value: _distance, items: const {'km': 'Kilometres (km)', 'mi': 'Miles (mi)'}, onChanged: (v) { setState(() => _distance = v); _save('distance_unit', v); }),
      const Divider(height: 1), _SettingDropdown(icon: Icons.eco_rounded, label: 'Fuel economy', value: _economy, items: const {'km/l': 'km/L', 'l/100 km': 'L/100 km', 'mpg': 'MPG'}, onChanged: (v) { setState(() => _economy = v); _save('economy_unit', v); }),
      const Divider(height: 1), _SettingDropdown(icon: Icons.thermostat_rounded, label: 'Temperature', value: _temperature, items: const {'°C': 'Celsius (°C)', '°F': 'Fahrenheit (°F)'}, onChanged: (v) { setState(() => _temperature = v); _save('temperature_unit', v); }),
    ])),
    const SectionHeading('Permissions and system'),
    const SurfaceCard(child: Column(children: [ActionRow(icon: Icons.bluetooth_rounded, title: 'Bluetooth / ConnectNext', subtitle: 'Permission flow integration pending', tint: AppColors.blue), Divider(height: 1), ActionRow(icon: Icons.location_on_outlined, title: 'Location', subtitle: 'GPS and route permission integration pending', tint: AppColors.green), Divider(height: 1), ActionRow(icon: Icons.notifications_active_outlined, title: 'Notifications', subtitle: 'Reminder scheduling integration pending', tint: AppColors.orange)])),
    const SizedBox(height: 12), const Text('Appearance and unit preferences are stored locally in Flutter. They have not been wired into the existing native telemetry/data engine.', style: TextStyle(color: AppColors.muted, fontSize: 10.5, height: 1.45)),
  ]));
  String _accentLabel(String key) => switch (key) {'gold' => 'Gold', 'cyan' => 'Cyan', 'emerald' => 'Emerald', 'amber' => 'Amber', 'turquoise' => 'Turquoise', 'jade' => 'Jade', _ => 'Olive gold'};
}

class _ThemeOption extends StatelessWidget {
  final String label, keyName;
  final bool selected;
  final VoidCallback onTap;
  const _ThemeOption({required this.label, required this.keyName, required this.selected, required this.onTap});
  @override
  Widget build(BuildContext context) => InkWell(onTap: onTap, borderRadius: BorderRadius.circular(15), child: AnimatedContainer(duration: const Duration(milliseconds: 150), padding: const EdgeInsets.all(10), decoration: BoxDecoration(color: selected ? AppColors.goldSoft.withOpacity(.55) : AppColors.surfaceSoft, border: Border.all(color: selected ? AppColors.gold : AppColors.border, width: selected ? 1.4 : 1), borderRadius: BorderRadius.circular(15)), child: Row(children: [Container(width: 26, height: 26, decoration: BoxDecoration(color: keyName == 'amoled' || keyName == 'obsidian' || keyName == 'carbon' || keyName == 'graphite' ? AppColors.navy : const Color(0xFFE9E0D0), borderRadius: BorderRadius.circular(8)), child: Center(child: Container(width: 10, height: 10, decoration: BoxDecoration(color: selected ? AppColors.gold : AppColors.green, shape: BoxShape.circle)))), const SizedBox(width: 7), Expanded(child: Text(label, style: const TextStyle(fontSize: 10.5, color: AppColors.ink, fontWeight: FontWeight.w700))), if (selected) const Icon(Icons.check_circle_rounded, size: 15, color: AppColors.gold)])));
}

class _SettingDropdown extends StatelessWidget {
  final IconData icon;
  final String label, value;
  final Map<String, String> items;
  final ValueChanged<String> onChanged;
  const _SettingDropdown({required this.icon, required this.label, required this.value, required this.items, required this.onChanged});
  @override
  Widget build(BuildContext context) => Padding(padding: const EdgeInsets.symmetric(vertical: 9), child: Row(children: [IconTile(icon, tint: AppColors.gold, size: 40), const SizedBox(width: 10), Expanded(child: Text(label, style: const TextStyle(fontSize: 12.5, color: AppColors.ink, fontWeight: FontWeight.w700))), const SizedBox(width: 8), SizedBox(width: 145, child: DropdownButtonFormField<String>(value: value, isExpanded: true, iconSize: 18, decoration: const InputDecoration(contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 5)), items: items.entries.map((e) => DropdownMenuItem(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 10.5)))).toList(), onChanged: (v) { if (v != null) onChanged(v); }))]));
}

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'About AltrozDrive', subtitle: 'App information', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    SurfaceCard(color: AppColors.navy, padding: const EdgeInsets.symmetric(vertical: 27, horizontal: 20), border: Border.all(color: AppColors.navy2), child: Column(children: [Container(width: 78, height: 78, decoration: BoxDecoration(color: Colors.white.withOpacity(.09), borderRadius: BorderRadius.circular(24)), child: const Icon(Icons.directions_car_filled_rounded, size: 42, color: Color(0xFFE8D4AD))), const SizedBox(height: 16), const Text('AltrozDrive', style: TextStyle(fontSize: 24, color: Colors.white, fontWeight: FontWeight.w800)), const SizedBox(height: 7), const Text('Flutter migration build', style: TextStyle(fontSize: 12, color: Color(0xFFBBC5CC))), const SizedBox(height: 15), const StatusPill('Version 1.1.21 • test build', color: Color(0xFFD4DDE3))])),
    const SectionHeading('Migration status'),
    const SurfaceCard(child: Column(children: [
      _StatusDetailRow(icon: Icons.palette_outlined, title: 'Flutter screen layer', value: 'Implemented', color: AppColors.green), Divider(height: 1),
      _StatusDetailRow(icon: Icons.bluetooth_rounded, title: 'Vehicle / ConnectNext logic', value: 'Not migrated', color: AppColors.orange), Divider(height: 1),
      _StatusDetailRow(icon: Icons.storage_outlined, title: 'Database parity', value: 'Not migrated', color: AppColors.orange), Divider(height: 1),
      _StatusDetailRow(icon: Icons.map_outlined, title: 'GPS and route parity', value: 'Not migrated', color: AppColors.orange), Divider(height: 1),
      _StatusDetailRow(icon: Icons.verified_outlined, title: 'Device-tested release build', value: 'Not yet verified', color: AppColors.muted),
    ])),
    const SizedBox(height: 14), const Text('This build is an in-progress migration and must not replace the production 1.1.20 app until vehicle, data, route, background and permission parity tests pass.', style: TextStyle(color: AppColors.muted, fontSize: 11, height: 1.5)),
  ]));
}

class RemindersScreen extends StatefulWidget {
  const RemindersScreen({super.key});
  @override
  State<RemindersScreen> createState() => _RemindersScreenState();
}
class _RemindersScreenState extends State<RemindersScreen> {
  DateTime? _insurance;
  DateTime? _puc;
  Future<void> _chooseDate(bool insurance) async {
    final today = DateTime.now();
    final picked = await showDatePicker(context: context, initialDate: (insurance ? _insurance : _puc) ?? today, firstDate: DateTime(today.year - 1), lastDate: DateTime(today.year + 12));
    if (picked == null || !mounted) return;
    setState(() { if (insurance) { _insurance = picked; } else { _puc = picked; } });
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(insurance ? 'insurance_expiry' : 'puc_expiry', picked.toIso8601String());
  }
  @override
  void initState() { super.initState(); _loadDates(); }
  Future<void> _loadDates() async { final p = await SharedPreferences.getInstance(); if (!mounted) return; setState(() { _insurance = DateTime.tryParse(p.getString('insurance_expiry') ?? ''); _puc = DateTime.tryParse(p.getString('puc_expiry') ?? ''); }); }
  String _date(DateTime? v) => v == null ? 'Not set' : '${v.day.toString().padLeft(2, '0')} ${_month(v.month)} ${v.year}';
  String _month(int m) => const ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][m-1];
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Vehicle reminders', subtitle: 'Insurance and PUC expiry dates', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    const SurfaceCard(color: AppColors.navy, border: Border.fromBorderSide(BorderSide(color: AppColors.navy2)), child: Row(children: [Icon(Icons.notifications_active_outlined, color: Color(0xFFE8D4AD), size: 29), SizedBox(width: 13), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Stay road-ready', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w800)), SizedBox(height: 5), Text('Keep essential document expiry dates handy.', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 10.5))]))])),
    const SectionHeading('Documents'),
    SurfaceCard(child: Column(children: [
      ActionRow(icon: Icons.verified_user_outlined, title: 'Vehicle insurance', subtitle: 'Expiry: ${_date(_insurance)}', tint: AppColors.blue, onTap: () => _chooseDate(true), trailing: const Icon(Icons.calendar_month_rounded, color: AppColors.gold)),
      const Divider(height: 1),
      ActionRow(icon: Icons.assignment_turned_in_outlined, title: 'PUC certificate', subtitle: 'Expiry: ${_date(_puc)}', tint: AppColors.green, onTap: () => _chooseDate(false), trailing: const Icon(Icons.calendar_month_rounded, color: AppColors.gold)),
    ])),
    const SizedBox(height: 14),
    const EmptyDataPanel(icon: Icons.notifications_none_rounded, title: 'Notification scheduling not connected', description: 'The selected expiry dates are saved in this Flutter build. Actual scheduled notifications, boot recovery and app-start permission behavior still need migration from the original Android scheduler.', tag: 'Do not rely on this build for reminders'),
    const SectionHeading('How to use'),
    const SurfaceCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('1. Tap a document row to choose the expiry date.', style: TextStyle(color: AppColors.ink, fontSize: 11.5)), SizedBox(height: 8), Text('2. Confirm dates with your current policy or certificate.', style: TextStyle(color: AppColors.ink, fontSize: 11.5)), SizedBox(height: 8), Text('3. Keep the existing app’s reminders enabled until notification parity is tested.', style: TextStyle(color: AppColors.muted, fontSize: 11.5, height: 1.4))])),
  ]));
}

class DrivingInsightsScreen extends StatelessWidget {
  const DrivingInsightsScreen({super.key});
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Driving insights', subtitle: 'Driving patterns and efficiency', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    const SurfaceCard(color: AppColors.navy, border: Border.fromBorderSide(BorderSide(color: AppColors.navy2)), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Eyebrow('DRIVING SUMMARY', color: Color(0xFFBBC5CC)), SizedBox(height: 12), Text('Your driving,\nat a glance.', style: TextStyle(color: Colors.white, fontSize: 25, height: 1.1, fontWeight: FontWeight.w800)), SizedBox(height: 8), Text('Insights will be calculated from verified trip records.', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 11))])),
    const SectionHeading('Key indicators'),
    const _MetricGrid(children: [MetricCard(icon: Icons.route_rounded, label: 'Total distance', value: '—', unit: 'km', accent: AppColors.blue), MetricCard(icon: Icons.timer_outlined, label: 'Driving time', value: '—', accent: AppColors.purple), MetricCard(icon: Icons.eco_rounded, label: 'Avg. economy', value: '—', unit: 'km/l', accent: AppColors.green), MetricCard(icon: Icons.speed_rounded, label: 'Avg. speed', value: '—', unit: 'km/h', accent: AppColors.gold)]),
    const SectionHeading('Driving quality'),
    const EmptyDataPanel(icon: Icons.insights_rounded, title: 'Insights need trip history', description: 'No driving score, economy score or trend is fabricated. Values will become available when original trip and telemetry calculations have been ported.', tag: 'Awaiting verified records'),
  ]));
}

class FuelIntelligenceScreen extends StatelessWidget {
  const FuelIntelligenceScreen({super.key});
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Fuel intelligence', subtitle: 'Fuel economy and consumption analysis', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    const SurfaceCard(color: AppColors.navy, border: Border.fromBorderSide(BorderSide(color: AppColors.navy2)), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Eyebrow('FUEL INTELLIGENCE', color: Color(0xFFBBC5CC)), SizedBox(height: 13), Row(crossAxisAlignment: CrossAxisAlignment.end, children: [Text('—', style: TextStyle(fontSize: 45, height: 1, color: Colors.white, fontWeight: FontWeight.w800)), SizedBox(width: 6), Padding(padding: EdgeInsets.only(bottom: 5), child: Text('km/l', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 12))), Spacer(), Icon(Icons.local_gas_station_rounded, color: Color(0xFFE8D4AD), size: 32)]), SizedBox(height: 8), Text('Calculated average economy', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 11))])),
    const SectionHeading('Fuel metrics'),
    const _MetricGrid(children: [MetricCard(icon: Icons.local_gas_station_rounded, label: 'Estimated fuel used', value: '—', unit: 'L', accent: AppColors.orange), MetricCard(icon: Icons.currency_rupee_rounded, label: 'Estimated cost', value: '—', unit: '₹', accent: AppColors.gold), MetricCard(icon: Icons.eco_rounded, label: 'Economy best', value: '—', unit: 'km/l', accent: AppColors.green), MetricCard(icon: Icons.route_rounded, label: 'Distance', value: '—', unit: 'km', accent: AppColors.blue)]),
    const SizedBox(height: 12),
    const EmptyDataPanel(icon: Icons.query_stats_rounded, title: 'Fuel calculations not available yet', description: 'Fuel formulas and telemetry aggregation must be carried over exactly from the original app. The Flutter UI will not substitute estimated or demo results.', tag: 'Calculation parity pending'),
  ]));
}

class VehicleTrendsScreen extends StatefulWidget {
  const VehicleTrendsScreen({super.key});
  @override
  State<VehicleTrendsScreen> createState() => _VehicleTrendsScreenState();
}
class _VehicleTrendsScreenState extends State<VehicleTrendsScreen> {
  String _window = '6 months';
  String _signal = 'Fuel level';
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Vehicle trends', subtitle: 'Long-term telemetry patterns', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    const SectionHeading('Period'),
    FilterSegment<String>(value: _window, values: const ['7 days', '30 days', '3 months', '6 months'], label: (v) => switch (v) {'7 days' => '7D', '30 days' => '30D', '3 months' => '3M', _ => '6M'}, onChanged: (v) => setState(() => _window = v)),
    const SectionHeading('Signal'),
    Wrap(spacing: 8, runSpacing: 8, children: ['Fuel level', 'Coolant', 'Speed', 'Economy'].map((v) => ChoiceChip(label: Text(v), selected: _signal == v, onSelected: (_) => setState(() => _signal = v), selectedColor: AppColors.goldSoft, labelStyle: TextStyle(color: _signal == v ? AppColors.ink : AppColors.muted, fontWeight: FontWeight.w700, fontSize: 11))).toList()),
    const SectionHeading('Trend chart'),
    const EmptyDataPanel(icon: Icons.show_chart_rounded, title: 'No trend points to plot', description: 'Time-series data must come from the migrated telemetry repository. No artificial values are used to fill gaps.', tag: 'Telemetry repository pending'),
    const SectionHeading('Current selection'),
    SurfaceCard(child: Row(children: [IconTile(Icons.query_stats_rounded, tint: AppColors.purple), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(_signal, style: const TextStyle(fontSize: 13, color: AppColors.ink, fontWeight: FontWeight.w800)), const SizedBox(height: 4), Text('Window: $_window', style: const TextStyle(fontSize: 10.5, color: AppColors.muted))])), const StatusPill('No data')])),
  ]));
}

class SinceLastDriveScreen extends StatelessWidget {
  const SinceLastDriveScreen({super.key});
  @override
  Widget build(BuildContext context) => DetailScaffold(title: 'Since last drive', subtitle: 'Changes captured since your previous trip', child: ListView(padding: const EdgeInsets.fromLTRB(18, 5, 18, 28), children: [
    const SurfaceCard(color: AppColors.navy, border: Border.fromBorderSide(BorderSide(color: AppColors.navy2)), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Eyebrow('SINCE LAST DRIVE', color: Color(0xFFBBC5CC)), SizedBox(height: 10), Text('A fresh start,\nevery journey.', style: TextStyle(fontSize: 24, height: 1.1, color: Colors.white, fontWeight: FontWeight.w800)), SizedBox(height: 8), Text('Compare verified snapshots from your last journey.', style: TextStyle(color: Color(0xFFBBC5CC), fontSize: 11))])),
    const SectionHeading('Snapshot comparison'),
    const _MetricGrid(children: [MetricCard(icon: Icons.speed_rounded, label: 'Odometer delta', value: '—', unit: 'km', accent: AppColors.blue), MetricCard(icon: Icons.local_gas_station_rounded, label: 'Fuel change', value: '—', unit: '%', accent: AppColors.orange), MetricCard(icon: Icons.battery_charging_full_rounded, label: 'Battery', value: '—', unit: 'V', accent: AppColors.green), MetricCard(icon: Icons.thermostat_rounded, label: 'Coolant', value: '—', unit: '°C', accent: AppColors.purple)]),
    const SizedBox(height: 12),
    const EmptyDataPanel(icon: Icons.compare_arrows_rounded, title: 'No previous snapshot available', description: 'This screen requires the original snapshot and telemetry comparison rules. It will not infer changes from missing or disconnected values.', tag: 'Awaiting snapshots'),
  ]));
}

class _MetricGrid extends StatelessWidget {
  final List<Widget> children;
  const _MetricGrid({required this.children});
  @override
  Widget build(BuildContext context) => LayoutBuilder(builder: (context, constraints) => GridView.count(crossAxisCount: 2, shrinkWrap: true, physics: const NeverScrollableScrollPhysics(), crossAxisSpacing: 10, mainAxisSpacing: 10, childAspectRatio: constraints.maxWidth / 2 / 118, children: children));
}

class _FieldLabel extends StatelessWidget {
  final String text;
  const _FieldLabel(this.text);
  @override
  Widget build(BuildContext context) => Text(text, style: const TextStyle(color: AppColors.muted, letterSpacing: .8, fontWeight: FontWeight.w800, fontSize: 9.5));
}

class _StatusDetailRow extends StatelessWidget {
  final IconData icon;
  final String title, value;
  final Color color;
  const _StatusDetailRow({required this.icon, required this.title, required this.value, required this.color});
  @override
  Widget build(BuildContext context) => Padding(padding: const EdgeInsets.symmetric(vertical: 12), child: Row(children: [Icon(icon, size: 20, color: color), const SizedBox(width: 11), Expanded(child: Text(title, style: const TextStyle(color: AppColors.ink, fontWeight: FontWeight.w700, fontSize: 11.5))), Text(value, style: TextStyle(color: color, fontWeight: FontWeight.w800, fontSize: 9.5))]));
}

extension _Chunked<E> on List<E> {
  List<List<E>> chunked(int size) { final result = <List<E>>[]; for (var i = 0; i < length; i += size) { result.add(sublist(i, i + size > length ? length : i + size)); } return result; }
}

class FirstLaunchSetupScreen extends StatefulWidget {
  final VoidCallback onComplete;
  const FirstLaunchSetupScreen({super.key, required this.onComplete});
  @override
  State<FirstLaunchSetupScreen> createState() => _FirstLaunchSetupScreenState();
}
class _FirstLaunchSetupScreenState extends State<FirstLaunchSetupScreen> with WidgetsBindingObserver {
  int _step = 0;
  bool _busy = false;
  String? _message;
  static const _channel = MethodChannel('com.altrozdrive.app/system_settings');

  Future<void> _continue() async {
    setState(() { _busy = true; _message = null; });
    try {
      if (_step == 0) {
        final scan = await Permission.bluetoothScan.status;
        final connect = await Permission.bluetoothConnect.status;
        if (scan.isGranted && connect.isGranted) {
          setState(() => _step = 1);
        } else {
          await [Permission.bluetoothScan, Permission.bluetoothConnect].request();
          // The original app moves to the media-access step after the Bluetooth
          // permission result, even if access was denied.
          if (mounted) setState(() => _step = 1);
        }
      } else if (_step == 1) {
        if (await _mediaAccessGranted()) {
          if (mounted) setState(() => _step = 2);
        } else {
          await _channel.invokeMethod<void>('openNotificationListenerSettings');
          if (mounted) setState(() => _message = 'Enable AltrozDrive in Notification access, then return here.');
          // Intentionally remain on this step until Android confirms the special
          // access. This matches the original media-access gate.
        }
      } else if (_step == 2) {
        final current = await Permission.locationWhenInUse.status;
        final result = current.isGranted ? current : await Permission.locationWhenInUse.request();
        if (result.isGranted) {
          if (mounted) setState(() => _step = 3);
        } else if (mounted) {
          setState(() => _message = 'Location access is required for GPS route features. Allow it and try again.');
        }
      } else if (_step == 3) {
        final current = await Permission.notification.status;
        final result = current.isGranted ? current : await Permission.notification.request();
        if (result.isGranted) {
          widget.onComplete();
        } else if (mounted) {
          setState(() => _message = 'Notifications are needed for expiry reminders. Allow them and try again.');
        }
      }
    } catch (_) {
      if (mounted) setState(() => _message = 'Android could not complete this access step. Check system settings and try again.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  void initState() { super.initState(); WidgetsBinding.instance.addObserver(this); }

  @override
  void dispose() { WidgetsBinding.instance.removeObserver(this); super.dispose(); }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _step == 1) _refreshMediaPermission();
  }

  Future<bool> _mediaAccessGranted() async {
    try { return await _channel.invokeMethod<bool>('isNotificationListenerEnabled') ?? false; } catch (_) { return false; }
  }

  Future<void> _refreshMediaPermission() async {
    if (await _mediaAccessGranted() && mounted && _step == 1) setState(() { _step = 2; _message = null; });
  }

  @override
  Widget build(BuildContext context) {
    final items = <(IconData, String, String, Color)>[
      (Icons.bluetooth_rounded, 'Connect your vehicle', 'Allow Bluetooth access so AltrozDrive can communicate with the ConnectNext head unit.', AppColors.blue),
      (Icons.music_note_rounded, 'Enable media access', 'Enable AltrozDrive in Android Notification access settings to read media metadata and control playback.', AppColors.purple),
      (Icons.location_on_outlined, 'Allow location', 'GPS permission is used for trip route capture and location-related features.', AppColors.green),
      (Icons.notifications_active_outlined, 'Turn on reminders', 'Allow notifications so vehicle document reminders can be shown after their scheduler is migrated.', AppColors.orange),
    ];
    final item = items[_step.clamp(0, items.length - 1).toInt()];
    return Scaffold(backgroundColor: AppColors.canvas, body: SafeArea(child: Padding(padding: const EdgeInsets.fromLTRB(23, 24, 23, 25), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Eyebrow('ALTROZDRIVE • FIRST-TIME SETUP', color: AppColors.gold),
      const SizedBox(height: 17), const Text('Let’s get your drive ready.', style: TextStyle(color: AppColors.ink, fontSize: 31, height: 1.08, fontWeight: FontWeight.w800, letterSpacing: -.8)),
      const SizedBox(height: 10), const Text('A one-time setup for the features you already use.', style: TextStyle(color: AppColors.muted, fontSize: 12.5, height: 1.5)),
      const Spacer(),
      SurfaceCard(padding: const EdgeInsets.all(22), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [IconTile(item.$1, tint: item.$4, size: 56), const Spacer(), Text('STEP ${_step + 1} OF 4', style: const TextStyle(color: AppColors.muted, fontWeight: FontWeight.w800, fontSize: 9, letterSpacing: .9))]),
        const SizedBox(height: 20), Text(item.$2, style: const TextStyle(color: AppColors.ink, fontSize: 22, fontWeight: FontWeight.w800)),
        const SizedBox(height: 8), Text(item.$3, style: const TextStyle(color: AppColors.muted, fontSize: 12, height: 1.55)),
        if (_message != null) ...[const SizedBox(height: 12), Text(_message!, style: const TextStyle(color: AppColors.orange, fontSize: 11, height: 1.4))],
        const SizedBox(height: 19), Row(children: List.generate(4, (i) => Expanded(child: AnimatedContainer(duration: const Duration(milliseconds: 180), height: 4, margin: EdgeInsets.only(right: i == 3 ? 0 : 6), decoration: BoxDecoration(color: i <= _step ? AppColors.gold : AppColors.border, borderRadius: BorderRadius.circular(8)))))),
      ])),
      const Spacer(),
      FilledButton(onPressed: _busy ? null : _continue, style: FilledButton.styleFrom(backgroundColor: AppColors.navy, foregroundColor: Colors.white, minimumSize: const Size.fromHeight(54), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(17))), child: _busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : Text(_step == 3 ? 'Finish setup' : _step == 1 ? 'Open media access settings' : 'Continue', style: const TextStyle(fontWeight: FontWeight.w800))),
      const SizedBox(height: 10),
      const SizedBox(height: 4), const Text('You can change permissions later in Android Settings. Features remain unavailable until their native integrations are connected.', textAlign: TextAlign.center, style: TextStyle(fontSize: 9.5, color: AppColors.muted, height: 1.4)),
    ]))));
  }
}
