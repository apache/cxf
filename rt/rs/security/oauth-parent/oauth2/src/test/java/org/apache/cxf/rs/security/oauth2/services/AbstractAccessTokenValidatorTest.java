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
package org.apache.cxf.rs.security.oauth2.services;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class AbstractAccessTokenValidatorTest {

    @Test
    public void testValidationDataCacheReusesSuccessfulValidationWithinLifetime() {
        TestAccessTokenValidator validator = new TestAccessTokenValidator(true);
        TestValidator accessTokenValidator = createAccessTokenValidator(validator);

        AccessTokenValidation firstValidation = accessTokenValidator.getValidation("token");
        AccessTokenValidation secondValidation = accessTokenValidator.getValidation("token");

        assertSame(firstValidation, secondValidation);
        assertEquals(1, validator.getValidationCount());
    }

    @Test
    public void testValidationDataCacheLifetimeBoundsSuccessfulValidationReuse() {
        TestAccessTokenValidator validator = new TestAccessTokenValidator(true);
        TestValidator accessTokenValidator = createAccessTokenValidator(validator);
        accessTokenValidator.setValidationDataCacheLifetime(0L);

        accessTokenValidator.getValidation("token");
        accessTokenValidator.getValidation("token");

        assertEquals(2, validator.getValidationCount());
    }

    @Test
    public void testValidationDataCacheDoesNotReuseUnsuccessfulValidation() {
        TestAccessTokenValidator validator = new TestAccessTokenValidator(false);
        TestValidator accessTokenValidator = createAccessTokenValidator(validator);

        accessTokenValidator.getValidation("token");
        accessTokenValidator.getValidation("token");

        assertEquals(2, validator.getValidationCount());
    }

    @Test
    public void testValidationDataCacheReusesTokenExpiringWithinDefaultLifetime() {
        TestAccessTokenValidator validator = new TestAccessTokenValidator(true);
        validator.setTokenLifetime(30L);
        TestValidator accessTokenValidator = createAccessTokenValidator(validator);

        AccessTokenValidation firstValidation = accessTokenValidator.getValidation("token");
        AccessTokenValidation secondValidation = accessTokenValidator.getValidation("token");

        assertSame(firstValidation, secondValidation);
        assertEquals(1, validator.getValidationCount());
    }

    @Test
    public void testValidationDataCacheRejectsExpiredTokenBeforeCacheLifetime() {
        TestAccessTokenValidator validator = new TestAccessTokenValidator(true);
        TestValidator accessTokenValidator = createAccessTokenValidator(validator);

        AccessTokenValidation validation = accessTokenValidator.getValidation("token");
        validation.setTokenIssuedAt(OAuthUtils.getIssuedAt() - 2L);
        validation.setTokenLifetime(1L);

        assertThrows(NotAuthorizedException.class, () -> accessTokenValidator.getValidation("token"));
        accessTokenValidator.getValidation("token");

        assertEquals(2, validator.getValidationCount());
    }

    @Test
    public void testValidationDataCacheRevalidatesBeforeTokenExpires() {
        TestAccessTokenValidator validator = new TestAccessTokenValidator(true);
        TestValidator accessTokenValidator = createAccessTokenValidator(validator);
        accessTokenValidator.setValidationDataCacheLifetime(0L);

        accessTokenValidator.getValidation("token");
        accessTokenValidator.getValidation("token");

        assertEquals(2, validator.getValidationCount());
    }

    @Test
    public void testValidationDataCacheLifetimeBoundsTokenWithNoLifetime() {
        TestAccessTokenValidator validator = new TestAccessTokenValidator(true);
        validator.setTokenLifetime(0L);
        TestValidator accessTokenValidator = createAccessTokenValidator(validator);
        accessTokenValidator.setValidationDataCacheLifetime(0L);

        accessTokenValidator.getValidation("token");
        accessTokenValidator.getValidation("token");

        assertEquals(2, validator.getValidationCount());
    }

    private static TestValidator createAccessTokenValidator(TestAccessTokenValidator validator) {
        TestValidator accessTokenValidator = new TestValidator();
        accessTokenValidator.setTokenValidator(validator);
        accessTokenValidator.setMaxValidationDataCacheSize(10);
        return accessTokenValidator;
    }

    private static final class TestValidator extends AbstractAccessTokenValidator {
        private AccessTokenValidation getValidation(String authSchemeData) {
            return getAccessTokenValidation(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, authSchemeData, null);
        }
    }

    private static final class TestAccessTokenValidator implements AccessTokenValidator {
        private final boolean validationSuccessful;
        private long tokenLifetime = 3600L;
        private int validationCount;

        private TestAccessTokenValidator(boolean validationSuccessful) {
            this.validationSuccessful = validationSuccessful;
        }

        @Override
        public List<String> getSupportedAuthorizationSchemes() {
            return Collections.singletonList(OAuthConstants.BEARER_AUTHORIZATION_SCHEME);
        }

        @Override
        public AccessTokenValidation validateAccessToken(MessageContext mc,
                                                         String authScheme,
                                                         String authSchemeData,
                                                         MultivaluedMap<String, String> extraProps)
            throws OAuthServiceException {
            validationCount++;
            AccessTokenValidation validation = new AccessTokenValidation();
            validation.setInitialValidationSuccessful(validationSuccessful);
            validation.setTokenKey(authSchemeData);
            validation.setTokenIssuedAt(OAuthUtils.getIssuedAt());
            validation.setTokenLifetime(tokenLifetime);
            return validation;
        }

        private void setTokenLifetime(long tokenLifetime) {
            this.tokenLifetime = tokenLifetime;
        }

        private int getValidationCount() {
            return validationCount;
        }
    }
}
