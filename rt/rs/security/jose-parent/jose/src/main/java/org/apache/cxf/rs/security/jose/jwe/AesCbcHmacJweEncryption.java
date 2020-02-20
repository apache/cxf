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

import javax.crypto.Mac;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweException.Error;
import org.apache.cxf.rt.security.crypto.HmacUtils;

public class AesCbcHmacJweEncryption extends JweEncryption {

    public AesCbcHmacJweEncryption(ContentAlgorithm cekAlgoJwt,
                                   KeyEncryptionProvider keyEncryptionAlgorithm) {
        this(cekAlgoJwt, keyEncryptionAlgorithm, false);
    }

    public AesCbcHmacJweEncryption(ContentAlgorithm cekAlgoJwt,
                                   KeyEncryptionProvider keyEncryptionAlgorithm,
                                   boolean generateCekOnce) {
        super(keyEncryptionAlgorithm, new AesCbcContentEncryptionAlgorithm(cekAlgoJwt, generateCekOnce));
    }

    public AesCbcHmacJweEncryption(ContentAlgorithm cekAlgoJwt, byte[] cek,
                                   byte[] iv, KeyEncryptionProvider keyEncryptionAlgorithm) {
        super(keyEncryptionAlgorithm, new AesCbcContentEncryptionAlgorithm(cek, iv, cekAlgoJwt));
    }

    public AesCbcHmacJweEncryption(KeyEncryptionProvider keyEncryptionAlgorithm,
        AesCbcContentEncryptionAlgorithm contentEncryptionAlgorithm) {
        super(keyEncryptionAlgorithm, contentEncryptionAlgorithm);
    }

    @Override
    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        return doGetActualCek(theCek, algoJwt);
    }

    protected static byte[] doGetActualCek(byte[] theCek, String algoJwt) {
        // K
        int inputKeySize = AesCbcContentEncryptionAlgorithm.getFullCekKeySize(algoJwt);
        if (theCek.length != inputKeySize) {
            LOG.warning("Length input key [" + theCek.length + "] invalid for algorithm " + algoJwt
                + " [" + inputKeySize + "]");
            throw new JweException(Error.INVALID_CONTENT_KEY);
        }
        // MAC_KEY, ENC_KEY
        int secondaryKeySize = inputKeySize / 2;
        // Extract secondary key ENC_KEY from the input key K
        byte[] encKey = new byte[secondaryKeySize];
        System.arraycopy(theCek, secondaryKeySize, encKey, 0, secondaryKeySize);
        return encKey;
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
        String algoJwt = theHeaders.getContentEncryptionAlgorithm().getJwaName();
        int size = AesCbcContentEncryptionAlgorithm.getFullCekKeySize(algoJwt) / 2;
        byte[] macKey = new byte[size];
        System.arraycopy(secretKey, 0, macKey, 0, size);

        String hmacAlgoJava = AesCbcContentEncryptionAlgorithm.getHMACAlgorithm(algoJwt);
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

    protected static class MacState {
        protected Mac mac;
        private byte[] al;
    }

}
