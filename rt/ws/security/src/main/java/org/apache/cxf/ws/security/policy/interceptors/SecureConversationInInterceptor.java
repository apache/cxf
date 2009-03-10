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

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
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
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.ProtectionToken;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
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
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.SecurityContextToken;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.apache.xml.security.utils.Base64;

class SecureConversationInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    static final Logger LOG = LogUtils.getL7dLogger(SecureConversationInInterceptor.class);

    
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
                
                Object s = message.getContextualProperty(SecurityConstants.STS_TOKEN_DO_CANCEL);
                if (s != null && (Boolean.TRUE.equals(s) || "true".equalsIgnoreCase(s.toString()))) {
                    message.getInterceptorChain().add(SecureConversationCancelInterceptor.INSTANCE);
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
                    PolicyAssertion ass = SecureConversationTokenInterceptorProvider
                        .getAddressingPolicy(aim, false);
                    all.addPolicyComponent(ass);
                    ea.addPolicyComponent(all);
                    SymmetricBinding binding = new SymmetricBinding(SP12Constants.INSTANCE);
                    binding.setIncludeTimestamp(true);
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
                    message.getInterceptorChain().add(SecureConversationTokenFinderInterceptor.INSTANCE);
                } else {
                    Policy p = new Policy();
                    ExactlyOne ea = new ExactlyOne();
                    p.addPolicyComponent(ea);
                    All all = new All();
                    PolicyAssertion ass = SecureConversationTokenInterceptorProvider
                        .getAddressingPolicy(aim, false);
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
                message.getInterceptorChain().add(SecureConversationTokenFinderInterceptor.INSTANCE);
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
                                                  policy,
                                                  null);
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
                throw new Fault("Unknown SecureConversation element: " + requestEl.getLocalName(),
                                LOG);
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
                new SecurityContextToken(SecureConversationTokenInterceptorProvider
                                              .getWSCVersion(tokenType), writer.getDocument());
            
            Calendar created = Calendar.getInstance();
            Calendar expires = Calendar.getInstance();
            expires.setTimeInMillis(System.currentTimeMillis() + ttl);

            SecurityToken token = new SecurityToken(sct.getIdentifier(), created, expires);
            token.setToken(sct.getElement());
            token.setTokenType(WSConstants.WSC_SCT);
            
            writer.getCurrentNode().appendChild(sct.getElement());
            writer.writeEndElement();        
            
            writer.writeStartElement(prefix, "RequestedAttachedReference", namespace);
            token.setAttachedReference(SecureConversationTokenInterceptorProvider
                                           .writeSecurityTokenReference(writer,
                                                                   "#" + sct.getID(), 
                                                                   tokenType));
            writer.writeEndElement();
            
            writer.writeStartElement(prefix, "RequestedUnattachedReference", namespace);
            token.setUnattachedReference(SecureConversationTokenInterceptorProvider
                                             .writeSecurityTokenReference(writer,
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

            byte[] secret = SecureConversationTokenInterceptorProvider.writeProofToken(prefix, 
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
    
    
    static final class SecureConversationTokenFinderInterceptor 
        extends AbstractPhaseInterceptor<SoapMessage> {
        
        static final SecureConversationTokenFinderInterceptor INSTANCE 
            = new SecureConversationTokenFinderInterceptor();
        
        private SecureConversationTokenFinderInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
        }

        public void handleMessage(SoapMessage message) throws Fault {
            //Find the SC token
            boolean found = false;
            List results = (List)message.get(WSHandlerConstants.RECV_RESULTS);
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
                        found = true;
                    }
                }
            }
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.SECURE_CONVERSATION_TOKEN);
                if (ais == null || ais.isEmpty()) {
                    return;
                }
                for (AssertionInfo inf : ais) {
                    if (found) {
                        inf.setAsserted(true);
                    } else {
                        inf.setNotAsserted("No SecureConversation token found in message.");
                    }
                }
            }
        }
    }
    
    static class SecureConversationCancelInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
        static final SecureConversationCancelInterceptor INSTANCE = new SecureConversationCancelInterceptor();
        
        public SecureConversationCancelInterceptor() {
            super(Phase.POST_LOGICAL);
        }
        
        public void handleMessage(SoapMessage message) throws Fault {
            // TODO Auto-generated method stub
            
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim == null) {
                return;
            }
            Collection<AssertionInfo> ais = aim.get(SP12Constants.SECURE_CONVERSATION_TOKEN);
            if (ais == null || ais.isEmpty()) {
                return;
            }
            
            SecureConversationToken tok = (SecureConversationToken)ais.iterator()
                .next().getAssertion();
            doCancel(message, aim, tok);

        }
        private void doCancel(SoapMessage message, AssertionInfoMap aim, SecureConversationToken itok) {
            Message m2 = message.getExchange().getOutMessage();
            
            SecurityToken tok = (SecurityToken)m2.getContextualProperty(SecurityConstants.TOKEN);
            if (tok == null) {
                String tokId = (String)m2.getContextualProperty(SecurityConstants.TOKEN_ID);
                if (tokId != null) {
                    tok = SecureConversationTokenInterceptorProvider
                        .getTokenStore(m2).getToken(tokId);
                }
            }

            STSClient client = SecureConversationTokenInterceptorProvider.getClient(m2);
            AddressingProperties maps =
                (AddressingProperties)message
                    .get("javax.xml.ws.addressing.context.inbound");
            if (maps == null) {
                maps = (AddressingProperties)m2
                    .get("javax.xml.ws.addressing.context");
            }
            
            synchronized (client) {
                try {
                    SecureConversationTokenInterceptorProvider
                        .setupClient(client, message, aim, itok, true);

                    if (maps != null) {
                        client.setAddressingNamespace(maps.getNamespaceURI());
                    }
                    
                    client.cancelSecurityToken(tok);
                    SecureConversationTokenInterceptorProvider
                        .getTokenStore(m2).remove(tok);
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
    

    
}