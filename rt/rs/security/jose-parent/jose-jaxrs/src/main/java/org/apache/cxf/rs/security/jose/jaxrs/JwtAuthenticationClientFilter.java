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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationClientFilter extends JoseJwtProducer
    implements ClientRequestFilter {

    private static final String DEFAULT_AUTH_SCHEME = "Bearer";
    private String authScheme = DEFAULT_AUTH_SCHEME;
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        JwtToken jwt = getJwtToken(requestContext);
        if (jwt == null && super.isJweRequired()) {
            AuthorizationPolicy ap = JAXRSUtils.getCurrentMessage().getExchange()
                .getEndpoint().getEndpointInfo().getExtensor(AuthorizationPolicy.class);
            if (ap != null && ap.getUserName() != null) {
                JwtClaims claims = new JwtClaims();
                claims.setSubject(ap.getUserName());
                claims.setClaim("password", ap.getPassword());
                claims.setIssuedAt(System.currentTimeMillis() / 1000L);
                jwt = new JwtToken(new JweHeaders(), claims);
            }
        }
        if (jwt == null) {
            throw new JoseException("JWT token is not available");
        }
        String data = super.processJwt(jwt);
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION,
                                              authScheme + " " + data);
    }

    protected JwtToken getJwtToken(ClientRequestContext requestContext) {
        // Try the filter properties first, then the message properties
        JwtToken token = (JwtToken)requestContext.getProperty(JwtConstants.JWT_TOKEN);
        if (token == null) {
            Message m = PhaseInterceptorChain.getCurrentMessage();
            token = (JwtToken)m.getContextualProperty(JwtConstants.JWT_TOKEN);
        }

        if (token != null) {
            return token;
        }

        // Otherwise check to see if we have some claims + construct the header ourselves
        JwtClaims claims = (JwtClaims)requestContext.getProperty(JwtConstants.JWT_CLAIMS);
        if (claims == null) {
            Message m = PhaseInterceptorChain.getCurrentMessage();
            claims = (JwtClaims)m.getContextualProperty(JwtConstants.JWT_CLAIMS);
        }

        if (claims != null) {
            token = new JwtToken(claims);
        }

        return token;
    }

    protected String getContextPropertyValue() {
        return Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(16));
    }

    public void setAuthScheme(String authScheme) {
        this.authScheme = authScheme;
    }



}
