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


import java.io.IOException;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.transport.https.SSLUtils;
import org.mortbay.jetty.security.SslSocketConnector;

/**
 * This class extends the Jetty SslSocketConnector, which allows
 * us to configure it more in tune with the JSSE, using KeyManagers
 * and TrustManagers. Also, Jetty version 6.1.3 has a bug where
 * the Trust store needs a password.
 */
public class CXFJettySslSocketConnector extends SslSocketConnector {
    private static final Logger LOG = LogUtils.getL7dLogger(CXFJettySslSocketConnector.class);    
    
    protected KeyManager[]   keyManagers;
    protected TrustManager[] trustManagers;
    protected SecureRandom   secureRandom;
    protected List<String>   cipherSuites;
    protected FiltersType    cipherSuitesFilter;
    
    /**
     * Set the cipherSuites
     */
    protected void setCipherSuites(List<String> cs) {
        cipherSuites = cs;
    }
    
    /**
     * Set the CipherSuites Filter
     */
    protected void setCipherSuitesFilter(FiltersType filter) {
        cipherSuitesFilter = filter;
    }
    
    /**
     * Set the KeyManagers.
     */
    protected void setKeyManagers(KeyManager[] kmgrs) {
        keyManagers = kmgrs;
    }
    
    /**
     * Set the TrustManagers.
     */
    protected void setTrustManagers(TrustManager[] tmgrs) {
        trustManagers = tmgrs;
    }
    
    /**
     * Set the SecureRandom Parameters
     */
    protected void setSecureRandom(SecureRandom random) {
        secureRandom = random;
    }
    
    /**
     * Set the ClientAuthentication (from the JAXB type) that
     * configures an HTTP Destination.
     */
    protected void setClientAuthentication(ClientAuthentication clientAuth) {
        setWantClientAuth(true);
        if (clientAuth != null) {
            if (clientAuth.isSetWant()) {
                setWantClientAuth(clientAuth.isWant());
            }
            if (clientAuth.isSetRequired()) {
                setNeedClientAuth(clientAuth.isRequired());
            }
        }
    }
    
    /**
     * We create our own socket factory.
     */
    @Override
    protected SSLServerSocketFactory createFactory()
        throws Exception {
    
        String proto = getProtocol() == null
               ? "TLS"
               : getProtocol();
        
        SSLContext context = getProvider() == null
               ? SSLContext.getInstance(proto)
               : SSLContext.getInstance(proto, getProvider());

        context.init(keyManagers, trustManagers, secureRandom);

        SSLServerSocketFactory con = context.getServerSocketFactory();

        
        String[] cs = 
            SSLUtils.getCiphersuites(
                    cipherSuites,
                    SSLUtils.getServerSupportedCipherSuites(context),
                    cipherSuitesFilter,
                    LOG, true);
        
        setExcludeCipherSuites(cs);
        return con;
    }
    protected ServerSocket newServerSocket(String host, int port, int backlog) throws IOException {
        ServerSocket sock = super.newServerSocket(host, port, backlog);
        if (sock instanceof SSLServerSocket && LOG.isLoggable(Level.INFO)) {
            SSLServerSocket sslSock = (SSLServerSocket)sock;
            LOG.log(Level.INFO, "CIPHERSUITES_SET", Arrays.asList(sslSock.getEnabledCipherSuites()));
        }
        return sock;
    }

}
