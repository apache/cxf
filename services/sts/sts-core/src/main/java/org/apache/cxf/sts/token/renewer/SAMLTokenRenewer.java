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

package org.apache.cxf.sts.token.renewer;

import java.security.Principal;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.cache.CacheUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.provider.AbstractSAMLTokenProvider;
import org.apache.cxf.sts.token.provider.ConditionsProvider;
import org.apache.cxf.sts.token.provider.DefaultConditionsProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.builder.SAML1ComponentBuilder;
import org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.saml.DOMSAMLUtil;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Audience;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml.saml2.core.AudienceRestriction;

/**
 * A TokenRenewer implementation that renews a (valid or expired) SAML Token.
 */
public class SAMLTokenRenewer extends AbstractSAMLTokenProvider implements TokenRenewer {

    // The default maximum expired time a token is allowed to be is 30 minutes
    public static final long DEFAULT_MAX_EXPIRY = 60L * 30L;

    private static final Logger LOG = LogUtils.getL7dLogger(SAMLTokenRenewer.class);
    private boolean signToken = true;
    private ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
    private Map<String, RealmProperties> realmMap = new HashMap<>();
    private long maxExpiry = DEFAULT_MAX_EXPIRY;
    // boolean to enable/disable the check of proof of possession
    private boolean verifyProofOfPossession = true;
    private boolean allowRenewalAfterExpiry;

    /**
     * Return true if this TokenRenewer implementation is able to renew a token.
     */
    public boolean canHandleToken(ReceivedToken renewTarget) {
        return canHandleToken(renewTarget, null);
    }

