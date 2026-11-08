package com.allsender.app;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import java.io.IOException;
import java.security.SecureRandom;

/** Mantém o servidor ativo em segundo plano (Foreground Service). */
public class ShareService extends Service {
    private static final String CH = "share";
    private WebServer server;
    private WifiManager.WifiLock lock;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && "stop".equals(i.getAction())) { stopSelf(); return START_NOT_STICKY; }
        Notification n = notif("A iniciar…");
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(1, n);
        if (server == null) startServer();
        return START_NOT_STICKY;
    }

    @SuppressWarnings("deprecation")
    private void startServer() {
        String ip = NetUtil.localIp();
        if (ip == null) { stopSelf(); return; }
        String token = token();
        for (int port = 8080; port < 8100 && server == null; port++) {
            try {
                WebServer s = new WebServer(this, port, token);
                s.start(30000, false);
                server = s;
                ShareState.url = "http://" + ip + ":" + port + "/" + token + "/";
            } catch (IOException ignored) { }
        }
        if (server == null) { stopSelf(); return; }
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null) {
            lock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "allsender");
            lock.acquire();
        }
        getSystemService(NotificationManager.class).notify(1, notif(ShareState.url));
    }

    private static String token() {
        String a = "abcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(a.charAt(r.nextInt(a.length())));
        return sb.toString();
    }

    private Notification notif(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "AllSender", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, ShareService.class).setAction("stop"), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("AllSender — partilha ativa").setContentText(text)
                .setContentIntent(open).setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar", stop).build();
    }

    @Override
    public void onDestroy() {
        if (server != null) { server.stop(); server = null; }
        if (lock != null && lock.isHeld()) lock.release();
        ShareState.url = null;
        super.onDestroy();
    }
}
