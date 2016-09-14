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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;

@Path("oauth-authorization-server")
public class AuthorizationMetadataService {

    private String issuer;
    private String authorizationEndpointAddress;
    private String tokenEndpointAddress;
    private String tokenRevocationEndpointAddress;
    private String jwkEndpointAddress;
    
    @GET
    @Produces("application/json")
    public String getConfiguration(@Context UriInfo ui) {
        Map<String, Object> cfg = new LinkedHashMap<String, Object>();
        String baseUri = getBaseUri(ui);
        prepareConfigurationData(cfg, baseUri);
        
        JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
        writer.setFormat(true);
        return writer.toJson(cfg);
    }
    
    protected void prepareConfigurationData(Map<String, Object> cfg, String baseUri) {
        // Issuer
        cfg.put("issuer", issuer == null ? baseUri : issuer);
        // Authorization Endpoint
        String theAuthorizationEndpointAddress = 
            calculateEndpointAddress(authorizationEndpointAddress, baseUri, "/idp/authorize");
        cfg.put("authorization_endpoint", theAuthorizationEndpointAddress);
        // Token Endpoint
        String theTokenEndpointAddress = 
            calculateEndpointAddress(tokenEndpointAddress, baseUri, "/oauth2/token");
        cfg.put("token_endpoint", theTokenEndpointAddress);
        // Token Revocation Endpoint
        String theTokenRevocationEndpointAddress = 
            calculateEndpointAddress(tokenRevocationEndpointAddress, baseUri, "/oauth2/revoke");
        cfg.put("revocation_endpoint", theTokenRevocationEndpointAddress);
        // Jwks Uri Endpoint
        String theJwkEndpointAddress = 
            calculateEndpointAddress(jwkEndpointAddress, baseUri, "/jwk/keys");
        cfg.put("jwks_uri", theJwkEndpointAddress);
    }

    protected static String calculateEndpointAddress(String endpointAddress, String baseUri, String defRelAddress) {
        endpointAddress = endpointAddress != null ? endpointAddress : defRelAddress;
        if (endpointAddress.startsWith("https")) {
            return endpointAddress;
        } else {
            return baseUri + endpointAddress; 
        }
    }

    private String getBaseUri(UriInfo ui) {
        String requestUri = ui.getRequestUri().toString();
        int ind = requestUri.lastIndexOf(".well-known");
        if (ind != -1) {
            requestUri = requestUri.substring(0, ind);
        }
        return requestUri;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setAuthorizationEndpointAddress(String authorizationEndpointAddress) {
        this.authorizationEndpointAddress = authorizationEndpointAddress;
    }

    public void setTokenEndpointAddress(String tokenEndpointAddress) {
        this.tokenEndpointAddress = tokenEndpointAddress;
    }

    public void setJwkEndpointAddress(String jwkEndpointAddress) {
        this.jwkEndpointAddress = jwkEndpointAddress;
    }

    public void setTokenRevocationEndpointAddress(String tokenRevocationEndpointAddress) {
        this.tokenRevocationEndpointAddress = tokenRevocationEndpointAddress;
    }
    
}
