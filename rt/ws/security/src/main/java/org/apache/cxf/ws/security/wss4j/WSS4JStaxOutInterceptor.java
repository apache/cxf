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
package org.apache.cxf.ws.security.wss4j;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.WSSPolicyException;
import org.apache.wss4j.stax.ConfigurationConverter;
import org.apache.wss4j.stax.WSSec;
import org.apache.wss4j.stax.ext.OutboundWSSec;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.impl.OutboundSecurityContextImpl;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;

public class WSS4JStaxOutInterceptor extends AbstractWSS4JStaxInterceptor {
    
    /**
     * Property name for a map of action IDs ({@link Integer}) to action
     * class names. Values can be either {@link Class}) or Objects
-    * implementing {@link Action}.
     */
    public static final String WSS4J_ACTION_MAP = "wss4j.action.map";
    
    public static final String OUTPUT_STREAM_HOLDER = 
        WSS4JStaxOutInterceptor.class.getName() + ".outputstream";
    private WSS4JStaxOutInterceptorInternal ending;
    
    private boolean mtomEnabled;
    
    public WSS4JStaxOutInterceptor(WSSSecurityProperties securityProperties) {
        super();
        setPhase(Phase.PRE_STREAM);
        getBefore().add(StaxOutInterceptor.class.getName());
        
        ending = createEndingInterceptor();
        setSecurityProperties(securityProperties);
    }

    public WSS4JStaxOutInterceptor(Map<String, Object> props) {
        super(props);
        setPhase(Phase.PRE_STREAM);
        getBefore().add(StaxOutInterceptor.class.getName());
        
        ending = createEndingInterceptor();
    }
    
    public boolean isAllowMTOM() {
        return mtomEnabled;
    }
    
    /**
     * Enable or disable mtom with WS-Security.   By default MTOM is disabled as
     * attachments would not get encrypted or be part of the signature.
     * @param mtomEnabled
     */
    public void setAllowMTOM(boolean allowMTOM) {
        this.mtomEnabled = allowMTOM;
    }
    

    @Override
    public Object getProperty(Object msgContext, String key) {
        return super.getProperty(msgContext, key);
    }

    public void handleMessage(SoapMessage mc) throws Fault {
        //must turn off mtom when using WS-Sec so binary is inlined so it can
        //be properly signed/encrypted/etc...
        if (!mtomEnabled) {
            mc.put(org.apache.cxf.message.Message.MTOM_ENABLED, false);
        }
        
        OutputStream os = mc.getContent(OutputStream.class);
        String encoding = getEncoding(mc);

        XMLStreamWriter newXMLStreamWriter;
        try {
            @SuppressWarnings("unchecked")
            final List<SecurityEvent> requestSecurityEvents = 
                (List<SecurityEvent>) mc.getExchange().get(SecurityEvent.class.getName() + ".in");
            
            translateProperties(mc);
            Map<String, SecurityTokenProvider<OutboundSecurityToken>> outboundTokens = 
                new HashMap<String, SecurityTokenProvider<OutboundSecurityToken>>();
            configureCallbackHandler(mc);
            configureProperties(mc, outboundTokens);
            
            OutboundWSSec outboundWSSec = null;
            WSSSecurityProperties secProps = null;
            if (getSecurityProperties() != null) {
                secProps = getSecurityProperties();
            } else {
                secProps = ConfigurationConverter.convert(getProperties());
            }
            
            if ((secProps.getOutAction() == null || secProps.getOutAction().length == 0)
                && mc.get(AssertionInfoMap.class) != null) {
                // If no actions configured (with SecurityPolicy) then return
                return;
            }
            
            SecurityEventListener securityEventListener = 
                configureSecurityEventListener(mc, secProps);
            
            outboundWSSec = WSSec.getOutboundWSSec(secProps);
            
            final OutboundSecurityContextImpl outboundSecurityContext = new OutboundSecurityContextImpl();
            outboundSecurityContext.putList(SecurityEvent.class, requestSecurityEvents);
            outboundSecurityContext.addSecurityEventListener(securityEventListener);
            
            // Save Tokens on the security context
            for (String key : outboundTokens.keySet()) {
                SecurityTokenProvider<OutboundSecurityToken> provider = outboundTokens.get(key);
                outboundSecurityContext.registerSecurityTokenProvider(provider.getId(), provider);
                outboundSecurityContext.put(key, provider.getId());
            }
            
            newXMLStreamWriter = outboundWSSec.processOutMessage(os, encoding, outboundSecurityContext);
            mc.setContent(XMLStreamWriter.class, newXMLStreamWriter);
        } catch (WSSecurityException e) {
            throw new Fault(e);
        } catch (WSSPolicyException e) {
            throw new Fault(e);
        }

        mc.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION, Boolean.TRUE);
        mc.put(StaxOutInterceptor.FORCE_START_DOCUMENT, Boolean.TRUE);

        if (MessageUtils.getContextualBoolean(mc, StaxOutInterceptor.FORCE_START_DOCUMENT, false)) {
            try {
                newXMLStreamWriter.writeStartDocument(encoding, "1.0");
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
            mc.removeContent(OutputStream.class);
            mc.put(OUTPUT_STREAM_HOLDER, os);
        }

        // Add a final interceptor to write end elements
        mc.getInterceptorChain().add(ending);
        
    }
    
