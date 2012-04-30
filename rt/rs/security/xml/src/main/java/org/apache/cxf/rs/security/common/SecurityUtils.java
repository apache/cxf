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

package org.apache.cxf.rs.security.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoType;
import org.apache.xml.security.utils.Constants;

public final class SecurityUtils {
    
    public static final String X509_CERT = "X509Certificate";
    public static final String X509_ISSUER_SERIAL = "X509IssuerSerial";
    public static final String USE_REQUEST_SIGNATURE_CERT = "useReqSigCert";
    
    private SecurityUtils() {
        
    }
    
    public static boolean isSignedAndEncryptedTwoWay(Message m) {
        Message outMessage = m.getExchange().getOutMessage();
        
        Message requestMessage = outMessage != null && MessageUtils.isRequestor(outMessage) 
            ? outMessage : m;
        return "POST".equals((String)requestMessage.get(Message.HTTP_REQUEST_METHOD))
            && m.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES) != null 
            && m.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES) != null;
    }
    
    public static X509Certificate loadX509Certificate(Crypto crypto, Element certNode) 
        throws Exception {
        String base64Value = certNode.getTextContent().trim();
        byte[] certBytes = Base64Utility.decode(base64Value);
        return crypto.loadCertificate(new ByteArrayInputStream(certBytes));
    }
    
    public static X509Certificate loadX509IssuerSerial(Crypto crypto, Element certNode) 
        throws Exception {
        Node issuerNameNode = 
            certNode.getElementsByTagNameNS(Constants.SignatureSpecNS, "X509IssuerName").item(0);
        Node serialNumberNode = 
            certNode.getElementsByTagNameNS(Constants.SignatureSpecNS, "X509SerialNumber").item(0);
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
        cryptoType.setIssuerSerial(issuerNameNode.getTextContent(), 
                                   new BigInteger(serialNumberNode.getTextContent()));
        return crypto.getX509Certificates(cryptoType)[0];
    }
    
    public static X509Certificate[] getCertificates(Crypto crypto, String user)
        throws WSSecurityException {
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(user);
        X509Certificate[] issuerCerts = crypto.getX509Certificates(cryptoType);
        if (issuerCerts == null || issuerCerts.length == 0) {
            throw new WSSecurityException(
                "No issuer certs were found using issuer name: " + user);
        }
        return issuerCerts;
    }
    
    public static Crypto getCrypto(Message message,
                            String cryptoKey, 
                            String propKey) 
        throws IOException, WSSecurityException {
        return new CryptoLoader().getCrypto(message, cryptoKey, propKey); 
    }
    
    public static String getUserName(Message message, Crypto crypto, String userNameKey) {
        String user = (String)message.getContextualProperty(userNameKey);
        return getUserName(crypto, user);
    }
    
    public static String getUserName(Crypto crypto, String userName) {
        if (crypto != null && StringUtils.isEmpty(userName)) {
            try {
                userName = crypto.getDefaultX509Identifier();
            } catch (WSSecurityException e1) {
                throw new Fault(e1);
            }
        }
        return userName;
    }
    
    public static String getPassword(Message message, String userName, 
                                     int type, Class<?> callingClass) {
        CallbackHandler handler = getCallbackHandler(message, callingClass);
        if (handler == null) {
            return null;
        }
        
        WSPasswordCallback[] cb = {new WSPasswordCallback(userName, type)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            return null;
        }
        
        //get the password
        String password = cb[0].getPassword();
        return password == null ? "" : password;
    }
    
    public static CallbackHandler getCallbackHandler(Message message, Class<?> callingClass) {
        return getCallbackHandler(message, callingClass, SecurityConstants.CALLBACK_HANDLER);
    }
    
    public static CallbackHandler getCallbackHandler(Message message, 
                                                     Class<?> callingClass,
                                                     String callbackProperty) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(callbackProperty);
    
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, callingClass).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        return handler;
    }
 
}
