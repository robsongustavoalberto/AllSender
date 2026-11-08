package com.allsender.app;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.net.Uri;
import fi.iki.elonen.NanoHTTPD;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Servidor HTTP: / (página), /files, /d/N (download), /up (upload). Tudo sob /TOKEN/. */
class WebServer extends NanoHTTPD {
    private final Context ctx;
    private final String prefix;
    private String page;

    WebServer(Context ctx, int port, String token) {
        super(port);
        this.ctx = ctx.getApplicationContext();
        this.prefix = "/" + token;
    }

    @Override
    public Response serve(IHTTPSession s) {
        String uri = s.getUri();
        if (uri.equals(prefix)) {
            Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_PLAINTEXT, "");
            r.addHeader("Location", prefix + "/");
            return r;
        }
        if (!uri.startsWith(prefix + "/")) return text(Response.Status.FORBIDDEN, "403");
        String path = uri.substring(prefix.length() + 1);
        try {
            if (path.isEmpty()) return html();
            if (path.equals("files")) return files();
            if (path.startsWith("d/")) return download(Integer.parseInt(path.substring(2)));
            if (path.equals("up") && s.getMethod() == Method.POST) return upload(s);
        } catch (Exception e) {
            return text(Response.Status.INTERNAL_ERROR, "Erro: " + e.getMessage());
        }
        return text(Response.Status.NOT_FOUND, "404");
    }

    private Response text(Response.IStatus st, String t) {
        return newFixedLengthResponse(st, MIME_PLAINTEXT, t);
    }

    private Response html() throws IOException {
        if (page == null) {
            try (InputStream in = ctx.getAssets().open("web/index.html")) {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                byte[] b = new byte[8192]; int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
                page = o.toString("UTF-8");
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", page);
    }

    private Response files() throws Exception {
        List<ShareState.Item> copy = new ArrayList<>(ShareState.items);
        JSONArray arr = new JSONArray();
        for (int i = 0; i < copy.size(); i++) {
            JSONObject o = new JSONObject();
            o.put("id", i); o.put("name", copy.get(i).name); o.put("size", copy.get(i).size);
            arr.put(o);
        }
        Response r = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", arr.toString());
        r.addHeader("Cache-Control", "no-store");
        return r;
    }

    private Response download(int id) throws Exception {
        List<ShareState.Item> copy = new ArrayList<>(ShareState.items);
        if (id < 0 || id >= copy.size()) return text(Response.Status.NOT_FOUND, "404");
        ShareState.Item it = copy.get(id);
        InputStream in = ctx.getContentResolver().openInputStream(it.uri);
        if (in == null) return text(Response.Status.NOT_FOUND, "404");
        String mime = ctx.getContentResolver().getType(it.uri);
        if (mime == null) mime = "application/octet-stream";
        Response r = it.size >= 0 ? newFixedLengthResponse(Response.Status.OK, mime, in, it.size)
                                  : newChunkedResponse(Response.Status.OK, mime, in);
        r.addHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(it.name, "UTF-8").replace("+", "%20"));
        return r;
    }

    private Response upload(IHTTPSession s) throws Exception {
        Map<String, String> tmp = new HashMap<>();
        s.parseBody(tmp);
        String path = tmp.get("file");
        String name = s.getParms().get("file");
        if (path == null || name == null) return text(Response.Status.BAD_REQUEST, "400");
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("^\\.+", "");
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty()) name = "ficheiro";
        save(path, name);
        return text(Response.Status.OK, "ok");
    }

    /** Guarda em Downloads/AllSender (Android 10+) ou na pasta da app (Android 8-9). */
    private void save(String tmpPath, String name) throws IOException {
        OutputStream out;
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AllSender");
            Uri u = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IOException("MediaStore");
            out = ctx.getContentResolver().openOutputStream(u);
        } else {
            File d = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AllSender");
            d.mkdirs();
            File f = new File(d, name);
            if (f.exists()) f = new File(d, System.currentTimeMillis() + "_" + name);
            out = new FileOutputStream(f);
        }
        try (InputStream in = new FileInputStream(tmpPath); OutputStream o = out) {
            byte[] b = new byte[64 * 1024]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
        }
    }
}
