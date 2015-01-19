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

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.realm.CertConstraintsParser;
import org.apache.cxf.sts.token.realm.SAMLRealmCodec;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
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
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.ValidatorSuite;

/**
 * Validate a SAML Assertion. It is valid if it was issued and signed by this STS.
 */
public class SAMLTokenValidator implements TokenValidator {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SAMLTokenValidator.class);
    
    private Validator validator = new SignatureTrustValidator();
    
    private CertConstraintsParser certConstraints = new CertConstraintsParser();
    
    private SAMLRealmCodec samlRealmCodec;
    
    private SAMLRoleParser samlRoleParser = new DefaultSAMLRoleParser();
    
    /**
     * Whether to validate the signature of the Assertion (if it exists) against the 
     * relevant profile. Default is true.
     */
    private boolean validateSignatureAgainstProfile = true;
    
    /**
     * Set a list of Strings corresponding to regular expression constraints on the subject DN
     * of a certificate that was used to sign a received Assertion
     */
    public void setSubjectConstraints(List<String> subjectConstraints) {
        certConstraints.setSubjectConstraints(subjectConstraints);
    }
    
    /**
     * Set the WSS4J Validator instance to use to validate the token.
     * @param validator the WSS4J Validator instance to use to validate the token
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }
    
    /**
     * Set the SAMLRealmCodec instance to use to return a realm from a validated token
     * @param samlRealmCodec the SAMLRealmCodec instance to use to return a realm from a validated token
     */
    public void setSamlRealmCodec(SAMLRealmCodec samlRealmCodec) {
        this.samlRealmCodec = samlRealmCodec;
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
     * ReceivedToken argument. The realm is ignored in this Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
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
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        Crypto sigCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();
        
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(STATE.INVALID);
        response.setToken(validateTarget);
        
        if (!validateTarget.isDOMElement()) {
            return response;
        }
        
        try {
            Element validateTargetElement = (Element)validateTarget.getToken();
            AssertionWrapper assertion = new AssertionWrapper(validateTargetElement);
            
            SAMLTokenPrincipal samlPrincipal = new SAMLTokenPrincipal(assertion);
            response.setPrincipal(samlPrincipal);
            
            if (!assertion.isSigned()) {
                LOG.log(Level.WARNING, "The received assertion is not signed, and therefore not trusted");
                return response;
            }
                
            RequestData requestData = new RequestData();
            requestData.setSigCrypto(sigCrypto);
            WSSConfig wssConfig = WSSConfig.getNewInstance();
            requestData.setWssConfig(wssConfig);
            requestData.setCallbackHandler(callbackHandler);
            requestData.setMsgContext(tokenParameters.getWebServiceContext().getMessageContext());
            requestData.setSubjectCertConstraints(certConstraints.getCompiledSubjectContraints());

            // Verify the signature
            assertion.verifySignature(
                requestData, new WSDocInfo(validateTargetElement.getOwnerDocument())
            );
                
            SecurityToken secToken = null;
            byte[] signatureValue = assertion.getSignatureValue();
            if (tokenParameters.getTokenStore() != null && signatureValue != null
                && signatureValue.length > 0) {
                int hash = Arrays.hashCode(signatureValue);
                secToken = tokenParameters.getTokenStore().getToken(Integer.toString(hash));
                if (secToken != null && secToken.getTokenHash() != hash) {
                    secToken = null;
                }
            }
            if (secToken != null && secToken.isExpired()) {
                LOG.fine("Token: " + secToken.getId() + " is in the cache but expired - revalidating");
                secToken = null;
            }
            
            if (secToken == null) {
                // Validate the assertion against schemas/profiles
                validateAssertion(assertion);

                // Now verify trust on the signature
                Credential trustCredential = new Credential();
                SAMLKeyInfo samlKeyInfo = assertion.getSignatureKeyInfo();
                trustCredential.setPublicKey(samlKeyInfo.getPublicKey());
                trustCredential.setCertificates(samlKeyInfo.getCerts());
    
                trustCredential = validator.validate(trustCredential, requestData);

                // Finally check that subject DN of the signing certificate matches a known constraint
                X509Certificate cert = null;
                if (trustCredential.getCertificates() != null) {
                    cert = trustCredential.getCertificates()[0];
                }
                
                if (!certConstraints.matches(cert)) {
                    return response;
                }
            }
            
            // Parse roles from the validated token
            if (samlRoleParser != null) {
                Set<Principal> roles = 
                    samlRoleParser.parseRolesFromAssertion(samlPrincipal, null, assertion);
                response.setRoles(roles);
            }
           
            // Get the realm of the SAML token
            String tokenRealm = null;
            if (samlRealmCodec != null) {
                tokenRealm = samlRealmCodec.getRealmFromToken(assertion);
                // verify the realm against the cached token
                if (secToken != null) {
                    Properties props = secToken.getProperties();
                    if (props != null) {
                        String cachedRealm = props.getProperty(STSConstants.TOKEN_REALM);
                        if (cachedRealm != null && !tokenRealm.equals(cachedRealm)) {
                            return response;
                        }
                    }
                }
            }
            response.setTokenRealm(tokenRealm);
            
            if (!validateConditions(assertion, validateTarget)) {
                return response;
            }
            
            // Store the successfully validated token in the cache
            if (secToken == null) {
                storeTokenInCache(
                    tokenParameters.getTokenStore(), assertion, tokenParameters.getPrincipal(), tokenRealm
                );
            }
            
            // Add the AssertionWrapper to the properties, as the claims are required to be transformed
            Map<String, Object> addProps = new HashMap<String, Object>();
            addProps.put(AssertionWrapper.class.getName(), assertion);
            response.setAdditionalProperties(addProps);
            
            validateTarget.setState(STATE.VALID);
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }

        return response;
    }
    
    /**
     * Validate the assertion against schemas/profiles
     */
    protected void validateAssertion(AssertionWrapper assertion) throws WSSecurityException {
        if (validateSignatureAgainstProfile) {
            assertion.validateSignatureAgainstProfile();
        }
        
        if (assertion.getSaml1() != null) {
            ValidatorSuite schemaValidators = 
                org.opensaml.Configuration.getValidatorSuite("saml1-schema-validator");
            ValidatorSuite specValidators = 
                org.opensaml.Configuration.getValidatorSuite("saml1-spec-validator");
            try {
                schemaValidators.validate(assertion.getSaml1());
                specValidators.validate(assertion.getSaml1());
            } catch (ValidationException e) {
                LOG.fine("Saml Validation error: " + e.getMessage());
                throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
            }
        } else if (assertion.getSaml2() != null) {
            ValidatorSuite schemaValidators = 
                org.opensaml.Configuration.getValidatorSuite("saml2-core-schema-validator");
            ValidatorSuite specValidators = 
                org.opensaml.Configuration.getValidatorSuite("saml2-core-spec-validator");
            try {
                schemaValidators.validate(assertion.getSaml2());
                specValidators.validate(assertion.getSaml2());
            } catch (ValidationException e) {
                LOG.fine("Saml Validation error: " + e.getMessage());
                throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
            }
        }
    }
    
    protected boolean validateConditions(
        AssertionWrapper assertion, ReceivedToken validateTarget
    ) {
        DateTime validFrom = null;
        DateTime validTill = null;
        DateTime issueInstant = null;
        if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            validFrom = assertion.getSaml2().getConditions().getNotBefore();
            validTill = assertion.getSaml2().getConditions().getNotOnOrAfter();
            issueInstant = assertion.getSaml2().getIssueInstant();
        } else {
            validFrom = assertion.getSaml1().getConditions().getNotBefore();
            validTill = assertion.getSaml1().getConditions().getNotOnOrAfter();
            issueInstant = assertion.getSaml1().getIssueInstant();
        }
        
        if (validFrom != null && validFrom.isAfterNow()) {
            LOG.log(Level.WARNING, "SAML Token condition not met");
            return false;
        } else if (validTill != null && validTill.isBeforeNow()) {
            LOG.log(Level.WARNING, "SAML Token condition not met");
            validateTarget.setState(STATE.EXPIRED);
            return false;
        }
        
        if (issueInstant != null && issueInstant.isAfterNow()) {
            LOG.log(Level.WARNING, "SAML Token IssueInstant not met");
            return false;
        }
        
        return true;
    }
    
    private void storeTokenInCache(
        TokenStore tokenStore, 
        AssertionWrapper assertion, 
        Principal principal,
        String tokenRealm
    ) throws WSSecurityException {
        // Store the successfully validated token in the cache
        byte[] signatureValue = assertion.getSignatureValue();
        if (tokenStore != null && signatureValue != null && signatureValue.length > 0) {
            DateTime validTill = null;
            if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
                validTill = assertion.getSaml2().getConditions().getNotOnOrAfter();
            } else {
                validTill = assertion.getSaml1().getConditions().getNotOnOrAfter();
            }
            
            SecurityToken securityToken = new SecurityToken(assertion.getId(), null, validTill.toDate());
            securityToken.setToken(assertion.getElement());
            securityToken.setPrincipal(principal);
            
            if (tokenRealm != null) {
                Properties props = new Properties();
                props.setProperty(STSConstants.TOKEN_REALM, tokenRealm);
                securityToken.setProperties(props);
            }

            int hash = Arrays.hashCode(signatureValue);
            securityToken.setTokenHash(hash);
            String identifier = Integer.toString(hash);
            tokenStore.add(identifier, securityToken);
        }
    }

    public SAMLRoleParser getSamlRoleParser() {
        return samlRoleParser;
    }

    public void setSamlRoleParser(SAMLRoleParser samlRoleParser) {
        this.samlRoleParser = samlRoleParser;
    }
    
    /**
     * Whether to validate the signature of the Assertion (if it exists) against the 
     * relevant profile. Default is true.
     */
    public boolean isValidateSignatureAgainstProfile() {
        return validateSignatureAgainstProfile;
    }

    /**
     * Whether to validate the signature of the Assertion (if it exists) against the 
     * relevant profile. Default is true.
     */
    public void setValidateSignatureAgainstProfile(boolean validateSignatureAgainstProfile) {
        this.validateSignatureAgainstProfile = validateSignatureAgainstProfile;
    }
}
