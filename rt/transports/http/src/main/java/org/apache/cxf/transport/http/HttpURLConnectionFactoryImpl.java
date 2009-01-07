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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

/**
 * This class is a URLConnectionFactory that creates URLConnections
 * for the HTTP protocol.
 *
 */
public class HttpURLConnectionFactoryImpl implements HttpURLConnectionFactory {

    public static final String HTTP_URL_PROTOCOL_ID = "http";
    
    /**
     * This call creates an URLConnection for an HTTP url.
     * @throws IOException if the url protocol is not "http".
     */
    public HttpURLConnection createConnection(Proxy proxy, URL url)
        throws IOException {

        if (!url.getProtocol().equals(HTTP_URL_PROTOCOL_ID)) {
            throw new IOException("Illegal Protocol " 
                    + url.getProtocol() 
                    + " for HTTP URLConnection Factory.");
        }
        if (proxy != null) {
            return (HttpURLConnection) url.openConnection(proxy);
        } else {
            return (HttpURLConnection) url.openConnection();
        }
    }

    /**
     * This operation returns the HttpURLConnectionInfo object that
     * represents the HttpURLConnection.
     */
    public HttpURLConnectionInfo getConnectionInfo(
        HttpURLConnection connection
    ) throws IOException {
        // There is no special information other than the URL
        // to represent for an HttpURLConnection.
        return new HttpURLConnectionInfo(connection);
    }

    public String getProtocol() {
        return "http";
    }
}
