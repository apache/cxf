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

class EcdhHelper {

    private final ECPublicKey peerPublicKey;
    private final String ecurve;
    private final byte[] apuBytes;
    private final byte[] apvBytes;
    private final String ctAlgo;

    EcdhHelper(ECPublicKey peerPublicKey,
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
        KeyAlgorithm keyAlgo = headers.getKeyEncryptionAlgorithm();
        ContentAlgorithm contentAlgo = ContentAlgorithm.valueOf(ctAlgo);
        String algorithm = (KeyAlgorithm.isDirect(keyAlgo)) ? contentAlgo.getJwaName() : keyAlgo.getJwaName();
        int keySizeBits = (KeyAlgorithm.isDirect(keyAlgo)) ? contentAlgo.getKeySizeBits() : keyAlgo.getKeySizeBits();

        if (apuBytes != null) {
            headers.setHeader("apu", Base64UrlUtility.encode(apuBytes));
        }
        if (apvBytes != null) {
            headers.setHeader("apv", Base64UrlUtility.encode(apvBytes));
        }
        headers.setJsonWebKey("epk", JwkUtils.fromECPublicKey(publicKey, ecurve));

        return JweUtils.getECDHKey(privateKey, peerPublicKey, apuBytes, apvBytes,
                                   algorithm, keySizeBits);
    }
    private byte[] toApuBytes(String apuString) {
        if (apuString != null) {
            return toBytes(apuString);
        }
        return CryptoUtils.generateSecureRandomBytes(512 / 8);

    }
    private byte[] toBytes(String str) {
        return str == null ? null : StringUtils.toBytesUTF8(str);
    }

}
