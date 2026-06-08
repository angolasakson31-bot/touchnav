package com.touchnav.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class TouchNavWidget extends AppWidgetProvider {

    private static final String ACTION_TOGGLE = "com.touchnav.WIDGET_TOGGLE";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            SettingsManager settings = new SettingsManager(ctx);
            if (FloatingService.isRunning()) {
                settings.setServiceEnabled(false);
                ctx.stopService(new Intent(ctx, FloatingService.class));
            } else {
                settings.setServiceEnabled(true);
                Intent svc = new Intent(ctx, FloatingService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ctx.startForegroundService(svc);
                else
                    ctx.startService(svc);
            }
            // Widget'ı kısa gecikmeyle yenile (servis başlamış olsun)
            new android.os.Handler().postDelayed(() -> {
                AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
                int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, TouchNavWidget.class));
                for (int id : ids) updateWidget(ctx, mgr, id);
            }, 500);
        }
    }

    private static void updateWidget(Context ctx, AppWidgetManager mgr, int id) {
        boolean running = FloatingService.isRunning();
        L.init(ctx);

        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_layout);

        // Durum metni ve rengi
        views.setTextViewText(R.id.widget_status_text,
            running ? (L.isTr() ? "Aktif ●" : "Active ●")
                    : (L.isTr() ? "Kapalı ○" : "Off ○"));
        views.setTextColor(R.id.widget_status_text,
            running ? 0xFF4CAF50 : 0xFFFF5252);

        // Buton metni
        views.setTextViewText(R.id.widget_btn_text,
            running ? (L.isTr() ? "⏹ Durdur" : "⏹ Stop")
                    : (L.isTr() ? "▶ Başlat" : "▶ Start"));

        // Buton arka plan rengi
        views.setInt(R.id.widget_btn_bg, "setBackgroundColor",
            running ? 0xFF1B3A1B : 0xFF1A2A3A);

        // Toggle tıklama
        Intent toggle = new Intent(ctx, TouchNavWidget.class);
        toggle.setAction(ACTION_TOGGLE);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, toggle,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_bg, pi);

        // Ayarlar tıklama
        Intent settingsI = new Intent(ctx, MainActivity.class);
        settingsI.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent settingsPi = PendingIntent.getActivity(ctx, 1, settingsI,
            PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_settings_btn, settingsPi);

        mgr.updateAppWidget(id, views);
    }
}
