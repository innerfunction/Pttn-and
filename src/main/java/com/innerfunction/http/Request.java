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

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An HTTP request.
 *
 * Attached by juliangoacher on 09/07/16.
 */
public abstract class Request {

    static final int DataBufferSize = 4096;

    /** The URL being connected to. */
    private URL url;
    /** The request URL as a URI (needed for storing cookies. */
    private URI uri;
    /** The HTTP method, e.g. GET, POST. */
    private String method;
    /** Optional request body data. */
    private byte[] body;
    /** Optional additional request headers. */
    private Map<String,Object> headers;

    public Request(String url, String method) throws MalformedURLException {
        this.url = new URL( url );
        try {
            this.uri = this.url.toURI();
        }
        catch(URISyntaxException e) {
            // Won't/can't happen.
        }
        this.method = method;
    }

    /** Get the request URL. */
    public URL getURL() {
        return url;
    }

    /** Set the request body. */
    public void setBody(String body) {
        this.body = body.getBytes();
    }

    /** Set the request body data. */
    public void setBody(byte[] body) {
        this.body = body;
    }

    /** Set request headers. */
    public void setHeaders(Map<String,Object> headers) {
        this.headers = headers;
    }

    /** Connect to the server and send the request data. */
    Response connect(Client client) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            connection.setRequestMethod( method );
            // TODO Some of these connection settings should be configured via properties on the client.
            connection.setConnectTimeout( 5000 );
            connection.setReadTimeout( 5000 );
            connection.setDoInput( true );
            addCookies( connection );
            if( headers != null ) {
                for( String key : headers.keySet() ) {
                    connection.setRequestProperty( key, headers.get( key ).toString() );
                }
            }
            if( body != null ) {
                connection.setDoOutput( true ); // NOTE This call forces the request method to POST
                connection.setFixedLengthStreamingMode( body.length );
                BufferedOutputStream out = new BufferedOutputStream( connection.getOutputStream() );
                out.write( body );
                out.flush();
            }
            Response response = readResponse( connection );
            storeCookies( connection );
            return response;
        }
        finally {
            connection.disconnect();
        }
    }

    /** Read the server response. */
    abstract Response readResponse(HttpURLConnection connection) throws IOException;

    /**
     * Open an input stream on a connection.
     * This method encapsulates the complexity associated with opening an input stream on a non-2xx
     * HTTP response.
     * @param connection    The HTTP connection.
     * @return An input stream on the HTTP response.
     * @throws IOException If a non-HTTP connection error occurs.
     */
    protected InputStream openInputStream(HttpURLConnection connection) throws IOException {
        InputStream in;
        try {
            in = connection.getInputStream();
        }
        catch(IOException e) {
            if( connection.getResponseCode() == -1 ) {
                throw e;
            }
            in = connection.getErrorStream();
        }
        return new BufferedInputStream( in, DataBufferSize );
    }

    /**
     * Add cookies to a request connection.
     */
    protected void addCookies(HttpURLConnection connection) {
        CookieStore cookieStore = Client.CookieManager.getCookieStore();
        List<HttpCookie> cookies = cookieStore.get( uri );
        String cookie = TextUtils.join(";", cookies );
        connection.setRequestProperty("Cookie", cookie );
    }

    /**
     * Store cookies returned by a request connection.
     */
    protected void storeCookies(HttpURLConnection connection) {
        Map<String,List<String>> headers = connection.getHeaderFields();
        List<String> cookieHeaders = new ArrayList<>();
        if( headers.containsKey("Set-Cookie") ) {
            cookieHeaders.addAll( headers.get("Set-Cookie") );
        }
        if( headers.containsKey("Set-Cookie2") ) {
            cookieHeaders.addAll( headers.get("Set-Cookie2") );
        }
        if( cookieHeaders.size() > 0 ) {
            CookieStore cookieStore = Client.CookieManager.getCookieStore();
            for( String header : cookieHeaders ) {
                List<HttpCookie> cookies = HttpCookie.parse( header );
                for( HttpCookie cookie : cookies ) {
                    cookieStore.add( uri, cookie );
                }
            }
        }
    }

    /**
     * Check for network signon.
     * As per 'Handling Network Sign-On' at https://developer.android.com/reference/java/net/HttpURLConnection.html
     */
    protected void checkForNetworkSignon(HttpURLConnection connection) throws IOException {
        if( !url.getHost().equals( connection.getURL().getHost() ) ) {
            throw new IOException("Network sign-on");
        }
    }
}
