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

public enum AccessTokenGrantType {
    AUTHORIZATION_CODE("authorization_code"),
    IMPLICIT("token"),
    CLIENT_CREDENTIALS("client_credentials"),
    RESOURCE_OWNER_CREDENTIALS("password"),
    SAML2(""),
    JWT("");
    
    private String type;
    
    private AccessTokenGrantType(String type) {
        this.type = type;
    }

    public String getGrantType() {
        return type;
    }
    
    public String toString() {
        return type;
    }
}
