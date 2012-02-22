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
package org.apache.cxf.rs.security.oauth.common;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Base Token representation
 */
@XmlRootElement
public class ClientAccessToken extends AccessToken {

    @XmlElement(name = "scope")
    private String approvedScope;
    @XmlElement(name = "refresh_token")
    private String refreshToken;
       
    public ClientAccessToken(AccessTokenType type, String tokenKey) {
        super(type, tokenKey);
    }

    public void setApprovedScope(String approvedScope) {
        this.approvedScope = approvedScope;
    }

    public String getApprovedScope() {
        return approvedScope;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

}
