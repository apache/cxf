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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.imageio.IIOException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HttpURLConnectionFactory;
import org.apache.cxf.transport.http.HttpURLConnectionInfo;

/**
 * This HttpsURLConnectionFactory implements the HttpURLConnectionFactory
 * for using the given SSL Policy to configure TLS connections for "https:"
 * URLs.
 * 
 */
public final class HttpsURLConnectionFactory 
    implements HttpURLConnectionFactory {
    
    /**
     * This constant holds the URL Protocol Identifier for HTTPS
     */
    public static final String HTTPS_URL_PROTOCOL_ID = "https";

    private static final long serialVersionUID = 1L;
    private static final Logger LOG =
        LogUtils.getL7dLogger(HttpsURLConnectionFactory.class);

    /*
     *  For development and testing only
     */
    private static final String[] UNSUPPORTED =
    {"SessionCaching", "SessionCacheKey", "MaxChainLength",
     "CertValidator", "ProxyHost", "ProxyPort"};
    
    /*
     *  For development and testing only
     */
    private static final String[] DERIVATIVE = {"CiphersuiteFilters"};
    
    /**
     * This field holds the conduit to which this connection factory
     * is a slave.
     */
    HTTPConduit conduit;
    
    /**
     * This field contains the TLS configuration for the URLs created by
     * this factory.
     */
    TLSClientParameters tlsClientParameters;
    
    
    /**
     * Cache the last SSLContext to avoid recreation
     */
    SSLSocketFactory socketFactory;

    private Class deprecatedSunHttpsURLConnectionClass;

    private Class deprecatedSunHostnameVerifierClass;
    
    /**
     * This constructor initialized the factory with the configured TLS
     * Client Parameters for the HTTPConduit for which this factory is used.
     * 
     * @param params The TLS Client Parameters. This parameter is guaranteed 
     *               to be non-null.
     */
    public HttpsURLConnectionFactory(TLSClientParameters params) {
        tlsClientParameters        = params;
        assert tlsClientParameters != null;
    }
    
    /**
     * Create a HttpURLConnection, proxified if necessary.
     * 
     * 
     * @param proxy This parameter is non-null if connection should be proxied.
     * @param url   The target URL. This parameter must be an https url.
     * 
     * @return The HttpsURLConnection for the given URL.
     * @throws IOException This exception is thrown if 
     *         the "url" is not "https" or other IOException
     *         is thrown. 
     *                     
     */
    public HttpURLConnection createConnection(Proxy proxy, URL url)
        throws IOException {

        if (!url.getProtocol().equals(HTTPS_URL_PROTOCOL_ID)) {
            throw new IOException("Illegal Protocol " 
                    + url.getProtocol() 
                    + " for HTTPS URLConnection Factory.");
        }
        
        HttpURLConnection connection =
            (HttpURLConnection) (proxy != null 
                                   ? url.openConnection(proxy)
                                   : url.openConnection());
                                   
        if (tlsClientParameters != null) {
            Exception ex = null;
            try {
                decorateWithTLS(connection);
            } catch (Exception e) {
                ex = e;
            } finally {
                if (ex != null) {
                    if (ex instanceof IOException) {
                        throw (IOException) ex;
                    }
                    throw new IIOException("Error while initializing secure socket", ex);
                }
            }
        }

        return connection;
    }
    
    /**
     * This method assigns the various TLS parameters on the HttpsURLConnection
     * from the TLS Client Parameters. Connection parameter is of supertype HttpURLConnection, 
     * which allows internal cast to potentially divergent subtype (https) implementations.
     */
    protected synchronized void decorateWithTLS(HttpURLConnection connection)
        throws NoSuchAlgorithmException,
               NoSuchProviderException,
               KeyManagementException {

        // First see if an SSLSocketFactory was set.  This allows easy interop
        // with not-yet-commons-ssl.jar, or even just people who like doing their
        // own JSSE.
        if (socketFactory == null) {
            SSLSocketFactory preSetFactory = tlsClientParameters.getSSLSocketFactory();
            if (preSetFactory != null) {
                socketFactory = preSetFactory;
            }
        }

        // Okay, no SSLSocketFactory available in TLSClientParameters.  Maybe
        // TrustManagers, KeyManagers, etc?
        if (socketFactory == null) {
            String provider = tlsClientParameters.getJsseProvider();
            
            String protocol = tlsClientParameters.getSecureSocketProtocol() != null
                      ? tlsClientParameters.getSecureSocketProtocol()
                      : "TLS";
                      
            SSLContext ctx = provider == null
                      ? SSLContext.getInstance(protocol)
                      : SSLContext.getInstance(protocol, provider);
            
                      

            TrustManager[] trustAllCerts = tlsClientParameters.getTrustManagers();
            /*
            TrustManager[] trustAllCerts = new TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
            */         
            ctx.init(
                tlsClientParameters.getKeyManagers(),
                trustAllCerts, 
                tlsClientParameters.getSecureRandom());
            
            // The "false" argument means opposite of exclude.
            String[] cipherSuites =
                SSLUtils.getCiphersuites(tlsClientParameters.getCipherSuites(),
                                         SSLUtils.getSupportedCipherSuites(ctx),
                                         tlsClientParameters.getCipherSuitesFilter(),
                                         LOG, false);
            // The SSLSocketFactoryWrapper enables certain cipher suites
            // from the policy.
            socketFactory = new SSLSocketFactoryWrapper(ctx.getSocketFactory(),
                                                        cipherSuites,
                                                        tlsClientParameters.getSecureSocketProtocol());
        }
        
        if (connection instanceof HttpsURLConnection) {
            // handle the expected case (javax.net.ssl)
            HttpsURLConnection conn = (HttpsURLConnection) connection;
            if (tlsClientParameters.isDisableCNCheck()) {
                conn.setHostnameVerifier(CertificateHostnameVerifier.ALLOW_ALL);
            } else {
                conn.setHostnameVerifier(CertificateHostnameVerifier.DEFAULT);
            }
            conn.setSSLSocketFactory(socketFactory);
        } else {
            // handle the deprecated sun case
            try {
                Class<?> connectionClass = getDeprecatedSunHttpsURLConnectionClass();
                Class<?> verifierClass = getDeprecatedSunHostnameVerifierClass();
                Method setHostnameVerifier = connectionClass.getMethod("setHostnameVerifier", verifierClass);
                InvocationHandler handler = new InvocationHandler() {
                    public Object invoke(Object proxy, 
                                         Method method, 
                                         Object[] args) throws Throwable {
                        return true;
                    }
                };
                Object proxy = java.lang.reflect.Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                          new Class[] {verifierClass},
                                                                          handler);
                setHostnameVerifier.invoke(connectionClass.cast(connection), verifierClass.cast(proxy));
                Method setSSLSocketFactory = connectionClass.getMethod("setSSLSocketFactory", 
                                                                       SSLSocketFactory.class);
                setSSLSocketFactory.invoke(connectionClass.cast(connection), socketFactory);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error decorating connection class " 
                        + connection.getClass().getName(), ex);
            }
        }
    }

    private Class getDeprecatedSunHttpsURLConnectionClass() throws ClassNotFoundException {
        if (deprecatedSunHttpsURLConnectionClass == null) {
            deprecatedSunHttpsURLConnectionClass = Class.forName("com.sun.net.ssl.HttpsURLConnection");
        }
        return deprecatedSunHttpsURLConnectionClass;
    }

    private Class getDeprecatedSunHostnameVerifierClass() throws ClassNotFoundException {
        if (deprecatedSunHostnameVerifierClass == null) {
            deprecatedSunHostnameVerifierClass = Class.forName("com.sun.net.ssl.HostnameVerifier");
        }
        return deprecatedSunHostnameVerifierClass;
    }

    /*
     *  For development and testing only
     */
    protected void addLogHandler(Handler handler) {
        LOG.addHandler(handler);
    }
       
    protected String[] getUnSupported() {
        return UNSUPPORTED;
    }
    
    protected String[] getDerivative() {
        return DERIVATIVE;
    }

    /**
     * This operation returns an HttpsURLConnectionInfo for the 
     * given HttpsURLConnection. 
     * 
     * @param connection The HttpsURLConnection
     * @return The HttpsURLConnectionInfo object for the given 
     *         HttpsURLConnection.
     * @throws IOException Normal IO Exceptions.
     * @throws ClassCastException If "connection" is not an HttpsURLConnection 
     *         (or a supported subtype of HttpURLConnection)
     */
    public HttpURLConnectionInfo getConnectionInfo(
            HttpURLConnection connection
    ) throws IOException {  
        return new HttpsURLConnectionInfo(connection);
    }
    
    public String getProtocol() {
        return "https";
    }

}



