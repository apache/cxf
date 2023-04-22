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
import java.util.Base64;
import java.util.Properties;

import jakarta.xml.bind.JAXBElement;
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
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for validating an X.509 Token via the X509TokenValidator.
 */
public class X509TokenValidatorTest {

    /**
     * Test a valid certificate
     */
    @org.junit.Test
    public void testValidCertificate() throws Exception {
        TokenValidator x509TokenValidator = new X509TokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of an X509Certificate
        BinarySecurityTokenType binarySecurityToken = new BinarySecurityTokenType();
        JAXBElement<BinarySecurityTokenType> tokenType =
            new JAXBElement<BinarySecurityTokenType>(
                QNameConstants.BINARY_SECURITY_TOKEN, BinarySecurityTokenType.class, binarySecurityToken
            );
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        Crypto crypto = validatorParameters.getStsProperties().getSignatureCrypto();
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        assertTrue(certs != null && certs.length > 0);
        binarySecurityToken.setValue(Base64.getMimeEncoder().encodeToString(certs[0].getEncoded()));

        ReceivedToken validateTarget = new ReceivedToken(tokenType);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        // It can't handle the token as the value type is not set
        assertFalse(x509TokenValidator.canHandleToken(validateTarget));

        binarySecurityToken.setValueType(X509TokenValidator.X509_V3_TYPE);
        assertTrue(x509TokenValidator.canHandleToken(validateTarget));

        // This will fail as the encoding type is not set
        TokenValidatorResponse validatorResponse = x509TokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);

        binarySecurityToken.setEncodingType(WSS4JConstants.SOAPMESSAGE_NS + "#Base64Binary");

        validatorResponse = x509TokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
    }

    /**
     * Test an invalid certificate
     */
    @org.junit.Test
    public void testInvalidCertificate() throws Exception {
        TokenValidator x509TokenValidator = new X509TokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of an X509Certificate
        BinarySecurityTokenType binarySecurityToken = new BinarySecurityTokenType();
        JAXBElement<BinarySecurityTokenType> tokenType =
            new JAXBElement<BinarySecurityTokenType>(
                QNameConstants.BINARY_SECURITY_TOKEN, BinarySecurityTokenType.class, binarySecurityToken
            );

        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("eve");
        Crypto crypto = CryptoFactory.getInstance(getEveCryptoProperties());
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        assertTrue(certs != null && certs.length > 0);

        binarySecurityToken.setValue(Base64.getMimeEncoder().encodeToString(certs[0].getEncoded()));
        binarySecurityToken.setValueType(X509TokenValidator.X509_V3_TYPE);
        binarySecurityToken.setEncodingType(WSS4JConstants.SOAPMESSAGE_NS + "#Base64Binary");

        ReceivedToken validateTarget = new ReceivedToken(tokenType);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(x509TokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse = x509TokenValidator.validateToken(validatorParameters);
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

    private Properties getEveCryptoProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "evespass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "eve.jks");

        return properties;
    }



}
