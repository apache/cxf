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
package org.apache.cxf.sts.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.ws.security.WSConstants;

/**
 * This class contains various configuration properties that can be used to encrypt an issued token.
 * The encryptionName property must be specified (corresponding to the alias to be used to select a
 * certificate from a KeyStore) - everything else is optional.
 */
public class EncryptionProperties {
    private String encryptionAlgorithm = WSConstants.AES_256;
    private String keyWrapAlgorithm = WSConstants.KEYTRANSPORT_RSA15;
    private int keyIdentifierType = WSConstants.ISSUER_SERIAL;
    private List<String> acceptedEncryptionAlgorithms = new ArrayList<String>();
    private List<String> acceptedKeyWrapAlgorithms = new ArrayList<String>();
    private String encryptionName;
    
    public EncryptionProperties() {
        // Default symmetric encryption algorithms
        acceptedEncryptionAlgorithms.add(WSConstants.TRIPLE_DES);
        acceptedEncryptionAlgorithms.add(WSConstants.AES_128);
        acceptedEncryptionAlgorithms.add(WSConstants.AES_192);
        acceptedEncryptionAlgorithms.add(WSConstants.AES_256);
        acceptedEncryptionAlgorithms.add(WSConstants.AES_128_GCM);
        acceptedEncryptionAlgorithms.add(WSConstants.AES_192_GCM);
        acceptedEncryptionAlgorithms.add(WSConstants.AES_256_GCM);
        
        // Default key wrap algorithms
        acceptedKeyWrapAlgorithms.add(WSConstants.KEYTRANSPORT_RSA15);
        acceptedKeyWrapAlgorithms.add(WSConstants.KEYTRANSPORT_RSAOEP);
    }
    
    /**
     * Get the encryption algorithm to use
     */
    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }
    
    /**
     * Set the encryption algorithm to use
     */
    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }
    
    /**
     * Get the encryption key-wrap algorithm to use
     */
    public String getKeyWrapAlgorithm() {
        return keyWrapAlgorithm;
    }
    
    /**
     * Set the encryption key-wrap algorithm to use
     */
    public void setKeyWrapAlgorithm(String keyWrapAlgorithm) {
        this.keyWrapAlgorithm = keyWrapAlgorithm;
    }
    
    /**
     * Get the (WSS4J) key identifier type used to reference a certificate for encryption
     */
    public int getKeyIdentifierType() {
        return keyIdentifierType;
    }
    
    /**
     * Set the (WSS4J) key identifier type used to reference a certificate for encryption
     */
    public void setKeyIdentifierType(int keyIdentifierType) {
        this.keyIdentifierType = keyIdentifierType;
    }
    
    /**
     * Get the alias used to select a certificate for encryption
     */
    public String getEncryptionName() {
        return encryptionName;
    }
    
    /**
     * Set the alias used to select a certificate for encryption
     */
    public void setEncryptionName(String encryptionName) {
        this.encryptionName = encryptionName;
    }
    
    /**
     * Set the list of accepted encryption algorithms. A request can contain a wst:EncryptionAlgorithm
     * uri to use to encrypt an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are 3-DES, AES-128, AES-128 GCM, AES-192, AES-192 GCM, AES-256 and AES-256 GCM.
     */
    public void setAcceptedEncryptionAlgorithms(List<String> acceptedEncryptionAlgorithms) {
        this.acceptedEncryptionAlgorithms = acceptedEncryptionAlgorithms;
    }
    
    /**
     * Get the list of accepted encryption algorithms. A request can contain a wst:EncryptionAlgorithm
     * uri to use to encrypt an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are 3-DES, AES-128, AES-128 GCM, AES-192, AES-192 GCM, AES-256 and AES-256 GCM.
     */
    public List<String> getAcceptedEncryptionAlgorithms() {
        return acceptedEncryptionAlgorithms;
    }

    /**
     * Set the list of accepted key-wrap algorithms. A request can contain a wst:KeyWrapAlgorithm
     * uri for use in encrypting an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are RSA 1.5 and RSA OEP.
     */
    public void setAcceptedKeyWrapAlgorithms(List<String> acceptedKeyWrapAlgorithms) {
        this.acceptedKeyWrapAlgorithms = acceptedKeyWrapAlgorithms;
    }
    
    /**
     * Get the list of accepted key-wrap algorithms. A request can contain a wst:KeyWrapAlgorithm
     * uri for use in encrypting an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are RSA 1.5 and RSA OEP.
     */
    public List<String> getAcceptedKeyWrapAlgorithms() {
        return acceptedKeyWrapAlgorithms;
    }
    
}