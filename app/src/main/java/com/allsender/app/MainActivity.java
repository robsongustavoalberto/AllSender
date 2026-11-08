package com.allsender.app;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class MainActivity extends Activity {
    private static final int REQ_PICK = 1;
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView filesView, urlView, hint;
    private Button start, share, copy;
    private ImageView qr;
    private String shownQr;

    private final Runnable tick = new Runnable() {
        @Override public void run() { render(); h.postDelayed(this, 500); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF0F172A);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(36), dp(20), dp(24));
        sv.addView(root);
        setContentView(sv);

        add(root, tv("AllSender", 30, true));
        add(root, tv("Partilha de ficheiros por Wi-Fi ou Hotspot, sem internet.", 14, false));
        add(root, btn("1. Escolher ficheiros", v -> pick()));
        filesView = tv("", 14, false); add(root, filesView);
        add(root, btn("Limpar lista", v -> { ShareState.items.clear(); render(); }));
        start = btn("2. Iniciar partilha", v -> toggle()); add(root, start);

        qr = new ImageView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(240), dp(240));
        lp.gravity = Gravity.CENTER_HORIZONTAL; lp.topMargin = dp(16);
        root.addView(qr, lp);
        urlView = tv("", 16, true); urlView.setTextIsSelectable(true); urlView.setGravity(Gravity.CENTER);
        add(root, urlView);
        copy = btn("Copiar link", v -> {
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("AllSender", ShareState.url));
            Toast.makeText(this, "Link copiado", Toast.LENGTH_SHORT).show();
        });
        add(root, copy);
        share = btn("Partilhar link", v -> {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ShareState.url);
            startActivity(Intent.createChooser(i, "Partilhar link"));
        });
        add(root, share);
        hint = tv("Noutro telemóvel ou PC ligado à mesma rede (ou ao seu Hotspot), leia o QR ou abra o link "
                + "no browser. A página permite descarregar ficheiros e também enviar ficheiros para este "
                + "telemóvel (ficam em Downloads/AllSender).", 13, false);
        add(root, hint);
    }

    @Override protected void onResume() { super.onResume(); h.post(tick); }
    @Override protected void onPause() { h.removeCallbacks(tick); super.onPause(); }

    private void pick() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req != REQ_PICK || res != RESULT_OK || d == null) return;
        if (d.getClipData() != null)
            for (int i = 0; i < d.getClipData().getItemCount(); i++) addUri(d.getClipData().getItemAt(i).getUri());
        else if (d.getData() != null) addUri(d.getData());
        render();
    }

    private void addUri(Uri u) {
        synchronized (ShareState.items) {
            for (ShareState.Item it : ShareState.items) if (it.uri.equals(u)) return;
        }
        try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignored) { }
        String name = "ficheiro"; long size = -1;
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME), s = c.getColumnIndex(OpenableColumns.SIZE);
                if (n >= 0) name = c.getString(n);
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s);
            }
        }
        ShareState.items.add(new ShareState.Item(u, name, size));
    }

    private void toggle() {
        if (ShareState.url != null) { stopService(new Intent(this, ShareService.class)); ShareState.url = null; render(); return; }
        if (NetUtil.localIp() == null) {
            Toast.makeText(this, "Ligue-se a um Wi-Fi ou ative o Hotspot.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
        startForegroundService(new Intent(this, ShareService.class));
    }

    private void render() {
        String u = ShareState.url; boolean on = u != null;
        start.setText(on ? "Parar partilha" : "2. Iniciar partilha");
        StringBuilder sb = new StringBuilder();
        synchronized (ShareState.items) {
            if (ShareState.items.isEmpty()) sb.append("Nenhum ficheiro escolhido (pode apenas receber).");
            for (ShareState.Item it : ShareState.items)
                sb.append("• ").append(it.name).append(" (")
                  .append(it.size >= 0 ? Formatter.formatShortFileSize(this, it.size) : "?").append(")\n");
        }
        filesView.setText(sb.toString().trim());
        int vis = on ? View.VISIBLE : View.GONE;
        for (View v : new View[]{qr, urlView, copy, share, hint}) v.setVisibility(vis);
        if (on) {
            urlView.setText(u);
            if (!u.equals(shownQr)) { qr.setImageBitmap(makeQr(u)); shownQr = u; }
        } else shownQr = null;
    }

    private Bitmap makeQr(String text) {
        try {
            int n = 512;
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, n, n);
            int[] px = new int[n * n];
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) px[y * n + x] = m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            return Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888);
        } catch (Exception e) { return null; }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private TextView tv(String t, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(0xFFE2E8F0);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private Button btn(String t, View.OnClickListener l) {
        Button b = new Button(this); b.setText(t); b.setOnClickListener(l); return b;
    }

    private void add(LinearLayout root, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        root.addView(v, lp);
    }
}
