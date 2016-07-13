// Copyright 2016 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.pttn.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import com.innerfunction.pttn.Configuration;
import com.innerfunction.pttn.Container;
import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.MessageReceiver;
import com.innerfunction.pttn.MessageRouter;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.Resource;
import com.innerfunction.uri.StandardURIHandler;
import com.innerfunction.uri.URIScheme;
import com.innerfunction.uri.URIValueFormatter;
import com.innerfunction.util.I18nMap;
import com.innerfunction.util.UserDefaults;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * An IOC container encapsulating an app's UI and functionality.
 *
 * Created by juliangoacher on 30/03/16.
 */
public class AppContainer extends Container {

    static final String Tag = AppContainer.class.getSimpleName();

    /**
     * Global values available to the container's configuration. Can be referenced from within
     * templated configuration values.
     * Available values include the following:
     * - *platform*: Information about the container platform. Has the following values:
     *   - name: Always "ios" on iOS systems.
     *   - display: The display scale, e.g. 2x, 3x.
     * - *locale*: Information about the device's default locale. Has the following values:
     *   - id: The locale identifier, e.g. en_US
     *   - lang: The language code, e.g. en
     *   - variant: The locale variant, e.g. US
     */
    private Map<String,Object> globals;
    // TODO Local storage
    /** The app's default background colour. */
    private int appBackgroundColor;
    /**
     * A map of Android class configurations, keyed by fully qualified class name.
     * This provides a mechanism for performing IOC configuration of Android system class
     * instances, such as Activities and Receivers, which can't be directly instantiated by
     * the container.
     */
    private Map<String,Configuration> andSystemClassConfigs;
    /** Map of additional scheme configurations. */
    private Map<String,URIScheme> schemes;
    /** Make configurations. */
    private Configuration makes;
    /**
     * The currently active activity.
     * Normally corresponds to the currently visible activity within an app. However, if the app
     * opens an activity _external_ to the app container (i.e. an activity which doesn't implement
     * PttnActivity) then this will be null.
     */
    private PttnActivity currentActivity;
    /** A flag indicating that the container has fully started. */
    private boolean started = false;

    public AppContainer(Context context) {
        super( context, StandardURIHandler.getInstance( context ) );
        setPriorityNames("types","formats","schemes","aliases","makes");
    }

    public Map<String,URIScheme> getSchemes() {
        return schemes;
    }

    public void setSchemes(Map<String,URIScheme> schemes) {
        this.schemes = schemes;
    }

    public Configuration getMakes() {
        return makes;
    }

    public void setMakes(Configuration makes) {
        this.makes = makes;
    }

    public Map<String,URIValueFormatter> getFormats() {
        return uriHandler.getFormats();
    }

    public void setFormats(Map<String,URIValueFormatter> formats) {
        uriHandler.setFormats( formats );
    }

    public Map<String,String> getAliases() {
        return uriHandler.getAliases();
    }

    public void setAliases(Map<String,String> aliases) {
        uriHandler.setAliases( aliases );
    }

    /** Test if the container is fully started. */
    public boolean isStarted() {
        return started;
    }

    @Override
    public void startService() {
        super.startService();
        started = true;
    }

    /**
     * Load the app configuration.
     */
    public void loadConfiguration(Object configSource) {
        Configuration configuration = null;
        if( configSource instanceof Configuration ) {
            // Configuration source is already a configuration.
            configuration = (Configuration)configSource;
        }
        else {
            // Test if config source specifies a URI.
            CompoundURI uri = null;
            if( configSource instanceof CompoundURI ) {
                uri = (CompoundURI)configSource;
            }
            else if( configSource instanceof String ) {
                try {
                    uri = CompoundURI.parse( (String)configSource );
                }
                catch(URISyntaxException e) {
                    Log.e( Tag, "Error parsing app container configuration URI", e );
                    return;
                }
            }
            Object configData = null;
            if( uri != null ) {
                // If a configuration source URI has been resolved then attempt loading the
                // configuration from the URI.
                Log.i( Tag, String.format("Attempting to load app container configuration from %s", uri ) );
                configData = uriHandler.dereference( uri );
            }
            else {
                configData = configSource;
            }
            // Create configuration from data.
            if( configData instanceof Resource ) {
                configuration = makeConfiguration( configData );
                // Use the configuration's URI handler instead from this point on, to ensure
                // relative URI's resolve properly and also so that additional URI schemes added
                // to this container are available within the configuration.
                uriHandler = (StandardURIHandler)configuration.getURIHandler();
            }
            else {
                configuration = makeConfiguration( configSource );
            }
        }
        if( configuration != null ) {
            configureWith( configuration );
        }
        else {
            Log.w( Tag, String.format("Unable to resolve configuration from %s", configSource ) );
        }
    }

