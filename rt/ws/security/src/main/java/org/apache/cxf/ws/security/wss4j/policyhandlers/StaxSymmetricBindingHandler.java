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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;
import org.apache.xml.security.stax.impl.securityToken.GenericOutboundSecurityToken;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.stax.securityEvent.AbstractSecuredElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants.TokenType;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;
import org.apache.xml.security.utils.Base64;

/**
 * 
 */
public class StaxSymmetricBindingHandler extends AbstractStaxBindingHandler {
    
    private SymmetricBinding sbinding;
    private SoapMessage message;
    
    public StaxSymmetricBindingHandler(
        Map<String, Object> properties, 
        SoapMessage msg,
        Map<String, SecurityTokenProvider<OutboundSecurityToken>> outboundTokens
    ) {
        super(properties, msg, outboundTokens);
        this.message = msg;
    }
    
    private AbstractTokenWrapper getSignatureToken() {
        if (sbinding.getProtectionToken() != null) {
            return sbinding.getProtectionToken();
        }
        return sbinding.getSignatureToken();
    }
    
    private AbstractTokenWrapper getEncryptionToken() {
        if (sbinding.getProtectionToken() != null) {
            return sbinding.getProtectionToken();
        }
        return sbinding.getEncryptionToken();
    }
    
    public void handleBinding() {
        AssertionInfoMap aim = getMessage().get(AssertionInfoMap.class);
        configureTimestamp(aim);
        sbinding = (SymmetricBinding)getBinding(aim);
        
        // Set up CallbackHandler which wraps the configured Handler
        Map<String, Object> config = getProperties();
        TokenStoreCallbackHandler callbackHandler = 
            new TokenStoreCallbackHandler(
                (CallbackHandler)config.get(ConfigurationConstants.PW_CALLBACK_REF), getTokenStore()
            );
        config.put(ConfigurationConstants.PW_CALLBACK_REF, callbackHandler);
        
        if (sbinding.getProtectionOrder() 
            == AbstractSymmetricAsymmetricBinding.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
        } else {
            doSignBeforeEncrypt();
        }
        
        if (!isRequestor()) {
            config.put(ConfigurationConstants.ENC_SYM_ENC_KEY, "false");
        }
        
        configureLayout(aim);
    }
    
    private void doEncryptBeforeSign() {
        try {
            AbstractTokenWrapper encryptionWrapper = getEncryptionToken();
            AbstractToken encryptionToken = encryptionWrapper.getToken();

            //The encryption token can be an IssuedToken or a 
            //SecureConversationToken
            String tokenId = null;
            SecurityToken tok = null;
            if (encryptionToken instanceof KerberosToken) {
                tok = getSecurityToken();
                addKerberosToken((KerberosToken)encryptionToken, false, false);
            } else if (encryptionToken instanceof IssuedToken 
                || encryptionToken instanceof SecureConversationToken
                || encryptionToken instanceof SecurityContextToken
                || encryptionToken instanceof SpnegoContextToken) {
                tok = getSecurityToken();
            } else if (encryptionToken instanceof X509Token) {
                if (isRequestor()) {
                    tokenId = setupEncryptedKey(encryptionWrapper, encryptionToken);
                } else {
                    tokenId = getEncryptedKey();
                }
            } else if (encryptionToken instanceof UsernameToken) {
                policyNotAsserted(sbinding, "UsernameTokens not supported with Symmetric binding");
                return;
            }
            if (tok == null) {
                if (tokenId != null && tokenId.startsWith("#")) {
                    tokenId = tokenId.substring(1);
                }

                // Get hold of the token from the token storage
                tok = getTokenStore().getToken(tokenId);
            }
            
            // Store key
            storeSecurityToken(tok);
            
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
                if (sbinding.isEncryptSignature()) {
                    SecurePart part = 
                        new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                    encrParts.add(part);
                }
                
                doEncryption(encryptionWrapper, encrParts, true);
                if (timestampAdded) {
                    SecurePart part = 
                        new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                    sigParts.add(part);
                }
                sigParts.addAll(this.getSignedParts());
                
                AbstractTokenWrapper sigAbstractTokenWrapper = getSignatureToken();
                AbstractToken sigToken = sigAbstractTokenWrapper.getToken();
                if ((sigParts.size() > 0) && sigAbstractTokenWrapper != null && isRequestor()) {
                    doSignature(sigAbstractTokenWrapper, sigToken, tok, sigParts);
                } else if (!isRequestor()) {
                    addSignatureConfirmation(sigParts);
                    if (!sigParts.isEmpty()) {
                        doSignature(sigAbstractTokenWrapper, sigToken, tok, sigParts);
                    }
                }
    
                //if (isRequestor()) {
                //    doEndorse();
                //}
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new Fault(ex);
        }
    }
    
