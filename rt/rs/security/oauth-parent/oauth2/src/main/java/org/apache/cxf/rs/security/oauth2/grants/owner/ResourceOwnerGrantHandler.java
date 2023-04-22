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
package org.apache.cxf.rs.security.oauth2.grants.owner;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.AbstractGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * The "resource owner" grant handler
 */
public class ResourceOwnerGrantHandler extends AbstractGrantHandler {
    private ResourceOwnerLoginHandler loginHandler;

    public ResourceOwnerGrantHandler() {
        super(OAuthConstants.RESOURCE_OWNER_GRANT);
    }
    
    public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
        throws OAuthServiceException {

        String ownerName = params.getFirst(OAuthConstants.RESOURCE_OWNER_NAME);
        String ownerPassword = params.getFirst(OAuthConstants.RESOURCE_OWNER_PASSWORD);
        if (ownerName == null || ownerPassword == null) {
            throw new OAuthServiceException(
                 new OAuthError(OAuthConstants.INVALID_REQUEST));
        }
        UserSubject subject = loginHandler.createSubject(client, ownerName, ownerPassword);
        if (subject == null) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        return doCreateAccessToken(client, subject, params);
    }

    public ResourceOwnerLoginHandler getLoginHandler() {
        return this.loginHandler;
    }

    public void setLoginHandler(ResourceOwnerLoginHandler loginHandler) {
        this.loginHandler = loginHandler;
    }

    public void setMessageContext(MessageContext context) {
        if (loginHandler != null) {
            OAuthUtils.injectContextIntoOAuthProvider(context, loginHandler);
        }
    }
    
}
