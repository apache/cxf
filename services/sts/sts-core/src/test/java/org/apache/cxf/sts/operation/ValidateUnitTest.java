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

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for the validate operation.
 */
public class ValidateUnitTest {

    private static final QName QNAME_WST_STATUS =
        QNameConstants.WS_TRUST_FACTORY.createStatus(null).getName();


    /**
     * Test to successfully validate a (dummy) token.
     */
    @org.junit.Test
    public void testValidateToken() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new DummyTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.STATUS
            );
        request.getAny().add(tokenType);
        ValidateTargetType validateTarget = new ValidateTargetType();
        JAXBElement<BinarySecurityTokenType> token = createToken();
        validateTarget.setAny(token);
        JAXBElement<ValidateTargetType> validateTargetType =
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, null, msgCtx);
        assertTrue(validateResponse(response));
    }


    /**
     * Test to successfully validate multiple (dummy) tokens.
     */
    @org.junit.Test
    public void testValidateMultipleTokens() throws Exception {
        TokenRequestCollectionOperation requestCollectionOperation =
            new TokenRequestCollectionOperation();
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        requestCollectionOperation.setValidateOperation(validateOperation);

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new DummyTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenCollectionType requestCollection =
            new RequestSecurityTokenCollectionType();
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.STATUS
            );
        request.getAny().add(tokenType);

        JAXBElement<String> requestType =
            new JAXBElement<String>(
                QNameConstants.REQUEST_TYPE, String.class,
                TokenRequestCollectionOperation.WSTRUST_REQUESTTYPE_BATCH_VALIDATE
            );
        request.getAny().add(requestType);

        ValidateTargetType validateTarget = new ValidateTargetType();
        JAXBElement<BinarySecurityTokenType> token = createToken();
        validateTarget.setAny(token);
        JAXBElement<ValidateTargetType> validateTargetType =
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);
        requestCollection.getRequestSecurityToken().add(request);

        request = new RequestSecurityTokenType();
        request.getAny().add(tokenType);
        request.getAny().add(requestType);
        validateTarget.setAny(token);
        request.getAny().add(validateTargetType);
        requestCollection.getRequestSecurityToken().add(request);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Validate a token
        RequestSecurityTokenResponseCollectionType response =
            requestCollectionOperation.requestCollection(requestCollection, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertEquals(securityTokenResponse.size(), 2);
        assertTrue(validateResponse(securityTokenResponse.get(0)));
        assertTrue(validateResponse(securityTokenResponse.get(1)));
    }

    /**
     * Test that calls Validate without a ValidateTarget
     */
    @org.junit.Test
    public void testNoToken() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new DummyTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.STATUS
            );
        request.getAny().add(tokenType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Validate a token
        try {
            validateOperation.validate(request, null, msgCtx);
            fail("Failure expected when no element is presented for validation");
        } catch (STSException ex) {
            // expected
        }
    }

    /**
     * Test to validate a token of an unknown or missing TokenType value.
     */
    @org.junit.Test
    public void testTokenType() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new DummyTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, "UnknownTokenType"
            );
        request.getAny().add(tokenType);
        ValidateTargetType validateTarget = new ValidateTargetType();
        JAXBElement<BinarySecurityTokenType> token = createToken();
        validateTarget.setAny(token);
        JAXBElement<ValidateTargetType> validateTargetType =
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Validate a token - failure expected on an unknown token type
        try {
            validateOperation.validate(request, null, msgCtx);
            fail("Failure expected on an unknown token type");
        } catch (STSException ex) {
            // expected
        }

        // Validate a token - no token type is sent, so it defaults to status
        request.getAny().remove(0);
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, null, msgCtx);
        assertTrue(validateResponse(response));
    }


    /**
     * Test that sends a Context attribute when validating a token, and checks it gets
     * a response with the Context attribute properly set.
     */
    @org.junit.Test
    public void testContext() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new DummyTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.STATUS
            );
        request.getAny().add(tokenType);
        ValidateTargetType validateTarget = new ValidateTargetType();
        JAXBElement<BinarySecurityTokenType> token = createToken();
        validateTarget.setAny(token);
        JAXBElement<ValidateTargetType> validateTargetType =
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);
        request.setContext("AuthenticationContext");

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, null, msgCtx);
        assertTrue(validateResponse(response));
        assertEquals("AuthenticationContext", response.getContext());
    }

    /**
     * Mock up a (JAXB) BinarySecurityTokenType.
     */
    private JAXBElement<BinarySecurityTokenType> createToken() {
        BinarySecurityTokenType binarySecurityToken = new BinarySecurityTokenType();
        binarySecurityToken.setId("BST-1234");
        binarySecurityToken.setValue("12345678");
        binarySecurityToken.setValueType(DummyTokenProvider.TOKEN_TYPE);
        binarySecurityToken.setEncodingType(DummyTokenProvider.BASE64_NS);
        return new JAXBElement<BinarySecurityTokenType>(
                QNameConstants.BINARY_SECURITY_TOKEN, BinarySecurityTokenType.class, binarySecurityToken
            );
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
}
