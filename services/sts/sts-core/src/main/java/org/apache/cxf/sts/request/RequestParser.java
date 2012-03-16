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

package org.apache.cxf.sts.request;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.claims.ClaimsParser;
import org.apache.cxf.sts.claims.IdentityClaimsParser;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.BinarySecretType;
import org.apache.cxf.ws.security.sts.provider.model.CancelTargetType;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.EntropyType;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.OnBehalfOfType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.UseKeyType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;
import org.apache.cxf.ws.security.sts.provider.model.secext.ReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.secext.SecurityTokenReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.wstrust14.ActAsType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.KeyInfoType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.X509DataType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.SecurityContextToken;
import org.apache.xml.security.utils.Constants;

/**
 * This class parses a RequestSecurityToken object. It stores the values that it finds into a KeyRequirements
 * and TokenRequirements objects.
 */
public class RequestParser {
    
    private static final Logger LOG = LogUtils.getL7dLogger(RequestParser.class);
    
    private KeyRequirements keyRequirements = new KeyRequirements();
    private TokenRequirements tokenRequirements = new TokenRequirements();

    @Deprecated
    public void parseRequest(
        RequestSecurityTokenType request, WebServiceContext wsContext
    ) throws STSException {
        parseRequest(request, wsContext, null, null);
    }
    
