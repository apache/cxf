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
package org.apache.cxf.microprofile.client;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClientUtil;
import org.apache.cxf.microprofile.client.mock.EchoClientReqFilter;
import org.apache.cxf.microprofile.client.mock.ExceptionMappingClient;
import org.apache.cxf.microprofile.client.mock.HighPriorityClientReqFilter;
import org.apache.cxf.microprofile.client.mock.HighPriorityMBW;
import org.apache.cxf.microprofile.client.mock.InvokedMethodClientRequestFilter;
import org.apache.cxf.microprofile.client.mock.LowPriorityClientReqFilter;
import org.apache.cxf.microprofile.client.mock.MyClient;
import org.apache.cxf.microprofile.client.mock.NoSuchEntityException;
import org.apache.cxf.microprofile.client.mock.NotFoundClientReqFilter;
import org.apache.cxf.microprofile.client.mock.NotFoundExceptionMapper;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.tck.interfaces.InterfaceWithoutProvidersDefined;
import org.eclipse.microprofile.rest.client.tck.providers.TestClientRequestFilter;
import org.eclipse.microprofile.rest.client.tck.providers.TestClientResponseFilter;
import org.eclipse.microprofile.rest.client.tck.providers.TestMessageBodyReader;
import org.eclipse.microprofile.rest.client.tck.providers.TestMessageBodyWriter;
import org.eclipse.microprofile.rest.client.tck.providers.TestParamConverterProvider;
import org.eclipse.microprofile.rest.client.tck.providers.TestReaderInterceptor;
import org.eclipse.microprofile.rest.client.tck.providers.TestWriterInterceptor;
import org.eclipse.microprofile.rest.client.tck.providers.Widget;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CxfTypeSafeClientBuilderTest {

    @Test
    public void testConfigMethods() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();

        assertEquals("y", builder.property("x", "y").getConfiguration().getProperty("x"));

        assertTrue(builder.register(HighPriorityMBW.class).getConfiguration().isRegistered(HighPriorityMBW.class));

        HighPriorityMBW mbw = new HighPriorityMBW(1);
        assertTrue(builder.register(mbw).getConfiguration().isRegistered(mbw));

    }

    @Test
    public void testConfigPriorityOverrides() throws Exception {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        builder.property("microprofile.rest.client.disable.default.mapper", true);
        builder.register(HighPriorityClientReqFilter.class); // annotation priority of 10
        builder.register(LowPriorityClientReqFilter.class, 5);
        // overriding priority to be 5 (preferred)
        assertTrue(builder.getConfiguration().isRegistered(LowPriorityClientReqFilter.class));
        MyClient c = builder.baseUrl(new URL("http://localhost/null")).build(MyClient.class);
        Response r = c.get();
        assertEquals("low", r.readEntity(String.class));
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProviders() throws Exception {
        String inputBody = "input body will be removed";
        String expectedResponseBody = TestMessageBodyReader.REPLACED_BODY;

        InterfaceWithoutProvidersDefined api = new CxfTypeSafeClientBuilder()
                .register(TestClientRequestFilter.class)
                .register(TestClientResponseFilter.class)
                .register(TestMessageBodyReader.class, 4999)
                .register(TestMessageBodyWriter.class)
                .register(TestParamConverterProvider.class)
                .register(TestReaderInterceptor.class)
                .register(TestWriterInterceptor.class)
                .register(EchoClientReqFilter.class)
                .property("microprofile.rest.client.disable.default.mapper", true)
                .baseUrl(new URL("http://localhost/null"))
                .build(InterfaceWithoutProvidersDefined.class);

        Response response = api.executePost(inputBody);

        String body = response.readEntity(String.class);

        response.close();

        assertEquals(expectedResponseBody, body);

        assertEquals(TestClientResponseFilter.getAndResetValue(), 1);
        assertEquals(TestClientRequestFilter.getAndResetValue(), 1);
        assertEquals(TestReaderInterceptor.getAndResetValue(), 1);
        // If we use the EchoClientReqFilter, it will be executed before the TestWriterInterceptor,
        // so that interceptor won't be called in this test.
        // TODO: add a test for writer interceptors - possibly in systests
        //assertEquals(TestWriterInterceptor.getAndResetValue(), 1);
    }
    
    @Test
    public void testInvokesPostOperationWithRegisteredFeature() throws Exception {
        String inputBody = "input body will be removed";
        String expectedResponseBody = TestMessageBodyReader.REPLACED_BODY;

        InterfaceWithoutProvidersDefined api = new CxfTypeSafeClientBuilder()
                .register(SomeFeature.class)
                .property("microprofile.rest.client.disable.default.mapper", true)
                .baseUrl(new URL("http://localhost/null"))
                .build(InterfaceWithoutProvidersDefined.class);

        Response response = api.executePost(inputBody);

        String body = response.readEntity(String.class);

        response.close();

        assertEquals(expectedResponseBody, body);

        assertEquals(TestClientResponseFilter.getAndResetValue(), 1);
        assertEquals(TestClientRequestFilter.getAndResetValue(), 1);
        assertEquals(TestReaderInterceptor.getAndResetValue(), 1);
    }

    @Test(expected = NoSuchEntityException.class)
    public void testResponseExceptionMapper() throws Exception {
        ExceptionMappingClient client = new CxfTypeSafeClientBuilder()
            .register(NotFoundExceptionMapper.class)
            .register(NotFoundClientReqFilter.class)
            .baseUrl(new URL("http://localhost/null"))
            .build(ExceptionMappingClient.class);

        Response r = client.getEntity();
        fail(r, "Did not throw expected mapped exception: NoSuchEntityException");
    }

    @Test(expected = WebApplicationException.class)
    public void testDefaultResponseExceptionMapper() throws Exception {
        ExceptionMappingClient client = new CxfTypeSafeClientBuilder()
            .register(NotFoundClientReqFilter.class)
            .baseUrl(new URL("http://localhost/null"))
            .build(ExceptionMappingClient.class);

        Response r = client.getEntity();
        fail(r, "Did not throw expected mapped exception: WebApplicationException");
    }

    @Test
    public void testClientRequestFilterCanAccessInvokedMethod() throws Exception {
        InterfaceWithoutProvidersDefined client = RestClientBuilder.newBuilder()
            .register(InvokedMethodClientRequestFilter.class)
            .baseUri(new URI("http://localhost:8080/neverUsed"))
            .build(InterfaceWithoutProvidersDefined.class);

        Response response = client.executePut(new Widget("foo", 7), "bar");
        assertEquals(200, response.getStatus());
        assertEquals(Response.class.getName(), response.getHeaderString("ReturnType"));
        assertEquals("PUT", response.getHeaderString("PUT"));
        assertEquals("/{id}", response.getHeaderString("Path"));
        assertEquals(Widget.class.getName(), response.getHeaderString("Parm1"));
        assertEquals(PathParam.class.getName(), response.getHeaderString("Parm1Annotation"));
        assertEquals(String.class.getName(), response.getHeaderString("Parm2"));
    }

    @Test
    public void testClientPropertiesAreSet() throws Exception {
        InterfaceWithoutProvidersDefined client = RestClientBuilder.newBuilder()
            .register(InvokedMethodClientRequestFilter.class)
            .property("hello", "world")
            .baseUri(new URI("http://localhost:8080/neverUsed"))
            .build(InterfaceWithoutProvidersDefined.class);
        assertEquals("world",
            WebClientUtil.getClientConfigFromProxy(client).getRequestContext().get("hello"));
    }

    @Test
    public void testCanInvokeDefaultInterfaceMethods() throws Exception {
        MyClient client = RestClientBuilder.newBuilder()
            .register(InvokedMethodClientRequestFilter.class)
            .baseUri(new URI("http://localhost:8080/neverUsed"))
            .build(MyClient.class);
        assertEquals("defaultValue", client.myDefaultMethod(false));
    }

    @Test(expected = IOException.class)
    public void testCanInvokeDefaultInterfaceMethodsWithException() throws Exception {
        MyClient client = RestClientBuilder.newBuilder()
            .register(InvokedMethodClientRequestFilter.class)
            .baseUri(new URI("http://localhost:8080/neverUsed"))
            .build(MyClient.class);
        client.myDefaultMethod(true);
        Assert.fail("Should have thrown IOException");
    }
    private void fail(Response r, String failureMessage) {
        System.out.println(r.getStatus());
        Assert.fail(failureMessage);
    }

    @Test
    public void testFollowRedirectSetsProperty() {
        CxfTypeSafeClientBuilder builder = (CxfTypeSafeClientBuilder) RestClientBuilder.newBuilder()
                                                                                       .followRedirects(true);
        assertEquals("true", builder.getConfiguration().getProperty("http.autoredirect"));

        builder = (CxfTypeSafeClientBuilder) RestClientBuilder.newBuilder().followRedirects(false);
        assertEquals("false", builder.getConfiguration().getProperty("http.autoredirect"));
    }

    @Test
    public void testProxyAddressSetsProperty() {
        CxfTypeSafeClientBuilder builder = (CxfTypeSafeClientBuilder)
            RestClientBuilder.newBuilder().proxyAddress("cxf.apache.org", 8080);
        assertEquals("cxf.apache.org", builder.getConfiguration().getProperty("http.proxy.server.uri"));
        assertEquals(8080, builder.getConfiguration().getProperty("http.proxy.server.port"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProxyAddressInvalidPort1() {
        RestClientBuilder.newBuilder().proxyAddress("cxf.apache.org", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProxyAddressInvalidPort2() {
        RestClientBuilder.newBuilder().proxyAddress("a.com", Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProxyAddressNullHost() {
        RestClientBuilder.newBuilder().proxyAddress(null, 8080);
    }
    
    public static class SomeFeature implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            context
                .register(TestClientRequestFilter.class)
                .register(TestClientResponseFilter.class)
                .register(TestMessageBodyReader.class, 4999)
                .register(TestMessageBodyWriter.class)
                .register(TestParamConverterProvider.class)
                .register(TestReaderInterceptor.class)
                .register(TestWriterInterceptor.class)
                .register(EchoClientReqFilter.class);
            return true;
        }
    }
}
