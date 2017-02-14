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
package org.apache.cxf.rt.security.crypto;

import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

public class KeyProperties {
    private String keyAlgo;
    private int keySize;
    private int blockSize = -1;
    private byte[] additionalData;
    private SecureRandom secureRandom;
    private AlgorithmParameterSpec algoSpec;
    private boolean compressionSupported;

    public KeyProperties() {
    }

    public KeyProperties(String keyAlgo) {
        this(keyAlgo, -1);
    }
    public KeyProperties(String keyAlgo, int keySize) {
        this.keyAlgo = keyAlgo;
        this.keySize = keySize;
    }
    public String getKeyAlgo() {
        return keyAlgo;
    }
    public void setKeyAlgo(String keyAlgo) {
        this.keyAlgo = keyAlgo;
    }
    public int getKeySize() {
        return keySize;
    }
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }
    public SecureRandom getSecureRandom() {
        return secureRandom;
    }
    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }
    public AlgorithmParameterSpec getAlgoSpec() {
        return algoSpec;
    }
    public void setAlgoSpec(AlgorithmParameterSpec algoSpec) {
        this.algoSpec = algoSpec;
    }
    public int getBlockSize() {
        return blockSize;
    }
    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
    public boolean isCompressionSupported() {
        return compressionSupported;
    }
    public void setCompressionSupported(boolean compressionSupported) {
        this.compressionSupported = compressionSupported;
    }
    public byte[] getAdditionalData() {
        return additionalData;
    }
    public void setAdditionalData(byte[] additionalData) {
        this.additionalData = additionalData;
    }



}
