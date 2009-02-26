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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
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
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
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
import org.apache.cxf.ws.security.policy.SPConstants.SupportTokenType;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.ProtectionToken;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.SymmetricBinding;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.conversation.dkalgo.P_SHA1;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.message.token.SecurityContextToken;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.apache.xml.security.utils.Base64;

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

    static String setupClient(STSClient client,
                            SoapMessage message,
                            AssertionInfoMap aim,
                            SecureConversationToken itok,
                            boolean endorse) {
        client.setTrust(getTrust10(aim));
        client.setTrust(getTrust13(aim));
        Policy pol = itok.getBootstrapPolicy();
        Policy p = new Policy();
        ExactlyOne ea = new ExactlyOne();
        p.addPolicyComponent(ea);
        All all = new All();
        all.addPolicyComponent(getAddressingPolicy(aim, false));
        ea.addPolicyComponent(all);
        
        if (endorse) {
            SupportingToken st = new SupportingToken(SupportTokenType.SUPPORTING_TOKEN_ENDORSING,
                                                     SP12Constants.INSTANCE);
            st.addToken(itok);
            all.addPolicyComponent(st);
        }
        pol = p.merge(pol);
        
        client.setPolicy(pol);
        client.setSoap11(message.getVersion() == Soap11.getInstance());
        client.setSecureConv(true);
        String s = message
            .getContextualProperty(Message.ENDPOINT_ADDRESS).toString();
        client.setLocation(s);
        
        Map<String, Object> ctx = client.getRequestContext();
        mapSecurityProps(message, ctx);
        return s;
    }
    private static void mapSecurityProps(Message message, Map<String, Object> ctx) {
        for (String s : SecurityConstants.ALL_PROPERTIES) {
            Object v = message.getContextualProperty(s + ".sct");
            if (v != null) {
                ctx.put(s, v);
            }
        }
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
                        tok = issueToken(message, aim, itok);
                    } else {
                        renewToken(message, aim, tok, itok);
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
        
        
        private void renewToken(SoapMessage message,
                                AssertionInfoMap aim, 
                                SecurityToken tok,
                                SecureConversationToken itok) {
            if (tok.getState() != SecurityToken.State.EXPIRED) {
                return;
            }
            
            STSClient client = getClient(message);
            AddressingProperties maps =
                (AddressingProperties)message
                    .get("javax.xml.ws.addressing.context.outbound");
            if (maps == null) {
                maps = (AddressingProperties)message
                    .get("javax.xml.ws.addressing.context");
            } else if (maps.getAction().getValue().endsWith("Renew")) {
                return;
            }
            synchronized (client) {
                try {
                    setupClient(client, message, aim, itok, true);

                    String s = message
                        .getContextualProperty(Message.ENDPOINT_ADDRESS).toString();
                    client.setLocation(s);
                    
                    Map<String, Object> ctx = client.getRequestContext();
                    ctx.put(SecurityConstants.TOKEN, tok);
                    if (maps != null) {
                        client.setAddressingNamespace(maps.getNamespaceURI());
                    }
                    client.renewSecurityToken(tok);
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
        }
        private SecurityToken issueToken(SoapMessage message,
                                         AssertionInfoMap aim,
                                         SecureConversationToken itok) {
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
                    String s = setupClient(client, message, aim, itok, false);

                    SecurityToken tok = null;
                    if (maps != null) {
                        client.setAddressingNamespace(maps.getNamespaceURI());
                    }
                    tok = client.requestSecurityToken(s);
                    tok.setTokenType(WSConstants.WSC_SCT);
                    return tok;
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
        }

    }
    static class SecureConversationTokenFinderInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
        public SecureConversationTokenFinderInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
        }

        public void handleMessage(SoapMessage message) throws Fault {
            //Find the SC token
            Vector results = (Vector)message.get(WSHandlerConstants.RECV_RESULTS);
            for (int i = 0; i < results.size(); i++) {
                WSHandlerResult rResult =
                        (WSHandlerResult) results.get(i);

                Vector wsSecEngineResults = rResult.getResults();

                for (int j = 0; j < wsSecEngineResults.size(); j++) {
                    WSSecurityEngineResult wser =
                            (WSSecurityEngineResult) wsSecEngineResults.get(j);
                    Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                    if (actInt.intValue() == WSConstants.SCT) {
                        SecurityContextToken tok
                            = (SecurityContextToken)wser
                                .get(WSSecurityEngineResult.TAG_SECURITY_CONTEXT_TOKEN);
                        message.getExchange().put(SecurityConstants.TOKEN_ID, tok.getID());
                    }
                }
            }
        }
    }
    static class SecureConversationInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
        public SecureConversationInInterceptor() {
            super(Phase.PRE_PROTOCOL);
        }
        private Binding getBinding(AssertionInfoMap aim) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
            if (ais != null && !ais.isEmpty()) {
                return (Binding)ais.iterator().next().getAssertion();
            }
            ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
            if (ais != null && !ais.isEmpty()) {
                return (Binding)ais.iterator().next().getAssertion();
            }
            ais = aim.get(SP12Constants.TRANSPORT_BINDING);
            if (ais != null && !ais.isEmpty()) {
                return (Binding)ais.iterator().next().getAssertion();
            }
            return null;
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
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                    return;
                }
                String s = (String)message.get(SoapBindingConstants.SOAP_ACTION);
                String addNs = null;
                AddressingProperties inProps = (AddressingProperties)message
                    .getContextualProperty(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND);
                if (inProps != null) {
                    addNs = inProps.getNamespaceURI();
                    if (s == null) {
                        //MS/WCF doesn't put a soap action out for this, must check the headers
                        s = inProps.getAction().getValue();
                    }
                }

                if (s != null 
                    && s.contains("/RST/SCT")
                    && (s.startsWith(STSUtils.WST_NS_05_02)
                        || s.startsWith(STSUtils.WST_NS_05_12))) {

                    SecureConversationToken tok = (SecureConversationToken)ais.iterator()
                        .next().getAssertion();
                    Policy pol = tok.getBootstrapPolicy();
                    if (s.endsWith("Cancel") || s.endsWith("/Renew")) {
                        //Cancel and Renew just sign with the token
                        Policy p = new Policy();
                        ExactlyOne ea = new ExactlyOne();
                        p.addPolicyComponent(ea);
                        All all = new All();
                        PolicyAssertion ass = getAddressingPolicy(aim, false);
                        all.addPolicyComponent(ass);
                        ea.addPolicyComponent(all);
                        SymmetricBinding binding = new SymmetricBinding(SP12Constants.INSTANCE);
                        ProtectionToken token = new ProtectionToken(SP12Constants.INSTANCE);
                        token.setToken(new SecureConversationToken(SP12Constants.INSTANCE));
                        binding.setProtectionToken(token);
                        binding.setEntireHeadersAndBodySignatures(true);
                        
                        Binding origBinding = getBinding(aim);
                        binding.setAlgorithmSuite(origBinding.getAlgorithmSuite());
                        all.addPolicyComponent(binding);
                        
                        SignedEncryptedParts parts = new SignedEncryptedParts(true, 
                                                                              SP12Constants.INSTANCE);
                        parts.setBody(true);
                        if (addNs != null) {
                            parts.addHeader(new Header("To", addNs));
                            parts.addHeader(new Header("From", addNs));
                            parts.addHeader(new Header("FaultTo", addNs));
                            parts.addHeader(new Header("ReplyTO", addNs));
                            parts.addHeader(new Header("MessageID", addNs));
                            parts.addHeader(new Header("RelatesTo", addNs));
                            parts.addHeader(new Header("Action", addNs));
                        }
                        all.addPolicyComponent(parts);
                        pol = p;
                    } else {
                        Policy p = new Policy();
                        ExactlyOne ea = new ExactlyOne();
                        p.addPolicyComponent(ea);
                        All all = new All();
                        PolicyAssertion ass = getAddressingPolicy(aim, false);
                        all.addPolicyComponent(ass);
                        ea.addPolicyComponent(all);
                        pol = p.merge(pol);
                    }
                    
                    //setup SCT endpoint and forward to it.
                    unmapSecurityProps(message);
                    String ns = STSUtils.WST_NS_05_12;
                    if (s.startsWith(STSUtils.WST_NS_05_02)) {
                        ns = STSUtils.WST_NS_05_02;
                    }
                    recalcEffectivePolicy(message, ns, pol);
                } else {
                    message.getInterceptorChain().add(new SecureConversationTokenFinderInterceptor());
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
                Endpoint endpoint = message.getExchange().get(Endpoint.class);
                TokenStore store = (TokenStore)message.getContextualProperty(TokenStore.class.getName());
                if (store == null) {
                    store = new MemoryTokenStore();
                    endpoint.getEndpointInfo().setProperty(TokenStore.class.getName(), store);
                }
                endpoint = STSUtils.createSTSEndpoint(bus, 
                                                      namespace,
                                                      null,
                                                      destination.getAddress().getAddress().getValue(),
                                                      message.getVersion().getBindingId(), 
                                                      policy);
                endpoint.getEndpointInfo().setProperty(TokenStore.class.getName(), store);
            
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
            Exchange ex = message.getExchange();
            for (String s : SecurityConstants.ALL_PROPERTIES) {
                Object v = message.getContextualProperty(s + ".sct");
                if (v != null) {
                    ex.put(s, v);
                }
            }
        }

        public class STSInvoker implements Invoker {

            public Object invoke(Exchange exchange, Object o) {
                AddressingProperties inProps = (AddressingProperties)exchange.getInMessage()
                    .getContextualProperty(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND);
                if (inProps != null) {
                    AddressingProperties props = new AddressingPropertiesImpl(inProps.getNamespaceURI());
                    AttributedURIType action = new AttributedURIType();
                    action.setValue(inProps.getAction().getValue().replace("/RST/", "/RSTR/"));
                    props.setAction(action);
                    exchange.getOutMessage().put(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND,
                                                 props);
                }
                
                MessageContentsList lst = (MessageContentsList)o;
                DOMSource src = (DOMSource)lst.get(0);
                Node nd = src.getNode();
                Element requestEl = null;
                if (nd instanceof Document) {
                    requestEl = ((Document)nd).getDocumentElement();
                } else {
                    requestEl = (Element)nd;
                }
                String namespace = requestEl.getNamespaceURI();
                String prefix = requestEl.getPrefix();
                SecurityToken cancelToken = null;
                if ("RequestSecurityToken".equals(requestEl.getLocalName())) {
                    try {
                        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
                        writer.setNsRepairing(true);
                        if (STSUtils.WST_NS_05_12.equals(namespace)) {
                            writer.writeStartElement(prefix, "RequestSecurityTokenResponseCollection",
                                                     namespace);
                        }
                        writer.writeStartElement(prefix, "RequestSecurityTokenResponse", namespace);
                        
                        String requestType = null;
                        Element el = DOMUtils.getFirstElement(requestEl);
                        while (el != null) {
                            String localName = el.getLocalName();
                            if (namespace.equals(el.getNamespaceURI())) {
                                if ("RequestType".equals(localName)) {
                                    requestType = el.getTextContent();
                                } else if ("CancelTarget".equals(localName)) {
                                    cancelToken = findCancelToken(exchange, el);
                                }
                            }
                            
                            el = DOMUtils.getNextElement(el);
                        }
                        if (requestType == null) {
                            requestType = "/Issue";
                        }
                        
                        if (requestType.endsWith("/Issue")) { 
                            doIssue(requestEl, exchange, writer, prefix, namespace);
                        } else if (requestType.endsWith("/Cancel")) {
                            TokenStore store = (TokenStore)exchange.get(Endpoint.class).getEndpointInfo()
                                .getProperty(TokenStore.class.getName());
                            cancelToken.setState(SecurityToken.State.CANCELLED);
                            store.update(cancelToken);
                            writer.writeEmptyElement(prefix, "RequestedTokenCancelled", namespace);
                            exchange.put(SecurityConstants.TOKEN, cancelToken);
                        } else if (requestType.endsWith("/Renew")) {
                            //REVISIT - implement
                        }
                        writer.writeEndElement();
                        if (STSUtils.WST_NS_05_12.equals(namespace)) {
                            writer.writeEndElement();
                        }
                        return new MessageContentsList(new DOMSource(writer.getDocument()));
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new Fault(ex);
                    }
                } else {
                    throw new Fault("Unknown SecureConversation element: " + requestEl.getLocalName(), LOG);
                }
            }

            private void doIssue(Element requestEl,
                                 Exchange exchange, W3CDOMStreamWriter writer,
                                 String prefix, String namespace) 
                throws Exception {
                byte clientEntropy[] = null;
                int keySize = 256;
                int ttl = 300000;
                String tokenType = null;
                Element el = DOMUtils.getFirstElement(requestEl);
                while (el != null) {
                    String localName = el.getLocalName();
                    if (namespace.equals(el.getNamespaceURI())) {
                        if ("Entropy".equals(localName)) {
                            Element bs = DOMUtils.getFirstElement(el);
                            if (bs != null) {
                                clientEntropy = Base64.decode(bs.getTextContent());
                            }
                        } else if ("KeySize".equals(localName)) {
                            keySize = Integer.parseInt(el.getTextContent());
                        } else if ("TokenType".equals(localName)) {
                            tokenType = el.getTextContent();
                        }
                    }
                    
                    el = DOMUtils.getNextElement(el);
                }
                
                writer.writeStartElement(prefix, "RequestedSecurityToken", namespace);
                SecurityContextToken sct =
                    new SecurityContextToken(getWSCVersion(tokenType), writer.getDocument());
                
                Calendar created = Calendar.getInstance();
                Calendar expires = Calendar.getInstance();
                expires.setTimeInMillis(System.currentTimeMillis() + ttl);

                SecurityToken token = new SecurityToken(sct.getIdentifier(), created, expires);
                token.setToken(sct.getElement());
                token.setTokenType(WSConstants.WSC_SCT);
                
                writer.getCurrentNode().appendChild(sct.getElement());
                writer.writeEndElement();        
                
                writer.writeStartElement(prefix, "RequestedAttachedReference", namespace);
                token.setAttachedReference(writeSecurityTokenReference(writer,
                                                                       "#" + sct.getID(), 
                                                                       tokenType));
                writer.writeEndElement();
                
                writer.writeStartElement(prefix, "RequestedUnattachedReference", namespace);
                token.setUnattachedReference(writeSecurityTokenReference(writer,
                                                                         sct.getIdentifier(),
                                                                         tokenType));
                writer.writeEndElement();
                
                XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
                writer.writeStartElement(prefix, "Lifetime", namespace);
                writer.writeNamespace("wsu", WSConstants.WSU_NS);
                writer.writeStartElement("wsu", "Created", WSConstants.WSU_NS);
                writer.writeCharacters(fmt.format(created.getTime()));
                writer.writeEndElement();
                
                writer.writeStartElement("wsu", "Expires", WSConstants.WSU_NS);
                writer.writeCharacters(fmt.format(expires.getTime()));
                writer.writeEndElement();
                writer.writeEndElement();

                byte[] secret = writeProofToken(prefix, 
                                                namespace,
                                                writer,
                                                clientEntropy, 
                                                keySize);
                token.setSecret(secret);
                ((TokenStore)exchange.get(Endpoint.class).getEndpointInfo()
                        .getProperty(TokenStore.class.getName())).add(token);
            }

            private SecurityToken findCancelToken(Exchange exchange, Element el) throws WSSecurityException {
                SecurityTokenReference ref = new SecurityTokenReference(DOMUtils.getFirstElement(el));
                String uri = ref.getReference().getURI();
                TokenStore store = (TokenStore)exchange.get(Endpoint.class).getEndpointInfo()
                        .getProperty(TokenStore.class.getName());
                return store.getToken(uri);
            }

        }
    }
    private static byte[] writeProofToken(String prefix, 
                                          String namespace,
                                          W3CDOMStreamWriter writer,
                                          byte[] clientEntropy,
                                          int keySize) 
        throws NoSuchAlgorithmException, WSSecurityException, ConversationException, XMLStreamException {
        byte secret[] = null; 
        writer.writeStartElement(prefix, "RequestedProofToken", namespace);
        if (clientEntropy == null) {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            secret = new byte[keySize / 8];
            random.nextBytes(secret);
            
            writer.writeStartElement(prefix, "BinarySecret", namespace);
            writer.writeAttribute("Type", namespace + "/Nonce");
            writer.writeCharacters(Base64.encode(secret));
            writer.writeEndElement();
        } else {
            byte entropy[] = WSSecurityUtil.generateNonce(keySize / 8);
            P_SHA1 psha1 = new P_SHA1();
            secret = psha1.createKey(clientEntropy,
                                     entropy,
                                     0,
                                     keySize / 8);

            writer.writeStartElement(prefix, "ComputedKey", namespace);
            writer.writeCharacters(namespace + "/CK/PSHA1");            
            writer.writeEndElement();
            writer.writeEndElement();

            writer.writeStartElement(prefix, "Entropy", namespace);
            writer.writeStartElement(prefix, "BinarySecret", namespace);
            writer.writeAttribute("Type", namespace + "/Nonce");
            writer.writeCharacters(Base64.encode(entropy));
            writer.writeEndElement();
            
        }
        writer.writeEndElement();
        return secret;
    }
    
    private static Element writeSecurityTokenReference(W3CDOMStreamWriter writer,
                                                    String id,
                                                    String refValueType) {

        Reference ref = new Reference(writer.getDocument());
        ref.setURI(id);
        if (refValueType != null) {
            ref.setValueType(refValueType);
        }
        SecurityTokenReference str = new SecurityTokenReference(writer.getDocument());
        str.setReference(ref);

        writer.getCurrentNode().appendChild(str.getElement());
        return str.getElement();
    }

    
    private static int getWSCVersion(String tokenTypeValue) throws ConversationException {

        if (tokenTypeValue == null) {
            return ConversationConstants.DEFAULT_VERSION;
        }

        if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_02)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_02);
        } else if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_12)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_12);
        } else {
            throw new ConversationException("unsupportedSecConvVersion");
        }
    }

}
