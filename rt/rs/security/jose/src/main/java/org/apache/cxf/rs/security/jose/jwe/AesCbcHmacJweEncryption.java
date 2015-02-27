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

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;

import org.apache.cxf.common.util.crypto.HmacUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;

public class AesCbcHmacJweEncryption extends JweEncryption {
    private static final Map<String, String> AES_HMAC_MAP;
    private static final Map<String, Integer> AES_CEK_SIZE_MAP;
    static {
        AES_HMAC_MAP = new HashMap<String, String>();
        AES_HMAC_MAP.put(ContentAlgorithm.A128CBC_HS256.getJwaName(), AlgorithmUtils.HMAC_SHA_256_JAVA);
        AES_HMAC_MAP.put(ContentAlgorithm.A192CBC_HS384.getJwaName(), AlgorithmUtils.HMAC_SHA_384_JAVA);
        AES_HMAC_MAP.put(ContentAlgorithm.A256CBC_HS512.getJwaName(), AlgorithmUtils.HMAC_SHA_512_JAVA);
        
        AES_CEK_SIZE_MAP = new HashMap<String, Integer>();
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A128CBC_HS256.getJwaName(), 32);
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A192CBC_HS384.getJwaName(), 48);
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A256CBC_HS512.getJwaName(), 64);
    }
    public AesCbcHmacJweEncryption(String cekAlgo, 
                                   KeyEncryptionProvider keyEncryptionAlgorithm) {
        this(ContentAlgorithm.getAlgorithm(cekAlgo), keyEncryptionAlgorithm);
    }
    public AesCbcHmacJweEncryption(ContentAlgorithm cekAlgoJwt, 
                                   KeyEncryptionProvider keyEncryptionAlgorithm) {
        this(cekAlgoJwt, null, null, keyEncryptionAlgorithm);
    }
    public AesCbcHmacJweEncryption(ContentAlgorithm cekAlgoJwt, byte[] cek, 
                                   byte[] iv, KeyEncryptionProvider keyEncryptionAlgorithm) {
        super(keyEncryptionAlgorithm,
              new AesCbcContentEncryptionAlgorithm(cek, iv, 
                                                   validateCekAlgorithm(cekAlgoJwt)));
        
    }
    @Override
    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        return doGetActualCek(theCek, algoJwt);
    }
    @Override
    protected int getCekSize(String algoJwt) {
        return getFullCekKeySize(algoJwt) * 8;
    }
    protected static byte[] doGetActualCek(byte[] theCek, String algoJwt) {
        int size = getFullCekKeySize(algoJwt) / 2;
        byte[] actualCek = new byte[size];
        System.arraycopy(theCek, size, actualCek, 0, size);
        return actualCek;
    }
    
    protected static int getFullCekKeySize(String algoJwt) {
        return AES_CEK_SIZE_MAP.get(algoJwt);
    }
    protected byte[] getActualCipher(byte[] cipher) {
        return cipher;
    }
    protected byte[] getAuthenticationTag(JweEncryptionInternal state, byte[] cipher) {
        final MacState macState = getInitializedMacState(state);
        macState.mac.update(cipher);
        return signAndGetTag(macState);
    }
    
    protected static byte[] signAndGetTag(MacState macState) {
        macState.mac.update(macState.al);
        byte[] sig = macState.mac.doFinal();
        
        int authTagLen = DEFAULT_AUTH_TAG_LENGTH / 8;
        byte[] authTag = new byte[authTagLen];
        System.arraycopy(sig, 0, authTag, 0, authTagLen);
        return authTag;
    }
    private MacState getInitializedMacState(final JweEncryptionInternal state) {
        return getInitializedMacState(state.secretKey, state.theIv, state.aad, 
                                      state.theHeaders, state.protectedHeadersJson);
    }
    protected static MacState getInitializedMacState(byte[] secretKey,
                                                     byte[] theIv,
                                                     byte[] extraAad,
                                                     JweHeaders theHeaders,
                                                     String protectedHeadersJson) {
        String algoJwt = theHeaders.getContentEncryptionAlgorithm();
        int size = getFullCekKeySize(algoJwt) / 2;
        byte[] macKey = new byte[size];
        System.arraycopy(secretKey, 0, macKey, 0, size);
        
        String hmacAlgoJava = AES_HMAC_MAP.get(algoJwt);
        Mac mac = HmacUtils.getInitializedMac(macKey, hmacAlgoJava, null);
        
        byte[] aad = JweUtils.getAdditionalAuthenticationData(protectedHeadersJson, extraAad);
        ByteBuffer buf = ByteBuffer.allocate(8);
        final byte[] al = buf.putInt(0).putInt(aad.length * 8).array();
        
        mac.update(aad);
        mac.update(theIv);
        MacState macState = new MacState();
        macState.mac = mac;
        macState.al = al;
        return macState;
    }
    
    protected AuthenticationTagProducer getAuthenticationTagProducer(final JweEncryptionInternal state) {
        final MacState macState = getInitializedMacState(state);
        
        
        return new AuthenticationTagProducer() {

            @Override
            public void update(byte[] cipher, int off, int len) {
                macState.mac.update(cipher, off, len);
            }

            @Override
            public byte[] getTag() {
                return signAndGetTag(macState);
            }
        };
    }
    @Override
    protected byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] theCek) {
        return getKeyEncryptionAlgo().getEncryptedContentEncryptionKey(headers, theCek);
    }
    
    private static class AesCbcContentEncryptionAlgorithm extends AbstractContentEncryptionAlgorithm {
        public AesCbcContentEncryptionAlgorithm(byte[] cek, byte[] iv, ContentAlgorithm algo) { 
            super(cek, iv, algo);    
        }
        @Override
        public AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] theIv) {
            return new IvParameterSpec(theIv);
        }
        @Override
        public byte[] getAdditionalAuthenticationData(String headersJson, byte[] aad) {
            return null;
        }
    }
    
    protected static class MacState {
        protected Mac mac;
        private byte[] al;
    }
    
    private static ContentAlgorithm validateCekAlgorithm(ContentAlgorithm cekAlgo) {
        if (!AlgorithmUtils.isAesCbcHmac(cekAlgo.getJwaName())) {
            throw new SecurityException();
        }
        return cekAlgo;
    }
}
