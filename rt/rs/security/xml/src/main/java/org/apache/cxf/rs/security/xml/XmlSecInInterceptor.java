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
package org.apache.cxf.rs.security.xml;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.rs.security.common.TrustValidator;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.InboundXMLSec;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.securityEvent.AlgorithmSuiteSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.SecurityToken;

/**
 * A new StAX-based interceptor for processing messages with XML Signature + Encryption content.
 */
public class XmlSecInInterceptor implements PhaseInterceptor<Message> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(XmlSecInInterceptor.class);
    
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
    private EncryptionProperties encryptionProperties;
    private SignatureProperties sigProps;
    private String phase;
    private String decryptionAlias;
    private String signatureVerificationAlias;

    public XmlSecInInterceptor() {
        setPhase(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
    }
    
    public void handleFault(Message message) {
    }

    public void handleMessage(Message message) throws Fault {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        if ("GET".equals(method)) {
            return;
        }
        
        Message outMs = message.getExchange().getOutMessage();
        Message inMsg = outMs == null ? message : outMs.getExchange().getInMessage();
        
        XMLStreamReader originalXmlStreamReader = inMsg.getContent(XMLStreamReader.class);
        if (originalXmlStreamReader == null) {
            InputStream is = inMsg.getContent(InputStream.class);
            if (is != null) {
                originalXmlStreamReader = StaxUtils.createXMLStreamReader(is);
            }
        }
        
        try {
            XMLSecurityProperties properties = new XMLSecurityProperties();
            configureDecryptionKeys(inMsg, properties);
            Crypto signatureCrypto = getSignatureCrypto(inMsg);
            configureSignatureKeys(signatureCrypto, inMsg, properties);
            
            SecurityEventListener securityEventListener = 
                configureSecurityEventListener(signatureCrypto, inMsg, properties);
            InboundXMLSec inboundXMLSec = XMLSec.getInboundWSSec(properties);
            
            XMLStreamReader newXmlStreamReader = 
                inboundXMLSec.processInMessage(originalXmlStreamReader, null, securityEventListener);
            inMsg.setContent(XMLStreamReader.class, newXmlStreamReader);

        } catch (XMLStreamException e) {
            throwFault(e.getMessage(), e);
        } catch (XMLSecurityException e) {
            throwFault(e.getMessage(), e);
        } catch (IOException e) {
            throwFault(e.getMessage(), e);
        } catch (UnsupportedCallbackException e) {
            throwFault(e.getMessage(), e);
        }
    }
    
    private void configureDecryptionKeys(Message message, XMLSecurityProperties properties) 
        throws IOException, 
        UnsupportedCallbackException, WSSecurityException {
        String cryptoKey = null; 
        String propKey = null;
        if (SecurityUtils.isSignedAndEncryptedTwoWay(message)) {
            cryptoKey = SecurityConstants.SIGNATURE_CRYPTO;
            propKey = SecurityConstants.SIGNATURE_PROPERTIES;
        } else {
            cryptoKey = SecurityConstants.ENCRYPT_CRYPTO;
            propKey = SecurityConstants.ENCRYPT_PROPERTIES;
        }
        
        Crypto crypto = null;
        try {
            crypto = new CryptoLoader().getCrypto(message, cryptoKey, propKey);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
        }
        
        if (crypto != null && decryptionAlias != null) {
            CallbackHandler callback = SecurityUtils.getCallbackHandler(message, this.getClass());
            WSPasswordCallback passwordCallback = 
                new WSPasswordCallback(decryptionAlias, WSPasswordCallback.DECRYPT);
            callback.handle(new Callback[] {passwordCallback});

            Key privateKey = crypto.getPrivateKey(decryptionAlias, passwordCallback.getPassword());
            properties.setDecryptionKey(privateKey);
        }
    }
    
    private Crypto getSignatureCrypto(Message message) {
        String cryptoKey = null; 
        String propKey = null;
        if (SecurityUtils.isSignedAndEncryptedTwoWay(message)) {
            cryptoKey = SecurityConstants.ENCRYPT_CRYPTO;
            propKey = SecurityConstants.ENCRYPT_PROPERTIES;
        } else {
            cryptoKey = SecurityConstants.SIGNATURE_CRYPTO;
            propKey = SecurityConstants.SIGNATURE_PROPERTIES;    
        }
        
        try {
            return new CryptoLoader().getCrypto(message, cryptoKey, propKey);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
            return null;
        }
    }
    
    private void configureSignatureKeys(
        Crypto sigCrypto, Message message, XMLSecurityProperties properties
    ) throws IOException, 
        UnsupportedCallbackException, WSSecurityException {
        
        if (sigCrypto != null && signatureVerificationAlias != null) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(signatureVerificationAlias);
            X509Certificate[] certs = sigCrypto.getX509Certificates(cryptoType);
            if (certs != null && certs.length > 0) {
                properties.setSignatureVerificationKey(certs[0].getPublicKey());
            }
        }
    }
    
    protected SecurityEventListener configureSecurityEventListener(
        final Crypto sigCrypto, Message msg, XMLSecurityProperties securityProperties
    ) {
        final List<SecurityEvent> incomingSecurityEventList = new LinkedList<SecurityEvent>();
        SecurityEventListener securityEventListener = new SecurityEventListener() {
            @Override
            public void registerSecurityEvent(SecurityEvent securityEvent) throws XMLSecurityException {
                if (securityEvent.getSecurityEventType() == SecurityEventConstants.AlgorithmSuite) {
                    if (encryptionProperties != null) {
                        checkEncryptionAlgorithms((AlgorithmSuiteSecurityEvent)securityEvent);
                    }
                    if (sigProps != null) {
                        checkSignatureAlgorithms((AlgorithmSuiteSecurityEvent)securityEvent);
                    }
                } else if (securityEvent.getSecurityEventType() != SecurityEventConstants.EncryptedKeyToken
                    && securityEvent instanceof TokenSecurityEvent<?>) {
                    checkSignatureTrust(sigCrypto, (TokenSecurityEvent<?>)securityEvent);
                }
                incomingSecurityEventList.add(securityEvent);
            }
        };
        msg.getExchange().put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);
        msg.put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);

        return securityEventListener;
    }
    
    private void checkEncryptionAlgorithms(AlgorithmSuiteSecurityEvent event) 
        throws XMLSecurityException {
        if (XMLSecurityConstants.Enc.equals(event.getAlgorithmUsage())
            && encryptionProperties.getEncryptionSymmetricKeyAlgo() != null
            && !encryptionProperties.getEncryptionSymmetricKeyAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", "The symmetric encryption algorithm "
                                           + event.getAlgorithmURI() + " is not allowed");
        } else if ((XMLSecurityConstants.Sym_Key_Wrap.equals(event.getAlgorithmUsage())
            || XMLSecurityConstants.Asym_Key_Wrap.equals(event.getAlgorithmUsage()))
            && encryptionProperties.getEncryptionKeyTransportAlgo() != null
            && !encryptionProperties.getEncryptionKeyTransportAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", "The key transport algorithm "
                + event.getAlgorithmURI() + " is not allowed");
        } else if (XMLSecurityConstants.EncDig.equals(event.getAlgorithmUsage())
            && encryptionProperties.getEncryptionDigestAlgo() != null
            && !encryptionProperties.getEncryptionDigestAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", "The encryption digest algorithm "
                + event.getAlgorithmURI() + " is not allowed");
        }
    }
    
    private void checkSignatureAlgorithms(AlgorithmSuiteSecurityEvent event) 
        throws XMLSecurityException {
        if (XMLSecurityConstants.Asym_Sig.equals(event.getAlgorithmUsage())
            || XMLSecurityConstants.Sym_Sig.equals(event.getAlgorithmUsage())
            && sigProps.getSignatureAlgo() != null
            && !sigProps.getSignatureAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", "The signature algorithm "
                                           + event.getAlgorithmURI() + " is not allowed");
        } else if (XMLSecurityConstants.SigDig.equals(event.getAlgorithmUsage())
            && sigProps.getSignatureDigestAlgo() != null
            && !sigProps.getSignatureDigestAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", "The signature digest algorithm "
                + event.getAlgorithmURI() + " is not allowed");
        } else if (XMLSecurityConstants.SigC14n.equals(event.getAlgorithmUsage())
            && sigProps.getSignatureC14nMethod() != null
            && !sigProps.getSignatureC14nMethod().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", "The signature c14n algorithm "
                + event.getAlgorithmURI() + " is not allowed");
        } else if (XMLSecurityConstants.SigTransform.equals(event.getAlgorithmUsage())
            && sigProps.getSignatureC14nTransform() != null
            && !sigProps.getSignatureC14nTransform().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", "The signature transformation algorithm "
                + event.getAlgorithmURI() + " is not allowed");
        }
    }
    
    private void checkSignatureTrust(
        Crypto sigCrypto, TokenSecurityEvent<?> event
    ) throws XMLSecurityException {
        SecurityToken token = event.getSecurityToken();
        if (token != null) {
            X509Certificate[] certs = token.getX509Certificates();
            PublicKey publicKey = token.getPublicKey();
            X509Certificate cert = null;
            if (certs != null && certs.length > 0) {
                cert = certs[0];
            }
            
            // validate trust 
            try {
                new TrustValidator().validateTrust(sigCrypto, cert, publicKey);
            } catch (WSSecurityException e) {
                throw new XMLSecurityException("empty", "Error during Signature Trust "
                                               + "validation: " + e.getMessage());
            }
        }
    }
    
    protected void throwFault(String error, Exception ex) {
        LOG.warning(error);
        Response response = Response.status(400).entity(error).build();
        throw ex != null ? new BadRequestException(response, ex) : new BadRequestException(response);
    }

    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    public Set<String> getAfter() {
        return after;
    }

    public void setAfter(Set<String> after) {
        this.after = after;
    }

    public Set<String> getBefore() {
        return before;
    }

    public void setBefore(Set<String> before) {
        this.before = before;
    }

    public String getId() {
        return getClass().getName();
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public void setEncryptionProperties(EncryptionProperties properties) {
        this.encryptionProperties = properties;
    }
    
    public void setSignatureProperties(SignatureProperties properties) {
        this.sigProps = properties;
    }

    public String getDecryptionAlias() {
        return decryptionAlias;
    }

    public void setDecryptionAlias(String decryptionAlias) {
        this.decryptionAlias = decryptionAlias;
    }

    public String getSignatureVerificationAlias() {
        return signatureVerificationAlias;
    }

    public void setSignatureVerificationAlias(String signatureVerificationAlias) {
        this.signatureVerificationAlias = signatureVerificationAlias;
    }
    
}
