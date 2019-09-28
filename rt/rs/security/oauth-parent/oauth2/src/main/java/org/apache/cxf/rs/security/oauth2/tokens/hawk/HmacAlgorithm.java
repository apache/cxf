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

package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public enum HmacAlgorithm {

    HmacSHA1(OAuthConstants.HMAC_ALGO_SHA_1),
    HmacSHA256(OAuthConstants.HMAC_ALGO_SHA_256);

    private final String oauthName;

    HmacAlgorithm(String oauthName) {
        this.oauthName = oauthName;
    }

    public String getOAuthName() {
        return oauthName;
    }

    public String getJavaName() {
        return name();
    }

    public static HmacAlgorithm toHmacAlgorithm(String value) {
        for (HmacAlgorithm ha : HmacAlgorithm.values()) {
            if (ha.oauthName.equals(value)) {
                return ha;
            }
        }
        throw new IllegalArgumentException(value);
    }

}