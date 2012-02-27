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

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.DOMX509Data;
import org.apache.ws.security.message.token.DOMX509IssuerSerial;
import org.apache.ws.security.util.Base64;
import org.apache.ws.security.util.UUIDGenerator;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;

public class XmlEncOutInterceptor extends AbstractXmlSecOutInterceptor {
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(XmlEncOutInterceptor.class);
    private static final String DEFAULT_RETRIEVAL_METHOD_TYPE =
        "http://www.w3.org/2001/04/xmlenc#EncryptedKey";
    
    private boolean encryptSymmetricKey = true;
    private SecretKey symmetricKey;
    private String keyEncAlgo = XMLCipher.RSA_OAEP; 
    private String symEncAlgo = XMLCipher.AES_256;
    private String keyIdentifierType = SecurityUtils.X509_KEY;
    
    public XmlEncOutInterceptor() {
        addAfter(XmlSigOutInterceptor.class.getName());
    } 

    public void setKeyIdentifierType(String type) {
        keyIdentifierType = type;
    }
    
    public void setSymmetricEncAlgorithm(String algo) {
        if (!(algo.startsWith(EncryptionConstants.EncryptionSpecNS)
            || algo.startsWith(EncryptionConstants.EncryptionSpec11NS))) {
            algo = EncryptionConstants.EncryptionSpecNS + algo;
        }
        symEncAlgo = algo;
    }
    
    public void setKeyEncAlgorithm(String algo) {
        keyEncAlgo = algo;
    }
    
    protected Document processDocument(Message message, Document payloadDoc) 
        throws Exception {
        return encryptDocument(message, payloadDoc);
    }
    
    protected Document encryptDocument(Message message, Document payloadDoc) 
        throws Exception {
        
        byte[] secretKey = getSymmetricKey();

        Document encryptedDataDoc = DOMUtils.createDocument();
        Element encryptedDataElement = createEncryptedDataElement(encryptedDataDoc);
        if (encryptSymmetricKey) {
            CryptoLoader loader = new CryptoLoader();
            Crypto crypto = loader.getCrypto(message, 
                                      SecurityConstants.ENCRYPT_CRYPTO,
                                      SecurityConstants.ENCRYPT_PROPERTIES);
            
            String user = 
                SecurityUtils.getUserName(message, crypto, SecurityConstants.ENCRYPT_USERNAME);
            if (StringUtils.isEmpty(user)) {
                return null;
            }
            X509Certificate cert = getReceiverCertificate(crypto, user);
            byte[] encryptedSecretKey = encryptSymmetricKey(secretKey, cert, crypto);

            addEncryptedKeyElement(encryptedDataElement, cert, encryptedSecretKey);
        }
               
        // encrypt payloadDoc
        XMLCipher xmlCipher = 
            EncryptionUtils.initXMLCipher(symEncAlgo, XMLCipher.ENCRYPT_MODE, symmetricKey);
        
        Document result = xmlCipher.doFinal(payloadDoc, payloadDoc.getDocumentElement(), false);
        NodeList list = result.getElementsByTagNameNS(WSConstants.ENC_NS, "CipherValue");
        if (list.getLength() != 1) {
            throw new WSSecurityException("Payload CipherData is missing", null);
        }
        String cipherText = ((Element)list.item(0)).getTextContent().trim();
        Element cipherValue = 
            createCipherValue(encryptedDataDoc, encryptedDataDoc.getDocumentElement());
        cipherValue.appendChild(encryptedDataDoc.createTextNode(cipherText));
         
        //StaxUtils.copy(new DOMSource(encryptedDataDoc), System.out);
        return encryptedDataDoc;
    }
    
    private byte[] getSymmetricKey() throws Exception {
        synchronized (this) {
            if (symmetricKey == null) {
                KeyGenerator keyGen = getKeyGenerator();
                symmetricKey = keyGen.generateKey();
            } 
        }
        return symmetricKey.getEncoded();
    }
    
