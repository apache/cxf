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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
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
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;

/**
 * 
 */
public class SecureConversationTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(SecureConversationTokenInterceptorProvider.class);


    public SecureConversationTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.SECURE_CONVERSATION_TOKEN,
                            SP12Constants.SECURE_CONVERSATION_TOKEN));
        this.getOutInterceptors().add(new SecureConversationOutInterceptor());
        this.getOutFaultInterceptors().add(new SecureConversationOutInterceptor());
        this.getInInterceptors().add(new SecureConversationInInterceptor());
        this.getInFaultInterceptors().add(new SecureConversationInInterceptor());
    }
    
    static final Trust10 getTrust10(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRUST_10);
        if (ais == null || ais.isEmpty()) {
            ais = aim.get(SP11Constants.TRUST_10);
        }
        if (ais == null || ais.isEmpty()) {
            return null;
        }
        return (Trust10)ais.iterator().next().getAssertion();
    }
    static final Trust13 getTrust13(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRUST_13);
        if (ais == null || ais.isEmpty()) {
            return null;
        }
        return (Trust13)ais.iterator().next().getAssertion();
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
    private static PolicyAssertion getAddressingPolicy(AssertionInfoMap aim, boolean optional) {
        Collection<AssertionInfo> lst = aim.get(MetadataConstants.USING_ADDRESSING_2004_QNAME);
        PolicyAssertion assertion = null;
        if (null != lst && !lst.isEmpty()) {
            assertion = lst.iterator().next().getAssertion();
        }
        if (assertion == null) {
            lst = aim.get(MetadataConstants.USING_ADDRESSING_2005_QNAME);
            if (null != lst && !lst.isEmpty()) {
                assertion = lst.iterator().next().getAssertion();
            }
        }
        if (assertion == null) {
            lst = aim.get(MetadataConstants.USING_ADDRESSING_2006_QNAME);
            if (null != lst && !lst.isEmpty()) {
                assertion = lst.iterator().next().getAssertion();
            }
        }
        if (assertion == null) {
            return new PrimitiveAssertion(MetadataConstants.USING_ADDRESSING_2006_QNAME,
                                          optional);
        } else if (optional) {
            return new PrimitiveAssertion(assertion.getName(),
                                          optional);            
        }
        return assertion;
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
                                    all.addPolicyComponent(getAddressingPolicy(aim, false));
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
                                    tok = client.requestSecurityToken(s);
                                } else {
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
        
        
        private void mapSecurityProps(Message message, Map<String, Object> ctx) {
            for (String s : SecurityConstants.ALL_PROPERTIES) {
                Object v = message.getContextualProperty(s + ".sct");
                if (v != null) {
                    ctx.put(s, v);
                }
            }
        }

    }
    
    static class SecureConversationInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
        public SecureConversationInInterceptor() {
            super(Phase.PRE_PROTOCOL);
        }

        public void handleMessage(SoapMessage message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.SECURE_CONVERSATION_TOKEN);
                if (ais == null || ais.isEmpty()) {
                    return;
                }
                if (!isRequestor(message)) {
                    String s = (String)message.get(SoapBindingConstants.SOAP_ACTION);
                    
                    if (s != null 
                        && s.contains("/RST/SCT")
                        && (s.startsWith(STSUtils.WST_NS_05_02)
                            || s.startsWith(STSUtils.WST_NS_05_12))) {

                        SecureConversationToken tok = (SecureConversationToken)ais.iterator()
                            .next().getAssertion();
                        Policy pol = tok.getBootstrapPolicy();
                        Policy p = new Policy();
                        ExactlyOne ea = new ExactlyOne();
                        p.addPolicyComponent(ea);
                        All all = new All();
                        PolicyAssertion ass = getAddressingPolicy(aim, false);
                        all.addPolicyComponent(ass);
                        ea.addPolicyComponent(all);
                        pol = p.merge(pol);
                        
                        //setup SCT endpoint and forward to it.
                        unmapSecurityProps(message);
                        String ns = STSUtils.WST_NS_05_12;
                        if (s.startsWith(STSUtils.WST_NS_05_02)) {
                            ns = STSUtils.WST_NS_05_02;
                        }
                        recalcEffectivePolicy(message, ns, pol);
                    }
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        private void recalcEffectivePolicy(SoapMessage message, 
                                           String namespace,
                                           Policy policy) {
            Exchange ex = message.getExchange();
            Bus bus = ex.get(Bus.class);
            PolicyEngine pe = bus.getExtension(PolicyEngine.class);
            if (null == pe) {
                return;
            }
            Destination destination = ex.getDestination();
            try {
                Endpoint endpoint = STSUtils.createSTSEndpoint(bus, 
                                                           namespace,
                                                           null,
                                                           destination.getAddress().getAddress().getValue(),
                                                           message.getVersion().getBindingId(), 
                                                           policy);
            
                EndpointPolicy ep = pe.getServerEndpointPolicy(endpoint.getEndpointInfo(), destination);
                List<Interceptor> interceptors = ep.getInterceptors();
                for (Interceptor i : interceptors) {
                    message.getInterceptorChain().add(i);
                }
                
                Collection<PolicyAssertion> assertions = ep.getVocabulary();
                if (null != assertions) {
                    message.put(AssertionInfoMap.class, new AssertionInfoMap(assertions));
                }
                endpoint.getService().setInvoker(new STSInvoker());
                ex.put(Endpoint.class, endpoint);
                ex.put(Service.class, endpoint.getService());
            } catch (Exception exc) {
                throw new Fault(exc);
            }
        }
        private void unmapSecurityProps(Message message) {
            for (String s : SecurityConstants.ALL_PROPERTIES) {
                Object v = message.getContextualProperty(s + ".sct");
                if (v != null) {
                    message.put(s, v);
                }
            }
        }

        public class STSInvoker implements Invoker {

            public Object invoke(Exchange exchange, Object o) {
                MessageContentsList lst = (MessageContentsList)o;
                DOMSource src = (DOMSource)lst.get(0);
                Node nd = src.getNode();
                Element el = null;
                if (nd instanceof Document) {
                    el = ((Document)nd).getDocumentElement();
                } else {
                    el = (Element)nd;
                }
                String name = el.getLocalName();
                if ("RequestSecurityToken".equals(name)) {
                    XMLUtils.printDOM(el);
                    el = DOMUtils.getFirstElement(el);
                    while (el != null) {
                        
                        
                        el = DOMUtils.getNextElement(el);
                    }
                    
                    
                    return lst;
                } else {
                    throw new Fault("Unknown SecureConversation request type: " + name, LOG);
                }
            }

        }
    }
}
