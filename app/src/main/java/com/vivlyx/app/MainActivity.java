
package com.vivlyx.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient.FileChooserParams;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private static final int FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    String[] types = params.getAcceptTypes();
                    java.util.ArrayList<String> mimeTypes =
                            new java.util.ArrayList<>();

                    if (types != null) {
                        for (String type : types) {
                            if (type != null && !type.trim().isEmpty()) {
                                mimeTypes.add(type);
                            }
                        }
                    }

                    if (mimeTypes.size() == 1) {
                        intent.setType(mimeTypes.get(0));
                    } else {
                        intent.setType("*/*");

                        if (!mimeTypes.isEmpty()) {
                            intent.putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                mimeTypes.toArray(new String[0])
                            );
                        }
                    }

                    intent.putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        params.getMode() ==
                        FileChooserParams.MODE_OPEN_MULTIPLE
                    );

                    startActivityForResult(
                        Intent.createChooser(intent, "Select Photo or Video"),
                        FILE_CHOOSER_REQUEST
                    );

                    return true;

                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });

        webView.loadUrl("https://vivlyx.xyz");
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {

            if (fileCallback == null) {
                return;
            }

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {

                if (data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];

                    for (int i = 0; i < count; i++) {
                        results[i] =
                            data.getClipData().getItemAt(i).getUri();
                    }

                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }

            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
