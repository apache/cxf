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

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.JoseException;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jwt.AbstractJoseJwtProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;

@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationClientFilter extends AbstractJoseJwtProducer 
    implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        JwtToken jwt = getJwtToken(requestContext);
        boolean jweRequired = false;
        if (jwt == null) {
            AuthorizationPolicy ap = JAXRSUtils.getCurrentMessage().getExchange()
                .get(Endpoint.class).getEndpointInfo().getExtensor(AuthorizationPolicy.class);
            if (ap != null && ap.getUserName() != null) {
                JwtClaims claims = new JwtClaims();
                claims.setSubject(ap.getUserName());
                claims.setClaim("password", ap.getPassword());
                claims.setIssuedAt(System.currentTimeMillis() / 1000);
                jwt = new JwtToken(new JoseHeaders(), claims);
                jweRequired = true;
            }
        }
        if (jwt == null) {
            throw new JoseException("JWT token is not available");
        }
        JoseUtils.setJoseMessageContextProperty(jwt.getHeaders(),
                                                getContextPropertyValue());
        String data = super.processJwt(jwt, true, jweRequired);
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, 
                                              "JWT " + data);
    }
    protected JwtToken getJwtToken(ClientRequestContext requestContext) {
        return (JwtToken)requestContext.getProperty("jwt.token");
    }
    protected String getContextPropertyValue() {
        return Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(16));
    }
}
