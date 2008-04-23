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

import java.net.URL;
import java.net.URLConnection;

/**
 * This class is returned from the URLConnectionFactory to give
 * information that is from the URLConnection that was created by that
 * factory.
 */
public class URLConnectionInfo {
    /**
     * The URL the connection is associated with.
     */
    protected final URL theURL;
    
    /**
     * This constructor is used to represent a URLConnection.
     * 
     * @param connection The URLConnection that this info object will represent.
     */
    public URLConnectionInfo(URLConnection connection) {
        theURL = connection.getURL();
    }
    
    /**
     * This field returns the URL associated with the connection
     * in question.
     * 
     * @return
     */
    public URL getURL() {
        return theURL;
    }
}
