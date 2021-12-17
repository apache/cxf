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
package org.apache.cxf.sts.operation;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.token.validator.X509TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the validate operation to validate X.509 tokens.
 */
public class ValidateX509TokenUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    private static final QName QNAME_WST_STATUS =
        QNameConstants.WS_TRUST_FACTORY.createStatus(null).getName();

    /**
     * Test to successfully validate an X.509 token
     */
    @org.junit.Test
    public void testValidateX509Token() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new X509TokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.STATUS
            );
        request.getAny().add(tokenType);

        // Create a BinarySecurityToken
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        assertTrue(certs != null && certs.length > 0);

        JAXBElement<BinarySecurityTokenType> binarySecurityTokenType =
            createBinarySecurityToken(certs[0]);
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(binarySecurityTokenType);

        JAXBElement<ValidateTargetType> validateTargetType =
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, principal, msgCtx);
        assertTrue(validateResponse(response));
    }

    /**
     * Test to validate an invalid X.509 token
     */
    @org.junit.Test
    public void testValidateInvalidX509Token() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new X509TokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.STATUS
            );
        request.getAny().add(tokenType);

        // Create a BinarySecurityToken
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("eve");
        Crypto eveCrypto = CryptoFactory.getInstance(getEveCryptoProperties());
        X509Certificate[] certs = eveCrypto.getX509Certificates(cryptoType);
        assertTrue(certs != null && certs.length > 0);

        JAXBElement<BinarySecurityTokenType> binarySecurityTokenType =
            createBinarySecurityToken(certs[0]);
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(binarySecurityTokenType);

        JAXBElement<ValidateTargetType> validateTargetType =
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, principal, msgCtx);
        assertFalse(validateResponse(response));
    }


    /*
     * Create a security context object
     */
    private SecurityContext createSecurityContext(final Principal p) {
        return new SecurityContext() {
            public Principal getUserPrincipal() {
                return p;
            }
            public boolean isUserInRole(String role) {
                return false;
            }
        };
    }

    /**
     * Return true if the response has a valid status, false otherwise
     */
    private boolean validateResponse(RequestSecurityTokenResponseType response) {
        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());

        for (Object requestObject : response.getAny()) {
            if (requestObject instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) requestObject;
                if (QNAME_WST_STATUS.equals(jaxbElement.getName())) {
                    StatusType status = (StatusType)jaxbElement.getValue();
                    if (STSConstants.VALID_CODE.equals(status.getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    private JAXBElement<BinarySecurityTokenType> createBinarySecurityToken(
        X509Certificate cert
    ) throws Exception {
        BinarySecurityTokenType binarySecurityToken = new BinarySecurityTokenType();
        binarySecurityToken.setValue(Base64.getMimeEncoder().encodeToString(cert.getEncoded()));
        binarySecurityToken.setValueType(X509TokenValidator.X509_V3_TYPE);
        binarySecurityToken.setEncodingType(WSS4JConstants.SOAPMESSAGE_NS + "#Base64Binary");
        return new JAXBElement<BinarySecurityTokenType>(
                QNameConstants.BINARY_SECURITY_TOKEN, BinarySecurityTokenType.class, binarySecurityToken
            );
    }

}
