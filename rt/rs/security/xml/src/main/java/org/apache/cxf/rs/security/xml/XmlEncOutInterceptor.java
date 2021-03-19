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
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.DOMX509Data;
import org.apache.wss4j.common.token.DOMX509IssuerSerial;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.utils.EncryptionConstants;

public class XmlEncOutInterceptor extends AbstractXmlSecOutInterceptor {

    private static final Logger LOG =
        LogUtils.getL7dLogger(XmlEncOutInterceptor.class);
    private static final String DEFAULT_RETRIEVAL_METHOD_TYPE =
        "http://www.w3.org/2001/04/xmlenc#EncryptedKey";

    private boolean encryptSymmetricKey = true;
    private SecretKey symmetricKey;

    private EncryptionProperties encProps = new EncryptionProperties();

    public XmlEncOutInterceptor() {
        addAfter(XmlSigOutInterceptor.class.getName());
    }

    public void setEncryptionProperties(EncryptionProperties props) {
        this.encProps = props;
    }

    public void setKeyIdentifierType(String type) {
        encProps.setEncryptionKeyIdType(type);
    }

    public void setSymmetricEncAlgorithm(String algo) {
        if (!(algo.startsWith(EncryptionConstants.EncryptionSpecNS)
            || algo.startsWith(EncryptionConstants.EncryptionSpec11NS))) {
            algo = EncryptionConstants.EncryptionSpecNS + algo;
        }
        encProps.setEncryptionSymmetricKeyAlgo(algo);
    }

    public void setKeyEncAlgorithm(String algo) {
        encProps.setEncryptionKeyTransportAlgo(algo);
    }

    public void setDigestAlgorithm(String algo) {
        encProps.setEncryptionDigestAlgo(algo);
    }

    protected Document processDocument(Message message, Document payloadDoc)
        throws Exception {
        return encryptDocument(message, payloadDoc);
    }

    protected Document encryptDocument(Message message, Document payloadDoc)
        throws Exception {

        String symEncAlgo = encProps.getEncryptionSymmetricKeyAlgo() == null
            ? XMLCipher.AES_256 : encProps.getEncryptionSymmetricKeyAlgo();

        byte[] secretKey = getSymmetricKey(symEncAlgo);

        Document encryptedDataDoc = DOMUtils.createDocument();
        Element encryptedDataElement = createEncryptedDataElement(encryptedDataDoc, symEncAlgo);
        if (encryptSymmetricKey) {
            X509Certificate receiverCert;

            String userName =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_USERNAME, message);
            if (RSSecurityUtils.USE_REQUEST_SIGNATURE_CERT.equals(userName)
                && !MessageUtils.isRequestor(message)) {
                receiverCert =
                    (X509Certificate)message.getExchange().getInMessage().get(
                        AbstractXmlSecInHandler.SIGNING_CERT);
                if (receiverCert == null) {
                    receiverCert =
                        (X509Certificate)message.getExchange().getInMessage().get(
                            SecurityConstants.ENCRYPT_CERT);
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
                receiverCert = getReceiverCertificateFromCrypto(crypto, userName);
            }
            if (receiverCert == null) {
                throw new Exception("Receiver certificate is not available");
            }

            String keyEncAlgo = encProps.getEncryptionKeyTransportAlgo() == null
                ? XMLCipher.RSA_OAEP : encProps.getEncryptionKeyTransportAlgo();
            String digestAlgo = encProps.getEncryptionDigestAlgo();

            byte[] encryptedSecretKey = encryptSymmetricKey(secretKey, receiverCert,
                                                            keyEncAlgo, digestAlgo);
            addEncryptedKeyElement(encryptedDataElement, receiverCert, encryptedSecretKey,
                                   keyEncAlgo, digestAlgo);
        }

        // encrypt payloadDoc
        XMLCipher xmlCipher =
            EncryptionUtils.initXMLCipher(symEncAlgo, XMLCipher.ENCRYPT_MODE, symmetricKey);

        Document result = xmlCipher.doFinal(payloadDoc, payloadDoc.getDocumentElement(), false);
        NodeList list = result.getElementsByTagNameNS(ENC_NS, "CipherValue");
        if (list.getLength() != 1) {
            throw new Exception("Payload CipherData is missing");
        }
        String cipherText = ((Element)list.item(0)).getTextContent().trim();
        Element cipherValue =
            createCipherValue(encryptedDataDoc, encryptedDataDoc.getDocumentElement());
        cipherValue.appendChild(encryptedDataDoc.createTextNode(cipherText));

        //StaxUtils.copy(new DOMSource(encryptedDataDoc), System.out);
        return encryptedDataDoc;
    }

