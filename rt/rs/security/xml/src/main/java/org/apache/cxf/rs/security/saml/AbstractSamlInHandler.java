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

package org.apache.cxf.rs.security.saml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.rs.security.saml.authorization.SecurityContextProvider;
import org.apache.cxf.rs.security.saml.authorization.SecurityContextProviderImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;
import org.apache.ws.security.validate.Validator;
import org.apache.xml.security.signature.XMLSignature;


public abstract class AbstractSamlInHandler implements RequestHandler {

    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractSamlInHandler.class);
    
    static {
        WSSConfig.init();
    }
    
    private Validator samlValidator = new SamlAssertionValidator();
    private SecurityContextProvider scProvider = new SecurityContextProviderImpl(); 
    
    public void setValidator(Validator validator) {
        samlValidator = validator;
    }
    
    public void setSecurityContextProvider(SecurityContextProvider p) {
        scProvider = p;
    }
    
    
    protected void validateToken(Message message, InputStream tokenStream) {
        
        Element token = readToken(message, tokenStream);
        validateToken(message, token);
        
    }
    
    protected Element readToken(Message message, InputStream tokenStream) {
        
        try {
            Document doc = StaxUtils.read(new InputStreamReader(tokenStream, "UTF-8"));
            return doc.getDocumentElement();
        } catch (Exception ex) {
            throwFault("Assertion can not be read as XML document", ex);
        }
        return null;
        
    }

    protected void validateToken(Message message, Element tokenElement) {
        validateToken(message, toWrapper(tokenElement));
    }
    
    protected AssertionWrapper toWrapper(Element tokenElement) {
        try {
            return new AssertionWrapper(tokenElement);
        } catch (Exception ex) {
            throwFault("Assertion can not be validated", ex);
        }
        return null;
    }
    
    protected void validateToken(Message message, AssertionWrapper assertion) {
        try {
            RequestData data = new RequestData();
            
            // Add Audience Restrictions for SAML
            configureAudienceRestriction(message, data);
            
            if (assertion.isSigned()) {
                WSSConfig cfg = WSSConfig.getNewInstance(); 
                data.setWssConfig(cfg);
                data.setCallbackHandler(SecurityUtils.getCallbackHandler(message, this.getClass()));
                try {
                    data.setSigCrypto(new CryptoLoader().getCrypto(message,
                                                SecurityConstants.SIGNATURE_CRYPTO,
                                                SecurityConstants.SIGNATURE_PROPERTIES));
                } catch (IOException ex) {
                    throwFault("Crypto can not be loaded", ex);
                }
                data.setEnableRevocation(MessageUtils.isTrue(
                    message.getContextualProperty(WSHandlerConstants.ENABLE_REVOCATION)));
                assertion.verifySignature(data, null);
                assertion.parseHOKSubject(data, null);
            } else if (getTLSCertificates(message) == null) {
                throwFault("Assertion must be signed", null);
            }
            if (samlValidator != null) {
                Credential credential = new Credential();
                credential.setAssertion(assertion);
                samlValidator.validate(credential, data);
            }
                
            
            checkSubjectConfirmationData(message, assertion);
            setSecurityContext(message, assertion);
            
        } catch (Exception ex) {
            throwFault("Assertion can not be validated", ex);
        }
    }
    
    protected void configureAudienceRestriction(Message msg, RequestData reqData) {
        // Add Audience Restrictions for SAML
        boolean enableAudienceRestriction = 
            MessageUtils.getContextualBoolean(msg, 
                                              SecurityConstants.AUDIENCE_RESTRICTION_VALIDATION, 
                                              false);
        if (enableAudienceRestriction) {
            List<String> audiences = new ArrayList<String>();
            if (msg.getContextualProperty(org.apache.cxf.message.Message.REQUEST_URL) != null) {
                audiences.add((String)msg.getContextualProperty(org.apache.cxf.message.Message.REQUEST_URL));
            }
            reqData.setAudienceRestrictions(audiences);
        }
    }
    
    protected void checkSubjectConfirmationData(Message message, AssertionWrapper assertion) {
        Certificate[] tlsCerts = getTLSCertificates(message);
        if (!checkHolderOfKey(message, assertion, tlsCerts)) {
            throwFault("Holder Of Key claim fails", null);
        }
        if (!checkSenderVouches(message, assertion, tlsCerts)) {
            throwFault("Sender vouchers claim fails", null);
        }
        if (!checkBearer(assertion, tlsCerts)) {
            throwFault("Bearer claim fails", null);
        }
    }
    
    protected void setSecurityContext(Message message, AssertionWrapper wrapper) {
        if (scProvider != null) {
            SecurityContext sc = scProvider.getSecurityContext(message, wrapper);
            message.put(SecurityContext.class, sc);
        }
    }
    
    private Certificate[] getTLSCertificates(Message message) {
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        return tlsInfo != null ? tlsInfo.getPeerCertificates() : null;
    }
    
    protected void throwFault(String error, Exception ex) {
        // TODO: get bundle resource message once this filter is moved 
        // to rt/rs/security
        LOG.warning(error + ": " + ExceptionUtils.getStackTrace(ex));
        Response response = JAXRSUtils.toResponseBuilder(401).entity(error).build();
        throw ExceptionUtils.toNotAuthorizedException(null, response);
    }
    
    /**
     * Check the sender-vouches requirements against the received assertion. The SAML
     * Assertion and the request body must be signed by the same signature.
     */
    protected boolean checkSenderVouches(
        Message message,
        AssertionWrapper assertionWrapper,
        Certificate[] tlsCerts
    ) {
        //
        // If we have a 2-way TLS connection, then we don't have to check that the
        // assertion + body are signed
        
        // If no body is available (ex, with GET) then consider validating that
        // the base64-encoded token is signed by the same signature
        //
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        }
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
                
                Element signedElement = message.getContent(Element.class);
                Node assertionParent = assertionWrapper.getElement().getParentNode();
                
                // if we have a shared parent signed node then we can assume both 
                // this SAML assertion and the main payload have been signed by the same
                // signature
                if (assertionParent != signedElement) {
                    // if not then try to compare if the same cert/key was used to sign SAML token
                    // and the payload
                    XMLSignature signature = message.getContent(XMLSignature.class);
                    if (signature == null) {
                        return false;
                    }
                    SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSignatureKeyInfo();
                    if (!compareCredentials(subjectKeyInfo, signature, tlsCerts)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    
    
    protected boolean checkHolderOfKey(Message message,
                                    AssertionWrapper assertionWrapper,
                                    Certificate[] tlsCerts) {
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
                XMLSignature sig = message.getContent(XMLSignature.class);
                SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
                if (!compareCredentials(subjectKeyInfo, sig, tlsCerts)) {
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
        XMLSignature sig,
        Certificate[] tlsCerts
    ) {
        X509Certificate[] subjectCerts = subjectKeyInfo.getCerts();
        PublicKey subjectPublicKey = subjectKeyInfo.getPublicKey();
        
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
        
        if (sig == null) {
            return false;
        }
        
        //
        // Now try the message-level signatures
        //
        try {
            X509Certificate[] certs =
                new X509Certificate[] {sig.getKeyInfo().getX509Certificate()};
            PublicKey publicKey = sig.getKeyInfo().getPublicKey();
            if (certs != null && certs.length > 0 && subjectCerts != null
                && subjectCerts.length > 0 && certs[0].equals(subjectCerts[0])) {
                return true;
            }
            if (publicKey != null && publicKey.equals(subjectPublicKey)) {
                return true;
            }
        } catch (Exception ex) {
            // ignore
        }
        
        return false;
    }
    
    protected boolean checkBearer(AssertionWrapper assertionWrapper, Certificate[] tlsCerts) {
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            boolean isBearer = isMethodBearer(confirmationMethod);
            if (isBearer && !assertionWrapper.isSigned() && (tlsCerts == null || tlsCerts.length == 0)) {
                return false;
            } 
            // do some more validation - time based, etc
        }
        return true;
    }
    
    private boolean isMethodBearer(String confirmMethod) {
        return confirmMethod != null && confirmMethod.startsWith("urn:oasis:names:tc:SAML:") 
                && confirmMethod.endsWith(":cm:bearer");
    }
}
