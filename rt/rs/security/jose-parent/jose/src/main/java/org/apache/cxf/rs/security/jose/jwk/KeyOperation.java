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


public enum KeyOperation {
    SIGN(JsonWebKey.KEY_OPER_SIGN),
    VERIFY(JsonWebKey.KEY_OPER_VERIFY),
    ENCRYPT(JsonWebKey.KEY_OPER_ENCRYPT),
    DECRYPT(JsonWebKey.KEY_OPER_DECRYPT),
    WRAPKEY(JsonWebKey.KEY_OPER_WRAP_KEY),
    UNWRAPKEY(JsonWebKey.KEY_OPER_UNWRAP_KEY),
    DERIVEKEY(JsonWebKey.KEY_OPER_DERIVE_KEY),
    DERIVEBITS(JsonWebKey.KEY_OPER_DERIVE_BITS);

    private final String oper;
    KeyOperation(String oper) {
        this.oper = oper;
    }
    public static KeyOperation getKeyOperation(String oper) {
        if (oper == null) {
            return null;
        }
        return valueOf(oper.toUpperCase());
    }
    public String toString() {
        return oper;
    }

}
