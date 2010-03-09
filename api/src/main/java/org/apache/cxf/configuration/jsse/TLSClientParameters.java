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
package org.apache.cxf.configuration.jsse;

import javax.net.ssl.SSLSocketFactory;

/**
 * This class extends {@link TLSParameterBase} with client-specific
 * SSL/TLS parameters.
 * 
 */
public class TLSClientParameters extends TLSParameterBase {
    private boolean disableCNCheck;
    private SSLSocketFactory sslSocketFactory;
    private boolean useHttpsURLConnectionDefaultSslSocketFactory;
    private boolean useHttpsURLConnectionDefaultHostnameVerifier;

    /**
     * Set whether or not JSEE should omit checking if the host name
     * specified in the URL matches that of the Common Name
     * (CN) on the server's certificate. Default is false;  
     * this attribute should not be set to true during production use.
     */
    public void setDisableCNCheck(boolean disableCNCheck) {
        this.disableCNCheck = disableCNCheck;
    }

    /**
     * Returns whether or not JSSE omits checking if the
     * host name specified in the URL matches that of the Common Name
     * (CN) on the server's certificate.
     */
    public boolean isDisableCNCheck() {
        return disableCNCheck;
    }

    /**
     * This sets the SSLSocketFactory to use, causing all other properties of
     * this bean (and its superclass) to get ignored (this takes precendence).
     */
    public final void setSSLSocketFactory(SSLSocketFactory factory) {
        sslSocketFactory = factory;
    }


    /**
     * Returns the SSLSocketFactory to be used, or null if none has been set.
     */
    public final SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }
    
    /**
     * Returns whether or not {@link javax.net.ssl.HttpsURLConnection#getDefaultSSLSocketFactory()}Êshould be
     * used to create https connections. If <code>true</code> , {@link #getJsseProvider()} ,
     * {@link #getSecureSocketProtocol()}, {@link #getTrustManagers()}, {@link #getKeyManagers()},
     * {@link #getSecureRandom()}, {@link #getCipherSuites()} and {@link #getCipherSuitesFilter()} are
     * ignored.
     */
    public boolean isUseHttpsURLConnectionDefaultSslSocketFactory() {
        return useHttpsURLConnectionDefaultSslSocketFactory;
    }

    /**
     * Sets whether or not {@link javax.net.ssl.HttpsURLConnection#getDefaultSSLSocketFactory()}Êshould be
     * used to create https connections.
     * 
     * @see #isUseHttpsURLConnectionDefaultSslSocketFactory()
     */
    public void setUseHttpsURLConnectionDefaultSslSocketFactory(
                      boolean useHttpsURLConnectionDefaultSslSocketFactory) {
        this.useHttpsURLConnectionDefaultSslSocketFactory = useHttpsURLConnectionDefaultSslSocketFactory;
    }

    /**
     * Returns whether or not {@link javax.net.ssl.HttpsURLConnection#getDefaultHostnameVerifier()} should be
     * used to create https connections. If <code>true</code>, {@link #isDisableCNCheck()} is ignored.
     */
    public boolean isUseHttpsURLConnectionDefaultHostnameVerifier() {
        return useHttpsURLConnectionDefaultHostnameVerifier;
    }

    /**
     * Sets whether or not {@link javax.net.ssl.HttpsURLConnection#getDefaultHostnameVerifier()} should be
     * used to create https connections.
     * 
     * @see #isUseHttpsURLConnectionDefaultHostnameVerifier()
     */
    public void setUseHttpsURLConnectionDefaultHostnameVerifier(
                      boolean useHttpsURLConnectionDefaultHostnameVerifier) {
        this.useHttpsURLConnectionDefaultHostnameVerifier = useHttpsURLConnectionDefaultHostnameVerifier;
    }
}
