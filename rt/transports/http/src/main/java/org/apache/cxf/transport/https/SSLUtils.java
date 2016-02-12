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
package org.apache.cxf.transport.https;

import java.security.GeneralSecurityException;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509KeyManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.jsse.TLSParameterBase;
import org.apache.cxf.configuration.jsse.TLSServerParameters;

import org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier;
import org.apache.cxf.transport.https.httpclient.PublicSuffixMatcherLoader;

public final class SSLUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SSLUtils.class);
                              
    private SSLUtils() {
        //Helper class
    }
    
    public static HostnameVerifier getHostnameVerifier(TLSClientParameters tlsClientParameters) {
        HostnameVerifier verifier;
        
        if (tlsClientParameters.getHostnameVerifier() != null) {
            verifier = tlsClientParameters.getHostnameVerifier();
        } else if (tlsClientParameters.isUseHttpsURLConnectionDefaultHostnameVerifier()) {
            verifier = HttpsURLConnection.getDefaultHostnameVerifier();
        } else if (tlsClientParameters.isDisableCNCheck()) {
            verifier = new AllowAllHostnameVerifier();
        } else {
            verifier = new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault());
        }
        return verifier;
    }
    
    public static SSLContext getSSLContext(TLSParameterBase parameters) throws GeneralSecurityException {
        // TODO do we need to cache the context
        String provider = parameters.getJsseProvider();

        String protocol = parameters.getSecureSocketProtocol() != null ? parameters
            .getSecureSocketProtocol() : "TLS";

        SSLContext ctx = provider == null ? SSLContext.getInstance(protocol) : SSLContext
            .getInstance(protocol, provider);
        
        if (parameters instanceof TLSClientParameters) {
            ctx.getClientSessionContext().setSessionTimeout(((TLSClientParameters)parameters).getSslCacheTimeout());
        }
        
        KeyManager[] keyManagers = parameters.getKeyManagers();
        if (keyManagers == null && parameters instanceof TLSClientParameters) {
            keyManagers = org.apache.cxf.configuration.jsse.SSLUtils.getDefaultKeyStoreManagers(LOG);
        }
        configureKeyManagersWithCertAlias(parameters, keyManagers);
        
        ctx.init(keyManagers, parameters.getTrustManagers(),
                 parameters.getSecureRandom());
        
        return ctx;
    }
        
    public static void configureKeyManagersWithCertAlias(TLSParameterBase tlsParameters,
                                                      KeyManager[] keyManagers)
        throws GeneralSecurityException {
        if (tlsParameters.getCertAlias() != null && keyManagers != null) {
            for (int idx = 0; idx < keyManagers.length; idx++) {
                if (keyManagers[idx] instanceof X509KeyManager
                    && !(keyManagers[idx] instanceof AliasedX509ExtendedKeyManager)) {
                    try {
                        keyManagers[idx] = new AliasedX509ExtendedKeyManager(tlsParameters.getCertAlias(),
                                                                             (X509KeyManager)keyManagers[idx]);
                    } catch (Exception e) {
                        throw new GeneralSecurityException(e);
                    }
                }
            }
        }
    }
    
    public static SSLEngine createServerSSLEngine(TLSServerParameters parameters) throws Exception {
        SSLContext sslContext = getSSLContext(parameters);
        SSLEngine serverEngine = sslContext.createSSLEngine();
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(parameters.getClientAuthentication().isRequired());
        return serverEngine;
    }
    
    public static SSLEngine createClientSSLEngine(TLSClientParameters parameters) throws Exception {
        SSLContext sslContext = getSSLContext(parameters);
        SSLEngine clientEngine = sslContext.createSSLEngine();
        clientEngine.setUseClientMode(true);
        return clientEngine;
    }
    

}
