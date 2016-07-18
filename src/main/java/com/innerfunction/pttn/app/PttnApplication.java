package com.innerfunction.pttn.app;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

/**
 * Standard Pttn application class.
 *
 * Created by juliangoacher on 12/07/16.
 */
public class PttnApplication extends Application {

    static final String Tag = PttnApplication.class.getSimpleName();

    static final boolean TraceEnabled = false;

    /**
     * A URI specifying the location of the app container configuration.
     * Defaults to a path resolving to the file at assets/pttn/config.json.
     */
    private String configurationURI = "app:/pttn/config.json";
    /** A container for all of the app's components. */
    private AppContainer appContainer;

    public PttnApplication() {}

    /**
     * Create a new application instance using the specified configuration URI.
     * Subclasses can use this constructor to specify an alternative location for the app
     * configuration.
     *
     * @param configurationURI An internal URI resolving to the app configuration.
     */
    public PttnApplication(String configurationURI) {
        this.configurationURI = configurationURI;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Enable debugging of webviews via chrome.
            // Taken from https://developer.chrome.com/devtools/docs/remote-debugging#debugging-webviews
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                if( 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) ) {
                    WebView.setWebContentsDebuggingEnabled( true );
                }
            }
            // Configure and start the app container.
            this.appContainer = AppContainer.getAppContainer( getApplicationContext() );
            if( TraceEnabled) {
                android.os.Debug.startMethodTracing("semo-trace");
            }
            appContainer.loadConfiguration( configurationURI );
            if( TraceEnabled ) {
                android.os.Debug.stopMethodTracing();
            }
            appContainer.startService();
        }
        catch(Exception e) {
            Log.e(Tag, "Application startup failure", e );
        }
    }
}
