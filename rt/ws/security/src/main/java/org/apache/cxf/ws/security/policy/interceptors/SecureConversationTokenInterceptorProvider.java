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
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;


import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants.SupportTokenType;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.conversation.dkalgo.P_SHA1;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.utils.Base64;

/**
 * 
 */
public class SecureConversationTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    static final Logger LOG = LogUtils.getL7dLogger(SecureConversationTokenInterceptorProvider.class);


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
            message.getExchange().put(TokenStore.class.getName(), tokenStore);
        }
        return tokenStore;
    }
    static PolicyAssertion getAddressingPolicy(AssertionInfoMap aim, boolean optional) {
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
            Endpoint ep = message.getExchange().get(Endpoint.class);
            client.setEndpointName(ep.getEndpointInfo().getName().toString() + ".sct-client");
            client.setBeanName(ep.getEndpointInfo().getName().toString() + ".sct-client");
        }
        return client;
    }
    static byte[] writeProofToken(String prefix, 
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
    
    static Element writeSecurityTokenReference(W3CDOMStreamWriter writer,
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

    
    static int getWSCVersion(String tokenTypeValue) throws ConversationException {

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
