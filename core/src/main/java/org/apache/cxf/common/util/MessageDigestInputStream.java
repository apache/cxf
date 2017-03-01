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
package org.apache.cxf.common.util;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestInputStream extends java.security.DigestInputStream {
    public static final String ALGO_SHA_256 = "SHA-256";

    public MessageDigestInputStream(InputStream is) {
        super(is, getDigestInstance(ALGO_SHA_256));
    }

    private static MessageDigest getDigestInstance(String algo)  {
        try {
            return MessageDigest.getInstance(algo);
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex);
        }
    }
    public byte[] getDigestBytes() {
        return super.getMessageDigest().digest();
    }
    public String getBase64Digest() {
        return Base64Utility.encode(getDigestBytes());
    }
    public String getBase64UrlDigest() {
        return Base64UrlUtility.encode(getDigestBytes());
    }
}
