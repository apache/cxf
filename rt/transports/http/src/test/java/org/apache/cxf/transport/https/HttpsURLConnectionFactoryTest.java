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
package org.apache.cxf.transport.https;

import java.lang.reflect.Field;

import javax.net.ssl.HttpsURLConnection;

import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class HttpsURLConnectionFactoryTest {

    @Test
    public void noExplicitKeystoreNoCertAlias() throws Exception {
        clearDefaults();
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");

        HttpsURLConnectionFactory factory = new HttpsURLConnectionFactory();
        Assert.assertNull(factory.socketFactory);

        TLSClientParameters tlsClientParams = new TLSClientParameters();
        tlsClientParams.setUseHttpsURLConnectionDefaultSslSocketFactory(false);

        HttpsURLConnection conn = Mockito.mock(HttpsURLConnection.class);

        try {
            factory.decorateWithTLS(tlsClientParams, conn);
        } catch (NullPointerException e) {
            Assert.fail("should not fail with NullPointerException");
        }
    }

    @Test
    public void noExplicitKeystoreWithCertAlias() throws Exception {
        clearDefaults();
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");

        HttpsURLConnectionFactory factory = new HttpsURLConnectionFactory();
        Assert.assertNull(factory.socketFactory);

        TLSClientParameters tlsClientParams = new TLSClientParameters();
        tlsClientParams.setUseHttpsURLConnectionDefaultSslSocketFactory(false);
        tlsClientParams.setCertAlias("someAlias");

        HttpsURLConnection conn = Mockito.mock(HttpsURLConnection.class);

        try {
            factory.decorateWithTLS(tlsClientParams, conn);
        } catch (NullPointerException e) {
            Assert.fail("should not fail with NullPointerException");
        }
    }

    @Test
    public void defaultKeystoreNoCertAlias() throws Exception {
        clearDefaults();
        String keystorePath = getClass().getResource("resources/defaultkeystore2").getPath();
        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        HttpsURLConnectionFactory factory = new HttpsURLConnectionFactory();
        Assert.assertNull(factory.socketFactory);

        TLSClientParameters tlsClientParams = new TLSClientParameters();
        tlsClientParams.setUseHttpsURLConnectionDefaultSslSocketFactory(false);

        HttpsURLConnection conn = Mockito.mock(HttpsURLConnection.class);

        try {
            factory.decorateWithTLS(tlsClientParams, conn);
        } catch (NullPointerException e) {
            Assert.fail("should not fail with NullPointerException");
        }
    }

    @Test
    public void defaultKeystoreWithCertAlias() throws Exception {
        clearDefaults();
        String keystorePath = getClass().getResource("resources/defaultkeystore2").getPath();
        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        HttpsURLConnectionFactory factory = new HttpsURLConnectionFactory();
        Assert.assertNull(factory.socketFactory);

        TLSClientParameters tlsClientParams = new TLSClientParameters();
        tlsClientParams.setUseHttpsURLConnectionDefaultSslSocketFactory(false);
        tlsClientParams.setCertAlias("someAlias");

        HttpsURLConnection conn = Mockito.mock(HttpsURLConnection.class);

        try {
            factory.decorateWithTLS(tlsClientParams, conn);
        } catch (NullPointerException e) {
            Assert.fail("should not fail with NullPointerException");
        }
    }

    private void clearDefaults() throws IllegalAccessException {
        Field defaultManagers = ReflectionUtil.getDeclaredField(SSLUtils.class, "defaultManagers");
        ReflectionUtil.setAccessible(defaultManagers);

        defaultManagers.set(SSLUtils.class, null);
    }

}
