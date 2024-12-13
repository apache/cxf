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

package org.apache.cxf.systest.jaxrs.bootstrap;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.transport.https.SSLUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Some tests are taken from {@linkplain https://github.com/jakartaee/rest/blob/tck-3.1.2/jaxrs-tck
 * /src/main/java/ee/jakarta/tck/ws/rs/sebootstrap/SeBootstrapIT.java}
 */
public class SeBootstrapTest {
    private static Bus bus;
    private Client client;

    @BeforeClass
    public static void createBus() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        bus = BusFactory.newInstance().createBus();
        BusFactory.setDefaultBus(bus);
    }

    
    @AfterClass
    public static void destroyBus() throws Exception {
        bus.shutdown(true);
        bus = null;
    }

    @Before
    public void setUp() throws Exception {
        client = ClientBuilder
            .newBuilder()
            .sslContext(createSSLContext())
            .hostnameVerifier(new NoopHostnameVerifier())
            .build();
    }

    @After
    public void tearDown() {
        client.close();
    }

    /**
     * Verifies that an instance will boot using default configuration.
     */
    @Test
    public final void shouldBootInstanceUsingDefaults() {
        final Application application = new StaticApplication();
        final SeBootstrap.Configuration.Builder bootstrapConfigurationBuilder = SeBootstrap.Configuration.builder();
        final SeBootstrap.Configuration requestedConfiguration = bootstrapConfigurationBuilder.build();

        final CompletionStage<SeBootstrap.Instance> completionStage = SeBootstrap.start(application,
                requestedConfiguration);
        final SeBootstrap.Instance instance = completionStage.toCompletableFuture().join();
        final SeBootstrap.Configuration actualConfiguration = instance.configuration();
        final String actualResponse = client.target(UriBuilder.newInstance().scheme(actualConfiguration.protocol())
                .host(actualConfiguration.host()).port(actualConfiguration.port()).path(actualConfiguration.rootPath())
                .path("application/resource")).request().get(String.class);

        assertThat(actualResponse, is("OK"));
        assertThat(actualConfiguration.protocol(), is("HTTP"));
        assertThat(actualConfiguration.host(), is("localhost"));
        assertThat(actualConfiguration.port(), is(greaterThan(0)));
        assertThat(actualConfiguration.rootPath(), is("/"));

        instance.stop().toCompletableFuture().join();
    }

    /**
     * Verifies that an instance will boot using a self-detected free IP port.
     */
    @Test
    public final void shouldBootInstanceUsingSelfDetectedFreeIpPort() {
        final Application application = new StaticApplication();
        final SeBootstrap.Configuration.Builder bootstrapConfigurationBuilder = SeBootstrap.Configuration.builder();
        final SeBootstrap.Configuration requestedConfiguration = bootstrapConfigurationBuilder.protocol("HTTP")
                .host("localhost").port(SeBootstrap.Configuration.FREE_PORT).rootPath("/root/path").build();

        final CompletionStage<SeBootstrap.Instance> completionStage = SeBootstrap.start(application,
                requestedConfiguration);
        final SeBootstrap.Instance instance = completionStage.toCompletableFuture().join();
        final SeBootstrap.Configuration actualConfiguration = instance.configuration();
        final String actualResponse = client.target(UriBuilder.newInstance().scheme(actualConfiguration.protocol())
                .host(actualConfiguration.host()).port(actualConfiguration.port()).path(actualConfiguration.rootPath())
                .path("application/resource")).request().get(String.class);

        assertThat(actualResponse, is("OK"));
        assertThat(actualConfiguration.protocol(), is(requestedConfiguration.protocol()));
        assertThat(actualConfiguration.host(), is(requestedConfiguration.host()));
        assertThat(actualConfiguration.port(), is(greaterThan(0)));
        assertThat(actualConfiguration.rootPath(), is(requestedConfiguration.rootPath()));
        instance.stop().toCompletableFuture().join();
    }

    @Test
    public final void shouldBootInstanceUsingImplementationsDefaultIpPort() {
        final Application application = new StaticApplication();
        final SeBootstrap.Configuration.Builder bootstrapConfigurationBuilder = SeBootstrap.Configuration.builder();
        final SeBootstrap.Configuration requestedConfiguration = bootstrapConfigurationBuilder.protocol("HTTP")
                .host("localhost").port(SeBootstrap.Configuration.DEFAULT_PORT).rootPath("/root/path").build();

        final CompletionStage<SeBootstrap.Instance> completionStage = SeBootstrap.start(application,
                requestedConfiguration);
        final SeBootstrap.Instance instance = completionStage.toCompletableFuture().join();
        final SeBootstrap.Configuration actualConfiguration = instance.configuration();
        final String actualResponse = client.target(UriBuilder.newInstance().scheme(actualConfiguration.protocol())
                .host(actualConfiguration.host()).port(actualConfiguration.port()).path(actualConfiguration.rootPath())
                .path("application/resource")).request().get(String.class);

        assertThat(actualResponse, is("OK"));
        assertThat(actualConfiguration.protocol(), is(requestedConfiguration.protocol()));
        assertThat(actualConfiguration.host(), is(requestedConfiguration.host()));
        assertThat(actualConfiguration.port(), is(greaterThan(0)));
        assertThat(actualConfiguration.rootPath(), is(requestedConfiguration.rootPath()));
        instance.stop().toCompletableFuture().join();
    }
    
    /**
     * Verifies that an instance will boot using default configuration.
     */
    @Test
    public final void shouldBootInstanceUsingHttps() throws Exception {
        final Application application = new StaticApplication();
        final SeBootstrap.Configuration.Builder bootstrapConfigurationBuilder = SeBootstrap.Configuration.builder()
            .protocol("HTTPS")
            .sslContext(createSSLContext());

        final SeBootstrap.Configuration requestedConfiguration = bootstrapConfigurationBuilder.build();
        final CompletionStage<SeBootstrap.Instance> completionStage = SeBootstrap.start(application,
                requestedConfiguration);
        final SeBootstrap.Instance instance = completionStage.toCompletableFuture().join();
        final SeBootstrap.Configuration actualConfiguration = instance.configuration();

        final String actualResponse = client.target(UriBuilder.newInstance().scheme(actualConfiguration.protocol())
                .host(actualConfiguration.host()).port(actualConfiguration.port()).path(actualConfiguration.rootPath())
                .path("application/resource")).request().get(String.class);

        assertThat(actualResponse, is("OK"));
        assertThat(actualConfiguration.protocol(), is("HTTPS"));
        assertThat(actualConfiguration.host(), is("localhost"));
        assertThat(actualConfiguration.port(), is(8443));
        assertThat(actualConfiguration.rootPath(), is("/"));

        instance.stop().toCompletableFuture().join();
    }

    private SSLContext createSSLContext() throws Exception {
        final TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setHostnameVerifier(new NoopHostnameVerifier());

        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", this.getClass())) {
            KeyStore trustStore = loadStore(keystore, "password");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            tlsParams.setTrustManagers(tmf.getTrustManagers());
        }

        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Morpit.jks", this.getClass())) {
            KeyStore keyStore = loadStore(keystore, "password");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());
            tlsParams.setKeyManagers(kmf.getKeyManagers());
        }

        return SSLUtils.getSSLContext(tlsParams);
    }

    private KeyStore loadStore(InputStream inputStream, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(inputStream, password.toCharArray());
        return store;
    }

    @ApplicationPath("application")
    public static final class StaticApplication extends Application {
        private final StaticResource staticResource;

        private StaticApplication() {
            this.staticResource = new StaticResource();
        }

        @Override
        public Set<Object> getSingletons() {
            return Collections.<Object>singleton(staticResource);
        }

        @Path("resource")
        public static final class StaticResource {
            private StaticResource() {
            }

            @GET
            public String staticResponse() {
                return "OK";
            }
        }
    };
}
