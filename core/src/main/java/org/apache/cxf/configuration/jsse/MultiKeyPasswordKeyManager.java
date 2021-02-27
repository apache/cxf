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
package org.apache.cxf.configuration.jsse;

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

public class MultiKeyPasswordKeyManager implements X509KeyManager {
    private final KeyStore mKeyStore;
    private final String mKeyAlias;
    private final String mKeyPassword;

    public MultiKeyPasswordKeyManager(KeyStore keystore, String keyAlias, String keyPassword) {
        mKeyStore = keystore;
        mKeyAlias = keyAlias;
        mKeyPassword = keyPassword;
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return new String[] {
            mKeyAlias
        };
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return new String[] {
            mKeyAlias
        };
    }
    public X509Certificate[] getCertificateChain(String alias) {
        final Certificate[] chain;
        try {
            chain = mKeyStore.getCertificateChain(alias);
        } catch (KeyStoreException kse) {
            throw new RuntimeException(kse);
        }
        final X509Certificate[] certChain = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            certChain[i] = (X509Certificate)chain[i];
        }
        return certChain;
    }

    public PrivateKey getPrivateKey(String alias) {
        try {
            return (PrivateKey)mKeyStore.getKey(alias, mKeyPassword.toCharArray());
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException(gse);
        }
    }

    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return mKeyAlias;
    }

    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return mKeyAlias;
    }

}
