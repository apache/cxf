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
package org.apache.cxf.systest.microprofile.rest.client;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.apache.cxf.systest.microprofile.rest.client.mock.AsyncClientWithCompletionStage;
import org.apache.cxf.systest.microprofile.rest.client.mock.AsyncInvocationInterceptorFactoryTestImpl;
import org.apache.cxf.systest.microprofile.rest.client.mock.AsyncInvocationInterceptorFactoryTestImpl2;
import org.apache.cxf.systest.microprofile.rest.client.mock.ThreadLocalClientFilter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.tck.providers.TestClientRequestFilter;
import org.eclipse.microprofile.rest.client.tck.providers.TestClientResponseFilter;
import org.eclipse.microprofile.rest.client.tck.providers.TestMessageBodyReader;
import org.eclipse.microprofile.rest.client.tck.providers.TestMessageBodyWriter;
import org.eclipse.microprofile.rest.client.tck.providers.TestParamConverterProvider;
import org.eclipse.microprofile.rest.client.tck.providers.TestReaderInterceptor;
import org.eclipse.microprofile.rest.client.tck.providers.TestWriterInterceptor;

import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AsyncMethodTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    @Test
    public void verifyNoCDIOrMPConfigInClassPath() {
        try {
            Class.forName("org.eclipse.microprofile.config.ConfigProvider");
            fail("ConfigProvider API class is on the classpath - results from this test project are not valid");
        } catch (ClassNotFoundException expected) {
            // expected
        }
        try {
            Class.forName("jakarta.enterprise.inject.spi.BeanManager");
            fail("BeanManager API class is on the classpath - results from this test project are not valid");
        } catch (ClassNotFoundException expected) {
            //expected
        }
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProvidersAsyncCompletionStage() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/echo/test"))
                                .willReturn(aResponse()
                                .withBody("this is the replaced writer input body will be removed")));
        String inputBody = "input body will be removed";
        String expectedResponseBody = TestMessageBodyReader.REPLACED_BODY;

        AsyncClientWithCompletionStage api = RestClientBuilder.newBuilder()
                .register(TestClientRequestFilter.class)
                .register(TestClientResponseFilter.class)
                .register(TestMessageBodyReader.class, 3)
                .register(TestMessageBodyWriter.class)
                .register(TestParamConverterProvider.class)
                .register(TestReaderInterceptor.class)
                .register(TestWriterInterceptor.class)
                .baseUri(getBaseUri())
                .build(AsyncClientWithCompletionStage.class);

        CompletionStage<Response> cs = api.put(inputBody);

        // should need <1 second, but 20s timeout in case something goes wrong
        Response response = cs.toCompletableFuture().get(20, TimeUnit.SECONDS);
        String actualResponseBody = response.readEntity(String.class);

        assertEquals(expectedResponseBody, actualResponseBody);

        assertEquals(TestClientResponseFilter.getAndResetValue(), 1);
        assertEquals(TestClientRequestFilter.getAndResetValue(), 1);
        assertEquals(TestReaderInterceptor.getAndResetValue(), 1);
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProvidersAsyncCompletionStageWithExecutor() throws Exception {
        final String inputBody = "input body will be ignored";
        wireMockRule.stubFor(put(urlEqualTo("/echo/test"))
                                .willReturn(aResponse()
                                .withBody(inputBody)));
        AsyncInvocationInterceptorFactoryTestImpl.INBOUND.remove();
        AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.remove();
        try {
            final String asyncThreadName = "CXF-MPRestClientThread-2";

            AsyncClientWithCompletionStage api = RestClientBuilder.newBuilder()
                .register(AsyncInvocationInterceptorFactoryTestImpl.class)
                .register(AsyncInvocationInterceptorFactoryTestImpl2.class)
                .register(ThreadLocalClientFilter.class)
                .baseUri(getBaseUri())
                .executorService(Executors.newSingleThreadExecutor(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, asyncThreadName);
                    }
                }))
                .build(AsyncClientWithCompletionStage.class);

            CompletionStage<Response> cs = api.put(inputBody);
            List<String> outboundList = AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.get();
            assertEquals(4, outboundList.size());

            // ensure filters and asyncInvocationInterceptors are executed in the correct order and the correct thread
            // outbound:
            assertEquals(ThreadLocalClientFilter.class.getSimpleName(), outboundList.get(0));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl.class.getSimpleName(), outboundList.get(1));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName(), outboundList.get(2));
            assertEquals(Thread.currentThread().getName(), outboundList.get(3));

            // inbound:
            // should need <1 second, but 20s timeout in case something goes wrong
            Response response = cs.toCompletableFuture().get(20, TimeUnit.SECONDS);

            List<String> responseList = response.getStringHeaders().get("CXFTestResponse");
            assertEquals(4, responseList.size());

            assertEquals(asyncThreadName, responseList.get(0));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName(), responseList.get(1));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl.class.getSimpleName(), responseList.get(2));
            assertEquals(ThreadLocalClientFilter.class.getSimpleName(), responseList.get(3));
        } finally {
            AsyncInvocationInterceptorFactoryTestImpl.INBOUND.remove();
            AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.remove();
        }
    }

    @Test
    public void testInvokesGetOperationWithRegisteredProvidersAsyncCompletionStage() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/echo/test2"))
                                .willReturn(aResponse()
                                .withBody("{\"name\": \"test\"}")));

        AsyncClientWithCompletionStage api = RestClientBuilder.newBuilder()
                .register(TestClientRequestFilter.class)
                .register(TestClientResponseFilter.class)
                .register(TestMessageBodyReader.class, 3)
                .register(TestMessageBodyWriter.class)
                .register(TestParamConverterProvider.class)
                .register(TestReaderInterceptor.class)
                .register(TestWriterInterceptor.class)
                .register(JsrJsonpProvider.class)
                .baseUri(getBaseUri())
                .build(AsyncClientWithCompletionStage.class);

        CompletionStage<JsonStructure> cs = api.get();

        // should need <1 second, but 20s timeout in case something goes wrong
        JsonStructure response = cs.toCompletableFuture().get(20, TimeUnit.SECONDS);
        assertThat(response, instanceOf(JsonObject.class));
        
        final JsonObject jsonObject = (JsonObject)response;
        assertThat(jsonObject.get("name"), instanceOf(JsonString.class));
        assertThat(((JsonString)jsonObject.get("name")).getString(), equalTo("test"));

        assertEquals(TestClientResponseFilter.getAndResetValue(), 1);
        assertEquals(TestClientRequestFilter.getAndResetValue(), 1);
        assertEquals(TestReaderInterceptor.getAndResetValue(), 1);
    }

    @Test
    public void testInvokesGetAllOperationWithRegisteredProvidersAsyncCompletionStage() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/echo/test3"))
                                .willReturn(aResponse()
                                .withBody("[{\"name\": \"test\"}]")));

        AsyncClientWithCompletionStage api = RestClientBuilder.newBuilder()
                .register(TestClientRequestFilter.class)
                .register(TestClientResponseFilter.class)
                .register(TestMessageBodyReader.class, 3)
                .register(TestMessageBodyWriter.class)
                .register(TestParamConverterProvider.class)
                .register(TestReaderInterceptor.class)
                .register(TestWriterInterceptor.class)
                .register(JsrJsonpProvider.class)
                .baseUri(getBaseUri())
                .build(AsyncClientWithCompletionStage.class);

        CompletionStage<Collection<JsonObject>> cs = api.getAll();

        // should need <1 second, but 20s timeout in case something goes wrong
        Collection<JsonObject> response = cs.toCompletableFuture().get(20, TimeUnit.SECONDS);
        assertEquals(1, response.size());
        
        final JsonObject jsonObject = response.iterator().next();
        assertThat(jsonObject.get("name"), instanceOf(JsonString.class));
        assertThat(((JsonString)jsonObject.get("name")).getString(), equalTo("test"));

        assertEquals(TestClientResponseFilter.getAndResetValue(), 1);
        assertEquals(TestClientRequestFilter.getAndResetValue(), 1);
        assertEquals(TestReaderInterceptor.getAndResetValue(), 1);
    }

    private URI getBaseUri() {
        return URI.create("http://localhost:" + wireMockRule.port() + "/echo");
    }

}