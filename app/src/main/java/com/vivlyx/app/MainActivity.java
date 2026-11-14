
package com.vivlyx.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView errorView;
    private ValueCallback<Uri[]> fileCallback;

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final String WEBSITE = "https://vivlyx.xyz";

    private void showMessage(String message) {
        runOnUiThread(() -> {
            if (errorView != null) {
                errorView.setText(message);
                errorView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout layout = new FrameLayout(this);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);

        errorView = new TextView(this);
        errorView.setTextColor(Color.BLACK);
        errorView.setTextSize(16);
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(30, 30, 30, 30);
        errorView.setBackgroundColor(Color.WHITE);
        errorView.setText("Connecting to Vivlyx...");

        layout.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        layout.addView(errorView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(layout);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(
                    WebView view, String url, android.graphics.Bitmap favicon) {
                showMessage("Connecting to Vivlyx...");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (errorView != null) {
                    errorView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                if (request.isForMainFrame()) {
                    showMessage(
                        "Vivlyx could not load.\n\nError: "
                        + error.getErrorCode()
                        + "\n"
                        + error.getDescription()
                        + "\n\nPlease check your internet connection."
                    );
                }
            }

            @Override
            public void onReceivedHttpError(
                    WebView view,
                    WebResourceRequest request,
                    android.webkit.WebResourceResponse response) {

                if (request.isForMainFrame()) {
                    showMessage(
                        "Website loading failed.\n\nHTTP Status: "
                        + response.getStatusCode()
                        + "\n\nPlease contact the website administrator."
                    );
                }
            }

            @Override
            public void onReceivedSslError(
                    WebView view,
                    SslErrorHandler handler,
                    SslError error) {

                handler.cancel();

                showMessage(
                    "Secure connection failed.\n\n"
                    + error.toString()
                );
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(
                    android.webkit.ConsoleMessage message) {

                if (message.messageLevel() ==
                        android.webkit.ConsoleMessage.MessageLevel.ERROR) {

                    showMessage(
                        "Website JavaScript Error:\n\n"
                        + message.message()
                        + "\n\nLine: "
                        + message.lineNumber()
                    );
                }

                return true;
            }

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
                    showMessage("File picker error: " + e.getMessage());
                    return true;
                }
            }
        });

        webView.loadUrl(WEBSITE);
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

            ValueCallback<Uri[]> callback = fileCallback;
            fileCallback = null;
            callback.onReceiveValue(results);
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
