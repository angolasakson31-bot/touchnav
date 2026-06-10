package com.touchnav.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

public class NavService extends AccessibilityService {

    private static NavService instance;
    public static NavService getInstance() { return instance; }

    private boolean keyboardWasVisible = false;
    private int     lastKbTop          = -1;

    private final Handler  kbHandler = new Handler();
    private final Runnable kbCheck   = this::detectKeyboard;

    @Override
    public void onServiceConnected() {
        instance = this;
        // KRİTİK: getWindows() bu bayrak olmadan HER ZAMAN boş liste döner —
        // klavye penceresi asla görülemez. XML'de tanımlı ama bazı cihazlarda
        // uygulama güncellemesi sonrası eski yapılandırma kalabildiği için
        // çalışma zamanında da zorluyoruz.
        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info != null) {
                info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
                info.eventTypes |= AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                        | AccessibilityEvent.TYPE_WINDOWS_CHANGED
                        | AccessibilityEvent.TYPE_VIEW_FOCUSED
                        | AccessibilityEvent.TYPE_VIEW_CLICKED;
                setServiceInfo(info);
            }
        } catch (Exception ignored) {}
        // Servis bağlanırken klavye zaten açık olabilir
        scheduleKeyboardChecks();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getPackageName() != null) {
            // Pencere değişimini FloatingService'e bildir (otomatik gizle)
            Intent i = new Intent("com.touchnav.WINDOW_CHANGED");
            i.setPackage(getPackageName()); // Android 12+ için zorunlu
            i.putExtra("package", event.getPackageName().toString());
            sendBroadcast(i);
        }

        // Klavye kontrolü tetikleyicileri:
        //  • pencere olayları: klavye penceresi eklendi/kaldırıldı
        //  • odak/tıklama: metin kutusuna dokunuldu — bazı OEM'lerde pencere
        //    olayı gecikir veya hiç gelmez, bu yüzden bunlar da tetikler
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_FOCUSED
                || type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            scheduleKeyboardChecks();
        }
    }

    /**
     * Klavye penceresi, tetikleyici olaydan birkaç yüz ms SONRA (açılış
     * animasyonu sırasında) pencere listesine girer/çıkar. Tek seferlik
     * kontrol bu yüzden kaçırabilir: hemen + 250ms + 600ms sonra üç kez
     * bakılır. Bekleyen kontroller önce iptal edilir (olay fırtınasında
     * birikme olmaz). detectKeyboard durumu değişmedikçe yayın göndermez,
     * dolayısıyla tekrarlı çağrı zararsızdır.
     */
    private void scheduleKeyboardChecks() {
        kbHandler.removeCallbacks(kbCheck);
        detectKeyboard();
        kbHandler.postDelayed(kbCheck, 250);
        kbHandler.postDelayed(kbCheck, 600);
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
        kbHandler.removeCallbacksAndMessages(null);
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
