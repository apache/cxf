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
package org.apache.cxf.rs.security.oidc.rp.idp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.utils.OAuthContextUtils;
import org.apache.cxf.rs.security.oidc.common.UserInfo;

@Path("/userinfo")
public class UserInfoService {
    // TODO: review if it makes sense to do JWE and JWS at the out filter level instead
    private JwsSignatureProvider sigProvider;
    private JweEncryptionProvider encryptionProvider;
    private UserInfoProvider userInfoProvider;
    private String issuer;
    
    @Context
    private MessageContext mc;
    @GET
    @Produces({"application/json", "application/jwt" })
    public Response getUserInfo() {
        OAuthContext oauth = OAuthContextUtils.getContext(mc);
        UserInfo userInfo = 
            userInfoProvider.getUserInfo(oauth.getClientId(), oauth.getSubject(), oauth.getPermissions());
        if (userInfo != null) {
            userInfo.setIssuer(issuer);
        }
        userInfo.setAudience(oauth.getClientId());
        
        Object responseEntity = userInfo;
        
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(userInfo);
        JoseHeaders headers = new JoseHeaders();
        JwsSignatureProvider theSigProvider = getInitializedSigProvider(headers);
        JweEncryptionProvider theEncryptionProvider = getInitializedEncryptionProvider();
        if (theSigProvider != null) {
            String userInfoString = producer.signWith(theSigProvider);
            if (theEncryptionProvider != null) {
                userInfoString = theEncryptionProvider.encrypt(StringUtils.toBytesUTF8(userInfoString), null);
            }
            responseEntity = userInfoString;
        } else if (theEncryptionProvider != null) {
            JweJwtCompactProducer jwe = new JweJwtCompactProducer(userInfo);
            responseEntity = jwe.encryptWith(theEncryptionProvider);
        }
        return Response.ok(responseEntity).build();
        
    }
    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }
    
    protected JwsSignatureProvider getInitializedSigProvider(JoseHeaders headers) {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        JwsSignatureProvider theSigProvider = JwsUtils.loadSignatureProvider(false); 
        headers.setAlgorithm(theSigProvider.getAlgorithm());
        return theSigProvider;
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider() {
        if (encryptionProvider != null) {
            return encryptionProvider;    
        } 
        return JweUtils.loadEncryptionProvider(false);
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    public UserInfoProvider getUserInfoProvider() {
        return userInfoProvider;
    }
    public void setUserInfoProvider(UserInfoProvider userInfoProvider) {
        this.userInfoProvider = userInfoProvider;
    }
}