    @Override
    public void configureWith(Configuration configuration) {

        // Setup template context.
        this.globals = makeDefaultGlobalModelValues( configuration );
        configuration.setContext( globals );

        // Set object type mappings.
        addTypes( configuration.getValueAsConfiguration("types") );

        // Add additional schemes to the resolver/dispatcher.
        uriHandler.addHandlerForScheme("new", new NewScheme( this ) );
        uriHandler.addHandlerForScheme("make", new MakeScheme( this ) );
        uriHandler.addHandlerForScheme("named", new NamedScheme( this ) );
        uriHandler.addHandlerForScheme("post", new PostScheme() );

        // Default local settings.
        // TODO locals + settings

        nameds.put("uriHandler", uriHandler );
        nameds.put("globals", globals );
        //nameds.put("locals", locals );
        nameds.put("container", this );

        // Perform default container configuration.
        super.configureWith( configuration );

        // Map any additional schemes to the URI handler.
        for( String schemeName : schemes.keySet() ) {
            URIScheme scheme = schemes.get( schemeName );
            uriHandler.addHandlerForScheme( schemeName, scheme );
        }
    }

    /**
     * Make the set of global values.
     * @param configuration
     * @return
     */
    protected Map<String,Object> makeDefaultGlobalModelValues(Configuration configuration) {

        Resources r = androidContext.getResources();

        Map<String,Object> values = new HashMap<String,Object>();
        DisplayMetrics dm = r.getDisplayMetrics();
        String density;
        switch( dm.densityDpi ) {
        case DisplayMetrics.DENSITY_LOW:    density = "ldpi"; break;
        case DisplayMetrics.DENSITY_MEDIUM: density = "mdpi"; break;
        case DisplayMetrics.DENSITY_HIGH:   density = "hdpi"; break;
        case DisplayMetrics.DENSITY_XHIGH:  density = "xhdpi"; break;
        case DisplayMetrics.DENSITY_XXHIGH: density = "xxhdpi"; break;
        default:                            density = "hdpi";
        }
        Map<String,Object> platformValues = new HashMap<String,Object>();
        platformValues.put("name", "and");
        platformValues.put( "display", density );
        platformValues.put( "defaultDisplay", "hdpi");
        platformValues.put( "full", "and-"+density);
        values.put( "platform", platformValues );

        String mode = configuration.getValueAsString( "mode", "LIVE" );
        Log.i( Tag, String.format( "Configuration mode: %s", mode ) );
        values.put( "mode", mode );

        Locale locale = r.getConfiguration().locale;
        // The 'supportedLocales' setting can be used to declare a list of the locales that app assets are
        // available in. If the platform's default locale (above) isn't on this list then the code below
        // will attempt to find a supported locale that uses the same language; if no match is found then
        // the first locale on the list is used as the default.
        if( configuration.hasValue("supportedLocales") ) {
            @SuppressWarnings("unchecked")
            List<String> assetLocales = (List<String>)configuration.getValue("supportedLocales");
            if( assetLocales.size() > 0 && !assetLocales.contains( locale.toString() ) ) {
                // Attempt to find a matching locale.
                // Always assigns the first item on the list (as the default option); if a later
                // item has a matching language then that is assigned and the loop is exited.
                String lang = locale.getLanguage();
                boolean langMatch = false, assignDefault;
                for( int i = 0; i < assetLocales.size() && !langMatch; i++ ) {
                    String[] localeParts = assetLocales.get( 0 ).split("_");
                    assignDefault = (i == 0);
                    langMatch = localeParts[0].equals( lang );
                    if( assignDefault || langMatch ) {
                        switch( localeParts.length ) {
                        case 1: locale = new Locale( localeParts[0] ); break;
                        case 2: locale = new Locale( localeParts[0], localeParts[1] ); break;
                        case 3: locale = new Locale( localeParts[0], localeParts[1], localeParts[3] ); break;
                        default:
                            Log.w(Tag,String.format("Bad locale identifier: %s", assetLocales.get( 0 )));
                        }
                    }
                }
            }
        }

        Map<String,Object> localeValues = new HashMap<String,Object>();
        localeValues.put("id", locale.toString());
        localeValues.put( "lang", locale.getLanguage() );
        localeValues.put( "variant", locale.getVariant() );
        values.put( "locale", localeValues );

        // Access to localized resources through a Map interface.
        values.put("i18n", new I18nMap( androidContext ) );

        return values;
    }

    /**
     * Perform IOC configuration on an Android system class instance.
     * @param instance  An Android class instance.
     */
    public void configureAndroid(Object instance) {
        if( andSystemClassConfigs != null ) {
            String className = instance.getClass().getName();
            Configuration config = andSystemClassConfigs.get( className );
            if( config != null ) {
                configureObject( instance, config, className );
            }
        }
    }

