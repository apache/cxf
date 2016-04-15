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
package demo.jaxrs.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectProvider;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.client.AccessTokenGrantWriter;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.jwt.JwtBearerGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


public final class BigQueryServer {
    private BigQueryServer() {
    }
    
    public static void main(String[] args) throws Exception {
        final String pc12File = args[0];
        final String keySecret = args[1];
        final String issuer = args[2];
        final String projectId = args[3];
        
        PrivateKey privateKey = loadPrivateKey(pc12File, keySecret);
        
        
        ClientAccessToken accessToken = getAccessToken(privateKey, issuer);
        
        WebClient bigQueryClient = WebClient.create("https://www.googleapis.com/bigquery/v2/projects/" 
                                                    + projectId + "/queries",
                                                    Collections.singletonList(new JsonMapObjectProvider()));
        bigQueryClient.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        
        List<ShakespeareText> texts = BigQueryService.getMatchingTexts(bigQueryClient, accessToken, "brave", "10");
        
        System.out.println("Matching texts:");
        for (ShakespeareText text : texts) {
            System.out.println(text.getText() + ":" + text.getDate());
        }
    }

    private static ClientAccessToken getAccessToken(PrivateKey privateKey, String issuer) {
        JwsHeaders headers = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.RS256);
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        claims.setAudience("https://www.googleapis.com/oauth2/v3/token");
        
        long issuedAt = OAuthUtils.getIssuedAt();
        claims.setIssuedAt(issuedAt);
        claims.setExpiryTime(issuedAt + 60 * 60);
        claims.setProperty("scope", "https://www.googleapis.com/auth/bigquery.readonly");
        
        JwtToken token = new JwtToken(headers, claims);
        JwsJwtCompactProducer p = new JwsJwtCompactProducer(token);
        String base64UrlAssertion = p.signWith(privateKey);
        
        JwtBearerGrant grant = new JwtBearerGrant(base64UrlAssertion);
        
        WebClient accessTokenService = WebClient.create("https://www.googleapis.com/oauth2/v3/token",
                                                        Arrays.asList(new OAuthJSONProvider(),
                                                                      new AccessTokenGrantWriter()));
        WebClient.getConfig(accessTokenService).getInInterceptors().add(new LoggingInInterceptor());
        
        accessTokenService.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON);
        
        return accessTokenService.post(grant, ClientAccessToken.class);
    }

    private static PrivateKey loadPrivateKey(String p12File, String password) throws Exception {
        try (InputStream is = new FileInputStream(p12File)) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(is, password.toCharArray());
            return (PrivateKey)store.getKey("privateKey", password.toCharArray());
        }
    }

}
