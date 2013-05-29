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

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;

/**
 * 
 */
public class StaxAsymmetricBindingHandler extends AbstractStaxBindingHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxAsymmetricBindingHandler.class);

    private AsymmetricBinding abinding;
    private SoapMessage message;
    
    public StaxAsymmetricBindingHandler(Map<String, Object> properties, SoapMessage msg) {
        super(properties, msg);
        this.message = msg;
    }
    
    public void handleBinding() {
        AssertionInfoMap aim = getMessage().get(AssertionInfoMap.class);
        configureTimestamp(aim);
        configureLayout(aim);
        abinding = (AsymmetricBinding)getBinding(aim);
        
        if (abinding.getProtectionOrder() 
            == AbstractSymmetricAsymmetricBinding.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
        } else {
            doSignBeforeEncrypt();
        }
    }

    private void doSignBeforeEncrypt() {
        try {
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            boolean attached = false;
            /*
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
            */
            
            // Add timestamp
            List<SecurePart> sigs = new ArrayList<SecurePart>();
            if (timestampAdded) {
                SecurePart part = 
                    new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                sigs.add(part);
            }
            addSupportingTokens();
            
            if (isRequestor() && initiatorWrapper != null) {
                doSignature(initiatorWrapper, sigs, attached);
                //doEndorse();
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
            
            List<SecurePart> enc = getEncryptedParts();
            
            //Check for signature protection
            if (abinding.isEncryptSignature()) {
                SecurePart part = 
                    new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                enc.add(part);
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
            encryptionToken = wrapper.getToken();
            
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            
            boolean attached = false;
            /*
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
                        LOG.log(Level.FINE, "Encrypt before sign failed due to : " + reason);
                        throw new Fault(e);
                    }
                }
            }
            */
            
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
                //Check for signature protection
                if (abinding.isEncryptSignature()) {
                    SecurePart part = 
                        new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                    encrParts.add(part);
                }
                
                doEncryption(wrapper, encrParts, true);
                if (timestampAdded) {
                    SecurePart part = 
                        new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                    sigParts.add(part);
                }
                
                if (isRequestor()) {
                    addSupportingTokens();
                } else {
                    addSignatureConfirmation(sigParts);
                }
                
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
            
            encrParts.addAll(this.getEncryptedParts());
            
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
        }
    }
    
    private void doSignature(AbstractTokenWrapper wrapper, List<SecurePart> sigParts, boolean attached) 
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
        
        sigParts.addAll(this.getSignedParts());
        
        for (SecurePart part : sigParts) {
            QName name = part.getName();
            parts += "{Element}{" +  name.getNamespaceURI() + "}" + name.getLocalPart() + ";";
        }
        
        AbstractToken sigToken = wrapper.getToken();
        if (abinding.isProtectTokens() && (sigToken instanceof X509Token)
            && sigToken.getIncludeTokenType() != IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            parts += "{Element}{" + WSSConstants.NS_WSSE10 + "}BinarySecurityToken;";
        }
        
        config.put(ConfigurationConstants.SIGNATURE_PARTS, parts);
        
        configureSignature(wrapper, sigToken, false);
        
        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            config.put(ConfigurationConstants.SIG_ALGO, 
                   abinding.getAlgorithmSuite().getSymmetricSignature());
        }
    }

}
