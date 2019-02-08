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
package org.apache.cxf.rs.security.xml;

import org.apache.xml.security.encryption.XMLCipher;

public class EncryptionProperties {
    private String encryptionKeyTransportAlgo = XMLCipher.RSA_OAEP;
    private String encryptionSymmetricKeyAlgo;
    private String encryptionDigestAlgo;
    private String encryptionKeyIdType;
    private String encryptionKeyName;

    public void setEncryptionKeyTransportAlgo(String encryptionKeyTransportAlgo) {
        this.encryptionKeyTransportAlgo = encryptionKeyTransportAlgo;
    }
    public String getEncryptionKeyTransportAlgo() {
        return encryptionKeyTransportAlgo;
    }
    public void setEncryptionSymmetricKeyAlgo(String encryptionSymmetricKeyAlgo) {
        this.encryptionSymmetricKeyAlgo = encryptionSymmetricKeyAlgo;
    }
    public String getEncryptionSymmetricKeyAlgo() {
        return encryptionSymmetricKeyAlgo;
    }
    public void setEncryptionDigestAlgo(String encryptionDigestAlgo) {
        this.encryptionDigestAlgo = encryptionDigestAlgo;
    }
    public String getEncryptionDigestAlgo() {
        return encryptionDigestAlgo;
    }
    public void setEncryptionKeyIdType(String encryptionKeyIdType) {
        this.encryptionKeyIdType = encryptionKeyIdType;
    }
    public String getEncryptionKeyIdType() {
        return encryptionKeyIdType;
    }
    public String getEncryptionKeyName() {
        return encryptionKeyName;
    }
    public void setEncryptionKeyName(String encryptionKeyName) {
        this.encryptionKeyName = encryptionKeyName;
    }

}
