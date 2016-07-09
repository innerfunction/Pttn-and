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
import java.net.URL;
import java.util.Map;

/**
 * An HTTP response.
 *
 * Created by juliangoacher on 09/07/16.
 */
public class Response {

    /** The request URL. */
    private String url;
    /** The HTTP response code. */
    private int statusCode;
    /** The response body. Will be null for file responses. */
    private byte[] body;
    /** A file containing the response. */
    private File dataFile;

    Response(URL url, int statusCode, byte[] body) {
        this.url = url.toString();
        this.statusCode = statusCode;
        this.body = body;
    }

    Response(URL url, int statusCode, File dataFile) {
        this.url = url.toString();
        this.statusCode = statusCode;
        this.dataFile = dataFile;
    }

    public String getRequestURL() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public File getDataFile() {
        return dataFile;
    }

    public Map<String,Object> parseData() {
        // TODO Rename this to parse JSON? Check content type? What other content types need to be supported?
        return null;
    }
}
