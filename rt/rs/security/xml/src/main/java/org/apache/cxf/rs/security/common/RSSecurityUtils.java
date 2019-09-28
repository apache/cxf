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

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.utils.Constants;

public final class RSSecurityUtils {

    public static final String X509_CERT = "X509Certificate";
    public static final String X509_ISSUER_SERIAL = "X509IssuerSerial";
    public static final String USE_REQUEST_SIGNATURE_CERT = "useReqSigCert";

    private RSSecurityUtils() {

    }

    public static boolean isSignedAndEncryptedTwoWay(Message m) {
        Message outMessage = m.getExchange().getOutMessage();

        Message requestMessage = outMessage != null && MessageUtils.isRequestor(outMessage)
            ? outMessage : m;

        Object encryptionProperties =
            SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, m);
        Object signatureProperties =
            SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, m);

        return "POST".equals(requestMessage.get(Message.HTTP_REQUEST_METHOD))
            && encryptionProperties != null && signatureProperties != null;
    }

    public static X509Certificate loadX509Certificate(Crypto crypto, Element certNode)
        throws Exception {
        String base64Value = certNode.getTextContent().trim();
        byte[] certBytes = Base64Utility.decode(base64Value);

        Crypto certCrypto = crypto;
        if (certCrypto == null) {
            certCrypto = new Merlin();
        }
        return certCrypto.loadCertificate(new ByteArrayInputStream(certBytes));
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
        throws Exception {
        if (crypto == null) {
            throw new Exception("Crypto instance is null");
        }
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(user);
        X509Certificate[] issuerCerts = crypto.getX509Certificates(cryptoType);
        if (issuerCerts == null || issuerCerts.length == 0) {
            throw new Exception(
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
        String user = (String)SecurityUtils.getSecurityPropertyValue(userNameKey, message);
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

    public static String getSignaturePassword(Message message, String userName,
                                              Class<?> callingClass) throws WSSecurityException {
        CallbackHandler handler = getCallbackHandler(message, callingClass);
        if (handler == null) {
            // See if we have a signature password we can use here instead
            return (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD, message);
        }

        WSPasswordCallback[] cb = {new WSPasswordCallback(userName, WSPasswordCallback.SIGNATURE)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            return null;
        }

        //get the password
        String password = cb[0].getPassword();
        return password == null ? "" : password;
    }

    public static CallbackHandler getCallbackHandler(Message message, Class<?> callingClass)
        throws WSSecurityException {
        return getCallbackHandler(message, callingClass, SecurityConstants.CALLBACK_HANDLER);
    }

    public static CallbackHandler getCallbackHandler(Message message,
                                                     Class<?> callingClass,
                                                     String callbackProperty) throws WSSecurityException {
        //Then try to get the password from the given callback handler
        Object o = SecurityUtils.getSecurityPropertyValue(callbackProperty, message);

        try {
            return SecurityUtils.getCallbackHandler(o);
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
    }

}
