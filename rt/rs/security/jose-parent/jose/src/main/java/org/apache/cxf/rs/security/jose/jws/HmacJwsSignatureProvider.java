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
package org.apache.cxf.rs.security.jose.jws;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Mac;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rt.security.crypto.HmacUtils;

public class HmacJwsSignatureProvider extends AbstractJwsSignatureProvider {
    private final byte[] key;
    private final AlgorithmParameterSpec hmacSpec;

    public HmacJwsSignatureProvider(byte[] key, SignatureAlgorithm algo) {
        this(key, null, algo);
    }
    public HmacJwsSignatureProvider(byte[] key, AlgorithmParameterSpec spec, SignatureAlgorithm algo) {
        super(algo);
        this.key = key;
        this.hmacSpec = spec;
    }
    public HmacJwsSignatureProvider(String encodedKey, SignatureAlgorithm algo) {
        super(algo);
        hmacSpec = null;
        try {
            this.key = Base64UrlUtility.decode(encodedKey);
        } catch (Base64Exception ex) {
            LOG.warning("Hmac key can not be decoded");
            throw new JwsException(JwsException.Error.INVALID_KEY, ex);
        }
    }

    protected JwsSignature doCreateJwsSignature(JwsHeaders headers) {
        final String sigAlgo = headers.getSignatureAlgorithm().getJwaName();
        final Mac mac = HmacUtils.getInitializedMac(key,
                                                    AlgorithmUtils.toJavaName(sigAlgo),
                                                    hmacSpec);
        return new JwsSignature() {

            @Override
            public void update(byte[] src, int off, int len) {
                mac.update(src, off, len);
            }

            @Override
            public byte[] sign() {
                return mac.doFinal();
            }

        };
    }
    @Override
    protected boolean isValidAlgorithmFamily(String algo) {
        return AlgorithmUtils.isHmacSign(algo);
    }
}
