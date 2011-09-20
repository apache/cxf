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
package org.apache.cxf.sts;

/**
 * This class contains various configuration properties that can be used to sign an issued token.
 */
public class SignatureProperties {
    private boolean useKeyValue;
    private long keySize = 256;
    private long minimumKeySize = 128;
    private long maximumKeySize = 512;
    
    /**
     * Get whether a KeyValue is used to refer to a a certificate used to sign an issued token. 
     * The default is false.
     */
    public boolean isUseKeyValue() {
        return useKeyValue;
    }

    /**
     * Set whether a KeyValue is used to refer to a a certificate used to sign an issued token. 
     * The default is false.
     */
    public void setUseKeyValue(boolean useKeyValue) {
        this.useKeyValue = useKeyValue;
    }

    /**
     * Get the key size to use when generating a symmetric key to sign an issued token. The default is
     * 256 bits.
     */
    public long getKeySize() {
        return keySize;
    }

    /**
     * Set the key size to use when generating a symmetric key to sign an issued token. The default is
     * 256 bits.
     */
    public void setKeySize(long keySize) {
        this.keySize = keySize;
    }
    
    /**
     * Get the minimum key size to use when generating a symmetric key to sign an issued token. The
     * requestor can specify a KeySize value to use. The default is 128 bits.
     */
    public long getMinimumKeySize() {
        return minimumKeySize;
    }

    /**
     * Set the minimum key size to use when generating a symmetric key to sign an issued token. The
     * requestor can specify a KeySize value to use. The default is 128 bits.
     */
    public void setMinimumKeySize(long minimumKeySize) {
        this.minimumKeySize = minimumKeySize;
    }

    /**
     * Get the maximum key size to use when generating a symmetric key to sign an issued token. The
     * requestor can specify a KeySize value to use. The default is 512 bits.
     */
    public long getMaximumKeySize() {
        return maximumKeySize;
    }

    /**
     * Set the maximum key size to use when generating a symmetric key to sign an issued token. The
     * requestor can specify a KeySize value to use. The default is 512 bits.
     */
    public void setMaximumKeySize(long maximumKeySize) {
        this.maximumKeySize = maximumKeySize;
    }
    
}