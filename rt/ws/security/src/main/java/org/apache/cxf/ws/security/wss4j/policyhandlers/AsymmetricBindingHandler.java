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
import java.util.Map;
import java.util.Vector;

import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.policy.model.RecipientToken;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.TokenWrapper;
import org.apache.cxf.ws.security.policy.model.Wss10;
import org.apache.cxf.ws.security.policy.model.Wss11;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.WSSecBase;
import org.apache.ws.security.message.WSSecDKEncrypt;
import org.apache.ws.security.message.WSSecDKSign;
import org.apache.ws.security.message.WSSecEncrypt;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecSignatureConfirmation;
import org.apache.ws.security.message.WSSecTimestamp;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * 
 */
public class AsymmetricBindingHandler extends BindingBuilder {
    AsymmetricBinding abinding;
    
    private WSSecEncryptedKey encrKey;
    private String encryptedKeyId;
    private byte[] encryptedKeyValue;
    
    private Map<Token, WSSecBase> sigSuppTokMap;
    private Map<Token, WSSecBase> sgndEndSuppTokMap;
    private Map<Token, WSSecBase> sgndEncSuppTokMap;
    private Map<Token, WSSecBase> sgndEndEncSuppTokMap;
    private Map<Token, WSSecBase> endSuppTokMap;
    private Map<Token, WSSecBase> endEncSuppTokMap;

    
    public AsymmetricBindingHandler(AsymmetricBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) {
        super(binding, saaj, secHeader, aim, message);
        this.abinding = binding;
    }
    
    public void handleBinding() {
        WSSecTimestamp timestamp = createTimestamp();
        timestamp = handleLayout(timestamp);
        
        if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
        } else {
            doSignBeforeEncrypt();
        }

