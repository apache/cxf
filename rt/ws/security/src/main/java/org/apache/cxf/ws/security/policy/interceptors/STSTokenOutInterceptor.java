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

import java.util.HashMap;
import java.util.Map;
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
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSTokenRetriever;
import org.apache.cxf.ws.security.trust.STSTokenRetriever.TokenRequestParams;

public class STSTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(STSTokenOutInterceptor.class);
    private static final String TOKEN_TYPE_SAML_2_0 = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String KEY_TYPE_X509 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/";
    private static final QName STS_SERVICE_NAME = new QName(WS_TRUST_NS, "SecurityTokenService");
    private static final QName X509_ENDPOINT = new QName(WS_TRUST_NS, "X509_Port");
    private static final QName TRANSPORT_ENDPOINT = new QName(WS_TRUST_NS, "Transport_Port");
    
    private STSClient stsClient;
    private TokenRequestParams tokenParams;

    public STSTokenOutInterceptor(AuthParams authParams, String stsWsdlLocation, Bus bus) {
        super(Phase.PREPARE_SEND);
        this.stsClient = configureBasicSTSClient(authParams, stsWsdlLocation, bus);
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
        SecurityToken tok = STSTokenRetriever.getToken(message, tokenParams);
        if (tok == null) {
            LOG.warning("Security token was not retrieved from STS");
        }
    }
    
    public STSClient getSTSClient() {
        return stsClient;
    }
    
    public static enum AuthMode {
        X509(X509_ENDPOINT, KEY_TYPE_X509), 
        TRANSPORT(TRANSPORT_ENDPOINT, null);
        
        private final QName endpointName;
        private final String keyType;
        
        private AuthMode(QName endpointName, String keyType) {
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
    
    private STSClient configureBasicSTSClient(AuthParams authParams, String stsWsdlLocation, Bus bus) {
        STSClient basicStsClient = new STSClient(bus);
        basicStsClient.setWsdlLocation(stsWsdlLocation);
        basicStsClient.setServiceName(STS_SERVICE_NAME.toString());
        basicStsClient.setEndpointName(authParams.getAuthMode().endpointName.toString());
        if (authParams.getAuthMode().getKeyType() != null) {
            basicStsClient.setKeyType(authParams.getAuthMode().getKeyType());
        } else {
            basicStsClient.setSendKeyType(false);
        }
        basicStsClient.setTokenType(TOKEN_TYPE_SAML_2_0);
        basicStsClient.setAllowRenewingAfterExpiry(true);
        basicStsClient.setEnableLifetime(true);

        Map<String, Object> props = new HashMap<String, Object>();
        if (authParams.getUserName() != null) {
            props.put(SecurityConstants.USERNAME, authParams.getUserName());
        }
        props.put(SecurityConstants.CALLBACK_HANDLER, authParams.getCallbackHandler());
        if ((authParams.getKeystoreProperties() != null) && (authParams.getKeystoreProperties() != null)) {
            props.put(SecurityConstants.ENCRYPT_USERNAME, authParams.getAlias());
            props.put(SecurityConstants.ENCRYPT_PROPERTIES, authParams.getKeystoreProperties());
            props.put(SecurityConstants.SIGNATURE_PROPERTIES, authParams.getKeystoreProperties());
            props.put(SecurityConstants.STS_TOKEN_USERNAME, authParams.getAlias());
            props.put(SecurityConstants.STS_TOKEN_PROPERTIES, authParams.getKeystoreProperties());
            props.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, "true");
        }
        basicStsClient.setProperties(props);
        
        return basicStsClient;
    }
}
