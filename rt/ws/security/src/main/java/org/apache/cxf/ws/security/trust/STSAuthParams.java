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

package org.apache.cxf.ws.security.trust;

import javax.xml.namespace.QName;

/**
 * Authentication parameters to obtain SAML token from STS.
 */
public class STSAuthParams {
    private static final String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/";
    private static final String KEY_TYPE_X509 = WS_TRUST_NS + "PublicKey";
    private static final QName X509_ENDPOINT = new QName(WS_TRUST_NS, "X509_Port");
    private static final QName TRANSPORT_ENDPOINT = new QName(WS_TRUST_NS, "Transport_Port");
    private static final QName UT_ENDPOINT = new QName(WS_TRUST_NS, "UT_Port");

    private final AuthMode authMode;
    private final String userName;
    private final String callbackHandler;
    private final String alias;
    private final String keystoreProperties;

    public STSAuthParams(AuthMode authMode, String userName, String callbackHandler) {
        this(authMode, userName, callbackHandler, null, null);
    }

    public STSAuthParams(AuthMode authMode, String userName, String callbackHandler, String alias,
                      String keystoreProperties) {
        this.authMode = authMode;
        this.userName = userName;
        this.callbackHandler = callbackHandler;
        this.alias = alias;
        this.keystoreProperties = keystoreProperties;
    }

    public AuthMode getAuthMode() {
        return authMode;
    }
    public String getUserName() {
        return userName;
    }
    public String getCallbackHandler() {
        return callbackHandler;
    }
    public String getAlias() {
        return alias;
    }
    public String getKeystoreProperties() {
        return keystoreProperties;
    }

    public enum AuthMode {
        X509_ASSYMETRIC(X509_ENDPOINT, KEY_TYPE_X509),
        UT_TRANSPORT(TRANSPORT_ENDPOINT, null),
        UT_SYMMETRIC(UT_ENDPOINT, null);

        private final QName endpointName;
        private final String keyType;

        AuthMode(QName endpointName, String keyType) {
            this.endpointName = endpointName;
            this.keyType = keyType;
        }

        public QName getEndpointName() {
            return endpointName;
        }

        public String getKeyType() {
            return keyType;
        }
    }
}
