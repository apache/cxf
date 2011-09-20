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

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.bind.JAXBElement;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;

import org.apache.cxf.ws.security.sts.provider.model.secext.EncodedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.UsernameToken;

/**
 * This class validates a wsse UsernameToken.
 */
public class UsernameTokenValidator implements TokenValidator {
    
    private static final Logger LOG = LogUtils.getL7dLogger(UsernameTokenValidator.class);
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
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
        
        if (validateTarget != null && validateTarget.isUsernameToken()) {
            //
            // Parse the JAXB object
            //
            String passwordType = null;
            String passwordValue = null;
            String nonce = null;
            String created = null;
            UsernameTokenType usernameTokenType = (UsernameTokenType)validateTarget.getToken();
            for (Object any : usernameTokenType.getAny()) {
                if (any instanceof JAXBElement<?>) {
                    JAXBElement<?> anyElement = (JAXBElement<?>) any;
                    if (QNameConstants.PASSWORD.equals(anyElement.getName())) {
                        PasswordString passwordString = 
                            (PasswordString)anyElement.getValue();
                        passwordType = passwordString.getType();
                        passwordValue = passwordString.getValue();
                    } else if (QNameConstants.NONCE.equals(anyElement.getName())) {
                        EncodedString nonceES = (EncodedString)anyElement.getValue();
                        nonce = nonceES.getValue();
                        // Encoding Type must be equal to Base64Binary
                        if (!BinarySecurity.BASE64_ENCODING.equals(nonceES.getEncodingType())) {
                            String errorMsg = "The UsernameToken nonce element has a bad encoding type";
                            LOG.log(Level.WARNING, errorMsg + " : " + nonceES.getEncodingType());
                            return response;
                        }
                    }
                } else if (any instanceof Element) {
                    Element element = (Element)any;
                    if (WSConstants.WSU_NS.equals(element.getNamespaceURI()) 
                        && WSConstants.CREATED_LN.equals(element.getLocalName())) {
                        created = element.getTextContent();
                    }
                }
            }
            
            //
            // Validate the token
            //
            try {
                boolean valid = 
                    verifyPassword(
                        usernameTokenType.getUsername().getValue(), passwordValue, passwordType, 
                        nonce, created, requestData
                    );
                response.setValid(valid);
                if (valid) {
                    Principal principal = 
                        createPrincipal(
                            usernameTokenType.getUsername().getValue(), passwordValue, passwordType, 
                            nonce, created
                        );
                    response.setPrincipal(principal);
                }
            } catch (WSSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
            }
        }
            
        return response;
    }
    
    private boolean verifyPassword(
        String username,
        String passwordValue,
        String passwordType,
        String nonce,
        String createdTime,
        RequestData requestData
    ) throws WSSecurityException {
        WSPasswordCallback pwCb = 
            new WSPasswordCallback(
                username, null, passwordType, WSPasswordCallback.USERNAME_TOKEN, requestData
            );
        try {
            requestData.getCallbackHandler().handle(new Callback[]{pwCb});
        } catch (IOException e) {
            LOG.log(Level.WARNING, "", e);
            throw new WSSecurityException(
                WSSecurityException.FAILED_AUTHENTICATION, null, null, e
            );
        } catch (UnsupportedCallbackException e) {
            LOG.log(Level.WARNING, "", e);
            throw new WSSecurityException(
                WSSecurityException.FAILED_AUTHENTICATION, null, null, e
            );
        }
        String origPassword = pwCb.getPassword();
        if (origPassword == null) {
            LOG.fine("Callback supplied no password for: " + username);
            return false;
        }
        if (WSConstants.PASSWORD_DIGEST.equals(passwordType)) {
            String passDigest = UsernameToken.doPasswordDigest(nonce, createdTime, origPassword);
            if (!passDigest.equals(passwordValue)) {
                return false;
            }
        } else {
            if (!origPassword.equals(passwordValue)) {
                return false;
            }
        }
        return true;
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
