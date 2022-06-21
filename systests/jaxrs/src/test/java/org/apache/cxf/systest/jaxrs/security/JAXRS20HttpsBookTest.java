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

package org.apache.cxf.systest.jaxrs.security;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.https.SSLUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRS20HttpsBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookHttpsServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus("org/apache/cxf/systest/jaxrs/security/jaxrs-https-server.xml");
        assertTrue("server did not launch correctly",
                   launchServer(BookHttpsServer.class, true));
    }

    @Test
    public void testGetBook() throws Exception {

        ClientBuilder builder = ClientBuilder.newBuilder();

        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", this.getClass())) {
            KeyStore trustStore = loadStore(keystore, "password");
            builder.trustStore(trustStore);
        }
        builder.hostnameVerifier(new AllowAllHostnameVerifier());

        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Morpit.jks", this.getClass())) {
            KeyStore keyStore = loadStore(keystore, "password");
            builder.keyStore(keyStore, "password");
        }

        Client client = builder.build();
        client.register(new LoggingFeature());

        WebTarget target = client.target("https://localhost:" + PORT + "/bookstore/securebooks/123");
        Book b = target.request().accept(MediaType.APPLICATION_XML_TYPE).get(Book.class);
        assertEquals(123, b.getId());
    }

    @Test
    public void testGetBookSslContext() throws Exception {

        ClientBuilder builder = ClientBuilder.newBuilder();

        SSLContext sslContext = createSSLContext();
        builder.sslContext(sslContext);

        builder.hostnameVerifier(new AllowAllHostnameVerifier());


        Client client = builder.build();

        WebTarget target = client.target("https://localhost:" + PORT + "/bookstore/securebooks/123");
        Book b = target.request().accept(MediaType.APPLICATION_XML_TYPE).get(Book.class);
        assertEquals(123, b.getId());
    }

    private KeyStore loadStore(InputStream inputStream, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(inputStream, password.toCharArray());
        return store;
    }

    private SSLContext createSSLContext() throws Exception {
        TLSClientParameters tlsParams = new TLSClientParameters();

        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", this.getClass())) {
            KeyStore trustStore = loadStore(keystore, "password");

            TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            tlsParams.setTrustManagers(tmf.getTrustManagers());
        }

        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Morpit.jks", this.getClass())) {
            KeyStore keyStore = loadStore(keystore, "password");

            KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());
            tlsParams.setKeyManagers(kmf.getKeyManagers());
        }

        return SSLUtils.getSSLContext(tlsParams);
    }
}
