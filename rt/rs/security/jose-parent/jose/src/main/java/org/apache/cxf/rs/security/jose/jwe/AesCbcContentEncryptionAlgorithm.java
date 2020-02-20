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

import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.IvParameterSpec;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;

public class AesCbcContentEncryptionAlgorithm extends AbstractContentEncryptionAlgorithm {

    private static final Map<String, String> AES_HMAC_MAP;
    private static final Map<String, Integer> AES_CEK_SIZE_MAP;

    static {
        AES_HMAC_MAP = new HashMap<>();
        AES_HMAC_MAP.put(ContentAlgorithm.A128CBC_HS256.getJwaName(), AlgorithmUtils.HMAC_SHA_256_JAVA);
        AES_HMAC_MAP.put(ContentAlgorithm.A192CBC_HS384.getJwaName(), AlgorithmUtils.HMAC_SHA_384_JAVA);
        AES_HMAC_MAP.put(ContentAlgorithm.A256CBC_HS512.getJwaName(), AlgorithmUtils.HMAC_SHA_512_JAVA);

        AES_CEK_SIZE_MAP = new HashMap<>();
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A128CBC_HS256.getJwaName(), 32);
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A192CBC_HS384.getJwaName(), 48);
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A256CBC_HS512.getJwaName(), 64);
    }

    public AesCbcContentEncryptionAlgorithm(ContentAlgorithm algo, boolean generateCekOnce) {
        super(validateCekAlgorithm(algo), generateCekOnce);
    }

    public AesCbcContentEncryptionAlgorithm(byte[] cek, byte[] iv, ContentAlgorithm algo) {
        super(cek, iv, validateCekAlgorithm(algo));
    }

    @Override
    public AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] theIv) {
        return new IvParameterSpec(theIv);
    }

    @Override
    public byte[] getAdditionalAuthenticationData(String headersJson, byte[] aad) {
        return null;
    }

    @Override
    protected int getContentEncryptionKeySize(JweHeaders headers) {
        return getFullCekKeySize(getAlgorithm().getJwaName()) * 8;
    }

    protected static int getFullCekKeySize(String algoJwt) {
        return AES_CEK_SIZE_MAP.get(algoJwt);
    }

    protected static String getHMACAlgorithm(String algoJwt) {
        return AES_HMAC_MAP.get(algoJwt);
    }

    protected static ContentAlgorithm validateCekAlgorithm(ContentAlgorithm cekAlgo) {
        if (!AlgorithmUtils.isAesCbcHmac(cekAlgo.getJwaName())) {
            LOG.warning("Invalid content encryption algorithm");
            throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
        }
        return cekAlgo;
    }

}
