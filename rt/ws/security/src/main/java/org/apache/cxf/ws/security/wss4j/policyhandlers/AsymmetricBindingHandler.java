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


import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.policy.model.RecipientToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.TokenWrapper;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.WSSecBase;
import org.apache.ws.security.message.WSSecDKEncrypt;
import org.apache.ws.security.message.WSSecDKSign;
import org.apache.ws.security.message.WSSecEncrypt;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;

/**
 * 
 */
public class AsymmetricBindingHandler extends AbstractBindingBuilder {

    private static final Logger LOG = LogUtils.getL7dLogger(AsymmetricBindingHandler.class);

    AsymmetricBinding abinding;
    
    private WSSecEncryptedKey encrKey;
    private String encryptedKeyId;
    private byte[] encryptedKeyValue;
    
    public AsymmetricBindingHandler(AsymmetricBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) {
        super(binding, saaj, secHeader, aim, message);
        this.abinding = binding;
        protectionOrder = binding.getProtectionOrder();
    }
    
    public void handleBinding() {
        WSSecTimestamp timestamp = createTimestamp();
        handleLayout(timestamp);
        
        if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
        } else {
            doSignBeforeEncrypt();
        }
    }



    private void doSignBeforeEncrypt() {
        try {
            Vector<WSEncryptionPart> sigs = getSignedParts();
            if (isRequestor()) {
                //Add timestamp
                if (timestampEl != null) {
                    Element el = timestampEl.getElement();
                    sigs.add(new WSEncryptionPart(addWsuIdToElement(el)));
                }

                addSupportingTokens(sigs);
                doSignature(sigs);
                doEndorse();
            } else {
                //confirm sig
                assertSupportingTokens(sigs);
                
                
                //Add timestamp
                if (timestampEl != null) {
                    Element el = timestampEl.getElement();
                    sigs.add(new WSEncryptionPart(addWsuIdToElement(el)));
                }

                addSignatureConfirmation(sigs);
                doSignature(sigs);
            }

            Vector<WSEncryptionPart> enc = getEncryptedParts();
            
            //Check for signature protection
            if (abinding.isSignatureProtection() && mainSigId != null) {
                enc.add(new WSEncryptionPart(mainSigId, "Element"));
            }
            
            if (isRequestor()) {
                for (String id : encryptedTokensIdList) {
                    enc.add(new WSEncryptionPart(id, "Element"));
                }
            }

            //Do encryption
            RecipientToken recToken = abinding.getRecipientToken();

            
            doEncryption(recToken, enc, false);
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Sign before encryption failed due to : " + reason);
            throw new Fault(e);
        }
    }

    private void doEncryptBeforeSign() {
        TokenWrapper wrapper;
        Token encryptionToken = null;
        if (isRequestor()) {
            wrapper = abinding.getRecipientToken();
        } else {
            wrapper = abinding.getInitiatorToken();
        }
        encryptionToken = wrapper.getToken();
        Vector<WSEncryptionPart> encrParts = null;
        Vector<WSEncryptionPart> sigParts = null;
        try {
            encrParts = getEncryptedParts();
            //Signed parts are determined before encryption because encrypted signed  headers
            //will not be included otherwise
            sigParts = getSignedParts();
        } catch (SOAPException e1) {
            //REVISIT - exception
            e1.printStackTrace();
        }
        
        
        if (encryptionToken == null && encrParts.size() > 0) {
            //REVISIT - no token to encrypt with  
        }
        
        
        if (encryptionToken != null && encrParts.size() > 0) {
            WSSecBase encrBase = doEncryption(wrapper, encrParts, true);
            handleEncryptedSignedHeaders(encrParts, sigParts);
            
            
            if (timestampEl != null) {
                sigParts.add(new WSEncryptionPart(addWsuIdToElement(timestampEl.getElement())));
            }
            
            if (isRequestor()) {
                addSupportingTokens(sigParts);
            } else {
                addSignatureConfirmation(sigParts);
            }
            
            if ((sigParts.size() > 0 
                    && isRequestor()
                    && abinding.getInitiatorToken() != null) 
                || (!isRequestor() && abinding.getRecipientToken() != null)) {
                try {
                    doSignature(sigParts);
                } catch (WSSecurityException e) {
                    //REVISIT - exception
                    e.printStackTrace();
                }
            }

            if (isRequestor()) {
                doEndorse();
            }
            
            // Check for signature protection
            if (abinding.isSignatureProtection() && mainSigId != null) {
                Vector<WSEncryptionPart> secondEncrParts = new Vector<WSEncryptionPart>();

                // Now encrypt the signature using the above token
                secondEncrParts.add(new WSEncryptionPart(mainSigId, "Element"));
                
                if (isRequestor()) {
                    for (String id : encryptedTokensIdList) {
                        secondEncrParts.add(new WSEncryptionPart(id, "Element"));
                    }
                }

                if (encryptionToken.isDerivedKeys()) {
                    try {
                        Element secondRefList 
                            = ((WSSecDKEncrypt)encrBase).encryptForExternalRef(null, secondEncrParts);
                        ((WSSecDKEncrypt)encrBase).addExternalRefElement(secondRefList, secHeader);

                    } catch (WSSecurityException e) {
                        //REVISIT - exception
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Encrypt, get hold of the ref list and add it
                        Element secondRefList = saaj.getSOAPPart()
                            .createElementNS(WSConstants.ENC_NS,
                                             WSConstants.ENC_PREFIX + ":ReferenceList");
                        this.insertBeforeBottomUp(secondRefList);
                        ((WSSecEncrypt)encrBase).encryptForExternalRef(secondRefList, secondEncrParts);
                        
                    } catch (WSSecurityException e) {
                        //REVISIT - exception
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    
    private WSSecBase doEncryption(TokenWrapper recToken,
                                    Vector<WSEncryptionPart> encrParts,
                                    boolean externalRef) {
        //Do encryption
        if (recToken != null && recToken.getToken() != null && encrParts.size() > 0) {
            Token encrToken = recToken.getToken();
            policyAsserted(recToken);
            policyAsserted(encrToken);
            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();
            if (encrToken.isDerivedKeys()) {
                try {
                    WSSecDKEncrypt dkEncr = new WSSecDKEncrypt();
                    
                    if (encrKey == null) {
                        setupEncryptedKey(recToken, encrToken);
                    }
                    
                    dkEncr.setExternalKey(this.encryptedKeyValue, this.encryptedKeyId);
                    dkEncr.setParts(encrParts);
                    dkEncr.setCustomValueType(WSConstants.SOAPMESSAGE_NS11 + "#"
                            + WSConstants.ENC_KEY_VALUE_TYPE);
                    dkEncr.setSymmetricEncAlgorithm(algorithmSuite.getEncryption());
                    dkEncr.setDerivedKeyLength(algorithmSuite.getEncryptionDerivedKeyLength() / 8);
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
                    WSSecEncrypt encr = new WSSecEncrypt();
                    
                    setKeyIdentifierType(encr, recToken, encrToken);
                    
                    encr.setDocument(saaj.getSOAPPart());
                    Crypto crypto = getEncryptionCrypto(recToken);
                    setEncryptionUser(encr, recToken, false, crypto);
                    encr.setSymmetricEncAlgorithm(algorithmSuite.getEncryption());
                    encr.setKeyEncAlgo(algorithmSuite.getAsymmetricKeyWrap());
                    
                    encr.prepare(saaj.getSOAPPart(),
                                 crypto);
                    
                    if (encr.getBSTTokenId() != null) {
                        encr.prependBSTElementToHeader(secHeader);
                    }
                    
                    
                    Element encryptedKeyElement = encr.getEncryptedKeyElement();
                                       
                    //Encrypt, get hold of the ref list and add it
                    if (externalRef) {
                        Element refList = encr.encryptForExternalRef(null, encrParts);
                        insertBeforeBottomUp(refList);
                    } else {
                        Element refList = encr.encryptForInternalRef(null, encrParts);
                    
                        // Add internal refs
                        encryptedKeyElement.appendChild(refList);
                    }
                    this.addEncyptedKeyElement(encryptedKeyElement);
                    return encr;
                } catch (WSSecurityException e) {
                    policyNotAsserted(recToken, e.getMessage());
                }    
            }
        }
        return null;
    }    
    
    private void assertUnusedTokens(TokenWrapper wrapper) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(wrapper.getName());
        for (AssertionInfo ai : ais) {
            if (ai.getAssertion() == wrapper) {
                ai.setAsserted(true);
            }
        }
        ais = aim.getAssertionInfo(wrapper.getToken().getName());
        for (AssertionInfo ai : ais) {
            if (ai.getAssertion() == wrapper.getToken()) {
                ai.setAsserted(true);
            }
        }
    }
    private void doSignature(Vector<WSEncryptionPart> sigParts) throws WSSecurityException {
        Token sigToken = null;
        TokenWrapper wrapper = null;
        if (isRequestor()) {
            wrapper = abinding.getInitiatorToken();
        } else {
            wrapper = abinding.getRecipientToken();
            assertUnusedTokens(abinding.getInitiatorToken());
        }
        sigToken = wrapper.getToken();

        if (sigToken.isDerivedKeys()) {
            // Set up the encrypted key to use
            setupEncryptedKey(wrapper, sigToken);
            
            WSSecDKSign dkSign = new WSSecDKSign();
            dkSign.setExternalKey(this.encryptedKeyValue, this.encryptedKeyId);

            // Set the algo info
            dkSign.setSignatureAlgorithm(abinding.getAlgorithmSuite()
                    .getSymmetricSignature());
            dkSign.setDerivedKeyLength(abinding.getAlgorithmSuite()
                    .getSignatureDerivedKeyLength() / 8);
            dkSign.setCustomValueType(WSConstants.SOAPMESSAGE_NS11 + "#"
                    + WSConstants.ENC_KEY_VALUE_TYPE);
            
            try {
                dkSign.prepare(saaj.getSOAPPart(), secHeader);

                if (abinding.isTokenProtection()) {
                    sigParts.add(new WSEncryptionPart(encrKey.getId()));
                }

                dkSign.setParts(sigParts);

                dkSign.addReferencesToSign(sigParts, secHeader);

                // Do signature
                dkSign.computeSignature();
                signatures.add(dkSign.getSignatureValue());

                // Add elements to header
                addDerivedKeyElement(dkSign.getdktElement());
                insertBeforeBottomUp(dkSign.getSignatureElement());                
                mainSigId = addWsuIdToElement(dkSign.getSignatureElement());
            } catch (Exception e) {
                //REVISIT
                e.printStackTrace();
            }
        } else {
            WSSecSignature sig = getSignatureBuider(wrapper, sigToken, false);
            sig.prependBSTElementToHeader(secHeader);
            insertBeforeBottomUp(sig.getSignatureElement());
            
            if (abinding.isTokenProtection()) {                
                // Special flag telling WSS4J to sign the initiator token.
                // Use this instead of the BST ID so that we don't
                // have to deal with maintaining such logic here.
                sigParts.add(new WSEncryptionPart("Token", null, 
                        "Element", WSConstants.PART_TYPE_ELEMENT));
            }
                    
            sig.prependBSTElementToHeader(secHeader);

            sig.addReferencesToSign(sigParts, secHeader);
            sig.computeSignature();
            signatures.add(sig.getSignatureValue());

                        
            mainSigId = addWsuIdToElement(sig.getSignatureElement());
        }
    }

    private void setupEncryptedKey(TokenWrapper wrapper, Token token) throws WSSecurityException {
        if (!isRequestor() && token.isDerivedKeys()) {
            //If we already have them, simply return
            if (encryptedKeyId != null && encryptedKeyValue != null) {
                return;
            }
            
            //Use the secret from the incoming EncryptedKey element
            Object resultsObj = message.getExchange().getInMessage().get(WSHandlerConstants.RECV_RESULTS);
            if (resultsObj != null) {
                encryptedKeyId = getRequestEncryptedKeyId((Vector)resultsObj);
                encryptedKeyValue = getRequestEncryptedKeyValue((Vector)resultsObj);
                
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
    public static String getRequestEncryptedKeyId(Vector results) {
        
        for (int i = 0; i < results.size(); i++) {
            WSHandlerResult rResult =
                    (WSHandlerResult) results.get(i);

            Vector wsSecEngineResults = rResult.getResults();
            /*
            * Scan the results for the first Signature action. Use the
            * certificate of this Signature to set the certificate for the
            * encryption action :-).
            */
            for (int j = 0; j < wsSecEngineResults.size(); j++) {
                WSSecurityEngineResult wser =
                        (WSSecurityEngineResult) wsSecEngineResults.get(j);
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                String encrKeyId = (String)wser.get(WSSecurityEngineResult.TAG_ENCRYPTED_KEY_ID);
                if (actInt.intValue() == WSConstants.ENCR
                    && encrKeyId != null) {
                    return encrKeyId;
                }
            }
        }
        
        return null;
    }
    
    public static byte[] getRequestEncryptedKeyValue(Vector results) {
        
        for (int i = 0; i < results.size(); i++) {
            WSHandlerResult rResult =
                    (WSHandlerResult) results.get(i);

            Vector wsSecEngineResults = rResult.getResults();
            /*
            * Scan the results for the first Signature action. Use the
            * certificate of this Signature to set the certificate for the
            * encryption action :-).
            */
            for (int j = 0; j < wsSecEngineResults.size(); j++) {
                WSSecurityEngineResult wser =
                        (WSSecurityEngineResult) wsSecEngineResults.get(j);
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                byte[] decryptedKey = (byte[])wser.get(WSSecurityEngineResult.TAG_DECRYPTED_KEY);
                if (actInt.intValue() == WSConstants.ENCR 
                    && decryptedKey != null) {
                    return decryptedKey;
                }
            }
        }
        
        return null;
    }
    
    private void createEncryptedKey(TokenWrapper wrapper, Token token)
        throws WSSecurityException {
        //Set up the encrypted key to use
        encrKey = this.getEncryptedKeyBuilder(wrapper, token);
        Element bstElem = encrKey.getBinarySecurityTokenElement();
        if (bstElem != null) {
            // If a BST is available then use it
            encrKey.prependBSTElementToHeader(secHeader);
        }
        
        // Add the EncryptedKey
        this.addEncyptedKeyElement(encrKey.getEncryptedKeyElement());
        encryptedKeyValue = encrKey.getEphemeralKey();
        encryptedKeyId = encrKey.getId();
        
        //Store the token for client - response verification 
        // and server - response creation
        message.put(WSSecEncryptedKey.class.getName(), encrKey);
    }



}
