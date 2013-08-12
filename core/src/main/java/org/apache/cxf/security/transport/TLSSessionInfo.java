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

package org.apache.cxf.security.transport;

import java.security.cert.Certificate;
import javax.net.ssl.SSLSession;

/**
 * An immutable struct that contains information about a negotiated
 * TLS Session, including the (potentially negotiated) peer certificates
 * as well as the currently effective TLS ciper suite.
 */
public class TLSSessionInfo {

    private final SSLSession sslSession;
    private final Certificate[] peerCertificates;
    private final String cipherSuite;
    
    /**
     * This constructor has the effect of calling
     * TLSSessionInfo(null, suite)
     */
    public TLSSessionInfo(
        final String suite
    ) {
        this(suite, null, null);
    }
    
    /**
     * @param       suite
     *              The negotiated cipher suite
     *              This parameter may not be null, by contract
     *
     * @param       session
     *              The JSSE representation of the SSL Session
     *              negotiated with the peer (optionally null, if
     *              it is unavailable)
     *
     * @param       certs
     *              the peer X.509 certificate chain (optinally null)
     */
    public TLSSessionInfo(
        final String suite,
        final SSLSession session,
        final Certificate[] certs
    ) {
        assert suite != null;
        cipherSuite = suite;
        sslSession = session;
        peerCertificates = certs;
    }

    /**
     * @return      the negotiated cipher suite.  This attribute is
     *              guaranteed to be non-null.
     */
    public final String getChipherSuite() {
        return cipherSuite;
    }

    /**
     * @return      the peer X.509 certificate chain, as negotiated
     *              though the TLS handshake.  This attribute may be
     *              null, for example, if the SSL peer has not been
     *              authenticated.
     */
    public final Certificate[] getPeerCertificates() {
        return peerCertificates;
    }

    /**
     * @return      the negotiated SSL Session.  This attribute may be
     *              null if it is unavailable from the underlying
     *              transport.
     */
    public final SSLSession getSSLSession() {
        return sslSession;
    }
}
