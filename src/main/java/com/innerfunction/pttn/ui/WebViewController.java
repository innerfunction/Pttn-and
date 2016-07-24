package com.innerfunction.pttn.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.R;
import com.innerfunction.pttn.app.AppContainer;
import com.innerfunction.pttn.app.ViewController;
import com.innerfunction.uri.FileResource;
import com.innerfunction.uri.Resource;

import java.lang.reflect.Field;

/**
 * A view for displaying web content.
 *
 * Attached by juliangoacher on 19/05/16.
 */
public class WebViewController extends ViewController {

    static final String Tag = WebViewController.class.getSimpleName();

    /** An image view to display when the web view first loads. */
    private ImageView loadingImageView;
    /** If true then show a spinner whenever a page loads. */
    private boolean showLoadingSpinner;
    /** The page loading spinner view. */
    private View loadingSpinner;
    /** An image to be displayed whilst the web view is loading. */
    private Drawable loadingImage;
    /** Flag indicating whether to use the HTML page's title as the view title. */
    private boolean useHTMLTitle;
    /** The native web view. */
    protected WebView webView;
    /** Flag indicating that external links should be opened within the webview. */
    private boolean loadExternalLinks = false;
    /** Flag indicating whether the web view page is loaded. */
    private boolean webViewLoaded = false;
    /** The web view's vertical scroll offset. */
    private int scrollOffset = -1;
    /** The fragment's layout. */
    private FrameLayout layout;
    /** The view's content. */
    private Object content;
    /** The view content's base URL; or an external URL to load data from. */
    private String contentURL;
    /** Flag indicating whether content has been loaded into the web view. */
    private boolean contentLoaded = false;

    public WebViewController(Context context) {
        super( context );
        setLayout("web_view_layout");
    }

    public void setShowLoadingSpinner(boolean showLoadingSpinner) {
        this.showLoadingSpinner = showLoadingSpinner;
    }

    public void setLoadingImage(Drawable loadingImage) {
        this.loadingImage = loadingImage;
    }

    public void setUseHTMLTitle(boolean useHTMLTitle) {
        this.useHTMLTitle = useHTMLTitle;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public void setContentURL(String contentURL) {
        this.contentURL = contentURL;
    }

    @Override
    public View onCreateView(Activity activity) {
        this.layout = (FrameLayout)super.onCreateView( activity );

        /* According to http://code.google.com/p/android/issues/detail?id=9375 creating a web view
         * through the xml layout causes a memory leak; so instead, create as follows using the
         * application context and insert into layout by replacing placeholder view.
         */
        webView = new NonLeakingWebView( activity );
        layoutManager.replaceView("webview", webView );

        loadingSpinner = layout.findViewById( R.id.loadingSpinner );

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled( true );
        webSettings.setDomStorageEnabled( true );

        WebViewClient webViewClient = new DefaultWebViewClient();
        webView.setWebViewClient( webViewClient );

        // Set WebChromeClient for console.log
        webView.addJavascriptInterface( new Console(), "console");

        if( loadingImage != null ) {
            loadingImageView = new ImageView( activity );
            loadingImageView.setScaleType( ImageView.ScaleType.CENTER );
            loadingImageView.setLayoutParams( webView.getLayoutParams() );
            loadingImageView.setImageDrawable( loadingImage );
            layout.addView( loadingImageView );
        }

        if( showLoadingSpinner ) {
            showLoadingSpinnerView();
        }

        webView.setBackgroundColor( getBackgroundColor() );

        return layout;
    }

    public void hideLoadingImageView() {
        if( loadingImageView != null ) {
            layout.removeView( loadingImageView );
        }
    }

    public void showLoadingSpinnerView() {
        loadingSpinner.setVisibility( View.VISIBLE );
    }

    public void hideLoadingSpinnerView() {
        loadingSpinner.setVisibility( View.INVISIBLE );
    }

    private void loadContent() {
        if( !contentLoaded ) {
            // Specified content takes precedence over a contentURL property. Note that contentURL
            // can still be used to specify the content base URL in those cases where it can't
            // otherwise be determined.
            if( content != null ) {
                if( content instanceof FileResource ) {
                    FileResource fileResource = (FileResource)content;
                    String html = fileResource.asString();
                    // Note that a file resource can specify the base URL.
                    String baseURL = fileResource.asURL().toString();
                    webView.loadDataWithBaseURL( baseURL, html, "text/html", "utf-8", null );
                }
                else if( content instanceof Resource ) {
                    Resource resource = (Resource)content;
                    String html = resource.asString();
                    webView.loadDataWithBaseURL( contentURL, html, "text/html", "utf-8", null );
                }
                else {
                    // Assume content's description will yield valid HTML.
                    String html = content.toString();
                    webView.loadDataWithBaseURL( contentURL, html, "text/html", "utf-8", null );
                }
            }
            else if( contentURL != null ) {
                webView.loadUrl( contentURL );
            }
            contentLoaded = true;
        }
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("load") ) {
            content = message.getParameter("content");
            contentLoaded = false;
            loadContent();
            return true;
        }
        return super.receiveMessage( message, sender );
    }