    /**
     * Set the current activity.
     * Should be called by a PttnActivity instance from within its onResume method.
     */
    public void setCurrentActivity(PttnActivity activity) {
        currentActivity = activity;
    }

    /** Get the current activity. */
    public PttnActivity getCurrentActivity() {
        return currentActivity;
    }

    /**
     * Clear the current activity.
     * Called by the active activity when it is paused (i.e. as the user navigates away).
     */
    public void clearCurrentActivity(PttnActivity activity) {
        if( currentActivity == activity ) {
            currentActivity = null;
        }
    }

    /**
     * Get an object for reading and writing shared preferences.
     */
    public UserDefaults getUserDefaults() {
        return new UserDefaults( androidContext );
    }

    /**
     * Register a view to be displayed once a new activity has launched.
     * @return The UUID the view has been registered under.
     */
    public String registerPendingView(Object view) {
        String uuid = UUID.randomUUID().toString();
        globals.put( uuid, view );
        return uuid;
    }

    /**
     * Get a view instance by UUID.
     * Used by PttnActivity to fetch the activity's view after it has been launched.
     */
    public Object getViewForUUID(String uuid) {
        return globals.get( uuid );
    }

    /**
     * Remove a view instance, identified by its UUID.
     * Used in conjunction with getViewForUUID(...), and called by the activity when it knows that
     * the view is no longer needed.
     */
    public void removeViewForUUID(String uuid) {
        globals.remove( uuid );
    }