    private void doSignBeforeEncrypt() {
        AbstractTokenWrapper sigAbstractTokenWrapper = getSignatureToken();
        AbstractToken sigToken = sigAbstractTokenWrapper.getToken();
        String sigTokId = null;
        
        try {
            SecurityToken sigTok = null;
            if (sigToken != null) {
                if (sigToken instanceof KerberosToken) {
                    sigTok = getSecurityToken();
                    addKerberosToken((KerberosToken)sigToken, false, false);
                } else if (sigToken instanceof SecureConversationToken
                    || sigToken instanceof SecurityContextToken
                    || sigToken instanceof IssuedToken 
                    || sigToken instanceof SpnegoContextToken) {
                    sigTok = getSecurityToken();
                } else if (sigToken instanceof X509Token) {
                    if (isRequestor()) {
                        sigTokId = setupEncryptedKey(sigAbstractTokenWrapper, sigToken);
                    } else {
                        sigTokId = getEncryptedKey();
                    }
                } else if (sigToken instanceof UsernameToken) {
                    policyNotAsserted(sbinding, "UsernameTokens not supported with Symmetric binding");
                    return;
                }
            } else {
                policyNotAsserted(sbinding, "No signature token");
                return;
            }
            
            if (sigTok == null && StringUtils.isEmpty(sigTokId)) {
                policyNotAsserted(sigAbstractTokenWrapper, "No signature token id");
                return;
            }
            if (sigTok == null) {
                sigTok = getTokenStore().getToken(sigTokId);
            }
            
            // Store key
            storeSecurityToken(sigTok);

            // Add timestamp
            List<SecurePart> sigs = new ArrayList<SecurePart>();
            if (timestampAdded) {
                SecurePart part = 
                    new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                sigs.add(part);
            }
            sigs.addAll(this.getSignedParts());

            if (isRequestor()) {
                addSupportingTokens();
                if (!sigs.isEmpty()) {
                    doSignature(sigAbstractTokenWrapper, sigToken, sigTok, sigs);
                }
                // doEndorse();
            } else {
                addSignatureConfirmation(sigs);
                if (!sigs.isEmpty()) {
                    doSignature(sigAbstractTokenWrapper, sigToken, sigTok, sigs);
                }
            }

            //Encryption
            List<SecurePart> enc = getEncryptedParts();
            
            //Check for signature protection
            if (sbinding.isEncryptSignature()) {
                SecurePart part = 
                    new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                enc.add(part);
            }
            
            //Do encryption
            if (isRequestor()) {
                enc.addAll(encryptedTokensList);
            }
            AbstractTokenWrapper encrAbstractTokenWrapper = getEncryptionToken();
            doEncryption(encrAbstractTokenWrapper, enc, false);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }
    
    private void doEncryption(AbstractTokenWrapper recToken,
                              List<SecurePart> encrParts,
                              boolean externalRef) throws SOAPException {
        //Do encryption
        if (recToken != null && recToken.getToken() != null && encrParts.size() > 0) {
            AbstractToken encrToken = recToken.getToken();
            AlgorithmSuite algorithmSuite = sbinding.getAlgorithmSuite();

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

            if (isRequestor()) {
                config.put(ConfigurationConstants.ENC_KEY_ID, 
                       getKeyIdentifierType(recToken, encrToken));
            } else if (recToken.getToken() instanceof KerberosToken && !isRequestor()) {
                config.put(ConfigurationConstants.ENC_KEY_ID, "KerberosSHA1");
            } else {
                config.put(ConfigurationConstants.ENC_KEY_ID, "EncryptedKeySHA1");
            }

            config.put(ConfigurationConstants.ENC_KEY_TRANSPORT, 
                       algorithmSuite.getAlgorithmSuiteType().getAsymmetricKeyWrap());
            config.put(ConfigurationConstants.ENC_SYM_ALGO, 
                       algorithmSuite.getAlgorithmSuiteType().getEncryption());

            String encUser = (String)message.getContextualProperty(SecurityConstants.ENCRYPT_USERNAME);
            if (encUser != null) {
                config.put(ConfigurationConstants.ENCRYPTION_USER, encUser);
            }
            
            if (encrToken instanceof KerberosToken) {
                config.put(ConfigurationConstants.ENC_SYM_ENC_KEY, "false");
            }
        }
    }
    
    private void doSignature(AbstractTokenWrapper wrapper, AbstractToken policyToken, 
                             SecurityToken tok, List<SecurePart> sigParts) 
        throws WSSecurityException, SOAPException {
        
        // Action
        Map<String, Object> config = getProperties();
        String actionToPerform = ConfigurationConstants.SIGNATURE;
        if (wrapper.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            actionToPerform = ConfigurationConstants.SIGNATURE_DERIVED;
        }
        
        if (config.containsKey(ConfigurationConstants.ACTION)) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            if (!action.contains(ConfigurationConstants.SAML_TOKEN_SIGNED)) {
                config.put(ConfigurationConstants.ACTION, action + " " + actionToPerform);
            }
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
        if (sbinding.isProtectTokens() && (sigToken instanceof X509Token) && isRequestor()) {
            parts += "{Element}{" + WSSConstants.NS_XMLENC + "}EncryptedKey;";
        }
        
        config.put(ConfigurationConstants.SIGNATURE_PARTS, parts);
        config.put(ConfigurationConstants.OPTIONAL_SIGNATURE_PARTS, optionalParts);
        
        configureSignature(wrapper, sigToken, false);
        
        if (policyToken instanceof X509Token) {
            config.put(ConfigurationConstants.INCLUDE_SIGNATURE_TOKEN, "false");
            if (isRequestor()) {
                config.put(ConfigurationConstants.SIG_KEY_ID, "EncryptedKey");
            } else {
                config.put(ConfigurationConstants.SIG_KEY_ID, "EncryptedKeySHA1");
            }
        } else if (policyToken instanceof KerberosToken && !isRequestor()) {
            config.put(ConfigurationConstants.SIG_KEY_ID, "KerberosSHA1");
        }
        
        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            config.put(ConfigurationConstants.SIG_ALGO, 
                   sbinding.getAlgorithmSuite().getSymmetricSignature());
        }
    }

