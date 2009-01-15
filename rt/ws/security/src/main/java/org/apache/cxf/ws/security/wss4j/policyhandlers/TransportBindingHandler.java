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

import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.IssuedToken;
import org.apache.cxf.ws.security.policy.model.KeyValueToken;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.TokenWrapper;
import org.apache.cxf.ws.security.policy.model.TransportBinding;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.message.WSSecDKSign;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.apache.ws.security.message.WSSecUsernameToken;

/**
 * 
 */
public class TransportBindingHandler extends AbstractBindingBuilder {
    TransportBinding tbinding;
    
    public TransportBindingHandler(TransportBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) {
        super(binding, saaj, secHeader, aim, message);
        this.tbinding = binding;
    }
    
    private void addUsernameTokens(SupportingToken sgndSuppTokens) {
        for (Token token : sgndSuppTokens.getTokens()) {
            if (token instanceof UsernameToken) {
                WSSecUsernameToken utBuilder = 
                    addUsernameToken((UsernameToken)token);
                if (utBuilder != null) {
                    utBuilder.prepare(saaj.getSOAPPart());
                    utBuilder.appendToHeader(secHeader);
                }
            } else if (token instanceof IssuedToken) {
                SecurityToken secTok = getSecurityToken();
                
                SPConstants.IncludeTokenType inclusion = token.getInclusion();
                
                if (inclusion == SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS
                    || ((inclusion == SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT 
                        || inclusion == SPConstants.IncludeTokenType.INCLUDE_TOKEN_ONCE) 
                        && isRequestor())) {
                  
                    //Add the token
                    addEncyptedKeyElement(cloneElement(secTok.getToken()));
                }
            } else {
                //REVISIT - not supported for signed.  Exception?
            }
        }
        
    }
    
