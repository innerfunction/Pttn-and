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
package com.innerfunction.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.innerfunction.q.Q;
import com.innerfunction.util.Files;
import com.innerfunction.util.RunQueue;

import static com.innerfunction.util.DataLiterals.*;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * An HTTP client.
 * Provides asynchronous methods for fetching files and data from an HTTP server.
 *
 * TODO Setup caching behaviour, as per https://developer.android.com/reference/android/net/http/HttpResponseCache.html
 * TODO Need to figure out how this will work with the filesystem content cache.
 *
 * Attached by juliangoacher on 08/07/16.
 */
public class Client {

    static final String Tag = "HTTPClient";

    // Setup cookie management.
    static final CookieManager CookieManager = new CookieManager();
    static {
        CookieHandler.setDefault( CookieManager );
    }

    /** A delegate object used to perform HTTP authentication, when required. */
    private AuthenticationDelegate authenticationDelegate;
    /** An object for checking network connectivity. */
    private ConnectivityManager connectivityManager;
    /** The app's cache location. Used for temporary download files. */
    private File cacheDir;

    public Client(Context context) {
        this.connectivityManager = (ConnectivityManager)context.getSystemService( Context.CONNECTIVITY_SERVICE );
        this.cacheDir = context.getCacheDir();
    }

    public void setAuthenticationDelegate(AuthenticationDelegate delegate) {
        this.authenticationDelegate = delegate;
    }

    /** Get an HTTP URL. */
    public Q.Promise<Response> get(String url) throws MalformedURLException {
        return get( url, null );
    }

    /** Get an HTTP URL, using the specified values as query parameters. */
    public Q.Promise<Response> get(String url, Map<String,Object> params) throws MalformedURLException {
        if( params != null ) {
            String query = makeQueryString( params );
            // Does the URL already have a query string?
            if( url.indexOf('?') > -1 ) {
                // Append additional parameters to end of query string.
                url = String.format("%s&%s", url, query );
            }
            else {
                // Append parameters as new query string.
                url = String.format("%s?%s", url, query );
            }
        }
        Request request = new DataRequest( url, "GET");
        return send( request );
    }

    /**
     * Get a file from an HTTP URL.
     * @param url       The URL to get.
     * @param dataFile  A file to write the URL's contents to.
     */
    public Q.Promise<Response> getFile(String url, File dataFile) throws MalformedURLException {
        Request request = new FileRequest( url, "GET", dataFile );
        return send( request );
    }

    /**
     * Get a file from an HTTP URL.
     * Writes the response to a temporary file. The file can be retrieved through a call to
     * Response.getDataFile(). The file should be moved to suitable location if it needs to
     * be kept. (Note that the file object returned has a modified renameTo() method that can
     * properly handle moving files between separate disk partitions).
     * @param url       The URL to get.
     */
    public Q.Promise<Response> getFile(String url) throws MalformedURLException {
        String filename = String.format("%s_%d.tmp", getClass().getName(), System.currentTimeMillis() );
        File dataFile = new File( cacheDir, filename ) {
            @Override
            public void finalize() throws Throwable {
                try {
                    // Delete temporary file as soon as this object is GC'd.
                    delete();
                }
                catch(SecurityException e) {}
                super.finalize();
            }
        };
        Request request = new FileRequest( url, "GET", dataFile );
        return send( request );
    }

    /**
     * Post to an HTTP URL.
     * Performs an HTTP form post.
     * @param url       The URL to post to.
     * @param data      The data to post.
     * @return
     * @throws MalformedURLException
     */
    public Q.Promise<Response> post(String url, Map<String,Object> data) throws MalformedURLException {
        Request request = new DataRequest( url, "POST");
        request.setBody( makeQueryString( data ) );
        request.setHeaders( m(
            kv("Content-Type", "application/x-www-form-urlencoded"),
            // The following is necessary because the string version of setBody is used above,
            // which converts the body string to a byte array using the platform default encoding.
            kv("charset", Charset.defaultCharset().name() )
        ));
        return send( request );
    }

    /**
     * Submit a request to an HTTP URL.
     * @param method    The HTTP method to use, e.g. GET or POST.
     * @param url       The URL to submit the request to.
     * @param data      Data to include in the request.
     * @return
     * @throws MalformedURLException
     */
    public Q.Promise<Response> submit(String method, String url, Map<String,Object> data) throws MalformedURLException {
        return "POST".equals( method ) ? post( url, data ) : get( url, data );
    }

    /** Test whether a response represents an authentication error. */
    private boolean isAuthenticationErrorResponse(Response response) {
        if( authenticationDelegate != null ) {
            return authenticationDelegate.isAuthenticationErrorResponse( this, response );
        }
        return false;
    }

    /** Perform HTTP authentication. */
    private Q.Promise<Response> authenticate() {
        if( authenticationDelegate != null ) {
            return authenticationDelegate.authenticateUsingHTTPClient( this );
        }
        return Q.reject("Authentication delegate not available");
    }

    /** The background queue used to asynchronously submit HTTP requests. */
    static final RunQueue RequestQueue = new RunQueue( Tag );

    /**
     * Send an HTTP request.
     */
    public Q.Promise<Response> send(final Request request) {
        final Q.Promise<Response> promise = new Q.Promise<>();
        // Create a task for submitting the request on the request queue.
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    // First check for network connectivity.
                    NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
                    if( netInfo == null || !netInfo.isConnected() ) {
                        // TODO Add client configuration options to control which networks can be used.
                        throw new IOException("Network not available");
                    }
                    // Connectivity OK, try submitting the request. (Note that this method call
                    // blocks until the request completes, but that's ok because we are on a
                    // background thread).
                    Response response = request.connect( Client.this );
                    // Check for authentication failures.
                    if( isAuthenticationErrorResponse( response ) ) {
                        // Try to authenticate and then resubmit the original request.
                        authenticate()
                            .then(new Q.Promise.Callback<Response, Void>() {
                                @Override
                                public Void result(Response response) {
                                    // Retry the original request.
                                    promise.resolve( send( request ) );
                                    return null;
                                }
                            })
                            .error(new Q.Promise.ErrorCallback() {
                                @Override
                                public void error(Exception e) {
                                    promise.reject( e );
                                }
                            });
                    }
                    else {
                        promise.resolve( response );
                    }
                }
                catch(IOException e) {
                    promise.reject( e );
                }
            }
        };
        // Place the request on the request queue.
        if( !RequestQueue.dispatch( task ) ) {
            promise.reject("Failed to dispatch to request queue");
        }
        return promise;
    }

    /** Make a HTTP query string using the values in the specified map. */
    static String makeQueryString(Map<String,Object> params) {
        if( params == null ) {
            return "";
        }
        String[] pairs = new String[params.size()];
        int i = 0;
        for( String key : params.keySet() ) {
            pairs[i++] = (Uri.encode( key ) + '=' + Uri.encode( params.get( key ).toString() ) );
        }
        return TextUtils.join("&", pairs );
    }
}
