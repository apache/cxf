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

import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;


public abstract class AbstractContentEncryptionAlgorithm extends AbstractContentEncryptionCipherProperties
    implements ContentEncryptionProvider {
    private static final int DEFAULT_IV_SIZE = 128;
    private byte[] cek;
    private byte[] iv;
    private AtomicInteger providedIvUsageCount;
    private boolean generateCekOnce;

    protected AbstractContentEncryptionAlgorithm(ContentAlgorithm algo, boolean generateCekOnce) {
        super(algo);
        this.generateCekOnce = generateCekOnce;
    }
    protected AbstractContentEncryptionAlgorithm(byte[] cek, byte[] iv, ContentAlgorithm algo) {
        super(algo);
        this.cek = cek;
        this.iv = iv;
        if (iv != null && iv.length > 0) {
            providedIvUsageCount = new AtomicInteger();
        }
    }

    public byte[] getContentEncryptionKey(JweHeaders headers) {
        final byte[] theCek;
        synchronized (this) {
            if (cek == null) {
                String algoJava = getAlgorithm().getJavaName();
                SecretKey secretKey = CryptoUtils.getSecretKey(AlgorithmUtils.stripAlgoProperties(algoJava),
                              getContentEncryptionKeySize(headers));
                theCek = secretKey.getEncoded();
                if (generateCekOnce) {
                    cek = theCek;
                }
                // Clean the key after we're done with it
                try {
                    secretKey.destroy();
                } catch (DestroyFailedException e) {
                    // ignore
                }
            } else {
                theCek = cek;
            }
        }
        return theCek;
    }
    public byte[] getInitVector() {
        if (iv == null) {
            return CryptoUtils.generateSecureRandomBytes(getIvSize() / 8);
        } else if (iv.length > 0 && providedIvUsageCount.addAndGet(1) > 1) {
            LOG.warning("Custom IV is recommended to be used once");
            throw new JweException(JweException.Error.CUSTOM_IV_REUSED);
        } else {
            return iv;
        }
    }
    protected int getContentEncryptionKeySize(JweHeaders headers) {
        return getAlgorithm().getKeySizeBits();
    }
    protected int getIvSize() {
        return DEFAULT_IV_SIZE;
    }
}
