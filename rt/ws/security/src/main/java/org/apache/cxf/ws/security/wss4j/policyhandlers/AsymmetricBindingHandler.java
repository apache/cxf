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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.crypto.dsig.Reference;
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
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSEncryptionPart;
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
        
        if (abinding.getProtectionOrder() 
            == AbstractSymmetricAsymmetricBinding.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
            policyAsserted(SPConstants.ENCRYPT_BEFORE_SIGNING);
        } else {
            doSignBeforeEncrypt();
            policyAsserted(SPConstants.SIGN_BEFORE_ENCRYPTING);
        }
    }

    private void doSignBeforeEncrypt() {
        try {
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            boolean attached = false;
            if (initiatorWrapper != null) {
                AbstractToken initiatorToken = initiatorWrapper.getToken();
                if (initiatorToken instanceof IssuedToken) {
                    SecurityToken secToken = getSecurityToken();
                    if (secToken == null) {
                        policyNotAsserted(initiatorToken, "Security token is not found or expired");
                        return;
                    } else {
                        policyAsserted(initiatorToken);
                        
                        if (includeToken(initiatorToken.getIncludeTokenType())) {
                            Element el = secToken.getToken();
                            this.addEncryptedKeyElement(cloneElement(el));
                            attached = true;
                        } 
                    }
                } else if (initiatorToken instanceof SamlToken) {
                    SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)initiatorToken);
                    if (assertionWrapper != null) {
                        if (includeToken(initiatorToken.getIncludeTokenType())) {
                            addSupportingElement(assertionWrapper.toDOM(saaj.getSOAPPart()));
                            storeAssertionAsSecurityToken(assertionWrapper);
                        }
                        policyAsserted(initiatorToken);
                    }
                }
            }
            
            // Add timestamp
            List<WSEncryptionPart> sigs = new ArrayList<WSEncryptionPart>();
            if (timestampEl != null) {
                WSEncryptionPart timestampPart = 
                    convertToEncryptionPart(timestampEl.getElement());
                sigs.add(timestampPart);
            }
            addSupportingTokens(sigs);
            
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
                policyAsserted(SPConstants.ENCRYPT_SIGNATURE);
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
            
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Sign before encryption failed due to : " + reason);
            throw new Fault(e);
        }
    }

    private void doEncryptBeforeSign() {
        AbstractTokenWrapper wrapper;
        AbstractToken encryptionToken = null;
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
        encryptionToken = wrapper.getToken();
        
        AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
        if (initiatorWrapper == null) {
            initiatorWrapper = abinding.getInitiatorToken();
        }
        boolean attached = false;
        if (initiatorWrapper != null) {
            AbstractToken initiatorToken = initiatorWrapper.getToken();
            if (initiatorToken instanceof IssuedToken) {
                SecurityToken secToken = getSecurityToken();
                if (secToken == null) {
                    policyNotAsserted(initiatorToken, "Security token is not found or expired");
                    return;
                } else {
                    policyAsserted(initiatorToken);
                    
                    if (includeToken(initiatorToken.getIncludeTokenType())) {
                        Element el = secToken.getToken();
                        this.addEncryptedKeyElement(cloneElement(el));
                        attached = true;
                    } 
                }
            } else if (initiatorToken instanceof SamlToken) {
                try {
                    SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)initiatorToken);
                    if (assertionWrapper != null) {
                        if (includeToken(initiatorToken.getIncludeTokenType())) {
                            addSupportingElement(assertionWrapper.toDOM(saaj.getSOAPPart()));
                            storeAssertionAsSecurityToken(assertionWrapper);
                        }
                        policyAsserted(initiatorToken);
                    }
                } catch (Exception e) {
                    String reason = e.getMessage();
                    LOG.log(Level.WARNING, "Encrypt before sign failed due to : " + reason);
                    throw new Fault(e);
                }
            }
        }
        
        List<WSEncryptionPart> encrParts = null;
        List<WSEncryptionPart> sigParts = null;
        try {
            encrParts = getEncryptedParts();
            //Signed parts are determined before encryption because encrypted signed  headers
            //will not be included otherwise
            sigParts = getSignedParts();
        } catch (SOAPException ex) {
            throw new Fault(ex);
        }
        
        //if (encryptionToken == null && encrParts.size() > 0) {
            //REVISIT - no token to encrypt with  
        //}
        
        if (encryptionToken != null && encrParts.size() > 0) {
            WSSecBase encrBase = doEncryption(wrapper, encrParts, true);
            handleEncryptedSignedHeaders(encrParts, sigParts);
            
            if (timestampEl != null) {
                WSEncryptionPart timestampPart = 
                    convertToEncryptionPart(timestampEl.getElement());
                sigParts.add(timestampPart);
            }
            
            if (isRequestor()) {
                try {
                    addSupportingTokens(sigParts);
                } catch (WSSecurityException ex) {
                    policyNotAsserted(encryptionToken, ex);
                }
            } else {
                addSignatureConfirmation(sigParts);
            }
            
            try {
                if ((sigParts.size() > 0) && initiatorWrapper != null && isRequestor()) {
                    doSignature(initiatorWrapper, sigParts, attached);
                } else if (!isRequestor()) {
                    AbstractTokenWrapper recipientSignatureToken = abinding.getRecipientSignatureToken();
                    if (recipientSignatureToken == null) {
                        recipientSignatureToken = abinding.getRecipientToken(); 
                    }
                    if (recipientSignatureToken != null) {
                        doSignature(recipientSignatureToken, sigParts, attached);
                    }
                }
            } catch (WSSecurityException ex) {
                throw new Fault(ex);
            } catch (SOAPException ex) {
                throw new Fault(ex);
            }

            if (isRequestor()) {
                doEndorse();
            }
            
            encryptTokensInSecurityHeader(encryptionToken, encrBase);
        }
    }
    
    
    private void encryptTokensInSecurityHeader(AbstractToken encryptionToken, WSSecBase encrBase) {
        List<WSEncryptionPart> secondEncrParts = new ArrayList<WSEncryptionPart>();
        
        // Check for signature protection
        if (abinding.isEncryptSignature()) {
            policyAsserted(SPConstants.ENCRYPT_SIGNATURE);

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
            policyAsserted(recToken);
            policyAsserted(encrToken);
            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();
            if (encrToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                try {
                    WSSecDKEncrypt dkEncr = new WSSecDKEncrypt(wssConfig);
                    
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
                    policyNotAsserted(recToken, e);
                }
            } else {
                try {
                    WSSecEncrypt encr = new WSSecEncrypt(wssConfig);
                    
                    encr.setDocument(saaj.getSOAPPart());
                    Crypto crypto = getEncryptionCrypto(recToken);
                    
                    SecurityToken securityToken = getSecurityToken();
                    setKeyIdentifierType(encr, recToken, encrToken);
                    //
                    // Using a stored cert is only suitable for the Issued Token case, where
                    // we're extracting the cert from a SAML Assertion on the provider side
                    //
                    if (!isRequestor() && securityToken != null 
                        && securityToken.getX509Certificate() != null) {
                        encr.setUseThisCert(securityToken.getX509Certificate());
                    } else {
                        setEncryptionUser(encr, recToken, false, crypto);
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
                    
                    if (encr.getBSTTokenId() != null) {
                        encr.prependBSTElementToHeader(secHeader);
                    }
                    
                    Element encryptedKeyElement = encr.getEncryptedKeyElement();
                                       
                    //Encrypt, get hold of the ref list and add it
                    if (externalRef) {
                        Element refList = encr.encryptForRef(null, encrParts);
                        insertBeforeBottomUp(refList);
                    } else {
                        Element refList = encr.encryptForRef(null, encrParts);
                    
                        // Add internal refs
                        encryptedKeyElement.appendChild(refList);
                    }
                    this.addEncryptedKeyElement(encryptedKeyElement);
                    return encr;
                } catch (WSSecurityException e) {
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
        sigParts.addAll(this.getSignedParts());
        if (sigParts.isEmpty()) {
            // Add the BST to the security header if required
            if (!attached && includeToken(sigToken.getIncludeTokenType())) {
                WSSecSignature sig = getSignatureBuilder(wrapper, sigToken, attached, false);
                sig.prependBSTElementToHeader(secHeader);
            } 
            return;
        }
        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            // Set up the encrypted key to use
            setupEncryptedKey(wrapper, sigToken);
            
            WSSecDKSign dkSign = new WSSecDKSign(wssConfig);
            dkSign.setExternalKey(this.encryptedKeyValue, this.encryptedKeyId);

            // Set the algo info
            dkSign.setSignatureAlgorithm(abinding.getAlgorithmSuite()
                    .getSymmetricSignature());
            AlgorithmSuiteType algType = abinding.getAlgorithmSuite().getAlgorithmSuiteType();
            dkSign.setDerivedKeyLength(algType.getSignatureDerivedKeyLength() / 8);
            dkSign.setCustomValueType(WSConstants.SOAPMESSAGE_NS11 + "#"
                    + WSConstants.ENC_KEY_VALUE_TYPE);
            
            try {
                dkSign.prepare(saaj.getSOAPPart(), secHeader);

                if (abinding.isProtectTokens()) {
                    policyAsserted(SPConstants.PROTECT_TOKENS);
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
                throw new Fault(ex);
            }
        } else {
            WSSecSignature sig = getSignatureBuilder(wrapper, sigToken, attached, false);
                      
            // This action must occur before sig.prependBSTElementToHeader
            if (abinding.isProtectTokens()) {
                policyAsserted(SPConstants.PROTECT_TOKENS);
                if (sig.getBSTTokenId() != null) {
                    WSEncryptionPart bstPart = 
                        new WSEncryptionPart(sig.getBSTTokenId());
                    bstPart.setElement(sig.getBinarySecurityTokenElement());
                    sigParts.add(bstPart);
                } else if (bstElement != null) {
                    WSEncryptionPart bstPart = 
                        new WSEncryptionPart(bstElement.getAttributeNS(WSConstants.WSU_NS, "Id"));
                    bstPart.setElement(bstElement);
                    sigParts.add(bstPart);
                }
            }

            sig.prependBSTElementToHeader(secHeader);
            
            List<Reference> referenceList = sig.addReferencesToSign(sigParts, secHeader);
            //Do signature
            if (bottomUpElement == null) {
                sig.computeSignature(referenceList, false, null);
            } else {
                sig.computeSignature(referenceList, true, bottomUpElement);
            }
            bottomUpElement = sig.getSignatureElement();
            
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
        encrKey = this.getEncryptedKeyBuilder(wrapper, token);
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



}
