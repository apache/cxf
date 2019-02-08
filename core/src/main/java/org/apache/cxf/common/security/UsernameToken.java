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
package org.apache.cxf.common.security;

public class UsernameToken implements SecurityToken {

    private String name;
    private String password;
    private String passwordType;
    private boolean isHashed;
    private String nonce;
    private String createdTime;

    public UsernameToken(String name,
                         String password,
                         String passwordType,
                         boolean isHashed,
                         String nonce,
                         String createdTime) {
        this.name = name;
        this.password = password;
        this.passwordType = passwordType;
        this.isHashed = isHashed;
        this.nonce = nonce;
        this.createdTime = createdTime;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordType() {
        return passwordType;
    }

    public boolean isHashed() {
        return isHashed;
    }

    public String getNonce() {
        return nonce;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public TokenType getTokenType() {
        return TokenType.UsernameToken;
    }
}