    /**
     * Return true if this TokenRenewer implementation is able to renew a token in the given realm.
     */
    public boolean canHandleToken(ReceivedToken renewTarget, String realm) {
        if (realm != null && !realmMap.containsKey(realm)) {
            return false;
        }
        Object token = renewTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if ((WSS4JConstants.SAML_NS.equals(namespace) || WSS4JConstants.SAML2_NS.equals(namespace))
                && "Assertion".equals(localname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set whether proof of possession is required or not to renew a token
     */
    public void setVerifyProofOfPossession(boolean verifyProofOfPossession) {
        this.verifyProofOfPossession = verifyProofOfPossession;
    }

    /**
     * Get whether we allow renewal after expiry. The default is false.
     */
    public boolean isAllowRenewalAfterExpiry() {
        return allowRenewalAfterExpiry;
    }

    /**
     * Set whether we allow renewal after expiry. The default is false.
     */
    public void setAllowRenewalAfterExpiry(boolean allowRenewalAfterExpiry) {
        this.allowRenewalAfterExpiry = allowRenewalAfterExpiry;
    }

    /**
     * Set a new value (in seconds) for how long a token is allowed to be expired for before renewal.
     * The default is 30 minutes.
     */
    public void setMaxExpiry(long newExpiry) {
        maxExpiry = newExpiry;
    }

    /**
     * Get how long a token is allowed to be expired for before renewal (in seconds). The default is
     * 30 minutes.
     */
    public long getMaxExpiry() {
        return maxExpiry;
    }

    /**
     * Renew a token given a TokenRenewerParameters
     */
    public TokenRenewerResponse renewToken(TokenRenewerParameters tokenParameters) {
        TokenRenewerResponse response = new TokenRenewerResponse();
        ReceivedToken tokenToRenew = tokenParameters.getToken();
        if (tokenToRenew == null || tokenToRenew.getToken() == null
            || (tokenToRenew.getState() != STATE.EXPIRED && tokenToRenew.getState() != STATE.VALID)) {
            LOG.log(Level.WARNING, "The token to renew is null or invalid");
            throw new STSException(
                "The token to renew is null or invalid", STSException.INVALID_REQUEST
            );
        }

        TokenStore tokenStore = tokenParameters.getTokenStore();
        if (tokenStore == null) {
            LOG.log(Level.FINE, "A cache must be configured to use the SAMLTokenRenewer");
            throw new STSException("Can't renew SAML assertion", STSException.REQUEST_FAILED);
        }

        try {
            SamlAssertionWrapper assertion = new SamlAssertionWrapper((Element)tokenToRenew.getToken());

            byte[] oldSignature = assertion.getSignatureValue();
            int hash = Arrays.hashCode(oldSignature);
            SecurityToken cachedToken = tokenStore.getToken(Integer.toString(hash));
            if (cachedToken == null) {
                LOG.log(Level.FINE, "The token to be renewed must be stored in the cache");
                throw new STSException("Can't renew SAML assertion", STSException.REQUEST_FAILED);
            }

            // Validate the Assertion
            validateAssertion(assertion, tokenToRenew, cachedToken, tokenParameters);

            SamlAssertionWrapper renewedAssertion = new SamlAssertionWrapper(assertion.getSamlObject());
            String oldId = createNewId(renewedAssertion);
            // Remove the previous token (now expired) from the cache
            tokenStore.remove(oldId);
            tokenStore.remove(Integer.toString(hash));

            // Create new Conditions & sign the Assertion
            createNewConditions(renewedAssertion, tokenParameters);
            signAssertion(renewedAssertion, tokenParameters);

            Document doc = DOMUtils.createDocument();
            Element token = renewedAssertion.toDOM(doc);
            if (renewedAssertion.getSaml1() != null) {
                token.setIdAttributeNS(null, "AssertionID", true);
            } else {
                token.setIdAttributeNS(null, "ID", true);
            }
            doc.appendChild(token);

            // Cache the token
            storeTokenInCache(
                tokenStore, renewedAssertion, tokenParameters.getPrincipal(), tokenParameters
            );

            response.setToken(token);
            response.setTokenId(renewedAssertion.getId());

            final Instant validFrom;
            final Instant validTill;
            if (renewedAssertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
                validFrom = renewedAssertion.getSaml2().getConditions().getNotBefore();
                validTill = renewedAssertion.getSaml2().getConditions().getNotOnOrAfter();
            } else {
                validFrom = renewedAssertion.getSaml1().getConditions().getNotBefore();
                validTill = renewedAssertion.getSaml1().getConditions().getNotOnOrAfter();
            }
            response.setCreated(validFrom);
            response.setExpires(validTill);

            LOG.fine("SAML Token successfully renewed");
            return response;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "", ex);
            throw new STSException("Can't renew SAML assertion", ex, STSException.REQUEST_FAILED);
        }
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
        this.realmMap.putAll(realms);
    }

    /**
     * Get the map of realm->RealmProperties for this token provider
     * @return the map of realm->RealmProperties for this token provider
     */
    public Map<String, RealmProperties> getRealmMap() {
        return Collections.unmodifiableMap(realmMap);
    }

    private void validateAssertion(
        SamlAssertionWrapper assertion,
        ReceivedToken tokenToRenew,
        SecurityToken token,
        TokenRenewerParameters tokenParameters
    ) throws WSSecurityException {
        // Check the cached renewal properties
        Map<String, Object> props = token.getProperties();
        if (props == null) {
            LOG.log(Level.WARNING, "Error in getting properties from cached token");
            throw new STSException(
                "Error in getting properties from cached token", STSException.REQUEST_FAILED
            );
        }
        String isAllowRenewal = (String)props.get(STSConstants.TOKEN_RENEWING_ALLOW);
        String isAllowRenewalAfterExpiry =
            (String)props.get(STSConstants.TOKEN_RENEWING_ALLOW_AFTER_EXPIRY);

        if (isAllowRenewal == null || !Boolean.valueOf(isAllowRenewal)) {
            LOG.log(Level.WARNING, "The token is not allowed to be renewed");
            throw new STSException("The token is not allowed to be renewed", STSException.REQUEST_FAILED);
        }

        // Check to see whether the token has expired greater than the configured max expiry time
        if (tokenToRenew.getState() == STATE.EXPIRED) {
            if (!allowRenewalAfterExpiry || isAllowRenewalAfterExpiry == null
                || !Boolean.valueOf(isAllowRenewalAfterExpiry)) {
                LOG.log(Level.WARNING, "Renewal after expiry is not allowed");
                throw new STSException(
                    "Renewal after expiry is not allowed", STSException.REQUEST_FAILED
                );
            }
            Instant expiryDate = getExpiryDate(assertion);
            Instant currentDate = Instant.now();
            if ((currentDate.toEpochMilli() - expiryDate.toEpochMilli()) > (maxExpiry * 1000L)) {
                LOG.log(Level.WARNING, "The token expired too long ago to be renewed");
                throw new STSException(
                    "The token expired too long ago to be renewed", STSException.REQUEST_FAILED
                );
            }
        }

        // Verify Proof of Possession
        ProofOfPossessionValidator popValidator = new ProofOfPossessionValidator();
        if (verifyProofOfPossession) {
            STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
            Crypto sigCrypto = stsProperties.getSignatureCrypto();
            CallbackHandler callbackHandler = stsProperties.getCallbackHandler();
            RequestData requestData = new RequestData();
            requestData.setSigVerCrypto(sigCrypto);
            WSSConfig wssConfig = WSSConfig.getNewInstance();
            requestData.setWssConfig(wssConfig);

            WSDocInfo docInfo = new WSDocInfo(((Element)tokenToRenew.getToken()).getOwnerDocument());
            requestData.setWsDocInfo(docInfo);
            // Parse the HOK subject if it exists

            assertion.parseSubject(
                new WSSSAMLKeyInfoProcessor(requestData), sigCrypto, callbackHandler
            );

            SAMLKeyInfo keyInfo = assertion.getSubjectKeyInfo();
            if (keyInfo == null) {
                keyInfo = new SAMLKeyInfo((byte[])null);
            }
            if (!popValidator.checkProofOfPossession(tokenParameters, keyInfo)) {
                throw new STSException(
                    "Failed to verify the proof of possession of the key associated with the "
                    + "saml token. No matching key found in the request.",
                    STSException.INVALID_REQUEST
                );
            }
        }

        // Check the AppliesTo address
        String appliesToAddress = tokenParameters.getAppliesToAddress();
        if (appliesToAddress != null) {
            if (assertion.getSaml1() != null) {
                List<AudienceRestrictionCondition> restrConditions =
                    assertion.getSaml1().getConditions().getAudienceRestrictionConditions();
                if (!matchSaml1AudienceRestriction(appliesToAddress, restrConditions)) {
                    LOG.log(Level.WARNING, "The AppliesTo address does not match the Audience Restriction");
                    throw new STSException(
                        "The AppliesTo address does not match the Audience Restriction",
                        STSException.INVALID_REQUEST
                    );
                }
            } else {
                List<AudienceRestriction> audienceRestrs =
                    assertion.getSaml2().getConditions().getAudienceRestrictions();
                if (!matchSaml2AudienceRestriction(appliesToAddress, audienceRestrs)) {
                    LOG.log(Level.WARNING, "The AppliesTo address does not match the Audience Restriction");
                    throw new STSException(
                        "The AppliesTo address does not match the Audience Restriction",
                        STSException.INVALID_REQUEST
                    );
                }
            }
        }

    }

    private boolean matchSaml1AudienceRestriction(
        String appliesTo, List<AudienceRestrictionCondition> restrConditions
    ) {
        boolean found = false;
        if (restrConditions != null && !restrConditions.isEmpty()) {
            for (AudienceRestrictionCondition restrCondition : restrConditions) {
                if (restrCondition.getAudiences() != null) {
                    for (Audience audience : restrCondition.getAudiences()) {
                        if (appliesTo.equals(audience.getUri())) {
                            return true;
                        }
                    }
                }
            }
        }

        return found;
    }

    private boolean matchSaml2AudienceRestriction(
        String appliesTo, List<AudienceRestriction> audienceRestrictions
    ) {
        boolean found = false;
        if (audienceRestrictions != null && !audienceRestrictions.isEmpty()) {
            for (AudienceRestriction audienceRestriction : audienceRestrictions) {
                if (audienceRestriction.getAudiences() != null) {
                    for (org.opensaml.saml.saml2.core.Audience audience : audienceRestriction.getAudiences()) {
                        if (appliesTo.equals(audience.getAudienceURI())) {
                            return true;
                        }
                    }
                }
            }
        }

        return found;
    }

    private void signAssertion(
        SamlAssertionWrapper assertion,
        TokenRenewerParameters tokenParameters
    ) throws Exception {
        if (signToken) {
            STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
            String realm = tokenParameters.getRealm();
            RealmProperties samlRealm = null;
            if (realm != null) {
                samlRealm = realmMap.get(realm);
            }

            signToken(assertion, samlRealm, stsProperties, tokenParameters.getKeyRequirements());
        } else {
            if (assertion.getSaml1().getSignature() != null) {
                assertion.getSaml1().setSignature(null);
            } else if (assertion.getSaml2().getSignature() != null) {
                assertion.getSaml2().setSignature(null);
            }
        }

    }

    private void createNewConditions(SamlAssertionWrapper assertion, TokenRenewerParameters tokenParameters) {
        ConditionsBean conditions =
            conditionsProvider.getConditions(convertToProviderParameters(tokenParameters));

        if (assertion.getSaml1() != null) {
            org.opensaml.saml.saml1.core.Assertion saml1Assertion = assertion.getSaml1();
            saml1Assertion.setIssueInstant(Instant.now());

            org.opensaml.saml.saml1.core.Conditions saml1Conditions =
                SAML1ComponentBuilder.createSamlv1Conditions(conditions);

            saml1Assertion.setConditions(saml1Conditions);
        } else {
            org.opensaml.saml.saml2.core.Assertion saml2Assertion = assertion.getSaml2();
            saml2Assertion.setIssueInstant(Instant.now());

            org.opensaml.saml.saml2.core.Conditions saml2Conditions =
                SAML2ComponentBuilder.createConditions(conditions);

            saml2Assertion.setConditions(saml2Conditions);
        }
    }

    private TokenProviderParameters convertToProviderParameters(
        TokenRenewerParameters renewerParameters
    ) {
        TokenProviderParameters providerParameters = new TokenProviderParameters();
        providerParameters.setAppliesToAddress(renewerParameters.getAppliesToAddress());
        providerParameters.setEncryptionProperties(renewerParameters.getEncryptionProperties());
        providerParameters.setKeyRequirements(renewerParameters.getKeyRequirements());
        providerParameters.setPrincipal(renewerParameters.getPrincipal());
        providerParameters.setRealm(renewerParameters.getRealm());
        providerParameters.setStsProperties(renewerParameters.getStsProperties());
        providerParameters.setTokenRequirements(renewerParameters.getTokenRequirements());
        providerParameters.setTokenStore(renewerParameters.getTokenStore());
        providerParameters.setMessageContext(renewerParameters.getMessageContext());

        // Store token to renew in the additional properties in case you want to base some
        // Conditions on the token
        Map<String, Object> additionalProperties = renewerParameters.getAdditionalProperties();
        if (additionalProperties == null) {
            additionalProperties = new HashMap<>(1);
        }
        additionalProperties.put(ReceivedToken.class.getName(), renewerParameters.getToken());
        providerParameters.setAdditionalProperties(additionalProperties);

        return providerParameters;
    }

    private String createNewId(SamlAssertionWrapper assertion) {
        if (assertion.getSaml1() != null) {
            org.opensaml.saml.saml1.core.Assertion saml1Assertion = assertion.getSaml1();
            String oldId = saml1Assertion.getID();
            saml1Assertion.setID(IDGenerator.generateID("_"));

            return oldId;
        }
        org.opensaml.saml.saml2.core.Assertion saml2Assertion = assertion.getSaml2();
        String oldId = saml2Assertion.getID();
        saml2Assertion.setID(IDGenerator.generateID("_"));

        return oldId;
    }

    private void storeTokenInCache(
        TokenStore tokenStore,
        SamlAssertionWrapper assertion,
        Principal principal,
        TokenRenewerParameters tokenParameters
    ) throws WSSecurityException {
        // Store the successfully renewed token in the cache
        byte[] signatureValue = assertion.getSignatureValue();
        if (tokenStore != null && signatureValue != null && signatureValue.length > 0) {

            SecurityToken securityToken =
                CacheUtils.createSecurityTokenForStorage(assertion.getElement(), assertion.getId(),
                    assertion.getNotOnOrAfter(), tokenParameters.getPrincipal(), tokenParameters.getRealm(),
                    tokenParameters.getTokenRequirements().getRenewing());
            CacheUtils.storeTokenInCache(
                securityToken, tokenParameters.getTokenStore(), signatureValue);
        }
    }


    private Instant getExpiryDate(SamlAssertionWrapper assertion) {
        if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            return assertion.getSaml2().getConditions().getNotOnOrAfter();
        }
        return assertion.getSaml1().getConditions().getNotOnOrAfter();
    }

    private static class ProofOfPossessionValidator {

        public boolean checkProofOfPossession(
            TokenRenewerParameters tokenParameters,
            SAMLKeyInfo subjectKeyInfo
        ) {
            Map<String, Object> messageContext = tokenParameters.getMessageContext();
            final List<WSHandlerResult> handlerResults =
                CastUtils.cast((List<?>) messageContext.get(WSHandlerConstants.RECV_RESULTS));

            List<WSSecurityEngineResult> signedResults = new ArrayList<>();
            if (handlerResults != null && !handlerResults.isEmpty()) {
                WSHandlerResult handlerResult = handlerResults.get(0);

                if (handlerResult.getActionResults().containsKey(WSConstants.SIGN)) {
                    signedResults.addAll(handlerResult.getActionResults().get(WSConstants.SIGN));
                }
                if (handlerResult.getActionResults().containsKey(WSConstants.UT_SIGN)) {
                    signedResults.addAll(handlerResult.getActionResults().get(WSConstants.UT_SIGN));
                }
            }

            TLSSessionInfo tlsInfo = (TLSSessionInfo)messageContext.get(TLSSessionInfo.class.getName());
            Certificate[] tlsCerts = null;
            if (tlsInfo != null) {
                tlsCerts = tlsInfo.getPeerCertificates();
            }

            return DOMSAMLUtil.compareCredentials(subjectKeyInfo, signedResults, tlsCerts);
        }
    }
}
