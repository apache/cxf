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

package org.apache.cxf.ws.security.wss4j.policyhandlers;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.crypto.dsig.Reference;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.AttachmentCallbackHandler;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecBase;
import org.apache.wss4j.dom.message.WSSecDKEncrypt;
import org.apache.wss4j.dom.message.WSSecDKSign;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.opensaml.common.SAMLVersion;

/**
 * 
 */
public class AsymmetricBindingHandler extends AbstractBindingBuilder {

    private static final Logger LOG = LogUtils.getL7dLogger(AsymmetricBindingHandler.class);

    AsymmetricBinding abinding;
    
    private WSSecEncryptedKey encrKey;
    private String encryptedKeyId;
    private byte[] encryptedKeyValue;
    
    public AsymmetricBindingHandler(WSSConfig config,
                                    AsymmetricBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) {
        super(config, binding, saaj, secHeader, aim, message);
        this.abinding = binding;
        protectionOrder = binding.getProtectionOrder();
    }
    
    public void handleBinding() {
        WSSecTimestamp timestamp = createTimestamp();
        handleLayout(timestamp);
        assertPolicy(abinding.getName());
        
        if (abinding.getProtectionOrder() 
            == AbstractSymmetricAsymmetricBinding.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_BEFORE_SIGNING));
        } else {
            doSignBeforeEncrypt();
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.SIGN_BEFORE_ENCRYPTING));
        }
        reshuffleTimestamp();
        
        assertAlgorithmSuite(abinding.getAlgorithmSuite());
        assertWSSProperties(abinding.getName().getNamespaceURI());
        assertTrustProperties(abinding.getName().getNamespaceURI());
        assertPolicy(
            new QName(abinding.getName().getNamespaceURI(), SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));
    }

    private void doSignBeforeEncrypt() {
        try {
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            assertTokenWrapper(initiatorWrapper);
            boolean attached = false;
            if (initiatorWrapper != null) {
                AbstractToken initiatorToken = initiatorWrapper.getToken();
                if (initiatorToken instanceof IssuedToken) {
                    SecurityToken secToken = getSecurityToken();
                    if (secToken == null) {
                        policyNotAsserted(initiatorToken, "Security token is not found or expired");
                        return;
                    } else {
                        assertPolicy(initiatorToken);
                        
                        if (isTokenRequired(initiatorToken.getIncludeTokenType())) {
                            Element el = secToken.getToken();
                            this.addEncryptedKeyElement(cloneElement(el));
                            attached = true;
                        } 
                    }
                } else if (initiatorToken instanceof SamlToken && isRequestor()) {
                    SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)initiatorToken);
                    if (assertionWrapper != null) {
                        if (isTokenRequired(initiatorToken.getIncludeTokenType())) {
                            addSupportingElement(assertionWrapper.toDOM(saaj.getSOAPPart()));
                            storeAssertionAsSecurityToken(assertionWrapper);
                        }
                        assertPolicy(initiatorToken);
                    }
                } else if (initiatorToken instanceof SamlToken) {
                    String tokenId = getSAMLToken();
                    if (tokenId == null) {
                        policyNotAsserted(initiatorToken, "Security token is not found or expired");
                        return;
                    }
                }
                assertToken(initiatorToken);
            }
            
            // Add timestamp
            List<WSEncryptionPart> sigs = new ArrayList<WSEncryptionPart>();
            if (timestampEl != null) {
                WSEncryptionPart timestampPart = 
                    convertToEncryptionPart(timestampEl.getElement());
                sigs.add(timestampPart);
            }
            addSupportingTokens(sigs);
            
            sigs.addAll(this.getSignedParts(null));
            
            if (isRequestor() && initiatorWrapper != null) {
                doSignature(initiatorWrapper, sigs, attached);
                doEndorse();
            } else if (!isRequestor()) {
                //confirm sig
                addSignatureConfirmation(sigs);
                
                AbstractTokenWrapper recipientSignatureToken = abinding.getRecipientSignatureToken();
                if (recipientSignatureToken == null) {
                    recipientSignatureToken = abinding.getRecipientToken();
                }
                if (recipientSignatureToken != null) {
                    assertTokenWrapper(recipientSignatureToken);
                    assertToken(recipientSignatureToken.getToken());
                    doSignature(recipientSignatureToken, sigs, attached);
                }
            }

            List<WSEncryptionPart> enc = getEncryptedParts();
            
            //Check for signature protection
            if (abinding.isEncryptSignature()) {
                if (mainSigId != null) {
                    WSEncryptionPart sigPart = new WSEncryptionPart(mainSigId, "Element");
                    sigPart.setElement(bottomUpElement);
                    enc.add(sigPart);
                }
                if (sigConfList != null && !sigConfList.isEmpty()) {
                    enc.addAll(sigConfList);
                }
                assertPolicy(
                    new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));
            }
            
            //Do encryption
            AbstractTokenWrapper encToken;
            if (isRequestor()) {
                enc.addAll(encryptedTokensList);
                encToken = abinding.getRecipientEncryptionToken();
                if (encToken == null) {
                    encToken = abinding.getRecipientToken();
                }
            } else {
                encToken = abinding.getInitiatorEncryptionToken();
                if (encToken == null) {
                    encToken = abinding.getInitiatorToken();
                }
            }            
            doEncryption(encToken, enc, false);
            if (encToken != null) {
                assertTokenWrapper(encToken);
                assertToken(encToken.getToken());
            }
            
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Sign before encryption failed due to : " + reason);
            LOG.log(Level.FINE, e.getMessage(), e);
            throw new Fault(e);
        }
    }

    private AbstractTokenWrapper getEncryptBeforeSignWrapper() {
        AbstractTokenWrapper wrapper;
        if (isRequestor()) {
            wrapper = abinding.getRecipientEncryptionToken();
            if (wrapper == null) {
                wrapper = abinding.getRecipientToken();
            }            
        } else {
            wrapper = abinding.getInitiatorEncryptionToken();
            if (wrapper == null) {
                wrapper = abinding.getInitiatorToken();
            }
        }
        assertTokenWrapper(wrapper);
        
        return wrapper;
    }
    
    private void doEncryptBeforeSign() {
        AbstractTokenWrapper wrapper = getEncryptBeforeSignWrapper();
        AbstractToken encryptionToken = null;
        if (wrapper != null) {
            encryptionToken = wrapper.getToken();
            assertToken(encryptionToken);
        }
        
        AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
        if (initiatorWrapper == null) {
            initiatorWrapper = abinding.getInitiatorToken();
        }
        assertTokenWrapper(initiatorWrapper);
        boolean attached = false;
        
        if (initiatorWrapper != null) {
            AbstractToken initiatorToken = initiatorWrapper.getToken();
            if (initiatorToken instanceof IssuedToken) {
                SecurityToken secToken = getSecurityToken();
                if (secToken == null) {
                    policyNotAsserted(initiatorToken, "Security token is not found or expired");
                    return;
                } else {
                    assertPolicy(initiatorToken);
                    
                    if (isTokenRequired(initiatorToken.getIncludeTokenType())) {
                        Element el = secToken.getToken();
                        this.addEncryptedKeyElement(cloneElement(el));
                        attached = true;
                    } 
                }
            } else if (initiatorToken instanceof SamlToken && isRequestor()) {
                try {
                    SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)initiatorToken);
                    if (assertionWrapper != null) {
                        if (isTokenRequired(initiatorToken.getIncludeTokenType())) {
                            addSupportingElement(assertionWrapper.toDOM(saaj.getSOAPPart()));
                            storeAssertionAsSecurityToken(assertionWrapper);
                        }
                        assertPolicy(initiatorToken);
                    }
                } catch (Exception e) {
                    String reason = e.getMessage();
                    LOG.log(Level.WARNING, "Encrypt before sign failed due to : " + reason);
                    LOG.log(Level.FINE, e.getMessage(), e);
                    throw new Fault(e);
                }
            } else if (initiatorToken instanceof SamlToken) {
                String tokenId = getSAMLToken();
                if (tokenId == null) {
                    policyNotAsserted(initiatorToken, "Security token is not found or expired");
                    return;
                }
            }
            assertToken(initiatorToken);
        }
        
        List<WSEncryptionPart> sigParts = new ArrayList<WSEncryptionPart>();
        if (timestampEl != null) {
            WSEncryptionPart timestampPart = 
                convertToEncryptionPart(timestampEl.getElement());
            sigParts.add(timestampPart);
        }

        try {
            addSupportingTokens(sigParts);
        } catch (WSSecurityException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            policyNotAsserted(encryptionToken, ex);
        }
        
        List<WSEncryptionPart> encrParts = null;
        try {
            encrParts = getEncryptedParts();
            //Signed parts are determined before encryption because encrypted signed headers
            //will not be included otherwise
            sigParts.addAll(this.getSignedParts(null));
        } catch (SOAPException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            throw new Fault(ex);
        }
        
        WSSecBase encrBase = null;
        if (encryptionToken != null && encrParts.size() > 0) {
            encrBase = doEncryption(wrapper, encrParts, true);
            handleEncryptedSignedHeaders(encrParts, sigParts);
        }
        
        if (!isRequestor()) {
            addSignatureConfirmation(sigParts);
        }

        try {
            if (sigParts.size() > 0) {
                if (initiatorWrapper != null && isRequestor()) {
                    doSignature(initiatorWrapper, sigParts, attached);
                } else if (!isRequestor()) {
                    AbstractTokenWrapper recipientSignatureToken = 
                        abinding.getRecipientSignatureToken();
                    if (recipientSignatureToken == null) {
                        recipientSignatureToken = abinding.getRecipientToken(); 
                    }
                    if (recipientSignatureToken != null) {
                        assertTokenWrapper(recipientSignatureToken);
                        assertToken(recipientSignatureToken.getToken());
                        doSignature(recipientSignatureToken, sigParts, attached);
                    }
                }
            }
        } catch (WSSecurityException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            throw new Fault(ex);
        } catch (SOAPException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            throw new Fault(ex);
        }

        if (isRequestor()) {
            doEndorse();
        }

        if (encrBase != null) {
            encryptTokensInSecurityHeader(encryptionToken, encrBase);
        }
    }
    
    
    private void encryptTokensInSecurityHeader(AbstractToken encryptionToken, WSSecBase encrBase) {
        List<WSEncryptionPart> secondEncrParts = new ArrayList<WSEncryptionPart>();
        
        // Check for signature protection
        if (abinding.isEncryptSignature()) {
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));

            // Now encrypt the signature using the above token
            if (mainSigId != null) {
                WSEncryptionPart sigPart = new WSEncryptionPart(mainSigId, "Element");
                sigPart.setElement(bottomUpElement);
                secondEncrParts.add(sigPart);
            }
            
            if (sigConfList != null && !sigConfList.isEmpty()) {
                secondEncrParts.addAll(sigConfList);
            }
        }
            
        // Add any SupportingTokens that need to be encrypted
        if (isRequestor()) {
            secondEncrParts.addAll(encryptedTokensList);
        }
        
        if (secondEncrParts.isEmpty()) {
            return;
        }

        // Perform encryption
        if (encryptionToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys
            && encrBase instanceof WSSecDKEncrypt) {
            try {
                Element secondRefList = 
                    ((WSSecDKEncrypt)encrBase).encryptForExternalRef(null, secondEncrParts);
                ((WSSecDKEncrypt)encrBase).addExternalRefElement(secondRefList, secHeader);

            } catch (WSSecurityException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                throw new Fault(ex);
            }
        } else if (encrBase instanceof WSSecEncrypt) {
            try {
                // Encrypt, get hold of the ref list and add it
                Element secondRefList = saaj.getSOAPPart()
                    .createElementNS(WSConstants.ENC_NS,
                                     WSConstants.ENC_PREFIX + ":ReferenceList");
                if (lastEncryptedKeyElement != null) {
                    insertAfter(secondRefList, lastEncryptedKeyElement);
                } else {
                    this.insertBeforeBottomUp(secondRefList);
                }
                ((WSSecEncrypt)encrBase).encryptForRef(secondRefList, secondEncrParts);

            } catch (WSSecurityException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                throw new Fault(ex);
            }
        }
    }
    
    private WSSecBase doEncryption(AbstractTokenWrapper recToken,
                                    List<WSEncryptionPart> encrParts,
                                    boolean externalRef) {
        //Do encryption
        if (recToken != null && recToken.getToken() != null && encrParts.size() > 0) {
            AbstractToken encrToken = recToken.getToken();
            assertPolicy(recToken);
            assertPolicy(encrToken);
            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();
            if (encrToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                try {
<<<<<<< HEAD
                    WSSecDKEncrypt dkEncr = new WSSecDKEncrypt(wssConfig);
=======
                    WSSecDKEncrypt dkEncr = new WSSecDKEncrypt();
                    dkEncr.setIdAllocator(wssConfig.getIdAllocator());
                    dkEncr.setCallbackLookup(callbackLookup);
                    dkEncr.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
                    dkEncr.setStoreBytesInAttachment(storeBytesInAttachment);
>>>>>>> f399b92... Support the ability to store BASE-64 encoded (encryption) bytes in message attachments
                    if (recToken.getToken().getVersion() == SPConstants.SPVersion.SP11) {
                        dkEncr.setWscVersion(ConversationConstants.VERSION_05_02);
                    }
                    
                    if (encrKey == null) {
                        setupEncryptedKey(recToken, encrToken);
                    }
                    
                    dkEncr.setExternalKey(this.encryptedKeyValue, this.encryptedKeyId);
                    dkEncr.setParts(encrParts);
                    dkEncr.setCustomValueType(WSConstants.SOAPMESSAGE_NS11 + "#"
                            + WSConstants.ENC_KEY_VALUE_TYPE);
                    AlgorithmSuiteType algType = algorithmSuite.getAlgorithmSuiteType();
                    dkEncr.setSymmetricEncAlgorithm(algType.getEncryption());
                    dkEncr.setDerivedKeyLength(algType.getEncryptionDerivedKeyLength() / 8);
                    dkEncr.prepare(saaj.getSOAPPart());
                    
                    addDerivedKeyElement(dkEncr.getdktElement());
                    Element refList = dkEncr.encryptForExternalRef(null, encrParts);
                    insertBeforeBottomUp(refList);
                    return dkEncr;
                } catch (Exception e) {
                    LOG.log(Level.FINE, e.getMessage(), e);
                    policyNotAsserted(recToken, e);
                }
            } else {
                try {
                    WSSecEncrypt encr = new WSSecEncrypt(wssConfig);
                    encr.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
                    encr.setStoreBytesInAttachment(storeBytesInAttachment);
                    
                    encr.setDocument(saaj.getSOAPPart());
                    Crypto crypto = getEncryptionCrypto();
                    
                    SecurityToken securityToken = getSecurityToken();
                    if (!isRequestor() && securityToken != null 
                        && recToken.getToken() instanceof SamlToken) {
                        String tokenType = securityToken.getTokenType();
                        if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                            || WSConstants.SAML_NS.equals(tokenType)) {
                            encr.setCustomEKTokenValueType(WSConstants.WSS_SAML_KI_VALUE_TYPE);
                            encr.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
                            encr.setCustomEKTokenId(securityToken.getId());
                        } else if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                            || WSConstants.SAML2_NS.equals(tokenType)) {
                            encr.setCustomEKTokenValueType(WSConstants.WSS_SAML2_KI_VALUE_TYPE);
                            encr.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
                            encr.setCustomEKTokenId(securityToken.getId());
                        } else {
                            setKeyIdentifierType(encr, encrToken);
                        }
                    } else {
                        setKeyIdentifierType(encr, encrToken);
                    }
                    //
                    // Using a stored cert is only suitable for the Issued Token case, where
                    // we're extracting the cert from a SAML Assertion on the provider side
                    //
                    if (!isRequestor() && securityToken != null 
                        && securityToken.getX509Certificate() != null) {
                        encr.setUseThisCert(securityToken.getX509Certificate());
                    } else {
                        setEncryptionUser(encr, encrToken, false, crypto);
                    }
                    if (!encr.isCertSet() && crypto == null) {
                        policyNotAsserted(recToken, "Missing security configuration. "
                                + "Make sure jaxws:client element is configured " 
                                + "with a " + SecurityConstants.ENCRYPT_PROPERTIES + " value.");
                    }
                    AlgorithmSuiteType algType = algorithmSuite.getAlgorithmSuiteType();
                    encr.setSymmetricEncAlgorithm(algType.getEncryption());
                    encr.setKeyEncAlgo(algType.getAsymmetricKeyWrap());
                    encr.prepare(saaj.getSOAPPart(), crypto);
                    
                    Element encryptedKeyElement = encr.getEncryptedKeyElement();
                    List<Element> attachments = encr.getAttachmentEncryptedDataElements();
                    //Encrypt, get hold of the ref list and add it
                    if (externalRef) {
                        Element refList = encr.encryptForRef(null, encrParts);
                        insertBeforeBottomUp(refList);
                        if (attachments != null) {
                            for (Element attachment : attachments) {
                                this.insertBeforeBottomUp(attachment);
                            }
                        }
                        this.addEncryptedKeyElement(encryptedKeyElement);
                    } else {
                        Element refList = encr.encryptForRef(null, encrParts);
                        this.addEncryptedKeyElement(encryptedKeyElement);
                        
                        // Add internal refs
                        encryptedKeyElement.appendChild(refList);
                        if (attachments != null) {
                            for (Element attachment : attachments) {
                                this.addEncryptedKeyElement(attachment);
                            }
                        }
                    }

                    // Put BST before EncryptedKey element
                    if (encr.getBSTTokenId() != null) {
                        encr.prependBSTElementToHeader(secHeader);
                    }

                    return encr;
                } catch (WSSecurityException e) {
                    LOG.log(Level.FINE, e.getMessage(), e);
                    policyNotAsserted(recToken, e);
                }    
            }
        }
        return null;
    }    
    
    private void assertUnusedTokens(AbstractTokenWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        Collection<AssertionInfo> ais = aim.getAssertionInfo(wrapper.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == wrapper) {
                    ai.setAsserted(true);
                }
            }
        }
        ais = aim.getAssertionInfo(wrapper.getToken().getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == wrapper.getToken()) {
                    ai.setAsserted(true);
                }
            }
        }
    }
    
    private void doSignature(AbstractTokenWrapper wrapper, List<WSEncryptionPart> sigParts, boolean attached) 
        throws WSSecurityException, SOAPException {
        
        if (!isRequestor()) {
            assertUnusedTokens(abinding.getInitiatorToken());
            assertUnusedTokens(abinding.getInitiatorEncryptionToken());
            assertUnusedTokens(abinding.getInitiatorSignatureToken());
        } else {
            assertUnusedTokens(abinding.getRecipientToken());
            assertUnusedTokens(abinding.getRecipientEncryptionToken());
            assertUnusedTokens(abinding.getRecipientSignatureToken());
        }
        
        AbstractToken sigToken = wrapper.getToken();
        if (sigParts.isEmpty()) {
            // Add the BST to the security header if required
            if (!attached && isTokenRequired(sigToken.getIncludeTokenType())) {
                WSSecSignature sig = getSignatureBuilder(sigToken, attached, false);
                sig.appendBSTElementToHeader(secHeader);
            } 
            return;
        }
        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            // Set up the encrypted key to use
            setupEncryptedKey(wrapper, sigToken);
            
<<<<<<< HEAD
            WSSecDKSign dkSign = new WSSecDKSign(wssConfig);
=======
            WSSecDKSign dkSign = new WSSecDKSign();
            dkSign.setIdAllocator(wssConfig.getIdAllocator());
            dkSign.setCallbackLookup(callbackLookup);
            dkSign.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
            dkSign.setStoreBytesInAttachment(storeBytesInAttachment);
>>>>>>> f399b92... Support the ability to store BASE-64 encoded (encryption) bytes in message attachments
            if (wrapper.getToken().getVersion() == SPConstants.SPVersion.SP11) {
                dkSign.setWscVersion(ConversationConstants.VERSION_05_02);
            }
            
            dkSign.setExternalKey(this.encryptedKeyValue, this.encryptedKeyId);

            // Set the algo info
            dkSign.setSignatureAlgorithm(abinding.getAlgorithmSuite().getSymmetricSignature());
            dkSign.setSigCanonicalization(abinding.getAlgorithmSuite().getC14n().getValue());
            AlgorithmSuiteType algType = abinding.getAlgorithmSuite().getAlgorithmSuiteType();
            dkSign.setDerivedKeyLength(algType.getSignatureDerivedKeyLength() / 8);
            dkSign.setCustomValueType(WSConstants.SOAPMESSAGE_NS11 + "#"
                    + WSConstants.ENC_KEY_VALUE_TYPE);
            
            try {
                dkSign.prepare(saaj.getSOAPPart(), secHeader);

                if (abinding.isProtectTokens()) {
                    assertPolicy(
                        new QName(abinding.getName().getNamespaceURI(), SPConstants.PROTECT_TOKENS));
                    if (bstElement != null) {
                        WSEncryptionPart bstPart = 
                            new WSEncryptionPart(bstElement.getAttributeNS(WSConstants.WSU_NS, "Id"));
                        bstPart.setElement(bstElement);
                        sigParts.add(bstPart);
                    } else {
                        WSEncryptionPart ekPart = 
                            new WSEncryptionPart(encrKey.getId());
                        ekPart.setElement(encrKey.getEncryptedKeyElement());
                        sigParts.add(ekPart);
                    }
                }

                dkSign.setParts(sigParts);

                List<Reference> referenceList = dkSign.addReferencesToSign(sigParts, secHeader);

                // Add elements to header
                addDerivedKeyElement(dkSign.getdktElement());
                
                //Do signature
                if (bottomUpElement == null) {
                    dkSign.computeSignature(referenceList, false, null);
                } else {
                    dkSign.computeSignature(referenceList, true, bottomUpElement);
                }
                bottomUpElement = dkSign.getSignatureElement();
                signatures.add(dkSign.getSignatureValue());
                
                mainSigId = dkSign.getSignatureId();
            } catch (Exception ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                throw new Fault(ex);
            }
        } else {
            WSSecSignature sig = getSignatureBuilder(sigToken, attached, false);
                      
            // This action must occur before sig.prependBSTElementToHeader
            if (abinding.isProtectTokens()) {
                assertPolicy(
                    new QName(abinding.getName().getNamespaceURI(), SPConstants.PROTECT_TOKENS));
                if (sig.getBSTTokenId() != null) {
                    WSEncryptionPart bstPart = 
                        new WSEncryptionPart(sig.getBSTTokenId());
                    bstPart.setElement(sig.getBinarySecurityTokenElement());
                    sigParts.add(bstPart);
                }
                sig.prependBSTElementToHeader(secHeader);
            }

            List<Reference> referenceList = sig.addReferencesToSign(sigParts, secHeader);
            //Do signature
            if (bottomUpElement == null) {
                sig.computeSignature(referenceList, false, null);
            } else {
                sig.computeSignature(referenceList, true, bottomUpElement);
            }
            bottomUpElement = sig.getSignatureElement();
            
            if (!abinding.isProtectTokens()) {
                Element bstElement = sig.getBinarySecurityTokenElement();
                if (bstElement != null) {
                    secHeader.getSecurityHeader().insertBefore(bstElement, bottomUpElement);
                }
            }
            
            signatures.add(sig.getSignatureValue());
                        
            mainSigId = sig.getId();
        }
    }

    private void setupEncryptedKey(AbstractTokenWrapper wrapper, AbstractToken token) throws WSSecurityException {
        if (!isRequestor() && token.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            //If we already have them, simply return
            if (encryptedKeyId != null && encryptedKeyValue != null) {
                return;
            }
            
            //Use the secret from the incoming EncryptedKey element
            List<WSHandlerResult> results = 
                CastUtils.cast(
                    (List<?>)message.getExchange().getInMessage().get(WSHandlerConstants.RECV_RESULTS));
            if (results != null) {
                encryptedKeyId = getRequestEncryptedKeyId(results);
                encryptedKeyValue = getRequestEncryptedKeyValue(results);
                
                //In the case where we don't have the EncryptedKey in the 
                //request, for the control to have reached this state,
                //the scenario MUST be a case where this is the response
                //message by a listener created for an async client
                //Therefor we will create a new EncryptedKey
                if (encryptedKeyId == null && encryptedKeyValue == null) {
                    createEncryptedKey(wrapper, token);
                }
            } else {
                policyNotAsserted(token, "No security results found");
            }
        } else {
            createEncryptedKey(wrapper, token);
        }
    }
    
    public static String getRequestEncryptedKeyId(List<WSHandlerResult> results) {
        
        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();
            /*
             * Scan the results for the first Signature action. Use the
             * certificate of this Signature to set the certificate for the
             * encryption action :-).
             */
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                String encrKeyId = (String)wser.get(WSSecurityEngineResult.TAG_ID);
                if (actInt.intValue() == WSConstants.ENCR && encrKeyId != null) {
                    return encrKeyId;
                }
            }
        }
        
        return null;
    }
    
    public static byte[] getRequestEncryptedKeyValue(List<WSHandlerResult> results) {
        
        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();

            /*
            * Scan the results for the first Signature action. Use the
            * certificate of this Signature to set the certificate for the
            * encryption action :-).
            */
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                byte[] decryptedKey = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                if (actInt.intValue() == WSConstants.ENCR && decryptedKey != null) {
                    return decryptedKey;
                }
            }
        }
        
        return null;
    }
    
    private void createEncryptedKey(AbstractTokenWrapper wrapper, AbstractToken token)
        throws WSSecurityException {
        //Set up the encrypted key to use
        encrKey = this.getEncryptedKeyBuilder(token);
        assertPolicy(wrapper);
        Element bstElem = encrKey.getBinarySecurityTokenElement();
        if (bstElem != null) {
            // If a BST is available then use it
            encrKey.prependBSTElementToHeader(secHeader);
        }
        
        // Add the EncryptedKey
        this.addEncryptedKeyElement(encrKey.getEncryptedKeyElement());
        encryptedKeyValue = encrKey.getEphemeralKey();
        encryptedKeyId = encrKey.getId();
        
        //Store the token for client - response verification 
        // and server - response creation
        message.put(WSSecEncryptedKey.class.getName(), encrKey);
    }

    private String getSAMLToken() {
        
        List<WSHandlerResult> results = CastUtils.cast((List<?>)message.getExchange().getInMessage()
            .get(WSHandlerConstants.RECV_RESULTS));
        
        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();
            
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                String id = (String)wser.get(WSSecurityEngineResult.TAG_ID);
                if (actInt.intValue() == WSConstants.ST_SIGNED 
                    || actInt.intValue() == WSConstants.ST_UNSIGNED) {
                    Date created = new Date();
                    Date expires = new Date();
                    expires.setTime(created.getTime() + 300000);
                    SecurityToken tempTok = new SecurityToken(id, created, expires);
                    tempTok.setSecret((byte[])wser.get(WSSecurityEngineResult.TAG_SECRET));
                    tempTok.setX509Certificate(
                        (X509Certificate)wser.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE), null
                    );
                    
                    SamlAssertionWrapper samlAssertion = 
                        (SamlAssertionWrapper)wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                    if (samlAssertion.getSamlVersion() == SAMLVersion.VERSION_20) {
                        tempTok.setTokenType(WSConstants.WSS_SAML2_TOKEN_TYPE);
                    } else {
                        tempTok.setTokenType(WSConstants.WSS_SAML_TOKEN_TYPE);
                    }
                    
                    getTokenStore().add(tempTok);
                    message.put(SecurityConstants.TOKEN_ID, tempTok.getId());
                    
                    return id;
                }
            }
        }
        return null;
    }
}
