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
package org.apache.cxf.systest.jaxrs.security.oauth2.common;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Collections;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.grants.code.DefaultEHCacheCodeDataProvider;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.xml.security.utils.ClassLoaderUtils;

/**
 * Extend the DefaultEHCacheCodeDataProvider to allow refreshing of tokens
 */
public class OAuthDataProviderTlsImpl extends DefaultEHCacheCodeDataProvider {
    public OAuthDataProviderTlsImpl() throws Exception {

        Certificate cert = loadCert();
        String encodedCert = Base64Utility.encode(cert.getEncoded());

        Client client = new Client("CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US",
                                    null,
                                    true,
                                    null,
                                    null);
        client.getAllowedGrantTypes().add("custom_grant");
        client.setApplicationCertificates(Collections.singletonList(encodedCert));
        this.setClient(client);

    }

    private Certificate loadCert() throws Exception {
        try (InputStream is = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", this.getClass())) {
            return CryptoUtils.loadCertificate(is, new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'}, "morpit", null);
        }
    }
}