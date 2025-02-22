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

package org.apache.cxf.jaxrs.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.Bus.BusState;
import org.apache.cxf.jaxrs.resources.BookInterface;
import org.apache.cxf.jaxrs.resources.BookStore;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WebClientTest {

    @Test
    public void testReplaceHeader() {
        WebClient wc = WebClient.create("http://foo").header("a", "b");
        assertEquals(1, wc.getHeaders().size());
        assertEquals("b", wc.getHeaders().getFirst("a"));
        wc.replaceHeader("a", "c");
        assertEquals(1, wc.getHeaders().size());
        assertEquals("c", wc.getHeaders().getFirst("a"));
    }

    @Test
    public void testRemoveHeader() {
        WebClient wc = WebClient.create("http://foo").header("a", "b");
        assertEquals(1, wc.getHeaders().size());
        assertEquals("b", wc.getHeaders().getFirst("a"));
        wc.replaceHeader("a", null);
        assertEquals(0, wc.getHeaders().size());
    }

    @Test
    public void testEncoding() {
        URI u = WebClient.create("http://foo").path("bar+ %2B").matrix("a", "value+ ")
            .query("b", "bv+ %2B").getCurrentURI();
        assertEquals("http://foo/bar+%20%2B;a=value+%20?b=bv%2B+%2B", u.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPath() {
        WebClient.create("http://foo").path(null);
        fail("Exception expected");
    }

    @Test
    public void testExistingAsteriscs() {
        URI u = WebClient.create("http://foo/*").getCurrentURI();
        assertEquals("http://foo/*", u.toString());
    }

    @Test
    public void testAsteriscs() {
        URI u = WebClient.create("http://foo").path("*").getCurrentURI();
        assertEquals("http://foo/*", u.toString());
    }

    @Test
    public void testAddressNotEncoded() {
        URI u = WebClient.create("http://localhost/somepath ").getCurrentURI();
        assertEquals("http://localhost/somepath%20", u.toString());
    }

    @Test
    public void testDoubleAsteriscs() {
        URI u = WebClient.create("http://foo").path("**").getCurrentURI();
        assertEquals("http://foo/**", u.toString());
    }

    @Test
    public void testBaseCurrentPath() {
        assertEquals(URI.create("http://foo"), WebClient.create("http://foo").getBaseURI());
        assertEquals(URI.create("http://foo"), WebClient.create("http://foo").getCurrentURI());
    }

    @Test
    public void testNewBaseCurrentPath() {
        WebClient wc = WebClient.create("http://foo");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
        wc.to("http://bar", false);
        assertEquals(URI.create("http://bar"), wc.getBaseURI());
        assertEquals(URI.create("http://bar"), wc.getCurrentURI());
    }

    @Test
    public void testBaseCurrentWebSocketPath() {
        WebClient wc = WebClient.create("ws://foo");
        assertEquals(URI.create("ws://foo"), wc.getBaseURI());
        assertEquals(URI.create("ws://foo"), wc.getCurrentURI());
        wc.path("a");
        assertEquals(URI.create("ws://foo"), wc.getBaseURI());
        assertEquals(URI.create("ws://foo/a"), wc.getCurrentURI());
    }

    @Test
    public void testNewBaseCurrentWebSocketPath() {
        WebClient wc = WebClient.create("ws://foo");
        assertEquals(URI.create("ws://foo"), wc.getBaseURI());
        assertEquals(URI.create("ws://foo"), wc.getCurrentURI());
        wc.to("ws://bar", false);
        assertEquals(URI.create("ws://bar"), wc.getBaseURI());
        assertEquals(URI.create("ws://bar"), wc.getCurrentURI());
    }

    @Test
    public void testEmptyQuery() {
        WebClient wc = WebClient.create("http://foo");
        wc.query("_wadl");
        assertEquals("http://foo?_wadl", wc.getCurrentURI().toString());
    }

    @Test
    public void testEmptyQueryKey() {
        WebClient wc = WebClient.create("http://foo");
        wc.query("");
        assertEquals("http://foo", wc.getCurrentURI().toString());
    }

    @Test
    public void testForward() {
        WebClient wc = WebClient.create("http://foo");
        wc.to("http://foo/bar", true);
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar"), wc.getCurrentURI());
    }

    @Test
    public void testCompositePath() {
        WebClient wc = WebClient.create("http://foo");
        wc.path("/bar/baz/");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz/"), wc.getCurrentURI());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testWrongForward() {
        WebClient wc = WebClient.create("http://foo");
        wc.to("http://bar", true);
    }

    @Test
    public void testBaseCurrentPathAfterChange() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").matrix("m1", "m1value").query("q1", "q1value");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value?q1=q1value"), wc.getCurrentURI());
    }


    @Test
    public void testBaseCurrentPathAfterCopy() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").matrix("m1", "m1value").query("q1", "q1value");
        WebClient wc1 = WebClient.fromClient(wc);
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value?q1=q1value"), wc1.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value?q1=q1value"), wc1.getCurrentURI());
    }

    @Test
    public void testHeaders() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.header("h1", "h1value").header("h2", "h2value");
        assertEquals(1, wc.getHeaders().get("h1").size());
        assertEquals("h1value", wc.getHeaders().getFirst("h1"));
        assertEquals(1, wc.getHeaders().get("h2").size());
        assertEquals("h2value", wc.getHeaders().getFirst("h2"));
        wc.getHeaders().clear();
        assertEquals(1, wc.getHeaders().get("h1").size());
        assertEquals("h1value", wc.getHeaders().getFirst("h1"));
        assertEquals(1, wc.getHeaders().get("h2").size());
        assertEquals("h2value", wc.getHeaders().getFirst("h2"));
        wc.reset();
        assertTrue(wc.getHeaders().isEmpty());
    }

    @Test
    public void testBackFast() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").matrix("m1", "m1value");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value"), wc.getCurrentURI());
        wc.back(true);
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
    }

    @Test
    public void testBack() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz"), wc.getCurrentURI());
        wc.back(false);
        assertEquals(URI.create("http://foo/bar"), wc.getCurrentURI());
        wc.back(false);
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
        wc.back(false);
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
    }

    @Test
    public void testResetQueryAndBack() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").query("foo", "bar");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz?foo=bar"), wc.getCurrentURI());
        wc.resetQuery().back(false);
        assertEquals(URI.create("http://foo/bar"), wc.getCurrentURI());
    }

    @Test
    public void testReplaceQuery() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").query("foo", "bar");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz?foo=bar"), wc.getCurrentURI());
        wc.replaceQuery("foo1=bar1");
        assertEquals(URI.create("http://foo/bar/baz?foo1=bar1"), wc.getCurrentURI());
    }

    @Test
    public void testReplaceQueryParam() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").query("foo", "bar").query("foo1", "bar1");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz?foo=bar&foo1=bar1"), wc.getCurrentURI());
        wc.replaceQueryParam("foo1", "baz");
        assertEquals(URI.create("http://foo/bar/baz?foo=bar&foo1=baz"), wc.getCurrentURI());
    }

    @Test
    public void testReplacePathAll() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz"), wc.getCurrentURI());
        wc.replacePath("/new");
        assertEquals(URI.create("http://foo/new"), wc.getCurrentURI());
    }
    @Test
    public void testReplacePathLastSegment() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz"), wc.getCurrentURI());
        wc.replacePath("new");
        assertEquals(URI.create("http://foo/bar/new"), wc.getCurrentURI());
    }

    @Test
    public void testFragment() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").query("foo", "bar").fragment("1");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz?foo=bar#1"), wc.getCurrentURI());
    }


    @Test
    public void testPathWithTemplates() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());

        wc.path("{bar}/{foo}", 1, 2);
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/1/2"), wc.getCurrentURI());
    }

    @Test
    public void testWebClientConfiguration() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        assertNotNull(WebClient.getConfig(wc) != null);
    }

    @Test
    public void testWebClientParamConverter() {
        WebClient wc = WebClient.create("http://foo",
                                        Collections.singletonList(new ParamConverterProviderImpl()));
        wc.path(new ComplexObject());
        wc.query("param", new ComplexObject(), new ComplexObject());
        assertEquals("http://foo/complex?param=complex&param=complex", wc.getCurrentURI().toString());

    }

    @Test
    public void testProxyConfiguration() {
        // interface
        BookInterface proxy = JAXRSClientFactory.create("http://foo", BookInterface.class);
        assertNotNull(WebClient.getConfig(proxy) != null);
        // cglib
        BookStore proxy2 = JAXRSClientFactory.create("http://foo", BookStore.class);
        assertNotNull(WebClient.getConfig(proxy2) != null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProxyNull() {
        // interface
        BookInterface proxy = JAXRSClientFactory.create("http://foo", BookInterface.class);
        proxy.getBook(null);
    }

    @Test
    public void testWebClientAuthorization() {
        String auth = "auth";
        WebClient wc = WebClient.create(URI.create("http://foo")).authorization(auth);
        assertEquals(auth, wc.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testLanguageHeader() {
        WebClient wc = WebClient.create("http://foo").language("en_CA");
        assertEquals("en_CA", wc.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));
    }

    @Test
    public void testDefaultCookie() {
        WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.cookie(new Cookie("a", "1"));
        wc.cookie(new Cookie("b", "2"));
        assertThat(wc.getHeaders().get(HttpHeaders.COOKIE),
            containsInAnyOrder("$Version=1;a=1", "$Version=1;b=2"));
    }

    @Test
    public void testCookieNoVersion() {
        WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.cookie(new Cookie("a", "1", null, null, 0));
        wc.cookie(new Cookie("b", "2", null, null, 0));
        assertThat(wc.getHeaders().get(HttpHeaders.COOKIE),
            containsInAnyOrder("a=1", "b=2"));
    }

    @Test
    public void testCookieBuilder() {
        WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.cookie(new Cookie.Builder("a").value("1").build());
        wc.cookie(new Cookie.Builder("b").value("2").build());
        assertThat(wc.getHeaders().get(HttpHeaders.COOKIE),
            containsInAnyOrder("$Version=1;a=1", "$Version=1;b=2"));
    }

    @Test
    public void testWebClientClose() {
        final WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.getConfiguration().setShutdownBusOnClose(true);

        final Bus bus = wc.getConfiguration().getBus();
        assertThat(wc.getConfigurationReference(), is(not(nullValue())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));
        wc.close();

        assertThat(wc.getConfigurationReference(), is(nullValue()));
        assertThat(wc.getConfiguration(), is(nullValue()));

        assertThat(bus.getState(), equalTo(BusState.SHUTDOWN));
    }
    
    @Test
    public void testWebClientFromConcurrently() throws InterruptedException, ExecutionException, TimeoutException {
        final WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.getConfiguration().setShutdownBusOnClose(true);

        final List<Future<WebClient>> futures = new ArrayList<>(100);
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 100; ++i) {
            futures.add(executor.submit(() ->  WebClient.fromClient(wc)));
        }

        final Bus bus = wc.getConfiguration().getBus();
        final List<WebClient> clients = new ArrayList<>(100);
        try {
            for (Future<WebClient> future: futures) {
                final WebClient client = future.get(5, TimeUnit.SECONDS);
                assertThat(client, is(not(nullValue())));
                clients.add(client);
            }

            assertThat(bus.getState(), anyOf(equalTo(BusState.RUNNING),
                equalTo(BusState.INITIALIZING), equalTo(BusState.INITIAL)));
            assertThat(wc.getConfigurationReference(), is(not(nullValue())));
            assertThat(wc.getConfigurationReference().refCount(), equalTo(101L));

            for (WebClient client: clients) {
                executor.submit(client::close);
            }
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS), equalTo(true));
        }

        assertThat(wc.getConfigurationReference(), is(not(nullValue())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));
        wc.close();

        assertThat(wc.getConfigurationReference(), is(nullValue()));
        assertThat(wc.getConfiguration(), is(nullValue()));

        assertThat(bus.getState(), equalTo(BusState.SHUTDOWN));
    }

    @Test
    public void testWebClientFrom() {
        final WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.getConfiguration().setShutdownBusOnClose(true);

        assertThat(wc.getConfigurationReference(), is(not(nullValue())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));

        final WebClient wc1 = WebClient.fromClient(wc);
        assertThat(wc.getConfigurationReference(), equalTo(wc1.getConfigurationReference()));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(2L));
        wc.close();

        final ClientConfiguration configuration = wc1.getConfiguration();
        assertThat(configuration, is(not(nullValue())));
        assertThat(configuration.getBus().getState(), anyOf(equalTo(BusState.RUNNING),
            equalTo(BusState.INITIALIZING), equalTo(BusState.INITIAL)));

        assertThat(wc.getConfigurationReference(), is(nullValue()));
        assertThat(wc1.getConfigurationReference().refCount(), equalTo(1L));
        wc1.close();

        assertThat(wc1.getConfigurationReference(), is(nullValue()));
        assertThat(configuration.getBus().getState(), equalTo(BusState.SHUTDOWN));
    }
    
    @Test
    public void testWebClientFromChained() {
        final WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.getConfiguration().setShutdownBusOnClose(true);

        assertThat(wc.getConfigurationReference(), is(not(nullValue())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));

        final WebClient wc1 = WebClient.fromClient(wc);
        assertThat(wc.getConfigurationReference(), equalTo(wc1.getConfigurationReference()));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(2L));

        final WebClient wc2 = WebClient.fromClient(wc);
        assertThat(wc.getConfigurationReference(), equalTo(wc2.getConfigurationReference()));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(3L));
        wc.close();

        final ClientConfiguration configuration1 = wc1.getConfiguration();
        assertThat(configuration1, is(not(nullValue())));
        assertThat(configuration1.getBus().getState(), anyOf(equalTo(BusState.RUNNING),
            equalTo(BusState.INITIALIZING), equalTo(BusState.INITIAL)));
        assertThat(wc1.getConfigurationReference().refCount(), equalTo(2L));

        final ClientConfiguration configuration2 = wc2.getConfiguration();
        assertThat(configuration2, is(not(nullValue())));
        assertThat(configuration2.getBus(), is(configuration1.getBus()));
        wc1.close();

        assertThat(wc.getConfigurationReference(), is(nullValue()));
        assertThat(wc2.getConfigurationReference().refCount(), equalTo(1L));
        wc2.close();

        assertThat(wc2.getConfigurationReference(), is(nullValue()));
        assertThat(configuration2.getBus().getState(), equalTo(BusState.SHUTDOWN));
    }
    
    @Test
    public void testWebClientFromShare() {
        final WebClient wc = WebClient.create("http://foo").language("en_CA");
        wc.getConfiguration().setShutdownBusOnClose(true);
        wc.getConfiguration().getBus().setProperty(WebClient.USE_CONFIGURATION_REFERENCE_WHEN_COPY, false);

        assertThat(wc.getConfigurationReference(), is(not(nullValue())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));

        final WebClient wc1 = WebClient.fromClient(wc);
        assertThat(wc.getConfigurationReference(), not(equalTo(wc1.getConfigurationReference())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));
        assertThat(wc1.getConfigurationReference().refCount(), equalTo(1L));
        wc.close();

        final ClientConfiguration configuration = wc1.getConfiguration();
        assertThat(configuration, is(not(nullValue())));
        assertThat(configuration.getBus().getState(), equalTo(BusState.SHUTDOWN));

        assertThat(wc.getConfigurationReference(), is(nullValue()));
        assertThat(wc1.getConfigurationReference().refCount(), equalTo(1L));
        wc1.close();

        assertThat(wc1.getConfigurationReference(), is(nullValue()));
        assertThat(configuration.getBus().getState(), equalTo(BusState.SHUTDOWN));
    }

    private static final class ParamConverterProviderImpl implements ParamConverterProvider {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            return (ParamConverter<T>)new ParamConverterImpl();
        }

    }

    private static final class ParamConverterImpl implements ParamConverter<ComplexObject> {

        @Override
        public ComplexObject fromString(String value) throws IllegalArgumentException {
            return null;
        }

        @Override
        public String toString(ComplexObject value) throws IllegalArgumentException {
            return "complex";
        }
    }

    private static final class ComplexObject {

    }
}