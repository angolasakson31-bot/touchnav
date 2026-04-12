package com.touchnav.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class SettingsManager {
    private static final String PREFS = "touchnav_prefs";
    private SharedPreferences prefs;

    // ── Aksiyonlar ───────────────────────────────────────────────
    public static final int ACTION_BACK          = 0;
    public static final int ACTION_FORWARD       = 1;
    public static final int ACTION_HOME          = 2;
    public static final int ACTION_RECENTS       = 3;
    public static final int ACTION_NOTIFICATIONS = 4;
    public static final int ACTION_APP_DRAWER    = 5;
    public static final int ACTION_NONE          = 6;
    public static final int ACTION_SCREEN_RECORD = 7;
    public static final int ACTION_TRANSPARENCY  = 8;
    public static final int ACTION_ASSISTANT     = 9;
    public static final int ACTION_TORCH         = 10;
    /** Maksimum geçerli aksiyon indeksi — spinner clamp için */
    public static final int ACTION_MAX_VALID     = 10;

    // ── Buton stilleri ───────────────────────────────────────────
    public static final int STYLE_GHOST   = 0;
    public static final int STYLE_FROST   = 1;
    public static final int STYLE_SHADOW  = 2;
    public static final int STYLE_MINIMAL = 3;
    public static final int STYLE_NEON    = 4;
    public static final int STYLE_CRYSTAL = 5;
    public static final int STYLE_PLASMA  = 6;
    public static final int STYLE_SOLID   = 7;
    public static final int STYLE_HALO    = 8;
    public static final int STYLE_COMET   = 9;
    public static final int STYLE_DOTRING = 10;
    public static final int STYLE_FILNEON = 11;
    public static final int STYLE_CROSS   = 12;

    // ── Buton şekilleri ──────────────────────────────────────────
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_DROP   = 2;
    public static final int SHAPE_HEX    = 3;

    // ── Buton içi ikon ──────────────────────────────────────────
    public static final int ICON_NONE = 0;
    public static final int ICON_DOT  = 1;
    public static final int ICON_NAV  = 2;
    public static final int ICON_RING = 3;

    // ── Temalar ──────────────────────────────────────────────────
    public static final int THEME_MIDNIGHT = 0;
    public static final int THEME_LIGHT    = 1;
    public static final int THEME_AMOLED   = 2;

    // ── Konum önayarları ─────────────────────────────────────────
    public static final int PRESET_NONE   = -1;
    public static final int PRESET_LEFT   =  0;
    public static final int PRESET_RIGHT  =  1;
    public static final int PRESET_TOP    =  2;
    public static final int PRESET_BOTTOM =  3;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── Konum ────────────────────────────────────────────────────
    public int getX() { return prefs.getInt("pos_x", 100); }
    public int getY() { return prefs.getInt("pos_y", 400); }
    public void savePosition(int x, int y) {
        prefs.edit().putInt("pos_x", x).putInt("pos_y", y).apply();
    }
    public int getPositionPreset() { return prefs.getInt("pos_preset", PRESET_NONE); }
    public void setPositionPreset(int v) { prefs.edit().putInt("pos_preset", v).apply(); }

    // ── Kilit ────────────────────────────────────────────────────
    public boolean isLocked() { return prefs.getBoolean("locked", false); }
    public void setLocked(boolean v) { prefs.edit().putBoolean("locked", v).apply(); }

    // ── Görünüm ──────────────────────────────────────────────────
    public float getOpacity() { return prefs.getFloat("opacity", 0.7f); }
    public void setOpacity(float v) { prefs.edit().putFloat("opacity", v).apply(); }
    public int getSize() { return prefs.getInt("size", 60); }
    public void setSize(int v) { prefs.edit().putInt("size", v).apply(); }

    // ── Titreşim ─────────────────────────────────────────────────
    public boolean isVibrationEnabled() { return prefs.getBoolean("vibration", true); }
    public void setVibrationEnabled(boolean v) { prefs.edit().putBoolean("vibration", v).apply(); }
    /**
     * Titreşim süresi/şiddeti: 0–100.
     * 0-33 → hafif/kısa (KEYBOARD_TAP)
     * 34-66 → orta (VIRTUAL_KEY)
     * 67-100 → güçlü/uzun (LONG_PRESS)
     */
    public int getVibrationLevel() { return prefs.getInt("vib_level", 50); }
    public void setVibrationLevel(int v) { prefs.edit().putInt("vib_level", Math.max(0, Math.min(100, v))).apply(); }

    // ── Ghost / Şeffaflık ────────────────────────────────────────
    public boolean isGhostMode() { return prefs.getBoolean("ghost_mode", true); }
    public void setGhostMode(boolean v) { prefs.edit().putBoolean("ghost_mode", v).apply(); }
    public boolean isTransparencyShortcut() { return prefs.getBoolean("transparency_shortcut", false); }
    public void setTransparencyShortcut(boolean v) { prefs.edit().putBoolean("transparency_shortcut", v).apply(); }
    public int getTransparencyDuration() { return prefs.getInt("transparency_duration", 5000); }
    public void setTransparencyDuration(int v) { prefs.edit().putInt("transparency_duration", v).apply(); }

    // ── Nabız / Flash ────────────────────────────────────────────
    public boolean isPulseEnabled() { return prefs.getBoolean("pulse", false); }
    public void setPulseEnabled(boolean v) { prefs.edit().putBoolean("pulse", v).apply(); }
    public boolean isActionFlash() { return prefs.getBoolean("action_flash", true); }
    public void setActionFlash(boolean v) { prefs.edit().putBoolean("action_flash", v).apply(); }

    // ── Klavye küçültme ──────────────────────────────────────────
    public boolean isKeyboardShrink() { return prefs.getBoolean("keyboard_shrink", false); }
    public void setKeyboardShrink(boolean v) { prefs.edit().putBoolean("keyboard_shrink", v).apply(); }
    public int getKeyboardShrinkSize() { return prefs.getInt("keyboard_shrink_size", 40); }
    public void setKeyboardShrinkSize(int v) { prefs.edit().putInt("keyboard_shrink_size", v).apply(); }

    // ── Pil düşük uyarı ──────────────────────────────────────────
    public boolean isLowBatteryAlert() { return prefs.getBoolean("low_battery_alert", true); }
    public void setLowBatteryAlert(boolean v) { prefs.edit().putBoolean("low_battery_alert", v).apply(); }
    public int getLowBatteryThreshold() { return prefs.getInt("low_battery_threshold", 15); }
    public void setLowBatteryThreshold(int v) { prefs.edit().putInt("low_battery_threshold", v).apply(); }

    // ── Buton stili / şekli / ikonu ──────────────────────────────
    public int getButtonStyle() { return prefs.getInt("button_style", STYLE_GHOST); }
    public void setButtonStyle(int v) { prefs.edit().putInt("button_style", v).apply(); }
    public int getButtonColor() { return prefs.getInt("button_color", Color.WHITE); }
    public void setButtonColor(int v) { prefs.edit().putInt("button_color", v).apply(); }
    public int getButtonShape() { return prefs.getInt("button_shape", SHAPE_CIRCLE); }
    public void setButtonShape(int v) { prefs.edit().putInt("button_shape", v).apply(); }
    public int getButtonIcon() { return prefs.getInt("button_icon", ICON_NONE); }
    public void setButtonIcon(int v) { prefs.edit().putInt("button_icon", v).apply(); }

    // ── Tema ─────────────────────────────────────────────────────
    public int getTheme() { return prefs.getInt("theme", THEME_MIDNIGHT); }
    public void setTheme(int v) { prefs.edit().putInt("theme", v).apply(); }
    public boolean isDarkTheme() { return getTheme() != THEME_LIGHT; }
    public void setDarkTheme(boolean v) { setTheme(v ? THEME_MIDNIGHT : THEME_LIGHT); }

    // ── Zamanlama ────────────────────────────────────────────────
    public int getLongPressMs() { return prefs.getInt("long_press_ms", 300); }
    public void setLongPressMs(int v) { prefs.edit().putInt("long_press_ms", v).apply(); }
    public int getTempMoveDelay() { return prefs.getInt("temp_move_delay", 2000); }
    public void setTempMoveDelay(int v) { prefs.edit().putInt("temp_move_delay", v).apply(); }
    public int getIdleAlphaTime() { return prefs.getInt("idle_alpha_time", 3000); }
    public void setIdleAlphaTime(int v) { prefs.edit().putInt("idle_alpha_time", v).apply(); }

    // ── Asistan aksiyonu ─────────────────────────────────────────
    public String getAssistantApp() { return prefs.getString("assistant_app_pkg", ""); }
    public void setAssistantApp(String pkg) { prefs.edit().putString("assistant_app_pkg", pkg).apply(); }

    // ── Otomatik gizle ───────────────────────────────────────────
    public String getHidePackages() { return prefs.getString("hide_packages", ""); }
    public void setHidePackages(String v) { prefs.edit().putString("hide_packages", v).apply(); }

    // ── Çizim hareketi ───────────────────────────────────────────
    public boolean isDrawGestureEnabled() { return prefs.getBoolean("draw_gesture", false); }
    public void setDrawGestureEnabled(boolean v) { prefs.edit().putBoolean("draw_gesture", v).apply(); }
    public int getDrawGestureL() {
        int v = prefs.getInt("draw_gesture_l", ACTION_SCREEN_RECORD);
        return Math.min(v, ACTION_MAX_VALID);
    }
    public void setDrawGestureL(int v) { prefs.edit().putInt("draw_gesture_l", Math.min(v, ACTION_MAX_VALID)).apply(); }
    public int getDrawGestureZ() {
        int v = prefs.getInt("draw_gesture_z", ACTION_TRANSPARENCY);
        return Math.min(v, ACTION_MAX_VALID);
    }
    public void setDrawGestureZ(int v) { prefs.edit().putInt("draw_gesture_z", Math.min(v, ACTION_MAX_VALID)).apply(); }

    // ── Bildirim rozeti ──────────────────────────────────────────
    public boolean isNotifBadge() { return prefs.getBoolean("notif_badge", false); }
    public void setNotifBadge(boolean v) { prefs.edit().putBoolean("notif_badge", v).apply(); }

    // ── Hareketler (clamp ile — eski ACTION_ASSISTANT/TORCH koruması) ──
    private int safeAction(String key, int def) {
        int v = prefs.getInt(key, def);
        return Math.min(v, ACTION_MAX_VALID);
    }
    public int getSwipeRight() { return safeAction("swipe_right", ACTION_BACK); }
    public void setSwipeRight(int v) { prefs.edit().putInt("swipe_right", v).apply(); }
    public int getSwipeLeft() { return safeAction("swipe_left", ACTION_FORWARD); }
    public void setSwipeLeft(int v) { prefs.edit().putInt("swipe_left", v).apply(); }
    public int getSwipeUp() { return safeAction("swipe_up", ACTION_HOME); }
    public void setSwipeUp(int v) { prefs.edit().putInt("swipe_up", v).apply(); }
    public int getSwipeDown() { return safeAction("swipe_down", ACTION_RECENTS); }
    public void setSwipeDown(int v) { prefs.edit().putInt("swipe_down", v).apply(); }
    public int getSingleTap() { return safeAction("single_tap", ACTION_NOTIFICATIONS); }
    public void setSingleTap(int v) { prefs.edit().putInt("single_tap", v).apply(); }
    public int getDoubleTap() { return safeAction("double_tap", ACTION_APP_DRAWER); }
    public void setDoubleTap(int v) { prefs.edit().putInt("double_tap", v).apply(); }

    // ── Dil ──────────────────────────────────────────────────────
    public String getLanguage() { return prefs.getString("language", "tr"); }
    public void setLanguage(String v) { prefs.edit().putString("language", v).apply(); }

    // ── Onboarding ───────────────────────────────────────────────
    public boolean isOnboardingDone() { return prefs.getBoolean("onboarding_done", false); }
    public void setOnboardingDone(boolean v) { prefs.edit().putBoolean("onboarding_done", v).apply(); }

    // ── Profil ───────────────────────────────────────────────────
    public String getCurrentProfile() { return prefs.getString("current_profile", "Varsayilan"); }
    public void setCurrentProfile(String v) { prefs.edit().putString("current_profile", v).apply(); }
    public String getProfileList() { return prefs.getString("profile_list", "Varsayilan"); }
    public void setProfileList(String v) { prefs.edit().putString("profile_list", v).apply(); }
    public SharedPreferences getRawPrefs() { return prefs; }
}
