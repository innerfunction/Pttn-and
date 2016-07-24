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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * An HTTP request which returns a file.
 *
 * Attached by juliangoacher on 09/07/16.
 */
public class FileRequest extends Request {

    /** A file used to store the response data. */
    private File dataFile;

    public FileRequest(String url, String method, File file) throws MalformedURLException {
        super( url, method );
        this.dataFile = file;
    }

    @Override
    Response readResponse(HttpURLConnection connection) throws IOException {
        InputStream in = connection.getInputStream();
        checkForNetworkSignon( connection );
        FileOutputStream out = new FileOutputStream( dataFile );
        byte[] buffer = new byte[DataBufferSize];
        while( true ) {
            int read = in.read( buffer, 0, buffer.length );
            if( read > -1 ) {
                out.write( buffer, 0, read );
            }
            else break;
        }
        out.flush();
        out.close();
        return new Response( getURL(), connection, dataFile );
    }

}
