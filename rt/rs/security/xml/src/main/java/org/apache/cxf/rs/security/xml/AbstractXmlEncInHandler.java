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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.rs.security.common.TrustValidator;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.utils.Constants;


public abstract class AbstractXmlEncInHandler extends AbstractXmlSecInHandler {
    
    public void decryptContent(Message message) {
        Message outMs = message.getExchange().getOutMessage();
        Message inMsg = outMs == null ? message : outMs.getExchange().getInMessage();
        Document doc = getDocument(inMsg);
        if (doc == null) {
            return;
        }
        
        Element root = doc.getDocumentElement();
        
        byte[] symmetricKeyBytes = getSymmetricKeyBytes(message, root);
                
        String algorithm = getEncodingMethodAlgorithm(root);
        byte[] decryptedPayload = null;
        try {
            decryptedPayload = decryptPayload(root, symmetricKeyBytes, algorithm);
        } catch (Exception ex) {
            throwFault("Payload can not be decrypted", ex);
        }
        
        Document payloadDoc = null;
        try {
            payloadDoc = DOMUtils.readXml(new InputStreamReader(new ByteArrayInputStream(decryptedPayload),
                                               "UTF-8"));
        } catch (Exception ex) {
            throwFault("Payload document can not be created", ex);
        }
        message.setContent(XMLStreamReader.class, 
                           new W3CDOMStreamReader(payloadDoc));
        message.setContent(InputStream.class, null);
    }
    
    // Subclasses can overwrite it and return the bytes, assuming they know the actual key
    protected byte[] getSymmetricKeyBytes(Message message, Element encDataElement) {
        Crypto crypto = null;
        try {
            crypto = new CryptoLoader().getCrypto(message,
                               SecurityConstants.ENCRYPT_CRYPTO,
                               SecurityConstants.ENCRYPT_PROPERTIES);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
        }
        
        Element encKeyElement = getNode(encDataElement, WSConstants.ENC_NS, "EncryptedKey", 0);
        if (encKeyElement == null) {
            //TODO: support EncryptedData/ds:KeyInfo - the encrypted key is passed out of band
            throwFault("EncryptedKey element is not available", null);
        }
        
        X509Certificate cert = loadCertificate(crypto, encKeyElement);
        
        try {
            new TrustValidator().validateTrust(crypto, cert, null);
        } catch (Exception ex) {
            throwFault(ex.getMessage(), ex);
        }
        
        // now start decrypting
        String algorithm = getEncodingMethodAlgorithm(encKeyElement);
        String digestAlgorithm = getDigestMethodAlgorithm(encKeyElement);
        Element cipherValue = getNode(encKeyElement, WSConstants.ENC_NS, 
                                               "CipherValue", 0);
        if (cipherValue == null) {
            throwFault("CipherValue element is not available", null);
        }
        try {
            return decryptSymmetricKey(cipherValue.getTextContent().trim(),
                                       cert,
                                       crypto,
                                       algorithm,
                                       digestAlgorithm,
                                       message);
        } catch (Exception ex) {
            throwFault(ex.getMessage(), ex);
        }
        return null;
    }
    
    private X509Certificate loadCertificate(Crypto crypto, Element encKeyElement) {
        /**
         * TODO: the following can be easily supported too  
         <X509SKI>31d97bd7</X509SKI>
         <X509SubjectName>Subject of Certificate B</X509SubjectName>
         * 
         */
        
        Element certNode = getNode(encKeyElement, 
                                   Constants.SignatureSpecNS, "X509Certificate", 0);
        if (certNode != null) {
            try {
                return SecurityUtils.loadX509Certificate(crypto, certNode);
            } catch (Exception ex) {
                throwFault("X509Certificate can not be created", ex);
            }
        }
        certNode = getNode(encKeyElement, 
                Constants.SignatureSpecNS, "X509IssuerSerial", 0);
        if (certNode != null) {
            try {
                return SecurityUtils.loadX509IssuerSerial(crypto, certNode);
            } catch (Exception ex) {
                throwFault("X509Certificate can not be created", ex);
            }
        }
        throwFault("Certificate is missing", null);
        return null;
    }
    
    private String getEncodingMethodAlgorithm(Element parent) {
        Element encMethod = getNode(parent, WSConstants.ENC_NS, "EncryptionMethod", 0);
        if (encMethod == null) {
            throwFault("EncryptionMethod element is not available", null);
        }
        return encMethod.getAttribute("Algorithm");
    }
    
    private String getDigestMethodAlgorithm(Element parent) {
        Element encMethod = getNode(parent, WSConstants.ENC_NS, "EncryptionMethod", 0);
        if (encMethod != null) {
            Element digestMethod = getNode(encMethod, WSConstants.SIG_NS, "DigestMethod", 0);
            if (digestMethod != null) {
                return digestMethod.getAttributeNS(null, "Algorithm");
            }
        }
        return null;
    }

    //TODO: Support symmetric keys if requested
    protected byte[] decryptSymmetricKey(String base64EncodedKey, 
                                         X509Certificate cert,
                                         Crypto crypto,
                                         String keyEncAlgo,
                                         Message message) throws WSSecurityException {
        return decryptSymmetricKey(base64EncodedKey, cert, crypto, keyEncAlgo, null, message);
    }
    
    //TODO: Support symmetric keys if requested
    protected byte[] decryptSymmetricKey(String base64EncodedKey, 
                                         X509Certificate cert,
                                         Crypto crypto,
                                         String keyEncAlgo,
                                         String digestAlgo,
                                         Message message) throws WSSecurityException {
        CallbackHandler callback = SecurityUtils.getCallbackHandler(message, this.getClass());
        PrivateKey key = null;
        try {
            key = crypto.getPrivateKey(cert, callback);
        } catch (Exception ex) {
            throwFault("Encrypted key can not be decrypted", ex);
        }
        Cipher cipher = 
            EncryptionUtils.initCipherWithKey(keyEncAlgo, digestAlgo, Cipher.DECRYPT_MODE, key);
        try {
            byte[] encryptedBytes = Base64Utility.decode(base64EncodedKey);
            return cipher.doFinal(encryptedBytes);
        } catch (Base64Exception ex) {
            throwFault("Base64 decoding has failed", ex);
        } catch (Exception ex) {
            throwFault("Encrypted key can not be decrypted", ex);
        }
        return null;
        
    }
    
    protected byte[] decryptPayload(Element root, 
                                    byte[] secretKeyBytes,
                                    String symEncAlgo) throws WSSecurityException {
        SecretKey key = WSSecurityUtil.prepareSecretKey(symEncAlgo, secretKeyBytes);
        try {
            XMLCipher xmlCipher = 
                EncryptionUtils.initXMLCipher(symEncAlgo, XMLCipher.DECRYPT_MODE, key);
            return xmlCipher.decryptToByteArray(root);
        } catch (XMLEncryptionException ex) {
            throw new WSSecurityException(
                WSSecurityException.UNSUPPORTED_ALGORITHM, null, null, ex
            );
        }
        
    }
    
    
}
