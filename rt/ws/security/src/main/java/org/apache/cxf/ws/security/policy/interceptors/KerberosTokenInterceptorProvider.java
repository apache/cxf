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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.kerberos.KerberosClient;
import org.apache.cxf.ws.security.kerberos.KerberosUtils;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.policyvalidators.KerberosTokenPolicyValidator;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;

/**
 * 
 */
public class KerberosTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    public KerberosTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.KERBEROS_TOKEN, SP12Constants.KERBEROS_TOKEN));
        
        this.getOutInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getOutFaultInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getInInterceptors().add(PolicyBasedWSS4JInInterceptor.INSTANCE);
        this.getInFaultInterceptors().add(PolicyBasedWSS4JInInterceptor.INSTANCE);
        
        this.getOutInterceptors().add(new KerberosTokenOutInterceptor());
        this.getOutFaultInterceptors().add(new KerberosTokenOutInterceptor());
        this.getInInterceptors().add(new KerberosTokenInInterceptor());
        this.getInFaultInterceptors().add(new KerberosTokenInInterceptor());
    }
    
    
    static final TokenStore getTokenStore(Message message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore = (TokenStore)message.getContextualProperty(TokenStore.class.getName());
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(TokenStore.class.getName());
            }
            if (tokenStore == null) {
                tokenStore = new MemoryTokenStore();
                info.setProperty(TokenStore.class.getName(), tokenStore);
            }
            return tokenStore;
        }
    }

    static class KerberosTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        public KerberosTokenOutInterceptor() {
            super(Phase.PREPARE_SEND);
        }
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.KERBEROS_TOKEN);
                if (ais == null || ais.isEmpty()) {
                    return;
                }
                if (isRequestor(message)) {
                    SecurityToken tok = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
                    if (tok == null) {
                        String tokId = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                        if (tokId != null) {
                            tok = getTokenStore(message).getToken(tokId);
                        }
                    }
                    if (tok == null) {
                        try {
                            KerberosClient client = KerberosUtils.getClient(message, "kerberos");
                            synchronized (client) {
                                tok = client.requestSecurityToken();
                            }
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new Fault(e);
                        }
                    }
                    if (tok != null) {
                        for (AssertionInfo ai : ais) {
                            ai.setAsserted(true);
                        }
                        message.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN_ID, 
                                                                      tok.getId());
                        message.getExchange().put(SecurityConstants.TOKEN_ID, 
                                                  tok.getId());
                        getTokenStore(message).add(tok);
                    }
                } else {
                    //server side should be checked on the way in
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        
    }
    
    static class KerberosTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        public KerberosTokenInInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
            addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.KERBEROS_TOKEN);
                if (ais == null) {
                    return;
                }
                if (!isRequestor(message)) {
                    List<WSHandlerResult> results = 
                        CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
                    if (results != null && results.size() > 0) {
                        parseHandlerResults(results.get(0), message, aim);
                    }
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        
        private void parseHandlerResults(
            WSHandlerResult rResult,
            Message message,
            AssertionInfoMap aim
        ) {
            List<WSSecurityEngineResult> kerberosResults = findKerberosResults(rResult.getResults());
            for (WSSecurityEngineResult wser : kerberosResults) {
                KerberosSecurity kerberosToken = 
                    (KerberosSecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                KerberosTokenPolicyValidator kerberosValidator = 
                    new KerberosTokenPolicyValidator(message);
                boolean valid = kerberosValidator.validatePolicy(aim, kerberosToken);
                if (valid) {
                    SecurityToken token = createSecurityToken(kerberosToken);
                    token.setSecret((byte[])wser.get(WSSecurityEngineResult.TAG_SECRET));
                    message.getExchange().put(SecurityConstants.TOKEN, token);
                    return;
                }
            }
        }
        
        private List<WSSecurityEngineResult> findKerberosResults(
            List<WSSecurityEngineResult> wsSecEngineResults
        ) {
            List<WSSecurityEngineResult> results = new ArrayList<WSSecurityEngineResult>();
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                if (actInt.intValue() == WSConstants.BST) {
                    BinarySecurity binarySecurity = 
                        (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                    if (binarySecurity instanceof KerberosSecurity) {
                        results.add(wser);
                    }
                }
            }
            return results;
        }
    }
    
    private static SecurityToken createSecurityToken(KerberosSecurity binarySecurityToken) {
        SecurityToken token = new SecurityToken(binarySecurityToken.getID());
        token.setToken(binarySecurityToken.getElement());
        token.setTokenType(binarySecurityToken.getValueType());
        return token;
    }
        
}