    protected SecurityEventListener configureSecurityEventListener(
        final SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws WSSPolicyException {
        final List<SecurityEvent> outgoingSecurityEventList = new LinkedList<SecurityEvent>();
        SecurityEventListener securityEventListener = new SecurityEventListener() {
            @Override
            public void registerSecurityEvent(SecurityEvent securityEvent) throws XMLSecurityException {
                if (securityEvent.getSecurityEventType() == WSSecurityEventConstants.SamlToken
                    && securityEvent instanceof TokenSecurityEvent) {
                    // Store SAML keys in case we need them on the inbound side
                    TokenSecurityEvent<?> tokenSecurityEvent = (TokenSecurityEvent<?>)securityEvent;
                    WSS4JUtils.parseAndStoreStreamingSecurityToken(tokenSecurityEvent.getSecurityToken(), msg);
                } else {
                    outgoingSecurityEventList.add(securityEvent);
                }
            }
        };
        msg.getExchange().put(SecurityEvent.class.getName() + ".out", outgoingSecurityEventList);
        msg.put(SecurityEvent.class.getName() + ".out", outgoingSecurityEventList);

        return securityEventListener;
    }
    
    protected void configureProperties(
        SoapMessage msg, Map<String, SecurityTokenProvider<OutboundSecurityToken>> outboundTokens
    ) throws WSSecurityException {
        Map<String, Object> config = getProperties();
        
        // Crypto loading only applies for Map
        if (config != null) {
            String user = (String)msg.getContextualProperty(SecurityConstants.USERNAME);
            if (user != null) {
                config.put(ConfigurationConstants.USER, user);
            }
            String sigUser = (String)msg.getContextualProperty(SecurityConstants.SIGNATURE_USERNAME);
            if (sigUser != null) {
                config.put(ConfigurationConstants.SIGNATURE_USER, sigUser);
            }
            String encUser = (String)msg.getContextualProperty(SecurityConstants.ENCRYPT_USERNAME);
            if (encUser != null) {
                config.put(ConfigurationConstants.ENCRYPTION_USER, encUser);
            }
            
            Crypto sigCrypto = 
                loadCrypto(
                    msg,
                    ConfigurationConstants.SIG_PROP_FILE,
                    ConfigurationConstants.SIG_PROP_REF_ID
                );
            if (sigCrypto != null) {
                config.put(ConfigurationConstants.SIG_PROP_REF_ID, "RefId-" + sigCrypto.hashCode());
                config.put("RefId-" + sigCrypto.hashCode(), sigCrypto);
                if (sigUser == null && sigCrypto.getDefaultX509Identifier() != null) {
                    // Fall back to default identifier
                    config.put(ConfigurationConstants.SIGNATURE_USER, 
                               sigCrypto.getDefaultX509Identifier());
                }
            }
            
            Crypto encCrypto = 
                loadCrypto(
                    msg,
                    ConfigurationConstants.ENC_PROP_FILE,
                    ConfigurationConstants.ENC_PROP_REF_ID
                );
            if (encCrypto != null) {
                config.put(ConfigurationConstants.ENC_PROP_REF_ID, "RefId-" + encCrypto.hashCode());
                config.put("RefId-" + encCrypto.hashCode(), encCrypto);
                if (encUser == null && encCrypto.getDefaultX509Identifier() != null) {
                    // Fall back to default identifier
                    config.put(ConfigurationConstants.ENCRYPTION_USER, 
                               encCrypto.getDefaultX509Identifier());
                }
            }
        }
    }
    
    public final WSS4JStaxOutInterceptorInternal createEndingInterceptor() {
        return new WSS4JStaxOutInterceptorInternal();
    }
    
    private String getEncoding(Message message) {
        Exchange ex = message.getExchange();
        String encoding = (String) message.get(Message.ENCODING);
        if (encoding == null && ex.getInMessage() != null) {
            encoding = (String) ex.getInMessage().get(Message.ENCODING);
            message.put(Message.ENCODING, encoding);
        }

        if (encoding == null) {
            encoding = "UTF-8";
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
    }
    
    final class WSS4JStaxOutInterceptorInternal extends AbstractPhaseInterceptor<Message> {
        public WSS4JStaxOutInterceptorInternal() {
            super(Phase.PRE_STREAM_ENDING);
            getAfter().add(AttachmentOutInterceptor.AttachmentOutEndingInterceptor.class.getName());
        }
        
        public void handleMessage(Message mc) throws Fault {
            try {
                XMLStreamWriter xtw = mc.getContent(XMLStreamWriter.class);
                if (xtw != null) {
                    xtw.writeEndDocument();
                    xtw.flush();
                    xtw.close();
                }

                OutputStream os = (OutputStream) mc.get(OUTPUT_STREAM_HOLDER);
                if (os != null) {
                    mc.setContent(OutputStream.class, os);
                }
                mc.removeContent(XMLStreamWriter.class);
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }

    }
}
