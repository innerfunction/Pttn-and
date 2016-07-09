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

import com.innerfunction.q.Q;
import com.innerfunction.util.RunQueue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * An HTTP client.
 * Provides asynchronous methods for fetching files and data from an HTTP server.
 * 
 * Created by juliangoacher on 08/07/16.
 */
public class Client {

    private AuthenticationDelegate authenticationDelegate;

    public void setAuthenticationDelegate(AuthenticationDelegate delegate) {
        this.authenticationDelegate = delegate;
    }

    public Q.Promise<Response> get(String url) throws MalformedURLException {
        return get( url, null );
    }

    public Q.Promise<Response> get(String url, Map<String,Object> data) throws MalformedURLException {
        // TODO Add request parameters to URL
        Request request = new DataRequest( url, "GET");
        return send( request );
    }

    public Q.Promise<Response> getFile(String url, File dataFile) throws MalformedURLException {
        Request request = new FileRequest( url, "GET", dataFile );
        return send( request );
    }

    public Q.Promise<Response> post(String url, Map<String,Object> data) throws MalformedURLException {
        Request request = new DataRequest( url, "POST");
        // TODO Encode and set request body
        return send( request );
    }

    public Q.Promise<Response> submit(String method, String url, Map<String,Object> data) throws MalformedURLException {
        return "POST".equals( method ) ? post( url, data ) : get( url, data );
    }

    private boolean isAuthenticationErrorResponse(Response response) {
        if( authenticationDelegate != null ) {
            return authenticationDelegate.isAuthenticationErrorResponse( this, response );
        }
        return false;
    }

    private Q.Promise<Response> reauthenticate() {
        if( authenticationDelegate != null ) {
            return authenticationDelegate.reauthenticateUsingHTTPClient( this );
        }
        return Q.reject("Authentication delegate not available");
    }

    /** The background queue used to asynchronously submit HTTP requests. */
    static final RunQueue RequestQueue = new RunQueue();

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
                    Response response = request.connect( Client.this );
                    // Check for authentication failures.
                    // TODO Following code needs to be reviewed - does it interact correctly with the request queue?
                    // TODO i.e. specifically the reauthenticate step, and the re-submit that follows it.
                    if( isAuthenticationErrorResponse( response ) ) {
                        // Try to reauthenticate and then resubmit the original request.
                        reauthenticate()
                            .then(new Q.Promise.Callback<Response, Response>() {
                                public Response result(Response response) {
                                    // Retry the original request.
                                    send( request );
                                    return response;
                                }
                            })
                            .error(new Q.Promise.ErrorCallback() {
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
}
