package cn.time24.kezhu;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class FullScreenActivity extends Activity {

    private WebView webView;
    private FrameLayout fullVideo;
    private View customView = null;
    private ProgressBar progressBar;
    private Intent data;

    private String currentUrl;
    private String paramUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉应用标题
        setContentView(R.layout.activity_fullscreen);
        webView=findViewById(R.id.webView);
        fullVideo=findViewById(R.id.full_video);
        progressBar=findViewById(R.id.progress);
        data = getIntent();
        paramUrl = data.getStringExtra("url");
        currentUrl = paramUrl;
        webView.loadUrl(currentUrl);
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.setWebViewClient(new MyWebViewClient());
        setWebView();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        data = getIntent();
        paramUrl = data.getStringExtra("url");
        if (!paramUrl.equals(currentUrl)) {
            webView.loadUrl(paramUrl);
            currentUrl = paramUrl;
        }
        super.onResume();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK://返回键
                doFinish();
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);//返回键的super处理的就是退出应用
    }

    public void doFinish() {
        Intent intent2 = new Intent(FullScreenActivity.this, MainActivity.class);
        startActivity(intent2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView!=null){
            webView.destroy();
        }
    }

    private void setWebView(){
       WebSettings webSettings = webView.getSettings();
       webSettings.setJavaScriptEnabled(true);
       //webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }

    private class MyWebChromeClient extends WebChromeClient{

        @Override
        public void onHideCustomView() {

            if (customView == null){
                return;
            }
            fullVideo.removeView(customView);
            fullVideo.setVisibility(View.GONE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//清除全屏

        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            customView = view;
            fullVideo.setVisibility(View.VISIBLE);
            fullVideo.addView(customView);
            fullVideo.bringToFront();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        }

    }


    private class MyWebViewClient extends WebViewClient{

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }
    }

}
