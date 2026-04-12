package com.touchnav.app;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public class FloatingService extends Service {

    private static final String CHANNEL_ID   = "touchnav_channel";
    private static final String ACTION_STOP  = "com.touchnav.STOP";
    static final String ACTION_REFRESH       = "com.touchnav.REFRESH";
    static final String ACTION_SNAP          = "com.touchnav.SNAP";
    static final String ACTION_KEYBOARD_SHOW = "com.touchnav.KB_SHOW";
    static final String ACTION_KEYBOARD_HIDE = "com.touchnav.KB_HIDE";
    static final String ACTION_NOTIF_UPDATE  = "com.touchnav.NOTIF_COUNT";
    private static final int NOTIF_ID        = 1;
    private static final int DOUBLE_TAP_MS   = 300;
    private static final int SWIPE_MIN       = 55;

    private static boolean sRunning = false;
    public static boolean isRunning() { return sRunning; }

    private WindowManager              windowManager;
    private View                       floatView;
    private WindowManager.LayoutParams params;
    private SettingsManager            settings;

    private int     screenW, screenH;
    private float   dX, dY;
    private int     startX, startY;
    private boolean moved;
    private long    lastTapTime = 0;
    private boolean isLandscape = false;
    private boolean hiddenByPkg = false;
    private boolean longPressActive = false;
    private int     homeX, homeY;

    private int     currentStyle;
    private int     currentColor;
    private int     drawColor;
    private boolean keyboardVisible    = false;
    private int     savedYBeforeKb    = -1;  // klavye gelince eski Y konumunu sakla
    private int     notifCount        = 0;
    private boolean batteryLow       = false;
    private boolean transparencyActive = false;

    private ValueAnimator pulseAnimator;
    private ValueAnimator flashAnimator;

    private final Handler  longPressHandler    = new Handler();
    private       Runnable longPressRunnable   = null;
    private final Handler  ghostHandler        = new Handler();
    private       Runnable ghostRunnable       = null;
    private final Handler  transparencyHandler = new Handler();
    private       Runnable transparencyRunnable = null;
    // Klavye tespiti için ViewTreeObserver yöntemi — AccessibilityService'ten bağımsız
    private View    kbDetectorView;
    private int     maxKbDetectorH = 0;   // klavye kapalıyken maksimum görünür yükseklik
    private final Handler  kbCheckHandler  = new Handler();
    private final Runnable kbCheckRunnable = new Runnable() {
        @Override public void run() { checkKbFromDetector(); kbCheckHandler.postDelayed(this, 400); }
    };

    private final List<float[]> touchPath = new ArrayList<>();
    private static final int PATH_MIN_DIST = 15;

    // ── Receivers ────────────────────────────────────────────────
    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) { stopSelf(); }
    };

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) { applySettings(); }
    };

    private final BroadcastReceiver snapReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) { applyPositionPreset(); }
    };

    private final BroadcastReceiver windowReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) {
            String pkg = i.getStringExtra("package");
            if (pkg == null) return;
            String list = settings.getHidePackages();
            hiddenByPkg = false;
            if (!list.isEmpty())
                for (String p : list.split(","))
                    if (pkg.trim().equals(p.trim())) { hiddenByPkg = true; break; }
            updateVisibility();
        }
    };

    // Klavye receiver — tek instance, IntentFilter'a iki action ekliyoruz
    private final BroadcastReceiver keyboardReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) {
            boolean show = ACTION_KEYBOARD_SHOW.equals(i.getAction());
            if (keyboardVisible == show) return;
            keyboardVisible = show;
            if (floatView == null) return;

            if (show) {
                // Klavye yüksekliği NavService'ten gelir; yoksa screenH'ın %42'si tahmin
                int kbHeight = i.getIntExtra("kb_height", (int)(screenH * 0.42f));
                // Minimum mantıklı klavye yüksekliği: 150px
                if (kbHeight < 150) kbHeight = (int)(screenH * 0.42f);
                int visibleBottom = screenH - kbHeight;

                // Her zaman mevcut Y konumunu kaydet
                savedYBeforeKb = params.y;

                // Butonun alt kenarı görünür alanın dışına taşıyorsa yukarı taşı
                int btnBottom = params.y + params.height;
                if (btnBottom > visibleBottom - dpToPx(8)) {
                    params.y = Math.max(0, visibleBottom - params.height - dpToPx(16));
                }

                // Klavye küçültme özelliği aktifse boyutu da küçült
                if (settings.isKeyboardShrink()) {
                    int sizePx = dpToPx(settings.getKeyboardShrinkSize());
                    params.width  = sizePx;
                    params.height = sizePx;
                    params.x = clamp(params.x, 0, screenW - sizePx);
                    params.y = clamp(params.y, 0, visibleBottom - sizePx - dpToPx(8));
                }

                homeX = params.x;
                homeY = params.y;
            } else {
                // Klavye kapandı — eski Y konumuna dön
                if (savedYBeforeKb >= 0) {
                    params.y = savedYBeforeKb;
                    savedYBeforeKb = -1;
                }

                // Klavye küçültme özelliği aktifse boyutu geri yükle
                if (settings.isKeyboardShrink()) {
                    int sizePx = dpToPx(settings.getSize());
                    params.width  = sizePx;
                    params.height = sizePx;
                    params.x = clamp(params.x, 0, screenW - sizePx);
                    params.y = clamp(params.y, 0, screenH - sizePx);
                }

                homeX = params.x;
                homeY = params.y;
            }

            try {
                windowManager.updateViewLayout(floatView, params);
                floatView.requestLayout();
                floatView.invalidate();
            } catch (Exception ignored) {}
        }
    };

    /**
     * ViewTreeObserver + polling tabanlı klavye tespiti.
     *
     * KRİTİK: FLAG_NOT_FOCUSABLE ALONE disables SOFT_INPUT_ADJUST_RESIZE.
     * Fix: FLAG_NOT_FOCUSABLE | FLAG_ALT_FOCUSABLE_IM combination.
     *   → Window never takes focus (underlying app unaffected)
     *   → BUT IME still resizes this window (ADJUST_RESIZE works)
     *
     * Height tracking: record max observed height as baseline.
     * If current height drops > 150dp below baseline → keyboard is open.
     * 400ms polling also runs as fallback for ROMs that skip the listener.
     */
    private void createKeyboardDetector() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams kbp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, // CRITICAL: enables ADJUST_RESIZE
                PixelFormat.TRANSLUCENT);
        // No FLAG_LAYOUT_IN_SCREEN — window must shrink with IME
        kbp.gravity = Gravity.TOP | Gravity.LEFT;
        kbp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        kbDetectorView = new View(this);
        kbDetectorView.setAlpha(0f);

        // Fires immediately on layout change (keyboard open/close)
        kbDetectorView.getViewTreeObserver().addOnGlobalLayoutListener(this::checkKbFromDetector);

        try {
            windowManager.addView(kbDetectorView, kbp);
        } catch (Exception e) {
            kbDetectorView = null;
            return;
        }
        // Polling fallback for ROMs that don't fire the layout listener
        kbCheckHandler.postDelayed(kbCheckRunnable, 800);
    }

    /** Detector view'ın yüksekliğine bakarak klavye durumunu günceller */
    private void checkKbFromDetector() {
        if (kbDetectorView == null) return;
        int visH = kbDetectorView.getHeight();
        if (visH <= 0) return;
        if (visH > maxKbDetectorH) maxKbDetectorH = visH; // baseline güncelle
        if (maxKbDetectorH == 0) return;
        int kbH   = maxKbDetectorH - visH;
        boolean kbOpen = kbH > dpToPx(150);
        if (kbOpen != keyboardVisible) {
            Intent fake = new Intent(kbOpen ? ACTION_KEYBOARD_SHOW : ACTION_KEYBOARD_HIDE);
            fake.setPackage(getPackageName());
            if (kbOpen && kbH > 0) fake.putExtra("kb_height", kbH);
            keyboardReceiver.onReceive(this, fake);
        }
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) {
            int level = i.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 100);
            int scale = i.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
            int pct   = scale > 0 ? (int)(100f * level / scale) : 100;
            boolean wasLow = batteryLow;
            batteryLow = settings.isLowBatteryAlert() && (pct <= settings.getLowBatteryThreshold());
            if (wasLow != batteryLow && floatView != null) {
                updateDrawColor();
                floatView.invalidate();
            }
        }
    };

    private final BroadcastReceiver notifReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) {
            notifCount = i.getIntExtra("count", 0);
            if (floatView != null) floatView.invalidate();
        }
    };

    // ── Yaşam döngüsü ────────────────────────────────────────────
    @Override public void onCreate() {
        super.onCreate();
        sRunning  = true;
        settings  = new SettingsManager(this);
        L.init(this);
        currentStyle = settings.getButtonStyle();
        currentColor = settings.getButtonColor();
        updateDrawColor();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        isLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        boolean s31 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        reg(stopReceiver,    new IntentFilter(ACTION_STOP),                   s31);
        reg(refreshReceiver, new IntentFilter(ACTION_REFRESH),                s31);
        reg(snapReceiver,    new IntentFilter(ACTION_SNAP),                   s31);
        reg(windowReceiver,  new IntentFilter("com.touchnav.WINDOW_CHANGED"), s31);
        reg(notifReceiver,   new IntentFilter(ACTION_NOTIF_UPDATE),           s31);

        // Klavye receiver: TEK instance, çift action — doğru yöntem
        IntentFilter kbFilter = new IntentFilter();
        kbFilter.addAction(ACTION_KEYBOARD_SHOW);
        kbFilter.addAction(ACTION_KEYBOARD_HIDE);
        if (s31) registerReceiver(keyboardReceiver, kbFilter, Context.RECEIVER_NOT_EXPORTED);
        else     registerReceiver(keyboardReceiver, kbFilter);

        // Pil değişimi: sistem broadcast'ı, RECEIVER_NOT_EXPORTED kullanılmaz
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        createFloatingButton();
        createKeyboardDetector();
        // Sticky pil broadcast'ı createFloatingButton öncesinde gelmiş olabilir;
        // floatView null iken uygulanamayan rengi şimdi yenile.
        updateDrawColor();
        if (floatView != null) floatView.invalidate();
    }

    private void reg(BroadcastReceiver r, IntentFilter f, boolean s31) {
        if (s31) registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
        else     registerReceiver(r, f);
    }

    @Override public void onConfigurationChanged(Configuration cfg) {
        super.onConfigurationChanged(cfg);
        isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;
        updateVisibility();
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        sRunning = false;
        if (floatView != null) try { windowManager.removeView(floatView); } catch (Exception ignored) {}
        stopPulse();
        if (flashAnimator != null) { flashAnimator.cancel(); flashAnimator = null; }
        try { unregisterReceiver(stopReceiver);    } catch (Exception ignored) {}
        try { unregisterReceiver(refreshReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(snapReceiver);    } catch (Exception ignored) {}
        try { unregisterReceiver(windowReceiver);  } catch (Exception ignored) {}
        try { unregisterReceiver(keyboardReceiver);} catch (Exception ignored) {}
        try { unregisterReceiver(batteryReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(notifReceiver);   } catch (Exception ignored) {}
        cancelGhostTimer();
        cancelTransparencyTimer();
        longPressHandler.removeCallbacksAndMessages(null);
        kbCheckHandler.removeCallbacksAndMessages(null);
        if (kbDetectorView != null) {
            try { windowManager.removeView(kbDetectorView); } catch (Exception ignored) {}
            kbDetectorView = null;
        }
        super.onDestroy();
    }

    // ── Ayarları uygula (sendRefresh broadcast) ───────────────────
    private void applySettings() {
        if (floatView == null) return;
        currentStyle = settings.getButtonStyle();
        currentColor = settings.getButtonColor();
        updateDrawColor();

        if (!transparencyActive) floatView.setAlpha(settings.getOpacity());

        int newPx = dpToPx(settings.getSize());
        if (!keyboardVisible && params.width != newPx) {
            params.width  = newPx;
            params.height = newPx;
            params.x = clamp(params.x, 0, screenW - newPx);
            params.y = clamp(params.y, 0, screenH - newPx);
            homeX = params.x; homeY = params.y;
            try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
        }

        cancelGhostTimer();
        if (settings.isGhostMode()) startGhostTimer();
        else if (!transparencyActive) floatView.setAlpha(settings.getOpacity());

        if (settings.isPulseEnabled()) startPulse(); else stopPulse();

        floatView.invalidate();
    }

    private void updateDrawColor() {
        drawColor = batteryLow ? 0xFFFF3333 : currentColor;
    }

    // ── Floating buton ───────────────────────────────────────────
    private void createFloatingButton() {
        floatView = new View(this) {
            private final Paint stylePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint dashPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint contourD   = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint contourL   = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint iconPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint badgeTxt   = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint linePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                setLayerType(LAYER_TYPE_SOFTWARE, null);
                stylePaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStyle(Paint.Style.FILL);
                dashPaint.setStyle(Paint.Style.STROKE);
                iconPaint.setTextAlign(Paint.Align.CENTER);
                iconPaint.setAntiAlias(true);

                // Koyu arka plan için: gölge dış halkası
                contourD.setStyle(Paint.Style.STROKE);
                contourD.setStrokeWidth(4f);
                contourD.setColor(0x66000000);
                contourD.setMaskFilter(new BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL));

                // Açık arka plan için: parlak iç halkası
                contourL.setStyle(Paint.Style.STROKE);
                contourL.setStrokeWidth(1.5f);
                contourL.setColor(0x50FFFFFF);

                badgePaint.setColor(0xFFE53935);
                badgePaint.setStyle(Paint.Style.FILL);
                badgeTxt.setColor(0xFFFFFFFF);
                badgeTxt.setTextAlign(Paint.Align.CENTER);
                badgeTxt.setAntiAlias(true);
            }

            @Override protected void onDraw(Canvas canvas) {
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                float r  = Math.min(cx, cy) - 4f;
                if (r <= 0) return;

                // 1) Arka plan görünürlük konturları
                drawContours(canvas, cx, cy, r);

                // 2) Clip + stil çizimi
                canvas.save();
                Path clip = buildShapePath(cx, cy, r);
                canvas.clipPath(clip);
                drawStyle(canvas, cx, cy, r);
                canvas.restore();

                // 3) İkon
                drawIcon(canvas, cx, cy, r);

                // 4) Bildirim rozeti
                if (settings.isNotifBadge() && notifCount > 0) {
                    float bx = getWidth() - r * 0.28f;
                    float by = r * 0.28f;
                    float br = r * 0.22f;
                    canvas.drawCircle(bx, by, br, badgePaint);
                    badgeTxt.setTextSize(br * 1.3f);
                    String cnt = notifCount > 9 ? "9+" : String.valueOf(notifCount);
                    canvas.drawText(cnt, bx, by + br * 0.38f, badgeTxt);
                }
            }

            private void drawContours(Canvas c, float cx, float cy, float r) {
                int shape = settings.getButtonShape();
                // Hafif şeffaf arka plan dolgusu: hem açık hem koyu zeminde düğmeyi gösterir
                fillPaint.setColor(0x18000000); // ~%10 siyah — göze batmaz ama zeminden ayırt eder
                if (shape == SettingsManager.SHAPE_CIRCLE) {
                    c.drawCircle(cx, cy, r, fillPaint);
                    c.drawCircle(cx, cy, r, contourD);
                    c.drawCircle(cx, cy, r, contourL);
                } else {
                    Path p = buildShapePath(cx, cy, r);
                    c.drawPath(p, fillPaint);
                    c.drawPath(p, contourD);
                    c.drawPath(p, contourL);
                }
            }

            private void drawStyle(Canvas c, float cx, float cy, float r) {
                stylePaint.setColor(drawColor);
                dashPaint.setColor(drawColor);
                switch (currentStyle) {
                    case SettingsManager.STYLE_GHOST: {
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setStrokeWidth(1.8f);
                        stylePaint.setAlpha(130);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        break;
                    }
                    case SettingsManager.STYLE_FROST: {
                        fillPaint.setColor(0x22FFFFFF);
                        c.drawCircle(cx, cy, r, fillPaint);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setStrokeWidth(2f);
                        stylePaint.setAlpha(190);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        break;
                    }
                    case SettingsManager.STYLE_SHADOW: {
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setMaskFilter(new BlurMaskFilter(r * 0.4f, BlurMaskFilter.Blur.OUTER));
                        stylePaint.setStrokeWidth(3f);
                        stylePaint.setAlpha(210);
                        c.drawCircle(cx, cy, r * 0.72f, stylePaint);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStrokeWidth(1.5f);
                        stylePaint.setAlpha(160);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        break;
                    }
                    case SettingsManager.STYLE_MINIMAL: {
                        float d = r * 0.28f;
                        dashPaint.setStyle(Paint.Style.STROKE);
                        dashPaint.setPathEffect(new DashPathEffect(new float[]{d, d * 0.55f}, 0));
                        dashPaint.setStrokeWidth(2.2f);
                        dashPaint.setAlpha(190);
                        c.drawCircle(cx, cy, r - 1f, dashPaint);
                        break;
                    }
                    case SettingsManager.STYLE_NEON: {
                        stylePaint.setStyle(Paint.Style.STROKE);
                        for (int k = 0; k < 3; k++) {
                            stylePaint.setMaskFilter(new BlurMaskFilter(r * (0.6f - k * 0.15f), BlurMaskFilter.Blur.NORMAL));
                            stylePaint.setStrokeWidth(3f - k * 0.5f);
                            stylePaint.setAlpha(200 - k * 40);
                            c.drawCircle(cx, cy, r - 1f, stylePaint);
                        }
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStrokeWidth(1.2f);
                        stylePaint.setAlpha(255);
                        stylePaint.setColor(0xFFFFFFFF);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        break;
                    }
                    case SettingsManager.STYLE_CRYSTAL: {
                        fillPaint.setColor(Color.argb(40,
                            Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor)));
                        c.drawCircle(cx, cy, r, fillPaint);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setStrokeWidth(2f);
                        stylePaint.setAlpha(200);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        fillPaint.setColor(0x18FFFFFF);
                        c.drawCircle(cx, cy - r * 0.18f, r * 0.42f, fillPaint);
                        break;
                    }
                    case SettingsManager.STYLE_PLASMA: {
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setMaskFilter(new BlurMaskFilter(r * 0.22f, BlurMaskFilter.Blur.NORMAL));
                        stylePaint.setStrokeWidth(2f);
                        stylePaint.setAlpha(180);
                        c.drawCircle(cx, cy, r * 0.92f, stylePaint);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setAlpha(220);
                        c.drawCircle(cx, cy, r * 0.58f, stylePaint);
                        fillPaint.setColor(Color.argb(160,
                            Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor)));
                        c.drawCircle(cx, cy, r * 0.12f, fillPaint);
                        break;
                    }
                    case SettingsManager.STYLE_SOLID: {
                        // Solid semi-transparent fill + thin border
                        fillPaint.setColor(Color.argb(70, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor)));
                        c.drawCircle(cx, cy, r, fillPaint);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setStrokeWidth(2f);
                        stylePaint.setAlpha(200);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        break;
                    }
                    case SettingsManager.STYLE_HALO: {
                        // Three concentric rings: inner crisp, middle medium, outer blur
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStrokeWidth(2.5f); stylePaint.setAlpha(255);
                        c.drawCircle(cx, cy, r * 0.55f, stylePaint);
                        stylePaint.setStrokeWidth(1.5f); stylePaint.setAlpha(160);
                        c.drawCircle(cx, cy, r * 0.78f, stylePaint);
                        stylePaint.setMaskFilter(new BlurMaskFilter(r * 0.18f, BlurMaskFilter.Blur.NORMAL));
                        stylePaint.setStrokeWidth(1f); stylePaint.setAlpha(90);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        break;
                    }
                    case SettingsManager.STYLE_COMET: {
                        // Two arcs (top-right and bottom-left), creating a yin-yang feel
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStrokeWidth(3f); stylePaint.setAlpha(230);
                        RectF oval = new RectF(cx - r + 2f, cy - r + 2f, cx + r - 2f, cy + r - 2f);
                        c.drawArc(oval, -50f, 170f, false, stylePaint);
                        stylePaint.setAlpha(120);
                        c.drawArc(oval, 130f, 170f, false, stylePaint);
                        // center dot
                        fillPaint.setColor(drawColor); fillPaint.setAlpha(200);
                        c.drawCircle(cx, cy, r * 0.1f, fillPaint);
                        break;
                    }
                    case SettingsManager.STYLE_DOTRING: {
                        // Ring of small dots
                        fillPaint.setColor(drawColor);
                        int dots = 12;
                        float dotR = r * 0.09f;
                        for (int d = 0; d < dots; d++) {
                            double angle = (2 * Math.PI * d / dots) - Math.PI / 2;
                            float dotX = (float)(cx + (r - dotR * 2) * Math.cos(angle));
                            float dotY = (float)(cy + (r - dotR * 2) * Math.sin(angle));
                            fillPaint.setAlpha(d % 2 == 0 ? 230 : 100);
                            c.drawCircle(dotX, dotY, dotR, fillPaint);
                        }
                        break;
                    }
                    case SettingsManager.STYLE_FILNEON: {
                        // Solid fill + strong neon outer glow (most visible style)
                        fillPaint.setColor(Color.argb(50, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor)));
                        c.drawCircle(cx, cy, r, fillPaint);
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setMaskFilter(new BlurMaskFilter(r * 0.55f, BlurMaskFilter.Blur.NORMAL));
                        stylePaint.setStrokeWidth(4f); stylePaint.setAlpha(255);
                        c.drawCircle(cx, cy, r * 0.75f, stylePaint);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStrokeWidth(1.5f); stylePaint.setAlpha(255);
                        stylePaint.setColor(0xFFFFFFFF);
                        c.drawCircle(cx, cy, r - 1f, stylePaint);
                        break;
                    }
                    case SettingsManager.STYLE_CROSS: {
                        // Center dot + 4 tick marks (like a crosshair/target)
                        stylePaint.setStyle(Paint.Style.STROKE);
                        stylePaint.setMaskFilter(null);
                        stylePaint.setStrokeWidth(1.5f); stylePaint.setAlpha(180);
                        c.drawCircle(cx, cy, r - 1f, stylePaint); // outer ring
                        fillPaint.setColor(drawColor); fillPaint.setAlpha(220);
                        c.drawCircle(cx, cy, r * 0.08f, fillPaint); // center dot
                        linePaint.setColor(drawColor); linePaint.setAlpha(220);
                        linePaint.setStyle(Paint.Style.STROKE);
                        linePaint.setStrokeWidth(2f); linePaint.setStrokeCap(Paint.Cap.ROUND);
                        float tick = r * 0.28f;
                        c.drawLine(cx, cy - r + 3f, cx, cy - r + 3f + tick, linePaint); // top
                        c.drawLine(cx, cy + r - 3f - tick, cx, cy + r - 3f, linePaint); // bottom
                        c.drawLine(cx - r + 3f, cy, cx - r + 3f + tick, cy, linePaint); // left
                        c.drawLine(cx + r - 3f - tick, cy, cx + r - 3f, cy, linePaint); // right
                        break;
                    }
                }
            }

            private void drawIcon(Canvas c, float cx, float cy, float r) {
                int ic = settings.getButtonIcon();
                if (ic == SettingsManager.ICON_NONE) return;
                iconPaint.setColor(drawColor);
                switch (ic) {
                    case SettingsManager.ICON_DOT:
                        iconPaint.setStyle(Paint.Style.FILL);
                        iconPaint.setAlpha(200);
                        c.drawCircle(cx, cy, r * 0.12f, iconPaint);
                        break;
                    case SettingsManager.ICON_NAV:
                        iconPaint.setStyle(Paint.Style.FILL);
                        iconPaint.setAlpha(180);
                        iconPaint.setTextSize(r * 0.55f);
                        c.drawText("◈", cx, cy + r * 0.18f, iconPaint);
                        break;
                    case SettingsManager.ICON_RING:
                        iconPaint.setStyle(Paint.Style.STROKE);
                        iconPaint.setStrokeWidth(1.5f);
                        iconPaint.setAlpha(150);
                        c.drawCircle(cx, cy, r * 0.28f, iconPaint);
                        iconPaint.setStyle(Paint.Style.FILL);
                        break;
                }
            }
        };

        floatView.setAlpha(settings.getOpacity());

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int sizePx = dpToPx(settings.getSize());
        params = new WindowManager.LayoutParams(sizePx, sizePx, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = settings.getX(); params.y = settings.getY();
        homeX = params.x; homeY = params.y;

        floatView.setOnTouchListener((v, e) -> handleTouch(e));
        windowManager.addView(floatView, params);
        updateVisibility();
        startGhostTimer();
        if (settings.isPulseEnabled()) startPulse();
    }

    // ── Şekil yolu ───────────────────────────────────────────────
    private Path buildShapePath(float cx, float cy, float r) {
        Path path = new Path();
        switch (settings.getButtonShape()) {
            case SettingsManager.SHAPE_CIRCLE: default:
                path.addCircle(cx, cy, r, Path.Direction.CW);
                break;
            case SettingsManager.SHAPE_SQUARE: {
                float cr = r * 0.35f;
                path.addRoundRect(new RectF(cx-r, cy-r, cx+r, cy+r), cr, cr, Path.Direction.CW);
                break;
            }
            case SettingsManager.SHAPE_DROP: {
                float cr = r * 0.75f;
                path.addOval(new RectF(cx-cr, cy-r*0.8f, cx+cr, cy+cr*0.4f), Path.Direction.CW);
                path.moveTo(cx - cr*0.55f, cy + cr*0.2f);
                path.lineTo(cx, cy + r);
                path.lineTo(cx + cr*0.55f, cy + cr*0.2f);
                path.close();
                break;
            }
            case SettingsManager.SHAPE_HEX: {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI/6 + i * Math.PI/3;
                    float x = (float)(cx + r * Math.cos(angle));
                    float y = (float)(cy + r * Math.sin(angle));
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                path.close();
                break;
            }
        }
        return path;
    }

    // ── Dokunuş ──────────────────────────────────────────────────
    private boolean handleTouch(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                dX = params.x - e.getRawX(); dY = params.y - e.getRawY();
                startX = (int) e.getRawX(); startY = (int) e.getRawY();
                moved = false; longPressActive = false;
                touchPath.clear();
                touchPath.add(new float[]{e.getRawX(), e.getRawY()});
                cancelGhostTimer();
                cancelTransparencyTimer();
                floatView.setAlpha(1.0f);
                doHaptic(false);

                longPressRunnable = () -> {
                    longPressActive = true;
                    doHaptic(true);
                    floatView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).start();
                };
                longPressHandler.postDelayed(longPressRunnable, settings.getLongPressMs());
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (Math.abs(e.getRawX()-startX) > 8 || Math.abs(e.getRawY()-startY) > 8) moved = true;

                if (settings.isDrawGestureEnabled() && !longPressActive && !touchPath.isEmpty()) {
                    float[] last = touchPath.get(touchPath.size() - 1);
                    if ((float) Math.hypot(e.getRawX()-last[0], e.getRawY()-last[1]) >= PATH_MIN_DIST)
                        touchPath.add(new float[]{e.getRawX(), e.getRawY()});
                }

                if (longPressActive || !settings.isLocked()) {
                    int nx = clamp((int)(e.getRawX()+dX), 0, screenW - params.width);
                    int ny = clamp((int)(e.getRawY()+dY), 0, screenH - params.height);
                    params.x = nx; params.y = ny;
                    try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (longPressRunnable != null) longPressHandler.removeCallbacks(longPressRunnable);

                int diffX = (int)e.getRawX() - startX;
                int diffY = (int)e.getRawY() - startY;

                if (longPressActive) {
                    floatView.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    if (!settings.isLocked()) {
                        homeX = params.x; homeY = params.y;
                        settings.savePosition(params.x, params.y);
                    } else {
                        final int fx = params.x, fy = params.y;
                        ghostHandler.postDelayed(() -> animateReturnHome(fx, fy), settings.getTempMoveDelay());
                    }
                    longPressActive = false;
                    startGhostTimer();
                    return true;
                }

                // Çizim hareketi
                if (settings.isDrawGestureEnabled() && touchPath.size() >= 6) {
                    int g = recognizeGesture();
                    if (g == 1) { doActionWithHaptic(settings.getDrawGestureL()); startGhostTimer(); return true; }
                    if (g == 2) { doActionWithHaptic(settings.getDrawGestureZ()); startGhostTimer(); return true; }
                }

                if (!settings.isLocked()) {
                    settings.savePosition(params.x, params.y);
                    homeX = params.x; homeY = params.y;
                }
                startGhostTimer();

                if (Math.abs(diffX) >= SWIPE_MIN && Math.abs(diffX) > Math.abs(diffY)) {
                    doActionWithHaptic(diffX > 0 ? settings.getSwipeRight() : settings.getSwipeLeft());
                } else if (Math.abs(diffY) >= SWIPE_MIN && Math.abs(diffY) > Math.abs(diffX)) {
                    doActionWithHaptic(diffY < 0 ? settings.getSwipeUp() : settings.getSwipeDown());
                } else if (!moved) {
                    long now = System.currentTimeMillis();
                    if (now - lastTapTime < DOUBLE_TAP_MS) {
                        doActionWithHaptic(settings.getDoubleTap());
                        lastTapTime = 0;
                    } else {
                        lastTapTime = now;
                        final long t = now;
                        ghostHandler.postDelayed(() -> {
                            if (lastTapTime == t) doActionWithHaptic(settings.getSingleTap());
                        }, DOUBLE_TAP_MS + 50);
                    }
                }
                return true;
            }
        }
        return false;
    }

    // ── Çizim hareketi ───────────────────────────────────────────
    private int recognizeGesture() {
        int n = touchPath.size();
        if (n < 6) return 0;
        float[] p0 = touchPath.get(0);
        float[] pm = touchPath.get(n / 2);
        float[] p1 = touchPath.get(n - 1);
        float seg1dx = pm[0]-p0[0], seg1dy = pm[1]-p0[1];
        float seg2dx = p1[0]-pm[0], seg2dy = p1[1]-pm[1];
        boolean s1vert  = Math.abs(seg1dy) > Math.abs(seg1dx) * 1.2f;
        boolean s2horiz = Math.abs(seg2dx) > Math.abs(seg2dy) * 1.2f;
        boolean s1horiz = Math.abs(seg1dx) > Math.abs(seg1dy) * 1.2f;
        boolean s2diag  = Math.abs(seg2dx) > 20 && Math.abs(seg2dy) > 20
                && Math.signum(seg2dx) != Math.signum(seg1dx);
        float totalDist = (float) Math.hypot(p1[0]-p0[0], p1[1]-p0[1]);
        if (totalDist < 80) return 0;
        if (s1vert && s2horiz && Math.abs(seg1dy) > 50 && Math.abs(seg2dx) > 50) return 1;
        if (s1horiz && s2diag && Math.abs(seg1dx) > 50) return 2;
        return 0;
    }

    // ── Aksiyon ──────────────────────────────────────────────────
    private void doActionWithHaptic(int action) {
        if (settings.isActionFlash()) flashButton();
        doHaptic(false);
        doAction(action);
    }

    private void doAction(int action) {
        switch (action) {
            case SettingsManager.ACTION_SCREEN_RECORD: startScreenRecord(); return;
            case SettingsManager.ACTION_TRANSPARENCY:  toggleTransparency(); return;
            case SettingsManager.ACTION_ASSISTANT:     launchAssistant(); return;
            case SettingsManager.ACTION_TORCH:         toggleTorch(); return;
            case SettingsManager.ACTION_NONE: return;
        }
        NavService nav = NavService.getInstance();
        if (nav != null) nav.doAction(action);
    }

    private void startScreenRecord() {
        try {
            Intent i = new Intent();
            i.setClassName("com.miui.screenrecorder", "com.miui.screenrecorder.RecordingStartingActivity");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            NavService nav = NavService.getInstance();
            if (nav != null && Build.VERSION.SDK_INT >= 28)
                nav.performGlobalAction(NavService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS);
        }
    }

    private void toggleTransparency() {
        if (transparencyActive) { cancelTransparencyTimer(); return; }
        transparencyActive = true;
        // Görünmez + dokunulamaz yap (tam gizlilik — o konuma basınca buton olmaz)
        floatView.animate().alpha(0f).setDuration(150).start();
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
        transparencyRunnable = this::cancelTransparencyTimer;
        transparencyHandler.postDelayed(transparencyRunnable, settings.getTransparencyDuration());
    }

    private void cancelTransparencyTimer() {
        if (transparencyRunnable != null) { transparencyHandler.removeCallbacks(transparencyRunnable); transparencyRunnable = null; }
        transparencyActive = false;
        // Dokunulabilirliği geri ver, yavaşça görün
        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
        if (floatView != null) floatView.animate().alpha(settings.getOpacity()).setDuration(400).start();
    }

    private void launchAssistant() {
        // Sistem sesli asistanını tetikle (Google Assistant, Bixby, vs.)
        try {
            Intent i = new Intent("android.intent.action.ASSIST");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e1) {
            try {
                Intent i = new Intent(Intent.ACTION_VOICE_COMMAND);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception ignored) {}
        }
    }

    private boolean torchOn = false;
    private void toggleTorch() {
        try {
            android.hardware.camera2.CameraManager cm =
                (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
            if (cm == null) return;
            String[] ids = cm.getCameraIdList();
            if (ids.length == 0) return;
            torchOn = !torchOn;
            cm.setTorchMode(ids[0], torchOn);
        } catch (Exception ignored) {}
    }

    // ── Flash animasyonu ─────────────────────────────────────────
    private void flashButton() {
        if (floatView == null) return;
        if (flashAnimator != null) flashAnimator.cancel();
        final int savedColor = currentColor;
        currentColor = 0xFFFFFFFF;
        updateDrawColor();
        floatView.invalidate();
        flashAnimator = ValueAnimator.ofArgb(0xFFFFFFFF, savedColor);
        flashAnimator.setDuration(280);
        flashAnimator.addUpdateListener(va -> {
            currentColor = (int) va.getAnimatedValue();
            updateDrawColor();
            if (floatView != null) floatView.invalidate();
        });
        flashAnimator.start();
        floatView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(80)
            .withEndAction(() -> { if (floatView != null) floatView.animate().scaleX(1f).scaleY(1f).setDuration(160).start(); })
            .start();
    }

    // ── Nabız animasyonu ─────────────────────────────────────────
    private void startPulse() {
        if (pulseAnimator != null) return;
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.07f, 1f);
        pulseAnimator.setDuration(2200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(va -> {
            if (floatView == null) return;
            float s = (float) va.getAnimatedValue();
            floatView.setScaleX(s); floatView.setScaleY(s);
        });
        pulseAnimator.start();
    }

    private void stopPulse() {
        if (pulseAnimator == null) return;
        pulseAnimator.cancel(); pulseAnimator = null;
        if (floatView != null) { floatView.setScaleX(1f); floatView.setScaleY(1f); }
    }

    // ── Haptic ───────────────────────────────────────────────────
    /**
     * vib_level 0-100:
     * 0-33   → KEYBOARD_TAP (kısa/hafif)
     * 34-66  → VIRTUAL_KEY  (orta)
     * 67-100 → LONG_PRESS   (güçlü/uzun)
     * longPress=true ise her zaman LONG_PRESS
     */
    private void doHaptic(boolean longPress) {
        if (!settings.isVibrationEnabled() || floatView == null) return;
        int level = settings.getVibrationLevel();
        int constant;
        if (longPress || level >= 67)      constant = HapticFeedbackConstants.LONG_PRESS;
        else if (level >= 34)              constant = HapticFeedbackConstants.VIRTUAL_KEY;
        else                               constant = HapticFeedbackConstants.KEYBOARD_TAP;
        floatView.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    // ── Ghost ────────────────────────────────────────────────────
    private void startGhostTimer() {
        cancelGhostTimer();
        if (!settings.isGhostMode()) return;
        ghostRunnable = () -> {
            if (floatView != null && !transparencyActive)
                floatView.animate().alpha(0.22f).setDuration(600).start();
        };
        ghostHandler.postDelayed(ghostRunnable, settings.getIdleAlphaTime());
    }

    private void cancelGhostTimer() {
        if (ghostRunnable != null) { ghostHandler.removeCallbacks(ghostRunnable); ghostRunnable = null; }
    }

    // ── Preset / Animasyon ───────────────────────────────────────
    private void applyPositionPreset() {
        int sz = params.width, m = dpToPx(12);
        switch (settings.getPositionPreset()) {
            case SettingsManager.PRESET_LEFT:   params.x=m;              params.y=screenH/2-sz/2; break;
            case SettingsManager.PRESET_RIGHT:  params.x=screenW-sz-m;   params.y=screenH/2-sz/2; break;
            case SettingsManager.PRESET_TOP:    params.x=screenW/2-sz/2; params.y=m;              break;
            case SettingsManager.PRESET_BOTTOM: params.x=screenW/2-sz/2; params.y=screenH-sz-m;   break;
            default: return;
        }
        homeX=params.x; homeY=params.y;
        settings.savePosition(params.x, params.y);
        try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
    }

    private void animateReturnHome(int fromX, int fromY) {
        if (floatView == null) return;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(400);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(va -> {
            if (floatView == null) return;
            float t = (float) va.getAnimatedValue();
            params.x = (int)(fromX + (homeX-fromX)*t);
            params.y = (int)(fromY + (homeY-fromY)*t);
            try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
        });
        anim.start();
    }

    private void updateVisibility() {
        if (floatView == null) return;
        floatView.setVisibility((isLandscape || hiddenByPkg) ? View.GONE : View.VISIBLE);
    }

    // ── Bildirim ─────────────────────────────────────────────────
    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getBroadcast(this, 0,
            new Intent(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("TouchNav")
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .addAction(android.R.drawable.ic_delete, getString(R.string.action_stop), pi)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private int dpToPx(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }
    private static int clamp(int v, int mn, int mx) { return Math.max(mn, Math.min(mx, v)); }
}
