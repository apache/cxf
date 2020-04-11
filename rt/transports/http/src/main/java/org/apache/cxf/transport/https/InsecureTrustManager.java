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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.common.logging.LogUtils;

/**
 * This class provides a static method to create an array of TrustManagers, which disables TLS
 * trust verification. This is insecure and must not be used in production, only for testing!
 */
public final class InsecureTrustManager {

    private static final Logger LOG = LogUtils.getL7dLogger(InsecureTrustManager.class);

    private InsecureTrustManager() {
        // complete
    }

    public static TrustManager[] getNoOpX509TrustManagers() {
        LOG.warning("This class essentially disables TLS trust verification and is insecure!");
        return new TrustManager[] {new NoOpX509TrustManager()};
    }

    private static final class NoOpX509TrustManager implements X509TrustManager {

        private NoOpX509TrustManager() {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    }
}
