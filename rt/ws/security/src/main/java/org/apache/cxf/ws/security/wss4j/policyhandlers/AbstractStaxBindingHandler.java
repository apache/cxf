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
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.w3c.dom.Element;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.Attachments;
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
import org.apache.wss4j.stax.ext.WSSConstants.UsernameTokenPasswordType;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.impl.securityToken.KerberosClientSecurityToken;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.OutboundSecurityContext;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;
import org.apache.xml.security.stax.impl.securityToken.GenericOutboundSecurityToken;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;

/**
 * 
 */
public abstract class AbstractStaxBindingHandler extends AbstractCommonBindingHandler {
    protected boolean timestampAdded;
    protected boolean signatureConfirmationAdded;
    protected Set<SecurePart> encryptedTokensList = new HashSet<SecurePart>();
    
    protected Map<AbstractToken, SecurePart> endEncSuppTokMap;
    protected Map<AbstractToken, SecurePart> endSuppTokMap;
    protected Map<AbstractToken, SecurePart> sgndEndEncSuppTokMap;
    protected Map<AbstractToken, SecurePart> sgndEndSuppTokMap;
    protected final OutboundSecurityContext outboundSecurityContext;
    
    private final WSSSecurityProperties properties;
    private AbstractBinding binding;
    
    public AbstractStaxBindingHandler(
        WSSSecurityProperties properties, 
        SoapMessage msg,
        AbstractBinding binding,
        OutboundSecurityContext outboundSecurityContext
    ) {
        super(msg);
        this.properties = properties;
        this.binding = binding;
        this.outboundSecurityContext = outboundSecurityContext;
    }

    protected SecurePart addUsernameToken(UsernameToken usernameToken) {
        assertToken(usernameToken);
        IncludeTokenType includeToken = usernameToken.getIncludeTokenType();
        if (!isTokenRequired(includeToken)) {
            return null;
        }

        // Action
        properties.addAction(WSSConstants.USERNAMETOKEN);

        // Password Type
        PasswordType passwordType = usernameToken.getPasswordType();
        if (passwordType == PasswordType.HashPassword) {
            properties.setUsernameTokenPasswordType(UsernameTokenPasswordType.PASSWORD_DIGEST);
        } else if (passwordType == PasswordType.NoPassword) {
            properties.setUsernameTokenPasswordType(UsernameTokenPasswordType.PASSWORD_NONE);
        } else {
            properties.setUsernameTokenPasswordType(UsernameTokenPasswordType.PASSWORD_TEXT);
        }

        // Nonce + Created
        if (usernameToken.isNonce()) {
            properties.setAddUsernameTokenNonce(true);
        }
        if (usernameToken.isCreated()) {
            properties.setAddUsernameTokenCreated(true);
        }
        
        // Check if a CallbackHandler was specified
        if (properties.getCallbackHandler() == null) {
            String password = (String)message.getContextualProperty(SecurityConstants.PASSWORD);
            if (password != null) {
                String username = 
                    (String)message.getContextualProperty(SecurityConstants.USERNAME);
                UTCallbackHandler callbackHandler = new UTCallbackHandler(username, password);
                properties.setCallbackHandler(callbackHandler);
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
        KerberosToken token, boolean signed, boolean endorsing, boolean encrypting
    ) throws WSSecurityException {
        assertToken(token);
        IncludeTokenType includeToken = token.getIncludeTokenType();
        if (!isTokenRequired(includeToken)) {
            return null;
        }

        final SecurityToken secToken = getSecurityToken();
        if (secToken == null) {
            policyNotAsserted(token, "Could not find KerberosToken");
        }
        
        // Convert to WSS4J token
        final KerberosClientSecurityToken wss4jToken = 
            new KerberosClientSecurityToken(secToken.getData(), secToken.getKey(), secToken.getId()) {
            
                @Override
                public Key getSecretKey(String algorithmURI) throws XMLSecurityException {
                    if (secToken.getSecret() != null && algorithmURI != null && !"".equals(algorithmURI)) {
                        return KeyUtils.prepareSecretKey(algorithmURI, secToken.getSecret());
                    }
                    return secToken.getKey();
                }
            };
        wss4jToken.setSha1Identifier(secToken.getSHA1());
        
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
        outboundSecurityContext.registerSecurityTokenProvider(
                kerberosSecurityTokenProvider.getId(), kerberosSecurityTokenProvider);
        outboundSecurityContext.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_KERBEROS, 
                kerberosSecurityTokenProvider.getId());
        
        if (encrypting) {
            outboundSecurityContext.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION, 
                    kerberosSecurityTokenProvider.getId());
        }
        if (endorsing) {
            outboundSecurityContext.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE, 
                    kerberosSecurityTokenProvider.getId());
        }
        
