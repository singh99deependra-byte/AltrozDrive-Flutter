package com.altrozdrive.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.util.ArrayList;
import java.util.List;

public final class MapLibreBridge {
    private MapLibreBridge() {}

    public static void initialize(Context context) {
        MapLibre.getInstance(context.getApplicationContext());
    }

    public static final class RouteView extends FrameLayout {
        private final MapView mapView;
        private final boolean darkStyle;
        private MapLibreMap map;
        private boolean styleReady = false;
        private double[] lat = new double[0];
        private double[] lon = new double[0];

        public RouteView(Context context) {
            this(context, false);
        }

        public RouteView(Context context, boolean darkStyle) {
            super(context);
            this.darkStyle = darkStyle;
            mapView = new MapView(context);
            addView(mapView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mapView.onCreate(null);
            mapView.getMapAsync(ready -> {
                map = ready;
                map.getUiSettings().setZoomGesturesEnabled(true);
                map.getUiSettings().setScrollGesturesEnabled(true);
                map.getUiSettings().setRotateGesturesEnabled(true);
                map.getUiSettings().setTiltGesturesEnabled(false);
                map.getUiSettings().setCompassEnabled(false);
                map.getUiSettings().setLogoEnabled(true);
                map.getUiSettings().setAttributionEnabled(true);
                ready.setStyle(new Style.Builder().fromUri(RouteMapProviderJava.styleUri(darkStyle)), style -> {
                    styleReady = true;
                    render();
                });
            });
        }

        public void setRoute(double[] latitudes, double[] longitudes) {
            lat = latitudes == null ? new double[0] : latitudes;
            lon = longitudes == null ? new double[0] : longitudes;
            render();
        }

        public void fitRoute() {
            if (map == null || lat.length < 2) return;
            try {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (int i = 0; i < Math.min(lat.length, lon.length); i++) builder.include(new LatLng(lat[i], lon[i]));
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 70));
            } catch (IllegalStateException ignored) {}
        }

        private void render() {
            if (map == null || !styleReady || lat.length < 2) return;
            map.clear();
            List<LatLng> points = new ArrayList<>();
            for (int i = 0; i < Math.min(lat.length, lon.length); i++) points.add(new LatLng(lat[i], lon[i]));
            if (points.size() < 2) return;
            map.addPolyline(new PolylineOptions().addAll(points).color(Color.rgb(29,174,204)).width(7f).alpha(1f));
            IconFactory icons = IconFactory.getInstance(getContext());
            map.addMarker(new MarkerOptions().position(points.get(0)).title("Trip start").icon(icons.fromBitmap(markerBitmap(Color.rgb(53, 199, 89)))));
            map.addMarker(new MarkerOptions().position(points.get(points.size()-1)).title("Trip end").icon(icons.fromBitmap(markerBitmap(Color.rgb(255, 82, 82)))));
            fitRoute();
        }

        private Bitmap markerBitmap(int color) {
            int size = 72;
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(size / 2f, size / 2f, 28f, paint);
            paint.setColor(color);
            canvas.drawCircle(size / 2f, size / 2f, 21f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(size / 2f, size / 2f, 7f, paint);
            return bitmap;
        }

        public void onStartMap() { mapView.onStart(); }
        public void onResumeMap() { mapView.onResume(); }
        public void onPauseMap() { mapView.onPause(); }
        public void onStopMap() { mapView.onStop(); }
        public void onDestroyMap() { mapView.onDestroy(); map = null; }
    }
}
