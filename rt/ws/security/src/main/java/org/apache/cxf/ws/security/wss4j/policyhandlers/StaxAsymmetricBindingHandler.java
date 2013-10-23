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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;

/**
 * 
 */
public class StaxAsymmetricBindingHandler extends AbstractStaxBindingHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxAsymmetricBindingHandler.class);

    private AsymmetricBinding abinding;
    private SoapMessage message;
    
    public StaxAsymmetricBindingHandler(
        Map<String, Object> properties, 
        SoapMessage msg,
        Map<String, SecurityTokenProvider<OutboundSecurityToken>> outboundTokens
    ) {
        super(properties, msg, outboundTokens);
        this.message = msg;
    }
    
    public void handleBinding() {
        AssertionInfoMap aim = getMessage().get(AssertionInfoMap.class);
        configureTimestamp(aim);
        abinding = (AsymmetricBinding)getBinding(aim);
        assertPolicy(abinding.getName());
        
        String asymSignatureAlgorithm = 
            (String)getMessage().getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
        if (asymSignatureAlgorithm != null && abinding.getAlgorithmSuite() != null) {
            abinding.getAlgorithmSuite().setAsymmetricSignature(asymSignatureAlgorithm);
        }
        
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
        
        configureLayout(aim);
        assertAlgorithmSuite(abinding.getAlgorithmSuite());
        assertWSSProperties(abinding.getName().getNamespaceURI());
        assertTrustProperties(abinding.getName().getNamespaceURI());
        assertPolicy(
            new QName(abinding.getName().getNamespaceURI(), SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));
        if (abinding.isProtectTokens()) {
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.PROTECT_TOKENS));
        }
    }

    private void doSignBeforeEncrypt() {
        try {
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            if (initiatorWrapper != null) {
                assertTokenWrapper(initiatorWrapper);
                AbstractToken initiatorToken = initiatorWrapper.getToken();
                if (initiatorToken instanceof IssuedToken) {
                    SecurityToken sigTok = getSecurityToken();
                    addIssuedToken((IssuedToken)initiatorToken, sigTok, false, true);
                    if (sigTok != null) {
                        storeSecurityToken(sigTok);
                        outboundTokens.remove(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION); 
                    }
                    
                    // Set up CallbackHandler which wraps the configured Handler
                    Map<String, Object> config = getProperties();
                    TokenStoreCallbackHandler callbackHandler = 
                        new TokenStoreCallbackHandler(
                            (CallbackHandler)config.get(ConfigurationConstants.PW_CALLBACK_REF), 
                            WSS4JUtils.getTokenStore(message)
                        );
                    config.put(ConfigurationConstants.PW_CALLBACK_REF, callbackHandler);
                } else if (initiatorToken instanceof SamlToken) {
                    addSamlToken((SamlToken)initiatorToken, false, true);
                }
                assertToken(initiatorToken);
            }
            
            // Add timestamp
            List<SecurePart> sigs = new ArrayList<SecurePart>();
            if (timestampAdded) {
                SecurePart part = 
                    new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                sigs.add(part);
            }
            sigs.addAll(this.getSignedParts());
            
            if (isRequestor() && initiatorWrapper != null) {
                doSignature(initiatorWrapper, sigs);
                //doEndorse();
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
                }
                if (recipientSignatureToken != null && sigs.size() > 0) {
                    doSignature(recipientSignatureToken, sigs);
                }
            }
            
            addSupportingTokens();
            
            Map<String, Object> config = getProperties();
            if (config.containsKey(ConfigurationConstants.ACTION)) {
                String action = (String)config.get(ConfigurationConstants.ACTION);
                if (action.contains(ConfigurationConstants.SAML_TOKEN_SIGNED)
                    && action.contains(ConfigurationConstants.SIGNATURE)) {
                    String newAction = action.replaceFirst(ConfigurationConstants.SIGNATURE, "").trim();
                    config.put(ConfigurationConstants.ACTION, newAction);
                }
            } 
            
            List<SecurePart> enc = getEncryptedParts();
            
            //Check for signature protection
            if (abinding.isEncryptSignature()) {
                SecurePart part = 
                    new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                enc.add(part);
                if (signatureConfirmationAdded) {
                    SecurePart securePart = 
                        new SecurePart(WSSConstants.TAG_wsse11_SignatureConfirmation, Modifier.Element);
                    enc.add(securePart);
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
            if (encToken != null) {
                assertTokenWrapper(encToken);
                assertToken(encToken.getToken());
            }
            doEncryption(encToken, enc, false);
            
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Sign before encryption failed due to : " + reason);
            throw new Fault(e);
        }
    }

    private void doEncryptBeforeSign() {
        try {
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
            assertTokenWrapper(wrapper);
            encryptionToken = wrapper.getToken();
            assertToken(encryptionToken);
            
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            
            if (initiatorWrapper != null) {
                assertTokenWrapper(initiatorWrapper);
                AbstractToken initiatorToken = initiatorWrapper.getToken();
                if (initiatorToken instanceof IssuedToken) {
                    SecurityToken sigTok = getSecurityToken();
                    addIssuedToken((IssuedToken)initiatorToken, sigTok, false, true);
                    if (sigTok != null) {
                        storeSecurityToken(sigTok);
                        outboundTokens.remove(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION); 
                    }
                    
                    // Set up CallbackHandler which wraps the configured Handler
                    Map<String, Object> config = getProperties();
                    TokenStoreCallbackHandler callbackHandler = 
                        new TokenStoreCallbackHandler(
                            (CallbackHandler)config.get(ConfigurationConstants.PW_CALLBACK_REF), 
                            WSS4JUtils.getTokenStore(message)
                        );
                    config.put(ConfigurationConstants.PW_CALLBACK_REF, callbackHandler);
                } else if (initiatorToken instanceof SamlToken) {
                    addSamlToken((SamlToken)initiatorToken, false, true);
                }
            }
            
            List<SecurePart> encrParts = null;
            List<SecurePart> sigParts = null;
            try {
                encrParts = getEncryptedParts();
                //Signed parts are determined before encryption because encrypted signed headers
                //will not be included otherwise
                sigParts = getSignedParts();
            } catch (SOAPException ex) {
                throw new Fault(ex);
            }
            
            if (encryptionToken != null && encrParts.size() > 0) {
                if (isRequestor()) {
                    addSupportingTokens();
                    encrParts.addAll(encryptedTokensList);
                } else {
                    addSignatureConfirmation(sigParts);
                }
                
                //Check for signature protection
                if (abinding.isEncryptSignature()) {
                    SecurePart part = 
                        new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                    encrParts.add(part);
                    if (signatureConfirmationAdded) {
                        SecurePart securePart = 
                            new SecurePart(WSSConstants.TAG_wsse11_SignatureConfirmation, Modifier.Element);
                        encrParts.add(securePart);
                    }
                    assertPolicy(
                        new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));
                }
                
                doEncryption(wrapper, encrParts, true);
            }
            
            if (sigParts.size() > 0) {
                if (timestampAdded) {
                    SecurePart part = 
                        new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                    sigParts.add(part);
                }
                
                if ((sigParts.size() > 0) && initiatorWrapper != null && isRequestor()) {
                    doSignature(initiatorWrapper, sigParts);
                } else if (!isRequestor()) {
                    AbstractTokenWrapper recipientSignatureToken = abinding.getRecipientSignatureToken();
                    if (recipientSignatureToken == null) {
                        recipientSignatureToken = abinding.getRecipientToken(); 
                    }
                    if (recipientSignatureToken != null) {
                        assertTokenWrapper(recipientSignatureToken);
                        assertToken(recipientSignatureToken.getToken());
                        doSignature(recipientSignatureToken, sigParts);
                    }
                }
    
                //if (isRequestor()) {
                //    doEndorse();
                //}
            }
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Encrypt before signing failed due to : " + reason);
            throw new Fault(e);
        }
    }

    private void doEncryption(AbstractTokenWrapper recToken,
                                    List<SecurePart> encrParts,
                                    boolean externalRef) throws SOAPException {
        //Do encryption
        if (recToken != null && recToken.getToken() != null && encrParts.size() > 0) {
            AbstractToken encrToken = recToken.getToken();
            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();
            
            // Action
            Map<String, Object> config = getProperties();
            String actionToPerform = ConfigurationConstants.ENCRYPT;
            if (recToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                actionToPerform = ConfigurationConstants.ENCRYPT_DERIVED;
            }
            
            if (config.containsKey(ConfigurationConstants.ACTION)) {
                String action = (String)config.get(ConfigurationConstants.ACTION);
                config.put(ConfigurationConstants.ACTION, action + " " + actionToPerform);
            } else {
                config.put(ConfigurationConstants.ACTION, actionToPerform);
            }
            
            String parts = "";
            if (config.containsKey(ConfigurationConstants.ENCRYPTION_PARTS)) {
                parts = (String)config.get(ConfigurationConstants.ENCRYPTION_PARTS);
                if (!parts.endsWith(";")) {
                    parts += ";";
                }
            }
            
            for (SecurePart part : encrParts) {
                QName name = part.getName();
                parts += "{" + part.getModifier() + "}{"
                    +  name.getNamespaceURI() + "}" + name.getLocalPart() + ";";
            }
            
            config.put(ConfigurationConstants.ENCRYPTION_PARTS, parts);
    
            config.put(ConfigurationConstants.ENC_KEY_ID, 
                       getKeyIdentifierType(recToken, encrToken));

            config.put(ConfigurationConstants.ENC_KEY_TRANSPORT, 
                       algorithmSuite.getAlgorithmSuiteType().getAsymmetricKeyWrap());
            config.put(ConfigurationConstants.ENC_SYM_ALGO, 
                       algorithmSuite.getAlgorithmSuiteType().getEncryption());

            String encUser = (String)message.getContextualProperty(SecurityConstants.ENCRYPT_USERNAME);
            if (encUser != null) {
                config.put(ConfigurationConstants.ENCRYPTION_USER, encUser);
            }
            
            //
            // Using a stored cert is only suitable for the Issued Token case, where
            // we're extracting the cert from a SAML Assertion on the provider side
            //
            if (!isRequestor() && recToken.getToken() instanceof IssuedToken) {
                config.put(ConfigurationConstants.ENCRYPTION_USER, ConfigurationConstants.USE_REQ_SIG_CERT);
            }
        }
    }
    
    private void doSignature(AbstractTokenWrapper wrapper, List<SecurePart> sigParts) 
        throws WSSecurityException, SOAPException {
        
        // Action
        Map<String, Object> config = getProperties();
        String actionToPerform = ConfigurationConstants.SIGNATURE;
        if (wrapper.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            actionToPerform = ConfigurationConstants.SIGNATURE_DERIVED;
        }
        
        if (config.containsKey(ConfigurationConstants.ACTION)) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            config.put(ConfigurationConstants.ACTION, action + " " + actionToPerform);
        } else {
            config.put(ConfigurationConstants.ACTION, actionToPerform);
        }
        
        String parts = "";
        if (config.containsKey(ConfigurationConstants.SIGNATURE_PARTS)) {
            parts = (String)config.get(ConfigurationConstants.SIGNATURE_PARTS);
            if (!parts.endsWith(";")) {
                parts += ";";
            }
        }
        
        String optionalParts = "";
        if (config.containsKey(ConfigurationConstants.OPTIONAL_SIGNATURE_PARTS)) {
            optionalParts = (String)config.get(ConfigurationConstants.OPTIONAL_SIGNATURE_PARTS);
            if (!optionalParts.endsWith(";")) {
                optionalParts += ";";
            }
        }
        
        for (SecurePart part : sigParts) {
            QName name = part.getName();
            if (part.isRequired()) {
                parts += "{Element}{" +  name.getNamespaceURI() + "}" + name.getLocalPart() + ";";
            } else {
                optionalParts += "{Element}{" +  name.getNamespaceURI() + "}" + name.getLocalPart() + ";";
            }
        }
        
        AbstractToken sigToken = wrapper.getToken();
        configureSignature(wrapper, sigToken, false);
        
        if (abinding.isProtectTokens() && (sigToken instanceof X509Token)
            && sigToken.getIncludeTokenType() != IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            parts += "{Element}{" + WSSConstants.NS_WSSE10 + "}BinarySecurityToken;";
        } else if (sigToken instanceof IssuedToken || sigToken instanceof SecurityContextToken
            || sigToken instanceof SecureConversationToken || sigToken instanceof SpnegoContextToken
            || sigToken instanceof SamlToken) {
            config.put(ConfigurationConstants.INCLUDE_SIGNATURE_TOKEN, "false");
        }
        
        config.put(ConfigurationConstants.SIGNATURE_PARTS, parts);
        config.put(ConfigurationConstants.OPTIONAL_SIGNATURE_PARTS, optionalParts);
        
        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            config.put(ConfigurationConstants.SIG_ALGO, 
                   abinding.getAlgorithmSuite().getSymmetricSignature());
        }
    }

}
