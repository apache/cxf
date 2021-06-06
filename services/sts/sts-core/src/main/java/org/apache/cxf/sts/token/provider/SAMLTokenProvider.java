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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.cache.CacheUtils;
import org.apache.cxf.sts.claims.ClaimsAttributeStatementProvider;
import org.apache.cxf.sts.claims.CombinedClaimsAttributeStatementProvider;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.joda.time.DateTime;
import org.opensaml.saml.common.SAMLVersion;

/**
 * A TokenProvider implementation that provides a SAML Token.
 */
public class SAMLTokenProvider extends AbstractSAMLTokenProvider implements TokenProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(SAMLTokenProvider.class);

    private List<AttributeStatementProvider> attributeStatementProviders;
    private List<AuthenticationStatementProvider> authenticationStatementProviders;
    private List<AuthDecisionStatementProvider> authDecisionStatementProviders;
    private SubjectProvider subjectProvider = new DefaultSubjectProvider();
    private ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
    private boolean signToken = true;
    private Map<String, RealmProperties> realmMap = new HashMap<>();
    private SamlCustomHandler samlCustomHandler;
    private boolean combineClaimAttributes = true;

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
        return WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType) || WSS4JConstants.SAML2_NS.equals(tokenType)
            || WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType) || WSS4JConstants.SAML_NS.equals(tokenType);
    }

    /**
     * Create a token given a TokenProviderParameters
     */
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        testKeyType(tokenParameters);
        KeyRequirements keyRequirements = tokenParameters.getKeyRequirements();
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Handling token of type: " + tokenRequirements.getTokenType());
        }

        byte[] secret = null;
        byte[] entropyBytes = null;
        long keySize = 0;
        boolean computedKey = false;
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
            SamlAssertionWrapper assertion = createSamlToken(tokenParameters, secret, doc);
            Element token = assertion.toDOM(doc);

            // set the token in cache (only if the token is signed)
            byte[] signatureValue = assertion.getSignatureValue();
            if (tokenParameters.getTokenStore() != null && signatureValue != null
                && signatureValue.length > 0) {

                SecurityToken securityToken =
                    CacheUtils.createSecurityTokenForStorage(token, assertion.getId(),
                        assertion.getNotOnOrAfter(), tokenParameters.getPrincipal(), tokenParameters.getRealm(),
                        tokenParameters.getTokenRequirements().getRenewing());
                CacheUtils.storeTokenInCache(
                    securityToken, tokenParameters.getTokenStore(), signatureValue);
            }

            TokenProviderResponse response = new TokenProviderResponse();

            String tokenType = tokenRequirements.getTokenType();
            if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                || WSS4JConstants.SAML2_NS.equals(tokenType)) {
                response.setTokenId(token.getAttributeNS(null, "ID"));
            } else {
                response.setTokenId(token.getAttributeNS(null, "AssertionID"));
            }

            if (tokenParameters.isEncryptToken()) {
                token = TokenProviderUtils.encryptToken(token, response.getTokenId(),
                                                        tokenParameters.getStsProperties(),
                                                        tokenParameters.getEncryptionProperties(),
                                                        keyRequirements,
                                                        tokenParameters.getMessageContext());
            }
            response.setToken(token);

            final DateTime validFrom;
            final DateTime validTill;
            if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
                validFrom = assertion.getSaml2().getConditions().getNotBefore();
                validTill = assertion.getSaml2().getConditions().getNotOnOrAfter();
            } else {
                validFrom = assertion.getSaml1().getConditions().getNotBefore();
                validTill = assertion.getSaml1().getConditions().getNotOnOrAfter();
            }
            response.setCreated(validFrom.toDate().toInstant());
            response.setExpires(validTill.toDate().toInstant());

            response.setEntropy(entropyBytes);
            if (keySize > 0) {
                response.setKeySize(keySize);
            }
            response.setComputedKey(computedKey);

            LOG.fine("SAML Token successfully created");
            if (secret != null) {
                Arrays.fill(secret, (byte) 0);
            }
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
     * Set the map of realm->RealmProperties for this token provider
     * @param realms the map of realm->RealmProperties for this token provider
     */
    public void setRealmMap(Map<String, ? extends RealmProperties> realms) {
        this.realmMap.clear();
        if (realms != null) {
            this.realmMap.putAll(realms);
        }
    }

    /**
     * Get the map of realm->RealmProperties for this token provider
     * @return the map of realm->RealmProperties for this token provider
     */
    public Map<String, RealmProperties> getRealmMap() {
        return Collections.unmodifiableMap(realmMap);
    }

    public void setSamlCustomHandler(SamlCustomHandler samlCustomHandler) {
        this.samlCustomHandler = samlCustomHandler;
    }

    private SamlAssertionWrapper createSamlToken(
        TokenProviderParameters tokenParameters, byte[] secret, Document doc
    ) throws Exception {
        String realm = tokenParameters.getRealm();
        RealmProperties samlRealm = null;
        if (realm != null && realmMap.containsKey(realm)) {
            samlRealm = realmMap.get(realm);
        }

        SamlCallbackHandler handler = createCallbackHandler(tokenParameters, secret, samlRealm, doc);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(handler, samlCallback);

        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        if (samlCustomHandler != null) {
            samlCustomHandler.handle(assertion, tokenParameters);
        }

        if (signToken) {
            STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
            signToken(assertion, samlRealm, stsProperties, tokenParameters.getKeyRequirements());
        }

        return assertion;
    }

    public SamlCallbackHandler createCallbackHandler(
        TokenProviderParameters tokenParameters, byte[] secret, RealmProperties samlRealm, Document doc
    ) throws Exception {
        boolean statementAdded = false;

        // Parse the AttributeStatements
        List<AttributeStatementBean> attrBeanList = null;
        if (attributeStatementProviders != null && !attributeStatementProviders.isEmpty()) {
            attrBeanList = new ArrayList<>();
            for (AttributeStatementProvider statementProvider : attributeStatementProviders) {
                AttributeStatementBean statementBean = statementProvider.getStatement(tokenParameters);
                if (statementBean != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(
                            "AttributeStatements " + statementBean.toString()
                            + " returned by AttributeStatementProvider "
                            + statementProvider.getClass().getName()
                        );
                    }
                    attrBeanList.add(statementBean);
                    statementAdded = true;
                }
            }
        }

        // Parse the AuthenticationStatements
        List<AuthenticationStatementBean> authBeanList = null;
        if (authenticationStatementProviders != null && !authenticationStatementProviders.isEmpty()) {
            authBeanList = new ArrayList<>();
            for (AuthenticationStatementProvider statementProvider : authenticationStatementProviders) {
                AuthenticationStatementBean statementBean = statementProvider.getStatement(tokenParameters);
                if (statementBean != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(
                            "AuthenticationStatement " + statementBean.toString()
                            + " returned by AuthenticationStatementProvider "
                            + statementProvider.getClass().getName()
                        );
                    }
                    authBeanList.add(statementBean);
                    statementAdded = true;
                }
            }
        }

        // Parse the AuthDecisionStatements
        List<AuthDecisionStatementBean> authDecisionBeanList = null;
        if (authDecisionStatementProviders != null
            && !authDecisionStatementProviders.isEmpty()) {
            authDecisionBeanList = new ArrayList<>();
            for (AuthDecisionStatementProvider statementProvider : authDecisionStatementProviders) {
                AuthDecisionStatementBean statementBean = statementProvider.getStatement(tokenParameters);
                if (statementBean != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(
                            "AuthDecisionStatement " + statementBean.toString()
                            + " returned by AuthDecisionStatementProvider "
                            + statementProvider.getClass().getName()
                        );
                    }
                    authDecisionBeanList.add(statementBean);
                    statementAdded = true;
                }
            }
        }

        // If no providers have been configured, then default to the ClaimsAttributeStatementProvider
        // If no Claims are available then use the DefaultAttributeStatementProvider
        // Also handle "ActAs" via the ActAsAttributeStatementProvider
        if (!statementAdded) {
            attrBeanList = new ArrayList<>();
            AttributeStatementProvider attributeProvider;
            if (combineClaimAttributes) {
                attributeProvider = new CombinedClaimsAttributeStatementProvider();
            } else {
                attributeProvider = new ClaimsAttributeStatementProvider();
            }

            AttributeStatementBean attributeBean = attributeProvider.getStatement(tokenParameters);
            if (attributeBean != null && attributeBean.getSamlAttributes() != null
                && !attributeBean.getSamlAttributes().isEmpty()) {
                attrBeanList.add(attributeBean);
            } else {
                attributeProvider = new DefaultAttributeStatementProvider();
                attributeBean = attributeProvider.getStatement(tokenParameters);
                attrBeanList.add(attributeBean);
            }

            attributeProvider = new ActAsAttributeStatementProvider();
            attributeBean = attributeProvider.getStatement(tokenParameters);
            if (attributeBean != null && attributeBean.getSamlAttributes() != null
                && !attributeBean.getSamlAttributes().isEmpty()) {
                attrBeanList.add(attributeBean);
            }
        }

        // Get the Subject and Conditions
        SubjectProviderParameters subjectProviderParameters = new SubjectProviderParameters();
        subjectProviderParameters.setProviderParameters(tokenParameters);
        subjectProviderParameters.setDoc(doc);
        subjectProviderParameters.setSecret(secret);
        subjectProviderParameters.setAttrBeanList(attrBeanList);
        subjectProviderParameters.setAuthBeanList(authBeanList);
        subjectProviderParameters.setAuthDecisionBeanList(authDecisionBeanList);
        SubjectBean subjectBean = subjectProvider.getSubject(subjectProviderParameters);

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
            if (keyRequirements.getReceivedCredential() == null
                || (keyRequirements.getReceivedCredential().getX509Cert() == null
                    && keyRequirements.getReceivedCredential().getPublicKey() == null)) {
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

    public boolean isCombineClaimAttributes() {
        return combineClaimAttributes;
    }

    public void setCombineClaimAttributes(boolean combineClaimAttributes) {
        this.combineClaimAttributes = combineClaimAttributes;
    }


}
