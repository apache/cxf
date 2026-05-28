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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

public class ProxyFactory {

    /**
     * This method returns the Proxy server should it be set on the
     * Client Side Policy.
     *
     * @return The proxy server or null, if not set.
     */
    public Proxy createProxy(HTTPClientPolicy policy, URI currentUrl) {
        if (policy != null && policy.isSetProxyServer()
                && !StringUtils.isEmpty(policy.getProxyServer())) {
            return getProxy(policy, currentUrl.getHost());
        }
        return getSystemProxy(currentUrl);
    }

    /**
     * Delegate to the configured {@link ProxySelector} so that custom
     * implementations (e.g. wrappers around {@code DefaultProxySelector})
     * are honoured rather than bypassed.
     */
    private Proxy getSystemProxy(URI uri) {
        ProxySelector selector = ProxySelector.getDefault();
        if (selector == null) {
            return null;
        }
        List<Proxy> proxies = selector.select(uri);
        if (proxies != null && !proxies.isEmpty()) {
            Proxy proxy = proxies.get(0);
            if (proxy != Proxy.NO_PROXY) {
                return proxy;
            }
        }
        return null;
    }

    /**
     * Honor the nonProxyHosts property value (if set).
     */
    private Proxy getProxy(final HTTPClientPolicy policy, final String hostname) {
        if (policy.isSetNonProxyHosts()) {
            Pattern pattern = PatternBuilder.build(policy.getNonProxyHosts());
            if (pattern.matcher(hostname).matches()) {
                return Proxy.NO_PROXY;
            }
        }
        return createProxy(policy);
    }

    /**
     * Construct a new {@code Proxy} instance from the given policy.
     */
    private Proxy createProxy(final HTTPClientPolicy policy) {
        return new Proxy(Proxy.Type.valueOf(policy.getProxyServerType().toString()),
                         new InetSocketAddress(policy.getProxyServer(),
                                               policy.getProxyServerPort()));
    }
}
