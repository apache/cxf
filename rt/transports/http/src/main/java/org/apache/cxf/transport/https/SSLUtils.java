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

import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.SSLContextServerParameters;
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
    
    public static SSLContextInitParameters getSSLContextInitParameters(TLSParameterBase parameters) 
            throws GeneralSecurityException {
        
        final SSLContextInitParameters contextParameters = new SSLContextInitParameters();

        KeyManager[] keyManagers = parameters.getKeyManagers();
        if (keyManagers == null && parameters instanceof TLSClientParameters) {
            keyManagers = org.apache.cxf.configuration.jsse.SSLUtils.getDefaultKeyStoreManagers(LOG);
        }
        KeyManager[] configuredKeyManagers = configureKeyManagersWithCertAlias(parameters, keyManagers);

        TrustManager[] trustManagers = parameters.getTrustManagers();
        if (trustManagers == null && parameters instanceof TLSClientParameters) {
            trustManagers = org.apache.cxf.configuration.jsse.SSLUtils.getDefaultTrustStoreManagers(LOG);
        }
        
        contextParameters.setKeyManagers(configuredKeyManagers);
        contextParameters.setTrustManagers(trustManagers);
        
        return contextParameters;
    }

    public static SSLContext getSSLContext(TLSParameterBase parameters) throws GeneralSecurityException {
        return getSSLContext(parameters, false);
    }
    public static SSLContext getSSLContext(TLSParameterBase parameters, boolean addHNV)
        throws GeneralSecurityException {
        
        // TODO do we need to cache the context
        String provider = parameters.getJsseProvider();

        String protocol = parameters.getSecureSocketProtocol() != null ? parameters
            .getSecureSocketProtocol() : "TLS";

        SSLContext ctx = provider == null ? SSLContext.getInstance(protocol) : SSLContext
            .getInstance(protocol, provider);

        final SSLContextInitParameters initParams = getSSLContextInitParameters(parameters);
        TrustManager[] tms = initParams.getTrustManagers();
        if (tms != null && addHNV && parameters instanceof TLSClientParameters) {
            HostnameVerifier hnv = getHostnameVerifier((TLSClientParameters)parameters);
            for (int i = 0; i < tms.length; i++) {
                if (tms[i] instanceof  X509TrustManager) {
                    tms[i] = new X509TrustManagerWrapper((X509TrustManager)tms[i], hnv);
                }
            }
        }
        ctx.init(initParams.getKeyManagers(), tms, parameters.getSecureRandom());

        if (parameters instanceof TLSClientParameters && ctx.getClientSessionContext() != null) {
            ctx.getClientSessionContext().setSessionTimeout(((TLSClientParameters)parameters).getSslCacheTimeout());
        }

        return ctx;
    }

    public static KeyManager[] configureKeyManagersWithCertAlias(TLSParameterBase tlsParameters,
                                                      KeyManager[] keyManagers)
        throws GeneralSecurityException {
        if (tlsParameters.getCertAlias() == null || keyManagers == null) {
            return keyManagers;
        }

        KeyManager[] copiedKeyManagers = Arrays.copyOf(keyManagers, keyManagers.length);
        for (int idx = 0; idx < copiedKeyManagers.length; idx++) {
            if (copiedKeyManagers[idx] instanceof X509KeyManager
                && !(copiedKeyManagers[idx] instanceof AliasedX509ExtendedKeyManager)) {
                try {
                    copiedKeyManagers[idx] = new AliasedX509ExtendedKeyManager(tlsParameters.getCertAlias(),
                                                                         (X509KeyManager)copiedKeyManagers[idx]);
                } catch (Exception e) {
                    throw new GeneralSecurityException(e);
                }
            }
        }

        return copiedKeyManagers;
    }

    public static SSLEngine createServerSSLEngine(TLSServerParameters parameters) throws Exception {
        SSLContext sslContext = null;
        // The full SSL context is provided by SSLContextServerParameters
        if (parameters instanceof SSLContextServerParameters sslContextServerParameters) { 
            sslContext = sslContextServerParameters.getSslContext();
        } else {
            sslContext = getSSLContext(parameters);
        }

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

    /*
     * The classes below are used by the HttpClient implementation to allow use of the 
     * HostNameVerifier that is configured.  HttpClient does not provide a hook or
     * anything to call into the HostNameVerifier after the certs are verified.  It
     * prefers that the Hostname is verified at the same time as the certificates
     * but the only option for hostname is the global on/off system property. Thus,
     * we have to provide a X509TrustManagerWrapper that would turn off the 
     * EndpointIdentificationAlgorithm and then handle the hostname verification
     * directly. However, since the peer certs are not yet verified, we also need to wrapper
     * the session so the HostnameVerifier things they are.
     */
    static class X509TrustManagerWrapper extends X509ExtendedTrustManager {

        private final X509TrustManager delegate;
        private final X509ExtendedTrustManager extendedDelegate;
        private final HostnameVerifier verifier;
        
        X509TrustManagerWrapper(X509TrustManager delegate, HostnameVerifier hnv) {
            this.delegate = delegate;
            this.verifier = hnv;
            this.extendedDelegate = delegate instanceof X509ExtendedTrustManager 
                ? (X509ExtendedTrustManager)delegate : null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException {
            delegate.checkClientTrusted(chain, s);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            if (extendedDelegate != null) {
                extendedDelegate.checkClientTrusted(chain, s, socket);
            } else {
                delegate.checkClientTrusted(chain, s);
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
                throws CertificateException {
            if (extendedDelegate != null) {
                extendedDelegate.checkClientTrusted(chain, s, sslEngine);
            } else {
                delegate.checkClientTrusted(chain, s);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
            delegate.checkServerTrusted(chain, s);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            if (extendedDelegate != null) {
                extendedDelegate.checkServerTrusted(chain, s, socket);
            } else {
                delegate.checkServerTrusted(chain, s);
            }
        }

        private String getHostName(List<SNIServerName> names) {
            if (names == null) {
                return null;
            }
            for (SNIServerName n : names) {
                if (n.getType() != StandardConstants.SNI_HOST_NAME) {
                    continue;
                }
                if (n instanceof SNIHostName) {
                    SNIHostName hostname = (SNIHostName)n;
                    return hostname.getAsciiName();
                }
            }
            return null;
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, SSLEngine engine)
                throws CertificateException {
            if (extendedDelegate != null) {
                extendedDelegate.checkServerTrusted(chain, s, new SSLEngineWrapper(engine));
                //certificates are valid, now check hostnames
                SSLSession session = engine.getHandshakeSession();
                List<SNIServerName> names = null;
                if (session instanceof ExtendedSSLSession) {
                    ExtendedSSLSession extSession = (ExtendedSSLSession)session;
                    names = extSession.getRequestedServerNames();
                }
                
                boolean identifiable = false;
                String peerHost = session.getPeerHost();
                String hostname = getHostName(names);
                session = new SSLSessionWrapper(session, chain);
                if (hostname != null && verifier.verify(hostname, session)) {
                    identifiable = true;
                }
                if (!identifiable && !verifier.verify(peerHost, session)) {
                    throw new CertificateException("No name matching " + peerHost + " found");
                }                
            } else {
                delegate.checkServerTrusted(chain, s);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }
    
    static class SSLEngineWrapper extends SSLEngine {
        final SSLEngine delegate;
        SSLEngineWrapper(SSLEngine delegate) {
            // Unwrap the delegate if it is an instance of the SSLEngineWrapper
            if (delegate instanceof SSLEngineWrapper) {
                this.delegate = ((SSLEngineWrapper) delegate).delegate;
            } else {
                this.delegate = delegate;
            }
        }
        public SSLParameters getSSLParameters() {
            //make sure the hostname verification is not done in the default X509 stuff
            //so we can do it later
            SSLParameters params = delegate.getSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);
            return params;
        }
        @Override
        public SSLSession getHandshakeSession() {
            return delegate.getHandshakeSession();
        }
        @Override
        public void beginHandshake() throws SSLException {
            delegate.beginHandshake();
        }

        @Override
        public void closeInbound() throws SSLException {
            delegate.closeInbound();
        }
        @Override
        public void closeOutbound() {
            delegate.closeOutbound();
        }
        @Override
        public Runnable getDelegatedTask() {
            return delegate.getDelegatedTask();
        }
        @Override
        public boolean getEnableSessionCreation() {
            return delegate.getEnableSessionCreation();
        }
        @Override
        public String[] getEnabledCipherSuites() {
            return delegate.getEnabledCipherSuites();
        }
        @Override
        public String[] getEnabledProtocols() {
            return delegate.getEnabledProtocols();
        }
        @Override
        public HandshakeStatus getHandshakeStatus() {
            return delegate.getHandshakeStatus();
        }
        @Override
        public boolean getNeedClientAuth() {
            return delegate.getNeedClientAuth();
        }
        @Override
        public SSLSession getSession() {
            return delegate.getSession();
        }
        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }
        @Override
        public String[] getSupportedProtocols() {
            return delegate.getSupportedProtocols();
        }
        @Override
        public boolean getUseClientMode() {
            return delegate.getUseClientMode();
        }
        @Override
        public boolean getWantClientAuth() {
            return delegate.getWantClientAuth();
        }
        @Override
        public boolean isInboundDone() {
            return delegate.isInboundDone();
        }
        @Override
        public boolean isOutboundDone() {
            return delegate.isInboundDone();
        }
        @Override
        public void setEnableSessionCreation(boolean arg0) {
            delegate.setEnableSessionCreation(arg0);            
        }
        @Override
        public void setEnabledCipherSuites(String[] arg0) {
            delegate.setEnabledCipherSuites(arg0);
        }
        @Override
        public void setEnabledProtocols(String[] arg0) {
            delegate.setEnabledProtocols(arg0);
        }
        @Override
        public void setNeedClientAuth(boolean arg0) {
            delegate.setNeedClientAuth(arg0);
        }
        @Override
        public void setUseClientMode(boolean arg0) {
            delegate.setUseClientMode(arg0);
        }
        @Override
        public void setWantClientAuth(boolean arg0) {
            delegate.setWantClientAuth(arg0);
        }
        @Override
        public SSLEngineResult unwrap(ByteBuffer arg0, ByteBuffer[] arg1, int arg2, int arg3)
            throws SSLException {
            return null;
        }
        @Override
        public SSLEngineResult wrap(ByteBuffer[] arg0, int arg1, int arg2, ByteBuffer arg3)
            throws SSLException {
            return null;
        }
        
    }
    
    static class SSLSessionWrapper implements SSLSession {
        SSLSession session;
        Certificate[] certificates;
        SSLSessionWrapper(SSLSession s, Certificate[] certs) {
            this.certificates = certs;
            this.session = s;
        }
        @Override
        public byte[] getId() {
            return session.getId();
        }

        @Override
        public SSLSessionContext getSessionContext() {
            return session.getSessionContext();
        }

        @Override
        public long getCreationTime() {
            return session.getCreationTime();
        }

        @Override
        public long getLastAccessedTime() {
            return session.getLastAccessedTime();
        }

        @Override
        public void invalidate() {
            session.invalidate();
        }

        @Override
        public boolean isValid() {
            return session.isValid();
        }

        @Override
        public void putValue(String s, Object o) {
            session.putValue(s, o);
        }

        @Override
        public Object getValue(String s) {
            return session.getValue(s);
        }

        @Override
        public void removeValue(String s) {
            session.removeValue(s);
        }

        @Override
        public String[] getValueNames() {
            return session.getValueNames();
        }

        @Override
        public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
            return certificates;
        }

        @Override
        public Certificate[] getLocalCertificates() {
            return session.getLocalCertificates();
        }
        @Override
        public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
            return session.getPeerPrincipal();
        }

        @Override
        public Principal getLocalPrincipal() {
            return session.getLocalPrincipal();
        }

        @Override
        public String getCipherSuite() {
            return session.getCipherSuite();
        }

        @Override
        public String getProtocol() {
            return session.getProtocol();
        }

        @Override
        public String getPeerHost() {
            return session.getPeerHost();
        }

        @Override
        public int getPeerPort() {
            return session.getPeerPort();
        }

        @Override
        public int getPacketBufferSize() {
            return session.getPacketBufferSize();
        }

        @Override
        public int getApplicationBufferSize() {
            return session.getApplicationBufferSize();
        }
        @SuppressWarnings("removal")
        @Override
        public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
            return session.getPeerCertificateChain();
        }
    };

}
