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

package org.apache.cxf.rs.security.oauth2.provider;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

/**
 * ClientSecretVerifier which checks the passwords against hashes
 */
public class ClientSecretHashVerifier implements ClientSecretVerifier {
    private String hashAlgorithm = MessageDigestUtils.ALGO_SHA_256;
    public boolean validateClientSecret(Client client, String clientSecret) {
        String hash = MessageDigestUtils.generate(StringUtils.toBytesUTF8(clientSecret),
                                                  hashAlgorithm);
        return hash.equals(client.getClientSecret());
    }
    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
}
