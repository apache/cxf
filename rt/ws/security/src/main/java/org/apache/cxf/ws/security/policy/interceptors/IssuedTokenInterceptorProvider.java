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

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;


import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
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
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;

/**
 * 
 */
public class IssuedTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    public IssuedTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.ISSUED_TOKEN, SP12Constants.ISSUED_TOKEN));
        this.getOutInterceptors().add(new IssuedTokenOutInterceptor());
        this.getOutFaultInterceptors().add(new IssuedTokenOutInterceptor());
        this.getInInterceptors().add(new IssuedTokenInInterceptor());
        this.getInFaultInterceptors().add(new IssuedTokenInInterceptor());
    }
    
    
    static final TokenStore getTokenStore(Message message) {
        TokenStore tokenStore = (TokenStore)message.getContextualProperty(TokenStore.class.getName());
        if (tokenStore == null) {
            tokenStore = new MemoryTokenStore();
            message.getExchange().get(Endpoint.class).getEndpointInfo()
                .setProperty(TokenStore.class.getName(), tokenStore);
        }
        return tokenStore;
    }
    static STSClient getClient(Message message) {
        STSClient client = (STSClient)message
            .getContextualProperty(SecurityConstants.STS_CLIENT);
        if (client == null) {
            client = new STSClient(message.getExchange().get(Bus.class));
            Endpoint ep = message.getExchange().get(Endpoint.class);
            client.setEndpointName(ep.getEndpointInfo().getName().toString() + ".sts-client");
            client.setBeanName(ep.getEndpointInfo().getName().toString() + ".sts-client");
        }
        
        // Transpose ActAs info from original request to the STS client.
        client.setActAs(message.getContextualProperty(SecurityConstants.STS_TOKEN_ACT_AS));
        
        return client;
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
                    
                    SecurityToken tok = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
                    if (tok == null) {
                        String tokId = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                        if (tokId != null) {
                            tok = getTokenStore(message).getToken(tokId);
                        }
                    }
                    if (tok == null) {
                        STSClient client = getClient(message);
                        AddressingProperties maps =
                            (AddressingProperties)message
                                .get("javax.xml.ws.addressing.context.outbound");
                        if (maps == null) {
                            maps = (AddressingProperties)message
                                .get("javax.xml.ws.addressing.context");
                        }
                        synchronized (client) {
                            try {
                                client.setTrust(getTrust10(aim));
                                client.setTrust(getTrust13(aim));
                                client.setTemplate(itok.getRstTemplate());
                                if (maps == null) {
                                    tok = client.requestSecurityToken();
                                } else {
                                    Object o = message
                                        .getContextualProperty(SecurityConstants.STS_APPLIES_TO);
                                    String s = o == null ? null : o.toString();
                                    s = s == null 
                                        ? message.getContextualProperty(Message.ENDPOINT_ADDRESS).toString()
                                            : s;
                                    client.setAddressingNamespace(maps.getNamespaceURI());
                                    tok = client.requestSecurityToken(s);
                                }
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
    }
    
    static class IssuedTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        public IssuedTokenInInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
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
                    boolean found = false;
                    Vector results = (Vector)message.get(WSHandlerConstants.RECV_RESULTS);
                    for (int i = 0; i < results.size(); i++) {
                        WSHandlerResult rResult =
                                (WSHandlerResult) results.get(i);

                        Vector wsSecEngineResults = rResult.getResults();

                        for (int j = 0; j < wsSecEngineResults.size(); j++) {
                            //WSSecurityEngineResult wser =
                            //        (WSSecurityEngineResult) wsSecEngineResults.get(j);
                            //Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                            //how to find if it's due to an IssuedToken?
                            found = true;
                        }
                    }
                    for (AssertionInfo inf : ais) {
                        inf.setAsserted(found);
                    }
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
    }
}
