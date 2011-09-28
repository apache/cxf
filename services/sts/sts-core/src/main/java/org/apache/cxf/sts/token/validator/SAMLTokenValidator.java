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
package org.apache.cxf.sts.token.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.realm.SAMLRealm;

import org.apache.ws.security.SAMLTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SignatureTrustValidator;
import org.apache.ws.security.validate.Validator;

/**
 * Validate a SAML Assertion. It is valid if it was issued and signed by this STS.
 */
public class SAMLTokenValidator implements TokenValidator {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SAMLTokenValidator.class);
    
    private Validator validator = new SignatureTrustValidator();
    
    private Map<String, SAMLRealm> realmMap = new HashMap<String, SAMLRealm>();
    
    /**
     * Set the WSS4J Validator instance to use to validate the token.
     * @param validator the WSS4J Validator instance to use to validate the token
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        if (realm != null && !realmMap.containsKey(realm)) {
            return false;
        }
        
        Object token = validateTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if ((WSConstants.SAML_NS.equals(namespace) || WSConstants.SAML2_NS.equals(namespace))
                && "Assertion".equals(localname)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating SAML Token");
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        ReceivedToken validateTarget = tokenRequirements.getValidateTarget();
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        Crypto sigCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

        RequestData requestData = new RequestData();
        requestData.setSigCrypto(sigCrypto);
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        requestData.setWssConfig(wssConfig);
        requestData.setCallbackHandler(callbackHandler);

        TokenValidatorResponse response = new TokenValidatorResponse();
        response.setValid(false);
        
        if (validateTarget == null || !validateTarget.isDOMElement()) {
            return response;
        }
        
        try {
            Element validateTargetElement = (Element)validateTarget.getToken();
            AssertionWrapper assertion = new AssertionWrapper(validateTargetElement);
            if (!assertion.isSigned()) {
                LOG.log(Level.WARNING, "The received assertion is not signed, and therefore not trusted");
                return response;
            }
            // Verify the signature
            assertion.verifySignature(
                requestData, new WSDocInfo(validateTargetElement.getOwnerDocument())
            );

            // Now verify trust on the signature
            Credential trustCredential = new Credential();
            SAMLKeyInfo samlKeyInfo = assertion.getSignatureKeyInfo();
            trustCredential.setPublicKey(samlKeyInfo.getPublicKey());
            trustCredential.setCertificates(samlKeyInfo.getCerts());

            validator.validate(trustCredential, requestData);

            // Finally check that the issuer is trusted
            String trustedIssuer = null;
            String assertionIssuer = assertion.getIssuerString();
            for (String realm : realmMap.keySet()) {
                SAMLRealm samlRealm = realmMap.get(realm);
                if (samlRealm.getIssuer().equals(assertionIssuer)) {
                    trustedIssuer = realm;
                    break;
                }
            }
            if (trustedIssuer == null && assertionIssuer.equals(stsProperties.getIssuer())) {
                trustedIssuer = stsProperties.getIssuer();
            }
            if (trustedIssuer != null) {
                response.setValid(true);
                SAMLTokenPrincipal samlPrincipal = new SAMLTokenPrincipal(assertion);
                response.setPrincipal(samlPrincipal);
                response.setTokenRealm(trustedIssuer);
            }
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }

        return response;
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
    
}
