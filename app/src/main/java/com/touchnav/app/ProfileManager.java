package com.touchnav.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileManager {

    private static final String PROFILE_PREFIX = "touchnav_profile_";
    private static final String SKIP_KEY_1     = "profile_list";
    private static final String SKIP_KEY_2     = "current_profile";
    private static final String SKIP_KEY_3     = "pos_preset";
    // Uygulama durumu — profille taşınmamalı (ör. eski profili yüklemek
    // çalışan servisi "kapalı" işaretleyip açılışta başlamamasına yol açıyordu)
    private static final java.util.Set<String> STATE_KEYS = new java.util.HashSet<>(
        java.util.Arrays.asList("service_enabled", "onboarding_done"));

    /** Profil adını güvenli hale getirir: virgül liste ayırıcıdır, kullanılamaz. */
    public static String sanitizeName(String name) {
        if (name == null) return "";
        return name.replace(",", " ").trim();
    }

    private final Context context;
    private final SettingsManager settings;

    public ProfileManager(Context context) {
        this.context  = context;
        this.settings = new SettingsManager(context);
    }

    /** Tüm profil adlarını döndürür (en az "Varsayilan" içerir) */
    public List<String> getProfiles() {
        String raw = settings.getProfileList();
        List<String> list = new ArrayList<>();
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        if (list.isEmpty()) list.add("Varsayilan");
        return list;
    }

    /** Mevcut ayarları 'name' profiline kaydeder ve profil listesine ekler */
    public void saveProfile(String name) {
        name = sanitizeName(name);
        if (name.isEmpty()) return;
        SharedPreferences src = settings.getRawPrefs();
        SharedPreferences dst = context.getSharedPreferences(PROFILE_PREFIX + name, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = dst.edit();
        for (Map.Entry<String, ?> entry : src.getAll().entrySet()) {
            String k = entry.getKey();
            if (k.equals(SKIP_KEY_1) || k.equals(SKIP_KEY_2) || k.equals(SKIP_KEY_3)) continue;
            if (STATE_KEYS.contains(k)) continue;
            Object v = entry.getValue();
            if      (v instanceof Integer) e.putInt(k, (Integer) v);
            else if (v instanceof Float)   e.putFloat(k, (Float) v);
            else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
            else if (v instanceof String)  e.putString(k, (String) v);
        }
        e.apply();

        // Listeye ekle (yoksa)
        List<String> profiles = getProfiles();
        if (!profiles.contains(name)) {
            profiles.add(name);
            settings.setProfileList(join(profiles));
        }
    }

    /** 'name' profilini mevcut ayarlara yükler */
    public void loadProfile(String name) {
        SharedPreferences src = context.getSharedPreferences(PROFILE_PREFIX + name, Context.MODE_PRIVATE);
        SharedPreferences dst = settings.getRawPrefs();
        SharedPreferences.Editor e = dst.edit();
        for (Map.Entry<String, ?> entry : src.getAll().entrySet()) {
            String k = entry.getKey();
            if (k.equals(SKIP_KEY_1) || k.equals(SKIP_KEY_2) || STATE_KEYS.contains(k)) continue;
            Object v = entry.getValue();
            if      (v instanceof Integer) e.putInt(k, (Integer) v);
            else if (v instanceof Float)   e.putFloat(k, (Float) v);
            else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
            else if (v instanceof String)  e.putString(k, (String) v);
        }
        e.apply();
        settings.setCurrentProfile(name);
    }

    /** 'name' profilini siler (aktif profil silinemez, "Varsayilan" silinemez) */
    public boolean deleteProfile(String name) {
        if ("Varsayilan".equals(name)) return false;
        if (name.equals(settings.getCurrentProfile())) return false;
        List<String> profiles = getProfiles();
        profiles.remove(name);
        settings.setProfileList(join(profiles));
        // SharedPreferences dosyasını temizle
        context.getSharedPreferences(PROFILE_PREFIX + name, Context.MODE_PRIVATE).edit().clear().apply();
        return true;
    }

    private static String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
