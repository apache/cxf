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

package org.apache.cxf.systest.jaxrs.security.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SignatureTrustValidator;
import org.apache.xml.security.utils.Constants;

public class XmlEncInHandler implements RequestHandler {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(XmlEncInHandler.class);
    
    static {
        WSSConfig.init();
    }
    
    public Response handleRequest(Message message, ClassResourceInfo resourceClass) {
        
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        if ("GET".equals(method)) {
            return null;
        }
        
        InputStream is = message.getContent(InputStream.class);
        Document doc = null;
        try {
            doc = DOMUtils.readXml(is);
        } catch (Exception ex) {
            throwFault("Invalid XML payload", ex);
        }
        

        Element root = doc.getDocumentElement();
        Element encKeyElement = getNode(root, WSConstants.ENC_NS, "EncryptedKey", 0);
        if (encKeyElement == null) {
            throwFault("EncryptedKey element is not available", null);
        }
        byte[] symmetricKeyBytes = getSymmetricKey(message, encKeyElement);
                
        String algorithm = getEncodingMethodAlgorithm(root);
        Element cipherValue = getNode(root, WSConstants.ENC_NS, "CipherValue", 1);
        if (cipherValue == null) {
            throwFault("CipherValue element is not available", null);
        }
        
        byte[] decryptedPayload = null;
        try {
            decryptedPayload = decryptPayload(symmetricKeyBytes, cipherValue.getTextContent().trim(),
                                                algorithm);
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
        return null;
    }
    
    private byte[] getSymmetricKey(Message message, Element encKeyElement) {
        Element certNode = getNode(encKeyElement, 
                                      Constants.SignatureSpecNS, "X509Certificate", 0);
        if (certNode == null) {
            throwFault("Certificate is missing", null);
        }
        byte[] certBytes = null;
        try {
            certBytes = Base64Utility.decode(certNode.getTextContent().trim());
        } catch (Base64Exception ex) {
            throwFault("Base64 decoding has failed", ex);
        }
        
        X509Certificate cert = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (Exception ex) {
            throwFault("X509Certificate can not be created", ex);
        }
        
        Crypto crypto = null;
        try {
            crypto = getCrypto(message, SecurityConstants.ENCRYPT_PROPERTIES);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
        }
        
            
        Credential trustCredential = new Credential();
        trustCredential.setPublicKey(null);
        trustCredential.setCertificates(new X509Certificate[]{cert});
        try {
            validateTrust(trustCredential, crypto);
        } catch (Exception ex) {
            throwFault(ex.getMessage(), ex);
        }
        
        // now start decrypting
        String algorithm = getEncodingMethodAlgorithm(encKeyElement);
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
                                       message);
        } catch (Exception ex) {
            throwFault(ex.getMessage(), ex);
        }
        return null;
    }
    
    private String getEncodingMethodAlgorithm(Element parent) {
        Element encMethod = getNode(parent, WSConstants.ENC_NS, "EncryptionMethod", 0);
        if (encMethod == null) {
            throwFault("EncryptionMethod element is not available", null);
        }
        return encMethod.getAttribute("Algorithm");
    }
    
    protected byte[] decryptSymmetricKey(String base64EncodedKey, 
                                         X509Certificate cert,
                                         Crypto crypto,
                                         String keyEncAlgo,
                                         Message message) throws WSSecurityException {
        CallbackHandler callback = getCallbackHandler(message);
        PrivateKey key = null;
        try {
            key = crypto.getPrivateKey(cert, callback);
        } catch (Exception ex) {
            throwFault("Encrypted key can not be decrypted", ex);
        }
        Cipher cipher = WSSecurityUtil.getCipherInstance(keyEncAlgo);
        try {
            // see more: WSS4J EncryptedDataProcessor
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, e
            );
        }
        try {
            byte[] encryptedBytes = Base64Utility.decode(base64EncodedKey);
            return doDecrypt(cipher, encryptedBytes); 
        } catch (Base64Exception ex) {
            throwFault("Base64 decoding has failed", ex);
        } catch (Exception ex) {
            throwFault("Encrypted key can not be decrypted", ex);
        }
        return null;
        
    }
    
    protected byte[] decryptPayload(byte[] secretKeyBytes,
                                    String base64EncodedPayload, 
                                    String symEncAlgo) throws WSSecurityException {
        byte[] encryptedBytes = null;
        try {
            encryptedBytes = Base64Utility.decode(base64EncodedPayload);
        } catch (Base64Exception ex) {
            throwFault("Base64 decoding has failed", ex);
        }
        
        Cipher cipher = WSSecurityUtil.getCipherInstance(symEncAlgo);
        try {
            // see more: WSS4J EncryptedDataProcessor
            SecretKey key = WSSecurityUtil.prepareSecretKey(symEncAlgo, secretKeyBytes);
            // IV spec
            int ivLen = cipher.getBlockSize();
            byte[] ivBytes = new byte[ivLen];
            System.arraycopy(encryptedBytes, 0, ivBytes, 0, ivLen);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            
            return cipher.doFinal(encryptedBytes, 
                             ivLen, 
                             encryptedBytes.length - ivLen);
            
        } catch (InvalidKeyException e) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, e
            );
        } catch (Exception e) {
            throw new WSSecurityException(
                 WSSecurityException.FAILED_ENCRYPTION, null, null, e);
        }
    }
    
    private byte[] doDecrypt(Cipher cipher, byte[] encryptedBytes) throws Exception {
        return cipher.doFinal(encryptedBytes);
    }
    
    private Element getNode(Element parent, String ns, String name, int index) {
        NodeList list = parent.getElementsByTagNameNS(ns, name);
        if (list != null && list.getLength() >= index + 1) {
            return (Element)list.item(index);
        } 
        return null;
    }
    
    private void validateTrust(Credential cred, Crypto crypto) throws Exception {
        SignatureTrustValidator validator = new SignatureTrustValidator();
        RequestData data = new RequestData();
        data.setSigCrypto(crypto);
        validator.validate(cred, data);
    }
    
    protected void throwFault(String error, Exception ex) {
        // TODO: get bundle resource message once this filter is moved 
        // to rt/rs/security
        LOG.warning(error);
        Response response = Response.status(401).entity(error).build();
        throw ex != null ? new WebApplicationException(ex, response) : new WebApplicationException(response);
    }
    
    // this code will be moved to a common utility class
    protected Crypto getCrypto(Message message, String propKey) 
        throws IOException, WSSecurityException {
        
        Object o = message.getContextualProperty(propKey);
        if (o == null) {
            return null;
        }
        
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            URL url = ClassLoaderUtils.getResource((String)o, this.getClass());
            if (url == null) {
                ResourceManager manager = message.getExchange()
                        .getBus().getExtension(ResourceManager.class);
                ClassLoader loader = manager.resolveResource("", ClassLoader.class);
                if (loader != null) {
                    Thread.currentThread().setContextClassLoader(loader);
                }
                url = manager.resolveResource((String)o, URL.class);
            }
            if (url != null) {
                Properties props = new Properties();
                InputStream in = url.openStream(); 
                props.load(in);
                in.close();
                return CryptoFactory.getInstance(props);
            } else {
                return CryptoFactory.getInstance((String)o);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }
    
    private CallbackHandler getCallbackHandler(Message message) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
    
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        return handler;
    }
}