    /**
     * Post a message.
     * @param message   A string containing a message URI. Normally this is specified using a
     *                  post: URI, but the method will attempt to promote other URIs to messages.
     *                  A plain non-URI string is interpreted as the message name.
     * @param sender    The component sending the message.
     */
    public void postMessage(String message, final Object sender) {
        // Try parsing the action URI.
        CompoundURI messageURI = CompoundURI.tryParsing( message );
        // If URI doesn't parse then it may be a bare message, try prepending post: and parsing again.
        if( messageURI == null ) {
            message = String.format("post:%s", message );
            messageURI = CompoundURI.tryParsing( message );
        }
        if( messageURI != null ) {
            final CompoundURI uri = messageURI;
            // Process the message on the main thread. This is because the URI may dereference to a
            // view and some views (e.g. web views) have to be instantiated on the UI thread.
            new Handler( Looper.getMainLooper() ).post( new Runnable() {
                @Override
                public void run() {
                    // See if the URI resolves to a post message object.
                    Object messageObj = uriHandler.dereference( uri );
                    if( !(messageObj instanceof Message) ) {
                        // Automatically promote views to 'show' messages.
                        if( messageObj instanceof Fragment ) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("view", messageObj );
                            messageObj = new Message("show", params );
                        }
                        else if( messageObj instanceof String ) {
                            // Assume a simple name only message with no parameters.
                            messageObj = new Message( (String) messageObj, null );
                        }
                        else return; // Can't promote the message, so can't dispatch it.
                    }
                    // messageObj is always a Message instance by this point.
                    routeMessage( (Message) messageObj, sender );
                }
            } );
        }
    }

    /** Test whether a URI scheme name belongs to an internal URI scheme. */
    public boolean isInternalURISchemeName(String schemeName) {
        return uriHandler.hasHandlerForURIScheme( schemeName );
    }

    /** Open a public URL. */
    public boolean openURL(String url) {
        if( url == null ) {
            return false;
        }
        int idx = url.indexOf(':');
        String scheme = idx > 0 ? url.substring( 0, idx ) : "";
        Intent intent = null;
        if( "tel".equalsIgnoreCase( scheme )) {
            intent = new Intent( Intent.ACTION_DIAL, Uri.parse( url ));
        }
        if( "https".equalsIgnoreCase( scheme ) || "http".equalsIgnoreCase( scheme )) {
            intent = new Intent( Intent.ACTION_VIEW, Uri.parse( url ));
        }
        if( "mailto".equalsIgnoreCase( scheme )) {
            intent = new Intent( Intent.ACTION_SENDTO, Uri.parse( url ));
        }
        if( intent != null ) {
            ((Activity)currentActivity).startActivity( intent );
            return true;
        }
        return false;
    }

    // MessageRouter interface
    public boolean routeMessage(Message message, Object sender) {
        boolean routed = false;
        // If the sender is within the UI then search the view hierarchy for a message handler.
        Object target = sender;
        // Evaluate messages with relative target paths against the sender.
        while( target != null && !routed ) {
            // See if the current handler can take the message.
            if( message.hasEmptyTarget() ) {
                // Message has no target info so looking for a message handler.
                if( target instanceof MessageReceiver ) {
                    routed = ((MessageReceiver)target).receiveMessage( message, sender );
                }
            }
            else if( target instanceof MessageRouter ) {
                // Message does have target info so looking for a message router.
                routed = ((MessageRouter)target).routeMessage( message, sender );
            }
            if (!routed) {
                // Message not routed, so try moving up the view hierarchy.
                if( target instanceof ViewController ) {
                    // TODO
                }
                else if( target instanceof Fragment ) {
                    // If target is a fragment then bubble the message up through the fragment
                    // display hierarchy until a receiver is found.
                    Fragment parentFragment = ((Fragment)target).getParentFragment();
                    if( parentFragment == null ) {
                        target = ((Fragment)target).getActivity();
                    }
                    else {
                        target = parentFragment;
                    }
                }
                // TODO Is there a reliable way to bubble a message from a view up to the
                // TODO containing fragment? This may require (i) knowing the current activity, and
                // TODO (ii) asking the activity to ask each fragment it contains whether that
                // TODO fragment contains the specified view.
                else {
                    // Can't process the message any further, so leave the loop.
                    break;
                }
            }
        }
        // If message not dispatched then let this container try handling it.
        if( !routed ) {
            routed = super.routeMessage( message, sender );
        }
        return routed;
    }

    // MessageReceiver interface
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("open-url") ) {
            String url = (String)message.getParameter("url");
            // Open URLs by dispatching to the default intent (e.g. the browser)
            Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( url ) );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            androidContext.startActivity( intent );
        }
        else if( message.hasName("show") ) {
            Object view = message.getParameter("view");
            if( view != null ) {
                showView( view );
            }
            else {
                Log.w(Tag,"'show' message missing 'view' parameter.");
            }
        }
        return true;
    }

    /**
     * Start a new activity to display the app's root view.
     */
    public void showRootView() {
        Object rootView = nameds.get("rootView");
        if( rootView != null ) {
            showView( rootView );
        }
        else {
            Log.e(Tag,"No root view component found");
        }
    }

    /**
     * Display a view. Starts a new activity if necessary.
     */
    public void showView(Object view) {

        // NOTE on support view types: Pttn supports a number of different view types. The
        // main type is ViewController, and corresponds most closely to the UIViewController
        // class on iOS. Views can also be represented using Fragments (as the natural native
        // Android view type) but use of Fragments is discouraged where possible, due to the
        // baroque fragment life-cycle, and problems that occur when fragments are nested.
        // If a view is represented by an Activity then an Intent instance should be used at
        // this point to represent the configured view. (This is because of Android mechanics
        // - the OS is responsible for instantiating activities, through intents, so it doesn't
        // make sense for the container to instantiate an activity). Intents, if needed, can
        // be instantiated for the container by an object factory.

        if( view instanceof ViewController ) {
            showViewUsingActivityType( view, ViewControllerActivity.class );
        }
        else if( view instanceof ViewFragment ) {
            showViewUsingActivityType( view, ViewFragmentActivity.class );
        }
        else if( view instanceof Fragment ) {
            showViewUsingActivityType( view, FragmentActivity.class );
        }
        else if( view instanceof Intent ) {
            androidContext.startActivity( (Intent)view );
        }
        else {
            Log.w(Tag, String.format("Unable to display view of type %s", view.getClass() ) );
        }
        /* Following code left here as an example of why it doesn't make sense for the
           container to instantiate Activities as view instances - the instance is discarded
           at this point, an intent is created instead which requests the OS to start the
           activity.
        else if( view instanceof Activity ) {
            Intent intent = new Intent( androidContext, Activity.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            androidContext.startActivity( intent );
        }
        */
    }

    /**
     * Show a view using the specified activity type.
     * If the current activity is of the specified type then its showView(...) is called (note that
     * the current activity is an instance of PttnActivity). If the current activity can't be used
     * then an intent is raised to start a new activity of the specified type, which will then
     * display the view instance once it has launched.
     *
     * @param view          The view to display.
     * @param activityType  The type (i.e. class) of the activity to use.
     */
    private void showViewUsingActivityType(Object view, Class activityType) {
        if( activityType.isInstance( currentActivity ) ) {
            currentActivity.showView( view );
        }
        else {
            String uuid = registerPendingView( view );
            Intent intent = new Intent( androidContext, activityType );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            intent.putExtra( PttnActivity.IntentActions.ViewUUID.name(), uuid );
            androidContext.startActivity( intent );
        }
    }

    /** The app container's singleton instance. */
    static AppContainer Instance;

    public static synchronized AppContainer getAppContainer(Context context) {
        if( Instance == null ) {
            Instance = new AppContainer( context );
        }
        return Instance;
    }

    public static AppContainer getAppContainer() {
        return Instance;
    }

}