    private String setupEncryptedKey(AbstractTokenWrapper wrapper, AbstractToken sigToken) throws WSSecurityException {
        
        Date created = new Date();
        Date expires = new Date();
        expires.setTime(created.getTime() + 300000);
        SecurityToken tempTok = 
            new SecurityToken(IDGenerator.generateID(null), created, expires);
        
        KeyGenerator keyGenerator = 
            getKeyGenerator(sbinding.getAlgorithmSuite().getAlgorithmSuiteType().getEncryption());
        SecretKey symmetricKey = keyGenerator.generateKey();
        tempTok.setKey(symmetricKey);
        tempTok.setSecret(symmetricKey.getEncoded());
        
        getTokenStore().add(tempTok);
        
        return tempTok.getId();
    }
    
    private String getEncryptedKey() throws XMLSecurityException {
        org.apache.xml.security.stax.securityToken.SecurityToken securityToken = 
            findEncryptedKeyToken();
        if (securityToken != null) {
            Date created = new Date();
            Date expires = new Date();
            expires.setTime(created.getTime() + 300000);

            String encryptedKeyID = securityToken.getId();
            SecurityToken tempTok = new SecurityToken(encryptedKeyID, created, expires);
            tempTok.setSHA1(securityToken.getSha1Identifier());
            
            for (String key : securityToken.getSecretKey().keySet()) {
                if (securityToken.getSecretKey().get(key) != null) {
                    tempTok.setKey(securityToken.getSecretKey().get(key));
                    tempTok.setSecret(securityToken.getSecretKey().get(key).getEncoded());
                    break;
                }
            }
            getTokenStore().add(tempTok);

            return encryptedKeyID;
        }
        return null;
        
    }
    
