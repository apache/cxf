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
import java.net.URISyntaxException;
import java.net.URL;

/**
 * This class is returned from the URLConnectionFactory to give
 * information that is from the URLConnection that was created by that
 * factory.
 */
public class URLConnectionInfo {
    /**
     * The URL the connection is associated with.
     */
    protected final URI theURI;

    public URLConnectionInfo(URL url) {
        URI u = null;
        try {
            u = url.toURI();
        } catch (URISyntaxException e) {
            //ignore
        }
        theURI = u;
    }

    public URLConnectionInfo(URI uri) {
        theURI = uri;
    }

    /**
     * This field returns the URI associated with the connection
     * in question.
     *
     * @return
     */
    public URI getURI() {
        return theURI;
    }
}
