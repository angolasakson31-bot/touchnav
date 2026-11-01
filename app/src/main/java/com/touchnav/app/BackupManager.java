package com.touchnav.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/**
 * Tüm ayarları ve profilleri tek bir JSON dosyasına yedekler / geri yükler.
 * Dosya, Android'in dosya seçicisi (SAF) ile kullanıcının seçtiği yere yazılır;
 * böylece Drive'a, İndirilenler'e veya yeni telefona taşınabilir. Ek izin gerekmez.
 *
 * Biçim:
 * { "app":"touchnav", "format":1, "appVersion":"3.5",
 *   "settings": { key: {"t":"i|l|f|b|s", "v":değer}, ... },
 *   "profiles": { ad: { key: {...}, ... }, ... } }
 */
public class BackupManager {

    private static final String APP_TAG = "touchnav";
    private static final int    FORMAT  = 1;
    private static final String PROFILE_PREFIX = "touchnav_profile_";
    private static final int    MAX_BYTES = 2 * 1024 * 1024; // bozuk/dev dosyaya karşı

    // Cihaza özgü durum: yedekle taşınmaz (yeni telefonda servis kendiliğinden
    // açılmasın, kurulum adımları yeniden gösterilsin)
    private static boolean isStateKey(String k) {
        return "service_enabled".equals(k) || "onboarding_done".equals(k);
    }

    private final Context ctx;

    public BackupManager(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    // ── Dışa aktar ───────────────────────────────────────────────
    public void exportTo(Uri uri) throws Exception {
        SettingsManager settings = new SettingsManager(ctx);
        JSONObject root = new JSONObject();
        root.put("app", APP_TAG);
        root.put("format", FORMAT);
        try {
            root.put("appVersion", ctx.getPackageManager()
                .getPackageInfo(ctx.getPackageName(), 0).versionName);
        } catch (Exception ignored) {}
        root.put("settings", prefsToJson(settings.getRawPrefs(), true));

        JSONObject profiles = new JSONObject();
        for (String name : new ProfileManager(ctx).getProfiles()) {
            SharedPreferences p = ctx.getSharedPreferences(PROFILE_PREFIX + name, Context.MODE_PRIVATE);
            if (!p.getAll().isEmpty()) profiles.put(name, prefsToJson(p, true));
        }
        root.put("profiles", profiles);

        byte[] data = root.toString(2).getBytes(StandardCharsets.UTF_8);
        OutputStream os;
        try { os = ctx.getContentResolver().openOutputStream(uri, "wt"); } // üzerine yaz + kısalt
        catch (Exception e) { os = ctx.getContentResolver().openOutputStream(uri); } // bazı sağlayıcılar "wt" desteklemez
        if (os == null) throw new Exception("open failed");
        try { os.write(data); os.flush(); } finally { os.close(); }
    }

    // ── İçe aktar ────────────────────────────────────────────────
    /** @return geri yüklenen ayar sayısı */
    public int importFrom(Uri uri) throws Exception {
        String text = readAll(uri);
        JSONObject root = new JSONObject(text);
        if (!APP_TAG.equals(root.optString("app")))
            throw new IllegalArgumentException("not a TouchNav backup");
        if (root.optInt("format", 0) > FORMAT)
            throw new IllegalArgumentException("newer backup format");

        JSONObject s = root.optJSONObject("settings");
        if (s == null) throw new IllegalArgumentException("no settings");

        // Önce her şeyi doğrula ve hazırla, sonra tek seferde yaz:
        // yarım kalmış (bozuk) bir geri yükleme olmasın.
        SettingsManager settings = new SettingsManager(ctx);
        SharedPreferences.Editor main = settings.getRawPrefs().edit();
        int count = jsonToEditor(s, main);

        JSONObject profiles = root.optJSONObject("profiles");
        StringBuilder list = new StringBuilder();
        if (profiles != null) {
            Iterator<String> it = profiles.keys();
            while (it.hasNext()) {
                String raw  = it.next();
                String name = ProfileManager.sanitizeName(raw);
                JSONObject pj = profiles.optJSONObject(raw);
                if (name.isEmpty() || pj == null) continue;
                SharedPreferences.Editor pe = ctx.getSharedPreferences(
                    PROFILE_PREFIX + name, Context.MODE_PRIVATE).edit().clear();
                jsonToEditor(pj, pe);
                pe.apply();
                if (list.length() > 0) list.append(",");
                list.append(name);
            }
        }
        // Profil listesi: yedekteki liste (yoksa yedeğin kendi kaydı / varsayılan)
        if (list.length() > 0) {
            if (!("," + list + ",").contains(",Varsayilan,")) list.insert(0, "Varsayilan,");
            main.putString("profile_list", list.toString());
        }
        main.apply();
        return count;
    }

    // ── Yardımcılar ──────────────────────────────────────────────
    private static JSONObject prefsToJson(SharedPreferences p, boolean skipState) throws Exception {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            String k = e.getKey();
            if (skipState && isStateKey(k)) continue;
            Object v = e.getValue();
            String t;
            if      (v instanceof Integer) t = "i";
            else if (v instanceof Long)    t = "l";
            else if (v instanceof Float)   t = "f";
            else if (v instanceof Boolean) t = "b";
            else if (v instanceof String)  t = "s";
            else continue; // Set<String> vb. kullanılmıyor
            JSONObject item = new JSONObject();
            item.put("t", t);
            item.put("v", v);
            o.put(k, item);
        }
        return o;
    }

    private static int jsonToEditor(JSONObject o, SharedPreferences.Editor ed) {
        int n = 0;
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (isStateKey(k)) continue;
            JSONObject item = o.optJSONObject(k);
            if (item == null || !item.has("v")) continue;
            try {
                switch (item.optString("t")) {
                    case "i": ed.putInt(k, item.getInt("v")); break;
                    case "l": ed.putLong(k, item.getLong("v")); break;
                    case "f": ed.putFloat(k, (float) item.getDouble("v")); break;
                    case "b": ed.putBoolean(k, item.getBoolean("v")); break;
                    case "s": ed.putString(k, item.getString("v")); break;
                    default: continue;
                }
                n++;
            } catch (Exception ignored) { /* hatalı tek değer atlanır */ }
        }
        return n;
    }

    private String readAll(Uri uri) throws Exception {
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) throw new Exception("open failed");
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r, total = 0;
            while ((r = is.read(buf)) != -1) {
                total += r;
                if (total > MAX_BYTES) throw new IllegalArgumentException("file too large");
                bos.write(buf, 0, r);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } finally { is.close(); }
    }
}
