import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class ScreenPage extends StatelessWidget {
  final String title;
  final String subtitle;
  final Widget child;
  final Widget? trailing;
  final EdgeInsets padding;
  const ScreenPage({super.key, required this.title, required this.subtitle, required this.child, this.trailing, this.padding = const EdgeInsets.fromLTRB(18, 17, 18, 28)});

  @override
  Widget build(BuildContext context) => CustomScrollView(
        slivers: [
          SliverPadding(
            padding: padding,
            sliver: SliverList.list(children: [
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(title, style: const TextStyle(fontSize: 29, fontWeight: FontWeight.w800, letterSpacing: -.8, color: AppColors.ink, height: 1.08)),
                  const SizedBox(height: 5),
                  Text(subtitle, style: const TextStyle(fontSize: 12.5, color: AppColors.muted, height: 1.4)),
                ])),
                if (trailing != null) ...[const SizedBox(width: 10), trailing!],
              ]),
              const SizedBox(height: 20),
              child,
            ]),
          ),
        ],
      );
}

class SurfaceCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  final Color color;
  final Border? border;
  final double radius;
  final bool shadow;
  const SurfaceCard({super.key, required this.child, this.padding = const EdgeInsets.all(16), this.color = AppColors.surface, this.border, this.radius = 22, this.shadow = true});

  @override
  Widget build(BuildContext context) => Container(
        padding: padding,
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(radius),
          border: border ?? Border.all(color: AppColors.border.withOpacity(.9)),
          boxShadow: shadow ? [BoxShadow(color: AppColors.ink.withOpacity(.035), blurRadius: 18, offset: const Offset(0, 7))] : const [],
        ),
        child: child,
      );
}

class Eyebrow extends StatelessWidget {
  final String label;
  final Color color;
  const Eyebrow(this.label, {super.key, this.color = AppColors.muted});
  @override
  Widget build(BuildContext context) => Text(label.toUpperCase(), style: TextStyle(fontSize: 9.5, fontWeight: FontWeight.w800, letterSpacing: 1.2, color: color));
}

class SectionHeading extends StatelessWidget {
  final String title;
  final String? action;
  final VoidCallback? onAction;
  const SectionHeading(this.title, {super.key, this.action, this.onAction});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 23, bottom: 11),
        child: Row(children: [
          Expanded(child: Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.ink, letterSpacing: -.35))),
          if (action != null) InkWell(onTap: onAction, borderRadius: BorderRadius.circular(10), child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 5), child: Text(action!, style: const TextStyle(color: AppColors.gold, fontSize: 11, fontWeight: FontWeight.w800)))),
        ]),
      );
}

class MetricCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final String unit;
  final Color accent;
  const MetricCard({super.key, required this.icon, required this.label, required this.value, this.unit = '', this.accent = AppColors.gold});
  @override
  Widget build(BuildContext context) => SurfaceCard(
        padding: const EdgeInsets.all(13),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Container(width: 32, height: 32, decoration: BoxDecoration(color: accent.withOpacity(.12), borderRadius: BorderRadius.circular(11)), child: Icon(icon, color: accent, size: 17)),
          const SizedBox(height: 12),
          Text(label.toUpperCase(), maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 9, letterSpacing: .45, color: AppColors.muted, fontWeight: FontWeight.w800)),
          const SizedBox(height: 5),
          Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Flexible(child: Text(value, maxLines: 1, overflow: TextOverflow.fade, softWrap: false, style: const TextStyle(fontSize: 22, height: 1, color: AppColors.ink, fontWeight: FontWeight.w800, letterSpacing: -.65))),
            if (unit.isNotEmpty) Padding(padding: const EdgeInsets.only(left: 4, bottom: 1), child: Text(unit, style: const TextStyle(fontSize: 9, color: AppColors.muted, fontWeight: FontWeight.w700))),
          ]),
        ]),
      );
}

class StatusPill extends StatelessWidget {
  final String label;
  final Color color;
  final IconData? icon;
  const StatusPill(this.label, {super.key, this.color = AppColors.muted, this.icon});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
        decoration: BoxDecoration(color: color.withOpacity(.105), borderRadius: BorderRadius.circular(40), border: Border.all(color: color.withOpacity(.12))),
        child: Row(mainAxisSize: MainAxisSize.min, children: [if (icon != null) ...[Icon(icon, size: 12, color: color), const SizedBox(width: 5)], Text(label.toUpperCase(), style: TextStyle(fontSize: 8.5, color: color, fontWeight: FontWeight.w800, letterSpacing: .65))],),
      );
}