    private org.apache.xml.security.stax.securityToken.SecurityToken 
    findEncryptedKeyToken() throws XMLSecurityException {
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingEventList = 
            (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".in");
        if (incomingEventList != null) {
            for (SecurityEvent incomingEvent : incomingEventList) {
                if (WSSecurityEventConstants.EncryptedPart == incomingEvent.getSecurityEventType()
                    || WSSecurityEventConstants.EncryptedElement 
                        == incomingEvent.getSecurityEventType()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken token = 
                        ((AbstractSecuredElementSecurityEvent)incomingEvent).getSecurityToken();
                    if (token != null && token.getSecretKey() != null 
                        && token.getSha1Identifier() != null) {
                        return token;
                    }
                }
            }
        }
        return null;
    }
    
    private String getSHA1(byte[] input) {
        try {
            byte[] digestBytes = WSSecurityUtil.generateDigest(input);
            return Base64.encode(digestBytes);
        } catch (WSSecurityException e) {
            //REVISIT
        }
        return null;
    }
    
    private KeyGenerator getKeyGenerator(String symEncAlgo) throws WSSecurityException {
        try {
            //
            // Assume AES as default, so initialize it
            //
            WSSConfig.init();
            String keyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(symEncAlgo);
            if (keyAlgorithm == null || "".equals(keyAlgorithm)) {
                keyAlgorithm = JCEMapper.translateURItoJCEID(symEncAlgo);
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(keyAlgorithm);
            if (symEncAlgo.equalsIgnoreCase(WSConstants.AES_128)
                || symEncAlgo.equalsIgnoreCase(WSConstants.AES_128_GCM)) {
                keyGen.init(128);
            } else if (symEncAlgo.equalsIgnoreCase(WSConstants.AES_192)
                || symEncAlgo.equalsIgnoreCase(WSConstants.AES_192_GCM)) {
                keyGen.init(192);
            } else if (symEncAlgo.equalsIgnoreCase(WSConstants.AES_256)
                || symEncAlgo.equalsIgnoreCase(WSConstants.AES_256_GCM)) {
                keyGen.init(256);
            }
            return keyGen;
        } catch (NoSuchAlgorithmException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, e
            );
        }
    }
    
    private void storeSecurityToken(SecurityToken tok) {
        TokenType tokenType = WSSecurityTokenConstants.EncryptedKeyToken;
        if (tok.getTokenType() != null 
            && tok.getTokenType().startsWith(WSSConstants.NS_KERBEROS11_TOKEN_PROFILE)) {
            tokenType = WSSecurityTokenConstants.KerberosToken;
        }
        
        final GenericOutboundSecurityToken encryptedKeySecurityToken = 
            new GenericOutboundSecurityToken(tok.getId(), tokenType, tok.getKey());
        
        final SecurityTokenProvider<OutboundSecurityToken> encryptedKeySecurityTokenProvider =
            new SecurityTokenProvider<OutboundSecurityToken>() {

                @Override
                public OutboundSecurityToken getSecurityToken() throws XMLSecurityException {
                    return encryptedKeySecurityToken;
                }

                @Override
                public String getId() {
                    return encryptedKeySecurityToken.getId();
                }
            };
        encryptedKeySecurityToken.setSha1Identifier(tok.getSHA1());
        outboundTokens.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION, 
                           encryptedKeySecurityTokenProvider);
        outboundTokens.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE, 
                           encryptedKeySecurityTokenProvider);
    }
    
    private class TokenStoreCallbackHandler implements CallbackHandler {
        private CallbackHandler internal;
        private TokenStore store;
        public TokenStoreCallbackHandler(CallbackHandler in, TokenStore st) {
            internal = in;
            store = st;
        }
        
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
                
                if (pc.getKey() != null) {
                    String id = pc.getIdentifier();
                    SecurityToken token = store.getToken(id);
                    if (token != null && token.getSHA1() == null) {
                        token.setSHA1(getSHA1(pc.getKey()));
                        // Create another cache entry with the SHA1 Identifier as the key 
                        // for easy retrieval
                        store.add(token.getSHA1(), token);
                    }
                }
            }
            if (internal != null) {
                internal.handle(callbacks);
            }
        }
        
    }
}
