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
package org.apache.cxf.systest.crypto;

import java.io.InputStream;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

import org.junit.Assert;
import org.junit.Test;

public class CryptoUtilsTest {

    @Test
    public void testLoadCertificateNoAlias() throws Exception {
        try (InputStream is = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", this.getClass())) {
            CryptoUtils.loadCertificate(is, "password".toCharArray(), null, null);
            Assert.fail();
        } catch (SecurityException e) {
            // expected
        }
    }

    /*
    keytool -genkeypair \
      -alias somealias \
      -keyalg RSA -keysize 2048 \
      -validity 365 \
      -dname "CN=example.com, O=MyOrg, C=PL" \
      -storetype PKCS12 \
      -keystore somealias.p12 \
      -storepass changeit
    */
    @Test
    public void testLoadCertificateNoAliasSingle() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("somealias.p12")) {
            Assert.assertNotNull(CryptoUtils.loadCertificate(is, "changeit".toCharArray(), null, null));
        }
    }

}
