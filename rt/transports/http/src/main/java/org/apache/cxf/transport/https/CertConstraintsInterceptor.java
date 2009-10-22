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

import java.net.HttpURLConnection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException; 

/**
 * An interceptor that enforces certificate constraints logic at the TLS layer.
 */
public final class CertConstraintsInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final CertConstraintsInterceptor INSTANCE = new CertConstraintsInterceptor();
    
    static final Logger LOG = LogUtils.getL7dLogger(CertConstraintsInterceptor.class);
    
    private CertConstraintsInterceptor() {
        super(Phase.PRE_STREAM);
    }

    public void handleMessage(Message message) throws Fault {
        final CertConstraints certConstraints 
            = (CertConstraints)message.getContextualProperty(CertConstraints.class.getName());
        if (certConstraints == null) {
            return;
        }
        
        if (isRequestor(message)) {
            try {
                HttpURLConnection connection = 
                    (HttpURLConnection) message.get("http.connection");
                
                if (connection instanceof HttpsURLConnection) {
                    final MessageTrustDecider orig = message.get(MessageTrustDecider.class);
                    MessageTrustDecider trust = new MessageTrustDecider() {
                        public void establishTrust(String conduitName,
                                URLConnectionInfo connectionInfo,
                                Message message)
                            throws UntrustedURLConnectionIOException {
                            if (orig != null) {
                                orig.establishTrust(conduitName, connectionInfo, message);
                            }
                            HttpsURLConnectionInfo info = (HttpsURLConnectionInfo)connectionInfo;

                            if (info.getServerCertificates() == null 
                                    || info.getServerCertificates().length == 0) {
                                throw new UntrustedURLConnectionIOException(
                                    "No server certificates were found"
                                );
                            } else {
                                X509Certificate[] certs = (X509Certificate[])info.getServerCertificates();
                                if (!certConstraints.matches(certs[0])) {
                                    throw new UntrustedURLConnectionIOException(
                                        "The server certificate(s) do not match the defined cert constraints"
                                    );
                                }
                            }
                        }
                    };
                    message.put(MessageTrustDecider.class, trust);
                } else {
                    throw new UntrustedURLConnectionIOException(
                        "TLS is not in use"
                    );
                }
            } catch (UntrustedURLConnectionIOException ex) {
                throw new Fault(ex);
            }
        } else {
            try {
                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                final Certificate[] certs = tlsInfo.getPeerCertificates();
                if (certs == null || certs.length == 0) {
                    throw new UntrustedURLConnectionIOException(
                        "No client certificates were found"
                    );
                } else {
                    X509Certificate[] x509Certs = (X509Certificate[])certs;
                    if (!certConstraints.matches(x509Certs[0])) {
                        throw new UntrustedURLConnectionIOException(
                            "The client certificate does not match the defined cert constraints"
                        );
                    }
                }
            } catch (UntrustedURLConnectionIOException ex) {
                throw new Fault(ex);
            }
        }
    }
 
}
        