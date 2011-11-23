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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.IssuedToken;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.policyvalidators.IssuedTokenPolicyValidator;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * 
 */
public class IssuedTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    
    private static final String ASSOCIATED_TOKEN = 
        IssuedTokenInterceptorProvider.class.getName() + "-" + "Associated_Token";

    public IssuedTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.ISSUED_TOKEN, SP12Constants.ISSUED_TOKEN));
        
        //issued tokens can be attached as a supporting token without
        //any type of binding.  Make sure we can support that.
        this.getOutInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getOutFaultInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getInInterceptors().add(PolicyBasedWSS4JInInterceptor.INSTANCE);
        this.getInFaultInterceptors().add(PolicyBasedWSS4JInInterceptor.INSTANCE);
        
        this.getOutInterceptors().add(new IssuedTokenOutInterceptor());
        this.getOutFaultInterceptors().add(new IssuedTokenOutInterceptor());
        this.getInInterceptors().add(new IssuedTokenInInterceptor());
        this.getInFaultInterceptors().add(new IssuedTokenInInterceptor());
    }
    
    static final TokenStore createTokenStore(Message message) {
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
    static final TokenStore getTokenStore(Message message) {
        TokenStore tokenStore = (TokenStore)message.getContextualProperty(TokenStore.class.getName());
        if (tokenStore == null) {
            tokenStore = createTokenStore(message);
        }
        return tokenStore;
    }

    static class IssuedTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        public IssuedTokenOutInterceptor() {
            super(Phase.PREPARE_SEND);
        }
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.ISSUED_TOKEN);
                if (ais == null || ais.isEmpty()) {
                    return;
                }
                if (isRequestor(message)) {
                    IssuedToken itok = (IssuedToken)ais.iterator().next().getAssertion();
                    
                    SecurityToken tok = retrieveCachedToken(message);
                    if (tok == null) {
                        STSClient client = STSUtils.getClient(message, "sts");
                        AddressingProperties maps =
                            (AddressingProperties)message
                                .get("javax.xml.ws.addressing.context.outbound");
                        if (maps == null) {
                            maps = (AddressingProperties)message
                                .get("javax.xml.ws.addressing.context");
                        }
                        synchronized (client) {
                            try {
                                // Transpose ActAs/OnBehalfOf info from original request to the STS client.
                                Object token = 
                                    message.getContextualProperty(SecurityConstants.STS_TOKEN_ACT_AS);
                                if (token != null) {
                                    client.setActAs(token);
                                }
                                token = 
                                    message.getContextualProperty(SecurityConstants.STS_TOKEN_ON_BEHALF_OF);
                                if (token != null) {
                                    client.setOnBehalfOf(token);
                                }
                                
                                Object o = message.getContextualProperty(SecurityConstants.STS_APPLIES_TO);
                                String appliesTo = o == null ? null : o.toString();
                                appliesTo = appliesTo == null 
                                    ? message.getContextualProperty(Message.ENDPOINT_ADDRESS).toString()
                                        : appliesTo;
                                boolean enableAppliesTo = client.isEnableAppliesTo();
                                
                                client.setMessage(message);
                                Element onBehalfOfToken = client.getOnBehalfOfToken();
                                Element actAsToken = client.getActAsToken();
                                
                                SecurityToken secToken = 
                                    handleDelegation(
                                        message, onBehalfOfToken, actAsToken, appliesTo, enableAppliesTo
                                    );
                                if (secToken == null) {
                                    secToken = getTokenFromSTS(message, client, aim, maps, itok, appliesTo);
                                }
                                tok = secToken;
                                storeDelegationTokens(
                                    message, tok, onBehalfOfToken, actAsToken, appliesTo, enableAppliesTo
                                );
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new Fault(e);
                            } finally {
                                client.setTrust((Trust10)null);
                                client.setTrust((Trust13)null);
                                client.setTemplate(null);
                                client.setAddressingNamespace(null);
                            }
                        }
                    } else {
                        //renew token?
                    }
                    if (tok != null) {
                        for (AssertionInfo ai : ais) {
                            ai.setAsserted(true);
                        }
                        boolean cacheIssuedToken = 
                            MessageUtils.getContextualBoolean(
                                message, SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, true
                            );
                        if (cacheIssuedToken) {
                            message.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN_ID, 
                                                                          tok.getId());
                            message.getExchange().put(SecurityConstants.TOKEN_ID, tok.getId());
                        } else {
                            message.put(SecurityConstants.TOKEN_ID, tok.getId());
                        }
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
        private Trust10 getTrust10(AssertionInfoMap aim) {
            Collection<AssertionInfo> ais = aim.get(SP11Constants.TRUST_10);
            if (ais == null || ais.isEmpty()) {
                return null;
            }
            return (Trust10)ais.iterator().next().getAssertion();
        }
        private Trust13 getTrust13(AssertionInfoMap aim) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.TRUST_13);
            if (ais == null || ais.isEmpty()) {
                return null;
            }
            return (Trust13)ais.iterator().next().getAssertion();
        }
        
        private SecurityToken retrieveCachedToken(Message message) {
            boolean cacheIssuedToken = 
                MessageUtils.getContextualBoolean(
                    message, SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, true
                );
            SecurityToken tok = null;
            if (cacheIssuedToken) {
                tok = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
                if (tok == null) {
                    String tokId = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                    if (tokId != null) {
                        tok = getTokenStore(message).getToken(tokId);
                    }
                }
            } else {
                tok = (SecurityToken)message.get(SecurityConstants.TOKEN);
                if (tok == null) {
                    String tokId = (String)message.get(SecurityConstants.TOKEN_ID);
                    if (tokId != null) {
                        tok = getTokenStore(message).getToken(tokId);
                    }
                }
            }
            return tok;
        }
        
        /**
         * Parse ActAs/OnBehalfOf appropriately. See if the required token is stored in the cache.
         */
        private SecurityToken handleDelegation(
            Message message, 
            Element onBehalfOfToken,
            Element actAsToken,
            String appliesTo,
            boolean enableAppliesTo
        ) throws Exception {
            TokenStore tokenStore = getTokenStore(message);
            String key = appliesTo;
            if (!enableAppliesTo || key == null || "".equals(key)) {
                key = ASSOCIATED_TOKEN;
            }
            // See if the token corresponding to the OnBehalfOf Token is stored in the cache
            // and if it points to an issued token
            if (onBehalfOfToken != null) {
                String id = getIdFromToken(onBehalfOfToken);
                SecurityToken cachedToken = tokenStore.getToken(id);
                if (cachedToken != null) {
                    Properties properties = cachedToken.getProperties();
                    if (properties != null && properties.containsKey(key)) {
                        String associatedToken = properties.getProperty(key);
                        SecurityToken issuedToken = tokenStore.getToken(associatedToken);
                        if (issuedToken != null) {
                            return issuedToken;
                        }
                    }
                }
            }
            
            // See if the token corresponding to the ActAs Token is stored in the cache
            // and if it points to an issued token
            if (actAsToken != null) {
                String id = getIdFromToken(actAsToken);
                SecurityToken cachedToken = tokenStore.getToken(id);
                if (cachedToken != null) {
                    Properties properties = cachedToken.getProperties();
                    if (properties != null && properties.containsKey(key)) {
                        String associatedToken = properties.getProperty(key);
                        SecurityToken issuedToken = tokenStore.getToken(associatedToken);
                        if (issuedToken != null) {
                            return issuedToken;
                        }
                    }
                }
            }
            return null;
        }
        
        private String getIdFromToken(Element token) {
            if (token != null) {
                // Try to find the "Id" on the token.
                if (token.hasAttributeNS(WSConstants.WSU_NS, "Id")) {
                    return token.getAttributeNS(WSConstants.WSU_NS, "Id");
                } else if (token.hasAttributeNS(null, "ID")) {
                    return token.getAttributeNS(null, "ID");
                } else if (token.hasAttributeNS(null, "AssertionID")) {
                    return token.getAttributeNS(null, "AssertionID");
                }
            }
            return "";
        }
        
        private void storeDelegationTokens(
            Message message,
            SecurityToken issuedToken,
            Element onBehalfOfToken,
            Element actAsToken,
            String appliesTo,
            boolean enableAppliesTo
        ) throws Exception {
            if (issuedToken == null) {
                return;
            }
            TokenStore tokenStore = getTokenStore(message);
            String key = appliesTo;
            if (!enableAppliesTo || key == null || "".equals(key)) {
                key = ASSOCIATED_TOKEN;
            }
            if (onBehalfOfToken != null) {
                String id = getIdFromToken(onBehalfOfToken);
                SecurityToken cachedToken = tokenStore.getToken(id);
                if (cachedToken == null) {
                    cachedToken = new SecurityToken(id);
                    cachedToken.setToken(onBehalfOfToken);
                }
                Properties properties = cachedToken.getProperties();
                if (properties == null) {
                    properties = new Properties();
                    cachedToken.setProperties(properties);
                }
                properties.put(key, issuedToken.getId());
                tokenStore.add(cachedToken);
            }
            if (actAsToken != null) {
                String id = getIdFromToken(actAsToken);
                SecurityToken cachedToken = tokenStore.getToken(id);
                if (cachedToken == null) {
                    cachedToken = new SecurityToken(id);
                    cachedToken.setToken(actAsToken);
                }
                Properties properties = cachedToken.getProperties();
                if (properties == null) {
                    properties = new Properties();
                    cachedToken.setProperties(properties);
                }
                properties.put(key, issuedToken.getId());
                tokenStore.add(cachedToken);
            }
        }
        
        private SecurityToken getTokenFromSTS(
            Message message,
            STSClient client,
            AssertionInfoMap aim,
            AddressingProperties maps,
            IssuedToken itok,
            String appliesTo
        ) throws Exception {
            client.setTrust(getTrust10(aim));
            client.setTrust(getTrust13(aim));
            client.setTemplate(itok.getRstTemplate());
            if (maps == null) {
                return client.requestSecurityToken();
            } else {
                client.setAddressingNamespace(maps.getNamespaceURI());
                return client.requestSecurityToken(appliesTo);
            }
        }
        
    }
    
    static class IssuedTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        public IssuedTokenInInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
            addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.ISSUED_TOKEN);
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
            List<WSSecurityEngineResult> signedResults = new ArrayList<WSSecurityEngineResult>();
            WSSecurityUtil.fetchAllActionResults(
                rResult.getResults(), WSConstants.SIGN, signedResults
            );
            
            IssuedTokenPolicyValidator issuedValidator = 
                new IssuedTokenPolicyValidator(signedResults, message);
            Collection<AssertionInfo> issuedAis = aim.get(SP12Constants.ISSUED_TOKEN);

            for (AssertionWrapper assertionWrapper : findSamlTokenResults(rResult.getResults())) {
                boolean valid = issuedValidator.validatePolicy(issuedAis, assertionWrapper);
                if (valid) {
                    SecurityToken token = createSecurityToken(assertionWrapper);
                    message.getExchange().put(SecurityConstants.TOKEN, token);
                    return;
                }
            }
            for (BinarySecurity binarySecurityToken : findBinarySecurityTokenResults(rResult.getResults())) {
                boolean valid = issuedValidator.validatePolicy(issuedAis, binarySecurityToken);
                if (valid) {
                    SecurityToken token = createSecurityToken(binarySecurityToken);
                    message.getExchange().put(SecurityConstants.TOKEN, token);
                    return;
                }
            }
        }
        
        private List<AssertionWrapper> findSamlTokenResults(
            List<WSSecurityEngineResult> wsSecEngineResults
        ) {
            List<AssertionWrapper> results = new ArrayList<AssertionWrapper>();
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                if (actInt.intValue() == WSConstants.ST_SIGNED
                    || actInt.intValue() == WSConstants.ST_UNSIGNED) {
                    results.add((AssertionWrapper)wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION));
                }
            }
            return results;
        }
        
        private List<BinarySecurity> findBinarySecurityTokenResults(
            List<WSSecurityEngineResult> wsSecEngineResults
        ) {
            List<BinarySecurity> results = new ArrayList<BinarySecurity>();
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                if (actInt.intValue() == WSConstants.BST) {
                    results.add((BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN));
                }
            }
            return results;
        }
        
        private SecurityToken createSecurityToken(
            AssertionWrapper assertionWrapper
        ) {
            SecurityToken token = new SecurityToken(assertionWrapper.getId());

            SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
            if (subjectKeyInfo != null) {
                token.setSecret(subjectKeyInfo.getSecret());
                X509Certificate[] certs = subjectKeyInfo.getCerts();
                if (certs != null && certs.length > 0) {
                    token.setX509Certificate(certs[0], null);
                }
            }
            if (assertionWrapper.getSaml1() != null) {
                token.setTokenType(WSConstants.WSS_SAML_TOKEN_TYPE);
            } else if (assertionWrapper.getSaml2() != null) {
                token.setTokenType(WSConstants.WSS_SAML2_TOKEN_TYPE);
            }
            token.setToken(assertionWrapper.getElement());

            return token;
        }
    
        private SecurityToken createSecurityToken(BinarySecurity binarySecurityToken) {
            SecurityToken token = new SecurityToken(binarySecurityToken.getID());
            token.setToken(binarySecurityToken.getElement());
            token.setSecret(binarySecurityToken.getToken());
            token.setTokenType(binarySecurityToken.getValueType());
    
            return token;
        }
        
    }
        
}
