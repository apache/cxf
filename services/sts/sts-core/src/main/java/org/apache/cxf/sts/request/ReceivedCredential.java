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

import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * This class represents a received credential. It can contain either an X509Certificate or PublicKey.
 */
public class ReceivedCredential {

    private X509Certificate x509Cert;
    private PublicKey publicKey;

    public X509Certificate getX509Cert() {
        return x509Cert;
    }
    public void setX509Cert(X509Certificate x509Cert) {
        this.x509Cert = x509Cert;
    }
    public PublicKey getPublicKey() {
        return publicKey;
    }
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

}