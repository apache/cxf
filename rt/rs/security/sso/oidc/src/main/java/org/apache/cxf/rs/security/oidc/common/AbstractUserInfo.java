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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public abstract class AbstractUserInfo extends JwtClaims {
    public static final String NAME_CLAIM = "name";
    public static final String GIVEN_NAME_CLAIM = "given_name";
    public static final String FAMILY_NAME_CLAIM = "family_name";
    public static final String MIDDLE_NAME_CLAIM = "middle_name";
    public static final String NICKNAME_CLAIM = "nickname";
    public static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    public static final String PROFILE_CLAIM = "profile";
    public static final String PICTURE_CLAIM = "picture";
    public static final String WEBSITE_CLAIM = "website";
    public static final String EMAIL_CLAIM = "email";
    public static final String EMAIL_VERIFIED_CLAIM = "email_verified";
    public static final String GENDER_CLAIM = "gender";
    public static final String ZONEINFO_CLAIM = "zoneinfo";
    public static final String LOCALE_CLAIM = "locale";
    public static final String BIRTHDATE_CLAIM = "birthdate";
    public static final String PHONE_CLAIM = "phone_number";
    public static final String PHONE_VERIFIED_CLAIM = "phone_number_verified";
    public static final String ADDRESS_CLAIM = "address";
    public static final String UPDATED_AT_CLAIM = "updated_at";
    private static final long serialVersionUID = 4554501320190745304L;

    public AbstractUserInfo() {
    }
    public AbstractUserInfo(JwtClaims claims) {
        this(claims.asMap());
    }
    public AbstractUserInfo(Map<String, Object> claims) {
        super(claims);
    }

    public void setName(String name) {
        setProperty(NAME_CLAIM, name);
    }
    public String getName() {
        return (String)getProperty(NAME_CLAIM);
    }
    public void setGivenName(String name) {
        setProperty(GIVEN_NAME_CLAIM, name);
    }
    public String getGivenName() {
        return (String)getProperty(GIVEN_NAME_CLAIM);
    }
    public void setFamilyName(String name) {
        setProperty(FAMILY_NAME_CLAIM, name);
    }
    public String getFamilyName() {
        return (String)getProperty(FAMILY_NAME_CLAIM);
    }
    public void setMiddleName(String name) {
        setProperty(MIDDLE_NAME_CLAIM, name);
    }
    public String getMiddleName() {
        return (String)getProperty(MIDDLE_NAME_CLAIM);
    }
    public void setNickName(String name) {
        setProperty(NICKNAME_CLAIM, name);
    }
    public String getNickName() {
        return (String)getProperty(NICKNAME_CLAIM);
    }
    public void setPreferredUserName(String name) {
        setProperty(PREFERRED_USERNAME_CLAIM, name);
    }
    public String getPreferredUserName() {
        return (String)getProperty(PREFERRED_USERNAME_CLAIM);
    }
    public void setProfile(String name) {
        setProperty(PROFILE_CLAIM, name);
    }
    public String getProfile() {
        return (String)getProperty(PROFILE_CLAIM);
    }
    public void setPicture(String name) {
        setProperty(PICTURE_CLAIM, name);
    }
    public String getPicture() {
        return (String)getProperty(PICTURE_CLAIM);
    }
    public void setWebsite(String name) {
        setProperty(WEBSITE_CLAIM, name);
    }
    public String getWebsite() {
        return (String)getProperty(WEBSITE_CLAIM);
    }
    public void setGender(String name) {
        setProperty(GENDER_CLAIM, name);
    }
    public String getGender() {
        return (String)getProperty(GENDER_CLAIM);
    }
    public void setZoneInfo(String name) {
        setProperty(ZONEINFO_CLAIM, name);
    }
    public String getZoneInfo() {
        return (String)getProperty(ZONEINFO_CLAIM);
    }
    public void setLocale(String name) {
        setProperty(LOCALE_CLAIM, name);
    }
    public String getLocale() {
        return (String)getProperty(LOCALE_CLAIM);
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
    public String getBirthDate() {
        return (String)getProperty(BIRTHDATE_CLAIM);
    }
    public String getPhoneNumber() {
        return (String)getProperty(PHONE_CLAIM);
    }
    public void setPhoneNumber(String name) {
        setProperty(PHONE_CLAIM, name);
    }
    public void setPhoneVerified(Boolean verified) {
        setProperty(PHONE_VERIFIED_CLAIM, verified);
    }
    public Boolean getPhoneVerified() {
        return getBooleanProperty(PHONE_VERIFIED_CLAIM);
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
    public void setUserAddress(UserAddress address) {
        setProperty(ADDRESS_CLAIM, address);
    }
    public void setUpdatedAt(Long time) {
        setProperty(UPDATED_AT_CLAIM, time);
    }
    public Long getUpdatedAt() {
        return getLongProperty(UPDATED_AT_CLAIM);
    }

    public void setAggregatedClaims(AggregatedClaims claims) {
        setAddClaimNames(claims.getClaimNames());
        Map<String, Map<String, String>> sources = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : claims.getClaimNames().entrySet()) {
            String source = entry.getValue();
            String jwt = claims.getClaimSources().get(source);
            sources.put(source,
                        Collections.singletonMap(OidcUtils.JWT_CLAIM_SOURCE_PROPERTY, jwt));

        }
        setAddClaimSources(sources);
    }
    private void setAddClaimSources(Map<String, Map<String, String>> newSources) {
        Map<String, Map<String, String>> sources =
            CastUtils.cast((Map<?, ?>)getProperty(OidcUtils.CLAIM_SOURCES_PROPERTY));
        if (sources == null) {
            setProperty(OidcUtils.CLAIM_SOURCES_PROPERTY, sources);
        } else {
            sources.putAll(newSources);
        }
    }
    private void setAddClaimNames(Map<String, String> claimNames) {
        Map<String, String> names = CastUtils.cast((Map<?, ?>)getProperty(OidcUtils.CLAIM_NAMES_PROPERTY));
        if (names == null) {
            setProperty(OidcUtils.CLAIM_NAMES_PROPERTY, claimNames);
        } else {
            names.putAll(claimNames);
        }
    }
    public AggregatedClaims getAggregatedClaims() {
        Map<String, String> names = CastUtils.cast((Map<?, ?>)getProperty(OidcUtils.CLAIM_NAMES_PROPERTY));
        Map<String, Map<String, String>> sources =
            CastUtils.cast((Map<?, ?>)getProperty(OidcUtils.CLAIM_SOURCES_PROPERTY));
        if (names == null || sources == null) {
            return null;
        }

        AggregatedClaims claims = new AggregatedClaims();

        Map<String, String> namesMap = new LinkedHashMap<>();
        Map<String, String> sourcesMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String source = entry.getValue();
            Map<String, String> sourceValue = sources.get(source);
            if (sourceValue != null && sourceValue.containsKey(OidcUtils.JWT_CLAIM_SOURCE_PROPERTY)) {
                namesMap.put(entry.getKey(), source);
                String jwt = sourceValue.values().iterator().next();
                sourcesMap.put(source, jwt);
            }
        }
        claims.setClaimNames(namesMap);
        claims.setClaimSources(sourcesMap);
        return claims;
    }
    public void setDistributedClaims(DistributedClaims claims) {
        setAddClaimNames(claims.getClaimNames());
        Map<String, Map<String, String>> sources = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : claims.getClaimNames().entrySet()) {
            String source = entry.getValue();
            DistributedClaimSource distSource = claims.getClaimSources().get(source);
            Map<String, String> mapSource = new LinkedHashMap<>();
            mapSource.put(OidcUtils.ENDPOINT_CLAIM_SOURCE_PROPERTY, distSource.getEndpoint());
            if (distSource.getAccessToken() != null) {
                mapSource.put(OidcUtils.TOKEN_CLAIM_SOURCE_PROPERTY, distSource.getAccessToken());
            }
            sources.put(source, mapSource);
        }
        setAddClaimSources(sources);
    }
    public DistributedClaims getDistributedClaims() {
        Map<String, String> names = CastUtils.cast((Map<?, ?>)getProperty(OidcUtils.CLAIM_NAMES_PROPERTY));
        Map<String, Map<String, String>> sources =
            CastUtils.cast((Map<?, ?>)getProperty(OidcUtils.CLAIM_SOURCES_PROPERTY));
        if (names == null || sources == null) {
            return null;
        }
        DistributedClaims claims = new DistributedClaims();
        Map<String, String> namesMap = new LinkedHashMap<>();
        Map<String, DistributedClaimSource> sourcesMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String source = entry.getValue();
            Map<String, String> sourceValue = sources.get(source);
            if (sourceValue != null
                && !sourceValue.containsKey(OidcUtils.JWT_CLAIM_SOURCE_PROPERTY)) {
                namesMap.put(entry.getKey(), source);
                DistributedClaimSource distSource = new DistributedClaimSource();
                distSource.setEndpoint(sourceValue.get(OidcUtils.ENDPOINT_CLAIM_SOURCE_PROPERTY));
                distSource.setAccessToken(sourceValue.get(OidcUtils.TOKEN_CLAIM_SOURCE_PROPERTY));
                sourcesMap.put(source, distSource);
            }
        }
        claims.setClaimNames(namesMap);
        claims.setClaimSources(sourcesMap);
        return claims;
    }
}
