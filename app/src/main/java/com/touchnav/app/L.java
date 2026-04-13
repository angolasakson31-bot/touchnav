package com.touchnav.app;

import android.content.Context;

/**
 * Dil yardımcısı — tüm UI metinleri buradan geliyor.
 * Yeni kelime eklemek için her iki dile de satır ekle.
 */
public class L {

    private static String lang = "tr";

    public static void init(Context ctx) {
        lang = new SettingsManager(ctx).getLanguage();
    }

    public static boolean isTr() { return "tr".equals(lang); }

    public static String s(String tr, String en) {
        return isTr() ? tr : en;
    }

    // ── Genel ────────────────────────────────────────────────────
    public static String appName()     { return "TouchNav"; }
    public static String start()       { return s("Başlat", "Start"); }
    public static String stop()        { return s("Durdur", "Stop"); }
    public static String settings()    { return s("Ayarlar", "Settings"); }
    public static String save()        { return s("Kaydet", "Save"); }
    public static String cancel()      { return s("İptal", "Cancel"); }
    public static String delete()      { return s("Sil", "Delete"); }
    public static String ok()          { return s("Tamam", "OK"); }
    public static String newWord()     { return s("+ Yeni", "+ New"); }
    public static String back()        { return s("←", "←"); }
    public static String saved()       { return s("Kaydedildi.", "Saved."); }
    public static String cantDelete()  { return s("Silinemez.", "Cannot delete."); }

    // ── Ana ekran ─────────────────────────────────────────────────
    public static String subtitle()     { return s("YÜZEN NAVİGASYON", "FLOATING NAVIGATION"); }
    public static String active()       { return s("Aktif — Hazır ✓", "Active — Ready ✓"); }
    public static String accessNeed()   { return s("⚠  Erişilebilirlik gerekli", "⚠  Accessibility needed"); }
    public static String serviceStopped(){ return s("Servis Durduruldu", "Service Stopped"); }
    public static String accessNote()   { return s("Erişilebilirlik için rozete dokun", "Tap badge for accessibility"); }
    public static String stopNote()     { return s("⏹  Durdur", "⏹  Stop"); }
    public static String startBtn()     { return s("▶  Başlat", "▶  Start"); }

    // ── İzin kartları ─────────────────────────────────────────────
    public static String accOkTitle()   { return s("✓ Erişilebilirlik Aktif", "✓ Accessibility Active"); }
    public static String accOkSub()     { return s("Navigasyon kontrolü hazır", "Navigation control ready"); }
    public static String accBadTitle()  { return s("✕ Erişilebilirlik Kapalı", "✕ Accessibility Off"); }
    public static String accBadSub()    { return s("TouchNav'ı erişilebilirlikten etkinleştir", "Enable TouchNav in Accessibility"); }
    public static String autostartTitle(){ return s("⚙ Arka Planda Otomatik Başlatma", "⚙ Autostart in Background"); }
    public static String autostartSub() { return s("Dokun → TouchNav'ı listeden etkinleştir", "Tap → enable TouchNav from list"); }
    public static String battOkTitle()  { return s("✓ Arka Plan İzni Verildi", "✓ Background Allowed"); }
    public static String battOkSub()    { return s("Pil optimizasyonu devre dışı", "Battery optimization disabled"); }
    public static String battBadTitle() { return s("✕ Arka Plan Kısıtlı", "✕ Background Restricted"); }
    public static String battBadSub()   { return s("Dokun → kısıtlamayı kaldır", "Tap → remove restriction"); }
    public static String restrictedTitle(){ return s("ℹ Kısıtlı Ayarlar", "ℹ Restricted Settings"); }
    public static String restrictedSub(){ return s("Sorun varsa: Uygulama Bilgisi → Kısıtlı Ayarları Aç", "If issue: App Info → Allow Restricted Settings"); }
    public static String notifBadgeTitle(){ return s("⚙ Bildirim Erişimi", "⚙ Notification Access"); }
    public static String notifBadgeSub(){ return s("Rozet için bildirim iznini etkinleştir", "Enable notification access for badge"); }

