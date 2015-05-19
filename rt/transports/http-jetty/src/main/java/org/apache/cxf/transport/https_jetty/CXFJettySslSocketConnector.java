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


import java.security.SecureRandom;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.transport.https.AliasedX509ExtendedKeyManager;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;

/**
 * This class extends the Jetty SslSelectChannelConnector, which allows
 * us to configure it more in tune with the JSSE, using KeyManagers
 * and TrustManagers. 
 */
public class CXFJettySslSocketConnector extends SslSelectChannelConnector {
    private static final Logger LOG = LogUtils.getL7dLogger(CXFJettySslSocketConnector.class);    
    
    protected KeyManager[]   keyManagers;
    protected TrustManager[] trustManagers;
    protected SecureRandom   secureRandom;
    protected List<String>   cipherSuites;
    protected FiltersType    cipherSuitesFilter;
    protected List<String>   excludeProtocols;
    protected List<String>   includeProtocols;
       
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
     * Set the protocols to exclude
     */
    protected void setExcludeProtocols(List<String> ps) {
        excludeProtocols = ps;
    }
    
    /**
     * Set the protocols to include
     */
    protected void setIncludeProtocols(List<String> ps) {
        includeProtocols = ps;
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
        getCxfSslContextFactory().setWantClientAuth(true);
        if (clientAuth != null) {
            if (clientAuth.isSetWant()) {
                getCxfSslContextFactory().setWantClientAuth(clientAuth.isWant());
            }
            if (clientAuth.isSetRequired()) {
                getCxfSslContextFactory().setNeedClientAuth(clientAuth.isRequired());
            }
        }
    }
    
    protected void doStart() throws Exception {
        // setup the create SSLContext on the SSLContextFactory
        getCxfSslContextFactory().setSslContext(createSSLContext());
        super.doStart();
    }
    
    protected SSLContext createSSLContext() throws Exception  {
        String proto = getCxfSslContextFactory().getProtocol() == null
            ? "TLS" : getCxfSslContextFactory().getProtocol();
 
        // Exclude SSLv3 + SSLv2Hello by default unless the protocol is given as SSLv3, or if
        // they have been explicitly included
        if (!"SSLv3".equals(proto) 
            && (excludeProtocols == null || excludeProtocols.isEmpty())) {
            if (includeProtocols == null || !includeProtocols.contains("SSLv3")) {
                getSslContextFactory().addExcludeProtocols("SSLv3");
            }
            if (includeProtocols == null || !includeProtocols.contains("SSLv2Hello")) {
                getSslContextFactory().addExcludeProtocols("SSLv2Hello");
            }
        }
        
        if (excludeProtocols != null) {
            for (String p : excludeProtocols) {
                getSslContextFactory().addExcludeProtocols(p);
            }
        }

        SSLContext context = getCxfSslContextFactory().getProvider() == null
            ? SSLContext.getInstance(proto)
                : SSLContext.getInstance(proto, getCxfSslContextFactory().getProvider());
            
        if (getCxfSslContextFactory().getCertAlias() != null) {
            getKeyManagersWithCertAlias();
        }
        context.init(keyManagers, trustManagers, secureRandom);
/*
        String[] cs = 
            SSLUtils.getCiphersuites(
                    cipherSuites,
                    SSLUtils.getServerSupportedCipherSuites(context),
                    cipherSuitesFilter,
                    LOG, true);
        
        getCxfSslContextFactory().setExcludeCipherSuites(cs);
        */
        
        // Set the CipherSuites
        final String[] supportedCipherSuites = 
            SSLUtils.getServerSupportedCipherSuites(context);

        String[] excludedCipherSuites = 
            SSLUtils.getCiphersuites(
                    cipherSuites,
                    supportedCipherSuites,
                    cipherSuitesFilter,
                    LOG, true);
        getCxfSslContextFactory().setExcludeCipherSuites(excludedCipherSuites);
        
        String[] includedCipherSuites = 
            SSLUtils.getCiphersuites(
                    cipherSuites,
                    supportedCipherSuites,
                    cipherSuitesFilter,
                    LOG, false);
        getCxfSslContextFactory().setIncludeCipherSuites(includedCipherSuites);
        
        return context;
    }
    
    protected void getKeyManagersWithCertAlias() throws Exception {
        if (getCxfSslContextFactory().getCertAlias() != null) {
            for (int idx = 0; idx < keyManagers.length; idx++) {
                if (keyManagers[idx] instanceof X509KeyManager) {
                    keyManagers[idx] = new AliasedX509ExtendedKeyManager(
                        getCxfSslContextFactory().getCertAlias(), (X509KeyManager)keyManagers[idx]);
                }
            }
        }
    }
    
    public CxfSslContextFactory getCxfSslContextFactory() {
        try {
            Object o = getClass().getMethod("getSslContextFactory").invoke(this);
            return ReflectionInvokationHandler.createProxyWrapper(o, CxfSslContextFactory.class);
        } catch (Exception e) {
            //ignore, the NPE is fine
        }
        
        return null;
    }
    
    interface CxfSslContextFactory {
        void setExcludeCipherSuites(String ... cs);
        
        void setIncludeCipherSuites(String ... cs);

        String getProtocol();
        
        String getProvider();
        
        void setSslContext(SSLContext createSSLContext);

        void setNeedClientAuth(boolean required);

        void setWantClientAuth(boolean want);

        void setProtocol(String secureSocketProtocol);

        void setProvider(String jsseProvider);
        
        void setCertAlias(String certAlias);
        
        String getCertAlias();
    }
    
}
