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
package org.apache.cxf.rs.security.oidc.idp;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;

@Entity
public class OidcUserSubject extends UserSubject {

    private static final long serialVersionUID = 8806727177012442229L;

    private IdToken idToken;

    private UserInfo userInfo;

    public OidcUserSubject() {

    }

    public OidcUserSubject(String login) {
        super(login);
    }

    public OidcUserSubject(String login, String id) {
        super(login, id);
    }

    public OidcUserSubject(UserSubject sub) {
        super(sub);
    }

    @Lob
    @Basic(fetch = FetchType.EAGER)
    public IdToken getIdToken() {
        return idToken;
    }

    public void setIdToken(IdToken idToken) {
        this.idToken = idToken;
    }

    @Lob
    @Basic(fetch = FetchType.EAGER)
    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

}