    public void handleBinding() {
        Collection<AssertionInfo> ais;
        WSSecTimestamp timestamp = createTimestamp();
        handleLayout(timestamp);
        try {
            if (this.isRequestor()) {
                Vector<byte[]> signatureValues = new Vector<byte[]>();

                ais = aim.get(SP12Constants.SIGNED_SUPPORTING_TOKENS);
                if (ais != null) {
                    SupportingToken sgndSuppTokens = null;
                    for (AssertionInfo ai : ais) {
                        sgndSuppTokens = (SupportingToken)ai.getAssertion();
                        ai.setAsserted(true);
                    }
                    if (sgndSuppTokens != null) {
                        addUsernameTokens(sgndSuppTokens);
                    }
                }
                ais = aim.get(SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
                if (ais != null) {
                    SupportingToken sgndSuppTokens = null;
                    for (AssertionInfo ai : ais) {
                        sgndSuppTokens = (SupportingToken)ai.getAssertion();
                        ai.setAsserted(true);
                    }
                    if (sgndSuppTokens != null) {
                        SignedEncryptedParts signdParts = sgndSuppTokens.getSignedParts();

                        for (Token token : sgndSuppTokens.getTokens()) {
                            if (token instanceof IssuedToken) {
                                signatureValues.add(doIssuedTokenSignature(token, signdParts,
                                                                           sgndSuppTokens));
                            } else if (token instanceof X509Token) {
                                signatureValues.add(doX509TokenSignature(token, signdParts, sgndSuppTokens));
                            }
                        }
                    }
                }
                ais = aim.get(SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
                if (ais != null) {
                    SupportingToken sgndSuppTokens = null;
                    for (AssertionInfo ai : ais) {
                        sgndSuppTokens = (SupportingToken)ai.getAssertion();
                        ai.setAsserted(true);
                    }
                    if (sgndSuppTokens != null) {
                        addUsernameTokens(sgndSuppTokens);
                    }
                }
                
                ais = aim.get(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
                if (ais != null) {
                    SupportingToken sgndSuppTokens = null;
                    for (AssertionInfo ai : ais) {
                        sgndSuppTokens = (SupportingToken)ai.getAssertion();
                        ai.setAsserted(true);
                    } 
                    
                    if (sgndSuppTokens != null) {
                        for (Token token : sgndSuppTokens.getTokens()) {
                            if (token instanceof IssuedToken) {
                                signatureValues.add(doIssuedTokenSignature(token, null, 
                                                                           sgndSuppTokens));
                            } else if (token instanceof X509Token) {
                                signatureValues.add(doX509TokenSignature(token, 
                                                                         sgndSuppTokens.getSignedParts(), 
                                                                         sgndSuppTokens));
                            } else if (token instanceof SecureConversationToken) {
                                signatureValues.add(doSecureConversationSignature(token,
                                                                                  null));
                            } else if (token instanceof KeyValueToken) {
                                //
                            }
                        }
                    }
                    
                }
                
                ais = aim.get(SP12Constants.SUPPORTING_TOKENS);
                if (ais != null) {
                    SupportingToken suppTokens = null;
                    for (AssertionInfo ai : ais) {
                        suppTokens = (SupportingToken)ai.getAssertion();
                        ai.setAsserted(true);
                    }
                    if (suppTokens != null && suppTokens.getTokens() != null 
                        && suppTokens.getTokens().size() > 0) {
                        handleSupportingTokens(suppTokens, false);
                    }
                }

            } else {
                addSignatureConfirmation(null);
            }
        } catch (Exception e) {
            throw new Fault(e);
        }
    }
    
    
    private byte[] doX509TokenSignature(Token token, SignedEncryptedParts signdParts,
                                        TokenWrapper wrapper) 
        throws Exception {
        
        Document doc = saaj.getSOAPPart();
        
        Vector<WSEncryptionPart> sigParts = new Vector<WSEncryptionPart>();
        
        if (timestampEl != null) {
            sigParts.add(new WSEncryptionPart(timestampEl.getId()));                          
        }
        
        if (signdParts != null) {
            if (signdParts.isBody()) {
                sigParts.add(new WSEncryptionPart(addWsuIdToElement(saaj.getSOAPBody())));
            }
            for (Header header : signdParts.getHeaders()) {
                WSEncryptionPart wep = new WSEncryptionPart(header.getName(), 
                        header.getNamespace(),
                        "Content");
                sigParts.add(wep);
            }
        }
        if (token.isDerivedKeys()) {
            WSSecEncryptedKey encrKey = getEncryptedKeyBuilder(wrapper, token);
            
            Element bstElem = encrKey.getBinarySecurityTokenElement();
            if (bstElem != null) {
                addTopDownElement(bstElem);
            }
            encrKey.appendToHeader(secHeader);
            
            WSSecDKSign dkSig = new WSSecDKSign();
            
            dkSig.setSigCanonicalization(binding.getAlgorithmSuite().getInclusiveC14n());
            dkSig.setSignatureAlgorithm(binding.getAlgorithmSuite().getSymmetricSignature());
            dkSig.setDerivedKeyLength(binding.getAlgorithmSuite().getSignatureDerivedKeyLength() / 8);
            
            dkSig.setExternalKey(encrKey.getEphemeralKey(), encrKey.getId());
            
            dkSig.prepare(doc, secHeader);
            
            /*
            if(binding.isTokenProtection()) {
                sigParts.add(new WSEncryptionPart(encrKey.getBSTTokenId()));
            }
            */
            
            dkSig.setParts(sigParts);
            dkSig.addReferencesToSign(sigParts, secHeader);
            
            //Do signature
            dkSig.computeSignature();
            
            dkSig.appendDKElementToHeader(secHeader);
            dkSig.appendSigToHeader(secHeader);
            
            return dkSig.getSignatureValue();
        } else {
            WSSecSignature sig = getSignatureBuider(wrapper, token, false);
            sig.prependBSTElementToHeader(secHeader);
            /*
            if (isTokenProtection()
                && !(SPConstants.IncludeTokenType.INCLUDE_TOKEN_NEVER == token.getInclusion())) {
                sigParts.add(new WSEncryptionPart(sig.getBSTTokenId()));
            }
            */
            
            sig.addReferencesToSign(sigParts, secHeader);
            insertBeforeBottomUp(sig.getSignatureElement());
            
            sig.computeSignature();
            
            return sig.getSignatureValue();    
        }
    }

    private byte[] doIssuedTokenSignature(Token token, SignedEncryptedParts signdParts,
                                          TokenWrapper wrapper) throws Exception {
        Document doc = saaj.getSOAPPart();
        
        //Get the issued token
        SecurityToken secTok = getSecurityToken();
   
        SPConstants.IncludeTokenType inclusion = token.getInclusion();
        boolean tokenIncluded = false;
        
        if (inclusion == SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS
            || ((inclusion == SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT 
                || inclusion == SPConstants.IncludeTokenType.INCLUDE_TOKEN_ONCE) 
                && isRequestor())) {
          
            //Add the token
            addEncyptedKeyElement(cloneElement(secTok.getToken()));
            tokenIncluded = true;
        }

        Vector<WSEncryptionPart> sigParts = new Vector<WSEncryptionPart>();
        
        if (timestampEl != null) {
            sigParts.add(new WSEncryptionPart(timestampEl.getId()));                          
        }
        
        /*
        if (tbinding.isTokenProtection() && tokenIncluded) {
            sigParts.add(new WSEncryptionPart(secTok.getId()));
        }
        */
        
        if (signdParts != null) {
            if (signdParts.isBody()) {
                sigParts.add(new WSEncryptionPart(addWsuIdToElement(saaj.getSOAPBody())));
            }
    
            for (Header header : signdParts.getHeaders()) {
                WSEncryptionPart wep = new WSEncryptionPart(header.getName(), 
                        header.getNamespace(),
                        "Content");
                sigParts.add(wep);
            }
        }
        
        //check for derived keys
        AlgorithmSuite algorithmSuite = tbinding.getAlgorithmSuite();
        if (token.isDerivedKeys()) {
            //Do Signature with derived keys
            WSSecDKSign dkSign = new WSSecDKSign();
          
            //Setting the AttachedReference or the UnattachedReference according to the flag
            Element ref;
            if (tokenIncluded) {
                ref = secTok.getAttachedReference();
            } else {
                ref = secTok.getUnattachedReference();
            }
          
            if (ref != null) {
                dkSign.setExternalKey(secTok.getSecret(), cloneElement(ref));
            } else {
                dkSign.setExternalKey(secTok.getSecret(), secTok.getId());
            }
          
            //    Set the algo info
            dkSign.setSignatureAlgorithm(algorithmSuite.getSymmetricSignature());
            dkSign.setDerivedKeyLength(algorithmSuite.getSignatureDerivedKeyLength() / 8);
            if (token.getSPConstants() == SP12Constants.INSTANCE) {
                dkSign.setWscVersion(ConversationConstants.VERSION_05_12);
            }
            dkSign.prepare(doc, secHeader);
          
            addDerivedKeyElement(dkSign.getdktElement());
          
            dkSign.setParts(sigParts);
            dkSign.addReferencesToSign(sigParts, secHeader);
          
            //Do signature
            dkSign.computeSignature();
          
            dkSign.appendSigToHeader(secHeader);
          
            return dkSign.getSignatureValue();
        } else {
            WSSecSignature sig = new WSSecSignature();
            sig.setCustomTokenId(secTok.getId().substring(1));
            sig.setCustomTokenValueType(WSConstants.WSS_SAML_NS
                                        + WSConstants.SAML_ASSERTION_ID);
            sig.setSecretKey(secTok.getSecret());
            sig.setSignatureAlgorithm(algorithmSuite.getAsymmetricSignature());
            sig.setSignatureAlgorithm(algorithmSuite.getSymmetricSignature());
            sig.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING);
            sig.prepare(doc, getSignatureCrypto(wrapper), secHeader);

            sig.setParts(sigParts);
            sig.addReferencesToSign(sigParts, secHeader);

            //Do signature
            sig.computeSignature();

            //Add elements to header
            insertBeforeBottomUp(sig.getSignatureElement());

            return sig.getSignatureValue();
        }
    }
    
    private byte[] doSecureConversationSignature(Token token, 
                                                 SignedEncryptedParts signdParts) 
        throws Exception {
        return null;
    }


}
