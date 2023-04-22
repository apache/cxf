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
package org.apache.cxf.rs.security.oauth2.grants;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TokenGrantHandlerTest {



    @Test
    public void testSimpleGrantSupported() {
        SimpleGrantHandler handler = new SimpleGrantHandler();
        handler.setDataProvider(new OAuthDataProviderImpl());
        ServerAccessToken t = handler.createAccessToken(createClient("a"), createMap("a"));
        assertTrue(t instanceof BearerAccessToken);
    }

    @Test
    public void testSimpleGrantBug() {
        try {
            new SimpleGrantHandler(Arrays.asList("a", "b")).createAccessToken(createClient("a"),
                                                       createMap("a"));
            fail("Grant handler bug");
        } catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testComplexGrantSupported() {
        ComplexGrantHandler handler = new ComplexGrantHandler(Arrays.asList("a", "b"));
        handler.setDataProvider(new OAuthDataProviderImpl());
        ServerAccessToken t = handler.createAccessToken(createClient("a"), createMap("a"));
        assertTrue(t instanceof BearerAccessToken);
    }

    private Client createClient(String... grants) {
        Client c = new Client("alice", "password", true);
        for (String grant : grants) {
            c.getAllowedGrantTypes().add(grant);
        }
        return c;
    }

    private MultivaluedMap<String, String> createMap(String grant) {
        MultivaluedMap<String, String> map = new MetadataMap<>();
        map.putSingle(OAuthConstants.GRANT_TYPE, grant);
        return map;
    }

    private static class SimpleGrantHandler extends AbstractGrantHandler {

        SimpleGrantHandler() {
            super("a");
        }

        SimpleGrantHandler(List<String> grants) {
            super(grants);
        }

        @Override
        public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
            throws OAuthServiceException {
            return super.doCreateAccessToken(client, client.getSubject(), params);
        }

    }

    private static class ComplexGrantHandler extends AbstractGrantHandler {

        ComplexGrantHandler(List<String> grants) {
            super(grants);
        }

        @Override
        public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
            throws OAuthServiceException {
            return super.doCreateAccessToken(client, client.getSubject(),
                                             params.getFirst(OAuthConstants.GRANT_TYPE), null);
        }

    }
}