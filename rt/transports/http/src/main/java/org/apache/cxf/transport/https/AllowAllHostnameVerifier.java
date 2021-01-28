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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Allow all hostnames. This is only suitable for use in testing, and NOT in production!
 */
class AllowAllHostnameVerifier implements javax.net.ssl.HostnameVerifier {

    private static final Logger LOG = LogUtils.getL7dLogger(AllowAllHostnameVerifier.class);

    @Override
    public boolean verify(String host, SSLSession session) {
        try {
            Certificate[] certs = session.getPeerCertificates();
            return certs != null && certs[0] instanceof X509Certificate;
        } catch (SSLException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, e.getMessage(), e);
            }
            return false;
        }
    }

    public boolean verify(final String host, final String certHostname) {
        return certHostname != null && !certHostname.isEmpty();
    }
}