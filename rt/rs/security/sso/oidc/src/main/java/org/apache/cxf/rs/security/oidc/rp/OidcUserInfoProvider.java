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

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;

public class OidcUserInfoProvider implements ContextProvider<UserInfoContext> {

    @Override
    public UserInfoContext createContext(Message m) {
        final OidcClientTokenContext ctx = (OidcClientTokenContext)
            m.getContent(ClientTokenContext.class);
        final UserInfo userInfo = ctx != null ? ctx.getUserInfo() : m.getContent(UserInfo.class);
        if (userInfo != null) {
            final IdToken idToken = ctx != null ? ctx.getIdToken() : m.getContent(IdToken.class);
            return new UserInfoContext() {

                @Override
                public UserInfo getUserInfo() {
                    return userInfo;
                }

                @Override
                public IdToken getIdToken() {
                    return idToken;
                }

            };
        }
        return null;

    }

}