        // Action
        properties.addAction(WSSConstants.KERBEROS_TOKEN);
        
        /*
        if (endorsing) {
            String action = (String)config.get(ConfigurationConstants.ACTION);
            config.put(ConfigurationConstants.ACTION,
                ConfigurationConstants.SIGNATURE_WITH_KERBEROS_TOKEN  + " " + action);
            // config.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        }
        */
        
        SecurePart securePart = new SecurePart(WSSConstants.TAG_wsse_BinarySecurityToken, Modifier.Element);
        securePart.setIdToSign(wss4jToken.getId());
        
        return securePart;
    }
    
    protected SecurePart addSamlToken(
        SamlToken token, 
        boolean signed,
        boolean endorsing
    ) throws WSSecurityException {
        assertToken(token);
        IncludeTokenType includeToken = token.getIncludeTokenType();
        if (!isTokenRequired(includeToken)) {
            return null;
        }
        
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
        properties.setSamlCallbackHandler(handler);
        
        // Action
        WSSConstants.Action actionToPerform = WSSConstants.SAML_TOKEN_UNSIGNED;
        if (signed || endorsing) {
            actionToPerform = WSSConstants.SAML_TOKEN_SIGNED;
        }
        properties.addAction(actionToPerform);
        
        QName qname = WSSConstants.TAG_saml2_Assertion;
        SamlTokenType tokenType = token.getSamlTokenType();
        if (tokenType == SamlTokenType.WssSamlV11Token10 || tokenType == SamlTokenType.WssSamlV11Token11) {
            qname = WSSConstants.TAG_saml_Assertion;
        }
        
        return new SecurePart(qname, Modifier.Element);
    }
    
    protected SecurePart addIssuedToken(AbstractToken token, SecurityToken secToken, 
                                  boolean signed, boolean endorsing) {
        assertToken(token);
        if (isTokenRequired(token.getIncludeTokenType())) {
            final Element el = secToken.getToken();
            
            if (el != null && "Assertion".equals(el.getLocalName())
                && (WSSConstants.NS_SAML.equals(el.getNamespaceURI())
                || WSSConstants.NS_SAML2.equals(el.getNamespaceURI()))) {
                WSSConstants.Action actionToPerform = WSSConstants.SAML_TOKEN_UNSIGNED;
                if (endorsing) {
                    actionToPerform = WSSConstants.SAML_TOKEN_SIGNED;
                }
                properties.addAction(actionToPerform);
                
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
                                
                                if (WSConstants.SAML_NS.equals(el.getNamespaceURI())) {
                                    samlCallback.setSamlVersion(Version.SAML_11);
                                } else {
                                    samlCallback.setSamlVersion(Version.SAML_20);
                                }
                            }
                        }
                    }
                    
                };
                properties.setSamlCallbackHandler(callbackHandler);
                
                QName qname = WSSConstants.TAG_saml2_Assertion;
                if (WSConstants.SAML_NS.equals(el.getNamespaceURI())) {
                    qname = WSSConstants.TAG_saml_Assertion;
                }
                
                return new SecurePart(qname, Modifier.Element);
            } else if (isRequestor()) {
                // An Encrypted Token...just include it as is
                properties.addAction(WSSConstants.CUSTOM_TOKEN);
            }
        }
        
        return null;
    }
    
    protected void storeSecurityToken(AbstractToken policyToken, SecurityToken tok) {
        SecurityTokenConstants.TokenType tokenType = WSSecurityTokenConstants.EncryptedKeyToken;
        if (tok.getTokenType() != null) {
            if (tok.getTokenType().startsWith(WSSConstants.NS_KERBEROS11_TOKEN_PROFILE)) {
                tokenType = WSSecurityTokenConstants.KerberosToken;
            } else if (tok.getTokenType().startsWith(WSSConstants.NS_SAML10_TOKEN_PROFILE)
                || tok.getTokenType().startsWith(WSSConstants.NS_SAML11_TOKEN_PROFILE)) {
                tokenType = WSSecurityTokenConstants.Saml11Token;
            } else if (tok.getTokenType().startsWith(WSSConstants.NS_WSC_05_02)
                || tok.getTokenType().startsWith(WSSConstants.NS_WSC_05_12)) {
                tokenType = WSSecurityTokenConstants.SecureConversationToken;
            }
        }
        
        final Key key = tok.getKey();
        final byte[] secret = tok.getSecret();
        final X509Certificate[] certs = new X509Certificate[1];
        if (tok.getX509Certificate() != null) {
            certs[0] = tok.getX509Certificate();
        }
        
        final GenericOutboundSecurityToken encryptedKeySecurityToken = 
            new GenericOutboundSecurityToken(tok.getId(), tokenType, key, certs) {
          
                @Override
                public Key getSecretKey(String algorithmURI) throws XMLSecurityException {
                    if (secret != null && algorithmURI != null && !"".equals(algorithmURI)) {
                        return KeyUtils.prepareSecretKey(algorithmURI, secret);
                    }
                    if (key != null) {
                        return key;
                    }
                    if (secret != null) {
                        String jceAlg = JCEMapper.getJCEKeyAlgorithmFromURI(algorithmURI);
                        if (jceAlg == null || "".equals(jceAlg)) {
                            jceAlg = "HmacSHA1";
                        }
                        return new SecretKeySpec(secret, jceAlg);
                    }
                
                    return super.getSecretKey(algorithmURI);
                }
            };
            
        // Store a DOM Element reference if it exists
        Element ref;
        if (isTokenRequired(policyToken.getIncludeTokenType())) {
            ref = tok.getAttachedReference();
        } else {
            ref = tok.getUnattachedReference();
        }

        if (ref != null && policyToken instanceof IssuedToken) {
            encryptedKeySecurityToken.setCustomTokenReference(ref);
        }
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
        
        outboundSecurityContext.registerSecurityTokenProvider(
                encryptedKeySecurityTokenProvider.getId(), encryptedKeySecurityTokenProvider);
        outboundSecurityContext.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION, 
                encryptedKeySecurityTokenProvider.getId());
        outboundSecurityContext.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE, 
                encryptedKeySecurityTokenProvider.getId());
        outboundSecurityContext.put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_CUSTOM_TOKEN, 
                encryptedKeySecurityTokenProvider.getId());
    }
    
    protected void configureTimestamp(AssertionInfoMap aim) {
        if (binding != null && binding.isIncludeTimestamp()) {
            timestampAdded = true;
            assertPolicy(new QName(binding.getName().getNamespaceURI(), SPConstants.INCLUDE_TIMESTAMP));
        }
    }
    
    protected void configureLayout(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.LAYOUT);
        Layout layout = null;
        for (AssertionInfo ai : ais) {
            layout = (Layout)ai.getAssertion();
            Collection<AssertionInfo> layoutTypeAis = aim.get(layout.getName());
            if (layoutTypeAis != null) {
                for (AssertionInfo layoutAi : layoutTypeAis) {
                    layoutAi.setAsserted(true);
                }
            }
            ai.setAsserted(true);
        }
        if (layout != null && layout.getLayoutType() != null) {
            assertPolicy(new QName(layout.getName().getNamespaceURI(), layout.getLayoutType().name()));
        }
        
        if (!timestampAdded) {
            return;
        }
        
        boolean timestampLast = 
            layout != null && layout.getLayoutType() == LayoutType.LaxTsLast;
        
        WSSConstants.Action actionToPerform = WSSConstants.TIMESTAMP;
        List<WSSConstants.Action> actionList = properties.getActions();
        if (timestampLast) {
            actionList.add(0, actionToPerform);
        } else {
            actionList.add(actionToPerform);
        }
    }

    protected WSSSecurityProperties getProperties() {
        return properties;
    }

    protected void configureSignature(
        AbstractTokenWrapper wrapper, AbstractToken token, boolean attached
    ) throws WSSecurityException {
        
        if (token instanceof X509Token) {
            X509Token x509Token = (X509Token) token;
            TokenType tokenType = x509Token.getTokenType();
            if (tokenType == TokenType.WssX509PkiPathV1Token10
                || tokenType == TokenType.WssX509PkiPathV1Token11) {
                properties.setUseSingleCert(false);
            }
        }
        
        properties.setSignatureKeyIdentifier(getKeyIdentifierType(wrapper, token));

        // Find out do we also need to include the token as per the Inclusion requirement
        WSSecurityTokenConstants.KeyIdentifier keyIdentifier = properties.getSignatureKeyIdentifier();
        if (token instanceof X509Token 
            && isTokenRequired(token.getIncludeTokenType())
            && (WSSecurityTokenConstants.KeyIdentifier_IssuerSerial.equals(keyIdentifier)
                || WSSecurityTokenConstants.KeyIdentifier_ThumbprintIdentifier.equals(keyIdentifier)
                || WSSecurityTokenConstants.KeyIdentifier_SecurityTokenDirectReference.equals(
                    keyIdentifier))) {
            properties.setIncludeSignatureToken(true);
        } else {
            properties.setIncludeSignatureToken(false);
        }

        String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
        if (binding instanceof SymmetricBinding) {
            userNameKey = SecurityConstants.ENCRYPT_USERNAME;
            properties.setSignatureAlgorithm(
                       binding.getAlgorithmSuite().getSymmetricSignature());
        } else {
            properties.setSignatureAlgorithm(
                       binding.getAlgorithmSuite().getAsymmetricSignature());
        }
        String sigUser = (String)message.getContextualProperty(userNameKey);
        if (sigUser == null) {
            sigUser = (String)message.getContextualProperty(SecurityConstants.USERNAME);
        }
        if (sigUser != null && properties.getSignatureUser() == null) {
            properties.setSignatureUser(sigUser);
        }

        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        properties.setSignatureDigestAlgorithm(algType.getDigest());
        // sig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());

    }
    
    protected WSSecurityTokenConstants.KeyIdentifier getKeyIdentifierType(
        AbstractTokenWrapper wrapper, AbstractToken token
    ) {
        WSSecurityTokenConstants.KeyIdentifier identifier = null;
        if (token instanceof X509Token) {
            X509Token x509Token = (X509Token)token;
            if (x509Token.isRequireIssuerSerialReference()) {
                identifier = WSSecurityTokenConstants.KeyIdentifier_IssuerSerial;
            } else if (x509Token.isRequireKeyIdentifierReference()) {
                identifier = WSSecurityTokenConstants.KeyIdentifier_SkiKeyIdentifier;
            } else if (x509Token.isRequireThumbprintReference()) {
                identifier = WSSecurityTokenConstants.KeyIdentifier_ThumbprintIdentifier;
            }
        } else if (token instanceof KeyValueToken) {
            identifier = WSSecurityTokenConstants.KeyIdentifier_KeyValue;
        }
        
        if (identifier != null) {
            return identifier;
        }

        if (token.getIncludeTokenType() == IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            Wss10 wss = getWss10();
            if (wss == null || wss.isMustSupportRefKeyIdentifier()) {
                identifier = WSSecurityTokenConstants.KeyIdentifier_SkiKeyIdentifier;
            } else if (wss.isMustSupportRefIssuerSerial()) {
                identifier = WSSecurityTokenConstants.KeyIdentifier_IssuerSerial;
            } else if (wss instanceof Wss11
                && ((Wss11) wss).isMustSupportRefThumbprint()) {
                identifier = WSSecurityTokenConstants.KeyIdentifier_ThumbprintIdentifier;
            }
        } else if (token.getIncludeTokenType() == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT
            && !isRequestor() && token instanceof X509Token) {
            identifier = WSSecurityTokenConstants.KeyIdentifier_IssuerSerial;
        } else if (token.getIncludeTokenType() == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR
            && isRequestor() && token instanceof X509Token) {
            identifier = WSSecurityTokenConstants.KeyIdentifier_IssuerSerial;
        }
        
        if (identifier != null) {
            return identifier;
        }
        
        return WSSecurityTokenConstants.KeyIdentifier_SecurityTokenDirectReference;
    }
    
    protected Map<AbstractToken, SecurePart> handleSupportingTokens(
        Collection<AssertionInfo> tokenAssertions, 
        boolean signed,
        boolean endorse
    ) throws Exception {
        Map<AbstractToken, SecurePart> ret = null;
        if (tokenAssertions != null) {
            ret = new HashMap<AbstractToken, SecurePart>();
            for (AssertionInfo assertionInfo : tokenAssertions) {
                if (assertionInfo.getAssertion() instanceof SupportingTokens) {
                    assertionInfo.setAsserted(true);
                    handleSupportingTokens((SupportingTokens)assertionInfo.getAssertion(), 
                            signed, endorse, ret);
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
            assertToken(token);
            if (!isTokenRequired(token.getIncludeTokenType())) {
                continue;
            }
            
            if (token instanceof UsernameToken) {
                handleUsernameTokenSupportingToken(
                    (UsernameToken)token, endorse, suppTokens.isEncryptedToken(), ret
                );
            /* TODO else if (isRequestor() 
                && (token instanceof IssuedToken
                    || token instanceof SecureConversationToken
                    || token instanceof SecurityContextToken
                    || token instanceof KerberosToken)) {

            } */
            } else if (token instanceof IssuedToken) {
                SecurityToken sigTok = getSecurityToken();
                SecurePart securePart = addIssuedToken((IssuedToken)token, sigTok, signed, endorse);
                if (securePart != null) {
                    ret.put(token, securePart);
                    if (suppTokens.isEncryptedToken()) {
                        encryptedTokensList.add(securePart);
                    }
                }
            } else if (token instanceof KerberosToken) {
                SecurePart securePart = addKerberosToken((KerberosToken)token, signed, endorse, false);
                if (securePart != null) {
                    ret.put(token, securePart);
                    if (suppTokens.isEncryptedToken()) {
                        encryptedTokensList.add(securePart);
                    }
                }
            } else if (token instanceof X509Token || token instanceof KeyValueToken) {
                assertToken(token);
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
    
    protected void addSupportingTokens() throws Exception {
        
        Collection<AssertionInfo> sgndSuppTokens = 
            getAllAssertionsByLocalname(SPConstants.SIGNED_SUPPORTING_TOKENS);
        if (!sgndSuppTokens.isEmpty()) {
            Map<AbstractToken, SecurePart> sigSuppTokMap = 
                this.handleSupportingTokens(sgndSuppTokens, true, false);
            addSignatureParts(sigSuppTokMap);
        }
        
        Collection<AssertionInfo> endSuppTokens = 
            getAllAssertionsByLocalname(SPConstants.ENDORSING_SUPPORTING_TOKENS);
        if (!endSuppTokens.isEmpty()) {
            endSuppTokMap = this.handleSupportingTokens(endSuppTokens, false, true);
        }

        Collection<AssertionInfo> sgndEndSuppTokens 
            = getAllAssertionsByLocalname(SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        if (!sgndEndSuppTokens.isEmpty()) {
            sgndEndSuppTokMap = this.handleSupportingTokens(sgndEndSuppTokens, true, true);
            addSignatureParts(sgndEndSuppTokMap);
        }
        
        Collection<AssertionInfo> sgndEncryptedSuppTokens 
            = getAllAssertionsByLocalname(SPConstants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        if (!sgndEncryptedSuppTokens.isEmpty()) {
            Map<AbstractToken, SecurePart> sgndEncSuppTokMap = 
                this.handleSupportingTokens(sgndEncryptedSuppTokens, true, false);
            addSignatureParts(sgndEncSuppTokMap);
        }
        
        Collection<AssertionInfo> endorsingEncryptedSuppTokens 
            = getAllAssertionsByLocalname(SPConstants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (!endorsingEncryptedSuppTokens.isEmpty()) {
            endEncSuppTokMap 
                = this.handleSupportingTokens(endorsingEncryptedSuppTokens, false, true);
        }

        Collection<AssertionInfo> sgndEndEncSuppTokens 
            = getAllAssertionsByLocalname(SPConstants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (!sgndEndEncSuppTokens.isEmpty()) {
            sgndEndEncSuppTokMap = this.handleSupportingTokens(sgndEndEncSuppTokens, true, true);
            addSignatureParts(sgndEndEncSuppTokMap);
        }

        Collection<AssertionInfo> supportingToks 
            = getAllAssertionsByLocalname(SPConstants.SUPPORTING_TOKENS);
        if (!supportingToks.isEmpty()) {
            this.handleSupportingTokens(supportingToks, false, false);
        }

        Collection<AssertionInfo> encryptedSupportingToks 
            = getAllAssertionsByLocalname(SPConstants.ENCRYPTED_SUPPORTING_TOKENS);
        if (!encryptedSupportingToks.isEmpty()) {
            this.handleSupportingTokens(encryptedSupportingToks, false, false);
        }
    }
    
    protected void addSignatureParts(Map<AbstractToken, SecurePart> tokenMap) {
        if (tokenMap != null) {
            for (AbstractToken token : tokenMap.keySet()) {
                SecurePart part = tokenMap.get(token);
    
                QName name = part.getName();
                List<WSSConstants.Action> actionList = properties.getActions();
    
                // Don't add a signed SAML Token as a part, as it will be automatically signed by WSS4J
                if (!((WSSConstants.TAG_saml_Assertion.equals(name) 
                    || WSSConstants.TAG_saml2_Assertion.equals(name))
                    && actionList != null && actionList.contains(WSSConstants.SAML_TOKEN_SIGNED))) {
                    properties.addSignaturePart(part);
                }
            }
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
        if (isRequestor()) {
            properties.setEnableSignatureConfirmationVerification(true);
        } else {
            properties.getActions().add(WSSConstants.SIGNATURE_CONFIRMATION);
        }
        
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
        AssertionInfo assertionInfo = getFirstAssertionByLocalname(aim, SPConstants.SIGNED_PARTS);
        if (assertionInfo != null) {
            parts = (SignedParts)assertionInfo.getAssertion();
            assertionInfo.setAsserted(true);
        }
        
        assertionInfo = getFirstAssertionByLocalname(aim, SPConstants.SIGNED_ELEMENTS);
        if (assertionInfo != null) {
            elements = (SignedElements)assertionInfo.getAssertion();
            assertionInfo.setAsserted(true);
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
            Attachments attachments = parts.getAttachments();
            if (attachments != null) {
                Modifier modifier = Modifier.Element;
                if (attachments.isContentSignatureTransform()) {
                    modifier = Modifier.Content;
                }
                SecurePart securePart = new SecurePart("cid:Attachments", modifier);
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
            
            Attachments attachments = parts.getAttachments();
            if (attachments != null) {
                SecurePart securePart = new SecurePart("cid:Attachments", Modifier.Element);
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
    
    protected static class TokenStoreCallbackHandler implements CallbackHandler {
        private CallbackHandler internal;
        private TokenStore store;
        public TokenStoreCallbackHandler(CallbackHandler in, TokenStore st) {
            internal = in;
            store = st;
        }
        
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
                
                String id = pc.getIdentifier();
                SecurityToken token = store.getToken(id);
                if (token != null) {
                    if (token.getSHA1() == null && pc.getKey() != null) {
                        token.setSHA1(getSHA1(pc.getKey()));
                        // Create another cache entry with the SHA1 Identifier as the key 
                        // for easy retrieval
                        store.add(token.getSHA1(), token);
                    }
                    pc.setKey(token.getSecret());
                    pc.setCustomToken(token.getToken());
                    return;
                }
            }
            if (internal != null) {
                internal.handle(callbacks);
            }
        }
    }
    
    protected org.apache.xml.security.stax.securityToken.SecurityToken 
    findInboundSecurityToken(SecurityEventConstants.Event event) throws XMLSecurityException {
        
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingEventList = 
            (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".in");
        if (incomingEventList != null) {
            for (SecurityEvent incomingEvent : incomingEventList) {
                if (event == incomingEvent.getSecurityEventType()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken token = 
                        ((TokenSecurityEvent<?>)incomingEvent).getSecurityToken();
                    return token;
                }
            }
        }
        return null;
    }
    
    // Signature + Signed SAML Token actions are not allowed together
    protected void removeSignatureIfSignedSAML() {
        if (properties.getActions() != null) {
            List<WSSConstants.Action> actionList = properties.getActions();
            if (actionList.contains(WSSConstants.SAML_TOKEN_SIGNED)
                && actionList.contains(WSSConstants.SIGNATURE)) {
                actionList.remove(WSSConstants.SIGNATURE);
            }
        }
    }
    
    // Put the Signature action before the SignatureConfirmation action
    protected void prependSignatureToSC() {
        if (properties.getActions() != null) {
            List<WSSConstants.Action> actionList = properties.getActions();
            boolean sigConf = actionList.contains(WSSConstants.SIGNATURE_CONFIRMATION);
            if (sigConf && actionList.contains(WSSConstants.SIGNATURE)) {
                actionList.remove(WSSConstants.SIGNATURE_CONFIRMATION);
                actionList.add(actionList.indexOf(WSSConstants.SIGNATURE) + 1, 
                               WSSConstants.SIGNATURE_CONFIRMATION);
            } else if (sigConf && actionList.contains(WSSConstants.SIGNATURE_WITH_DERIVED_KEY)) {
                actionList.remove(WSSConstants.SIGNATURE_CONFIRMATION);
                actionList.add(actionList.indexOf(WSSConstants.SIGNATURE_WITH_DERIVED_KEY) + 1, 
                               WSSConstants.SIGNATURE_CONFIRMATION);
            }
        }
    }
    
    // If we have EncryptBeforeSigning, then we want to have the Signature component after
    // the Encrypt action, which is not the case if we have a Signed SAML Supporting Token
    protected void enforceEncryptBeforeSigningWithSignedSAML() {
        if (properties.getActions() != null) {
            List<WSSConstants.Action> actionList = properties.getActions();
            if (actionList.contains(WSSConstants.SAML_TOKEN_SIGNED)) {
                actionList.remove(WSSConstants.SAML_TOKEN_SIGNED);
                actionList.add(WSSConstants.SAML_TOKEN_SIGNED);
            }
        }
    }
    
    // Reshuffle so that a IssuedToken/SecureConveration is above a Signature that references it
    protected void putCustomTokenAfterSignature() {
        if (properties.getActions() != null) {
            List<WSSConstants.Action> actionList = properties.getActions();
            if ((actionList.contains(WSSConstants.SIGNATURE)
                || actionList.contains(WSSConstants.SIGNATURE_WITH_DERIVED_KEY)
                || actionList.contains(WSSConstants.SIGNATURE_WITH_KERBEROS_TOKEN))
                && actionList.contains(WSSConstants.CUSTOM_TOKEN)) {
                getProperties().getActions().remove(WSSConstants.CUSTOM_TOKEN);
                getProperties().getActions().add(WSSConstants.CUSTOM_TOKEN);
            }
      
        }
    }
}
