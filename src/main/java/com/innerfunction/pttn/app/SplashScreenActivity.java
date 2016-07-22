package com.innerfunction.pttn.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.innerfunction.pttn.R;

/**
 * A basic activity type for displaying a splashscreen before the main app UI loads.
 *
 * Created by juliangoacher on 12/07/16.
 */
public class SplashScreenActivity extends Activity {

    static final String Tag = SplashScreenActivity.class.getSimpleName();

    /**
     * The minimum time, in milliseconds, for which the splash screen should be displayed.
     * The app's main root view will be displayed once this time has elapsed.
     * This value can be configured within the application declaration in the app manifest by using
     * a meta-data tag with a name of 'splashScreenDelay', e.g.:
     *
     *  <meta-data android:name="splashScreenDelay" android:value="1000" />
     *
     */
    private int splashDelay = 2000;
    /**
     * The splash-screen layout ID..
     * Defaults to R.layout.splashscreen_layout. Can be configured within the application
     * declaration in the app manifesst by using a meta-data tag with a name of
     * 'splashScreenLayout', e.g:
     *
     *  <meta-data android:name="splashScreenLayout" android:resource="@R.layout.xxx" />
     *
     */
    private int splashScreenLayout = R.layout.splashscreen_layout;

    public void setSplashDelay(int delay) {
        this.splashDelay = delay;
    }

    public int getSplashDelay() {
        return splashDelay;
    }

    public void setSplashScreenLayout(int layout) {
        this.splashScreenLayout = layout;
    }

    public int getSplashScreenLayout() {
        return splashScreenLayout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        ManifestMetaData.applyTo( this );

        setContentView( splashScreenLayout );

        // Create a task to display the app's root view after the splash screen.
        Runnable task = new Runnable() {
            @Override
            public void run() {
                AppContainer appContainer = AppContainer.getAppContainer();
                if( appContainer != null && appContainer.isRunning() ) {
                    appContainer.showRootView();
                }
                else {
                    // App container not fully started yet, reschedule the task to try again after
                    // a small additional delay.
                    new Handler().postDelayed( this, 250 );
                }
            }
        };
        // Schedule the task to run.
        new Handler().postDelayed( task, (long)splashDelay );
    }
}
