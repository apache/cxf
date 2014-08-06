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

package org.apache.cxf.transport.https_jetty;

import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.transport.http_jetty.JettyConnectorFactory;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.apache.cxf.transport.https.AliasedX509ExtendedKeyManager;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * This class wraps the JettyConnectorFactory and will create 
 * TLS enabled acceptors.
 */
public final class JettySslConnectorFactory implements JettyConnectorFactory {
    private static final Logger LOG = LogUtils.getL7dLogger(JettySslConnectorFactory.class);    
    
    final TLSServerParameters tlsServerParameters;
    
    public JettySslConnectorFactory(TLSServerParameters params) {
        tlsServerParameters = params;
    }

    /**
     * Create a Listener.
     * 
     * @param host the host to bind to.  IP address or hostname is allowed. null to bind to all hosts.
     * @param port the listen port
     */
    public AbstractConnector createConnector(JettyHTTPServerEngine engine, String host, int port) {
        assert tlsServerParameters != null;
        
        SslContextFactory sslcf = new CXFSslContextFactory();
        SslSelectChannelConnector secureConnector = 
            new SslSelectChannelConnector(sslcf);
        if (host != null) {
            secureConnector.setHost(host);
        }
        secureConnector.setPort(port);
        if (engine.getMaxIdleTime() > 0) {
            secureConnector.setMaxIdleTime(engine.getMaxIdleTime());
        }
        secureConnector.setReuseAddress(engine.isReuseAddress());
        decorateCXFJettySslSocketConnector(sslcf);
        return secureConnector;
    }
    
    private class CXFSslContextFactory extends SslContextFactory {
        public CXFSslContextFactory() {
            super();
        }
        protected void doStart() throws Exception {
            setSslContext(createSSLContext(this));
            super.doStart();
        }
        public void checkKeyStore() {
            //we'll handle this later
        }
    }
    
    protected SSLContext createSSLContext(SslContextFactory scf) throws Exception  {
        String proto = tlsServerParameters.getSecureSocketProtocol() == null
            ? "TLS" : tlsServerParameters.getSecureSocketProtocol();
 
        SSLContext context = tlsServerParameters.getJsseProvider() == null
            ? SSLContext.getInstance(proto)
                : SSLContext.getInstance(proto, tlsServerParameters.getJsseProvider());
            
        KeyManager keyManagers[] = tlsServerParameters.getKeyManagers();
        if (tlsServerParameters.getCertAlias() != null) {
            keyManagers = getKeyManagersWithCertAlias(keyManagers);
        }
        context.init(tlsServerParameters.getKeyManagers(), 
                     tlsServerParameters.getTrustManagers(),
                     tlsServerParameters.getSecureRandom());

        String[] cs = 
            SSLUtils.getCiphersuites(
                    tlsServerParameters.getCipherSuites(),
                    SSLUtils.getServerSupportedCipherSuites(context),
                    tlsServerParameters.getCipherSuitesFilter(),
                    LOG, true);
                
        scf.setExcludeCipherSuites(cs);
        return context;
    }
    protected KeyManager[] getKeyManagersWithCertAlias(KeyManager keyManagers[]) throws Exception {
        if (tlsServerParameters.getCertAlias() != null) {
            for (int idx = 0; idx < keyManagers.length; idx++) {
                if (keyManagers[idx] instanceof X509KeyManager) {
                    keyManagers[idx] = new AliasedX509ExtendedKeyManager(
                        tlsServerParameters.getCertAlias(), (X509KeyManager)keyManagers[idx]);
                }
            }
        }
        return keyManagers;
    }
    protected void setClientAuthentication(SslContextFactory con,
                                           ClientAuthentication clientAuth) {
        con.setWantClientAuth(true);
        if (clientAuth != null) {
            if (clientAuth.isSetWant()) {
                con.setWantClientAuth(clientAuth.isWant());
            }
            if (clientAuth.isSetRequired()) {
                con.setNeedClientAuth(clientAuth.isRequired());
            }
        }
    }    
    /**
     * This method sets the security properties for the CXF extension
     * of the JettySslConnector.
     */
    private void decorateCXFJettySslSocketConnector(
            SslContextFactory con
    ) {
        setClientAuthentication(con,
                                tlsServerParameters.getClientAuthentication());
        con.setCertAlias(tlsServerParameters.getCertAlias());
    }


}
