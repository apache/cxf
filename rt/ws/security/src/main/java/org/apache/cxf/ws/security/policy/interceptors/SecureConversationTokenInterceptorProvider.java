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
import java.util.Map;


import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;

/**
 * 
 */
public class SecureConversationTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    public SecureConversationTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.SECURE_CONVERSATION_TOKEN,
                            SP12Constants.SECURE_CONVERSATION_TOKEN));
        this.getOutInterceptors().add(new SecureConversationOutInterceptor());
        this.getOutFaultInterceptors().add(new SecureConversationOutInterceptor());
        this.getInInterceptors().add(new SecureConversationInInterceptor());
        this.getInFaultInterceptors().add(new SecureConversationInInterceptor());
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
            client.setBeanName(message.getExchange().get(Endpoint.class)
                               .getEndpointInfo().getName().toString() + ".sct-client");
        }
        return client;
    }
    static class SecureConversationOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
        public SecureConversationOutInterceptor() {
            super(Phase.PREPARE_SEND);
        }
        public void handleMessage(SoapMessage message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.SECURE_CONVERSATION_TOKEN);
                if (ais == null || ais.isEmpty()) {
                    return;
                }
                if (isRequestor(message)) {
                    SecureConversationToken itok = (SecureConversationToken)ais.iterator()
                        .next().getAssertion();
                    
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
                                Policy pol = itok.getBootstrapPolicy();
                                if (maps != null) {
                                    Policy p = new Policy();
                                    ExactlyOne ea = new ExactlyOne();
                                    p.addPolicyComponent(ea);
                                    All all = new All();
                                    all.addPolicyComponent(getAddressingPolicy(aim));
                                    ea.addPolicyComponent(all);
                                    pol = p.merge(pol);
                                }
                                
                                client.setPolicy(pol);
                                client.setSoap11(message.getVersion() == Soap11.getInstance());
                                client.setSecureConv(true);
                                String s = message
                                    .getContextualProperty(Message.ENDPOINT_ADDRESS).toString();
                                client.setLocation(s);
                                
                                Map<String, Object> ctx = client.getRequestContext();
                                mapSecurityProps(message, ctx);
                                if (maps == null) {
                                    tok = client.requestSecurityToken();
                                } else {
                                    client.setAddressingNamespace(maps.getNamespaceURI());
                                    tok = client.requestSecurityToken();
                                }
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new Fault(e);
                            } finally {
                                client.setTrust((Trust10)null);
                                client.setTrust((Trust13)null);
                                client.setTemplate(null);
                                client.setLocation(null);
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
        
        
        private PolicyAssertion getAddressingPolicy(AssertionInfoMap aim) {
            Collection<AssertionInfo> lst = aim.get(MetadataConstants.USING_ADDRESSING_2004_QNAME);
            if (null != lst && !lst.isEmpty()) {
                return lst.iterator().next().getAssertion();
            }
            lst = aim.get(MetadataConstants.USING_ADDRESSING_2005_QNAME);
            if (null != lst && !lst.isEmpty()) {
                return lst.iterator().next().getAssertion();
            }
            lst = aim.get(MetadataConstants.USING_ADDRESSING_2006_QNAME);
            if (null != lst && !lst.isEmpty()) {
                return lst.iterator().next().getAssertion();
            }
            return new PrimitiveAssertion(MetadataConstants.USING_ADDRESSING_2006_QNAME, 
                                          false);
        }
        private void mapSecurityProps(Message message, Map<String, Object> ctx) {
            for (String s : SecurityConstants.ALL_PROPERTIES) {
                Object v = message.getContextualProperty(s + ".sct");
                if (v != null) {
                    ctx.put(s, v);
                }
            }
        }

        private Trust10 getTrust10(AssertionInfoMap aim) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.TRUST_10);
            if (ais == null || ais.isEmpty()) {
                ais = aim.get(SP11Constants.TRUST_10);
            }
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
    
    static class SecureConversationInInterceptor extends AbstractPhaseInterceptor<Message> {
        public SecureConversationInInterceptor() {
            super(Phase.PRE_PROTOCOL);
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.SECURE_CONVERSATION_TOKEN);
                if (ais == null) {
                    return;
                }
                if (!isRequestor(message)) {
                    //TODO
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