class IconTile extends StatelessWidget {
  final IconData icon;
  final Color tint;
  final double size;
  const IconTile(this.icon, {super.key, this.tint = AppColors.gold, this.size = 42});
  @override
  Widget build(BuildContext context) => Container(width: size, height: size, decoration: BoxDecoration(color: tint.withOpacity(.12), borderRadius: BorderRadius.circular(size * .32)), child: Icon(icon, color: tint, size: size * .48));
}

class ActionRow extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final Color tint;
  final VoidCallback? onTap;
  final Widget? trailing;
  const ActionRow({super.key, required this.icon, required this.title, required this.subtitle, this.tint = AppColors.gold, this.onTap, this.trailing});
  @override
  Widget build(BuildContext context) => InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10),
          child: Row(children: [
            IconTile(icon, tint: tint, size: 42),
            const SizedBox(width: 12),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: AppColors.ink)), const SizedBox(height: 3), Text(subtitle, style: const TextStyle(fontSize: 10.5, color: AppColors.muted, height: 1.35))])),
            trailing ?? const Icon(Icons.chevron_right_rounded, color: AppColors.muted, size: 20),
          ]),
        ),
      );
}

class EmptyDataPanel extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final String? tag;
  const EmptyDataPanel({super.key, required this.icon, required this.title, required this.description, this.tag});
  @override
  Widget build(BuildContext context) => SurfaceCard(
        color: const Color(0xFFFAF8F2),
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 25),
        child: Column(children: [
          Container(width: 58, height: 58, decoration: const BoxDecoration(color: AppColors.goldSoft, shape: BoxShape.circle), child: Icon(icon, color: AppColors.gold, size: 26)),
          const SizedBox(height: 14),
          Text(title, textAlign: TextAlign.center, style: const TextStyle(fontSize: 15, color: AppColors.ink, fontWeight: FontWeight.w800)),
          const SizedBox(height: 7),
          Text(description, textAlign: TextAlign.center, style: const TextStyle(fontSize: 11.5, color: AppColors.muted, height: 1.5)),
          if (tag != null) ...[const SizedBox(height: 13), StatusPill(tag!, color: AppColors.orange, icon: Icons.info_outline_rounded)],
        ]),
      );
}

class DetailScaffold extends StatelessWidget {
  final String title;
  final String subtitle;
  final Widget child;
  const DetailScaffold({super.key, required this.title, required this.subtitle, required this.child});
  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: AppColors.canvas,
        body: SafeArea(child: Column(children: [
          Padding(padding: const EdgeInsets.fromLTRB(12, 10, 18, 12), child: Row(children: [
            IconButton.filledTonal(onPressed: () => Navigator.of(context).maybePop(), icon: const Icon(Icons.arrow_back_rounded), style: IconButton.styleFrom(backgroundColor: AppColors.surface, foregroundColor: AppColors.ink)),
            const SizedBox(width: 8),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(fontSize: 19, color: AppColors.ink, fontWeight: FontWeight.w800)), Text(subtitle, style: const TextStyle(fontSize: 10.5, color: AppColors.muted))])),
          ])),
          Expanded(child: child),
        ])),
      );
}

class FilterSegment<T> extends StatelessWidget {
  final T value;
  final List<T> values;
  final String Function(T) label;
  final ValueChanged<T> onChanged;
  const FilterSegment({super.key, required this.value, required this.values, required this.label, required this.onChanged});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(color: AppColors.surfaceSoft, borderRadius: BorderRadius.circular(15), border: Border.all(color: AppColors.border)),
        child: Row(children: [for (final option in values) Expanded(child: GestureDetector(onTap: () => onChanged(option), child: AnimatedContainer(duration: const Duration(milliseconds: 180), padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 5), decoration: BoxDecoration(color: option == value ? AppColors.surface : Colors.transparent, borderRadius: BorderRadius.circular(11), boxShadow: option == value ? [BoxShadow(color: AppColors.ink.withOpacity(.06), blurRadius: 8, offset: const Offset(0, 2))] : []), child: Text(label(option), textAlign: TextAlign.center, maxLines: 1, style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w800, color: option == value ? AppColors.ink : AppColors.muted)))))]),
      );
}
