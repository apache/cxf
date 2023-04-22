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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;

public class AuthorizationMetadataService {
    private String issuer;
    private boolean stripPathFromIssuerUri = true;
    // Required
    private String authorizationEndpointAddress;
    // Optional if only an implicit flow is used
    private boolean tokenEndpointNotAvailable;
    private String tokenEndpointAddress;
    // Optional
    private boolean tokenRevocationEndpointNotAvailable;
    private String tokenRevocationEndpointAddress;
    // Required for OIDC, optional otherwise
    private boolean jwkEndpointNotAvailable;
    private String jwkEndpointAddress;
    // Optional
    private boolean dynamicRegistrationEndpointNotAvailable;
    private String dynamicRegistrationEndpointAddress;

    @GET
    @Path("oauth-authorization-server")
    @Produces(MediaType.APPLICATION_JSON)
    public String getConfiguration(@Context UriInfo ui) {
        Map<String, Object> cfg = new LinkedHashMap<>();
        String baseUri = getBaseUri(ui);
        prepareConfigurationData(cfg, baseUri);

        JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
        writer.setFormat(true);
        return writer.toJson(cfg);
    }

    protected void prepareConfigurationData(Map<String, Object> cfg, String baseUri) {
        // Issuer
        cfg.put("issuer", buildIssuerUri(baseUri));
        // Authorization Endpoint
        String theAuthorizationEndpointAddress =
            calculateEndpointAddress(authorizationEndpointAddress, baseUri, "/idp/authorize");
        cfg.put("authorization_endpoint", theAuthorizationEndpointAddress);
        // Token Endpoint
        if (!isTokenEndpointNotAvailable()) {
            String theTokenEndpointAddress =
                calculateEndpointAddress(tokenEndpointAddress, baseUri, "/oauth2/token");
            cfg.put("token_endpoint", theTokenEndpointAddress);
        }
        // Token Revocation Endpoint
        if (!isTokenRevocationEndpointNotAvailable()) {
            String theTokenRevocationEndpointAddress =
                calculateEndpointAddress(tokenRevocationEndpointAddress, baseUri, "/oauth2/revoke");
            cfg.put("revocation_endpoint", theTokenRevocationEndpointAddress);
        }
        // Jwks Uri Endpoint
        if (!isJwkEndpointNotAvailable()) {
            String theJwkEndpointAddress =
                calculateEndpointAddress(jwkEndpointAddress, baseUri, "/jwk/keys");
            cfg.put("jwks_uri", theJwkEndpointAddress);
        }
        // Dynamic Registration Endpoint
        if (!isDynamicRegistrationEndpointNotAvailable()) {
            String theDynamicRegistrationEndpointAddress =
                calculateEndpointAddress(dynamicRegistrationEndpointAddress, baseUri, "/dynamic/register");
            cfg.put("registration_endpoint", theDynamicRegistrationEndpointAddress);
        }
    }

    protected static String calculateEndpointAddress(String endpointAddress, String baseUri, String defRelAddress) {
        endpointAddress = endpointAddress != null ? endpointAddress : defRelAddress;
        if (isAbsoluteUri(endpointAddress)) {
            return endpointAddress;
        } else {
            URI uri = UriBuilder.fromUri(baseUri).path(endpointAddress).build();
            return removeDefaultPort(uri).toString();
        }
    }

    private static boolean isAbsoluteUri(String endpointAddress) {
        if (endpointAddress == null) {
            return false;
        }
        return endpointAddress.startsWith("http://") || endpointAddress.startsWith("https://");
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

    public void setTokenRevocationEndpointNotAvailable(boolean tokenRevocationEndpointNotAvailable) {
        this.tokenRevocationEndpointNotAvailable = tokenRevocationEndpointNotAvailable;
    }
    public boolean isTokenRevocationEndpointNotAvailable() {
        return tokenRevocationEndpointNotAvailable;
    }

    public void setJwkEndpointNotAvailable(boolean jwkEndpointNotAvailable) {
        this.jwkEndpointNotAvailable = jwkEndpointNotAvailable;
    }

    public boolean isJwkEndpointNotAvailable() {
        return jwkEndpointNotAvailable;
    }

    public boolean isTokenEndpointNotAvailable() {
        return tokenEndpointNotAvailable;
    }

    public void setTokenEndpointNotAvailable(boolean tokenEndpointNotAvailable) {
        this.tokenEndpointNotAvailable = tokenEndpointNotAvailable;
    }

    public boolean isDynamicRegistrationEndpointNotAvailable() {
        return dynamicRegistrationEndpointNotAvailable;
    }

    public void setDynamicRegistrationEndpointNotAvailable(boolean dynamicRegistrationEndpointNotAvailable) {
        this.dynamicRegistrationEndpointNotAvailable = dynamicRegistrationEndpointNotAvailable;
    }

    public String getDynamicRegistrationEndpointAddress() {
        return dynamicRegistrationEndpointAddress;
    }

    public void setDynamicRegistrationEndpointAddress(String dynamicRegistrationEndpointAddress) {
        this.dynamicRegistrationEndpointAddress = dynamicRegistrationEndpointAddress;
    }

    private String buildIssuerUri(String baseUri) {
        URI uri;
        if (isAbsoluteUri(issuer)) {
            uri = UriBuilder.fromUri(issuer).build();
        } else {
            uri = issuer == null || !issuer.startsWith("/") ? URI.create(baseUri)
                    : UriBuilder.fromUri(baseUri).path(issuer).build();
        }
        uri = removeDefaultPort(uri);
        if (stripPathFromIssuerUri) {
            StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() != -1) {
                sb.append(':').append(uri.getPort());
            }
            return sb.toString();
        } else {
            return uri.toString();
        }
    }

    private static URI removeDefaultPort(URI uri) {
        if ((uri.getPort() == 80 && "http".equals(uri.getScheme()))
                || (uri.getPort() == 443 && "https".equals(uri.getScheme()))) {
            try {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), -1,
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid URI " + uri + " : " + e.toString(), e);
            }
        }
        return uri;
    }

    public void setStripPathFromIssuerUri(boolean stripPathFromIssuerUri) {
        this.stripPathFromIssuerUri = stripPathFromIssuerUri;
    }
}
