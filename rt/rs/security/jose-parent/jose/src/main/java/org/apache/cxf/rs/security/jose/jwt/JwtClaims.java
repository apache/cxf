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

package org.apache.cxf.rs.security.jose.jwt;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;




public class JwtClaims extends JsonMapObject {

    private static final long serialVersionUID = 6274136637301800283L;

    public JwtClaims() {
    }

    public JwtClaims(Map<String, Object> values) {
        super(values);
    }

    public void setIssuer(String issuer) {
        setClaim(JwtConstants.CLAIM_ISSUER, issuer);
    }

    public String getIssuer() {
        return (String)getClaim(JwtConstants.CLAIM_ISSUER);
    }

    public void setSubject(String subject) {
        setClaim(JwtConstants.CLAIM_SUBJECT, subject);
    }

    public String getSubject() {
        return (String)getClaim(JwtConstants.CLAIM_SUBJECT);
    }

    /**
     * Set a single audience value which will be serialized as a String
     * @param audience the audience
     */
    public void setAudience(String audience) {
        setClaim(JwtConstants.CLAIM_AUDIENCE, audience);
    }

    /**
     * Get a single audience value. If the audience claim value is an array then the
     * first value will be returned.
     * @return the audience
     */
    public String getAudience() {
        List<String> audiences = getAudiences();
        if (!StringUtils.isEmpty(audiences)) {
            return audiences.get(0);
        }
        return null;
    }

    /**
     * Set an array of audiences
     * @param audiences the audiences array
     */
    public void setAudiences(List<String> audiences) {
        setClaim(JwtConstants.CLAIM_AUDIENCE, audiences);
    }

    /**
     * Get an array of audiences
     * @return the audiences array
     */
    public List<String> getAudiences() {
        Object audiences = getClaim(JwtConstants.CLAIM_AUDIENCE);
        if (audiences instanceof List<?>) {
            return CastUtils.cast((List<?>)audiences);
        } else if (audiences instanceof String) {
            return Collections.singletonList((String)audiences);
        }

        return Collections.emptyList();
    }

    public void setExpiryTime(Long expiresIn) {
        setClaim(JwtConstants.CLAIM_EXPIRY, expiresIn);
    }

    public Long getExpiryTime() {
        return getLongProperty(JwtConstants.CLAIM_EXPIRY);
    }

    public void setNotBefore(Long notBefore) {
        setClaim(JwtConstants.CLAIM_NOT_BEFORE, notBefore);
    }

    public Long getNotBefore() {
        return getLongProperty(JwtConstants.CLAIM_NOT_BEFORE);
    }

    public void setIssuedAt(Long issuedAt) {
        setClaim(JwtConstants.CLAIM_ISSUED_AT, issuedAt);
    }

    public Long getIssuedAt() {
        return getLongProperty(JwtConstants.CLAIM_ISSUED_AT);
    }

    public void setTokenId(String id) {
        setClaim(JwtConstants.CLAIM_JWT_ID, id);
    }

    public String getTokenId() {
        return (String)getClaim(JwtConstants.CLAIM_JWT_ID);
    }

    public JwtClaims setClaim(String name, Object value) {
        setProperty(name, value);
        return this;
    }

    public Object getClaim(String name) {
        return getProperty(name);
    }
}