    private X509Certificate getReceiverCertificate(Crypto crypto, String user) throws Exception {
        X509Certificate[] certs = SecurityUtils.getCertificates(crypto, user);
        return certs[0];
    }
    
    private KeyGenerator getKeyGenerator() throws WSSecurityException {
        try {
            //
            // Assume AES as default, so initialize it
            //
            String keyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(symEncAlgo);
            KeyGenerator keyGen = KeyGenerator.getInstance(keyAlgorithm);
            if (symEncAlgo.equalsIgnoreCase(WSConstants.AES_128)
                || symEncAlgo.equalsIgnoreCase(WSConstants.AES_128_GCM)) {
                keyGen.init(128);
            } else if (symEncAlgo.equalsIgnoreCase(WSConstants.AES_192)
                || symEncAlgo.equalsIgnoreCase(WSConstants.AES_192_GCM)) {
                keyGen.init(192);
            } else if (symEncAlgo.equalsIgnoreCase(WSConstants.AES_256)
                || symEncAlgo.equalsIgnoreCase(WSConstants.AES_256_GCM)) {
                keyGen.init(256);
            }
            return keyGen;
        } catch (NoSuchAlgorithmException e) {
            throw new WSSecurityException(
                WSSecurityException.UNSUPPORTED_ALGORITHM, null, null, e
            );
        }
    }
    
