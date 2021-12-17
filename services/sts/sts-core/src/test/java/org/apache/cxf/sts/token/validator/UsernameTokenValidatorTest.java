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


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.EncodedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.dom.message.token.UsernameToken;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for validating a UsernameToken via the UsernameTokenValidator.
 */
public class UsernameTokenValidatorTest {

    /**
     * Test a valid UsernameToken with password text
     */
    @org.junit.Test
    public void testValidUsernameTokenText() throws Exception {
        TokenValidator usernameTokenValidator = new UsernameTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a UsernameToken
        UsernameTokenType usernameToken = new UsernameTokenType();
        AttributedString username = new AttributedString();
        username.setValue("alice");
        usernameToken.setUsername(username);
        JAXBElement<UsernameTokenType> tokenType =
            new JAXBElement<UsernameTokenType>(
                QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameToken
            );

        ReceivedToken validateTarget = new ReceivedToken(tokenType);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(usernameTokenValidator.canHandleToken(validateTarget));

        // This will fail as there is no password
        TokenValidatorResponse validatorResponse =
                usernameTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);

        // Add a password
        PasswordString password = new PasswordString();
        password.setValue("clarinet");
        password.setType(WSS4JConstants.PASSWORD_TEXT);
        JAXBElement<PasswordString> passwordType =
            new JAXBElement<PasswordString>(
                QNameConstants.PASSWORD, PasswordString.class, password
            );
        usernameToken.getAny().add(passwordType);

        validatorResponse = usernameTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
    }

    /**
     * Test an invalid UsernameToken with password text
     */
    @org.junit.Test
    public void testInvalidUsernameTokenText() throws Exception {
        TokenValidator usernameTokenValidator = new UsernameTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a UsernameToken
        UsernameTokenType usernameToken = new UsernameTokenType();
        AttributedString username = new AttributedString();
        username.setValue("eve");
        usernameToken.setUsername(username);
        JAXBElement<UsernameTokenType> tokenType =
            new JAXBElement<UsernameTokenType>(
                QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameToken
            );

        // Add a password
        PasswordString password = new PasswordString();
        password.setValue("clarinet");
        password.setType(WSS4JConstants.PASSWORD_TEXT);
        JAXBElement<PasswordString> passwordType =
            new JAXBElement<PasswordString>(
                QNameConstants.PASSWORD, PasswordString.class, password
            );
        usernameToken.getAny().add(passwordType);

        ReceivedToken validateTarget = new ReceivedToken(tokenType);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(usernameTokenValidator.canHandleToken(validateTarget));

        // This will fail as the username is bad
        TokenValidatorResponse validatorResponse =
            usernameTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);

        // This will fail as the password is bad
        username.setValue("alice");
        password.setValue("badpassword");
        validatorResponse = usernameTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    /**
     * Test a valid UsernameToken with password digest
     */
    @org.junit.Test
    public void testValidUsernameTokenDigest() throws Exception {
        TokenValidator usernameTokenValidator = new UsernameTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a UsernameToken
        UsernameTokenType usernameToken = new UsernameTokenType();
        AttributedString username = new AttributedString();
        username.setValue("alice");
        usernameToken.setUsername(username);
        JAXBElement<UsernameTokenType> tokenType =
            new JAXBElement<UsernameTokenType>(
                QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameToken
            );

        // Create a WSS4J UsernameToken
        Document doc = DOMUtils.createDocument();
        UsernameToken ut = new UsernameToken(true, doc, WSS4JConstants.PASSWORD_DIGEST);
        ut.setName("alice");
        ut.setPassword("clarinet");
        ut.addNonce(doc);
        ut.addCreated(true, doc);

        // Add a password
        PasswordString password = new PasswordString();
        password.setValue(ut.getPassword());
        password.setType(WSS4JConstants.PASSWORD_DIGEST);
        JAXBElement<PasswordString> passwordType =
            new JAXBElement<PasswordString>(
                QNameConstants.PASSWORD, PasswordString.class, password
            );
        usernameToken.getAny().add(passwordType);

        // Add a nonce
        EncodedString nonce = new EncodedString();
        nonce.setValue(ut.getNonce());
        nonce.setEncodingType(WSS4JConstants.SOAPMESSAGE_NS + "#Base64Binary");
        JAXBElement<EncodedString> nonceType =
            new JAXBElement<EncodedString>(
                QNameConstants.NONCE, EncodedString.class, nonce
            );
        usernameToken.getAny().add(nonceType);

        // Add Created value
        String created = ut.getCreated();
        Element createdElement = doc.createElementNS(WSS4JConstants.WSU_NS, "Created");
        createdElement.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", WSS4JConstants.WSU_NS);
        createdElement.setTextContent(created);
        usernameToken.getAny().add(createdElement);

        ReceivedToken validateTarget = new ReceivedToken(tokenType);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(usernameTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                usernameTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);

        // Expected failure on a bad password
        password.setValue("badpassword");
        validatorResponse = usernameTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    private TokenValidatorParameters createValidatorParameters() throws WSSecurityException {
        TokenValidatorParameters parameters = new TokenValidatorParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(STSConstants.STATUS);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        return parameters;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return properties;
    }


}
