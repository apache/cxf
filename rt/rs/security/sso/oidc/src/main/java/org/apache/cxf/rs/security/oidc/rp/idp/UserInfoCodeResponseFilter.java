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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenResponseFilter;
import org.apache.cxf.rs.security.oidc.common.UserToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class UserInfoCodeResponseFilter implements AccessTokenResponseFilter {
    private JwsSignatureProvider sigProvider;
    private JweEncryptionProvider encryptionProvider;
    private UserInfoProvider userInfoProvider;
    private String issuer;
    @Override
    public void process(ClientAccessToken ct, ServerAccessToken st) {
        UserToken token = 
            userInfoProvider.getUserToken(st.getClient().getClientId(), st.getSubject(), st.getScopes());
        token.setIssuer(issuer);
        token.setAudience(st.getClient().getClientId());
        
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(token);
        JoseHeaders headers = new JoseHeaders();
        JwsSignatureProvider theSigProvider = getInitializedSigProvider(headers);
        String idToken = producer.signWith(theSigProvider);
        
        JweEncryptionProvider theEncryptionProvider = getInitializedEncryptionProvider();
        if (theEncryptionProvider != null) {
            idToken = theEncryptionProvider.encrypt(StringUtils.toBytesUTF8(idToken), null);
        }
        ct.getParameters().put(OidcUtils.ID_TOKEN, idToken);
        
    }
    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }
    
    protected JwsSignatureProvider getInitializedSigProvider(JoseHeaders headers) {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        JwsSignatureProvider theSigProvider = JwsUtils.loadSignatureProvider(true); 
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
    public void setUserInfoProvider(UserInfoProvider userInfoProvider) {
        this.userInfoProvider = userInfoProvider;
    }
}
