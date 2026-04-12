package com.touchnav.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OnboardingActivity extends Activity {

    private static final int REQ_OVERLAY = 2001;

    private SettingsManager settings;
    private int currentPage = 0;

    // Her adım tamamlandı mı
    private final boolean[] stepDone = {false, false, false, false};

    private LinearLayout pageContainer;
    private View[] dots;
    private TextView btnNext, btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsManager(this);
        L.init(this);
        buildUI();
        showPage(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sayfaya döndükten sonra durumu güncelle (ayarlardan geri gelince)
        refreshStepStatus();
        showPage(currentPage);
    }

    /** Her adımın gerçek durumunu kontrol et */
    private void refreshStepStatus() {
        // Adım 0: Ekran üstü izni — programatik kontrol mümkün
        stepDone[0] = Settings.canDrawOverlays(this);
        // Adım 1: Erişilebilirlik
        stepDone[1] = (NavService.getInstance() != null);
        // Adım 2: Arka plan
        stepDone[2] = isMiui() ? stepDone[2] : isBatteryOptimizationIgnored();
        // Adım 3: Her zaman tamamlanabilir (son ekran)
    }

    // ── UI ───────────────────────────────────────────────────────
    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D0D14);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        setContentView(root);

        // Logo
        View logo = buildLogo();
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(px(88), px(88));
        logoLp.setMargins(0, px(36), 0, px(24));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(logo, logoLp);

        // Adım göstergesi (4 halka)
        LinearLayout stepsRow = new LinearLayout(this);
        stepsRow.setOrientation(LinearLayout.HORIZONTAL);
        stepsRow.setGravity(Gravity.CENTER);
        dots = new View[4];
        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(px(10), px(10));
            dlp.setMargins(px(5), 0, px(5), 0);
            stepsRow.addView(dot, dlp);
            dots[i] = dot;
        }
        LinearLayout.LayoutParams stepsLp = new LinearLayout.LayoutParams(-2, -2);
        stepsLp.setMargins(0, 0, 0, px(28));
        root.addView(stepsRow, stepsLp);

        // Sayfa içeriği
        pageContainer = new LinearLayout(this);
        pageContainer.setOrientation(LinearLayout.VERTICAL);
        pageContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        pageContainer.setPadding(px(28), 0, px(28), 0);
        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(pageContainer, pcLp);

        // Butonlar
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(-1, -2);
        brLp.setMargins(px(24), 0, px(24), px(36));
        root.addView(btnRow, brLp);

        btnSkip = makeBtn(L.skip(), 0xFF1E1E2E, 0xFF888899);
        btnNext = makeBtn(L.next(), 0xFF64B5F6, 0xFF0A0A1A);

        LinearLayout.LayoutParams skipLp = new LinearLayout.LayoutParams(0, px(52), 0.38f);
        skipLp.setMargins(0, 0, px(10), 0);
        btnRow.addView(btnSkip, skipLp);
        btnRow.addView(btnNext, new LinearLayout.LayoutParams(0, px(52), 0.62f));

        btnSkip.setOnClickListener(v -> { haptic(v); finishOnboarding(); });
        btnNext.setOnClickListener(v -> { haptic(v); onNextClick(); });
    }

    private void showPage(int page) {
        currentPage = page;
        pageContainer.removeAllViews();
        pageContainer.setAlpha(0f);

        // Dots güncelle
        for (int i = 0; i < 4; i++) {
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            boolean active = (i == page);
            boolean done   = stepDone[i] && i < page;
            dg.setColor(done ? 0xFF4CAF50 : active ? 0xFF64B5F6 : 0xFF2A2A3E);
            dots[i].setBackground(dg);
        }

        // İçerik
        switch (page) {
            case 0: buildStepRestricted(); break;
            case 1: buildStepAccessibility(); break;
            case 2: buildStepBackground(); break;
            case 3: buildStepDone(); break;
        }

        // Buton
        boolean isLast = (page == 3);
        btnNext.setText(isLast ? L.ob4Btn() : L.next());
        btnSkip.setVisibility(isLast ? View.GONE : View.VISIBLE);

        pageContainer.animate().alpha(1f).setDuration(320)
            .setInterpolator(new OvershootInterpolator(0.7f)).start();
    }

    // ── Adım 0: Ekran Üstü İzni ──────────────────────────────────
    private void buildStepRestricted() {
        boolean canOverlay = Settings.canDrawOverlays(this);
        stepDone[0] = canOverlay;

        addTitle(L.isTr() ? "Ekran Üstü İzni" : "Overlay Permission");
        addSub(L.isTr()
            ? "TouchNav'ın diğer uygulamaların\nüzerinde görünmesi için bu izni ver."
            : "Allow TouchNav to appear\nover other applications.");

        addStatusChip(canOverlay,
            canOverlay ? (L.isTr() ? "✓ İzin Verildi" : "✓ Permission Granted")
                       : (L.isTr() ? "✕ İzin Gerekli" : "✕ Permission Required"),
            canOverlay ? (L.isTr() ? "Hazır, devam edebilirsin" : "Ready, you can proceed")
                       : (L.isTr() ? "Diğer uyg. üzerinde göster → zorunlu" : "Draw over other apps → required"));

        if (!canOverlay) {
            addActionBtn(
                L.isTr() ? "İzni Ver →" : "Grant Permission →",
                () -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            startActivityForResult(
                                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName())),
                                REQ_OVERLAY);
                        } catch (Exception e) { openAppInfo(); }
                    }
                });
        }
    }

    // ── Adım 1: Erişilebilirlik ──────────────────────────────────
    private void buildStepAccessibility() {
        addTitle(L.isTr() ? "Erişilebilirlik" : "Accessibility");
        addSub(L.isTr()
            ? "Geri / Ana Ekran gibi sistem\ntuşlarını kullanmak için erişilebilirliği aç."
            : "Enable Accessibility to use\nBack, Home and system actions.");

        boolean ok = (NavService.getInstance() != null);
        stepDone[1] = ok;
        addStatusChip(ok,
            ok ? (L.isTr() ? "✓ Erişilebilirlik Aktif" : "✓ Accessibility Active")
               : (L.isTr() ? "✕ Henüz Açılmadı" : "✕ Not Enabled Yet"),
            ok ? (L.isTr() ? "TouchNav aktif ve hazır" : "TouchNav active and ready")
               : (L.isTr() ? "Ayarlar → Erişilebilirlik → TouchNav" : "Settings → Accessibility → TouchNav"));

        if (!ok) {
            addActionBtn(
                L.isTr() ? "Erişilebilirlik Ayarlarını Aç →" : "Open Accessibility Settings →",
                () -> {
                    try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                    catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                });
        }
    }

    // ── Adım 2: Arka Plan ────────────────────────────────────────
    private void buildStepBackground() {
        addTitle(L.isTr() ? "Arka Planda Çalışma" : "Background Running");
        addSub(L.isTr()
            ? "TouchNav'ın arka planda durmaması için\notomatik başlatmaya izin ver."
            : "Allow autostart so TouchNav\ndoesn't stop in the background.");

        boolean miui = isMiui();
        boolean ok   = !miui && isBatteryOptimizationIgnored();
        stepDone[2] = miui ? stepDone[2] : ok;

        if (miui) {
            addStatusChip(stepDone[2],
                stepDone[2] ? (L.isTr() ? "✓ Ayarlandı" : "✓ Configured")
                            : (L.isTr() ? "Otomatik Başlatma Listesi" : "Autostart List"),
                L.isTr() ? "TouchNav'ı listede etkinleştir" : "Enable TouchNav in the list");
            addActionBtn(
                L.isTr() ? "Otomatik Başlatma Ayarlarını Aç →" : "Open Autostart Settings →",
                () -> {
                    stepDone[2] = true;
                    openMiuiAutostart();
                });
        } else {
            addStatusChip(ok,
                ok ? (L.isTr() ? "✓ Pil Optimizasyonu Devre Dışı" : "✓ Battery Optimization Off")
                   : (L.isTr() ? "✕ Kısıtlı" : "✕ Restricted"),
                ok ? (L.isTr() ? "Servis stabil çalışacak" : "Service will run stably")
                   : (L.isTr() ? "Optimizasyonu devre dışı bırak" : "Disable battery optimization"));
            if (!ok) {
                addActionBtn(
                    L.isTr() ? "Pil Ayarını Aç →" : "Open Battery Setting →",
                    () -> {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                startActivity(new Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + getPackageName())));
                            }
                        } catch (Exception e) {}
                    });
            }
        }
    }

    // ── Adım 3: Hazır ────────────────────────────────────────────
    private void buildStepDone() {
        stepDone[3] = true;

        // Özet: hangi adımlar tamamlandı
        addTitle(L.ob4Title());
        addSub(L.ob4Sub());

        String[] stepNames = L.isTr()
            ? new String[]{"Kısıtlı Ayarlar", "Erişilebilirlik", "Arka Plan"}
            : new String[]{"Restricted Settings", "Accessibility", "Background"};

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, px(8), 0, 0);

        for (int i = 0; i < 3; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(px(14), px(10), px(14), px(10));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(px(10));
            bg.setColor(stepDone[i] ? 0xFF1B3A1B : 0xFF2A1010);
            bg.setStroke(px(1), stepDone[i] ? 0xFF2E7D32 : 0xFF7D2E2E);
            row.setBackground(bg);

            TextView icon = new TextView(this);
            icon.setText(stepDone[i] ? "✓" : "✕");
            icon.setTextColor(stepDone[i] ? 0xFF4CAF50 : 0xFFEF5350);
            icon.setTextSize(15f);
            icon.setPadding(0, 0, px(12), 0);
            row.addView(icon);

            TextView tv = new TextView(this);
            tv.setText(stepNames[i]);
            tv.setTextColor(stepDone[i] ? 0xFF81C784 : 0xFFEF9A9A);
            tv.setTextSize(13f);
            row.addView(tv);

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
            rlp.setMargins(0, 0, 0, px(6));
            pageContainer.addView(row, rlp);
        }
    }

    // ── Yardımcı view builder'lar ────────────────────────────────
    private void addTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(24f);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, px(12));
        pageContainer.addView(tv, lp);
    }

    private void addSub(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF888899);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setLineSpacing(px(4), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, px(20));
        pageContainer.addView(tv, lp);
    }

    private void addStatusChip(boolean ok, String title, String sub) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setPadding(px(16), px(12), px(16), px(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(12));
        bg.setColor(ok ? 0xFF1B3A1B : 0xFF1A1A2E);
        bg.setStroke(px(1), ok ? 0xFF2E7D32 : 0xFF2A2A4E);
        chip.setBackground(bg);

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTextColor(ok ? 0xFF4CAF50 : 0xFF64B5F6);
        tvT.setTextSize(13f);
        tvT.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.addView(tvT);

        TextView tvS = new TextView(this);
        tvS.setText(sub);
        tvS.setTextColor(0xFF888899);
        tvS.setTextSize(11.5f);
        tvS.setPadding(0, px(3), 0, 0);
        chip.addView(tvS);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, px(12));
        pageContainer.addView(chip, lp);
    }

    private void addActionBtn(String label, Runnable action) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF0A0A1A);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(px(20), px(14), px(20), px(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(12));
        bg.setColor(0xFF64B5F6);
        tv.setBackground(bg);
        tv.setOnClickListener(v -> { haptic(v); action.run(); showPage(currentPage); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, px(4), 0, 0);
        pageContainer.addView(tv, lp);
    }

    // ── Navigasyon ───────────────────────────────────────────────
    private void onNextClick() {
        refreshStepStatus();
        // Adım 0: ekran üstü izni olmadan devam edemez (uygulama çalışmaz)
        if (currentPage == 0 && !stepDone[0]) {
            Toast.makeText(this,
                L.isTr() ? "Ekran üstü izni olmadan uygulama çalışmaz!"
                         : "App cannot work without overlay permission!",
                Toast.LENGTH_SHORT).show();
            return;
        }
        // Adım 1: erişilebilirlik olmadan nav tuşları çalışmaz
        if (currentPage == 1 && !stepDone[1]) {
            Toast.makeText(this,
                L.isTr() ? "Erişilebilirlik henüz aktif değil!"
                         : "Accessibility not enabled yet!",
                Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentPage < 3) showPage(currentPage + 1);
        else finishOnboarding();
    }

    private void finishOnboarding() {
        settings.setOnboardingDone(true);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) showPage(currentPage);
    }

    // ── Yardımcılar ──────────────────────────────────────────────
    private TextView makeBtn(String label, int bg, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(label); tv.setTextColor(textColor);
        tv.setTextSize(14f); tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(px(14)); d.setColor(bg);
        tv.setBackground(d);
        return tv;
    }

    private View buildLogo() {
        return new View(this) {
            private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                setLayerType(LAYER_TYPE_SOFTWARE, null);
                ring.setStyle(Paint.Style.STROKE);
                glow.setStyle(Paint.Style.STROKE);
            }
            @Override protected void onDraw(Canvas c) {
                float cx = getWidth()/2f, cy = getHeight()/2f;
                float r = cx - 8f, inner = r * 0.55f;
                float mid = (r + inner) / 2f, thick = r - inner;
                int color = new SettingsManager(getContext()).getButtonColor();
                glow.setColor(color); glow.setAlpha(70);
                glow.setMaskFilter(new android.graphics.BlurMaskFilter(thick, android.graphics.BlurMaskFilter.Blur.NORMAL));
                glow.setStrokeWidth(thick);
                c.drawCircle(cx, cy, mid, glow);
                ring.setColor(color); ring.setAlpha(230);
                ring.setMaskFilter(null); ring.setStrokeWidth(thick);
                c.drawCircle(cx, cy, mid, ring);
            }
        };
    }

    private boolean isMiui() {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method m = c.getDeclaredMethod("get", String.class);
            String v = (String) m.invoke(null, "ro.miui.ui.version.name");
            return v != null && !v.isEmpty();
        } catch (Exception e) {
            return "Xiaomi".equalsIgnoreCase(Build.MANUFACTURER)
                || "Redmi".equalsIgnoreCase(Build.MANUFACTURER);
        }
    }

    private boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + getPackageName())));
    }

    private void openMiuiAutostart() {
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName("com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent();
                i.setComponent(new ComponentName("com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HideAppsContainerManagementActivity"));
                startActivity(i);
            } catch (Exception e2) { openAppInfo(); }
        }
    }

    private void haptic(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private int px(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}
