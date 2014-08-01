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
package org.apache.cxf.rs.security.oauth2.jwe;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.HmacUtils;

public class AesCbcHmacJweEncryption extends AbstractJweEncryption {
    private static final Set<String> SUPPORTED_CEK_ALGORITHMS = new HashSet<String>(
        Arrays.asList(Algorithm.A128CBC_HS256.getJwtName(),
                      Algorithm.A192CBC_HS384.getJwtName(),
                      Algorithm.A256CBC_HS512.getJwtName()));
    private static final Map<String, String> AES_HMAC_MAP;
    private static final Map<String, Integer> AES_CEK_SIZE_MAP;
    static {
        AES_HMAC_MAP = new HashMap<String, String>();
        AES_HMAC_MAP.put(Algorithm.A128CBC_HS256.getJwtName(), Algorithm.HMAC_SHA_256_JAVA);
        AES_HMAC_MAP.put(Algorithm.A192CBC_HS384.getJwtName(), Algorithm.HMAC_SHA_384_JAVA);
        AES_HMAC_MAP.put(Algorithm.A256CBC_HS512.getJwtName(), Algorithm.HMAC_SHA_512_JAVA);
        
        AES_CEK_SIZE_MAP = new HashMap<String, Integer>();
        AES_CEK_SIZE_MAP.put(Algorithm.A128CBC_HS256.getJwtName(), 32);
        AES_CEK_SIZE_MAP.put(Algorithm.A192CBC_HS384.getJwtName(), 48);
        AES_CEK_SIZE_MAP.put(Algorithm.A256CBC_HS512.getJwtName(), 64);
    }
    public AesCbcHmacJweEncryption(JweHeaders headers, 
                                   KeyEncryptionAlgorithm keyEncryptionAlgorithm) {
        this(headers, null, null, keyEncryptionAlgorithm);
    }
    public AesCbcHmacJweEncryption(JweHeaders headers, byte[] cek, 
                                   byte[] iv, KeyEncryptionAlgorithm keyEncryptionAlgorithm) {
        this(headers, cek, iv, keyEncryptionAlgorithm, null);
    }
    public AesCbcHmacJweEncryption(JweHeaders headers, 
                                   byte[] cek, 
                                   byte[] iv, 
                                   KeyEncryptionAlgorithm keyEncryptionAlgorithm,
                                   JwtHeadersWriter writer) {
        super(headers, new AesCbcContentEncryptionAlgorithm(cek, iv), keyEncryptionAlgorithm, writer);
        if (!SUPPORTED_CEK_ALGORITHMS.contains(headers.getContentEncryptionAlgorithm())) {
            throw new SecurityException();
        }
    }
    
    protected byte[] getActualCek(byte[] theCek) {
        int size = getCekKeySize() / 2;
        byte[] actualCek = new byte[size];
        System.arraycopy(theCek, size, actualCek, 0, size);
        return actualCek;
    }
    
    protected int getCekKeySize() {
        return AES_CEK_SIZE_MAP.get(super.getContentEncryptionAlgoJwt());
    }
    
    protected JweCompactProducer getJweCompactProducer(JweEncryptionInternal state, byte[] cipher) {
        int size = getCekKeySize() / 2;
        byte[] macKey = new byte[size];
        System.arraycopy(state.secretKey, 0, macKey, 0, size);
        
        String hmacAlgoJava = AES_HMAC_MAP.get(super.getContentEncryptionAlgoJwt());
        Mac mac = HmacUtils.getInitializedMac(macKey, hmacAlgoJava, null);
        
        String headersJson = getJwtHeadersWriter().headersToJson(state.theHeaders);
        byte[] aad = JweHeaders.toCipherAdditionalAuthData(headersJson);
        ByteBuffer buf = ByteBuffer.allocate(8);
        byte[] al = buf.putInt(0).putInt(aad.length * 8).array();
        
        mac.update(aad);
        mac.update(state.theIv);
        mac.update(cipher);
        mac.update(al);
        byte[] sig = mac.doFinal();
        int authTagLen = getAuthTagLen() / 8;
        byte[] authTag = new byte[authTagLen];
        System.arraycopy(sig, 0, authTag, 0, authTagLen);
        
        return new JweCompactProducer(headersJson,
                                      state.jweContentEncryptionKey,
                                      state.theIv,
                                      cipher,
                                      authTag);
    }
    
    
    protected byte[] getEncryptedContentEncryptionKey(byte[] theCek) {
        return getKeyEncryptionAlgo().getEncryptedContentEncryptionKey(getJweHeaders(), theCek);
    }
    
    private static class AesCbcContentEncryptionAlgorithm extends AbstractContentEncryptionAlgorithm {
        public AesCbcContentEncryptionAlgorithm(byte[] cek, byte[] iv) { 
            super(cek, iv);    
        }
        @Override
        public AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] theIv) {
            return new IvParameterSpec(theIv);
        }
        @Override
        public byte[] getAAD(JweHeaders theHeaders, JwtHeadersWriter writer) {
            return null;
        }
    }
}
