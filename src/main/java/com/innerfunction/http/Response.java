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

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * An HTTP response.
 *
 * Attached by juliangoacher on 09/07/16.
 */
public class Response {

    static final String Tag = Response.class.getSimpleName();

    /** The request URL. */
    private String url;
    /** The HTTP response code. */
    private int statusCode;
    /** The response content encoding. */
    private String contentEncoding;
    /** The response content type. */
    private String contentType;
    /** The response body. Will be null for file responses. */
    private byte[] body;
    /** A file containing the response. */
    private File dataFile;

    Response(URL url, HttpURLConnection connection, byte[] body) throws IOException {
        this.url = url.toString();
        this.statusCode = connection.getResponseCode();
        this.contentEncoding = readContentEncoding( connection );
        this.contentType = connection.getContentType();
        this.body = body;
    }

    Response(URL url, HttpURLConnection connection, File dataFile) throws IOException {
        this.url = url.toString();
        this.statusCode = connection.getResponseCode();
        this.contentEncoding = connection.getContentEncoding();
        this.contentType = connection.getContentType();
        this.dataFile = dataFile;
    }

    private String readContentEncoding(HttpURLConnection connection) {
        String encoding = connection.getContentEncoding();
        return encoding == null ? "utf-8" : encoding;
    }

    public String getRequestURL() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public File getDataFile() {
        return dataFile;
    }

    public byte[] getRawBody() {
        return body;
    }

    public String getBody() {
        try {
            return new String( body, contentEncoding );
        }
        catch(UnsupportedEncodingException e) {
            Log.e( Tag, String.format( "Bad content encoding when reading response body: %s", contentEncoding ) );
        }
        return null;
    }

    public Object parseBodyData() {
        if( "application/json".equals( contentType ) ) {
            String json = getBody();
            if( json != null ) {
                return JSONValue.parse( json );
            }
        }
        else if( "application/x-www-form-urlencoded".equals( contentType ) ) {
            String data = getBody();
            if( data != null ) {
                Map<String,Object> result = new HashMap<>();
                String[] pairs = TextUtils.split( data, "&" );
                for( String pair : pairs ) {
                    String[] keyValue = TextUtils.split( pair, "=" );
                    String key = Uri.decode( keyValue[0] );
                    String value = Uri.decode( keyValue[1] );
                    result.put( key, value );
                }
                return result;
            }
        }
        return null;
    }
}
