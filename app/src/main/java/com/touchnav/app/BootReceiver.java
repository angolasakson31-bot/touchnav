package com.touchnav.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // Sadece kullanıcı servisi gerçekten açık bıraktıysa VE overlay izni
        // hâlâ varsa başlat. Aksi halde açılışta düğmesiz "zombi" bir
        // ön plan servisi başlatıp bildirimde takılı kalırdık.
        SettingsManager settings = new SettingsManager(context);
        if (!settings.isServiceEnabled()) return;
        if (!Settings.canDrawOverlays(context)) return;

        Intent service = new Intent(context, FloatingService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Exception ignored) {}
    }
}