    // Apache Security XMLCipher does not support 
    // Certificates for encrypting the keys
    protected byte[] encryptSymmetricKey(byte[] keyBytes, 
                                         X509Certificate remoteCert,
                                         Crypto crypto) throws WSSecurityException {
        Cipher cipher = 
            EncryptionUtils.initCipherWithCert(keyEncAlgo, Cipher.ENCRYPT_MODE, remoteCert);
        int blockSize = cipher.getBlockSize();
        if (blockSize > 0 && blockSize < keyBytes.length) {
            String message = "Public key algorithm too weak to encrypt symmetric key";
            LOG.severe(message);
            throw new WSSecurityException(
                WSSecurityException.FAILURE,
                "unsupportedKeyTransp",
                new Object[] {message}
            );
        }
        byte[] encryptedEphemeralKey = null;
        try {
            encryptedEphemeralKey = cipher.doFinal(keyBytes);
        } catch (IllegalStateException ex) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, ex
            );
        } catch (IllegalBlockSizeException ex) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, ex
            );
        } catch (BadPaddingException ex) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, ex
            );
        }
       
        return encryptedEphemeralKey;
       
    }
    
    private void addEncryptedKeyElement(Element encryptedDataElement,
                                        X509Certificate cert,
                                        byte[] encryptedKey) throws Exception {
        
        Document doc = encryptedDataElement.getOwnerDocument();
        
        String encodedKey = Base64Utility.encode(encryptedKey);
        Element encryptedKeyElement = createEncryptedKeyElement(doc);
        String encKeyId = "EK-" + UUIDGenerator.getUUID();
        encryptedKeyElement.setAttributeNS(null, "Id", encKeyId);
                
        Element keyInfoElement = createKeyInfoElement(doc, cert);
        encryptedKeyElement.appendChild(keyInfoElement);
        
        Element xencCipherValue = createCipherValue(doc, encryptedKeyElement);
        xencCipherValue.appendChild(doc.createTextNode(encodedKey));
        
        Element topKeyInfoElement = 
            doc.createElementNS(
                WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":" + WSConstants.KEYINFO_LN
            );
        Element retrievalMethodElement = 
            doc.createElementNS(
                WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":RetrievalMethod"
            );
        retrievalMethodElement.setAttribute("Type", DEFAULT_RETRIEVAL_METHOD_TYPE);
        topKeyInfoElement.appendChild(retrievalMethodElement);
        
        topKeyInfoElement.appendChild(encryptedKeyElement);
        
        encryptedDataElement.appendChild(topKeyInfoElement);
    }
    
    protected Element createCipherValue(Document doc, Element encryptedKey) {
        Element cipherData = 
            doc.createElementNS(WSConstants.ENC_NS, WSConstants.ENC_PREFIX + ":CipherData");
        Element cipherValue = 
            doc.createElementNS(WSConstants.ENC_NS, WSConstants.ENC_PREFIX + ":CipherValue");
        cipherData.appendChild(cipherValue);
        encryptedKey.appendChild(cipherData);
        return cipherValue;
    }
    
    private Element createKeyInfoElement(Document encryptedDataDoc,
                                         X509Certificate remoteCert) throws Exception {
        Element keyInfoElement = 
            encryptedDataDoc.createElementNS(
                WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":" + WSConstants.KEYINFO_LN
            );
        
        Node keyIdentifierNode = null; 
        if (keyIdentifierType.equals(SecurityUtils.X509_KEY)) {
            byte data[] = null;
            try {
                data = remoteCert.getEncoded();
            } catch (CertificateEncodingException e) {
                throw new WSSecurityException(
                    WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "encodeError", null, e
                );
            }
            Text text = encryptedDataDoc.createTextNode(Base64.encode(data));
            Element cert = encryptedDataDoc.createElementNS(
                WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":" + WSConstants.X509_CERT_LN);
            cert.appendChild(text);
            Element x509Data = encryptedDataDoc.createElementNS(
                WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":" + WSConstants.X509_DATA_LN);
            
            x509Data.appendChild(cert);
            keyIdentifierNode = x509Data;
        } else if (keyIdentifierType.equals(SecurityUtils.X509_ISSUER_SERIAL)) {
            String issuer = remoteCert.getIssuerDN().getName();
            java.math.BigInteger serialNumber = remoteCert.getSerialNumber();
            DOMX509IssuerSerial domIssuerSerial = 
                new DOMX509IssuerSerial(
                    encryptedDataDoc, issuer, serialNumber
                );
            DOMX509Data domX509Data = new DOMX509Data(encryptedDataDoc, domIssuerSerial);
            keyIdentifierNode = domX509Data.getElement();
        } else {
            throw new WSSecurityException("Unsupported key identifier:" + keyIdentifierType);
        }
 
        keyInfoElement.appendChild(keyIdentifierNode);
        
        return keyInfoElement;
    }
    
    protected Element createEncryptedKeyElement(Document encryptedDataDoc) {
        Element encryptedKey = 
            encryptedDataDoc.createElementNS(WSConstants.ENC_NS, WSConstants.ENC_PREFIX + ":EncryptedKey");

        Element encryptionMethod = 
            encryptedDataDoc.createElementNS(WSConstants.ENC_NS, WSConstants.ENC_PREFIX 
                                             + ":EncryptionMethod");
        encryptionMethod.setAttributeNS(null, "Algorithm", keyEncAlgo);
        encryptedKey.appendChild(encryptionMethod);
        return encryptedKey;
    }
    
    protected Element createEncryptedDataElement(Document encryptedDataDoc) {
        Element encryptedData = 
            encryptedDataDoc.createElementNS(WSConstants.ENC_NS, WSConstants.ENC_PREFIX + ":EncryptedData");

        WSSecurityUtil.setNamespace(encryptedData, WSConstants.ENC_NS, WSConstants.ENC_PREFIX);
        
        Element encryptionMethod = 
            encryptedDataDoc.createElementNS(WSConstants.ENC_NS, WSConstants.ENC_PREFIX 
                                             + ":EncryptionMethod");
        encryptionMethod.setAttributeNS(null, "Algorithm", symEncAlgo);
        encryptedData.appendChild(encryptionMethod);
        encryptedDataDoc.appendChild(encryptedData);
        
        return encryptedData;
    }
    
    
    
}
