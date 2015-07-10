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
package org.apache.cxf.rs.security.oidc.common;

import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

public class UserInfo extends JwtClaims {
    public static final String NAME_CLAIM = "name";
    public static final String PROFILE_CLAIM = "profile";
    public static final String EMAIL_CLAIM = "email";
    public static final String EMAIL_VERIFIED_CLAIM = "email_verified";
    public static final String BIRTHDATE_CLAIM = "birthdate";
    public static final String PHONE_CLAIM = "phone_number";
    public static final String ADDRESS_CLAIM = "address";
    public UserInfo() {
    }
    public UserInfo(JwtClaims claims) {
        this(claims.asMap());
    }
    public UserInfo(Map<String, Object> claims) {
        super(claims);
    }
    
    public void setName(String name) {
        setProperty(NAME_CLAIM, name);
    }
    public String getName() {
        return (String)getProperty(NAME_CLAIM);
    }
    public void setProfile(String name) {
        setProperty(PROFILE_CLAIM, name);
    }
    public String getProfile() {
        return (String)getProperty(PROFILE_CLAIM);
    }
    public void setEmail(String name) {
        setProperty(EMAIL_CLAIM, name);
    }
    public String getEmail() {
        return (String)getProperty(EMAIL_CLAIM);
    }
    public void setEmailVerified(Boolean verified) {
        setProperty(EMAIL_VERIFIED_CLAIM, verified);
    }
    public Boolean getEmailVerified() {
        return getBooleanProperty(EMAIL_VERIFIED_CLAIM);
    }
    public void setBirthDate(String date) {
        setProperty(BIRTHDATE_CLAIM, date);
    }
    public String getBirthdate() {
        return (String)getProperty(BIRTHDATE_CLAIM);
    }
    public String getPhoneNumber() {
        return (String)getProperty(PHONE_CLAIM);
    }
    public void setPhoneNumber(String name) {
        setProperty(PHONE_CLAIM, name);
    }
    public UserAddress getUserAddress() {
        Object value = getProperty(ADDRESS_CLAIM);
        if (value instanceof UserAddress) {
            return (UserAddress)value;
        } else if (value instanceof Map) {
            Map<String, Object> map = CastUtils.cast((Map<?, ?>)value); 
            return new UserAddress(map);
        } else {
            return null;
        }
    }
    public void setUserAddressNumber(UserAddress address) {
        setProperty(ADDRESS_CLAIM, address);
    }
    
}
