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

import java.util.List;

import javax.net.ssl.SSLSocketFactory;

/**
 * This class extends {@link TLSParameterBase} with client-specific
 * SSL/TLS parameters.
 * 
 */
public class TLSClientParameters extends TLSParameterBase {
    private boolean disableCNCheck;
    private SSLSocketFactory sslSocketFactory;
    private int sslCacheTimeout = 86400;
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
     * Returns the SSL cache timeout in seconds if it has been configured or the default value
     */
    public int getSslCacheTimeout() {
        return sslCacheTimeout;
    }

    /**
     * This sets the SSL Session Cache timeout value in seconds for client sessions handled by CXF
     */
    public void setSslCacheTimeout(int sslCacheTimeout) {
        this.sslCacheTimeout = sslCacheTimeout;
    }

    
    /**
     * Returns whether or not {@link javax.net.ssl.HttpsURLConnection#getDefaultSSLSocketFactory()} should be
     * used to create https connections. If <code>true</code> , {@link #getJsseProvider()} ,
     * {@link #getSecureSocketProtocol()}, {@link #getTrustManagers()}, {@link #getKeyManagers()},
     * {@link #getSecureRandom()}, {@link #getCipherSuites()} and {@link #getCipherSuitesFilter()} are
     * ignored.
     */
    public boolean isUseHttpsURLConnectionDefaultSslSocketFactory() {
        return useHttpsURLConnectionDefaultSslSocketFactory;
    }

    /**
     * Sets whether or not {@link javax.net.ssl.HttpsURLConnection#getDefaultSSLSocketFactory()} should be
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
    
    public int hashCode() {
        int hash = disableCNCheck ? 37 : 17;
        if (sslSocketFactory != null) {
            hash = hash * 41 + System.identityHashCode(sslSocketFactory);
        }
        hash = hash(hash, useHttpsURLConnectionDefaultSslSocketFactory);
        hash = hash(hash, useHttpsURLConnectionDefaultHostnameVerifier);
        hash = hash(hash, sslCacheTimeout);
        hash = hash(hash, secureRandom);
        hash = hash(hash, protocol);
        hash = hash(hash, certAlias);
        hash = hash(hash, provider);
        for (String cs : ciphersuites) {
            hash = hash(hash, cs);
        }
        hash = hash(hash, keyManagers);
        hash = hash(hash, trustManagers);
        if (cipherSuiteFilters != null) {
            hash = hash(hash, cipherSuiteFilters.getInclude());
            hash = hash(hash, cipherSuiteFilters.getExclude());
        }
        if (certConstraints != null) {
            hash = hash(hash, certConstraints.getIssuerDNConstraints());
            hash = hash(hash, certConstraints.getSubjectDNConstraints());
        }
        return hash;
    }
    private int hash(int i, Object o) {
        if (o != null) {
            i = i * 37 + o.hashCode();
        }
        return i;
    }
    private int hash(int i, Object[] os) {
        if (os == null) {
            return i;
        }
        for (Object o : os) {
            i = hash(i, o);
        }
        return i;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TLSClientParameters) {
            TLSClientParameters that = (TLSClientParameters)o;
            boolean eq = disableCNCheck == that.disableCNCheck;
            eq &= sslSocketFactory == that.sslSocketFactory;
            eq &= useHttpsURLConnectionDefaultSslSocketFactory == that.useHttpsURLConnectionDefaultSslSocketFactory;
            eq &= useHttpsURLConnectionDefaultHostnameVerifier == that.useHttpsURLConnectionDefaultHostnameVerifier;
            eq &= sslCacheTimeout == that.sslCacheTimeout;
            eq &= secureRandom == that.secureRandom;
            eq &= equals(certAlias, that.certAlias);
            eq &= equals(protocol, that.protocol);
            eq &= equals(provider, that.provider);
            eq &= equals(ciphersuites, that.ciphersuites);
            eq &= equals(keyManagers, that.keyManagers);
            eq &= equals(trustManagers, that.trustManagers);
            if (cipherSuiteFilters != null) {
                if (that.cipherSuiteFilters != null) {
                    eq &= equals(cipherSuiteFilters.getExclude(), that.cipherSuiteFilters.getExclude());
                    eq &= equals(cipherSuiteFilters.getInclude(), that.cipherSuiteFilters.getInclude());
                } else {
                    eq = false;
                }
            } else {
                eq &= that.cipherSuiteFilters == null;
            }
            if (certConstraints != null) {
                if (that.certConstraints != null) {
                    eq &= equals(certConstraints.getIssuerDNConstraints(), 
                                 that.certConstraints.getIssuerDNConstraints());
                    eq &= equals(certConstraints.getSubjectDNConstraints(),
                                 that.certConstraints.getSubjectDNConstraints());
                } else {
                    eq = false;
                }
            } else {
                eq &= that.certConstraints == null;
            }
            return eq;
        }
        return false;
    }
    
    private static boolean equals(final List<?> obj1, final List<?> obj2) {
        if (obj1.size() == obj2.size()) {
            for (int x = 0; x < obj1.size(); x++) {
                if (!equals(obj1.get(x), obj2.get(x))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    private static boolean equals(final Object obj1, final Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }
    private static boolean equals(final Object[] a1, final Object[] a2) {
        if (a1 == null) {
            return a2 == null;
        } else {
            if (a2 != null && a1.length == a2.length) {
                for (int i = 0; i < a1.length; i++) {
                    if (!equals(a1[i], a2[i])) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
