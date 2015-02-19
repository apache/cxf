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
package org.apache.cxf.rs.security.oauth.provider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.oauth.OAuthException;

/**
 * The utility MD5 sequence generator which can be used for generating
 * request or access token keys and secrets as well as request token
 * verifiers
 */
public class MD5SequenceGenerator {
    public String generate(byte[] input) throws OAuthException {
        if (input == null) {
            throw new OAuthException("You have to pass input to Token Generator");
        }

        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(input);
            byte[] messageDigest = algorithm.digest();
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new OAuthException(e);
        }
    }
}
