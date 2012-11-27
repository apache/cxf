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

package org.apache.cxf.ws.security.wss4j;

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSDerivedKeyTokenPrincipal;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.util.WSSecurityUtil;
import org.opensaml.common.SAMLVersion;
import org.opensaml.xml.XMLObject;

/**
 * internal SAMLUtils to avoid direct reference to opensaml from WSS4J interceptors.
 */
public final class SAMLUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SAMLUtils.class);
    
    private SAMLUtils() {
    }
    
    public static List<String> parseRolesInAssertion(Object assertion, String roleAttributeName) {
        if (((AssertionWrapper) assertion).getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            return parseRolesInAssertion(((AssertionWrapper)assertion).getSaml2(), roleAttributeName);
        } else {
            return parseRolesInAssertion(((AssertionWrapper)assertion).getSaml1(), roleAttributeName);
        }
    }
    
    public static String getIssuer(Object assertion) {
        return ((AssertionWrapper)assertion).getIssuerString();
    }
    
    public static Element getAssertionElement(Object assertion) {
        return ((AssertionWrapper)assertion).getElement();
    }
    
    //
    // these methods are moved from previous WSS4JInInterceptor
    //
    private static List<String> parseRolesInAssertion(org.opensaml.saml1.core.Assertion assertion,
            String roleAttributeName) {
        List<org.opensaml.saml1.core.AttributeStatement> attributeStatements = 
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            return null;
        }
        List<String> roles = new ArrayList<String>();
        
        for (org.opensaml.saml1.core.AttributeStatement statement : attributeStatements) {
            
            List<org.opensaml.saml1.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml1.core.Attribute attribute : attributes) {
                
                if (attribute.getAttributeName().equals(roleAttributeName)) {
                    for (XMLObject attributeValue : attribute.getAttributeValues()) {
                        Element attributeValueElement = attributeValue.getDOM();
                        String value = attributeValueElement.getTextContent();
                        roles.add(value);                    
                    }
                    if (attribute.getAttributeValues().size() > 1) {
//                        Don't search for other attributes with the same name if                         
//                        <saml:Attribute xmlns:saml="urn:oasis:names:tc:SAML:1.0:assertion"
//                             AttributeNamespace="http://schemas.xmlsoap.org/claims" AttributeName="roles">
//                        <saml:AttributeValue>Value1</saml:AttributeValue>
//                        <saml:AttributeValue>Value2</saml:AttributeValue>
//                        </saml:Attribute>
                        break;
                    }
                }
                
            }
        }
        return Collections.unmodifiableList(roles);
    }
    

    private static List<String> parseRolesInAssertion(org.opensaml.saml2.core.Assertion assertion,
            String roleAttributeName) {
        List<org.opensaml.saml2.core.AttributeStatement> attributeStatements = 
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            return null;
        }
        List<String> roles = new ArrayList<String>();
        
        for (org.opensaml.saml2.core.AttributeStatement statement : attributeStatements) {
            
            List<org.opensaml.saml2.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml2.core.Attribute attribute : attributes) {
                
                if (attribute.getName().equals(roleAttributeName)) {
                    for (XMLObject attributeValue : attribute.getAttributeValues()) {
                        Element attributeValueElement = attributeValue.getDOM();
                        String value = attributeValueElement.getTextContent();
                        roles.add(value);                    
                    }
                    if (attribute.getAttributeValues().size() > 1) {
//                        Don't search for other attributes with the same name if                         
//                        <saml:Attribute xmlns:saml="urn:oasis:names:tc:SAML:1.0:assertion"
//                             AttributeNamespace="http://schemas.xmlsoap.org/claims" AttributeName="roles">
//                        <saml:AttributeValue>Value1</saml:AttributeValue>
//                        <saml:AttributeValue>Value2</saml:AttributeValue>
//                        </saml:Attribute>
                        break;
                    }
                }
                
            }
        }
        return Collections.unmodifiableList(roles);
    }
    
    public static void validateSAMLResults(
        List<WSSecurityEngineResult> results,
        Message message,
        Element body
    ) throws WSSecurityException {
        List<WSSecurityEngineResult> samlResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_SIGNED, samlResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_UNSIGNED, samlResults);
        
        if (samlResults.isEmpty()) {
            return;
        }
        
        List<WSSecurityEngineResult> signedResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.SIGN, signedResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT_SIGN, signedResults);
        
        for (WSSecurityEngineResult samlResult : samlResults) {
            AssertionWrapper assertionWrapper = 
                (AssertionWrapper)samlResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
            
            TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
            Certificate[] tlsCerts = null;
            if (tlsInfo != null) {
                tlsCerts = tlsInfo.getPeerCertificates();
            }
            if (!SAMLUtils.checkHolderOfKey(assertionWrapper, signedResults, tlsCerts)) {
                LOG.warning("Assertion fails holder-of-key requirements");
                throw new WSSecurityException(WSSecurityException.INVALID_SECURITY);
            }
            if (!SAMLUtils.checkSenderVouches(assertionWrapper, tlsCerts, body, signedResults)) {
                LOG.warning("Assertion fails sender-vouches requirements");
                throw new WSSecurityException(WSSecurityException.INVALID_SECURITY);
            }
        }
        
    }
    
    /**
     * Check the holder-of-key requirements against the received assertion. The subject
     * credential of the SAML Assertion must have been used to sign some portion of
     * the message, thus showing proof-of-possession of the private/secret key. Alternatively,
     * the subject credential of the SAML Assertion must match a client certificate credential
     * when 2-way TLS is used.
     * @param assertionWrapper the SAML Assertion wrapper object
     * @param signedResults a list of all of the signed results
     */
    public static boolean checkHolderOfKey(
        AssertionWrapper assertionWrapper,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
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
    public static boolean compareCredentials(
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
    
    private static boolean checkSecretKey(
        byte[] secretKey,
        byte[] subjectSecretKey,
        WSSecurityEngineResult signedResult
    ) {
        if (secretKey != null && subjectSecretKey != null) {
            if (Arrays.equals(secretKey, subjectSecretKey)) {
                return true;
            } else {
                Principal principal =
                    (Principal)signedResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                if (principal instanceof WSDerivedKeyTokenPrincipal) {
                    secretKey = ((WSDerivedKeyTokenPrincipal)principal).getSecret();
                    if (Arrays.equals(secretKey, subjectSecretKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Check the sender-vouches requirements against the received assertion. The SAML
     * Assertion and the SOAP Body must be signed by the same signature.
     */
    public static boolean checkSenderVouches(
        AssertionWrapper assertionWrapper,
        Certificate[] tlsCerts,
        Element body,
        List<WSSecurityEngineResult> signed
    ) {
        //
        // If we have a 2-way TLS connection, then we don't have to check that the
        // assertion + SOAP body are signed
        //
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        }
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
                if (signed == null || signed.isEmpty()) {
                    return false;
                }
                if (!checkAssertionAndBodyAreSigned(assertionWrapper, body, signed)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if there is a signature which references the Assertion and the SOAP Body.
     * @param assertionWrapper the AssertionWrapper object
     * @param body The SOAP body
     * @param signed The List of signed results
     * @return true if there is a signature which references the Assertion and the SOAP Body.
     */
    private static boolean checkAssertionAndBodyAreSigned(
        AssertionWrapper assertionWrapper,
        Element body,
        List<WSSecurityEngineResult> signed
    ) {
        for (WSSecurityEngineResult signedResult : signed) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            boolean assertionIsSigned = false;
            boolean bodyIsSigned = false;
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    Element se = dataRef.getProtectedElement();
                    if (se == assertionWrapper.getElement()) {
                        assertionIsSigned = true;
                    }
                    if (se == body) {
                        bodyIsSigned = true;
                    }
                    if (assertionIsSigned && bodyIsSigned) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