    // ── Ayarlar başlıkları ────────────────────────────────────────
    public static String cardProfile()   { return s("PROFİL", "PROFILE"); }
    public static String cardStyle()     { return s("BUTON STİLİ", "BUTTON STYLE"); }
    public static String cardShape()     { return s("BUTON ŞEKLİ", "BUTTON SHAPE"); }
    public static String cardIcon()      { return s("BUTON İKONU", "BUTTON ICON"); }
    public static String cardTheme()     { return s("TEMA", "THEME"); }
    public static String cardAppearance(){ return s("GÖRÜNÜM", "APPEARANCE"); }
    public static String cardVibration() { return s("TİTREŞİM", "VIBRATION"); }
    public static String cardTiming()    { return s("ZAMANLAMA", "TIMING"); }
    public static String cardOptions()   { return s("SEÇENEKLER", "OPTIONS"); }
    public static String cardGestures()  { return s("HAREKETLER", "GESTURES"); }
    public static String cardDraw()      { return s("ÇİZİM HAREKETİ", "DRAW GESTURE"); }
    public static String cardAssistant() { return s("KİŞİSEL ASİSTAN", "PERSONAL ASSISTANT"); }
    public static String cardBattery()   { return s("PİL UYARISI", "BATTERY ALERT"); }
    public static String cardKeyboard()  { return s("KLAVYE", "KEYBOARD"); }
    public static String cardNotif()     { return s("BİLDİRİM ROZETİ", "NOTIFICATION BADGE"); }
    public static String cardTransparency(){ return s("GİZLİLİK KISAYOLU", "PRIVACY SHORTCUT"); }
    public static String cardCompat()      { return s("UYUMLU ÇALIŞMA MODU", "COMPATIBILITY MODE"); }

    // ── Ayar etiketleri ───────────────────────────────────────────
    public static String opacity()      { return s("Opaklık", "Opacity"); }
    public static String size()         { return s("Boyut", "Size"); }
    public static String vibActive()    { return s("Titreşim aktif", "Vibration active"); }
    public static String vibStrength()  { return s("Şiddet", "Strength"); }
    public static String vibDuration()  { return s("Süre", "Duration"); }
    public static String vibNote()      { return s("Erişilebilirlik servisi aktif olmalı", "Accessibility service must be active"); }
    public static String longPress()    { return s("Long press süresi", "Long press duration"); }
    public static String returnDelay()  { return s("Geri dönüş bekleme", "Return delay"); }
    public static String fadeDelay()    { return s("Soluklaşma gecikmesi", "Fade delay"); }
    public static String posLock()      { return s("Konum kilidi", "Position lock"); }
    public static String ghostMode()    { return s("Ghost mod (beklemede soluklaş)", "Ghost mode (idle fade)"); }
    public static String autoHide()     { return s("Otomatik Gizle", "Auto Hide"); }
    public static String selectApp()    { return s("Uygulama seç...", "Select app..."); }
    public static String appsSelected() { return s(" uygulama seçili", " apps selected"); }
    public static String pulseAnim()    { return s("Nabız animasyonu", "Pulse animation"); }
    public static String actionFlash()  { return s("Aksiyon anında parla", "Flash on action"); }
    public static String color()        { return s("Renk", "Color"); }
    public static String activeProfile(){ return s("Aktif profil", "Active profile"); }
    public static String profileName()  { return s("Profil adı", "Profile name"); }
    public static String newProfile()   { return s("Yeni Profil", "New Profile"); }
    public static String hideApps()     { return s("Gizlenecek Uygulamalar", "Apps to Hide"); }
    public static String drawInfo()     { return s("Parmakla L veya Z çizerek aksiyon tetikle", "Draw L or Z with finger to trigger action"); }
    public static String lShape()       { return s("L şekli aksiyonu", "L shape action"); }
    public static String zShape()       { return s("Z şekli aksiyonu", "Z shape action"); }
    public static String assistantInfo(){ return s("Butonu uzun basılı tutunca asistan başlatılır", "Hold button to launch assistant"); }
    public static String assistantApp() { return s("Asistan uygulaması", "Assistant app"); }
    public static String assistantHold(){ return s("Basılı tutma süresi", "Hold duration"); }
    public static String lowBattAlert() { return s("Düşük pil uyarısı", "Low battery alert"); }
    public static String battThreshold(){ return s("Uyarı eşiği (%)", "Alert threshold (%)"); }
    public static String kbShrink()     { return s("Klavye açıkken küçül", "Shrink when keyboard opens"); }
    public static String kbSize()       { return s("Klavye modunda boyut", "Size in keyboard mode"); }
    public static String notifBadgeToggle(){ return s("Bildirim rozeti göster", "Show notification badge"); }
    public static String transparencyToggle(){ return s("Gizlilik kısayolu aktif", "Privacy shortcut active"); }
    public static String transparencyDur(){ return s("Gizlilik süresi", "Privacy duration"); }
    public static String compatToggle()  { return s("Uyumlu çalışma modunu etkinleştir", "Enable compatibility mode"); }
    public static String compatInfo()    { return s(
        "Aktifken Play Store, bankacılık ve ödeme uygulamaları açıldığında TouchNav kendini tamamen kaldırır, sorun yaşamazsınız.",
        "When active, TouchNav fully hides itself when Play Store, banking or payment apps open — no more freezing or blocking."); }
    public static String language()     { return s("Dil / Language", "Language / Dil"); }

