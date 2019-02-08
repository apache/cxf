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

package org.apache.cxf.io;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.DestroyFailedException;

/**
 * A class to hold a pair of encryption and decryption ciphers.
 */
public class CipherPair {
    private String transformation;
    private Cipher enccipher;
    private SecretKey key;
    private byte[] ivp;

    public CipherPair(String transformation) throws GeneralSecurityException {
        this.transformation = transformation;

        int d = transformation.indexOf('/');
        String a;
        if (d > 0) {
            a = transformation.substring(0, d);
        } else {
            a = transformation;
        }
        try {
            KeyGenerator keygen = KeyGenerator.getInstance(a);
            keygen.init(new SecureRandom());
            key = keygen.generateKey();
            enccipher = Cipher.getInstance(transformation);
            enccipher.init(Cipher.ENCRYPT_MODE, key);
            ivp = enccipher.getIV();
        } catch (GeneralSecurityException e) {
            enccipher = null;
            throw e;
        }
    }

    public String getTransformation() {
        return transformation;
    }

    public Cipher getEncryptor() {
        return enccipher;
    }

    public Cipher getDecryptor() {
        Cipher deccipher = null;
        try {
            deccipher = Cipher.getInstance(transformation);
            deccipher.init(Cipher.DECRYPT_MODE, key, ivp == null ? null : new IvParameterSpec(ivp));
        } catch (GeneralSecurityException e) {
            // ignore
        }
        return deccipher;
    }

    public void clean() {
        if (ivp != null) {
            Arrays.fill(ivp, (byte) 0);
        }
        // Clean the key after we're done with it
        try {
            key.destroy();
        } catch (DestroyFailedException e) {
            // ignore
        }
    }
}
