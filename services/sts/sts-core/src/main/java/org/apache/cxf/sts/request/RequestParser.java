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
import java.security.KeyException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.claims.ClaimsParser;
import org.apache.cxf.sts.claims.IdentityClaimsParser;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.BinarySecretType;
import org.apache.cxf.ws.security.sts.provider.model.CancelTargetType;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.EntropyType;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.OnBehalfOfType;
import org.apache.cxf.ws.security.sts.provider.model.ParticipantType;
import org.apache.cxf.ws.security.sts.provider.model.ParticipantsType;
import org.apache.cxf.ws.security.sts.provider.model.RenewTargetType;
import org.apache.cxf.ws.security.sts.provider.model.RenewingType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.UseKeyType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;
import org.apache.cxf.ws.security.sts.provider.model.secext.ReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.secext.SecurityTokenReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.wstrust14.ActAsType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.KeyInfoType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.X509DataType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.dom.processor.EncryptedKeyProcessor;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.xml.security.utils.Constants;

/**
 * This class parses a RequestSecurityToken object. It stores the values that it finds into a KeyRequirements
 * and TokenRequirements objects.
 */
public class RequestParser {
    
    private static final Logger LOG = LogUtils.getL7dLogger(RequestParser.class);
    
    private KeyRequirements keyRequirements = new KeyRequirements();
    private TokenRequirements tokenRequirements = new TokenRequirements();

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
                try {
                    boolean found = 
                        parseTokenRequirements(jaxbElement, tokenRequirements, wsContext, claimsParsers);
                    if (!found) {
                        found = parseKeyRequirements(jaxbElement, keyRequirements, wsContext, stsProperties);
                    }
                    if (!found) {
                        LOG.log(
                            Level.WARNING, 
                            "Found a JAXB object of unknown type: " + jaxbElement.getName()
                        );
                        throw new STSException(
                            "An unknown element was received", STSException.BAD_REQUEST
                        );
                    }
                } catch (STSException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw ex;
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw ex;
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
                        + " " + element.getNamespaceURI()
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
        JAXBElement<?> jaxbElement, KeyRequirements keyRequirements, 
        WebServiceContext wsContext, STSPropertiesMBean stsProperties
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
            ReceivedKey receivedKey = parseUseKey(useKey, wsContext);
            keyRequirements.setReceivedKey(receivedKey);
        } else if (QNameConstants.ENTROPY.equals(jaxbElement.getName())) {
            EntropyType entropyType = (EntropyType)jaxbElement.getValue();
            Entropy entropy = parseEntropy(entropyType, stsProperties);
            keyRequirements.setEntropy(entropy);
        } else if (QNameConstants.SIGN_WITH.equals(jaxbElement.getName())) {
            String signWith = (String)jaxbElement.getValue();
            keyRequirements.setSignWith(signWith);
            LOG.fine("Found SignWith: " + signWith);
        } else if (QNameConstants.ENCRYPT_WITH.equals(jaxbElement.getName())) {
            String encryptWith = (String)jaxbElement.getValue();
            keyRequirements.setEncryptWith(encryptWith);
            LOG.fine("Found EncryptWith: " + encryptWith);
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
            if (isTokenReferenced(validateTarget.getToken())) {
                Element target = fetchTokenElementFromReference(validateTarget.getToken(), wsContext);
                validateTarget = new ReceivedToken(target);
            }  
            tokenRequirements.setValidateTarget(validateTarget);
            LOG.fine("Found ValidateTarget token");
        } else if (QNameConstants.CANCEL_TARGET.equals(jaxbElement.getName())) {
            CancelTargetType cancelTargetType = (CancelTargetType)jaxbElement.getValue();
            ReceivedToken cancelTarget = new ReceivedToken(cancelTargetType.getAny());
            if (isTokenReferenced(cancelTarget.getToken())) {
                Element target = fetchTokenElementFromReference(cancelTarget.getToken(), wsContext);
                cancelTarget = new ReceivedToken(target);
            }          
            tokenRequirements.setCancelTarget(cancelTarget);
            LOG.fine("Found CancelTarget token");
        } else if (QNameConstants.RENEW_TARGET.equals(jaxbElement.getName())) {
            RenewTargetType renewTargetType = (RenewTargetType)jaxbElement.getValue();
            ReceivedToken renewTarget = new ReceivedToken(renewTargetType.getAny());
            if (isTokenReferenced(renewTarget.getToken())) {
                Element target = fetchTokenElementFromReference(renewTarget.getToken(), wsContext);
                renewTarget = new ReceivedToken(target);
            }          
            tokenRequirements.setRenewTarget(renewTarget);
            LOG.fine("Found CancelTarget token");
        } else if (QNameConstants.CLAIMS.equals(jaxbElement.getName())) {
            ClaimsType claimsType = (ClaimsType)jaxbElement.getValue();
            ClaimCollection requestedClaims = parseClaims(claimsType, claimsParsers);
            tokenRequirements.setPrimaryClaims(requestedClaims);
            LOG.fine("Found Primary Claims token");
        } else if (QNameConstants.RENEWING.equals(jaxbElement.getName())) {
            RenewingType renewingType = (RenewingType)jaxbElement.getValue();
            Renewing renewing = new Renewing();
            if (renewingType.isAllow() != null) {
                renewing.setAllowRenewing(renewingType.isAllow());
            }
            if (renewingType.isOK() != null) {
                renewing.setAllowRenewingAfterExpiry(renewingType.isOK());
            }
            tokenRequirements.setRenewing(renewing);
            LOG.fine("Found Renewing token");
        } else if (QNameConstants.PARTICIPANTS.equals(jaxbElement.getName())) {
            ParticipantsType participantsType = (ParticipantsType)jaxbElement.getValue();
            
            Participants participants = parseParticipants(participantsType);
            tokenRequirements.setParticipants(participants);
            LOG.fine("Found Participants");
        } else {
            return false;
        }
        return true;
    }
    
    /**
     * Parse the UseKey structure to get a ReceivedKey containing a cert/public-key/secret-key.
     * @param useKey The UseKey object
     * @param wsContext The WebServiceContext object
     * @return the ReceivedKey that has been parsed
     * @throws STSException
     */
    private static ReceivedKey parseUseKey(
        UseKeyType useKey, 
        WebServiceContext wsContext
    ) throws STSException {
        byte[] x509 = null;
        if (useKey.getAny() instanceof JAXBElement<?>) {
            JAXBElement<?> useKeyJaxb = (JAXBElement<?>)useKey.getAny();
            Object obj = useKeyJaxb.getValue();
            if (KeyInfoType.class == useKeyJaxb.getDeclaredType() || obj instanceof KeyInfoType) {
                KeyInfoType keyInfoType = KeyInfoType.class.cast(useKeyJaxb.getValue());
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
            } else if (SecurityTokenReferenceType.class == useKeyJaxb.getDeclaredType()
                || obj instanceof SecurityTokenReferenceType) {
                SecurityTokenReferenceType strType = 
                    SecurityTokenReferenceType.class.cast(useKeyJaxb.getValue());
                Element token = fetchTokenElementFromReference(strType, wsContext);
                try {
                    x509 = Base64Utility.decode(token.getTextContent().trim());
                    LOG.fine("Found X509Certificate UseKey type via reference");
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "", e);
                    throw new STSException(e.getMessage(), e, STSException.INVALID_REQUEST);
                }
            }
        } else if (useKey.getAny() instanceof Element) {
            if (isTokenReferenced(useKey.getAny())) {
                Element token = fetchTokenElementFromReference(useKey.getAny(), wsContext);
                try {
                    x509 = Base64Utility.decode(token.getTextContent().trim());
                    LOG.fine("Found X509Certificate UseKey type via reference");
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "", e);
                    throw new STSException(e.getMessage(), e, STSException.INVALID_REQUEST);
                }
            } else {
                Element element = (Element)useKey.getAny();
                if ("KeyInfo".equals(element.getLocalName())) {
                    return parseKeyInfoElement((Element)useKey.getAny());
                } else {
                    NodeList x509CertData = 
                        element.getElementsByTagNameNS(
                            Constants.SignatureSpecNS, Constants._TAG_X509CERTIFICATE   
                        );
                    if (x509CertData != null && x509CertData.getLength() > 0) {
                        try {
                            x509 = Base64Utility.decode(x509CertData.item(0).getTextContent().trim());
                            LOG.fine("Found X509Certificate UseKey type");
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                            throw new STSException(e.getMessage(), e, STSException.INVALID_REQUEST);
                        }
                    }
                }
            }
        } else {
            LOG.log(Level.WARNING, "An unknown element was received");
            throw new STSException(
                "An unknown element was received", STSException.BAD_REQUEST
            );
        }
        
        if (x509 != null) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert =
                    (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(x509));
                LOG.fine("Successfully parsed X509 Certificate from UseKey");
                ReceivedKey receivedKey = new ReceivedKey();
                receivedKey.setX509Cert(cert);
                return receivedKey;
            } catch (CertificateException ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException("Error in parsing certificate: ", ex, STSException.INVALID_REQUEST);
            }
        }
        return null;
    }
    
    private static Participants parseParticipants(ParticipantsType participantsType) {
        Participants participants = new Participants();
        
        if (participantsType.getPrimary() != null) {
            participants.setPrimaryParticipant(participantsType.getPrimary().getAny());
        }
        
        if (participantsType.getParticipant() != null 
            && !participantsType.getParticipant().isEmpty()) {
            List<Object> secondaryParticipants = 
                new ArrayList<Object>(participantsType.getParticipant().size());
            for (ParticipantType secondaryParticipant : participantsType.getParticipant()) {
                secondaryParticipants.add(secondaryParticipant.getAny());
            }
            participants.setParticipants(secondaryParticipants);
        }
        
        return participants;
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
     * Parse the KeyInfo Element to return a ReceivedKey object containing the found certificate or
     * public key.
     */
    private static ReceivedKey parseKeyInfoElement(Element keyInfoElement) throws STSException {
        KeyInfoFactory keyInfoFactory = null;
        try {
            keyInfoFactory = KeyInfoFactory.getInstance("DOM", "ApacheXMLDSig");
        } catch (NoSuchProviderException ex) {
            keyInfoFactory = KeyInfoFactory.getInstance("DOM");
        }

        try {
            KeyInfo keyInfo = 
                keyInfoFactory.unmarshalKeyInfo(new DOMStructure(keyInfoElement));
            List<?> list = keyInfo.getContent();

            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof KeyValue) {
                    KeyValue keyValue = (KeyValue)list.get(i);
                    ReceivedKey receivedKey = new ReceivedKey();
                    receivedKey.setPublicKey(keyValue.getPublicKey());
                    return receivedKey;
                } else if (list.get(i) instanceof X509Certificate) {
                    ReceivedKey receivedKey = new ReceivedKey();
                    receivedKey.setX509Cert((X509Certificate)list.get(i));
                    return receivedKey;
                } else if (list.get(i) instanceof X509Data) {
                    X509Data x509Data = (X509Data)list.get(i);
                    for (int j = 0; j < x509Data.getContent().size(); j++) {
                        if (x509Data.getContent().get(j) instanceof X509Certificate) {
                            ReceivedKey receivedKey = new ReceivedKey();
                            receivedKey.setX509Cert((X509Certificate)x509Data.getContent().get(j));
                            return receivedKey;
                        }
                    }
                }
            }
        } catch (MarshalException e) {
            LOG.log(Level.WARNING, "", e);
            throw new STSException(e.getMessage(), e, STSException.INVALID_REQUEST);
        } catch (KeyException e) {
            LOG.log(Level.WARNING, "", e);
            throw new STSException(e.getMessage(), e, STSException.INVALID_REQUEST);
        }
        return null;
    }
    
    /**
     * Parse an Entropy object
     * @param entropy an Entropy object
     * @param stsProperties A STSPropertiesMBean object used to decrypt an EncryptedKey
     */
    private static Entropy parseEntropy(
        EntropyType entropyType, STSPropertiesMBean stsProperties
    ) throws STSException {
        for (Object entropyObject : entropyType.getAny()) {
            if (entropyObject instanceof JAXBElement<?>) {
                JAXBElement<?> entropyObjectJaxb = (JAXBElement<?>) entropyObject;
                if (QNameConstants.BINARY_SECRET.equals(entropyObjectJaxb.getName())) {
                    BinarySecretType binarySecretType = 
                        (BinarySecretType)entropyObjectJaxb.getValue();
                    LOG.fine("Found BinarySecret Entropy type");
                    Entropy entropy = new Entropy();
                    BinarySecret binarySecret = new BinarySecret();
                    binarySecret.setBinarySecretType(binarySecretType.getType());
                    binarySecret.setBinarySecretValue(binarySecretType.getValue());
                    entropy.setBinarySecret(binarySecret);
                    return entropy;
                } else {
                    LOG.fine("Unsupported Entropy type: " + entropyObjectJaxb.getName());
                }
            } else if (entropyObject instanceof Element
                && "EncryptedKey".equals(((Element)entropyObject).getLocalName())) {
                EncryptedKeyProcessor processor = new EncryptedKeyProcessor();
                Element entropyElement = (Element)entropyObject;
                RequestData requestData = new RequestData();
                requestData.setDecCrypto(stsProperties.getSignatureCrypto());
                requestData.setCallbackHandler(stsProperties.getCallbackHandler());
                requestData.setWssConfig(WSSConfig.getNewInstance());
                try {
                    List<WSSecurityEngineResult> results = 
                        processor.handleToken(
                            entropyElement, requestData, new WSDocInfo(entropyElement.getOwnerDocument())
                        );
                    Entropy entropy = new Entropy();
                    entropy.setDecryptedKey((byte[])results.get(0).get(WSSecurityEngineResult.TAG_SECRET));
                    return entropy;
                } catch (WSSecurityException e) {
                    LOG.log(Level.WARNING, "", e);
                    throw new STSException(e.getMessage(), e, STSException.INVALID_REQUEST);
                }
            } else {
                LOG.log(Level.WARNING, "An unknown element was received");
                throw new STSException(
                    "An unknown element was received", STSException.BAD_REQUEST
                );
            }
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
                long keySize = Integer.parseInt(child.getTextContent().trim());
                keyRequirements.setKeySize(keySize);
                LOG.fine("Found KeySize: " + keySize);
            } else if (tokenRequirements.getTokenType() == null 
                && "TokenType".equals(localName) && STSConstants.WST_NS_05_12.equals(namespace)) {
                String tokenType = child.getTextContent().trim();
                tokenRequirements.setTokenType(tokenType);
                LOG.fine("Found TokenType: " + tokenType);
            } else if (keyRequirements.getKeyType() == null 
                && "KeyType".equals(localName) && STSConstants.WST_NS_05_12.equals(namespace)) {
                String keyType = child.getTextContent().trim();
                LOG.fine("Found KeyType: " + keyType);
                keyRequirements.setKeyType(keyType);
            } else if ("Claims".equals(localName) && STSConstants.WST_NS_05_12.equals(namespace)) {
                LOG.fine("Found Secondary Claims element");
                ClaimCollection requestedClaims = parseClaims(child, claimsParsers);
                tokenRequirements.setSecondaryClaims(requestedClaims);
            } else {
                LOG.fine("Found unknown element: " + localName + " " + namespace);
            }
            child = DOMUtils.getNextElement(child);
        }
    }
    
    /**
     * Create a ClaimCollection from a DOM Element
     */
    private ClaimCollection parseClaims(Element claimsElement, List<ClaimsParser> claimsParsers) {
        String dialectAttr = null;
        ClaimCollection requestedClaims = new ClaimCollection();
        try {
            dialectAttr = claimsElement.getAttributeNS(null, "Dialect");
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
            Claim requestClaim = parseChildClaimType(childClaimType, dialectAttr, claimsParsers);
            if (requestClaim != null) {
                requestedClaims.add(requestClaim);
            }
            childClaimType = DOMUtils.getNextElement(childClaimType);
        }
        
        return requestedClaims;
    }
    
    /**
     * Create a ClaimCollection from a JAXB ClaimsType object
     */
    private static ClaimCollection parseClaims(
        ClaimsType claimsType, List<ClaimsParser> claimsParsers
    ) {
        String dialectAttr = null;
        ClaimCollection requestedClaims = new ClaimCollection();
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
                Claim requestClaim = parseChildClaimType((Element)claim, dialectAttr, claimsParsers);
                if (requestClaim != null) {
                    requestedClaims.add(requestClaim);
                }
            }
        }
        
        return requestedClaims;
    }
    
    /**
     * Parse a child ClaimType into a Claim object.
     */
    private static Claim parseChildClaimType(
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
    private static boolean isTokenReferenced(Object targetToken) {
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
    private static Element fetchTokenElementFromReference(
        Object targetToken, WebServiceContext wsContext
    ) {
        // Get the reference URI
        String referenceURI = null;
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
        if (referenceURI == null) {
            LOG.log(Level.WARNING, "No Reference URI was received");
            throw new STSException(
                "An unknown element was received", STSException.BAD_REQUEST
            );
        }
   
        // Find processed token corresponding to the URI
        referenceURI = WSSecurityUtil.getIDFromReference(referenceURI);

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
                    return tokenElement;
                } else if (actInt == WSConstants.SCT) {
                    // Need to check special case of SecurityContextToken Identifier separately
                    SecurityContextToken sct = 
                        (SecurityContextToken)
                            engineResult.get(WSSecurityEngineResult.TAG_SECURITY_CONTEXT_TOKEN);
                    if (referenceURI.equals(sct.getIdentifier())) {
                        return sct.getElement();
                    }
                }
            }
        }
        throw new STSException("Cannot retreive token from reference", STSException.REQUEST_FAILED);
    }

}
