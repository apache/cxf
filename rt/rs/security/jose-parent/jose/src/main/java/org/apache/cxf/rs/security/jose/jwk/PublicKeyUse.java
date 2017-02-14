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
package org.apache.cxf.rs.security.jose.jwk;


public enum PublicKeyUse {
    SIGN(JsonWebKey.PUBLIC_KEY_USE_SIGN),
    ENCRYPT(JsonWebKey.PUBLIC_KEY_USE_ENCRYPT);

    private final String use;
    PublicKeyUse(String use) {
        this.use = use;
    }
    public static PublicKeyUse getPublicKeyUse(String use) {
        if (use == null) {
            return null;
        }
        if (JsonWebKey.PUBLIC_KEY_USE_SIGN.equals(use)) {
            return SIGN;
        } else if (JsonWebKey.PUBLIC_KEY_USE_ENCRYPT.equals(use)) {
            return ENCRYPT;
        } else {
            return valueOf(use);
        }
    }
    public String toString() {
        return use;
    }

}
