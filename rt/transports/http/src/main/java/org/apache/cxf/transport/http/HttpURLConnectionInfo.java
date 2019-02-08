/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.http;

import java.net.URI;
import java.net.URL;

/**
 * This class contains the information about the HTTP Connection that
 * will be making an HTTP request. This class should be used
 * when the getURL().getProtocol() is "http" or "https".
 */
public class HttpURLConnectionInfo extends URLConnectionInfo {

    private final String httpRequestMethod;

    /**
     * This constructor takes the HttpURLConnection and extracts
     * the httpRequestMethod.
     */
    public HttpURLConnectionInfo(URL url, String method) {
        super(url);
        httpRequestMethod = method;
    }

    /**
     * This constructor takes the HttpURLConnection and extracts
     * the httpRequestMethod.
     */
    public HttpURLConnectionInfo(URI uri, String method) {
        super(uri);
        httpRequestMethod = method;
    }

    /**
     * This method returns the request method on the represented
     * HttpURLConnection.
     */
    public String getHttpRequestMethod() {
        return httpRequestMethod;
    }
}
