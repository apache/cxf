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

package org.apache.cxf.sts.token.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.realm.SAMLRealm;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.SAMLParms;
import org.apache.ws.security.saml.ext.bean.AttributeStatementBean;
import org.apache.ws.security.saml.ext.bean.AuthDecisionStatementBean;
import org.apache.ws.security.saml.ext.bean.AuthenticationStatementBean;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.saml.ext.bean.SubjectBean;

/**
 * A TokenProvider implementation that provides a SAML Token.
 */
public class SAMLTokenProvider implements TokenProvider {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SAMLTokenProvider.class);
    
    private List<AttributeStatementProvider> attributeStatementProviders;
    private List<AuthenticationStatementProvider> authenticationStatementProviders;
    private List<AuthDecisionStatementProvider> authDecisionStatementProviders;
    private SubjectProvider subjectProvider = new DefaultSubjectProvider();
    private ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
    private boolean signToken = true;
    private Map<String, SAMLRealm> realmMap = new HashMap<String, SAMLRealm>();
    
    /**
     * Return true if this TokenProvider implementation is capable of providing a token
     * that corresponds to the given TokenType.
     */
    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }
    
    /**
     * Return true if this TokenProvider implementation is capable of providing a token
     * that corresponds to the given TokenType in a given realm.
     */
    public boolean canHandleToken(String tokenType, String realm) {
        if (realm != null && !realmMap.containsKey(realm)) {
            return false;
        }
        if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType) || WSConstants.SAML2_NS.equals(tokenType)
            || WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType) || WSConstants.SAML_NS.equals(tokenType)) {
            return true;
        }
        return false;
    }
    
    /**
     * Create a token given a TokenProviderParameters
     */
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        testKeyType(tokenParameters);
        byte[] secret = null;
        byte[] entropyBytes = null;
        long keySize = 0;
        boolean computedKey = false;
        KeyRequirements keyRequirements = tokenParameters.getKeyRequirements();
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        LOG.fine("Handling token of type: " + tokenRequirements.getTokenType());
        
        if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyRequirements.getKeyType())) {
            SymmetricKeyHandler keyHandler = new SymmetricKeyHandler(tokenParameters);
            keyHandler.createSymmetricKey();
            secret = keyHandler.getSecret();
            entropyBytes = keyHandler.getEntropyBytes();
            keySize = keyHandler.getKeySize();
            computedKey = keyHandler.isComputedKey();
        } 
        
        try {
            Document doc = DOMUtils.createDocument();
            AssertionWrapper assertion = createSamlToken(tokenParameters, secret, doc);
            Element token = assertion.toDOM(doc);
            
            // set the token in cache
            if (tokenParameters.getTokenStore() != null) {
                SecurityToken securityToken = new SecurityToken(assertion.getId());
                securityToken.setToken(token);
                securityToken.setPrincipal(tokenParameters.getPrincipal());
                int hash = 0;
                byte[] signatureValue = assertion.getSignatureValue();
                if (signatureValue != null && signatureValue.length > 0) {
                    hash = Arrays.hashCode(signatureValue);
                    securityToken.setAssociatedHash(hash);
                }
                if (tokenParameters.getRealm() != null) {
                    Properties props = securityToken.getProperties();
                    if (props == null) {
                        props = new Properties();
                    }
                    props.setProperty(STSConstants.TOKEN_REALM, tokenParameters.getRealm());
                    securityToken.setProperties(props);
                }
                Integer timeToLive = (int)(conditionsProvider.getLifetime() * 1000);
                tokenParameters.getTokenStore().add(securityToken, timeToLive);
            }
            
            TokenProviderResponse response = new TokenProviderResponse();
            response.setToken(token);
            String tokenType = tokenRequirements.getTokenType();
            if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType) 
                || WSConstants.SAML2_NS.equals(tokenType)) {
                response.setTokenId(token.getAttribute("ID"));
            } else {
                response.setTokenId(token.getAttribute("AssertionID"));
            }
            response.setLifetime(conditionsProvider.getLifetime());
            response.setEntropy(entropyBytes);
            if (keySize > 0) {
                response.setKeySize(keySize);
            }
            response.setComputedKey(computedKey);
            
            return response;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "", e);
            throw new STSException("Can't serialize SAML assertion", e, STSException.REQUEST_FAILED);
        }
    }
    
    /**
     * Set the List of AttributeStatementProviders.
     */
    public void setAttributeStatementProviders(
        List<AttributeStatementProvider> attributeStatementProviders
    ) {
        this.attributeStatementProviders = attributeStatementProviders;
    }
    
    /**
     * Get the List of AttributeStatementProviders.
     */
    public List<AttributeStatementProvider> getAttributeStatementProviders() {
        return attributeStatementProviders;
    }
    
    /**
     * Set the List of AuthenticationStatementProviders.
     */
    public void setAuthenticationStatementProviders(
        List<AuthenticationStatementProvider> authnStatementProviders
    ) {
        this.authenticationStatementProviders = authnStatementProviders;
    }
    
    /**
     * Get the List of AuthenticationStatementProviders.
     */
    public List<AuthenticationStatementProvider> getAuthenticationStatementProviders() {
        return authenticationStatementProviders;
    }
    
    /**
     * Set the List of AuthDecisionStatementProviders.
     */
    public void setAuthDecisionStatementProviders(
        List<AuthDecisionStatementProvider> authDecisionStatementProviders
    ) {
        this.authDecisionStatementProviders = authDecisionStatementProviders;
    }
    
    /**
     * Get the List of AuthDecisionStatementProviders.
     */
    public List<AuthDecisionStatementProvider> getAuthDecisionStatementProviders() {
        return authDecisionStatementProviders;
    }

    /**
     * Set the SubjectProvider.
     */
    public void setSubjectProvider(SubjectProvider subjectProvider) {
        this.subjectProvider = subjectProvider;
    }
    
    /**
     * Get the SubjectProvider.
     */
    public SubjectProvider getSubjectProvider() {
        return subjectProvider;
    }
    
    /**
     * Set the ConditionsProvider
     */
    public void setConditionsProvider(ConditionsProvider conditionsProvider) {
        this.conditionsProvider = conditionsProvider;
    }
    
    /**
     * Get the ConditionsProvider
     */
    public ConditionsProvider getConditionsProvider() {
        return conditionsProvider;
    }

    /**
     * Return whether the provided token will be signed or not. Default is true.
     */
    public boolean isSignToken() {
        return signToken;
    }

    /**
     * Set whether the provided token will be signed or not. Default is true.
     */
    public void setSignToken(boolean signToken) {
        this.signToken = signToken;
    }
    
    /**
     * Set the map of realm->SAMLRealm for this token provider
     * @param realms the map of realm->SAMLRealm for this token provider
     */
    public void setRealmMap(Map<String, SAMLRealm> realms) {
        this.realmMap = realms;
    }
    
    /**
     * Get the map of realm->SAMLRealm for this token provider
     * @return the map of realm->SAMLRealm for this token provider
     */
    public Map<String, SAMLRealm> getRealmMap() {
        return realmMap;
    }

    private AssertionWrapper createSamlToken(
        TokenProviderParameters tokenParameters, byte[] secret, Document doc
    ) throws Exception {
        String realm = tokenParameters.getRealm();
        SAMLRealm samlRealm = null;
        if (realm != null && realmMap.containsKey(realm)) {
            samlRealm = realmMap.get(realm);
        }
        
        SamlCallbackHandler handler = createCallbackHandler(tokenParameters, secret, samlRealm, doc);
        
        SAMLParms samlParms = new SAMLParms();
        samlParms.setCallbackHandler(handler);
        AssertionWrapper assertion = new AssertionWrapper(samlParms);
        
        if (signToken) {
            STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
            
            String alias = null;
            if (samlRealm != null) {
                alias = samlRealm.getSignatureAlias();
            }
            if (alias == null || "".equals(alias)) {
                alias = stsProperties.getSignatureUsername();
            }
            if (alias == null || "".equals(alias)) {
                Crypto signatureCrypto = stsProperties.getSignatureCrypto();
                if (signatureCrypto != null) {
                    alias = signatureCrypto.getDefaultX509Identifier();
                    LOG.fine("Signature alias is null so using default alias: " + alias);
                }
            }
            // Get the password
            WSPasswordCallback[] cb = {new WSPasswordCallback(alias, WSPasswordCallback.SIGNATURE)};
            LOG.fine("Creating SAML Token");
            stsProperties.getCallbackHandler().handle(cb);
            String password = cb[0].getPassword();
    
            LOG.fine("Signing SAML Token");
            boolean useKeyValue = stsProperties.getSignatureProperties().isUseKeyValue();
            assertion.signAssertion(alias, password, stsProperties.getSignatureCrypto(), useKeyValue);
        }
        
        return assertion;
    }
    
    public SamlCallbackHandler createCallbackHandler(
        TokenProviderParameters tokenParameters, byte[] secret, SAMLRealm samlRealm, Document doc
    ) throws Exception {
        // Parse the AttributeStatements
        List<AttributeStatementBean> attrBeanList = null;
        if (attributeStatementProviders != null && attributeStatementProviders.size() > 0) {
            attrBeanList = new ArrayList<AttributeStatementBean>();
            for (AttributeStatementProvider statementProvider : attributeStatementProviders) {
                AttributeStatementBean statementBean = statementProvider.getStatement(tokenParameters);
                if (statementBean != null) {
                    LOG.fine(
                        "AttributeStatements" + statementBean.toString() 
                        + "returned by AttributeStatementProvider " 
                        + statementProvider.getClass().getName()
                    );
                    attrBeanList.add(statementBean);
                }
            }
        }
        
        // Parse the AuthenticationStatements
        List<AuthenticationStatementBean> authBeanList = null;
        if (authenticationStatementProviders != null && authenticationStatementProviders.size() > 0) {
            authBeanList = new ArrayList<AuthenticationStatementBean>();
            for (AuthenticationStatementProvider statementProvider : authenticationStatementProviders) {
                AuthenticationStatementBean statementBean = 
                    statementProvider.getStatement(tokenParameters);
                if (statementBean != null) {
                    LOG.fine(
                        "AuthenticationStatement" + statementBean.toString() 
                        + "returned by AuthenticationStatementProvider " 
                        + statementProvider.getClass().getName()
                    );
                    authBeanList.add(statementBean);
                }
            }
        }
        
        // Parse the AuthDecisionStatements
        List<AuthDecisionStatementBean> authDecisionBeanList = null;
        if (authDecisionStatementProviders != null 
            && authDecisionStatementProviders.size() > 0) {
            authDecisionBeanList = new ArrayList<AuthDecisionStatementBean>();
            for (AuthDecisionStatementProvider statementProvider 
                : authDecisionStatementProviders) {
                AuthDecisionStatementBean statementBean = 
                    statementProvider.getStatement(tokenParameters);
                if (statementBean != null) {
                    LOG.fine(
                        "AuthDecisionStatement" + statementBean.toString() 
                        + "returned by AuthDecisionStatementProvider " 
                        + statementProvider.getClass().getName()
                    );
                    authDecisionBeanList.add(statementBean);
                }
            }
        }
        
        // If no statements, then default to the DefaultAttributeStatementProvider
        if ((attrBeanList == null || attrBeanList.isEmpty()) 
            && (authBeanList == null || authBeanList.isEmpty())
            && (authDecisionBeanList == null || authDecisionBeanList.isEmpty())) {
            attrBeanList = new ArrayList<AttributeStatementBean>();
            AttributeStatementProvider attributeProvider = new DefaultAttributeStatementProvider();
            AttributeStatementBean attributeBean = attributeProvider.getStatement(tokenParameters);
            attrBeanList.add(attributeBean);
        }
        
        // Get the Subject and Conditions
        SubjectBean subjectBean = subjectProvider.getSubject(tokenParameters, doc, secret);
        ConditionsBean conditionsBean = conditionsProvider.getConditions(tokenParameters);
        
        // Set all of the beans on the SamlCallbackHandler
        SamlCallbackHandler handler = new SamlCallbackHandler();
        handler.setTokenProviderParameters(tokenParameters);
        handler.setSubjectBean(subjectBean);
        handler.setConditionsBean(conditionsBean);
        handler.setAttributeBeans(attrBeanList);
        handler.setAuthenticationBeans(authBeanList);
        handler.setAuthDecisionStatementBeans(authDecisionBeanList);
        
        if (samlRealm != null) {
            handler.setIssuer(samlRealm.getIssuer());
        }
        
        return handler;
    }
    
    /**
     * Do some tests on the KeyType parameter.
     */
    private void testKeyType(TokenProviderParameters tokenParameters) {
        KeyRequirements keyRequirements = tokenParameters.getKeyRequirements();

        String keyType = keyRequirements.getKeyType();
        if (STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            if (keyRequirements.getCertificate() == null) {
                LOG.log(Level.WARNING, "A PublicKey Keytype is requested, but no certificate is provided");
                throw new STSException(
                    "No client certificate for PublicKey KeyType", STSException.INVALID_REQUEST
                );
            }
        } else if (!STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)
            && !STSConstants.BEARER_KEY_KEYTYPE.equals(keyType) && keyType != null) {
            LOG.log(Level.WARNING, "An unknown KeyType was requested: " + keyType);
            throw new STSException("Unknown KeyType", STSException.INVALID_REQUEST);
        }
        
    }
    
    
}
