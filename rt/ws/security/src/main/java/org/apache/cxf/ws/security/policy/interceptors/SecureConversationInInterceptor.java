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

import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyBuilder;
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
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.ws.security.message.token.SecurityContextToken;
import org.apache.ws.security.util.Base64;

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
                    Assertion ass = NegotiationUtils.getAddressingPolicy(aim, false);
                    all.addPolicyComponent(ass);
                    ea.addPolicyComponent(all);
                    PolicyBuilder pbuilder = message.getExchange().getBus()
                        .getExtension(PolicyBuilder.class);
                    SymmetricBinding binding = new SymmetricBinding(SP12Constants.INSTANCE, pbuilder);
                    binding.setIncludeTimestamp(true);
                    ProtectionToken token = new ProtectionToken(SP12Constants.INSTANCE, pbuilder);
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
                    Assertion ass = NegotiationUtils.getAddressingPolicy(aim, false);
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
                NegotiationUtils.recalcEffectivePolicy(message, ns, pol, new SecureConversationSTSInvoker());
            } else {
                message.getInterceptorChain().add(SecureConversationTokenFinderInterceptor.INSTANCE);
            }
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

    public class SecureConversationSTSInvoker extends STSInvoker {

        void doIssue(
            Element requestEl,
            Exchange exchange,
            Element binaryExchange,
            W3CDOMStreamWriter writer,
            String prefix, 
            String namespace
        ) throws Exception {
            if (STSUtils.WST_NS_05_12.equals(namespace)) {
                writer.writeStartElement(prefix, "RequestSecurityTokenResponseCollection", namespace);
            }
            writer.writeStartElement(prefix, "RequestSecurityTokenResponse", namespace);
            
            byte clientEntropy[] = null;
            int keySize = 256;
            long ttl = 300000L;
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
            
            // Check received KeySize
            if (keySize < 128 || keySize > 512) {
                keySize = 256;
            }
            
            writer.writeStartElement(prefix, "RequestedSecurityToken", namespace);
            SecurityContextToken sct =
                new SecurityContextToken(NegotiationUtils.getWSCVersion(tokenType), writer.getDocument());
            
            Date created = new Date();
            Date expires = new Date();
            expires.setTime(created.getTime() + ttl);
            
            SecurityToken token = new SecurityToken(sct.getIdentifier(), created, expires);
            token.setToken(sct.getElement());
            token.setTokenType(sct.getTokenType());
            
            writer.getCurrentNode().appendChild(sct.getElement());
            writer.writeEndElement();        
            
            writer.writeStartElement(prefix, "RequestedAttachedReference", namespace);
            token.setAttachedReference(
                writeSecurityTokenReference(writer, "#" + sct.getID(), tokenType)
            );
            writer.writeEndElement();
            
            writer.writeStartElement(prefix, "RequestedUnattachedReference", namespace);
            token.setUnattachedReference(
                writeSecurityTokenReference(writer, sct.getIdentifier(), tokenType)
            );
            writer.writeEndElement();
            
            writeLifetime(writer, created, expires, prefix, namespace);

            byte[] secret = writeProofToken(prefix, namespace, writer, clientEntropy, keySize);
            
            token.setSecret(secret);
            ((TokenStore)exchange.get(Endpoint.class).getEndpointInfo()
                    .getProperty(TokenStore.class.getName())).add(token);
            
            writer.writeEndElement();
            if (STSUtils.WST_NS_05_12.equals(namespace)) {
                writer.writeEndElement();
            }
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
            boolean foundSCT = NegotiationUtils.parseSCTResult(message);

            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.SECURE_CONVERSATION_TOKEN);
                if (ais == null || ais.isEmpty()) {
                    return;
                }
                for (AssertionInfo inf : ais) {
                    if (foundSCT) {
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
                    tok = NegotiationUtils.getTokenStore(m2).getToken(tokId);
                }
            }

            STSClient client = STSUtils.getClient(m2, "sct");
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
                    NegotiationUtils.getTokenStore(m2).remove(tok);
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