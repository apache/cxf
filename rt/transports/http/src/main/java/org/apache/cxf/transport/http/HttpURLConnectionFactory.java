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
 * The primary purpose for this interface is to generate HttpURLConnections
 * and retrieve information about the connections. This interface is also 
 * meant to be used as a lower denominator for HttpURLConnections and
 * HttpsURLConnections.
 */
public interface HttpURLConnectionFactory {

    /**
     * Create an HttpURLConnection, proxified if neccessary.
     * 
     * @param proxy The proxy. May be null if connection is not to be proxied.
     * @param url The target URL
     * @return An appropriate URLConnection
     */
    HttpURLConnection createConnection(Proxy proxy, URL url) throws IOException;
    
    /**
     * This method returns Connection Info objects for the particular
     * connection. The connection must be connected.
     * @param con The connection that is the subject of the information object.
     * @return The HttpURLConnection Info for the given connection.
     * @throws IOException
     */
    HttpURLConnectionInfo getConnectionInfo(
            HttpURLConnection connnection
    ) throws IOException;
    
    /**
     * @return the protocol that this connection supports (http or https)
     */
    String getProtocol();
}
