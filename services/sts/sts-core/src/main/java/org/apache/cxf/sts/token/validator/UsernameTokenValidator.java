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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.realm.UsernameTokenRealmCodec;

import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;

import org.apache.ws.security.CustomTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;

/**
 * This class validates a wsse UsernameToken.
 */
public class UsernameTokenValidator implements TokenValidator {
    
    private static final Logger LOG = LogUtils.getL7dLogger(UsernameTokenValidator.class);
    
    private Validator validator = new org.apache.ws.security.validate.UsernameTokenValidator();
    
    private UsernameTokenRealmCodec usernameTokenRealmCodec;
    
    /**
     * Set the WSS4J Validator instance to use to validate the token.
     * @param validator the WSS4J Validator instance to use to validate the token
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }
    
    /**
     * Set the UsernameTokenRealmCodec instance to use to return a realm from a validated token
     * @param usernameTokenRealmCodec the UsernameTokenRealmCodec instance to use to return a 
     *                                realm from a validated token
     */
    public void setUsernameTokenRealmCodec(UsernameTokenRealmCodec usernameTokenRealmCodec) {
        this.usernameTokenRealmCodec = usernameTokenRealmCodec;
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
     * ReceivedToken argument. The realm is ignored in this token Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        if (validateTarget.getToken() instanceof UsernameTokenType) {
            return true;
        }
        return false;
    }
    
    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating UsernameToken");
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
        
        if (validateTarget == null || !validateTarget.isUsernameToken()) {
            return response;
        }
        
        //
        // Turn the JAXB UsernameTokenType into a DOM Element for validation
        //
        UsernameTokenType usernameTokenType = (UsernameTokenType)validateTarget.getToken();
        
        SecurityToken secToken = null;
        if (tokenParameters.getTokenStore() != null) {
            secToken = tokenParameters.getTokenStore().getToken(usernameTokenType.getId());
        }

        // Marshall the received JAXB object into a DOM Element
        Element usernameTokenElement = null;
        try {
            JAXBContext jaxbContext = 
                JAXBContext.newInstance("org.apache.cxf.ws.security.sts.provider.model");
            Marshaller marshaller = jaxbContext.createMarshaller();
            Document doc = DOMUtils.createDocument();
            Element rootElement = doc.createElement("root-element");
            JAXBElement<UsernameTokenType> tokenType = 
                new JAXBElement<UsernameTokenType>(
                    QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType
                );
            marshaller.marshal(tokenType, rootElement);
            usernameTokenElement = (Element)rootElement.getFirstChild();
        } catch (JAXBException ex) {
            LOG.log(Level.WARNING, "", ex);
            return response;
        }
        
        //
        // Validate the token
        //
        try {
            boolean allowNamespaceQualifiedPasswordTypes = 
                wssConfig.getAllowNamespaceQualifiedPasswordTypes();
            boolean bspCompliant = wssConfig.isWsiBSPCompliant();
            UsernameToken ut = 
                new UsernameToken(usernameTokenElement, allowNamespaceQualifiedPasswordTypes, bspCompliant);
            // The parsed principal is set independent whether validation is successful or not
            response.setPrincipal(new CustomTokenPrincipal(ut.getName()));
            if (ut.getPassword() == null) {
                return response;
            }
            if (secToken == null || secToken.isExpired()
                || (secToken.getAssociatedHash() != ut.hashCode())) {
                Credential credential = new Credential();
                credential.setUsernametoken(ut);
                validator.validate(credential, requestData);
            }
            Principal principal = 
                createPrincipal(
                    ut.getName(), ut.getPassword(), ut.getPasswordType(), ut.getNonce(), ut.getCreated()
                );
            
            // Get the realm of the UsernameToken
            String tokenRealm = null;
            if (usernameTokenRealmCodec != null) {
                tokenRealm = usernameTokenRealmCodec.getRealmFromToken(ut);
                // verify the realm against the cached token
                if (secToken != null) {
                    Properties props = secToken.getProperties();
                    if (props != null) {
                        String cachedRealm = props.getProperty(STSConstants.TOKEN_REALM);
                        if (!tokenRealm.equals(cachedRealm)) {
                            return response;
                        }
                    }
                }
            }
            
            response.setPrincipal(principal);
            response.setTokenRealm(tokenRealm);
            response.setValid(true);
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }
        
        return response;
    }
    
    /**
     * Create a principal based on the authenticated UsernameToken.
     */
    private Principal createPrincipal(
        String username,
        String passwordValue,
        String passwordType,
        String nonce,
        String createdTime
    ) {
        boolean hashed = false;
        if (WSConstants.PASSWORD_DIGEST.equals(passwordType)) {
            hashed = true;
        }
        WSUsernameTokenPrincipal principal = new WSUsernameTokenPrincipal(username, hashed);
        principal.setNonce(nonce);
        principal.setPassword(passwordValue);
        principal.setCreatedTime(createdTime);
        principal.setPasswordType(passwordType);
        return principal;
    }
    
}
