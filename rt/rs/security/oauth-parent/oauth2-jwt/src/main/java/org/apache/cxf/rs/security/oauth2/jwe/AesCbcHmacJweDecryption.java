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

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.spec.IvParameterSpec;

import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersReader;

public class AesCbcHmacJweDecryption extends AbstractJweDecryption {
    public AesCbcHmacJweDecryption(KeyDecryptionAlgorithm keyDecryptionAlgo) {
        this(keyDecryptionAlgo, null, null);
    }
    public AesCbcHmacJweDecryption(KeyDecryptionAlgorithm keyDecryptionAlgo,
                                   JweCryptoProperties props, 
                                   JwtHeadersReader reader) {
        super(props, reader, keyDecryptionAlgo, new AesCbcContentDecryptionAlgorithm());
    }
    protected JweDecryptionOutput doDecrypt(JweCompactConsumer consumer, byte[] cek) {
        validateAuthenticationTag(consumer, cek);
        return super.doDecrypt(consumer, cek);
    }
    @Override
    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        return AesCbcHmacJweEncryption.doGetActualCek(theCek, algoJwt);
    }
    protected void validateAuthenticationTag(JweCompactConsumer consumer, byte[] theCek) {
        byte[] actualAuthTag = consumer.getEncryptionAuthenticationTag();
        
        final AesCbcHmacJweEncryption.MacState macState = 
            AesCbcHmacJweEncryption.getInitializedMacState(theCek, 
                                                           consumer.getContentDecryptionCipherInitVector(),
                                                           consumer.getJweHeaders(),
                                                           consumer.getDecodedJsonHeaders());
        macState.mac.update(consumer.getEncryptedContent());
        byte[] expectedAuthTag = AesCbcHmacJweEncryption.signAndGetTag(macState);
        if (!Arrays.equals(actualAuthTag, expectedAuthTag)) {
            throw new SecurityException();
        }
        
    }
    private static class AesCbcContentDecryptionAlgorithm extends AbstractContentEncryptionCipherProperties
        implements ContentDecryptionAlgorithm {
        @Override
        public AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] theIv) {
            return new IvParameterSpec(theIv);
        }
        @Override
        public byte[] getAdditionalAuthenticationData(String headersJson) {
            return null;
        }
        @Override
        public byte[] getEncryptedSequence(byte[] cipher, byte[] authTag) {
            return cipher;
        }
    }
    
}
