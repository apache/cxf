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
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.apache.cxf.common.injection.NoJSR250Annotations;

/**
 * A convenient class for storing URI and URL representation of an address and avoid useless conversions.
 * A proxy for the current address is also lazily resolved and stored; most of the times, that proxy can
 * be used to prevent the HttpURLConnection from computing the proxy when the connection is opened.
 *
 * The class is thread-safe.
 */
@NoJSR250Annotations
public final class Address {

    private final String str;
    private final URI uri;
    private volatile URL url;
    private volatile Proxy defaultProxy;

    public Address(String str) throws URISyntaxException {
        this.str = str;
        this.uri = new URI(str);
    }

    public Address(String str, URI uri) {
        this.str = str;
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

    public String getString() {
        return str;
    }

    public Proxy getDefaultProxy() {
        if (defaultProxy == null) {
            synchronized (this) {
                if (defaultProxy == null) {
                    defaultProxy = chooseProxy(uri);
                }
            }
        }
        return defaultProxy;
    }

    private static Proxy chooseProxy(URI uri) {
        ProxySelector sel = java.security.AccessController
            .doPrivileged(new java.security.PrivilegedAction<ProxySelector>() {
                @Override
                public ProxySelector run() {
                    return ProxySelector.getDefault();
                }
            });
        if (sel == null) {
            return Proxy.NO_PROXY;
        }
        //detect usage of user-defined proxy and avoid optimizations in that case
        if (!"sun.net.spi.DefaultProxySelector".equals(sel.getClass().getName())) {
            return null;
        }
        Iterator<Proxy> it = sel.select(uri).iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return Proxy.NO_PROXY;
    }
}