    // ── Hareket etiketleri ───────────────────────────────────────
    public static String singleTap()   { return s("Tek tap", "Single tap"); }
    public static String doubleTap()   { return s("Çift tap", "Double tap"); }
    public static String swipeRight()  { return s("Sağa kaydır", "Swipe right"); }
    public static String swipeLeft()   { return s("Sola kaydır", "Swipe left"); }
    public static String swipeUp()     { return s("Yukarı kaydır", "Swipe up"); }
    public static String swipeDown()   { return s("Aşağı kaydır", "Swipe down"); }

    // ── Aksiyon isimleri ─────────────────────────────────────────
    public static String[] actionLabels() {
        return new String[]{
            s("Geri", "Back"),
            s("İleri", "Forward"),
            s("Ana Ekran", "Home"),
            s("Son Uygulamalar", "Recents"),
            s("Bildirimler", "Notifications"),
            s("Uygulama Çekmecesi", "App Drawer"),
            s("Hiçbir şey", "None"),
            s("Ekran Kaydı", "Screen Record"),
            s("Gizlilik", "Privacy"),
            s("Asistan", "Assistant"),
            s("El Feneri", "Torch")
        };
    }

    // ── Stil isimleri ─────────────────────────────────────────────
    public static String[] styleNames() {
        return new String[]{"Ghost","Shadow","Minimal","Neon","Plasma","Halo","Material","Glow","Beacon","Ripple","Mist","Ember","Whisper"};
    }

    // ── Şekil isimleri ────────────────────────────────────────────
    public static String[] shapeNames() {
        return new String[]{
            s("Daire", "Circle"),
            s("Kare", "Square"),
            s("Damla", "Drop"),
            s("Altıgen", "Hex")
        };
    }

    // ── İkon isimleri ─────────────────────────────────────────────
    public static String[] iconNames() {
        return new String[]{
            s("Yok", "None"),
            s("Nokta", "Dot"),
            s("Pusula", "Nav"),
            s("Halka", "Ring")
        };
    }

    // ── Tema isimleri ─────────────────────────────────────────────
    public static String[] themeNames() {
        return new String[]{"Midnight", s("Açık", "Light"), "AMOLED"};
    }

    // ── Titreşim seçenekleri ──────────────────────────────────────
    public static String[] strengthLabels() {
        return new String[]{s("Hafif","Soft"), s("Orta","Medium"), s("Güçlü","Strong")};
    }
    public static String[] durationLabels() {
        return new String[]{s("Kısa","Short"), s("Orta","Medium"), s("Uzun","Long")};
    }

    // ── Onboarding ────────────────────────────────────────────────
    public static String ob1Title() { return s("TouchNav'a Hoş Geldin!", "Welcome to TouchNav!"); }
    public static String ob1Sub()   { return s("Ekranında her zaman ulaşabileceğin\nyüzen bir navigasyon butonu.", "A floating navigation button\nalways within reach on your screen."); }
    public static String ob2Title() { return s("Overlay İzni", "Overlay Permission"); }
    public static String ob2Sub()   { return s("TouchNav'ın diğer uygulamaların\nüzerinde görünmesi için izin ver.", "Allow TouchNav to appear\nover other apps."); }
    public static String ob2Btn()   { return s("İzin Ver", "Grant Permission"); }
    public static String ob3Title() { return s("Erişilebilirlik", "Accessibility"); }
    public static String ob3Sub()   { return s("Geri / Ana Ekran gibi sistem\ntuşlarını kullanmak için erişilebilirliği aç.", "Enable accessibility to use\nBack, Home and system actions."); }
    public static String ob3Btn()   { return s("Erişilebilirliği Aç", "Enable Accessibility"); }
    public static String ob4Title() { return s("Hazırsın! 🎉", "All Set! 🎉"); }
    public static String ob4Sub()   { return s("Butonu başlat, ekranında sürükle\nve aksiyonları özelleştir.", "Start the button, drag it anywhere\nand customize your actions."); }
    public static String ob4Btn()   { return s("Başla!", "Let's Go!"); }
    public static String next()     { return s("İleri →", "Next →"); }
    public static String skip()     { return s("Geç", "Skip"); }
}
