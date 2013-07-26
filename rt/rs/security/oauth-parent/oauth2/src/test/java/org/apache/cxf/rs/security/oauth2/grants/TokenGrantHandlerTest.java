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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Assert;
import org.junit.Test;

public class TokenGrantHandlerTest extends Assert {

    @Test
    public void testSimpleGrantNotSupported() {
        try {
            new SimpleGrantHandler().createAccessToken(createClient("unsupported"), 
                                                       createMap("a"));
            fail("Unsupported Grant");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.UNAUTHORIZED_CLIENT, ex.getMessage());
        }
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
    public void testSimpleGrantSupported() {
        ServerAccessToken t = new SimpleGrantHandler().createAccessToken(createClient("a"), 
                                                                         createMap("a"));
        assertTrue(t instanceof BearerAccessToken);
    }
    
    @Test
    public void testComplexGrantNotSupported() {
        try {
            new ComplexGrantHandler(Arrays.asList("a", "b"))
                .createAccessToken(createClient("unsupported"), createMap("a"));
            fail("Unsupported Grant");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.UNAUTHORIZED_CLIENT, ex.getMessage());
        }
    }
    
    @Test
    public void testComplexGrantSupported() {
        ServerAccessToken t = new ComplexGrantHandler(Arrays.asList("a", "b"))
            .createAccessToken(createClient("a"), createMap("a"));
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
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putSingle(OAuthConstants.GRANT_TYPE, grant);
        return map;
    }
    
    private static class SimpleGrantHandler extends AbstractGrantHandler {

        public SimpleGrantHandler() {
            super("a");
        }
        
        public SimpleGrantHandler(List<String> grants) {
            super(grants);
        }
        
        @Override
        public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
            throws OAuthServiceException {
            super.checkIfGrantSupported(client);
            return new BearerAccessToken(client, 3600L);
        } 
        
    }
    
    private static class ComplexGrantHandler extends AbstractGrantHandler {

        public ComplexGrantHandler(List<String> grants) {
            super(grants);
        }
        
        @Override
        public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
            throws OAuthServiceException {
            super.checkIfGrantSupported(client, params.getFirst(OAuthConstants.GRANT_TYPE));
            return new BearerAccessToken(client, 3600L);
        } 
        
    }
}
