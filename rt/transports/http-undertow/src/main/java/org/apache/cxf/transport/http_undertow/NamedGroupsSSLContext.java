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

package org.apache.cxf.transport.http_undertow;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.cxf.transport.https.SSLUtils;

/**
 * An SSLContext that applies the configured TLS named groups to every SSLEngine it creates.
 * Undertow only accepts an SSLContext for an HTTPS listener and has no option for named groups.
 */
final class NamedGroupsSSLContext extends SSLContext {

    private NamedGroupsSSLContext(SSLContext delegate, List<String> namedGroups) {
        super(new Spi(delegate, namedGroups), delegate.getProvider(), delegate.getProtocol());
    }

    /**
     * Returns the context itself when no named groups are configured, else a wrapper that applies them.
     */
    static SSLContext wrap(SSLContext context, List<String> namedGroups) {
        if (context == null || namedGroups == null || namedGroups.isEmpty()) {
            return context;
        }
        return new NamedGroupsSSLContext(context, namedGroups);
    }

    private static final class Spi extends SSLContextSpi {
        private final SSLContext delegate;
        private final List<String> namedGroups;

        Spi(SSLContext delegate, List<String> namedGroups) {
            this.delegate = delegate;
            this.namedGroups = namedGroups;
        }

        @Override
        protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr)
            throws KeyManagementException {
            delegate.init(km, tm, sr);
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return delegate.getSocketFactory();
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return delegate.getServerSocketFactory();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            SSLEngine engine = delegate.createSSLEngine();
            SSLUtils.applyNamedGroups(engine, namedGroups);
            return engine;
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            SSLEngine engine = delegate.createSSLEngine(host, port);
            SSLUtils.applyNamedGroups(engine, namedGroups);
            return engine;
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return delegate.getServerSessionContext();
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return delegate.getClientSessionContext();
        }

        @Override
        protected SSLParameters engineGetDefaultSSLParameters() {
            return delegate.getDefaultSSLParameters();
        }

        @Override
        protected SSLParameters engineGetSupportedSSLParameters() {
            return delegate.getSupportedSSLParameters();
        }
    }
}
