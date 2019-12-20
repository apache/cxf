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

import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.IvParameterSpec;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;

public class AesCbcHmacJweDecryption extends JweDecryption {
    private final String supportedAlgo;

    public AesCbcHmacJweDecryption(KeyDecryptionProvider keyDecryptionAlgo) {
        this(keyDecryptionAlgo, null);
    }
    public AesCbcHmacJweDecryption(KeyDecryptionProvider keyDecryptionAlgo,
                                   ContentAlgorithm supportedAlgo) {
        super(keyDecryptionAlgo, new AesCbcContentDecryptionAlgorithm(supportedAlgo));
        this.supportedAlgo = supportedAlgo == null ? null : supportedAlgo.getJwaName();
    }
    protected JweDecryptionOutput doDecrypt(JweDecryptionInput jweDecryptionInput, byte[] cek) {
        validateAuthenticationTag(jweDecryptionInput, cek);
        return super.doDecrypt(jweDecryptionInput, cek);
    }
    @Override
    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        validateCekAlgorithm(algoJwt);
        return AesCbcHmacJweEncryption.doGetActualCek(theCek, algoJwt);
    }
    protected void validateAuthenticationTag(JweDecryptionInput jweDecryptionInput, byte[] theCek) {
        byte[] actualAuthTag = jweDecryptionInput.getAuthTag();

        final AesCbcHmacJweEncryption.MacState macState =
            AesCbcHmacJweEncryption.getInitializedMacState(theCek,
                                                           jweDecryptionInput.getInitVector(),
                                                           jweDecryptionInput.getAad(),
                                                           jweDecryptionInput.getJweHeaders(),
                                                           jweDecryptionInput.getDecodedJsonHeaders());
        macState.mac.update(jweDecryptionInput.getEncryptedContent());
        byte[] expectedAuthTag = AesCbcHmacJweEncryption.signAndGetTag(macState);
        if (!MessageDigest.isEqual(actualAuthTag, expectedAuthTag)) {
            LOG.warning("Invalid authentication tag");
            throw new JweException(JweException.Error.CONTENT_DECRYPTION_FAILURE);
        }

    }
    private static class AesCbcContentDecryptionAlgorithm extends AbstractContentEncryptionCipherProperties
        implements ContentDecryptionProvider {
        AesCbcContentDecryptionAlgorithm(ContentAlgorithm supportedAlgo) {
            super(supportedAlgo);
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
        public byte[] getEncryptedSequence(JweHeaders headers, byte[] cipher, byte[] authTag) {
            return cipher;
        }
    }
    private String validateCekAlgorithm(String cekAlgo) {
        if (!AlgorithmUtils.isAesCbcHmac(cekAlgo)
            || supportedAlgo != null && !supportedAlgo.equals(cekAlgo)) {
            LOG.warning("Invalid content encryption algorithm");
            throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
        }
        return cekAlgo;
    }
}
