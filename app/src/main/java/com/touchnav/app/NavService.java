package com.touchnav.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

public class NavService extends AccessibilityService {

    private static NavService instance;
    public static NavService getInstance() { return instance; }

    private boolean keyboardWasVisible = false;
    private int     lastKbTop          = -1;

    @Override
    public void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {

            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && event.getPackageName() != null) {
                // Pencere değişimini FloatingService'e bildir (otomatik gizle)
                Intent i = new Intent("com.touchnav.WINDOW_CHANGED");
                i.setPackage(getPackageName()); // Android 12+ için zorunlu
                i.putExtra("package", event.getPackageName().toString());
                sendBroadcast(i);
            }

            detectKeyboard();
        }
    }

    /**
     * Pencere listesinde TYPE_INPUT_METHOD var mı?
     * Varsa klavyenin GERÇEK üst kenar koordinatını (kb_top) ve yüksekliğini
     * (kb_height) broadcast'a ekler. kb_top, cihazdan cihaza değişen klavye
     * boylarında tahmin yapmadan tam isabetli konumlandırma sağlar.
     * Klavye görünür kalırken boyu değişirse (emoji paneli, sayı satırı vb.)
     * yeniden SHOW gönderilir ki buton yeni üst kenara taşınabilsin.
     * setPackage() olmadan Android 12+'de broadcast sessizce düşer.
     */
    private void detectKeyboard() {
        boolean kbVisible = false;
        int kbHeight = 0, kbTop = -1;
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                        kbVisible = true;
                        Rect bounds = new Rect();
                        w.getBoundsInScreen(bounds);
                        kbHeight = bounds.height();
                        kbTop    = bounds.top;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        boolean changed = (kbVisible != keyboardWasVisible);
        // Klavye açıkken üst kenar 40px'ten fazla kaydıysa (panel değişimi) yeniden bildir
        boolean resized = kbVisible && keyboardWasVisible
                && lastKbTop >= 0 && kbTop >= 0 && Math.abs(kbTop - lastKbTop) > 40;
        if (!changed && !resized) return;

        keyboardWasVisible = kbVisible;
        lastKbTop = kbVisible ? kbTop : -1;

        Intent i = new Intent(kbVisible
            ? FloatingService.ACTION_KEYBOARD_SHOW
            : FloatingService.ACTION_KEYBOARD_HIDE);
        i.setPackage(getPackageName()); // kritik: Android 12+
        if (kbVisible) {
            if (kbHeight > 0) i.putExtra("kb_height", kbHeight);
            if (kbTop    > 0) i.putExtra("kb_top",    kbTop);
        }
        sendBroadcast(i);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    public void doAction(int action) {
        switch (action) {
            case SettingsManager.ACTION_BACK:
                performGlobalAction(GLOBAL_ACTION_BACK); break;
            case SettingsManager.ACTION_FORWARD:
                // Android'de evrensel "ileri" aksiyonu yok; tarayıcılarda history.forward() için
                // IME/gesture ile simüle edilemez — Back ile aynı tuşa bağlı bırakılır
                performGlobalAction(GLOBAL_ACTION_BACK); break;
            case SettingsManager.ACTION_HOME:
                performGlobalAction(GLOBAL_ACTION_HOME); break;
            case SettingsManager.ACTION_RECENTS:
                performGlobalAction(GLOBAL_ACTION_RECENTS); break;
            case SettingsManager.ACTION_NOTIFICATIONS:
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS); break;
            case SettingsManager.ACTION_APP_DRAWER:
                openAppDrawer(); break;
        }
    }

    private void openAppDrawer() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
