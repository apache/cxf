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

import java.security.cert.X509Certificate;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;

final class HttpsMessageTrustDecider extends MessageTrustDecider {
    private final CertConstraints certConstraints;
    private final MessageTrustDecider orig;

    HttpsMessageTrustDecider(CertConstraints certConstraints,
            MessageTrustDecider orig) {
        this.certConstraints = certConstraints;
        this.orig = orig;
    }

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
        }
        X509Certificate[] certs = (X509Certificate[])info.getServerCertificates();
        if (!certConstraints.matches(certs[0])) {
            throw new UntrustedURLConnectionIOException(
                "The server certificate(s) do not match the defined cert constraints"
            );
        }
    }
}