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
package org.apache.cxf.rs.security.httpsignature;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class MockPublicKeyProvider implements PublicKeyProvider {
    @Override
    public PublicKey getKey(String keyId) {
        return getPublicKey(keyId);
    }

    private PublicKey getPublicKey(String keyId) {
        String publicKey = "-----BEGIN PUBLIC KEY-----\n"
                + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzqvSq5MPAX11nuh5zQqj\n"
                + "SZBDR9ErERCF+AoXs3uRJCroNIlaAuGZ3sXOYZCVCGkt28TMbH6Pnt8z9YfrH2Fl\n"
                + "SQWn6UDa+Dk7AjCtNLeOHInE7tlWuXZu4xPR2XgiKd90ky1xIbJL5PzAjzYLjTjE\n"
                + "Wi6uqvNexi/a8/BF85DP/LJVa5pbCzD3rSlIFNjLMcohs4qhWby7ZCPSUjGh6PMf\n"
                + "mhcBQtlScrqwhSPJeCIQ2eAMcZVDFRv+MVOzCIGrNan3/X8WGQMWJQQ+CXj1mgH1\n"
                + "t3mZy1a8WGzyBqhWzblsarO/tUEoOpd1DW9iX1JtJtLZmfNmXR6R3NUoiMaZoFeN\n"
                + "qQIDAQAB\n"
                + "-----END PUBLIC KEY-----\n";

        try {
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(SignatureHeaderUtils.loadPEM(publicKey.getBytes())));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
}
