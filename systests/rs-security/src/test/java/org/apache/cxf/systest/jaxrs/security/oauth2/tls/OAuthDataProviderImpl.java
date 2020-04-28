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
package org.apache.cxf.systest.jaxrs.security.oauth2.tls;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Collections;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.grants.code.JCacheCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.xml.security.utils.ClassLoaderUtils;

/**
 * Extend the JCacheCodeDataProvider to allow refreshing of tokens
 */
public class OAuthDataProviderImpl extends JCacheCodeDataProvider {
    public OAuthDataProviderImpl() throws Exception {

        Client client1 = new Client("CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US",
                                    null,
                                    true,
                                    null,
                                    null);
        client1.getAllowedGrantTypes().add("custom_grant");
        registerCert(client1);
        this.setClient(client1);

        Client client2 = new Client("bound",
                                   null,
                                   true,
                                   null,
                                   null);
        client2.getProperties().put(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN,
                                    "CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US");
        client2.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client2);

        Client client3 = new Client("unbound",
                                    null,
                                    true,
                                    null,
                                    null);
        this.setClient(client3);

    }

    private void registerCert(Client client) throws Exception {
        Certificate cert = loadCert();
        String encodedCert = Base64Utility.encode(cert.getEncoded());
        client.setApplicationCertificates(Collections.singletonList(encodedCert));

    }

    private Certificate loadCert() throws Exception {
        try (InputStream is = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", this.getClass())) {
            return CryptoUtils.loadCertificate(is, "password".toCharArray(), "morpit", null);
        }
    }
}
