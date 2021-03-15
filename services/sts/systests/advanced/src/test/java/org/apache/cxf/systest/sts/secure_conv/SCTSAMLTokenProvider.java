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

package org.apache.cxf.systest.sts.secure_conv;

import java.util.ArrayList;
import java.util.List;
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
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.ConditionsProvider;
import org.apache.cxf.sts.token.provider.DefaultAttributeStatementProvider;
import org.apache.cxf.sts.token.provider.DefaultConditionsProvider;
import org.apache.cxf.sts.token.provider.DefaultSubjectProvider;
import org.apache.cxf.sts.token.provider.SamlCallbackHandler;
import org.apache.cxf.sts.token.provider.SubjectProvider;
import org.apache.cxf.sts.token.provider.SubjectProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.SCTValidator;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;

/**
 * A TokenProvider implementation that provides a SAML Token that contains a Symmetric Key that is obtained
 * from the TokenProviderParameter properties.
 */
public class SCTSAMLTokenProvider implements TokenProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(SCTSAMLTokenProvider.class);

    private List<AttributeStatementProvider> attributeStatementProviders;
    private SubjectProvider subjectProvider = new DefaultSubjectProvider();
    private ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
    private boolean signToken = true;

    /**
     * Return true if this TokenProvider implementation is capable of providing a token
     * that corresponds to the given TokenType.
     */
    public boolean canHandleToken(String tokenType) {
        return WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType) || WSS4JConstants.SAML2_NS.equals(tokenType)
            || WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType) || WSS4JConstants.SAML_NS.equals(tokenType);
    }

    public boolean canHandleToken(String tokenType, String realm) {
        return canHandleToken(tokenType);
    }

    /**
     * Create a token given a TokenProviderParameters
     */
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        testKeyType(tokenParameters);
        byte[] entropyBytes = null;
        long keySize = 0;
        boolean computedKey = false;
        KeyRequirements keyRequirements = tokenParameters.getKeyRequirements();
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        LOG.fine("Handling token of type: " + tokenRequirements.getTokenType());

        keyRequirements.setKeyType(STSConstants.SYMMETRIC_KEY_KEYTYPE);
        byte[] secret = (byte[])tokenParameters.getAdditionalProperties().get(SCTValidator.SCT_VALIDATOR_SECRET);

        try {
            Document doc = DOMUtils.createDocument();
            SamlAssertionWrapper assertion = createSamlToken(tokenParameters, secret, doc);
            Element token = assertion.toDOM(doc);

            TokenProviderResponse response = new TokenProviderResponse();
            response.setToken(token);
            String tokenType = tokenRequirements.getTokenType();
            if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                    || WSS4JConstants.SAML2_NS.equals(tokenType)) {
                response.setTokenId(token.getAttributeNS(null, "ID"));
            } else {
                response.setTokenId(token.getAttributeNS(null, "AssertionID"));
            }

            response.setCreated(assertion.getNotBefore());
            response.setExpires(assertion.getNotOnOrAfter());

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
    public void setAttributeStatementProviders(List<AttributeStatementProvider> attributeStatementProviders) {
        this.attributeStatementProviders = attributeStatementProviders;
    }

    /**
     * Get the List of AttributeStatementProviders.
     */
    public List<AttributeStatementProvider> getAttributeStatementProviders() {
        return attributeStatementProviders;
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

    private SamlAssertionWrapper createSamlToken(
        TokenProviderParameters tokenParameters, byte[] secret, Document doc
    ) throws Exception {
        SamlCallbackHandler handler = createCallbackHandler(tokenParameters, secret, doc);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(handler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        if (signToken) {
            STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();

            // Get the password
            String alias = stsProperties.getSignatureUsername();
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
        TokenProviderParameters tokenParameters, byte[] secret, Document doc
    ) throws Exception {
        // Parse the AttributeStatements
        List<AttributeStatementBean> attrBeanList = null;
        if (attributeStatementProviders != null && !attributeStatementProviders.isEmpty()) {
            attrBeanList = new ArrayList<>();
            for (AttributeStatementProvider statementProvider : attributeStatementProviders) {
                AttributeStatementBean statementBean = statementProvider.getStatement(tokenParameters);
                if (statementBean != null) {
                    LOG.fine(
                        "AttributeStatements" + statementBean.toString()
                        + "returned by AttributeStatementProvider " + statementProvider.getClass().getName()
                    );
                    attrBeanList.add(statementBean);
                }
            }
        }

        // If no statements, then default to the DefaultAttributeStatementProvider
        if (attrBeanList == null || attrBeanList.isEmpty()) {
            attrBeanList = new ArrayList<>();
            AttributeStatementProvider attributeProvider = new DefaultAttributeStatementProvider();
            AttributeStatementBean attributeBean = attributeProvider.getStatement(tokenParameters);
            attrBeanList.add(attributeBean);
        }

        // Get the Subject and Conditions
        SubjectProviderParameters subjectProviderParameters = new SubjectProviderParameters();
        subjectProviderParameters.setProviderParameters(tokenParameters);
        subjectProviderParameters.setDoc(doc);
        subjectProviderParameters.setSecret(secret);
        subjectProviderParameters.setAttrBeanList(attrBeanList);
        SubjectBean subjectBean = subjectProvider.getSubject(subjectProviderParameters);

        ConditionsBean conditionsBean = conditionsProvider.getConditions(tokenParameters);

        // Set all of the beans on the SamlCallbackHandler
        SamlCallbackHandler handler = new SamlCallbackHandler();
        handler.setTokenProviderParameters(tokenParameters);
        handler.setSubjectBean(subjectBean);
        handler.setConditionsBean(conditionsBean);
        handler.setAttributeBeans(attrBeanList);

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
                || keyRequirements.getReceivedCredential().getX509Cert() == null) {
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
