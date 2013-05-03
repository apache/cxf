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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.URIMappingInterceptor;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.WSSec;
import org.apache.wss4j.stax.ext.InboundWSSec;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;

public class WSS4JStaxInInterceptor extends AbstractWSS4JStaxInterceptor {
    
    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JStaxInInterceptor.class);
    
    private List<String> actions;
    
    public WSS4JStaxInInterceptor(WSSSecurityProperties securityProperties) {
        super();
        setPhase(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
        setSecurityProperties(securityProperties);
    }
    
    public WSS4JStaxInInterceptor(Map<String, Object> props) {
        super(props);
        setPhase(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
        if (props != null && props.containsKey(ConfigurationConstants.ACTION)) {
            Object actionObject = props.get(ConfigurationConstants.ACTION);
            if (actionObject instanceof String) {
                String[] actionArray = ((String)actionObject).split(" ");
                this.actions = Arrays.asList(actionArray);
            }
        }
    }

    public final boolean isGET(SoapMessage message) {
        String method = (String)message.get(SoapMessage.HTTP_REQUEST_METHOD);
        boolean isGet = 
            "GET".equals(method) && message.getContent(XMLStreamReader.class) == null;
        if (isGet) {
            //make sure we skip the URIMapping as we cannot apply security requirements to that
            message.put(URIMappingInterceptor.URIMAPPING_SKIP, Boolean.TRUE);
        }
        return isGet;
    }
    
    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {
        
        if (isGET(soapMessage)) {
            return;
        }

        XMLStreamReader originalXmlStreamReader = soapMessage.getContent(XMLStreamReader.class);
        XMLStreamReader newXmlStreamReader;

        soapMessage.getInterceptorChain().add(new StaxSecurityContextInInterceptor());
        
        if (actions != null && !actions.isEmpty()) {
            soapMessage.getInterceptorChain().add(new StaxActionInInterceptor(actions));
        }
        
        try {
            @SuppressWarnings("unchecked")
            final List<SecurityEvent> requestSecurityEvents = 
                (List<SecurityEvent>) soapMessage.getExchange().get(SecurityEvent.class.getName() + ".out");
            
            translateProperties(soapMessage);
            configureProperties(soapMessage);
            configureCallbackHandler(soapMessage);
            
            InboundWSSec inboundWSSec = null;
            if (getSecurityProperties() != null) {
                inboundWSSec = WSSec.getInboundWSSec(getSecurityProperties());
            } else {
                inboundWSSec = WSSec.getInboundWSSec(getProperties());
            }
            
            SecurityEventListener securityEventListener = 
                configureSecurityEventListener(soapMessage, inboundWSSec);
            
            newXmlStreamReader = 
                inboundWSSec.processInMessage(originalXmlStreamReader, requestSecurityEvents, securityEventListener);
            soapMessage.setContent(XMLStreamReader.class, newXmlStreamReader);

            // Warning: The exceptions which can occur here are not security relevant exceptions
            // but configuration-errors. To catch security relevant exceptions you have to catch 
            // them e.g.in the FaultOutInterceptor. Why? Because we do streaming security. This 
            // interceptor doesn't handle the ws-security stuff but just setup the relevant stuff
            // for it. Exceptions will be thrown as a wrapped XMLStreamException during further
            // processing in the WS-Stack.
        } catch (WSSecurityException e) {
            throw createSoapFault(soapMessage.getVersion(), e);
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("STAX_EX", LOG), e, soapMessage.getVersion().getSender());
        }
    }
    
    protected SecurityEventListener configureSecurityEventListener(SoapMessage msg, InboundWSSec inboundWSSec) {
        final List<SecurityEvent> incomingSecurityEventList = new LinkedList<SecurityEvent>();
        SecurityEventListener securityEventListener = new SecurityEventListener() {
            @Override
            public void registerSecurityEvent(SecurityEvent securityEvent) throws WSSecurityException {
                incomingSecurityEventList.add(securityEvent);
            }
        };
        msg.getExchange().put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);
        msg.put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);
        
        return securityEventListener;
    }
    
    protected void configureProperties(SoapMessage msg) throws WSSecurityException {
        WSSSecurityProperties securityProperties = getSecurityProperties();
        Map<String, Object> config = getProperties();
        
        // Configure replay caching
        ReplayCache nonceCache = 
            WSS4JUtils.getReplayCache(
                msg, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE
            );
        if (nonceCache != null) {
            if (securityProperties != null) {
                securityProperties.setNonceReplayCache(nonceCache);
            } else {
                config.put(ConfigurationConstants.NONCE_CACHE_INSTANCE, nonceCache);
            }
        }
        
        ReplayCache timestampCache = 
            WSS4JUtils.getReplayCache(
                msg, SecurityConstants.ENABLE_TIMESTAMP_CACHE, SecurityConstants.TIMESTAMP_CACHE_INSTANCE
            );
        if (timestampCache != null) {
            if (securityProperties != null) {
                securityProperties.setTimestampReplayCache(timestampCache);
            } else {
                config.put(ConfigurationConstants.TIMESTAMP_CACHE_INSTANCE, timestampCache);
            }
        }
        
        boolean enableRevocation = 
            MessageUtils.isTrue(msg.getContextualProperty(SecurityConstants.ENABLE_REVOCATION));
        if (securityProperties != null) {
            securityProperties.setEnableRevocation(enableRevocation);
        } else {
            config.put(ConfigurationConstants.ENABLE_REVOCATION, Boolean.toString(enableRevocation));
        }
        
        // Crypto loading only applies for Map
        if (config != null) {
            Crypto sigVerCrypto = 
                loadCrypto(
                    msg,
                    ConfigurationConstants.SIG_VER_PROP_FILE,
                    ConfigurationConstants.SIG_VER_PROP_REF_ID
                );
            if (sigVerCrypto != null) {
                config.put(ConfigurationConstants.SIG_VER_PROP_REF_ID, "RefId-" + sigVerCrypto.hashCode());
                config.put("RefId-" + sigVerCrypto.hashCode(), sigVerCrypto);
            }
            
            Crypto decCrypto = 
                loadCrypto(
                    msg,
                    ConfigurationConstants.DEC_PROP_FILE,
                    ConfigurationConstants.DEC_PROP_REF_ID
                );
            if (decCrypto != null) {
                config.put(ConfigurationConstants.DEC_PROP_REF_ID, "RefId-" + decCrypto.hashCode());
                config.put("RefId-" + decCrypto.hashCode(), decCrypto);
            }
        }
    }
    
    private void configureCallbackHandler(SoapMessage soapMessage) throws WSSecurityException {
        Object o = soapMessage.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
        if (o instanceof String) {
            try {
                o = ClassLoaderUtils.loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
            }
        }            
        if (o instanceof CallbackHandler) {
            WSSSecurityProperties securityProperties = getSecurityProperties();
            Map<String, Object> config = getProperties();
            
            if (securityProperties != null) {
                securityProperties.setCallbackHandler((CallbackHandler)o);
            } else {
                config.put(ConfigurationConstants.PW_CALLBACK_REF, (CallbackHandler)o);
            }
        }
    }

    /**
     * Create a SoapFault from a WSSecurityException, following the SOAP Message Security
     * 1.1 specification, chapter 12 "Error Handling".
     * 
     * When the Soap version is 1.1 then set the Fault/Code/Value from the fault code
     * specified in the WSSecurityException (if it exists).
     * 
     * Otherwise set the Fault/Code/Value to env:Sender and the Fault/Code/Subcode/Value
     * as the fault code from the WSSecurityException.
     */
    private SoapFault 
    createSoapFault(SoapVersion version, WSSecurityException e) {
        SoapFault fault;
        javax.xml.namespace.QName faultCode = e.getFaultCode();
        if (version.getVersion() == 1.1 && faultCode != null) {
            fault = new SoapFault(e.getMessage(), e, faultCode);
        } else {
            fault = new SoapFault(e.getMessage(), e, version.getSender());
            if (version.getVersion() != 1.1 && faultCode != null) {
                fault.setSubCode(faultCode);
            }
        }
        return fault;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
