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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.stax.ext.OutboundXMLSec;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.EncryptionConstants;
import org.opensaml.xml.signature.SignatureConstants;

/**
 * A new StAX-based interceptor for creating messages with XML Signature + Encryption content.
 */
public class XmlSecOutInterceptor implements PhaseInterceptor<Message> {
    public static final String OUTPUT_STREAM_HOLDER = 
        XmlSecOutInterceptor.class.getName() + ".outputstream";
    private static final Logger LOG = LogUtils.getL7dLogger(XmlSecOutInterceptor.class);
    
    private XmlSecStaxOutInterceptorInternal ending;
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
    private EncryptionProperties encryptionProperties = new EncryptionProperties();
    private SignatureProperties sigProps = new SignatureProperties();
    private String phase;
    //private boolean encryptSymmetricKey = true;
    //private SecretKey symmetricKey;
    private boolean signRequest;
    private boolean encryptRequest;
    private List<QName> elementsToSign = new ArrayList<QName>();
    private List<QName> elementsToEncrypt = new ArrayList<QName>();

    public XmlSecOutInterceptor() {
        setPhase(Phase.PRE_STREAM);
        getBefore().add(StaxOutInterceptor.class.getName());
        
        ending = createEndingInterceptor();
    }
    
    public void handleFault(Message message) {
    }

    public void handleMessage(Message message) throws Fault {
        OutputStream os = message.getContent(OutputStream.class);
        String encoding = getEncoding(message);
        
        if (!(encryptRequest || signRequest)) {
            Exception ex = new Exception("Either encryption and/or signature must be enabled");
            throwFault(ex.getMessage(), ex);
        }
        
        XMLStreamWriter newXMLStreamWriter = null;
        try {
            XMLSecurityProperties properties = new XMLSecurityProperties();
            
            if (encryptRequest) {
                // Configure algorithms
                /*
                if (encryptionProperties != null) {
                    if (encryptionProperties.getEncryptionKeyTransportAlgo() != null) {
                        properties.setEncryptionKeyTransportAlgorithm(
                            encryptionProperties.getEncryptionKeyTransportAlgo());
                    }
                    if (encryptionProperties.getEncryptionSymmetricKeyAlgo() != null) {
                        properties.setEncryptionSymAlgorithm(
                            encryptionProperties.getEncryptionSymmetricKeyAlgo());
                    }
                    if (encryptionProperties.getEncryptionDigestAlgo() != null) {
                        properties.setEncryptionKeyTransportDigestAlgorithm(
                            encryptionProperties.getEncryptionDigestAlgo());
                    }
                    */
                    /* TODO
                    if (encryptionProperties.getEncryptionKeyIdType() != null) {
                        if (encryptionProperties.getEncryptionKeyIdType().equals(SecurityUtils.X509_CERT)) {
                        } else if (encryptionProperties.getEncryptionKeyIdType().equals(
                            SecurityUtils.X509_ISSUER_SERIAL)) {
                            
                        } else {
                            throw new Exception("Unsupported key identifier:" + keyIdType);
                        }
                    }
                }
                */
            }
            
            if (signRequest) {
                configureSignature(message, properties);
            }
            
            OutboundXMLSec outboundXMLSec = XMLSec.getOutboundXMLSec(properties);
            
            newXMLStreamWriter = outboundXMLSec.processOutMessage(os, encoding);
            message.setContent(XMLStreamWriter.class, newXMLStreamWriter);
        } catch (XMLSecurityException e) {
            throwFault(e.getMessage(), e);
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
    
    /*
    private void configureKeys(Message message, XMLSecurityProperties properties) 
        throws Exception {
        if (symmetricKey != null) {
            properties.setEncryptionKey(symmetricKey);
        }
        if (encryptSymmetricKey) {
            
        }
        String userName = 
            (String)message.getContextualProperty(SecurityConstants.ENCRYPT_USERNAME);
        CryptoLoader loader = new CryptoLoader();
        Crypto crypto = loader.getCrypto(message, 
                                  SecurityConstants.ENCRYPT_CRYPTO,
                                  SecurityConstants.ENCRYPT_PROPERTIES);
        
        userName = SecurityUtils.getUserName(crypto, userName);
        if (StringUtils.isEmpty(userName)) {
            throw new Exception("User name is not available");
        }
        X509Certificate receiverCert = getReceiverCertificateFromCrypto(crypto, userName);
        if (receiverCert == null) {
            throw new Exception("Receiver certificate is not available");
        }
        
        properties.setEncryptionKey(receiverCert.getPublicKey());
        properties.setEncryptionUseThisCertificate(receiverCert);
    }
    
    private X509Certificate getReceiverCertificateFromCrypto(Crypto crypto, String user) throws Exception {
        X509Certificate[] certs = SecurityUtils.getCertificates(crypto, user);
        return certs[0];
    }
    */
    private void configureSignature(
        Message message, XMLSecurityProperties properties
    ) throws Exception {
        if (elementsToSign == null || elementsToSign.isEmpty()) {
            throw new Exception("An Element to Sign must be specified");
        }
        
        String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
        
        CryptoLoader loader = new CryptoLoader();
        Crypto crypto = loader.getCrypto(message, 
                                         SecurityConstants.SIGNATURE_CRYPTO,
                                         SecurityConstants.SIGNATURE_PROPERTIES);
        String user = SecurityUtils.getUserName(message, crypto, userNameKey);
         
        if (StringUtils.isEmpty(user) || SecurityUtils.USE_REQUEST_SIGNATURE_CERT.equals(user)) {
            throw new Exception("User name is not available");
        }

        String password = 
            SecurityUtils.getPassword(message, user, WSPasswordCallback.SIGNATURE, this.getClass());
    
        X509Certificate[] issuerCerts = SecurityUtils.getCertificates(crypto, user);
        properties.setSignatureCerts(issuerCerts);
        
        String sigAlgo = sigProps.getSignatureAlgo() == null 
            ? SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1 : sigProps.getSignatureAlgo();
        properties.setSignatureAlgorithm(sigAlgo);
        
        String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
        if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
            sigAlgo = XMLSignature.ALGO_ID_SIGNATURE_DSA;
        }
        PrivateKey privateKey = null;
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
        
        properties.setSignatureKeyIdentifier(
            convertKeyIdentifier(sigProps.getSignatureKeyIdType()));
        
        properties.addAction(XMLSecurityConstants.SIGNATURE);
        // Only enveloped supported for the moment.
        SecurePart securePart = 
            new SecurePart(elementsToSign.get(0), SecurePart.Modifier.Element,
                           new String[]{
                               "http://www.w3.org/2000/09/xmldsig#enveloped-signature",
                               "http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
                           },
                           digestAlgo);
        properties.addSignaturePart(securePart);
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
    
    public void setKeyIdentifierType(String type) {
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
    
    public void setSignatureProperties(SignatureProperties props) {
        this.sigProps = props;
    }
    
    public void setSignatureAlgorithm(String algo) {
        sigProps.setSignatureAlgo(algo);
    }
    
    public void setSignatureDigestAlgorithm(String algo) {
        sigProps.setSignatureDigestAlgo(algo);
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
            encoding = "UTF-8";
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
        public XmlSecStaxOutInterceptorInternal() {
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
}