    private byte[] getSymmetricKey(String symEncAlgo) throws Exception {
        synchronized (this) {
            if (symmetricKey == null) {
                KeyGenerator keyGen = KeyUtils.getKeyGenerator(symEncAlgo);
                symmetricKey = keyGen.generateKey();
            }
            return symmetricKey.getEncoded();
        }
    }

    private X509Certificate getReceiverCertificateFromCrypto(Crypto crypto, String user) throws Exception {
        X509Certificate[] certs = RSSecurityUtils.getCertificates(crypto, user);
        return certs[0];
    }

    // Apache Security XMLCipher does not support
    // Certificates for encrypting the keys
    protected byte[] encryptSymmetricKey(byte[] keyBytes,
                                         X509Certificate remoteCert,
                                         String keyEncAlgo,
                                         String digestAlgo) throws WSSecurityException {
        Cipher cipher =
            EncryptionUtils.initCipherWithCert(
                keyEncAlgo, digestAlgo, Cipher.ENCRYPT_MODE, remoteCert
            );
        int blockSize = cipher.getBlockSize();
        if (blockSize > 0 && blockSize < keyBytes.length) {
            String message = "Public key algorithm too weak to encrypt symmetric key";
            LOG.severe(message);
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE,
                "unsupportedKeyTransp",
                new Object[] {message}
            );
        }
        final byte[] encryptedEphemeralKey;
        try {
            encryptedEphemeralKey = cipher.doFinal(keyBytes);
        } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_ENCRYPTION, ex
            );
        }

        return encryptedEphemeralKey;

    }

    private void addEncryptedKeyElement(Element encryptedDataElement,
                                        X509Certificate cert,
                                        byte[] encryptedKey,
                                        String keyEncAlgo,
                                        String digestAlgo) throws Exception {

        Document doc = encryptedDataElement.getOwnerDocument();

        String encodedKey = org.apache.xml.security.utils.XMLUtils.encodeToString(encryptedKey);
        Element encryptedKeyElement = createEncryptedKeyElement(doc, keyEncAlgo, digestAlgo);
        String encKeyId = IDGenerator.generateID("EK-");
        encryptedKeyElement.setAttributeNS(null, "Id", encKeyId);

        Element keyInfoElement = createKeyInfoElement(doc, cert);
        encryptedKeyElement.appendChild(keyInfoElement);

        Element xencCipherValue = createCipherValue(doc, encryptedKeyElement);
        xencCipherValue.appendChild(doc.createTextNode(encodedKey));

        Element topKeyInfoElement =
            doc.createElementNS(SIG_NS, SIG_PREFIX + ":KeyInfo");
        Element retrievalMethodElement =
            doc.createElementNS(SIG_NS, SIG_PREFIX + ":RetrievalMethod");

        retrievalMethodElement.setAttribute("Type", DEFAULT_RETRIEVAL_METHOD_TYPE);
        topKeyInfoElement.appendChild(retrievalMethodElement);

        topKeyInfoElement.appendChild(encryptedKeyElement);

        encryptedDataElement.appendChild(topKeyInfoElement);
    }

    protected Element createCipherValue(Document doc, Element encryptedKey) {
        Element cipherData =
            doc.createElementNS(ENC_NS, ENC_PREFIX + ":CipherData");
        Element cipherValue =
            doc.createElementNS(ENC_NS, ENC_PREFIX + ":CipherValue");
        cipherData.appendChild(cipherValue);
        encryptedKey.appendChild(cipherData);
        return cipherValue;
    }

    private Element createKeyInfoElement(Document encryptedDataDoc,
                                         X509Certificate remoteCert) throws Exception {
        Element keyInfoElement =
            encryptedDataDoc.createElementNS(SIG_NS, SIG_PREFIX + ":KeyInfo");

        String keyIdType = encProps.getEncryptionKeyIdType() == null
            ? RSSecurityUtils.X509_CERT : encProps.getEncryptionKeyIdType();

        final Node keyIdentifierNode;
        if (keyIdType.equals(RSSecurityUtils.X509_CERT)) {
            final byte[] data;
            try {
                data = remoteCert.getEncoded();
            } catch (CertificateEncodingException e) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "encodeError"
                );
            }
            Text text = encryptedDataDoc.createTextNode(org.apache.xml.security.utils.XMLUtils.encodeToString(data));
            Element cert = encryptedDataDoc.createElementNS(SIG_NS, SIG_PREFIX + ":X509Certificate");
            cert.appendChild(text);
            Element x509Data = encryptedDataDoc.createElementNS(SIG_NS, SIG_PREFIX + ":X509Data");

            x509Data.appendChild(cert);
            keyIdentifierNode = x509Data;
        } else if (keyIdType.equals(RSSecurityUtils.X509_ISSUER_SERIAL)) {
            String issuer = remoteCert.getIssuerDN().getName();
            java.math.BigInteger serialNumber = remoteCert.getSerialNumber();
            DOMX509IssuerSerial domIssuerSerial =
                new DOMX509IssuerSerial(
                    encryptedDataDoc, issuer, serialNumber
                );
            DOMX509Data domX509Data = new DOMX509Data(encryptedDataDoc, domIssuerSerial);
            keyIdentifierNode = domX509Data.getElement();
        } else {
            throw new Exception("Unsupported key identifier:" + keyIdType);
        }

        keyInfoElement.appendChild(keyIdentifierNode);

        return keyInfoElement;
    }

    protected Element createEncryptedKeyElement(Document encryptedDataDoc,
                                                String keyEncAlgo,
                                                String digestAlgo) {
        Element encryptedKey =
            encryptedDataDoc.createElementNS(ENC_NS, ENC_PREFIX + ":EncryptedKey");

        Element encryptionMethod =
            encryptedDataDoc.createElementNS(ENC_NS, ENC_PREFIX
                                             + ":EncryptionMethod");
        encryptionMethod.setAttributeNS(null, "Algorithm", keyEncAlgo);
        if (digestAlgo != null) {
            Element digestMethod =
                encryptedDataDoc.createElementNS(SIG_NS, SIG_PREFIX + ":DigestMethod");
            digestMethod.setAttributeNS(null, "Algorithm", digestAlgo);
            encryptionMethod.appendChild(digestMethod);
        }
        encryptedKey.appendChild(encryptionMethod);
        return encryptedKey;
    }

    protected Element createEncryptedDataElement(Document encryptedDataDoc, String symEncAlgo) {
        Element encryptedData =
            encryptedDataDoc.createElementNS(ENC_NS, ENC_PREFIX + ":EncryptedData");

        XMLUtils.setNamespace(encryptedData, ENC_NS, ENC_PREFIX);

        Element encryptionMethod =
            encryptedDataDoc.createElementNS(ENC_NS, ENC_PREFIX + ":EncryptionMethod");
        encryptionMethod.setAttributeNS(null, "Algorithm", symEncAlgo);
        encryptedData.appendChild(encryptionMethod);
        encryptedDataDoc.appendChild(encryptedData);

        return encryptedData;
    }



}
