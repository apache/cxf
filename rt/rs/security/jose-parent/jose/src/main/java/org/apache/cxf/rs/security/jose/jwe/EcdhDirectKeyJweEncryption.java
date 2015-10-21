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

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;


public class EcdhDirectKeyJweEncryption extends JweEncryption {
    public EcdhDirectKeyJweEncryption(ECPublicKey peerPublicKey,
                                      String curve,
                                      ContentAlgorithm ctAlgo) {
        this(peerPublicKey, curve, null, null, ctAlgo);
    }
    public EcdhDirectKeyJweEncryption(ECPublicKey peerPublicKey,
                                      String curve,
                                      String apuString,
                                      String apvString,
                                      ContentAlgorithm ctAlgo) {
        super(new EcdhDirectKeyEncryptionAlgorithm(),
              new EcdhAesGcmContentEncryptionAlgorithm(peerPublicKey,
                                                       curve,
                                                       apuString,
                                                       apvString,
                                                       ctAlgo));
    }
    protected static class EcdhDirectKeyEncryptionAlgorithm extends DirectKeyEncryptionAlgorithm {
        protected void checkKeyEncryptionAlgorithm(JweHeaders headers) {
            headers.setKeyEncryptionAlgorithm(KeyAlgorithm.ECDH_ES_DIRECT);
        }
    }
    protected static class EcdhAesGcmContentEncryptionAlgorithm extends AesGcmContentEncryptionAlgorithm {
        private EcdhHelper helper;
        public EcdhAesGcmContentEncryptionAlgorithm(ECPublicKey peerPublicKey,
                                                    String curve,
                                                    String apuString,
                                                    String apvString,
                                                    ContentAlgorithm ctAlgo) {
            super(ctAlgo);
            helper = new EcdhHelper(peerPublicKey, curve, apuString, apvString, ctAlgo.getJwaName());
        }
        public byte[] getContentEncryptionKey(JweHeaders headers) {
            return helper.getDerivedKey(headers);
        }
    }
    
    protected static class EcdhHelper {
        private ECPublicKey peerPublicKey;
        private String ecurve;
        private byte[] apuBytes;
        private byte[] apvBytes;
        private String ctAlgo;
        public EcdhHelper(ECPublicKey peerPublicKey,
                                                    String curve,
                                                    String apuString,
                                                    String apvString,
                                                    String ctAlgo) {
            this.ctAlgo = ctAlgo;
            this.peerPublicKey = peerPublicKey;
            this.ecurve = curve;
            // JWA spec suggests the "apu" field MAY either be omitted or
            // represent a random 512-bit value (...) and the "apv" field SHOULD NOT be present."
            this.apuBytes = toApuBytes(apuString);
            this.apvBytes = toBytes(apvString);
        }
        public byte[] getDerivedKey(JweHeaders headers) {
            KeyPair pair = CryptoUtils.generateECKeyPair(ecurve);
            ECPublicKey publicKey = (ECPublicKey)pair.getPublic();
            ECPrivateKey privateKey = (ECPrivateKey)pair.getPrivate();
            ContentAlgorithm jwtAlgo = ContentAlgorithm.valueOf(ctAlgo);
        
            headers.setHeader("apu", Base64UrlUtility.encode(apuBytes));
            headers.setHeader("apv", Base64UrlUtility.encode(apvBytes));
            headers.setJsonWebKey("epv", JwkUtils.fromECPublicKey(publicKey, ecurve));
            
            return JweUtils.getECDHKey(privateKey, peerPublicKey, apuBytes, apvBytes, 
                                       jwtAlgo.getJwaName(), jwtAlgo.getKeySizeBits());
            
        }
        private byte[] toApuBytes(String apuString) {
            if (apuString != null) {
                return toBytes(apuString);
            } else {
                return CryptoUtils.generateSecureRandomBytes(512 / 8);    
            }
            
        }
        private byte[] toBytes(String str) {
            return str == null ? null : StringUtils.toBytesUTF8(str);
        }
        
    }
    
}
