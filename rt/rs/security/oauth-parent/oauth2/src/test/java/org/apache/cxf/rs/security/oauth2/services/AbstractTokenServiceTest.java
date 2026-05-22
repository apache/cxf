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

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class AbstractTokenServiceTest {

    @Test
    public void testGetClientReportsInvalidClientWhenClientNotFound() {
        TestTokenService service = new TestTokenService();

        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callGetClient("missing-client", "secret", new MetadataMap<>()));

        assertNotNull(ex.getError());
        assertEquals(OAuthConstants.INVALID_CLIENT, ex.getError().getError());
    }

    @Test
    public void testInvalidClientInvalidCharactersHandled() {
        TestTokenService service = new TestTokenService();

        String clientId = "alice\r\n\r\n\r\n[FAKE]+Admin+login+successful";
        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callGetClient(clientId, "secret", new MetadataMap<>()));

        assertNotNull(ex.getError());
        assertEquals(OAuthConstants.INVALID_CLIENT, ex.getError().getError());
    }

    @Test
    public void testGetClientReportsCustomErrorWhenProviderThrowsIt() {
        TestTokenService service = new TestTokenService();
        OAuthError customError = new OAuthError(OAuthConstants.UNAUTHORIZED_CLIENT);
        service.setException(new OAuthServiceException(customError));

        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callGetClient("client", "secret", new MetadataMap<>()));

        assertSame(customError, ex.getError());
    }

    @Test
    public void testGetClientReturnsClientWhenFound() {
        TestTokenService service = new TestTokenService();
        Client expected = new Client("client", "secret", true);
        service.setClient(expected);

        Client actual = service.callGetClient("client", "secret", new MetadataMap<>());
        assertSame(expected, actual);
    }

    private static final class TestTokenService extends AbstractTokenService {
        private Client client;
        private OAuthServiceException exception;

        Client callGetClient(String clientId, String clientSecret, MultivaluedMap<String, String> params) {
            return getClient(clientId, clientSecret, params);
        }

        void setClient(Client client) {
            this.client = client;
        }

        void setException(OAuthServiceException exception) {
            this.exception = exception;
        }

        @Override
        protected Client getValidClient(String clientId, String clientSecret, MultivaluedMap<String, String> params)
            throws OAuthServiceException {
            if (exception != null) {
                throw exception;
            }
            return client;
        }

        @Override
        protected void reportInvalidClient() {
            throw new InvalidClientException(new OAuthError(OAuthConstants.INVALID_CLIENT));
        }

        @Override
        protected void reportInvalidClient(OAuthError error) {
            throw new InvalidClientException(error);
        }
    }

    private static class InvalidClientException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final OAuthError error;

        InvalidClientException(OAuthError error) {
            this.error = error;
        }

        OAuthError getError() {
            return error;
        }
    }
}