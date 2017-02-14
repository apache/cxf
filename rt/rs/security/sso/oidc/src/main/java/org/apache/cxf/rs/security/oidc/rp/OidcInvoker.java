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
package org.apache.cxf.rs.security.oidc.rp;

import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oauth2.client.OAuthInvoker;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class OidcInvoker extends OAuthInvoker {
    private IdTokenReader idTokenReader;
    @Override
    protected void validateRefreshedToken(ClientTokenContext tokenContext, ClientAccessToken refreshedToken) {
        if (refreshedToken.getParameters().containsKey(OidcUtils.ID_TOKEN)) {
            IdToken newIdToken = idTokenReader.getIdToken(refreshedToken, getConsumer());

            OidcClientTokenContextImpl oidcContext = (OidcClientTokenContextImpl)tokenContext;
            IdToken currentIdToken = oidcContext.getIdToken();

            if (!newIdToken.getIssuer().equals(currentIdToken.getIssuer())) {
                throw new OAuthServiceException("Invalid id token issuer");
            }
            if (!newIdToken.getSubject().equals(currentIdToken.getSubject())) {
                throw new OAuthServiceException("Invalid id token subject");
            }
            if (!newIdToken.getAudiences().containsAll(currentIdToken.getAudiences())) {
                throw new OAuthServiceException("Invalid id token audience(s)");
            }
            Long newAuthTime = newIdToken.getAuthenticationTime();
            if (newAuthTime != null && !newAuthTime.equals(currentIdToken.getAuthenticationTime())) {
                throw new OAuthServiceException("Invalid id token auth_time");
            }
            String newAzp = newIdToken.getAuthorizedParty();
            String origAzp = currentIdToken.getAuthorizedParty();
            if (newAzp != null && origAzp == null
                || newAzp == null && origAzp != null
                || newAzp != null && origAzp != null && !newAzp.equals(origAzp)) {
                throw new OAuthServiceException("Invalid id token authorized party");
            }
            Long newIssuedTime = newIdToken.getIssuedAt();
            Long origIssuedTime = currentIdToken.getIssuedAt();
            if (newIssuedTime < origIssuedTime) {
                throw new OAuthServiceException("Invalid id token issued time");
            }

            oidcContext.setIdToken(newIdToken);

        }
    }
    public void setIdTokenReader(IdTokenReader idTokenReader) {
        this.idTokenReader = idTokenReader;
    }
}
