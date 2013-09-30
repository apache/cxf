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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.w3c.dom.Element;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.neethi.Assertion;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.ContentEncryptedElements;
import org.apache.wss4j.policy.model.EncryptedElements;
import org.apache.wss4j.policy.model.EncryptedParts;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KeyValueToken;
import org.apache.wss4j.policy.model.Layout;
import org.apache.wss4j.policy.model.Layout.LayoutType;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SamlToken.SamlTokenType;
import org.apache.wss4j.policy.model.SignedElements;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.UsernameToken.PasswordType;
import org.apache.wss4j.policy.model.Wss10;
import org.apache.wss4j.policy.model.Wss11;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.policy.model.X509Token.TokenType;
import org.apache.wss4j.policy.model.XPath;
import org.apache.wss4j.policy.stax.PolicyUtils;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.impl.securityToken.KerberosClientSecurityToken;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;

/**
 * 
 */
public abstract class AbstractStaxBindingHandler {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractStaxBindingHandler.class);
    protected boolean timestampAdded;
    protected boolean signatureConfirmationAdded;
    protected Set<SecurePart> encryptedTokensList = new HashSet<SecurePart>();
    
    protected Map<AbstractToken, SecurePart> endEncSuppTokMap;
    protected Map<AbstractToken, SecurePart> endSuppTokMap;
    protected Map<AbstractToken, SecurePart> sgndEndEncSuppTokMap;
    protected Map<AbstractToken, SecurePart> sgndEndSuppTokMap;
    protected Map<String, SecurityTokenProvider<OutboundSecurityToken>> outboundTokens;
    
    private final Map<String, Object> properties;
    private final SoapMessage message;
    
    public AbstractStaxBindingHandler(
        Map<String, Object> properties, 
        SoapMessage msg,
        Map<String, SecurityTokenProvider<OutboundSecurityToken>> outboundTokens
    ) {
        this.properties = properties;
        this.message = msg;
        this.outboundTokens = outboundTokens;
    }

    protected SecurePart addUsernameToken(UsernameToken usernameToken) {
        IncludeTokenType includeToken = usernameToken.getIncludeTokenType();
        if (!isTokenRequired(includeToken)) {
            return null;
        }

        Map<String, Object> config = getProperties();
        
        // Action
        if (config.containsKey(ConfigurationConstants.ACTION)) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            config.put(ConfigurationConstants.ACTION, 
                       action + " " + ConfigurationConstants.USERNAME_TOKEN);
        } else {
            config.put(ConfigurationConstants.ACTION, 
                       ConfigurationConstants.USERNAME_TOKEN);
        }

        // Password Type
        PasswordType passwordType = usernameToken.getPasswordType();
        if (passwordType == PasswordType.HashPassword) {
            config.put(ConfigurationConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        } else if (passwordType == PasswordType.NoPassword) {
            config.put(ConfigurationConstants.PASSWORD_TYPE, WSConstants.PW_NONE);
        } else {
            config.put(ConfigurationConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        }

        // Nonce + Created
        if (usernameToken.isNonce()) {
            config.put(ConfigurationConstants.ADD_USERNAMETOKEN_NONCE, "true");
        }
        if (usernameToken.isCreated()) {
            config.put(ConfigurationConstants.ADD_USERNAMETOKEN_CREATED, "true");
        }
        
        // Check if a CallbackHandler was specified
        if (config.get(ConfigurationConstants.PW_CALLBACK_REF) == null) {
            String password = (String)message.getContextualProperty(SecurityConstants.PASSWORD);
            if (password != null) {
                String username = 
                    (String)message.getContextualProperty(SecurityConstants.USERNAME);
                UTCallbackHandler callbackHandler = new UTCallbackHandler(username, password);
                config.put(ConfigurationConstants.PW_CALLBACK_REF, callbackHandler);
            }
        }
        
        return new SecurePart(WSSConstants.TAG_wsse_UsernameToken, Modifier.Element);
    }
    
    private static class UTCallbackHandler implements CallbackHandler {
        
        private final String username;
        private final String password;
        
        public UTCallbackHandler(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof WSPasswordCallback) {
                    WSPasswordCallback pwcb = (WSPasswordCallback)callback;
                    if (pwcb.getIdentifier().equals(username)) {
                        pwcb.setPassword(password);
                    }
                }
            }
        }
        
    }
    
    protected SecurePart addKerberosToken(
        KerberosToken token, boolean signed, boolean endorsing
    ) throws WSSecurityException {
        IncludeTokenType includeToken = token.getIncludeTokenType();
        if (!isTokenRequired(includeToken)) {
            return null;
        }

        SecurityToken secToken = getSecurityToken();
        if (secToken == null) {
            policyNotAsserted(token, "Could not find KerberosToken");
        }
        
        // Convert to WSS4J token
        final KerberosClientSecurityToken wss4jToken = 
            new KerberosClientSecurityToken(secToken.getData(), secToken.getKey(), secToken.getId());
        
        final SecurityTokenProvider<OutboundSecurityToken> kerberosSecurityTokenProvider =
            new SecurityTokenProvider<OutboundSecurityToken>() {

                @Override
                public OutboundSecurityToken getSecurityToken() throws WSSecurityException {
                    return wss4jToken;
                }

                @Override
                public String getId() {
                    return wss4jToken.getId();
                }
            };
        outboundTokens.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_BST, 
                           kerberosSecurityTokenProvider);
        
        // Action
        Map<String, Object> config = getProperties();
        String actionToPerform = ConfigurationConstants.KERBEROS_TOKEN;
        if (endorsing) {
            actionToPerform = ConfigurationConstants.SIGNATURE_WITH_KERBEROS_TOKEN;
        }
        
        if (config.containsKey(ConfigurationConstants.ACTION)) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            config.put(ConfigurationConstants.ACTION, action + " " + actionToPerform);
        } else {
            config.put(ConfigurationConstants.ACTION, actionToPerform);
        }
        
        /*
        if (endorsing) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            config.put(ConfigurationConstants.ACTION,
                ConfigurationConstants.SIGNATURE_WITH_KERBEROS_TOKEN  + " " + action);
            // config.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        }
        */
        
        return new SecurePart(WSSConstants.TAG_wsse_BinarySecurityToken, Modifier.Element);
    }
    
    protected SecurePart addSamlToken(
        SamlToken token, 
        boolean signed,
        boolean endorsing
    ) throws WSSecurityException {
        IncludeTokenType includeToken = token.getIncludeTokenType();
        if (!isTokenRequired(includeToken)) {
            return null;
        }
        
        Map<String, Object> config = getProperties();
        
        //
        // Get the SAML CallbackHandler
        //
        Object o = message.getContextualProperty(SecurityConstants.SAML_CALLBACK_HANDLER);
    
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        if (handler == null) {
            policyNotAsserted(token, "No SAML CallbackHandler available");
            return null;
        }
        config.put(ConfigurationConstants.SAML_CALLBACK_REF, handler);
        
        // Action
        String samlAction = ConfigurationConstants.SAML_TOKEN_UNSIGNED;
        if (signed || endorsing) {
            samlAction = ConfigurationConstants.SAML_TOKEN_SIGNED;
        }
        
        if (config.containsKey(ConfigurationConstants.ACTION)) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            config.put(ConfigurationConstants.ACTION, action + " " + samlAction);
        } else {
            config.put(ConfigurationConstants.ACTION, samlAction);
        }
        
        QName qname = WSSConstants.TAG_saml2_Assertion;
        SamlTokenType tokenType = token.getSamlTokenType();
        if (tokenType == SamlTokenType.WssSamlV11Token10 || tokenType == SamlTokenType.WssSamlV11Token11) {
            qname = WSSConstants.TAG_saml_Assertion;
        }
        
        return new SecurePart(qname, Modifier.Element);
    }
    
    protected void addIssuedToken(IssuedToken token, SecurityToken secToken, 
                                  boolean signed, boolean endorsing) {
        if (isTokenRequired(token.getIncludeTokenType())) {
            final Element el = secToken.getToken();
            
            String samlAction = ConfigurationConstants.SAML_TOKEN_UNSIGNED;
            if (signed || endorsing) {
                samlAction = ConfigurationConstants.SAML_TOKEN_SIGNED;
            }
            Map<String, Object> config = getProperties();
            if (config.containsKey(ConfigurationConstants.ACTION)) {
                String action = (String)config.get(ConfigurationConstants.ACTION);
                config.put(ConfigurationConstants.ACTION, action + " " + samlAction);
            } else {
                config.put(ConfigurationConstants.ACTION, samlAction);
            }
            
            // Mock up a Subject so that the SAMLTokenOutProcessor can get access to the certificate
            final SubjectBean subjectBean;
            if (signed || endorsing) {
                KeyInfoBean keyInfo = new KeyInfoBean();
                keyInfo.setCertificate(secToken.getX509Certificate());
                keyInfo.setEphemeralKey(secToken.getSecret());
                subjectBean = new SubjectBean("", "", "");
                subjectBean.setKeyInfo(keyInfo);
            } else {
                subjectBean = null;
            }
            
            CallbackHandler callbackHandler = new CallbackHandler() {

                @Override
                public void handle(Callback[] callbacks) {
                    for (Callback callback : callbacks) {
                        if (callback instanceof SAMLCallback) {
                            SAMLCallback samlCallback = (SAMLCallback)callback;
                            samlCallback.setAssertionElement(el);
                            samlCallback.setSubject(subjectBean);
                        }
                    }
                }
                
            };
            config.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);
        } 
    }
    
    protected void policyNotAsserted(Assertion assertion, String reason) {
        if (assertion == null) {
            return;
        }
        LOG.log(Level.FINE, "Not asserting " + assertion.getName() + ": " + reason);
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason);
                }
            }
        }
        if (!assertion.isOptional()) {
            throw new PolicyException(new Message(reason, LOG));
        }
    }
    
    protected void configureTimestamp(AssertionInfoMap aim) {
        AbstractBinding binding = getBinding(aim);
        if (binding != null && binding.isIncludeTimestamp()) {
            timestampAdded = true;
        }
    }
    
    protected void configureLayout(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.LAYOUT);
        Layout layout = null;
        for (AssertionInfo ai : ais) {
            layout = (Layout)ai.getAssertion();
            ai.setAsserted(true);
        }
        
        if (!timestampAdded) {
            return;
        }
        
        Map<String, Object> config = getProperties();
        boolean timestampLast = 
            layout != null && layout.getLayoutType() == LayoutType.LaxTsLast;
        
        if (config.containsKey(ConfigurationConstants.ACTION)) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            if (timestampLast) {
                config.put(ConfigurationConstants.ACTION, 
                       ConfigurationConstants.TIMESTAMP + " " + action);
            } else {
                config.put(ConfigurationConstants.ACTION, 
                       action + " " + ConfigurationConstants.TIMESTAMP);
            }
        } else {
            config.put(ConfigurationConstants.ACTION, 
                       ConfigurationConstants.TIMESTAMP);
        }
    }

    protected AbstractBinding getBinding(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = 
            getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (ais != null && ais.size() > 0) {
            return (AbstractBinding)ais.iterator().next().getAssertion();
        }
        
        ais = getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (ais != null && ais.size() > 0) {
            return (AbstractBinding)ais.iterator().next().getAssertion();
        }
        
        ais = getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (ais != null && ais.size() > 0) {
            return (AbstractBinding)ais.iterator().next().getAssertion();
        }
        
        return null;
    }
    
    protected boolean isRequestor() {
        return MessageUtils.isRequestor(message);
    }
    
    protected boolean isTokenRequired(IncludeTokenType includeToken) {
        if (includeToken == IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            return false;
        } else if (includeToken == IncludeTokenType.INCLUDE_TOKEN_ALWAYS) {
            return true;
        } else {
            boolean initiator = MessageUtils.isRequestor(message);
            if (initiator && (includeToken == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT
                || includeToken == IncludeTokenType.INCLUDE_TOKEN_ONCE)) {
                return true;
            } else if (!initiator && includeToken == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR) {
                return true;
            }
            return false;
        }
    }
    
    protected Collection<AssertionInfo> getAllAssertionsByLocalname(
        AssertionInfoMap aim,
        String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));

        if ((sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty())) {
            Collection<AssertionInfo> ais = new HashSet<AssertionInfo>();
            if (sp11Ais != null) {
                ais.addAll(sp11Ais);
            }
            if (sp12Ais != null) {
                ais.addAll(sp12Ais);
            }
            return ais;
        }

        return Collections.emptySet();
    }

    protected Map<String, Object> getProperties() {
        return properties;
    }

    protected SoapMessage getMessage() {
        return message;
    }
    
    protected void configureSignature(
        AbstractTokenWrapper wrapper, AbstractToken token, boolean attached
    ) throws WSSecurityException {
        Map<String, Object> config = getProperties();
        
        if (token instanceof X509Token) {
            X509Token x509Token = (X509Token) token;
            TokenType tokenType = x509Token.getTokenType();
            if (tokenType == TokenType.WssX509PkiPathV1Token10
                || tokenType == TokenType.WssX509PkiPathV1Token11) {
                config.put(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "false");
            }
        }
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        AbstractBinding binding = getBinding(aim);
        
        config.put(ConfigurationConstants.SIG_KEY_ID, getKeyIdentifierType(wrapper, token));

        // Find out do we also need to include the token as per the Inclusion requirement
        if (token instanceof X509Token 
            && token.getIncludeTokenType() != IncludeTokenType.INCLUDE_TOKEN_NEVER
            && ("IssuerSerial".equals(config.get(ConfigurationConstants.SIG_KEY_ID))
                || "Thumbprint".equals(config.get(ConfigurationConstants.SIG_KEY_ID)))) {
            config.put(ConfigurationConstants.INCLUDE_SIGNATURE_TOKEN, "true");
        } else {
            config.put(ConfigurationConstants.INCLUDE_SIGNATURE_TOKEN, "false");
        }

        String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
        if (binding instanceof SymmetricBinding) {
            userNameKey = SecurityConstants.ENCRYPT_USERNAME;
            config.put(ConfigurationConstants.SIG_ALGO, 
                       binding.getAlgorithmSuite().getSymmetricSignature());
        } else {
            config.put(ConfigurationConstants.SIG_ALGO, 
                       binding.getAlgorithmSuite().getAsymmetricSignature());
        }
        String sigUser = (String)message.getContextualProperty(userNameKey);
        if (sigUser != null) {
            config.put(ConfigurationConstants.SIGNATURE_USER, sigUser);
        }

        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        config.put(ConfigurationConstants.SIG_DIGEST_ALGO, algType.getDigest());
        // sig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());

    }
    
    protected final TokenStore getTokenStore() {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore = 
                (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null) {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                String cacheKey = SecurityConstants.TOKEN_STORE_CACHE_INSTANCE;
                if (info.getName() != null) {
                    cacheKey += "-" + info.getName().toString().hashCode();
                }
                tokenStore = tokenStoreFactory.newTokenStore(cacheKey, message);
                info.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
            }
            return tokenStore;
        }
    }
    
    protected String getKeyIdentifierType(AbstractTokenWrapper wrapper, AbstractToken token) {

        String identifier = null;
        if (token instanceof X509Token) {
            X509Token x509Token = (X509Token)token;
            if (x509Token.isRequireIssuerSerialReference()) {
                identifier = "IssuerSerial";
            } else if (x509Token.isRequireKeyIdentifierReference()) {
                identifier = "SKIKeyIdentifier";
            } else if (x509Token.isRequireThumbprintReference()) {
                identifier = "Thumbprint";
            }
        } else if (token instanceof KeyValueToken) {
            identifier = "KeyValue";
        }
        
        if (identifier != null) {
            return identifier;
        }

        if (token.getIncludeTokenType() == IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            Wss10 wss = getWss10();
            if (wss == null || wss.isMustSupportRefKeyIdentifier()) {
                return "SKIKeyIdentifier";
            } else if (wss.isMustSupportRefIssuerSerial()) {
                return "IssuerSerial";
            } else if (wss instanceof Wss11
                && ((Wss11) wss).isMustSupportRefThumbprint()) {
                return "Thumbprint";
            }
        } else {
            return "DirectReference";
        }
        
        return "IssuerSerial";
    }
    
    protected Wss10 getWss10() {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.WSS10);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                return (Wss10)ai.getAssertion();
            }            
        }
        
        ais = getAllAssertionsByLocalname(aim, SPConstants.WSS11);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                return (Wss10)ai.getAssertion();
            }            
        }  
        
        return null;
    }
    
    protected Map<AbstractToken, SecurePart> handleSupportingTokens(
        Collection<Assertion> tokens, 
        boolean signed,
        boolean endorse
    ) throws Exception {
        Map<AbstractToken, SecurePart> ret = new HashMap<AbstractToken, SecurePart>();
        if (tokens != null) {
            for (Assertion pa : tokens) {
                if (pa instanceof SupportingTokens) {
                    handleSupportingTokens((SupportingTokens)pa, signed, endorse, ret);
                }
            }
        }
        return ret;
    }
                                                            
    protected Map<AbstractToken, SecurePart> handleSupportingTokens(
        SupportingTokens suppTokens,
        boolean signed,
        boolean endorse
    ) throws Exception {
        return handleSupportingTokens(suppTokens, signed, endorse, new HashMap<AbstractToken, SecurePart>());
    }
                                                            
    protected Map<AbstractToken, SecurePart> handleSupportingTokens(
        SupportingTokens suppTokens, 
        boolean signed,
        boolean endorse,
        Map<AbstractToken, SecurePart> ret
    ) throws Exception {
        if (suppTokens == null) {
            return ret;
        }
        for (AbstractToken token : suppTokens.getTokens()) {
            if (token instanceof UsernameToken) {
                handleUsernameTokenSupportingToken(
                    (UsernameToken)token, endorse, suppTokens.isEncryptedToken(), ret
                );
            /* TODO else if (isRequestor() 
                && (token instanceof IssuedToken
                    || token instanceof SecureConversationToken
                    || token instanceof SecurityContextToken
                    || token instanceof KerberosToken)) {
                //ws-trust/ws-sc stuff.......
                SecurityToken secToken = getSecurityToken();
                if (secToken == null) {
                    policyNotAsserted(token, "Could not find IssuedToken");
                }
                Element clone = cloneElement(secToken.getToken());
                secToken.setToken(clone);
                addSupportingElement(clone);

                String id = secToken.getId();
                if (id != null && id.charAt(0) == '#') {
                    id = id.substring(1);
                }
                if (suppTokens.isEncryptedToken()) {
                    WSEncryptionPart part = new WSEncryptionPart(id, "Element");
                    part.setElement(clone);
                    encryptedTokensList.add(part);
                }

                if (secToken.getX509Certificate() == null) {  
                    ret.put(token, new WSSecurityTokenHolder(wssConfig, secToken));
                } else {
                    WSSecSignature sig = new WSSecSignature(wssConfig);                    
                    sig.setX509Certificate(secToken.getX509Certificate());
                    sig.setCustomTokenId(id);
                    sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
                    String tokenType = secToken.getTokenType();
                    if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                        || WSConstants.SAML_NS.equals(tokenType)) {
                        sig.setCustomTokenValueType(WSConstants.WSS_SAML_KI_VALUE_TYPE);
                    } else if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                        || WSConstants.SAML2_NS.equals(tokenType)) {
                        sig.setCustomTokenValueType(WSConstants.WSS_SAML2_KI_VALUE_TYPE);
                    } else if (tokenType != null) {
                        sig.setCustomTokenValueType(tokenType);
                    } else {
                        sig.setCustomTokenValueType(WSConstants.WSS_SAML_KI_VALUE_TYPE);
                    }
                    sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAsymmetricSignature());
                    sig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());

                    Crypto crypto = secToken.getCrypto();
                    String uname = null;
                    try {
                        uname = crypto.getX509Identifier(secToken.getX509Certificate());
                    } catch (WSSecurityException e1) {
                        LOG.log(Level.FINE, e1.getMessage(), e1);
                        throw new Fault(e1);
                    }

                    String password = getPassword(uname, token, WSPasswordCallback.Usage.SIGNATURE);
                    sig.setUserInfo(uname, password);
                    try {
                        sig.prepare(saaj.getSOAPPart(), secToken.getCrypto(), secHeader);
                    } catch (WSSecurityException e) {
                        LOG.log(Level.FINE, e.getMessage(), e);
                        throw new Fault(e);
                    }

                    ret.put(token, sig);                
                }

            } */
            } else if (isRequestor() && token instanceof KerberosToken) {
                SecurePart securePart = addKerberosToken((KerberosToken)token, signed, endorse);
                if (securePart != null) {
                    ret.put(token, securePart);
                    if (suppTokens.isEncryptedToken()) {
                        encryptedTokensList.add(securePart);
                    }
                }
            } else if (token instanceof X509Token || token instanceof KeyValueToken) {
                configureSignature(suppTokens, token, false);
                if (suppTokens.isEncryptedToken()) {
                    SecurePart part = 
                        new SecurePart(WSSConstants.TAG_wsse_BinarySecurityToken, Modifier.Element);
                    encryptedTokensList.add(part);
                }
                ret.put(token, new SecurePart(WSSConstants.TAG_dsig_Signature, Modifier.Element));
            } else if (token instanceof SamlToken) {
                SecurePart securePart = addSamlToken((SamlToken)token, signed, endorse);
                if (securePart != null) {
                    ret.put(token, securePart);
                    if (suppTokens.isEncryptedToken()) {
                        encryptedTokensList.add(securePart);
                    }
                }
            }
        }
        return ret;
    }

    protected void handleUsernameTokenSupportingToken(
         UsernameToken token, boolean endorse, boolean encryptedToken, Map<AbstractToken, SecurePart> ret
    ) throws Exception {
        if (endorse) {
            throw new Exception("Endorsing UsernameTokens are not supported in the streaming code");
        } else {
            SecurePart securePart = addUsernameToken(token);
            if (securePart != null) {
                ret.put(token, securePart);
                //WebLogic and WCF always encrypt these
                //See:  http://e-docs.bea.com/wls/docs103/webserv_intro/interop.html
                //encryptedTokensIdList.add(utBuilder.getId());
                if (encryptedToken
                    || MessageUtils.getContextualBoolean(message, 
                                                         SecurityConstants.ALWAYS_ENCRYPT_UT,
                                                         true)) {
                    encryptedTokensList.add(securePart);
                }
            }
        }
    }
    
    protected SecurityToken getSecurityToken() {
        SecurityToken st = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
        if (st == null) {
            String id = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
            if (id != null) {
                st = getTokenStore().getToken(id);
            }
        }
        if (st != null) {
            getTokenStore().add(st);
            return st;
        }
        return null;
    }

    
    protected Collection<Assertion> findAndAssertPolicy(QName n) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(n);
        if (ais != null && !ais.isEmpty()) {
            List<Assertion> p = new ArrayList<Assertion>(ais.size());
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
                p.add(ai.getAssertion());
            }
            return p;
        }
        return null;
    } 
    
    protected void addSupportingTokens() throws Exception {
        
        Collection<Assertion> sgndSuppTokens = 
            findAndAssertPolicy(SP12Constants.SIGNED_SUPPORTING_TOKENS);
        Map<AbstractToken, SecurePart> sigSuppTokMap = 
            this.handleSupportingTokens(sgndSuppTokens, true, false);
        sgndSuppTokens = findAndAssertPolicy(SP11Constants.SIGNED_SUPPORTING_TOKENS);
        sigSuppTokMap.putAll(this.handleSupportingTokens(sgndSuppTokens, true, false));
        
        Collection<Assertion> endSuppTokens = 
            findAndAssertPolicy(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
        endSuppTokMap = this.handleSupportingTokens(endSuppTokens, false, true);
        endSuppTokens = findAndAssertPolicy(SP11Constants.ENDORSING_SUPPORTING_TOKENS);
        endSuppTokMap.putAll(this.handleSupportingTokens(endSuppTokens, false, true));

        Collection<Assertion> sgndEndSuppTokens 
            = findAndAssertPolicy(SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        sgndEndSuppTokMap = this.handleSupportingTokens(sgndEndSuppTokens, true, true);
        sgndEndSuppTokens = findAndAssertPolicy(SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        sgndEndSuppTokMap.putAll(this.handleSupportingTokens(sgndEndSuppTokens, true, true));
        
        Collection<Assertion> sgndEncryptedSuppTokens 
            = findAndAssertPolicy(SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        Map<AbstractToken, SecurePart> sgndEncSuppTokMap = 
            this.handleSupportingTokens(sgndEncryptedSuppTokens, true, false);
        
        Collection<Assertion> endorsingEncryptedSuppTokens 
            = findAndAssertPolicy(SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        endEncSuppTokMap 
            = this.handleSupportingTokens(endorsingEncryptedSuppTokens, false, true);

        Collection<Assertion> sgndEndEncSuppTokens 
            = findAndAssertPolicy(SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        sgndEndEncSuppTokMap = this.handleSupportingTokens(sgndEndEncSuppTokens, true, true);

        Collection<Assertion> supportingToks 
            = findAndAssertPolicy(SP12Constants.SUPPORTING_TOKENS);
        this.handleSupportingTokens(supportingToks, false, false);
        supportingToks = findAndAssertPolicy(SP11Constants.SUPPORTING_TOKENS);
        this.handleSupportingTokens(supportingToks, false, false);

        Collection<Assertion> encryptedSupportingToks 
            = findAndAssertPolicy(SP12Constants.ENCRYPTED_SUPPORTING_TOKENS);
        this.handleSupportingTokens(encryptedSupportingToks, false, false);

        //Setup signature parts
        addSignatureParts(sigSuppTokMap);
        addSignatureParts(sgndEncSuppTokMap);
        addSignatureParts(sgndEndSuppTokMap);
        addSignatureParts(sgndEndEncSuppTokMap);
    }
    
    protected void addSignatureParts(Map<AbstractToken, SecurePart> tokenMap) {
        for (AbstractToken token : tokenMap.keySet()) {
            SecurePart part = tokenMap.get(token);

            String parts = "";
            Map<String, Object> config = getProperties();
            if (config.containsKey(ConfigurationConstants.SIGNATURE_PARTS)) {
                parts = (String)config.get(ConfigurationConstants.SIGNATURE_PARTS);
                if (!parts.endsWith(";")) {
                    parts += ";";
                }
            }

            QName name = part.getName();
            String action = (String)config.get(ConfigurationConstants.ACTION);
            // Don't add a signed SAML Token as a part, as it will be automatically signed by WSS4J
            if (!((WSSConstants.TAG_saml_Assertion.equals(name) 
                || WSSConstants.TAG_saml2_Assertion.equals(name))
                && action != null && action.contains(ConfigurationConstants.SAML_TOKEN_SIGNED))) {
                parts += "{Element}{" +  name.getNamespaceURI() + "}" + name.getLocalPart() + ";";
            }

            config.put(ConfigurationConstants.SIGNATURE_PARTS, parts);
        }
        
    }
    
    protected void addSignatureConfirmation(List<SecurePart> sigParts) {
        Wss10 wss10 = getWss10();
        
        if (!(wss10 instanceof Wss11) 
            || !((Wss11)wss10).isRequireSignatureConfirmation()) {
            //If we don't require sig confirmation simply go back :-)
            return;
        }
        
        // Enable SignatureConfirmation
        Map<String, Object> config = getProperties();
        config.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        
        if (sigParts != null) {
            SecurePart securePart = 
                new SecurePart(WSSConstants.TAG_wsse11_SignatureConfirmation, Modifier.Element);
            sigParts.add(securePart);
        }
        signatureConfirmationAdded = true;
    }
    
    /**
     * Identifies the portions of the message to be signed
     */
    protected List<SecurePart> getSignedParts() throws SOAPException {
        SignedParts parts = null;
        SignedElements elements = null;
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.SIGNED_PARTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                parts = (SignedParts)ai.getAssertion();
                ai.setAsserted(true);
            }            
        }
        
        ais = getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ELEMENTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                elements = (SignedElements)ai.getAssertion();
                ai.setAsserted(true);
            }            
        }
        
        List<SecurePart> signedParts = new ArrayList<SecurePart>();
        if (parts != null) {
            if (parts.isBody()) {
                QName soapBody = new QName(WSSConstants.NS_SOAP12, "Body");
                SecurePart securePart = new SecurePart(soapBody, Modifier.Element);
                signedParts.add(securePart);
            }
            for (Header head : parts.getHeaders()) {
                String localName = head.getName();
                if (localName == null) {
                    localName = "*";
                }
                QName qname = new QName(head.getNamespace(), localName);
                SecurePart securePart = new SecurePart(qname, Modifier.Element);
                securePart.setRequired(false);
                signedParts.add(securePart);
            }
        }
        
        if (elements != null && elements.getXPaths() != null) {
            for (XPath xPath : elements.getXPaths()) {
                List<QName> qnames = PolicyUtils.getElementPath(xPath);
                if (!qnames.isEmpty()) {
                    SecurePart securePart = 
                        new SecurePart(qnames.get(qnames.size() - 1), Modifier.Element);
                    signedParts.add(securePart);
                }
            }
        }
        
        return signedParts;
    }
    
    /**
     * Identifies the portions of the message to be encrypted
     */
    protected List<SecurePart> getEncryptedParts() throws SOAPException {
        EncryptedParts parts = null;
        EncryptedElements elements = null;
        ContentEncryptedElements celements = null;
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_PARTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                parts = (EncryptedParts)ai.getAssertion();
                ai.setAsserted(true);
            }            
        }
        
        ais = getAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_ELEMENTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                elements = (EncryptedElements)ai.getAssertion();
                ai.setAsserted(true);
            }            
        }
        
        ais = getAllAssertionsByLocalname(aim, SPConstants.CONTENT_ENCRYPTED_ELEMENTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                celements = (ContentEncryptedElements)ai.getAssertion();
                ai.setAsserted(true);
            }            
        }
        
        List<SecurePart> encryptedParts = new ArrayList<SecurePart>();
        if (parts != null) {
            if (parts.isBody()) {
                QName soapBody = new QName(WSSConstants.NS_SOAP12, "Body");
                SecurePart securePart = new SecurePart(soapBody, Modifier.Content);
                encryptedParts.add(securePart);
            }
            for (Header head : parts.getHeaders()) {
                String localName = head.getName();
                if (localName == null) {
                    localName = "*";
                }
                QName qname = new QName(head.getNamespace(), localName);
                SecurePart securePart = new SecurePart(qname, Modifier.Element);
                securePart.setRequired(false);
                encryptedParts.add(securePart);
            }
        }
        
        if (elements != null && elements.getXPaths() != null) {
            for (XPath xPath : elements.getXPaths()) {
                List<QName> qnames = PolicyUtils.getElementPath(xPath);
                if (!qnames.isEmpty()) {
                    SecurePart securePart = 
                        new SecurePart(qnames.get(qnames.size() - 1), Modifier.Element);
                    encryptedParts.add(securePart);
                }
            }
        }
        
        if (celements != null && celements.getXPaths() != null) {
            for (XPath xPath : celements.getXPaths()) {
                List<QName> qnames = PolicyUtils.getElementPath(xPath);
                if (!qnames.isEmpty()) {
                    SecurePart securePart = 
                        new SecurePart(qnames.get(qnames.size() - 1), Modifier.Content);
                    encryptedParts.add(securePart);
                }
            }
        }
        
        return encryptedParts;
    }
    
}
