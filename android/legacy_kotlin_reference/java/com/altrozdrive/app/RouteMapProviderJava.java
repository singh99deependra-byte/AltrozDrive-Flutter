package com.altrozdrive.app;
public final class RouteMapProviderJava {
    private RouteMapProviderJava() {}
    public static String styleUri() { return styleUri(false); }
    public static String styleUri(boolean dark) { return dark ? "https://tiles.openfreemap.org/styles/dark" : "https://tiles.openfreemap.org/styles/liberty"; }
}
