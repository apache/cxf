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

package org.apache.cxf.ws.security.policy.interceptors;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.DefaultSTSTokenCacher;
import org.apache.cxf.ws.security.trust.STSAuthParams;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSTokenCacher;
import org.apache.cxf.ws.security.trust.STSTokenRetriever;
import org.apache.cxf.ws.security.trust.STSTokenRetriever.TokenRequestParams;
import org.apache.cxf.ws.security.trust.STSUtils;

public class STSTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(STSTokenOutInterceptor.class);
    private static final String KEY_TYPE_X509 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/";
    private static final QName X509_ENDPOINT = new QName(WS_TRUST_NS, "X509_Port");
    private static final QName TRANSPORT_ENDPOINT = new QName(WS_TRUST_NS, "Transport_Port");
    private static final QName UT_ENDPOINT = new QName(WS_TRUST_NS, "UT_Port");

    private STSClient stsClient;
    private TokenRequestParams tokenParams;
    private STSTokenCacher tokenCacher = new DefaultSTSTokenCacher();

    public STSTokenOutInterceptor(STSAuthParams authParams, String stsWsdlLocation, Bus bus) {
        this(Phase.PREPARE_SEND, authParams, stsWsdlLocation, bus);
    }

    public STSTokenOutInterceptor(String phase, STSAuthParams authParams, String stsWsdlLocation, Bus bus) {
        super(phase);
        this.stsClient = STSUtils.createSTSClient(authParams, stsWsdlLocation, bus);
        this.tokenParams = new TokenRequestParams();
    }

    public STSTokenOutInterceptor(STSClient stsClient) {
        this(Phase.PREPARE_SEND, stsClient, new TokenRequestParams());
    }

    public STSTokenOutInterceptor(STSClient stsClient, TokenRequestParams tokenParams) {
        this(Phase.PREPARE_SEND, stsClient, tokenParams);
    }

    public STSTokenOutInterceptor(String phase, STSClient stsClient, TokenRequestParams tokenParams) {
        super(phase);
        this.stsClient = stsClient;
        this.tokenParams = tokenParams;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        if (stsClient != null) {
            message.put(SecurityConstants.STS_CLIENT, stsClient);
        }
        SecurityToken tok = STSTokenRetriever.getToken(message, tokenParams, tokenCacher);
        if (tok == null) {
            LOG.warning("Security token was not retrieved from STS");
        }
        processToken(message, tok);
    }

    // An extension point to allow custom processing of the token
    protected void processToken(Message message, SecurityToken tok) {

    }

    public STSClient getSTSClient() {
        return stsClient;
    }

    public STSTokenCacher getTokenCacher() {
        return tokenCacher;
    }

    public void setTokenCacher(STSTokenCacher tokenCacher) {
        this.tokenCacher = tokenCacher;
    }

    /**
     * A enumeration to specify authentication mode in communication with STS.
     * @deprecated use {@link org.apache.cxf.ws.security.trust.STSAuthParams.AuthMode}
     */
    @Deprecated
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

    /**
     * A class to specify authentication parameters for communication with STS.
     * @deprecated use {@link org.apache.cxf.ws.security.trust.STSAuthParams}
     */
    @Deprecated
    public static class AuthParams {
        private final AuthMode authMode;
        private final String userName;
        private final String callbackHandler;
        private final String alias;
        private final String keystoreProperties;

        public AuthParams(AuthMode authMode, String userName, String callbackHandler) {
            this(authMode, userName, callbackHandler, null, null);
        }

        public AuthParams(AuthMode authMode, String userName, String callbackHandler, String alias,
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
    }
}