        if (timestamp != null) {
            timestamp.prependToHeader(secHeader);
        }
    }


    private void doSignBeforeEncrypt() {
        try {
            Vector<WSEncryptionPart> sigs = getSignedParts();
            if (isRequestor()) {
                SupportingToken sgndSuppTokens = 
                    (SupportingToken)findPolicy(SP12Constants.SIGNED_SUPPORTING_TOKENS);
                
                sigSuppTokMap = this.handleSupportingTokens(sgndSuppTokens);           
                
                SupportingToken endSuppTokens = 
                    (SupportingToken)findPolicy(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
                
                endSuppTokMap = this.handleSupportingTokens(endSuppTokens);
                
                SupportingToken sgndEndSuppTokens 
                    = (SupportingToken)findPolicy(SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
                sgndEndSuppTokMap = this.handleSupportingTokens(sgndEndSuppTokens);
                
                SupportingToken sgndEncryptedSuppTokens 
                    = (SupportingToken)findPolicy(SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
                sgndEncSuppTokMap = this.handleSupportingTokens(sgndEncryptedSuppTokens);
                
                SupportingToken endorsingEncryptedSuppTokens 
                    = (SupportingToken)findPolicy(SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
                endEncSuppTokMap = this.handleSupportingTokens(endorsingEncryptedSuppTokens);
                
                SupportingToken sgndEndEncSuppTokens 
                    = (SupportingToken)findPolicy(SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
                sgndEndEncSuppTokMap = this.handleSupportingTokens(sgndEndEncSuppTokens);
                
                SupportingToken supportingToks 
                    = (SupportingToken)findPolicy(SP12Constants.SUPPORTING_TOKENS);
                this.handleSupportingTokens(supportingToks);
                
                SupportingToken encryptedSupportingToks 
                    = (SupportingToken)findPolicy(SP12Constants.ENCRYPTED_SUPPORTING_TOKENS);
                this.handleSupportingTokens(encryptedSupportingToks);
            
                //Setup signature parts
                addSignatureParts(sigSuppTokMap, sigs);
                addSignatureParts(sgndEncSuppTokMap, sigs);
                addSignatureParts(sgndEndSuppTokMap, sigs);
                addSignatureParts(sgndEndEncSuppTokMap, sigs);

                
                //Add timestamp
                if (timestampEl != null) {
                    Element el = timestampEl.getElement();
                    sigs.add(new WSEncryptionPart(addWsuIdToElement(el)));
                }
                doSignature(sigs);
                doEndorse();
                
            } else {
                //confirm sig
                addSignatureConfirmation(sigs);
                doSignature(sigs);
            }

            Vector<WSEncryptionPart> enc = getEncryptedParts();
            doEncyption(enc);
        } catch (Exception e) {
            e.printStackTrace();
            //REVISIT!!
        }
    }

    private void doEncryptBeforeSign() {
        // REVISIT 
        
    }
    
    
    protected void addSignatureConfirmation(Vector<WSEncryptionPart> sigParts) {
        Wss10 wss10 = getWss10();
        
        if (!(wss10 instanceof Wss11) 
            || !((Wss11)wss10).isRequireSignatureConfirmation()) {
            //If we don't require sig confirmation simply go back :-)
            return;
        }
        
        Vector results = (Vector)message.getExchange().getInMessage().get(WSHandlerConstants.RECV_RESULTS);
        /*
         * loop over all results gathered by all handlers in the chain. For each
         * handler result get the various actions. After that loop we have all
         * signature results in the signatureActions vector
         */
        Vector signatureActions = new Vector();
        for (int i = 0; i < results.size(); i++) {
            WSHandlerResult wshResult = (WSHandlerResult) results.get(i);

            WSSecurityUtil.fetchAllActionResults(wshResult.getResults(),
                    WSConstants.SIGN, signatureActions);
            WSSecurityUtil.fetchAllActionResults(wshResult.getResults(),
                    WSConstants.ST_SIGNED, signatureActions);
            WSSecurityUtil.fetchAllActionResults(wshResult.getResults(),
                    WSConstants.UT_SIGN, signatureActions);
        }
        
        // prepare a SignatureConfirmation token
        WSSecSignatureConfirmation wsc = new WSSecSignatureConfirmation();
        if (signatureActions.size() > 0) {
            for (int i = 0; i < signatureActions.size(); i++) {
                WSSecurityEngineResult wsr = (WSSecurityEngineResult) signatureActions
                        .get(i);
                byte[] sigVal = (byte[]) wsr.get(WSSecurityEngineResult.TAG_SIGNATURE_VALUE);
                wsc.setSignatureValue(sigVal);
                wsc.prepare(saaj.getSOAPPart());
                wsc.prependToHeader(secHeader);
                if (sigParts != null) {
                    sigParts.add(new WSEncryptionPart(wsc.getId()));
                }
            }
        } else {
            //No Sig value
            wsc.prepare(saaj.getSOAPPart());
            wsc.prependToHeader(secHeader);
            if (sigParts != null) {
                sigParts.add(new WSEncryptionPart(wsc.getId()));
            }
        }
    }

    
    private void doEndorse() {
        // Adding the endorsing encrypted supporting tokens to endorsing supporting tokens
        endSuppTokMap.putAll(endEncSuppTokMap);
        // Do endorsed signatures
        doEndorsedSignatures(endSuppTokMap, abinding.isTokenProtection());

        //Adding the signed endorsed encrypted tokens to signed endorsed supporting tokens
        sgndEndSuppTokMap.putAll(sgndEndEncSuppTokMap);
        // Do signed endorsing signatures
        doEndorsedSignatures(sgndEndSuppTokMap, abinding.isTokenProtection());
    }    
    
    private void doEncyption(Vector<WSEncryptionPart> encrParts) {
        //Check for signature protection
        if (abinding.isSignatureProtection() && mainSigId != null) {
            encrParts.add(new WSEncryptionPart(mainSigId, "Element"));
        }
        
        if (isRequestor()) {
            for (String id : encryptedTokensIdList) {
                encrParts.add(new WSEncryptionPart(id, "Element"));
            }
        }

        //Do encryption
        RecipientToken recToken = abinding.getRecipientToken();
        if (recToken != null && recToken.getRecipientToken() != null && encrParts.size() > 0) {
            Token encrToken = recToken.getRecipientToken();
            policyAsserted(recToken);
            policyAsserted(encrToken);
            Element refList = null;
            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();
            if (encrToken.isDerivedKeys()) {
                try {
                    WSSecDKEncrypt dkEncr = new WSSecDKEncrypt();
                    
                    if (encrKey == null) {
                        setupEncryptedKey(recToken, encrToken);
                    }
                    
                    dkEncr.setExternalKey(this.encryptedKeyValue, this.encryptedKeyId);
                    dkEncr.setCustomValueType(WSConstants.SOAPMESSAGE_NS11 + "#"
                            + WSConstants.ENC_KEY_VALUE_TYPE);
                    dkEncr.setSymmetricEncAlgorithm(algorithmSuite.getEncryption());
                    dkEncr.setDerivedKeyLength(algorithmSuite.getEncryptionDerivedKeyLength() / 8);
                    dkEncr.prepare(saaj.getSOAPPart());
                    
                    dkEncr.prependDKElementToHeader(secHeader);
                    refList = dkEncr.encryptForExternalRef(null, encrParts);
                    
                } catch (Exception e) {
                    policyNotAsserted(recToken, e);
                }
            } else {
                try {
                    WSSecEncrypt encr = new WSSecEncrypt();
                    
                    setKeyIdentifierType(encr, recToken, encrToken);
                    
                    encr.setDocument(saaj.getSOAPPart());
                    setEncryptionUser(encr, encrToken, false);
                    encr.setSymmetricEncAlgorithm(algorithmSuite.getEncryption());
                    encr.setKeyEncAlgo(algorithmSuite.getAsymmetricKeyWrap());
                    
                    encr.prepare(saaj.getSOAPPart(),
                                 getEncryptionCrypto(recToken));
                    
                    if (encr.getBSTTokenId() != null) {
                        encr.appendBSTElementToHeader(secHeader);
                    }
                    
                    
                    Element encryptedKeyElement = encr.getEncryptedKeyElement();
                                       
                    //Encrypt, get hold of the ref list and add it
                    refList = encr.encryptForInternalRef(null, encrParts);
                    
                    //Add internal refs
                    encryptedKeyElement.appendChild(refList);
                    
                    encr.prependToHeader(secHeader);

                } catch (WSSecurityException e) {
                    policyNotAsserted(recToken, e.getMessage());
                }    
            }
        }
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

                // Add elements to header
                dkSign.appendDKElementToHeader(secHeader);
                dkSign.appendSigToHeader(secHeader);
                
                mainSigId = addWsuIdToElement(dkSign.getSignatureElement());
            } catch (Exception e) {
                //REVISIT
                e.printStackTrace();
            }
        } else {
            WSSecSignature sig = getSignatureBuider(wrapper, sigToken);
            sig.prependBSTElementToHeader(secHeader);
            
            if (abinding.isTokenProtection()
                    && sig.getBSTTokenId() != null) {
                sigParts.add(new WSEncryptionPart(sig.getBSTTokenId()));
            }

            sig.addReferencesToSign(sigParts, secHeader);
            sig.computeSignature();

            sig.prependToHeader(secHeader);

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
        encrKey.prependToHeader(secHeader);
        encryptedKeyValue = encrKey.getEphemeralKey();
        encryptedKeyId = encrKey.getId();
        
        //Store the token for client - response verification 
        // and server - response creation
        message.put(WSSecEncryptedKey.class.getName(), encrKey);
    }



}
