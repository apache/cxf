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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.cxf.common.injection.NoJSR250Annotations;

/**
 * A convenient class for storing URI and URL representation of an address and avoid useless conversions.
 * The class is thread-safe.
 */
@NoJSR250Annotations
public final class Address {  

    private final URI uri;
    private volatile URL url;
    
    public Address(URI uri) {
        this.uri = uri;
    }
    
    public URL getURL() throws MalformedURLException {
        if (url == null) {
            synchronized (this) {
                if (url == null) {
                    url = uri.toURL();
                }
            }
        }
        return url;
    }
    
    public URI getURI() {
        return uri;
    }
}
