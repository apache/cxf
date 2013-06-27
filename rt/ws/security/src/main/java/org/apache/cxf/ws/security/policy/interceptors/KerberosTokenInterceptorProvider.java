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
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.cxf.ws.security.wss4j.KerberosTokenInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.policyvalidators.KerberosTokenPolicyValidator;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.utils.Base64;

/**
 * 
 */
public class KerberosTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final long serialVersionUID = 5922028830873137490L;

    public KerberosTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.KERBEROS_TOKEN, SP12Constants.KERBEROS_TOKEN));
       
        this.getOutInterceptors().add(new KerberosTokenOutInterceptor());
        this.getOutFaultInterceptors().add(new KerberosTokenOutInterceptor());
        this.getInInterceptors().add(new KerberosTokenInInterceptor());
        this.getInFaultInterceptors().add(new KerberosTokenInInterceptor());
        
        this.getOutInterceptors().add(new KerberosTokenInterceptor());
        this.getInInterceptors().add(new KerberosTokenInterceptor());
    }
    
    
    static final TokenStore getTokenStore(Message message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore = 
                (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null) {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                String cacheKey = SecurityConstants.TOKEN_STORE_CACHE_INSTANCE;
                if (info.getName() != null) {
                    cacheKey += "-" + info.getName().toString().hashCode();
                }
                tokenStore = tokenStoreFactory.newTokenStore(cacheKey, message);
                info.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
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
                    SecurityToken tok = null;
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
                    if (tok != null) {
                        for (AssertionInfo ai : ais) {
                            ai.setAsserted(true);
                        }
                        message.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN_ID, 
                                                                      tok.getId());
                        message.getExchange().put(SecurityConstants.TOKEN_ID, 
                                                  tok.getId());
                        getTokenStore(message).add(tok);
                        
                        // Create another cache entry with the SHA1 Identifier as the key for easy retrieval
                        if (tok.getSHA1() != null) {
                            getTokenStore(message).add(tok.getSHA1(), tok);
                        }
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
                    getTokenStore(message).add(token);
                    message.getExchange().put(SecurityConstants.TOKEN_ID, token.getId());
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
        byte[] tokenBytes = binarySecurityToken.getToken();
        try {
            token.setSHA1(Base64.encode(WSSecurityUtil.generateDigest(tokenBytes)));
        } catch (WSSecurityException e) {
            // Just consume this for now as it isn't critical...
        }
        return token;
    }
        
}
