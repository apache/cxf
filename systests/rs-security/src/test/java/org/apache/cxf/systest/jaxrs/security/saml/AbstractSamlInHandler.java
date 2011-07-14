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

package org.apache.cxf.systest.jaxrs.security.saml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;
import org.apache.ws.security.validate.Validator;

public abstract class AbstractSamlInHandler implements RequestHandler {

    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractSamlInHandler.class);
    
    private Validator samlValidator = new SamlAssertionValidator();
    
    public void setValidator(Validator validator) {
        samlValidator = validator;
    }
    
    public void validateToken(Message message, InputStream tokenStream) {
        
        Document doc = null;
        try {
            doc = DOMUtils.readXml(new InputStreamReader(tokenStream, "UTF-8"));
        } catch (Exception ex) {
            throwFault("Assertion can not be read as XML document", ex);
        }
        
        try {
            AssertionWrapper assertion = new AssertionWrapper(doc.getDocumentElement());
            if (assertion.isSigned()) {
                RequestData data = new RequestData();
                WSSConfig cfg = new WSSConfig(); 
                data.setWssConfig(cfg);
                data.setCallbackHandler(getCallbackHandler(message));
                try {
                    data.setSigCrypto(getCrypto(message, 
                                                SecurityConstants.SIGNATURE_PROPERTIES));
                } catch (IOException ex) {
                    throwFault("Crypto can not be loaded", ex);
                }
                data.setEnableRevocation(MessageUtils.isTrue(
                    message.getContextualProperty(WSHandlerConstants.ENABLE_REVOCATION)));
                assertion.verifySignature(data, null);
                assertion.parseHOKSubject(data, null);
                Credential credential = new Credential();
                credential.setAssertion(assertion);
                if (samlValidator != null) {
                    samlValidator.validate(credential, data);
                }
                
                Certificate[] tlsCerts = getTLSCertificates(message);
                if (!checkHolderOfKey(assertion, null, tlsCerts)) {
                    throwFault("Holder Of Key claim fails", null);
                }
                if (!checkSenderVouches(assertion, null, tlsCerts)) {
                    throwFault("Sender vouchers claim fails", null);
                }
                if (!checkBearer(assertion, tlsCerts)) {
                    throwFault("Bearer claim fails", null);
                }
            } else if (getTLSCertificates(message) == null) {
                // alternatively ensure that the unsigned assertion inherits the signature
                // from the xml-sig envelope which this assertion must be contained in 
                throwFault("Unsigned Assertion can only be validated with two-way TLS", null);
            }
        } catch (Exception ex) {
            throwFault("Assertion can not be validated", ex);
        }
    }

    private Certificate[] getTLSCertificates(Message message) {
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        return tlsInfo != null ? tlsInfo.getPeerCertificates() : null;
    }
    
    protected void throwFault(String error, Exception ex) {
        // TODO: get bundle resource message once this filter is moved 
        // to rt/rs/security
        LOG.warning(error);
        Response response = Response.status(401).entity(error).build();
        throw ex != null ? new WebApplicationException(ex, response) : new WebApplicationException(response);
    }
    
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
    
    // TODO: Most of this code can make it into rt/security to minimize the duplication
    //       between ws/security and rs/security
    
    // WSSecurityEngineResult is HashMap extension and can be used as such
    /**
     * Check the sender-vouches requirements against the received assertion. The SAML
     * Assertion and the request body must be signed by the same signature.
     */
    private boolean checkSenderVouches(
        AssertionWrapper assertionWrapper,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        //
        // If we have a 2-way TLS connection, then we don't have to check that the
        // assertion + SOAP body are signed
        //
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        }
        return false;
//        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
//        for (String confirmationMethod : confirmationMethods) {
//            if (OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
//                if (signedResults == null || signedResults.isEmpty()) {
//                    return false;
//                }
//                if (!checkAssertionAndBodyAreSigned(assertionWrapper)) {
//                    return false;
//                }
//            }
//        }
//        return true;
    }
    
    private boolean checkBearer(AssertionWrapper assertionWrapper, Certificate[] tlsCerts) {
        
        // Check Recipient attribute. Perhaps, if STS validator is injected, then it can forward 
        // this assertion to IDP which will confirm being Recipient 
        
        // It seems if we have a signed assertion and a payload then bearer may get validated same
        // way as sender-vouches.
        
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        } 
        
        
        return false;
        
        
        //List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        //      for (String confirmationMethod : confirmationMethods) {
        //          if (isMethodBearer(confirmationMethod)) {
        //
        //          }
        //      }
        
         
    }
    
    //private boolean isMethodBearer(String confirmMethod) {
    //    return confirmMethod != null && confirmMethod.startsWith("urn:oasis:names:tc:SAML:") 
    //            && confirmMethod.endsWith(":cm:bearer");
    //}
    
    public boolean checkHolderOfKey(AssertionWrapper assertionWrapper,
                                    List<WSSecurityEngineResult> signedResults,
                                    Certificate[] tlsCerts) {
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
                if (tlsCerts == null && (signedResults == null || signedResults.isEmpty())) {
                    return false;
                }
                SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
                if (!compareCredentials(subjectKeyInfo, signedResults, tlsCerts)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compare the credentials of the assertion to the credentials used in 2-way TLS or those
     * used to verify signatures.
     * Return true on a match
     * @param subjectKeyInfo the SAMLKeyInfo object
     * @param signedResults a list of all of the signed results
     * @return true if the credentials of the assertion were used to verify a signature
     */
    private boolean compareCredentials(
        SAMLKeyInfo subjectKeyInfo,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        X509Certificate[] subjectCerts = subjectKeyInfo.getCerts();
        PublicKey subjectPublicKey = subjectKeyInfo.getPublicKey();
        byte[] subjectSecretKey = subjectKeyInfo.getSecret();
        
        //
        // Try to match the TLS certs first
        //
        if (tlsCerts != null && tlsCerts.length > 0 && subjectCerts != null 
            && subjectCerts.length > 0 && tlsCerts[0].equals(subjectCerts[0])) {
            return true;
        } else if (tlsCerts != null && tlsCerts.length > 0 && subjectPublicKey != null
            && tlsCerts[0].getPublicKey().equals(subjectPublicKey)) {
            return true;
        }
        
        //
        // Now try the message-level signatures
        //
        for (WSSecurityEngineResult signedResult : signedResults) {
            X509Certificate[] certs =
                (X509Certificate[])signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
            PublicKey publicKey =
                (PublicKey)signedResult.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
            byte[] secretKey =
                (byte[])signedResult.get(WSSecurityEngineResult.TAG_SECRET);
            if (certs != null && certs.length > 0 && subjectCerts != null
                && subjectCerts.length > 0 && certs[0].equals(subjectCerts[0])) {
                return true;
            }
            if (publicKey != null && publicKey.equals(subjectPublicKey)) {
                return true;
            }
            if (checkSecretKey(secretKey, subjectSecretKey, signedResult)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkSecretKey(
        byte[] secretKey,
        byte[] subjectSecretKey,
        WSSecurityEngineResult signedResult
    ) {
        if (secretKey != null && subjectSecretKey != null 
            && Arrays.equals(secretKey, subjectSecretKey)) {
            return true;
//            else {
//                Principal principal =
//                    (Principal)signedResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
//                if (principal instanceof WSDerivedKeyTokenPrincipal) {
//                    secretKey = ((WSDerivedKeyTokenPrincipal)principal).getSecret();
//                    if (Arrays.equals(secretKey, subjectSecretKey)) {
//                        return true;
//                    }
//                }
//            }
        }
        return false;
    }
}
