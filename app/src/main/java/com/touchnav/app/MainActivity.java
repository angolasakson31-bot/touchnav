package com.touchnav.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;
    private static final int REQ_BATTERY = 1002;

    private SettingsManager settings;
    private boolean serviceRunning = false;

    private View         rootMain;
    private View         logoView;
    private TextView     tvTitle, tvSubtitle, tvNote, tvAppName;
    private TextView     btnThemeToggle;
    private LinearLayout statusBadge;
    private View         statusDot;
    private TextView     statusText;
    private Button       btnToggle, btnSettings;
    private LinearLayout cardsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsManager(this);
        L.init(this);

        // İlk açılışta onboarding'e yönlendir
        // try/catch: manifest eksikse çökmez, direkt ana ekrana geçer
        if (!settings.isOnboardingDone()) {
            try {
                startActivity(new Intent(this, OnboardingActivity.class));
                finish();
                return;
            } catch (Exception e) {
                settings.setOnboardingDone(true);
            }
        }

        setContentView(R.layout.activity_main);
        bindViews();
        injectCardsContainer();
        setupLogoView();
        applyTheme();
        serviceRunning = FloatingService.isRunning();
        updateStatus();
        setupClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        L.init(this);
        serviceRunning = FloatingService.isRunning();
        applyTheme();
        updateStatus();
    }

    private void bindViews() {
        rootMain       = findViewById(R.id.root_main);
        tvAppName      = findViewById(R.id.tv_app_name);
        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        logoView       = findViewById(R.id.logo_view);
        tvTitle        = findViewById(R.id.tv_title);
        tvSubtitle     = findViewById(R.id.tv_subtitle);
        statusBadge    = findViewById(R.id.status_badge);
        statusDot      = findViewById(R.id.status_dot);
        statusText     = findViewById(R.id.status_text);
        btnToggle      = findViewById(R.id.btn_toggle);
        btnSettings    = findViewById(R.id.btn_settings);
        tvNote         = findViewById(R.id.tv_note);
    }

    private void injectCardsContainer() {
        LinearLayout root = (LinearLayout) rootMain;
        int noteIdx = root.indexOfChild(tvNote);
        cardsContainer = new LinearLayout(this);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        cardsContainer.setPadding(px(16), 0, px(16), px(10));
        root.addView(cardsContainer, noteIdx, new LinearLayout.LayoutParams(-1, -2));
    }

    // ── Logo ─────────────────────────────────────────────────────
    private void setupLogoView() {
        View logo = new View(this) {
            private final Paint outerP = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint ringP  = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint glowP  = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                setLayerType(LAYER_TYPE_SOFTWARE, null);
                outerP.setStyle(Paint.Style.STROKE); outerP.setStrokeWidth(1.5f);
                ringP.setStyle(Paint.Style.STROKE);
                glowP.setStyle(Paint.Style.STROKE);
            }
            @Override protected void onDraw(Canvas canvas) {
                int c = settings.getButtonColor();
                float cx = getWidth()/2f, cy = getHeight()/2f;
                float outerR = cx-6f, innerR = outerR*0.55f;
                float mid = (outerR+innerR)/2f, thick = outerR-innerR;
                glowP.setColor(c); glowP.setAlpha(60);
                glowP.setMaskFilter(new BlurMaskFilter(thick, BlurMaskFilter.Blur.NORMAL));
                glowP.setStrokeWidth(thick);
                canvas.drawCircle(cx, cy, mid, glowP);
                ringP.setColor(c); ringP.setAlpha(220);
                ringP.setMaskFilter(null); ringP.setStrokeWidth(thick);
                canvas.drawCircle(cx, cy, mid, ringP);
                outerP.setColor(c); outerP.setAlpha(80);
                canvas.drawCircle(cx, cy, outerR-1f, outerP);
                canvas.drawCircle(cx, cy, innerR+1f, outerP);
            }
        };
        LinearLayout parent = (LinearLayout) logoView.getParent();
        int idx = parent.indexOfChild(logoView);
        parent.removeView(logoView);
        parent.addView(logo, idx, new LinearLayout.LayoutParams(px(96), px(96)));
        this.logoView = logo;
    }

    // ── Tema ─────────────────────────────────────────────────────
    private void applyTheme() {
        int theme    = settings.getTheme();
        int bgColor  = theme==1 ? 0xFFF5F5F5 : theme==2 ? 0xFF000000 : 0xFF0D0D14;
        int textPrim = theme==1 ? 0xFF1A1A1A : 0xFFFFFFFF;
        rootMain.setBackgroundColor(bgColor);
        tvAppName.setTextColor(textPrim);
        tvTitle.setTextColor(textPrim);
        tvSubtitle.setTextColor(0xFF64B5F6);
        tvSubtitle.setText(L.subtitle());
        btnThemeToggle.setText(theme==0 ? "☀" : theme==1 ? "🌙" : "⬛");
        btnThemeToggle.setTextColor(theme==0 ? 0xFFFFCC44 : theme==1 ? 0xFF335577 : 0xFFAAAAAA);

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(px(100));
        badge.setColor(theme==1 ? 0xFFE8E8F0 : 0xFF1A1A2E);
        badge.setStroke(px(1), theme==1 ? 0xFFCCCCDD : 0xFF2A2A3E);
        statusBadge.setBackground(badge);

        GradientDrawable settingsBg = new GradientDrawable();
        settingsBg.setCornerRadius(px(14));
        settingsBg.setColor(theme==1 ? 0xFFE0E0EE : 0xFF1A1A2E);
        settingsBg.setStroke(px(1), theme==1 ? 0xFFCCCCDD : 0xFF2A2A4E);
        btnSettings.setBackground(settingsBg);
        btnSettings.setTextColor(theme==1 ? 0xFF333355 : 0xFFCCCCDD);

        if (logoView != null) logoView.invalidate();
    }

    // ── Durum ─────────────────────────────────────────────────────
    private void updateStatus() {
        boolean navReady = (NavService.getInstance() != null);

        if (serviceRunning && navReady) {
            statusDot.setBackground(makeDot(0xFF4CAF50));
            statusText.setText(L.active());
            statusText.setTextColor(0xFF4CAF50);
            statusBadge.setOnClickListener(null);
            btnToggle.setText(L.stopNote());
            setToggleBg(0xFF1B3A1B);
            tvNote.setText("");
        } else if (serviceRunning) {
            statusDot.setBackground(makeDot(0xFFFF9800));
            statusText.setText(L.accessNeed());
            statusText.setTextColor(0xFFFF9800);
            statusBadge.setOnClickListener(v -> openAccessibilitySettings());
            btnToggle.setText(L.stopNote());
            setToggleBg(0xFF3A2A00);
            tvNote.setText(L.accessNote());
            tvNote.setTextColor(0xFFFF9800);
        } else {
            statusDot.setBackground(makeDot(0xFFFF5252));
            statusText.setText(L.serviceStopped());
            statusText.setTextColor(0xFFFF5252);
            statusBadge.setOnClickListener(null);
            btnToggle.setText(L.startBtn());
            setToggleBg(0xFF64B5F6);
            int theme = settings.getTheme();
            tvNote.setText("Erişilebilirlik: Ayarlar → Erişilebilirlik → TouchNav");
            tvNote.setTextColor(theme==1 ? 0xFF999999 : 0xFF444455);
        }

        buildPermissionCards();
    }

    private void setToggleBg(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(16)); bg.setColor(color);
        btnToggle.setBackground(bg);
        btnToggle.setTextColor(0xFFFFFFFF);
    }

    // ── İzin kartları ─────────────────────────────────────────────
    private void buildPermissionCards() {
        cardsContainer.removeAllViews();
        int theme = settings.getTheme();

        // 1) Erişilebilirlik
        boolean accOk = (NavService.getInstance() != null);
        addPermCard(
            accOk ? L.accOkTitle()  : L.accBadTitle(),
            accOk ? L.accOkSub()   : L.accBadSub(),
            accOk ? 0xFF2E7D32     : 0xFFB71C1C,
            accOk ? 0xFF1B3A1B     : 0xFF3A1010,
            accOk ? null           : (Runnable) this::openAccessibilitySettings);

        // 2) Arka plan izni (MIUI veya standart)
        if (isMiui()) {
            addPermCard(L.autostartTitle(), L.autostartSub(),
                0xFF1565C0, 0xFF0A1A2E, this::openMiuiAutostart);
        } else {
            boolean battOk = isBatteryOptimizationIgnored();
            addPermCard(
                battOk ? L.battOkTitle()  : L.battBadTitle(),
                battOk ? L.battOkSub()   : L.battBadSub(),
                battOk ? 0xFF2E7D32      : 0xFFE65100,
                battOk ? 0xFF1B3A1B      : 0xFF3A1C00,
                battOk ? null            : (Runnable) this::openBatterySettings);
        }

        // 3) Bildirim rozeti izni
        if (settings.isNotifBadge()) {
            addPermCard(L.notifBadgeTitle(), L.notifBadgeSub(),
                0xFF1565C0, 0xFF0A1A2E, this::openNotifAccess);
        }

        // 4) Kısıtlı Ayarlar (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addPermCard(L.restrictedTitle(), L.restrictedSub(),
                0xFF37474F,
                theme==1 ? 0xFFEEEEF5 : 0xFF0D1117,
                this::openAppInfo);
        }
    }

    private void addPermCard(String title, String sub, int titleColor,
                             int cardBg, Runnable onClick) {
        boolean light = (settings.getTheme() == 1);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(px(14), px(11), px(14), px(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(12)); bg.setColor(cardBg);
        bg.setStroke(px(1), light ? 0xFFDDDDEE : 0xFF2A2A3E);
        card.setBackground(bg);

        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        TextView tvT = new TextView(this);
        tvT.setText(title); tvT.setTextColor(titleColor);
        tvT.setTextSize(12.5f); tvT.setTypeface(null, android.graphics.Typeface.BOLD);
        tc.addView(tvT);
        TextView tvS = new TextView(this);
        tvS.setText(sub); tvS.setTextColor(light ? 0xFF777777 : 0xFF888899);
        tvS.setTextSize(11f); tvS.setPadding(0, px(2), 0, 0);
        tc.addView(tvS);
        card.addView(tc, new LinearLayout.LayoutParams(0, -2, 1f));

        if (onClick != null) {
            TextView arrow = new TextView(this);
            arrow.setText("›");
            arrow.setTextColor(light ? 0xFFBBBBCC : 0xFF555566);
            arrow.setTextSize(22f); arrow.setPadding(px(8), 0, 0, 0);
            card.addView(arrow);
            card.setOnClickListener(v -> onClick.run());
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, px(8));
        cardsContainer.addView(card, lp);
    }

    // ── Tıklama olayları ─────────────────────────────────────────
    private void setupClicks() {
        btnToggle.setOnClickListener(v -> {
            if (serviceRunning) {
                settings.setServiceEnabled(false);
                stopService(new Intent(MainActivity.this, FloatingService.class));
                serviceRunning = false;
                updateStatus();
            } else {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())),
                        REQ_OVERLAY);
                } else {
                    startFloatingService();
                    serviceRunning = true;
                    btnToggle.post(this::updateStatus);
                }
            }
        });

        btnSettings.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        btnThemeToggle.setOnClickListener(v -> {
            settings.setTheme((settings.getTheme() + 1) % 3);
            applyTheme();
            updateStatus();
        });
    }

    private void startFloatingService() {
        settings.setServiceEnabled(true);
        Intent i = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i);
        else
            startService(i);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY && Settings.canDrawOverlays(this)) {
            startFloatingService();
            serviceRunning = true;
            btnToggle.postDelayed(this::updateStatus, 300);
        }
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────
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

    private boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void openBatterySettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivityForResult(
                    new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())),
                    REQ_BATTERY);
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {}
        }
    }

    private void openAccessibilitySettings() {
        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + getPackageName())));
    }

    private void openNotifAccess() {
        try {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        } catch (Exception e) { openAppInfo(); }
    }

    private GradientDrawable makeDot(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL); d.setColor(color);
        return d;
    }

    private int px(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}
