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
package org.apache.cxf.sts.request;

/**
 * This class contains values that have been extracted from a RequestSecurityToken corresponding to
 * various key and encryption requirements.
 */
public class KeyRequirements {

    private String authenticationType;
    private String keyType;
    private long keySize;
    private String signatureAlgorithm;
    private String encryptionAlgorithm;
    private String c14nAlgorithm;
    private String computedKeyAlgorithm;
    private String keywrapAlgorithm;
    private ReceivedCredential receivedCredential;
    private Entropy entropy;
    private String encryptWith;
    private String signWith;

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public long getKeySize() {
        return keySize;
    }

    public void setKeySize(long keySize) {
        this.keySize = keySize;
    }

    /**
     * This input parameter is ignored for the moment.
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * This input parameter is ignored for the moment.
     */
    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    /**
     * This input parameter is ignored for the moment.
     */
    public String getC14nAlgorithm() {
        return c14nAlgorithm;
    }

    /**
     * This input parameter is ignored for the moment.
     */
    public void setC14nAlgorithm(String c14nAlgorithm) {
        this.c14nAlgorithm = c14nAlgorithm;
    }

    public String getComputedKeyAlgorithm() {
        return computedKeyAlgorithm;
    }

    public void setComputedKeyAlgorithm(String computedKeyAlgorithm) {
        this.computedKeyAlgorithm = computedKeyAlgorithm;
    }

    public String getKeywrapAlgorithm() {
        return keywrapAlgorithm;
    }

    public void setKeywrapAlgorithm(String keywrapAlgorithm) {
        this.keywrapAlgorithm = keywrapAlgorithm;
    }

    public ReceivedCredential getReceivedCredential() {
        return receivedCredential;
    }

    public void setReceivedCredential(ReceivedCredential receivedCredential) {
        this.receivedCredential = receivedCredential;
    }

    public Entropy getEntropy() {
        return entropy;
    }

    public void setEntropy(Entropy entropy) {
        this.entropy = entropy;
    }

    public String getEncryptWith() {
        return encryptWith;
    }

    public void setEncryptWith(String encryptWith) {
        this.encryptWith = encryptWith;
    }

    public String getSignWith() {
        return signWith;
    }

    public void setSignWith(String signWith) {
        this.signWith = signWith;
    }


}