    @Override
    public void onStart() {
        super.onStart();
        loadContent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loadingImageView = null;
        loadingSpinner = null;
        // An Android bug means that a web view created through a layout causes a memory leak,
        // see http://stackoverflow.com/a/19391512
        webView.removeJavascriptInterface("console");
        webView.removeJavascriptInterface("app");
        webView.removeAllViews();
        webView.destroy();
        webView = null;
    }

    /** Javascript console. */
    private class Console {
        private static final String TAG = "[WebView]";
        @JavascriptInterface
        public void log(String msg) {
            Log.i( TAG, msg );
        }
    }

    private class DefaultWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
            int idx = url.indexOf(':');
            String scheme = idx > 0 ? url.substring( 0, idx ) : "";
            if( "file".equals( scheme ) ) {
                // Let the web view handle file: scheme URLs.
                return false;
            }
            AppContainer app = AppContainer.getAppContainer();
            if( app.isInternalURISchemeName( scheme ) ) {
                app.postMessage( url, WebViewController.this );
                return true;
            }
            else if( loadExternalLinks ) {
                return false;
            }
            else {
                // All other URLs are handled by the system.
                app.openURL( url );
                return true;
            }
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            webViewLoaded = false;
            showLoadingSpinnerView();
            super.onPageStarted( view, url, favicon );
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            hideLoadingImageView();
            hideLoadingSpinnerView();
            super.onPageFinished( view, url );
            webViewLoaded = true;
            if( scrollOffset > 0 ) {
                webView.postDelayed(new Runnable() {
                    public void run() {
                        webView.scrollTo( 0, scrollOffset );
                        scrollOffset = -1;
                    }
                }, 0 );
            }
            contentLoaded = true;
            if( useHTMLTitle ) {
                setTitle( view.getTitle() );
            }
        }
    }

    /**
     * A well behaved web view class.
     * see http://stackoverflow.com/questions/3130654/memory-leak-in-webview
     * and http://code.google.com/p/android/issues/detail?id=9375
     * "Note that the bug does NOT appear to be fixed in android 2.2 as romain claims.
     *  Also, you must call {@link #destroy()} from your activity's onDestroy method."
     */
    private class NonLeakingWebView extends android.webkit.WebView {
        public NonLeakingWebView(Context context) {
            super( context.getApplicationContext() );
        }
        @Override
        public void destroy() {
            super.destroy();
            try {
                Class frameClass = Class.forName("android.webkit.BrowserFrame");
                Field sConfigCallback = frameClass.getDeclaredField("sConfigCallback");
                sConfigCallback.setAccessible( true );
                sConfigCallback.set( null, null );
            }
            catch (Exception e) {
            }
        }
    }

}
