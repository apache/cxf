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
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rs.security.common.TrustValidator;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.utils.Constants;

public abstract class AbstractXmlEncInHandler extends AbstractXmlSecInHandler {

    private EncryptionProperties encProps;

    public void decryptContent(Message message) {
        Message outMs = message.getExchange().getOutMessage();
        Message inMsg = outMs == null ? message : outMs.getExchange().getInMessage();
        Document doc = getDocument(inMsg);
        if (doc == null) {
            return;
        }

        Element root = doc.getDocumentElement();

        byte[] symmetricKeyBytes = getSymmetricKeyBytes(message, root);

        String symKeyAlgo = getEncodingMethodAlgorithm(root);

        if (encProps != null && encProps.getEncryptionSymmetricKeyAlgo() != null
            && !encProps.getEncryptionSymmetricKeyAlgo().equals(symKeyAlgo)) {
            throwFault("Encryption Symmetric Key Algorithm is not supported", null);
        }


        byte[] decryptedPayload = null;
        try {
            decryptedPayload = decryptPayload(root, symmetricKeyBytes, symKeyAlgo);
        } catch (Exception ex) {
            throwFault("Payload can not be decrypted", ex);
        }

        // Clean the secret key from memory
        Arrays.fill(symmetricKeyBytes, (byte) 0);

        Document payloadDoc = null;
        try {
            payloadDoc = StaxUtils.read(new InputStreamReader(new ByteArrayInputStream(decryptedPayload),
                                               StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throwFault("Payload document can not be created", ex);
        }
        message.setContent(XMLStreamReader.class,
                           new W3CDOMStreamReader(payloadDoc));
        message.setContent(InputStream.class, null);
    }

    // Subclasses can overwrite it and return the bytes, assuming they know the actual key
    protected byte[] getSymmetricKeyBytes(Message message, Element encDataElement) {

        String cryptoKey = null;
        String propKey = null;
        if (RSSecurityUtils.isSignedAndEncryptedTwoWay(message)) {
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

        Element encKeyElement = getNode(encDataElement, ENC_NS, "EncryptedKey", 0);
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
        String keyEncAlgo = getEncodingMethodAlgorithm(encKeyElement);
        String digestAlgo = getDigestMethodAlgorithm(encKeyElement);

        if (encProps != null) {
            if (encProps.getEncryptionKeyTransportAlgo() != null
                && !encProps.getEncryptionKeyTransportAlgo().equals(keyEncAlgo)) {
                throwFault("Key Transport Algorithm is not supported", null);
            }
            if (encProps.getEncryptionDigestAlgo() != null
                && (digestAlgo == null || !encProps.getEncryptionDigestAlgo().equals(digestAlgo))) {
                throwFault("Digest Algorithm is not supported", null);
            }
        } else if (!XMLCipher.RSA_OAEP.equals(keyEncAlgo)) {
            // RSA OAEP is the required default Key Transport Algorithm
            throwFault("Key Transport Algorithm is not supported", null);
        }


        Element cipherValue = getNode(encKeyElement, ENC_NS, "CipherValue", 0);
        if (cipherValue == null) {
            throwFault("CipherValue element is not available", null);
        }
        try {
            return decryptSymmetricKey(cipherValue.getTextContent().trim(),
                                       cert,
                                       crypto,
                                       keyEncAlgo,
                                       digestAlgo,
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

        String keyIdentifierType = encProps != null ? encProps.getEncryptionKeyIdType() : null;
        if (keyIdentifierType == null || keyIdentifierType.equals(RSSecurityUtils.X509_CERT)) {
            Element certNode = getNode(encKeyElement,
                                       Constants.SignatureSpecNS, "X509Certificate", 0);
            if (certNode != null) {
                try {
                    return RSSecurityUtils.loadX509Certificate(crypto, certNode);
                } catch (Exception ex) {
                    throwFault("X509Certificate can not be created", ex);
                }
            }
        }
        if (keyIdentifierType == null || keyIdentifierType.equals(RSSecurityUtils.X509_ISSUER_SERIAL)) {
            Element certNode = getNode(encKeyElement,
                    Constants.SignatureSpecNS, "X509IssuerSerial", 0);
            if (certNode != null) {
                try {
                    return RSSecurityUtils.loadX509IssuerSerial(crypto, certNode);
                } catch (Exception ex) {
                    throwFault("X509Certificate can not be created", ex);
                }
            }
        }
        throwFault("Certificate is missing", null);
        return null;
    }

    private String getEncodingMethodAlgorithm(Element parent) {
        Element encMethod = getNode(parent, ENC_NS, "EncryptionMethod", 0);
        if (encMethod == null) {
            throwFault("EncryptionMethod element is not available", null);
        }
        return encMethod.getAttribute("Algorithm");
    }

    private String getDigestMethodAlgorithm(Element parent) {
        Element encMethod = getNode(parent, ENC_NS, "EncryptionMethod", 0);
        if (encMethod != null) {
            Element digestMethod = getNode(encMethod, SIG_NS, "DigestMethod", 0);
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
        CallbackHandler callback = RSSecurityUtils.getCallbackHandler(message, this.getClass());
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
            byte[] decryptedKey = cipher.doFinal(encryptedBytes);

            // Clean the private key from memory now that we're finished with it
            try {
                key.destroy();
            } catch (DestroyFailedException ex) {
                // ignore
            }

            return decryptedKey;
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
        SecretKey key = KeyUtils.prepareSecretKey(symEncAlgo, secretKeyBytes);
        try {
            XMLCipher xmlCipher =
                EncryptionUtils.initXMLCipher(symEncAlgo, XMLCipher.DECRYPT_MODE, key);
            byte[] decryptedContent = xmlCipher.decryptToByteArray(root);

            // Clean the private key from memory now that we're finished with it
            try {
                key.destroy();
            } catch (DestroyFailedException ex) {
                // ignore
            }

            return decryptedContent;
        } catch (XMLEncryptionException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, ex);
        }

    }

    public void setEncryptionProperties(EncryptionProperties properties) {
        this.encProps = properties;
    }

}
