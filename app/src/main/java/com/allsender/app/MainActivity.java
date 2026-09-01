package com.allsender.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inicializa o motor Python (Chaquopy)
        initPython();

        // 2. Configura a WebView da aplicação
        webView = new WebView(this);
        setContentView(webView);

        configureWebView();
        
        // 3. Solicita as permissões do sistema
        checkAndRequestPermissions();

        // 4. Carrega o front-end HTML/CSS/JS local
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    private void configureWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // Define a ponte JavaScript -> Android/Python
        webView.addJavascriptInterface(new WebAppInterface(), "pythonBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Mantém a navegação dentro da WebView
                return false;
            }
        });
    }

    /**
     * Interface que conecta o JavaScript (app.js) ao servidor Python (server.py)
     */
    public class WebAppInterface {

        @JavascriptInterface
        public String startServer(String filePathsJson) {
            try {
                Python py = Python.getInstance();
                PyObject serverModule = py.getModule("server");

                // Converte a string JSON de caminhos vinda do JS em lista Python
                PyObject jsonModule = py.getModule("json");
                PyObject pyList = jsonModule.callAttr("loads", filePathsJson);

                // Executa a função start_server(file_paths) do server.py
                PyObject result = serverModule.callAttr("start_server", pyList);
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
            }
        }

        @JavascriptInterface
        public String stopServer() {
            try {
                Python py = Python.getInstance();
                PyObject serverModule = py.getModule("server");
                PyObject result = serverModule.callAttr("stop_server");
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
            }
        }
    }

    /**
     * Gestão de Permissões de Armazenamento e Rede em tempo de execução
     */
    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissão necessária para selecionar ficheiros.", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Encerra o servidor Python quando a app é fechada
        try {
            Python py = Python.getInstance();
            PyObject serverModule = py.getModule("server");
            serverModule.callAttr("stop_server");
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