    public void parseRequest(
        RequestSecurityTokenType request, WebServiceContext wsContext, STSPropertiesMBean stsProperties, 
        List<ClaimsParser> claimsParsers
    ) throws STSException {
        LOG.fine("Parsing RequestSecurityToken");
        
        keyRequirements = new KeyRequirements();
        tokenRequirements = new TokenRequirements();
        
        for (Object requestObject : request.getAny()) {
            // JAXB types
            if (requestObject instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) requestObject;
                boolean found = 
                    parseTokenRequirements(jaxbElement, tokenRequirements, wsContext, claimsParsers);
                if (!found) {
                    found = parseKeyRequirements(jaxbElement, keyRequirements);
                }
                if (!found) {
                    LOG.log(Level.WARNING, "Found a JAXB object of unknown type: " + jaxbElement.getName());
                    throw new STSException(
                        "An unknown element was received", STSException.BAD_REQUEST
                    );
                }
            // SecondaryParameters/AppliesTo
            } else if (requestObject instanceof Element) {
                Element element = (Element)requestObject;
                if (STSConstants.WST_NS_05_12.equals(element.getNamespaceURI())
                    && "SecondaryParameters".equals(element.getLocalName())) {
                    parseSecondaryParameters(element, claimsParsers);
                } else if ("AppliesTo".equals(element.getLocalName())
                    && (STSConstants.WSP_NS.equals(element.getNamespaceURI())
                        || STSConstants.WSP_NS_04.equals(element.getNamespaceURI()))) {
                    tokenRequirements.setAppliesTo(element);
                    LOG.fine("Found AppliesTo element");
                } else {
                    LOG.log(
                        Level.WARNING, 
                        "An unknown (DOM) element was received: " + element.getLocalName()
                    );
                    throw new STSException(
                        "An unknown element was received", STSException.BAD_REQUEST
                    );
                }
            } else {
                LOG.log(Level.WARNING, "An unknown element was received");
                throw new STSException(
                    "An unknown element was received", STSException.BAD_REQUEST
                );
            }
        }
        String context = request.getContext();
        tokenRequirements.setContext(context);
        LOG.fine("Received Context attribute: " + context);
    }
    
    public KeyRequirements getKeyRequirements() {
        return keyRequirements;
    }
    
    public TokenRequirements getTokenRequirements() {
        return tokenRequirements;
    }
    
    /**
     * Parse the Key and Encryption requirements into the KeyRequirements argument.
     */
    private static boolean parseKeyRequirements(
        JAXBElement<?> jaxbElement, KeyRequirements keyRequirements
    ) {
        if (QNameConstants.AUTHENTICATION_TYPE.equals(jaxbElement.getName())) {
            String authenticationType = (String)jaxbElement.getValue();
            keyRequirements.setAuthenticationType(authenticationType);
            LOG.fine("Found AuthenticationType: " + authenticationType);
        } else if (QNameConstants.KEY_TYPE.equals(jaxbElement.getName())) {
            String keyType = (String)jaxbElement.getValue();
            keyRequirements.setKeyType(keyType);
            LOG.fine("Found KeyType: " + keyType);
        } else if (QNameConstants.KEY_SIZE.equals(jaxbElement.getName())) {
            long keySize = ((Long)jaxbElement.getValue()).longValue();
            keyRequirements.setKeySize(keySize);
            LOG.fine("Found KeySize: " + keySize);
        } else if (QNameConstants.SIGNATURE_ALGORITHM.equals(jaxbElement.getName())) {
            String signatureAlgorithm = (String)jaxbElement.getValue();
            keyRequirements.setSignatureAlgorithm(signatureAlgorithm);
            LOG.fine("Found Signature Algorithm: " + signatureAlgorithm);
        } else if (QNameConstants.ENCRYPTION_ALGORITHM.equals(jaxbElement.getName())) {
            String encryptionAlgorithm = (String)jaxbElement.getValue();
            keyRequirements.setEncryptionAlgorithm(encryptionAlgorithm);
            LOG.fine("Found Encryption Algorithm: " + encryptionAlgorithm);
        } else if (QNameConstants.C14N_ALGORITHM.equals(jaxbElement.getName())) {
            String c14nAlgorithm = (String)jaxbElement.getValue();
            keyRequirements.setC14nAlgorithm(c14nAlgorithm);
            LOG.fine("Found C14n Algorithm: " + c14nAlgorithm);
        } else if (QNameConstants.COMPUTED_KEY_ALGORITHM.equals(jaxbElement.getName())) {
            String computedKeyAlgorithm = (String)jaxbElement.getValue();
            keyRequirements.setComputedKeyAlgorithm(computedKeyAlgorithm);
            LOG.fine("Found ComputedKeyAlgorithm: " + computedKeyAlgorithm);
        } else if (QNameConstants.KEYWRAP_ALGORITHM.equals(jaxbElement.getName())) {
            String keywrapAlgorithm = (String)jaxbElement.getValue();
            keyRequirements.setKeywrapAlgorithm(keywrapAlgorithm);
            LOG.fine("Found KeyWrapAlgorithm: " + keywrapAlgorithm);
        } else if (QNameConstants.USE_KEY.equals(jaxbElement.getName())) {
            UseKeyType useKey = (UseKeyType)jaxbElement.getValue();
            X509Certificate cert = parseUseKey(useKey);
            keyRequirements.setCertificate(cert);
        } else if (QNameConstants.ENTROPY.equals(jaxbElement.getName())) {
            EntropyType entropyType = (EntropyType)jaxbElement.getValue();
            Entropy entropy = parseEntropy(entropyType);
            keyRequirements.setEntropy(entropy);
        } else if (QNameConstants.REQUEST_TYPE.equals(jaxbElement.getName())) { //NOPMD
            // Skip the request type.
        } else {
            return false;
        }
        return true;
    }
    
    /**
     * Parse the Token requirements into the TokenRequirements argument.
     */
    private static boolean parseTokenRequirements(
        JAXBElement<?> jaxbElement, 
        TokenRequirements tokenRequirements,
        WebServiceContext wsContext,
        List<ClaimsParser> claimsParsers
    ) {
        if (QNameConstants.TOKEN_TYPE.equals(jaxbElement.getName())) {
            String tokenType = (String)jaxbElement.getValue();
            tokenRequirements.setTokenType(tokenType);
            LOG.fine("Found TokenType: " + tokenType);
        } else if (QNameConstants.ON_BEHALF_OF.equals(jaxbElement.getName())) {
            OnBehalfOfType onBehalfOfType = (OnBehalfOfType)jaxbElement.getValue();
            ReceivedToken onBehalfOf = new ReceivedToken(onBehalfOfType.getAny());
            tokenRequirements.setOnBehalfOf(onBehalfOf);
            LOG.fine("Found OnBehalfOf token");
        } else if (QNameConstants.ACT_AS.equals(jaxbElement.getName())) {
            ActAsType actAsType = (ActAsType)jaxbElement.getValue();
            ReceivedToken actAs = new ReceivedToken(actAsType.getAny());
            tokenRequirements.setActAs(actAs);
            LOG.fine("Found ActAs token");
        } else if (QNameConstants.LIFETIME.equals(jaxbElement.getName())) {
            LifetimeType lifetimeType = (LifetimeType)jaxbElement.getValue();
            Lifetime lifetime = new Lifetime();
            if (lifetimeType.getCreated() != null) {
                lifetime.setCreated(lifetimeType.getCreated().getValue());
            }
            if (lifetimeType.getExpires() != null) {
                lifetime.setExpires(lifetimeType.getExpires().getValue());
            }
            tokenRequirements.setLifetime(lifetime);
            LOG.fine("Found Lifetime element");
        } else if (QNameConstants.VALIDATE_TARGET.equals(jaxbElement.getName())) {
            ValidateTargetType validateTargetType = (ValidateTargetType)jaxbElement.getValue();
            ReceivedToken validateTarget = new ReceivedToken(validateTargetType.getAny());
            if (isTokenReferenced(validateTarget)) {
                validateTarget = fetchTokenFromReference(validateTarget, wsContext);
            }  
            tokenRequirements.setValidateTarget(validateTarget);
            LOG.fine("Found ValidateTarget token");
        } else if (QNameConstants.CANCEL_TARGET.equals(jaxbElement.getName())) {
            CancelTargetType cancelTargetType = (CancelTargetType)jaxbElement.getValue();
            ReceivedToken cancelTarget = new ReceivedToken(cancelTargetType.getAny());
            if (isTokenReferenced(cancelTarget)) {
                cancelTarget = fetchTokenFromReference(cancelTarget, wsContext);
            }          
            tokenRequirements.setCancelTarget(cancelTarget);
            LOG.fine("Found CancelTarget token");
        } else if (QNameConstants.CLAIMS.equals(jaxbElement.getName())) {
            ClaimsType claimsType = (ClaimsType)jaxbElement.getValue();
            RequestClaimCollection requestedClaims = parseClaims(claimsType, claimsParsers);
            tokenRequirements.setClaims(requestedClaims);
            LOG.fine("Found Claims token");
        } else {
            return false;
        }
        return true;
    }
    
    /**
     * Parse the UseKey structure to get a certificate
     * @param useKey The UseKey object
     * @return the X509 certificate that has been parsed
     * @throws STSException
     */
    private static X509Certificate parseUseKey(UseKeyType useKey) throws STSException {
        byte[] x509 = null;
        KeyInfoType keyInfoType = extractType(useKey.getAny(), KeyInfoType.class);
        if (null != keyInfoType) {
            LOG.fine("Found KeyInfo UseKey type");
            for (Object keyInfoContent : keyInfoType.getContent()) {
                X509DataType x509DataType = extractType(keyInfoContent, X509DataType.class);
                if (null != x509DataType) {
                    LOG.fine("Found X509Data KeyInfo type");
                    for (Object x509Object 
                        : x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName()) {
                        x509 = extractType(x509Object, byte[].class);
                        if (null != x509) {
                            LOG.fine("Found X509Certificate UseKey type");
                            break;
                        }
                    }
                }
            }
        } else if (useKey.getAny() instanceof Element) {
            Element elementNSImpl = (Element) useKey.getAny();
            NodeList x509CertData = 
                elementNSImpl.getElementsByTagNameNS(
                    Constants.SignatureSpecNS, Constants._TAG_X509CERTIFICATE
                );
            if (x509CertData != null && x509CertData.getLength() > 0) {
                try {
                    x509 = Base64Utility.decode(x509CertData.item(0).getTextContent());
                    LOG.fine("Found X509Certificate UseKey type");
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "", e);
                    throw new STSException(e.getMessage(), e, STSException.INVALID_REQUEST);
                }
            }
        }
        
        if (x509 != null) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert =
                    (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(x509));
                LOG.fine("Successfully parsed X509 Certificate from UseKey");
                return cert;
            } catch (CertificateException ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException("Error in parsing certificate: ", ex, STSException.INVALID_REQUEST);
            }
        }
        return null;
    }
    
    private static <T> T extractType(Object param, Class<T> clazz) {
        if (param instanceof JAXBElement<?>) {
            JAXBElement<?> jaxbElement = (JAXBElement<?>) param;
            if (clazz == jaxbElement.getDeclaredType()) {
                return clazz.cast(jaxbElement.getValue());
            }
        }
        return null;
    }
    
    /**
     * Parse an Entropy object
     * @param entropy an Entropy object
     */
    private static Entropy parseEntropy(EntropyType entropyType) {
        for (Object entropyObject : entropyType.getAny()) {
            JAXBElement<?> entropyObjectJaxb = (JAXBElement<?>) entropyObject;
            if (QNameConstants.BINARY_SECRET.equals(entropyObjectJaxb.getName())) {
                BinarySecretType binarySecret = 
                    (BinarySecretType)entropyObjectJaxb.getValue();
                LOG.fine("Found BinarySecret Entropy type");
                Entropy entropy = new Entropy();
                entropy.setBinarySecretType(binarySecret.getType());
                entropy.setBinarySecretValue(binarySecret.getValue());
                return entropy;
            } else {
                LOG.fine("Unsupported Entropy type: " + entropyObjectJaxb.getName());
            }
            // TODO support EncryptedKey
        }
        return null;
    }
    
    /**
     * Parse the secondaryParameters element. Precedence goes to values that are specified as
     * direct children of the RequestSecurityToken element. 
     * @param secondaryParameters the secondaryParameters element to parse
     */
    private void parseSecondaryParameters(Element secondaryParameters, List<ClaimsParser> claimsParsers) {
        LOG.fine("Found SecondaryParameters element");
        Element child = DOMUtils.getFirstElement(secondaryParameters);
        while (child != null) {
            String localName = child.getLocalName();
            String namespace = child.getNamespaceURI();
            if (keyRequirements.getKeySize() == 0 && "KeySize".equals(localName) 
                && STSConstants.WST_NS_05_12.equals(namespace)) {
                long keySize = Integer.parseInt(child.getTextContent());
                keyRequirements.setKeySize(keySize);
                LOG.fine("Found KeySize: " + keySize);
            } else if (tokenRequirements.getTokenType() == null 
                && "TokenType".equals(localName) && STSConstants.WST_NS_05_12.equals(namespace)) {
                String tokenType = child.getTextContent();
                tokenRequirements.setTokenType(tokenType);
                LOG.fine("Found TokenType: " + tokenType);
            } else if (keyRequirements.getKeyType() == null 
                && "KeyType".equals(localName) && STSConstants.WST_NS_05_12.equals(namespace)) {
                String keyType = child.getTextContent();
                LOG.fine("Found KeyType: " + keyType);
                keyRequirements.setKeyType(keyType);
            } else if (tokenRequirements.getClaims() == null 
                && "Claims".equals(localName) && STSConstants.WST_NS_05_12.equals(namespace)) {
                LOG.fine("Found Claims element");
                RequestClaimCollection requestedClaims = parseClaims(child, claimsParsers);
                tokenRequirements.setClaims(requestedClaims);
            } else {
                LOG.fine("Found unknown element: " + localName + " " + namespace);
            }
            child = DOMUtils.getNextElement(child);
        }
    }
    
    /**
     * Create a RequestClaimCollection from a DOM Element
     */
    private RequestClaimCollection parseClaims(Element claimsElement, List<ClaimsParser> claimsParsers) {
        String dialectAttr = null;
        RequestClaimCollection requestedClaims = new RequestClaimCollection();
        try {
            dialectAttr = claimsElement.getAttribute("Dialect");
            if (dialectAttr != null && !"".equals(dialectAttr)) {
                requestedClaims.setDialect(new URI(dialectAttr));
            }
        } catch (URISyntaxException e1) {
            LOG.log(
                Level.WARNING, 
                "Cannot create URI from the given Dialect attribute value " + dialectAttr, 
                e1
            );
        }
        
        Element childClaimType = DOMUtils.getFirstElement(claimsElement);
        while (childClaimType != null) {
            RequestClaim requestClaim = parseChildClaimType(childClaimType, dialectAttr, claimsParsers);
            if (requestClaim != null) {
                requestedClaims.add(requestClaim);
            }
            childClaimType = DOMUtils.getNextElement(childClaimType);
        }
        
        return requestedClaims;
    }
    
    /**
     * Create a RequestClaimCollection from a JAXB ClaimsType object
     */
    private static RequestClaimCollection parseClaims(
        ClaimsType claimsType, List<ClaimsParser> claimsParsers
    ) {
        String dialectAttr = null;
        RequestClaimCollection requestedClaims = new RequestClaimCollection();
        try {
            dialectAttr = claimsType.getDialect();
            if (dialectAttr != null && !"".equals(dialectAttr)) {
                requestedClaims.setDialect(new URI(dialectAttr));
            }
        } catch (URISyntaxException e1) {
            LOG.log(
                Level.WARNING, 
                "Cannot create URI from the given Dialect attribute value " + dialectAttr, 
                e1
            );
        }
        
        for (Object claim : claimsType.getAny()) {
            if (claim instanceof Element) {
                RequestClaim requestClaim = parseChildClaimType((Element)claim, dialectAttr, claimsParsers);
                if (requestClaim != null) {
                    requestedClaims.add(requestClaim);
                }
            }
        }
        
        return requestedClaims;
    }
    
    /**
     * Parse a child ClaimType into a RequestClaim object.
     */
    private static RequestClaim parseChildClaimType(
        Element childClaimType, String dialect, List<ClaimsParser> claimsParsers
    ) {
        if (claimsParsers != null) {
            for (ClaimsParser parser : claimsParsers) {
                if (parser != null && dialect.equals(parser.getSupportedDialect())) {
                    return parser.parse(childClaimType);
                }
            }
        }
        if (IdentityClaimsParser.IDENTITY_CLAIMS_DIALECT.equals(dialect)) {
            return IdentityClaimsParser.parseClaimType(childClaimType);
        }
        
        LOG.log(Level.WARNING, "No ClaimsParser is registered for dialect " + dialect);
        throw new STSException(
            "No ClaimsParser is registered for dialect " + dialect, STSException.BAD_REQUEST
        );
    }
    
    
    /**
     * Method to check if the passed token is a SecurityTokenReference
     */
    private static boolean isTokenReferenced(ReceivedToken token) {
        Object targetToken = token.getToken();
        if (targetToken instanceof Element) {
            Element tokenElement = (Element)targetToken;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if (STSConstants.WSSE_EXT_04_01.equals(namespace)
                && "SecurityTokenReference".equals(localname)) {
                return true;
            }
        } else if (targetToken instanceof SecurityTokenReferenceType) {
            return true;
        }
        return false;
    } 
    
    /**
     * Method to fetch token from the SecurityTokenReference
     */
    private static ReceivedToken fetchTokenFromReference(
        ReceivedToken tokenReference, WebServiceContext wsContext
    ) {
        // Get the reference URI
        String referenceURI = null;
        Object targetToken = tokenReference.getToken();
        if (targetToken instanceof Element) {
            Element tokenElement = (Element) targetToken;
            NodeList refList = 
                tokenElement.getElementsByTagNameNS(STSConstants.WSSE_EXT_04_01, "Reference");
            if (refList.getLength() == 0) {
                throw new STSException(
                    "Cannot find Reference element in the SecurityTokenReference.", 
                    STSException.REQUEST_FAILED
                );
            }
            referenceURI = refList.item(0).getNodeValue();
        } else if (targetToken instanceof SecurityTokenReferenceType) {
            Iterator<?> iterator = ((SecurityTokenReferenceType) targetToken).getAny().iterator();
            while (iterator.hasNext()) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) iterator.next();
                if (jaxbElement.getValue() instanceof ReferenceType) {
                    referenceURI = ((ReferenceType) jaxbElement.getValue()).getURI();
                }
            }
        }
        LOG.fine("Reference URI found " + referenceURI);
   
        // Find processed token corresponding to the URI
        if (referenceURI.charAt(0) == '#') {
            referenceURI = referenceURI.substring(1);
        }
        MessageContext messageContext = wsContext.getMessageContext();
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>) messageContext.get(WSHandlerConstants.RECV_RESULTS));
        
        if (handlerResults != null && handlerResults.size() > 0) {
            WSHandlerResult handlerResult = handlerResults.get(0);
            List<WSSecurityEngineResult> engineResults = handlerResult.getResults();
            
            for (WSSecurityEngineResult engineResult : engineResults) {
                Integer actInt = (Integer)engineResult.get(WSSecurityEngineResult.TAG_ACTION);
                String id = (String)engineResult.get(WSSecurityEngineResult.TAG_ID);
                if (referenceURI.equals(id)) {
                    Element tokenElement = 
                        (Element)engineResult.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                    if (tokenElement == null) {
                        throw new STSException(
                            "Cannot retrieve token from reference", STSException.INVALID_REQUEST
                        );
                    }
                    return new ReceivedToken(tokenElement);
                } else if (actInt == WSConstants.SCT) {
                    // Need to check special case of SecurityContextToken Identifier separately
                    SecurityContextToken sct = 
                        (SecurityContextToken)
                            engineResult.get(WSSecurityEngineResult.TAG_SECURITY_CONTEXT_TOKEN);
                    if (referenceURI.equals(sct.getIdentifier())) {
                        return new ReceivedToken(sct.getElement());
                    }
                }
            }
        }
        throw new STSException("Cannot retreive token from reference", STSException.REQUEST_FAILED);
    }

}
