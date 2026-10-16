package com.example.globalbuildingtoggle;

/**
 * The single shared state. In-memory only: resets to "building allowed" on every server start.
 */
public final class BuildingState {
    private static volatile boolean restricted = false;

    private BuildingState() {}

    public static boolean isRestricted() {
        return restricted;
    }

    public static void setRestricted(boolean value) {
        restricted = value;
    }
}
