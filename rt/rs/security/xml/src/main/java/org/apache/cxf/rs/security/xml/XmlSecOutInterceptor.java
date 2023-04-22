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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.OutboundXMLSec;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.SecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.EncryptionConstants;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

/**
 * A new StAX-based interceptor for creating messages with XML Signature + Encryption content.
 */
public class XmlSecOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String OUTPUT_STREAM_HOLDER =
        XmlSecOutInterceptor.class.getName() + ".outputstream";
    private static final Logger LOG = LogUtils.getL7dLogger(XmlSecOutInterceptor.class);

    private XmlSecStaxOutInterceptorInternal ending;
    private EncryptionProperties encryptionProperties = new EncryptionProperties();
    private SignatureProperties sigProps = new SignatureProperties();
    private boolean encryptSymmetricKey = true;
    private SecretKey symmetricKey;
    private boolean signRequest;
    private boolean encryptRequest;
    private List<QName> elementsToSign = new ArrayList<>();
    private List<QName> elementsToEncrypt = new ArrayList<>();
    private boolean keyInfoMustBeAvailable = true;

    static {
        Init.init();
    }

    public XmlSecOutInterceptor() {
        super(Phase.PRE_STREAM);
        getBefore().add(StaxOutInterceptor.class.getName());

        ending = createEndingInterceptor();
    }

    public void handleMessage(Message message) throws Fault {

        if (message.getExchange().get(Throwable.class) != null) {
            return;
        }

        OutputStream os = message.getContent(OutputStream.class);
        String encoding = getEncoding(message);

        if (!(encryptRequest || signRequest)) {
            Exception ex = new Exception("Either encryption and/or signature must be enabled");
            throwFault(ex.getMessage(), ex);
        }

        XMLStreamWriter newXMLStreamWriter = null;
        try {
            XMLSecurityProperties properties = new XMLSecurityProperties();

            if (signRequest) {
                configureSignature(message, properties);
            }

            if (encryptRequest) {
                configureEncryption(message, properties);
            }

            OutboundXMLSec outboundXMLSec = XMLSec.getOutboundXMLSec(properties);

            newXMLStreamWriter = outboundXMLSec.processOutMessage(os, encoding);
            message.setContent(XMLStreamWriter.class, newXMLStreamWriter);
        } catch (Exception e) {
            throwFault(e.getMessage(), e);
        }

        message.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION, Boolean.TRUE);
        message.put(StaxOutInterceptor.FORCE_START_DOCUMENT, Boolean.TRUE);

        if (MessageUtils.getContextualBoolean(message, StaxOutInterceptor.FORCE_START_DOCUMENT, false)) {
            try {
                newXMLStreamWriter.writeStartDocument(encoding, "1.0");
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
            message.removeContent(OutputStream.class);
            message.put(OUTPUT_STREAM_HOLDER, os);
        }

        // Add a final interceptor to write end elements
        message.getInterceptorChain().add(ending);
    }

    private void configureEncryption(Message message, XMLSecurityProperties properties)
        throws Exception {
        String symEncAlgo = encryptionProperties.getEncryptionSymmetricKeyAlgo() == null
            ? XMLCipher.AES_256 : encryptionProperties.getEncryptionSymmetricKeyAlgo();
        properties.setEncryptionSymAlgorithm(symEncAlgo);
        properties.setEncryptionKey(getSymmetricKey(symEncAlgo));
        if (encryptSymmetricKey) {
            X509Certificate sendingCert;
            String userName =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_USERNAME, message);
            if (RSSecurityUtils.USE_REQUEST_SIGNATURE_CERT.equals(userName)
                && !MessageUtils.isRequestor(message)) {
                sendingCert =
                    message.getExchange().getInMessage().getContent(X509Certificate.class);
                if (sendingCert == null) {
                    @SuppressWarnings("unchecked")
                    final List<SecurityEvent> incomingSecurityEventList =
                        (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".in");
                    sendingCert = getUseReqSigCert(incomingSecurityEventList);
                }
            } else {
                CryptoLoader loader = new CryptoLoader();
                Crypto crypto = loader.getCrypto(message,
                                          SecurityConstants.ENCRYPT_CRYPTO,
                                          SecurityConstants.ENCRYPT_PROPERTIES);

                userName = RSSecurityUtils.getUserName(crypto, userName);
                if (StringUtils.isEmpty(userName)) {
                    throw new Exception("User name is not available");
                }
                sendingCert = getCertificateFromCrypto(crypto, userName);
            }

            if (sendingCert == null) {
                throw new Exception("Sending certificate is not available");
            }

            properties.setEncryptionUseThisCertificate(sendingCert);

            properties.setEncryptionKeyIdentifier(
                convertKeyIdentifier(encryptionProperties.getEncryptionKeyIdType()));

            properties.setEncryptionKeyName(encryptionProperties.getEncryptionKeyName());

            if (encryptionProperties.getEncryptionKeyTransportAlgo() != null) {
                properties.setEncryptionKeyTransportAlgorithm(
                    encryptionProperties.getEncryptionKeyTransportAlgo());
            }
            if (encryptionProperties.getEncryptionDigestAlgo() != null) {
                properties.setEncryptionKeyTransportDigestAlgorithm(
                    encryptionProperties.getEncryptionDigestAlgo());
            }
        }

        properties.addAction(XMLSecurityConstants.ENCRYPTION);

        if (elementsToEncrypt == null || elementsToEncrypt.isEmpty()) {
            LOG.fine("No Elements to encrypt are specified, so the entire request is encrypt");
            SecurePart securePart =
                new SecurePart((QName)null, SecurePart.Modifier.Element);
            securePart.setSecureEntireRequest(true);
            properties.addEncryptionPart(securePart);
        } else {
            for (QName element : elementsToEncrypt) {
                SecurePart securePart =
                    new SecurePart(element, SecurePart.Modifier.Element);
                properties.addEncryptionPart(securePart);
            }
        }
    }

    private X509Certificate getUseReqSigCert(List<SecurityEvent> incomingSecurityEventList)
        throws XMLSecurityException {
        SecurityToken signatureToken = getSignatureToken(incomingSecurityEventList);
        if (signatureToken != null && signatureToken.getX509Certificates() != null
            && signatureToken.getX509Certificates().length > 0) {
            return signatureToken.getX509Certificates()[0];
        }
        return null;
    }

    private SecurityToken getSignatureToken(List<SecurityEvent> incomingSecurityEventList)
        throws XMLSecurityException {
        if (incomingSecurityEventList != null) {
            for (int i = 0; i < incomingSecurityEventList.size(); i++) {
                SecurityEvent securityEvent = incomingSecurityEventList.get(i);
                if (securityEvent instanceof TokenSecurityEvent) {
                    @SuppressWarnings("unchecked")
                    TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent
                        = (TokenSecurityEvent<? extends SecurityToken>) securityEvent;
                    if (tokenSecurityEvent.getSecurityToken().getTokenUsages().contains(
                        SecurityTokenConstants.TokenUsage_Signature)
                    ) {
                        return tokenSecurityEvent.getSecurityToken();
                    }
                }
            }
        }
        return null;
    }

    private X509Certificate getCertificateFromCrypto(Crypto crypto, String user) throws Exception {
        X509Certificate[] certs = RSSecurityUtils.getCertificates(crypto, user);
        return certs[0];
    }

    private SecretKey getSymmetricKey(String symEncAlgo) throws Exception {
        synchronized (this) {
            if (symmetricKey == null) {
                KeyGenerator keyGen = KeyUtils.getKeyGenerator(symEncAlgo);
                symmetricKey = keyGen.generateKey();
            }
            return symmetricKey;
        }
    }

    private void configureSignature(
        Message message, XMLSecurityProperties properties
    ) throws Exception {
        String userNameKey = SecurityConstants.SIGNATURE_USERNAME;

        CryptoLoader loader = new CryptoLoader();
        Crypto crypto = loader.getCrypto(message,
                                         SecurityConstants.SIGNATURE_CRYPTO,
                                         SecurityConstants.SIGNATURE_PROPERTIES);
        String user = RSSecurityUtils.getUserName(message, crypto, userNameKey);

        if (StringUtils.isEmpty(user) || RSSecurityUtils.USE_REQUEST_SIGNATURE_CERT.equals(user)) {
            throw new Exception("User name is not available");
        }

        String password = RSSecurityUtils.getSignaturePassword(message, user, this.getClass());

        X509Certificate[] issuerCerts = RSSecurityUtils.getCertificates(crypto, user);
        properties.setSignatureCerts(issuerCerts);

        String sigAlgo = sigProps.getSignatureAlgo() == null
            ? SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1 : sigProps.getSignatureAlgo();

        String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
        if ("DSA".equalsIgnoreCase(pubKeyAlgo)) {
            sigAlgo = SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1;
        }

        properties.setSignatureAlgorithm(sigAlgo);
        final PrivateKey privateKey;
        try {
            privateKey = crypto.getPrivateKey(user, password);
        } catch (Exception ex) {
            String errorMessage = "Private key can not be loaded, user:" + user;
            LOG.severe(errorMessage);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
        properties.setSignatureKey(privateKey);

        String digestAlgo = sigProps.getSignatureDigestAlgo() == null
            ? Constants.ALGO_ID_DIGEST_SHA1 : sigProps.getSignatureDigestAlgo();
        properties.setSignatureDigestAlgorithm(digestAlgo);

        if (this.keyInfoMustBeAvailable) {
            properties.setSignatureKeyIdentifier(
                convertKeyIdentifier(sigProps.getSignatureKeyIdType()));
            properties.setSignatureKeyName(sigProps.getSignatureKeyName());
        } else {
            properties.setSignatureKeyIdentifier(SecurityTokenConstants.KeyIdentifier_NoKeyInfo);
        }

        String c14nMethod = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
        if (sigProps.getSignatureC14nMethod() != null) {
            c14nMethod = sigProps.getSignatureC14nMethod();
        }
        properties.setSignatureCanonicalizationAlgorithm(c14nMethod);

        properties.addAction(XMLSecurityConstants.SIGNATURE);
        // Only enveloped supported for the moment.
        String transform = "http://www.w3.org/2001/10/xml-exc-c14n#";
        if (sigProps.getSignatureC14nTransform() != null) {
            transform = sigProps.getSignatureC14nTransform();
        }

        if (sigProps.getSignatureLocation() != null) {
            properties.setSignaturePosition(sigProps.getSignatureLocation());
        }

        if (sigProps.getSignatureGenerateIdAttributes() != null) {
            properties.setSignatureGenerateIds(sigProps.getSignatureGenerateIdAttributes());
        }

        if (Boolean.TRUE.equals(sigProps.getSignatureOmitC14nTransform())) {
            properties.setSignatureIncludeDigestTransform(false);
        }

        if (elementsToSign == null || elementsToSign.isEmpty()) {
            LOG.fine("No Elements to sign are specified, so the entire request is signed");
            SecurePart securePart =
                new SecurePart(null, SecurePart.Modifier.Element,
                               new String[]{
                                   "http://www.w3.org/2000/09/xmldsig#enveloped-signature",
                                   transform
                               },
                               digestAlgo);
            securePart.setSecureEntireRequest(true);
            properties.addSignaturePart(securePart);
        } else {
            for (QName element : elementsToSign) {
                SecurePart securePart =
                    new SecurePart(element, SecurePart.Modifier.Element,
                                   new String[]{
                                       "http://www.w3.org/2000/09/xmldsig#enveloped-signature",
                                       transform
                                   },
                                   digestAlgo);
                properties.addSignaturePart(securePart);
            }
        }

    }

    protected void throwFault(String error, Exception ex) {
        LOG.warning(error);
        Response response = JAXRSUtils.toResponseBuilder(400).entity(error).build();
        throw ExceptionUtils.toBadRequestException(null, response);
    }

    public void setEncryptionProperties(EncryptionProperties properties) {
        this.encryptionProperties = properties;
    }

    public void setEncryptionKeyIdentifierType(String type) {
        encryptionProperties.setEncryptionKeyIdType(type);
    }

    public void setSymmetricEncAlgorithm(String algo) {
        if (!(algo.startsWith(EncryptionConstants.EncryptionSpecNS)
            || algo.startsWith(EncryptionConstants.EncryptionSpec11NS))) {
            algo = EncryptionConstants.EncryptionSpecNS + algo;
        }
        encryptionProperties.setEncryptionSymmetricKeyAlgo(algo);
    }

    public void setKeyEncAlgorithm(String algo) {
        encryptionProperties.setEncryptionKeyTransportAlgo(algo);
    }

    public void setEncryptionDigestAlgorithm(String algo) {
        encryptionProperties.setEncryptionDigestAlgo(algo);
    }

    public void setKeyInfoMustBeAvailable(boolean use) {
        this.keyInfoMustBeAvailable = use;
    }

    public void setSignatureProperties(SignatureProperties props) {
        this.sigProps = props;
    }

    public void setSignatureAlgorithm(String algo) {
        sigProps.setSignatureAlgo(algo);
    }

    public void setSignatureDigestAlgorithm(String algo) {
        sigProps.setSignatureDigestAlgo(algo);
    }

    public void setSignatureKeyIdentifierType(String type) {
        sigProps.setSignatureKeyIdType(type);
    }

    public final XmlSecStaxOutInterceptorInternal createEndingInterceptor() {
        return new XmlSecStaxOutInterceptorInternal();
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

    private static SecurityTokenConstants.KeyIdentifier convertKeyIdentifier(String keyIdentifier) {
        if ("IssuerSerial".equals(keyIdentifier)) {
            return SecurityTokenConstants.KeyIdentifier_IssuerSerial;
        } else if ("X509KeyIdentifier".equals(keyIdentifier)) {
            return SecurityTokenConstants.KeyIdentifier_X509KeyIdentifier;
        } else if ("SKIKeyIdentifier".equals(keyIdentifier)) {
            return SecurityTokenConstants.KeyIdentifier_SkiKeyIdentifier;
        } else if ("KeyValue".equals(keyIdentifier)) {
            return SecurityTokenConstants.KeyIdentifier_KeyValue;
        } else if ("KeyName".equals(keyIdentifier)) {
            return SecurityTokenConstants.KeyIdentifier_KeyName;
        }
        return SecurityTokenConstants.KeyIdentifier_X509KeyIdentifier;
    }

    public boolean isSignRequest() {
        return signRequest;
    }

    public void setSignRequest(boolean signRequest) {
        this.signRequest = signRequest;
    }

    public boolean isEncryptRequest() {
        return encryptRequest;
    }

    public void setEncryptRequest(boolean encryptRequest) {
        this.encryptRequest = encryptRequest;
    }

    public void setElementsToEncrypt(List<QName> elementsToEncrypt) {
        this.elementsToEncrypt = elementsToEncrypt;
    }

    public void addElementToEncrypt(QName elementToEncrypt) {
        elementsToEncrypt.add(elementToEncrypt);
    }

    public void setElementsToSign(List<QName> elementsToSign) {
        this.elementsToSign = elementsToSign;
    }

    public void addElementToSign(QName elementToSign) {
        elementsToSign.add(elementToSign);
    }

    final class XmlSecStaxOutInterceptorInternal extends AbstractPhaseInterceptor<Message> {
        XmlSecStaxOutInterceptorInternal() {
            super(Phase.PRE_STREAM_ENDING);
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

    public boolean isEncryptSymmetricKey() {
        return encryptSymmetricKey;
    }

    public void setEncryptSymmetricKey(boolean encryptSymmetricKey) {
        this.encryptSymmetricKey = encryptSymmetricKey;
    }

    public SecretKey getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(SecretKey symmetricKey) {
        this.symmetricKey = symmetricKey;
    }
}
