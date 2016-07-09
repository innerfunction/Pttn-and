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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * An HTTP request.
 *
 * Created by juliangoacher on 09/07/16.
 */
public abstract class Request {

    static final int DataBufferSize = 4096;

    /** The URL being connected to. */
    private URL url;
    /** The HTTP method, e.g. GET, POST. */
    private String method;
    /** Optional request body data. */
    private byte[] body;

    public Request(String url, String method) throws MalformedURLException {
        this.url = new URL( url );
        this.method = method;
    }

    /** Set the request body. */
    public void setBody(String body) {
        this.body = body.getBytes();
    }

    /** Connect to the server and send the request data. */
    Response connect(Client client) throws IOException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            connection.setRequestMethod( method );
            // TODO Some of these connection settings should be configured via properties on the client.
            connection.setConnectTimeout( 5000 );
            connection.setReadTimeout( 5000 );
            if( body != null ) {
                connection.setDoInput(true);
                connection.setFixedLengthStreamingMode( body.length );
                BufferedOutputStream out = new BufferedOutputStream( connection.getOutputStream() );
                out.write( body );
                out.flush();
            }
            return readResponse( connection );
        }
        finally {
            connection.disconnect();
        }
    }

    /** Read the server response. */
    abstract Response readResponse(HttpURLConnection connection) throws IOException;

}
