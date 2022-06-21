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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.net.URI;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJoseJwtProducer;



/**
 * Base Authorization Code Grant representation, captures the code
 * and the redirect URI this code has been returned to, visible to the client
 */
public class JwtRequestCodeGrant extends AuthorizationCodeGrant {
    private static final long serialVersionUID = -3738825769770411453L;
    private OAuthJoseJwtProducer joseProducer = new OAuthJoseJwtProducer();
    private String clientSecret;
    private String issuer;
    public JwtRequestCodeGrant() {
    }

    public JwtRequestCodeGrant(String issuer) {
        this.issuer = issuer;
    }

    public JwtRequestCodeGrant(String code, String issuer) {
        super(code);
        this.issuer = issuer;
    }

    public JwtRequestCodeGrant(String code, URI uri, String issuer) {
        super(code, uri);
        this.issuer = issuer;
    }
    public MultivaluedMap<String, String> toMap() {
        String request = getRequest();
        MultivaluedMap<String, String> newMap = new MetadataMap<>();
        newMap.putSingle("request", request);
        return newMap;

    }
    public String getRequest() {
        MultivaluedMap<String, String> map = super.toMap();
        JwtClaims claims = new JwtClaims();
        if (issuer != null) {
            claims.setIssuer(issuer);
        }
        for (String key : map.keySet()) {
            claims.setClaim(key, map.getFirst(key));
        }
        return joseProducer.processJwt(new JwtToken(claims), clientSecret);
    }

    public void setIssuer(String issuer) {
        // Can it be a client id ?

        this.issuer = issuer;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public OAuthJoseJwtProducer getJoseProducer() {
        return joseProducer;
    }

    public void setJoseProducer(OAuthJoseJwtProducer joseProducer) {
        this.joseProducer = joseProducer;
    }

}
