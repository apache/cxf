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
package org.apache.cxf.rs.security.jose.jwe;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;


public class JweCompactProducer  {
    private final JweHeaders headers;
    private final String data;

    public JweCompactProducer(String data) {
        this(new JweHeaders(), data);
    }
    public JweCompactProducer(JweHeaders joseHeaders, String data) {
        this.headers = joseHeaders;
        this.data = data;
    }

    public String encryptWith(JsonWebKey key) {
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(key, headers);
        return encryptWith(jwe);
    }
    public String encryptWith(PublicKey key) {
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(key, headers);
        return encryptWith(jwe);
    }
    public String encryptWith(SecretKey key) {
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(key, headers);
        return encryptWith(jwe);
    }
    public String encryptWith(JweEncryptionProvider jwe) {
        return jwe.encrypt(StringUtils.toBytesUTF8(data), headers);
    }
}
