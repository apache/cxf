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
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSSPolicyException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.ThreadLocalSecurityProvider;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.setup.ConfigurationConverter;
import org.apache.wss4j.stax.setup.OutboundWSSec;
import org.apache.wss4j.stax.setup.WSSec;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.OutboundSecurityContext;
import org.apache.xml.security.stax.impl.OutboundSecurityContextImpl;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;

public class WSS4JStaxOutInterceptor extends AbstractWSS4JStaxInterceptor {

    public static final String OUTPUT_STREAM_HOLDER =
        WSS4JStaxOutInterceptor.class.getName() + ".outputstream";
    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JStaxOutInterceptor.class);
    private WSS4JStaxOutInterceptorInternal ending;

    private boolean mtomEnabled;

    public WSS4JStaxOutInterceptor(WSSSecurityProperties securityProperties) {
        super(securityProperties);
        WSSec.init();
        setPhase(Phase.PRE_STREAM);
        getBefore().add(StaxOutInterceptor.class.getName());
        getAfter().add("org.apache.cxf.interceptor.LoggingOutInterceptor");
        getAfter().add("org.apache.cxf.ext.logging.LoggingOutInterceptor");
        ending = createEndingInterceptor();
    }

    public WSS4JStaxOutInterceptor(Map<String, Object> props) {
        super(props);
        WSSec.init();
        setPhase(Phase.PRE_STREAM);
        getBefore().add(StaxOutInterceptor.class.getName());
        getAfter().add("org.apache.cxf.interceptor.LoggingOutInterceptor");
        getAfter().add("org.apache.cxf.ext.logging.LoggingOutInterceptor");
        ending = createEndingInterceptor();
    }

    public WSS4JStaxOutInterceptor() {
        super();
        WSSec.init();
        setPhase(Phase.PRE_STREAM);
        getBefore().add(StaxOutInterceptor.class.getName());
        getAfter().add("org.apache.cxf.interceptor.LoggingOutInterceptor");
        getAfter().add("org.apache.cxf.ext.logging.LoggingOutInterceptor");
        ending = createEndingInterceptor();
    }

    public boolean isAllowMTOM() {
        return mtomEnabled;
    }

    /**
     * Enable or disable mtom with WS-Security. MTOM is disabled if we are signing or
     * encrypting the message Body, as otherwise attachments would not get encrypted
     * or be part of the signature.
     * @param allowMTOM
     */
    public void setAllowMTOM(boolean allowMTOM) {
        this.mtomEnabled = allowMTOM;
    }

    protected void handleSecureMTOM(SoapMessage mc, WSSSecurityProperties secProps) {
        if (mtomEnabled) {
            return;
        }

        //must turn off mtom when using WS-Sec so binary is inlined so it can
        //be properly signed/encrypted/etc...
        String mtomKey = org.apache.cxf.message.Message.MTOM_ENABLED;
        if (Boolean.TRUE.equals(mc.get(mtomKey))) {
            LOG.warning("MTOM will be disabled as the WSS4JOutInterceptor.mtomEnabled property"
                    + " is set to false");
        }
        mc.put(mtomKey, Boolean.FALSE);
    }

    public void handleMessage(SoapMessage mc) throws Fault {
        OutputStream os = mc.getContent(OutputStream.class);
        if (os == null) {
            String error =
                "The message outputstream is null - Multiple WSS4JStaxOutInterceptor instances are not supported";
            WSSecurityException exception = new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "empty",
                                          new Object[] {error});
            LOG.warning(error);
            throw new Fault(exception);
        }

        String encoding = getEncoding(mc);

        XMLStreamWriter newXMLStreamWriter;
        try {
            WSSSecurityProperties secProps = createSecurityProperties();
            secProps.setDocumentCreator(() -> DOMUtils.createDocument());
            translateProperties(mc, secProps);
            configureCallbackHandler(mc, secProps);

            final OutboundSecurityContext outboundSecurityContext = new OutboundSecurityContextImpl();
            configureProperties(mc, outboundSecurityContext, secProps);
            if (secProps.getActions() == null || secProps.getActions().isEmpty()) {
                // If no actions configured then return
                return;
            }

            handleSecureMTOM(mc, secProps);

            if (secProps.getAttachmentCallbackHandler() == null) {
                secProps.setAttachmentCallbackHandler(new AttachmentCallbackHandler(mc));
            }

            SecurityEventListener securityEventListener =
                configureSecurityEventListener(mc, secProps);

            OutboundWSSec outboundWSSec = WSSec.getOutboundWSSec(secProps);

            @SuppressWarnings("unchecked")
            final List<SecurityEvent> requestSecurityEvents =
                (List<SecurityEvent>) mc.getExchange().get(SecurityEvent.class.getName() + ".in");

            outboundSecurityContext.putList(SecurityEvent.class, requestSecurityEvents);
            outboundSecurityContext.addSecurityEventListener(securityEventListener);

            newXMLStreamWriter = outboundWSSec.processOutMessage(os, encoding, outboundSecurityContext);
            mc.setContent(XMLStreamWriter.class, newXMLStreamWriter);
        } catch (WSSecurityException | WSSPolicyException e) {
            throw new Fault(e);
        }

        mc.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION, Boolean.TRUE);

        try {
            newXMLStreamWriter.writeStartDocument(encoding, "1.0");
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
        mc.removeContent(OutputStream.class);
        mc.put(OUTPUT_STREAM_HOLDER, os);

        // Add a final interceptor to write end elements
        mc.getInterceptorChain().add(ending);

    }

    protected SecurityEventListener configureSecurityEventListener(
        final SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws WSSPolicyException {
        final List<SecurityEvent> outgoingSecurityEventList = new LinkedList<>();
        msg.getExchange().put(SecurityEvent.class.getName() + ".out", outgoingSecurityEventList);
        msg.put(SecurityEvent.class.getName() + ".out", outgoingSecurityEventList);

        return new SecurityEventListener() {
            @Override
            public void registerSecurityEvent(SecurityEvent securityEvent) throws XMLSecurityException {
                if (securityEvent.getSecurityEventType() == WSSecurityEventConstants.SAML_TOKEN) {
                    // Store SAML keys in case we need them on the inbound side
                    TokenSecurityEvent<?> tokenSecurityEvent = (TokenSecurityEvent<?>)securityEvent;
                    try {
                        WSS4JUtils.parseAndStoreStreamingSecurityToken(tokenSecurityEvent.getSecurityToken(), msg);
                    } catch (TokenStoreException e) {
                        throw new XMLSecurityException(e);
                    }
                } else if (securityEvent.getSecurityEventType() == WSSecurityEventConstants.SignatureValue) {
                    // Required for Signature Confirmation
                    outgoingSecurityEventList.add(securityEvent);
                }
            }
        };
    }

    protected void configureProperties(
        SoapMessage msg, OutboundSecurityContext outboundSecurityContext,
        WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        String user = (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.USERNAME, msg);
        if (user != null) {
            securityProperties.setTokenUser(user);
        }
        String sigUser =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_USERNAME, msg);
        if (sigUser != null) {
            securityProperties.setSignatureUser(sigUser);
        }
        String encUser =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_USERNAME, msg);
        if (encUser != null) {
            securityProperties.setEncryptionUser(encUser);
        }

        // Crypto loading only applies for Map
        Map<String, Object> config = getProperties();
        if (config != null && !config.isEmpty()) {
            Crypto sigCrypto =
                loadCrypto(
                    msg,
                    ConfigurationConstants.SIG_PROP_FILE,
                    ConfigurationConstants.SIG_PROP_REF_ID,
                    securityProperties
                );
            if (sigCrypto != null) {
                config.put(ConfigurationConstants.SIG_PROP_REF_ID, "RefId-" + sigCrypto.hashCode());
                config.put("RefId-" + sigCrypto.hashCode(), sigCrypto);
                if (sigUser == null && sigCrypto.getDefaultX509Identifier() != null) {
                    // Fall back to default identifier
                    securityProperties.setSignatureUser(sigCrypto.getDefaultX509Identifier());
                }
            }

            Crypto encCrypto =
                loadCrypto(
                    msg,
                    ConfigurationConstants.ENC_PROP_FILE,
                    ConfigurationConstants.ENC_PROP_REF_ID,
                    securityProperties
                );
            if (encCrypto != null) {
                config.put(ConfigurationConstants.ENC_PROP_REF_ID, "RefId-" + encCrypto.hashCode());
                config.put("RefId-" + encCrypto.hashCode(), encCrypto);
                if (encUser == null && encCrypto.getDefaultX509Identifier() != null) {
                    // Fall back to default identifier
                    securityProperties.setEncryptionUser(encCrypto.getDefaultX509Identifier());
                }
            }
            ConfigurationConverter.parseCrypto(config, securityProperties);
        } else {
            Crypto sigCrypto = securityProperties.getSignatureCrypto();
            if (sigCrypto != null && sigUser == null
                && sigCrypto.getDefaultX509Identifier() != null) {
                // Fall back to default identifier
                securityProperties.setSignatureUser(sigCrypto.getDefaultX509Identifier());
            }

            Crypto encrCrypto = securityProperties.getEncryptionCrypto();
            if (encrCrypto != null && encUser == null
                && encrCrypto.getDefaultX509Identifier() != null) {
                // Fall back to default identifier
                securityProperties.setEncryptionUser(encrCrypto.getDefaultX509Identifier());
            }
        }

        if (securityProperties.getSignatureUser() == null && user != null) {
            securityProperties.setSignatureUser(user);
        }
        if (securityProperties.getEncryptionUser() == null && user != null) {
            securityProperties.setEncryptionUser(user);
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
            encoding = StandardCharsets.UTF_8.name();
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
    }

    final class WSS4JStaxOutInterceptorInternal extends AbstractPhaseInterceptor<Message> {
        WSS4JStaxOutInterceptorInternal() {
            super(Phase.PRE_STREAM_ENDING);
            getBefore().add(AttachmentOutInterceptor.AttachmentOutEndingInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            Object provider = message.getExchange().get(Provider.class);
            final boolean useCustomProvider = provider != null && ThreadLocalSecurityProvider.isInstalled();
            try {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.setProvider((Provider)provider);
                }
                handleMessageInternal(message);
            } finally {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.unsetProvider();
                }
            }
        }

        private void handleMessageInternal(Message mc) throws Fault {
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
