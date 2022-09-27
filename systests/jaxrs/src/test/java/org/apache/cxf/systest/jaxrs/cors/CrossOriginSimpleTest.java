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

package org.apache.cxf.systest.jaxrs.cors;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.cors.CorsHeaderConstants;
import org.apache.cxf.systest.jaxrs.AbstractSpringServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.HttpClientBuilder;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for CORS. This isn't precisely simple as it's turned out.
 *
 * Note that it's not the server's job to detect invalid CORS requests. If a client
 * fails to preflight, it's just not our job. However, also note that all 'actual'
 * requests are treated as simple requests. In other words, a DELETE gets the same
 * treatment as a simple request. The 'hey, this is complex' test happens on the client,
 * which thus decides to do a preflight.
 *
 */
public class CrossOriginSimpleTest extends AbstractBusClientServerTestBase {
    public static final int PORT = SpringServer.PORT;
    private WebClient configClient;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(SpringServer.class, true));
    }

    @Before
    public void before() {
        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        configClient = WebClient.create("http://localhost:" + PORT + "/config", providers);
    }

    private List<String> headerValues(Header[] headers) {
        List<String> values = new ArrayList<>();
        for (Header h : headers) {
            for (HeaderElement e : h.getElements()) {
                values.add(e.getName());
            }
        }
        return values;
    }

    private void assertAllOrigin(boolean allOrigins, String[] originList, String[] requestOrigins,
                                 boolean permitted) throws ClientProtocolException, IOException {
        configureAllowOrigins(allOrigins, originList);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/untest/simpleGet/HelloThere");
        if (requestOrigins != null) {
            StringBuilder ob = new StringBuilder();
            for (String requestOrigin : requestOrigins) {
                ob.append(requestOrigin);
                ob.append(' '); // extra trailing space won't hurt.
            }
            httpget.addHeader("Origin", ob.toString());
        }
        HttpResponse response = httpclient.execute(httpget);
        assertEquals(200, response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        String e = IOUtils.toString(entity.getContent());

        assertEquals("HelloThere", e); // ensure that we didn't bust the operation itself.
        assertOriginResponse(allOrigins, requestOrigins, permitted, response);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    private void assertOriginResponse(boolean allOrigins, String[] requestOrigins, boolean permitted,
                                      HttpResponse response) {
        Header[] aaoHeaders = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN);
        if (permitted) {
            assertNotNull(aaoHeaders);
            if (allOrigins) {
                assertEquals(1, aaoHeaders.length);
                assertEquals("*", aaoHeaders[0].getValue());
            } else {
                List<String> ovalues = headerValues(aaoHeaders);
                assertEquals(1, ovalues.size()); // get back one ac-allow-origin header.
                String[] origins = ovalues.get(0).split(" +");
                for (int x = 0; x < requestOrigins.length; x++) {
                    assertEquals(requestOrigins[x], origins[x]);
                }
            }
        } else {
            // Origin: null? We don't use it and it's not in the CORS spec.
            assertTrue(aaoHeaders == null || aaoHeaders.length == 0);
        }
    }

    private void configureAllowOrigins(boolean allOrigins, String[] originList) {
        if (allOrigins) {
            originList = new String[0];
        }
        // tell filter what to do.
        String confResult = configClient.accept("text/plain").replacePath("/setOriginList")
            .type("application/json").post(originList, String.class);
        assertEquals("ok", confResult);
    }

    @Test
    public void failNoOrigin() throws Exception {
        assertAllOrigin(true, null, null, false);
    }

    @Test
    public void allowStarPassOne() throws Exception {
        // Allow *, pass origin
        assertAllOrigin(true, null, new String[] {
            "http://localhost:" + PORT
        }, true);
    }

    @Test
    public void preflightPostClassAnnotationFail() throws ClientProtocolException, IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions httpoptions = new HttpOptions("http://localhost:" + PORT + "/antest/unannotatedPost");
        httpoptions.addHeader("Origin", "http://in.org");
        // nonsimple header
        httpoptions.addHeader("Content-Type", "application/json");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "POST");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, "X-custom-1");
        HttpResponse response = httpclient.execute(httpoptions);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals(0, response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN).length);
        assertEquals(0, response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS).length);
        assertEquals(0, response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS).length);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void preflightPostClassAnnotationFail2() throws ClientProtocolException, IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions httpoptions = new HttpOptions("http://localhost:" + PORT + "/antest/unannotatedPost");
        httpoptions.addHeader("Origin", "http://area51.mil:31415");
        httpoptions.addHeader("Content-Type", "application/json");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "POST");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, "X-custom-3");
        HttpResponse response = httpclient.execute(httpoptions);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals(0, response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN).length);
        assertEquals(0, response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS).length);
        assertEquals(0, response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS).length);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void preflightPostClassAnnotationPass() throws ClientProtocolException, IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions httpoptions = new HttpOptions("http://localhost:" + PORT + "/antest/unannotatedPost");
        httpoptions.addHeader("Origin", "http://area51.mil:31415");
        httpoptions.addHeader("Content-Type", "application/json");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "POST");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, "X-custom-1");
        HttpResponse response = httpclient.execute(httpoptions);
        assertEquals(200, response.getStatusLine().getStatusCode());
        Header[] origin = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN);
        assertEquals(1, origin.length);
        assertEquals("http://area51.mil:31415", origin[0].getValue());
        Header[] method = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS);
        assertEquals(1, method.length);
        assertEquals("POST", method[0].getValue());
        Header[] requestHeaders = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS);
        assertEquals(1, requestHeaders.length);
        assertEquals("X-custom-1", requestHeaders[0].getValue());
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void preflightPostClassAnnotationPass2() throws ClientProtocolException, IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions httpoptions = new HttpOptions("http://localhost:" + PORT + "/antest/unannotatedPost");
        httpoptions.addHeader("Origin", "http://area51.mil:31415");
        httpoptions.addHeader("Content-Type", "application/json");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "POST");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, "X-custom-1, X-custom-2");
        HttpResponse response = httpclient.execute(httpoptions);
        assertEquals(200, response.getStatusLine().getStatusCode());
        Header[] origin = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN);
        assertEquals(1, origin.length);
        assertEquals("http://area51.mil:31415", origin[0].getValue());
        Header[] method = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS);
        assertEquals(1, method.length);
        assertEquals("POST", method[0].getValue());
        Header[] requestHeaders = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS);
        assertEquals(1, requestHeaders.length);
        assertTrue(requestHeaders[0].getValue().contains("X-custom-1"));
        assertTrue(requestHeaders[0].getValue().contains("X-custom-2"));
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void simplePostClassAnnotation() throws ClientProtocolException, IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions httpoptions = new HttpOptions("http://localhost:" + PORT + "/antest/unannotatedPost");
        httpoptions.addHeader("Origin", "http://in.org");
        // nonsimple header
        httpoptions.addHeader("Content-Type", "text/plain");
        httpoptions.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "POST");
        HttpResponse response = httpclient.execute(httpoptions);
        assertEquals(200, response.getStatusLine().getStatusCode());
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void allowStarPassNone() throws Exception {
        // allow *, no origin
        assertAllOrigin(true, null, null, false);
    }

    @Test
    public void allowOnePassOne() throws Exception {
        // allow one, pass that one
        assertAllOrigin(false, new String[] {
            "http://localhost:" + PORT
        }, new String[] {
            "http://localhost:" + PORT
        }, true);
    }

    @Test
    public void allowOnePassWrong() throws Exception {
        // allow one, pass something else
        assertAllOrigin(false, new String[] {
            "http://localhost:" + PORT
        }, new String[] {
            "http://area51.mil:31315",
        }, false);
    }

    @Test
    public void allowTwoPassOne() throws Exception {
        // allow two, pass one
        assertAllOrigin(false, new String[] {
            "http://localhost:" + PORT, "http://area51.mil:3141"
        }, new String[] {
            "http://localhost:" + PORT
        }, true);
    }

    @Test
    public void allowTwoPassTwo() throws Exception {
        // allow two, pass two
        assertAllOrigin(false, new String[] {
            "http://localhost:" + PORT, "http://area51.mil:3141"
        }, new String[] {
            "http://localhost:" + PORT, "http://area51.mil:3141"
        }, true);
    }

    @Test
    public void allowTwoPassThree() throws Exception {
        // allow two, pass three
        assertAllOrigin(false, new String[] {
            "http://localhost:" + PORT, "http://area51.mil:3141"
        }, new String[] {
            "http://localhost:" + PORT, "http://area51.mil:3141", "http://hogwarts.edu:9"
        }, false);

    }

    @Test
    public void testAllowCredentials() throws Exception {
        String r = configClient.replacePath("/setAllowCredentials/true")
                .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/untest/simpleGet/HelloThere");
        httpget.addHeader("Origin", "http://localhost:" + PORT);

        HttpResponse response = httpclient.execute(httpget);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertAllowCredentials(response, true);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void testForbidCredentials() throws Exception {
        String r = configClient.replacePath("/setAllowCredentials/false")
                .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/untest/simpleGet/HelloThere");
        httpget.addHeader("Origin", "http://localhost:" + PORT);

        HttpResponse response = httpclient.execute(httpget);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertAllowCredentials(response, false);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void testNonSimpleActualRequest() throws Exception {
        configureAllowOrigins(true, null);
        String r = configClient.replacePath("/setAllowCredentials/false")
            .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpDelete httpdelete = new HttpDelete("http://localhost:" + PORT + "/untest/delete");
        httpdelete.addHeader("Origin", "http://localhost:" + PORT);

        HttpResponse response = httpclient.execute(httpdelete);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertAllowCredentials(response, false);
        assertOriginResponse(true, null, true, response);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }
    }

    private void assertAllowCredentials(HttpResponse response, boolean correct) {
        Header[] aaoHeaders = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS);
        assertEquals(1, aaoHeaders.length);
        assertEquals(Boolean.toString(correct), aaoHeaders[0].getValue());
    }

    @Test
    public void testAnnotatedSimple() throws Exception {
        configureAllowOrigins(true, null);
        String r = configClient.replacePath("/setAllowCredentials/false")
            .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/untest/annotatedGet/HelloThere");
        // this is the origin we expect to get.
        httpget.addHeader("Origin", "http://area51.mil:31415");
        HttpResponse response = httpclient.execute(httpget);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertOriginResponse(false, new String[]{"http://area51.mil:31415"}, true, response);
        assertAllowCredentials(response, false);
        List<String> exposeHeadersValues
            = headerValues(response.getHeaders(CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS));
        assertEquals(Arrays.asList(new String[] {"X-custom-3", "X-custom-4" }), exposeHeadersValues);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void testAnnotatedMethodPreflight() throws Exception {
        configureAllowOrigins(true, null);
        String r = configClient.replacePath("/setAllowCredentials/false")
            .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions http = new HttpOptions("http://localhost:" + PORT + "/untest/annotatedPut");
        // this is the origin we expect to get.
        http.addHeader("Origin", "http://area51.mil:31415");
        http.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "PUT");
        http.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, "X-custom-1, x-custom-2");
        HttpResponse response = httpclient.execute(http);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertOriginResponse(false, new String[]{"http://area51.mil:31415"}, true, response);
        assertAllowCredentials(response, true);
        List<String> exposeHeadersValues
            = headerValues(response.getHeaders(CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS));
        // preflight never returns Expose-Headers
        assertEquals(Collections.emptyList(), exposeHeadersValues);
        List<String> allowHeadersValues
            = headerValues(response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS));
        assertEquals(Arrays.asList(new String[] {"X-custom-1", "x-custom-2" }), allowHeadersValues);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void testAnnotatedMethodPreflight2() throws Exception {
        configureAllowOrigins(true, null);
        String r = configClient.replacePath("/setAllowCredentials/false")
            .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions http = new HttpOptions("http://localhost:" + PORT + "/untest/annotatedPut2");
        // this is the origin we expect to get.
        http.addHeader("Origin", "http://area51.mil:31415");
        http.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "PUT");
        http.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, "X-custom-1, x-custom-2");
        HttpResponse response = httpclient.execute(http);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertOriginResponse(false, new String[]{"http://area51.mil:31415"}, true, response);
        assertAllowCredentials(response, true);
        List<String> exposeHeadersValues
            = headerValues(response.getHeaders(CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS));
        // preflight never returns Expose-Headers
        assertEquals(Collections.emptyList(), exposeHeadersValues);
        List<String> allowHeadersValues
            = headerValues(response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS));
        assertEquals(Arrays.asList(new String[] {"X-custom-1", "x-custom-2" }), allowHeadersValues);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }

    }

    @Test
    public void testAnnotatedClassCorrectOrigin() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/antest/simpleGet/HelloThere");
        httpget.addHeader("Origin", "http://area51.mil:31415");

        HttpResponse response = httpclient.execute(httpget);
        assertEquals(200, response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        String e = IOUtils.toString(entity.getContent());

        assertEquals("HelloThere", e); // ensure that we didn't bust the operation itself.
        assertOriginResponse(false, new String[] {"http://area51.mil:31415" }, true, response);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }
    }

    @Test
    public void testAnnotatedClassCorrectOrigin2() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/antest2/simpleGet/HelloThere");
        httpget.addHeader("Origin", "http://area51.mil:31415");

        HttpResponse response = httpclient.execute(httpget);
        assertEquals(200, response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        String e = IOUtils.toString(entity.getContent());

        assertEquals("HelloThere", e); // ensure that we didn't bust the operation itself.
        assertOriginResponse(false, new String[] {"http://area51.mil:31415" }, true, response);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }
    }

    @Test
    public void testAnnotatedClassWrongOrigin() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/antest/simpleGet/HelloThere");
        httpget.addHeader("Origin", "http://su.us:1001");

        HttpResponse response = httpclient.execute(httpget);
        assertEquals(200, response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        String e = IOUtils.toString(entity.getContent());

        assertEquals("HelloThere", e);
        assertOriginResponse(false, null, false, response);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }
    }

    @Test
    public void testAnnotatedLocalPreflight() throws Exception {
        configureAllowOrigins(true, null);
        String r = configClient.replacePath("/setAllowCredentials/false")
            .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions http = new HttpOptions("http://localhost:" + PORT + "/antest/delete");
        // this is the origin we expect to get.
        http.addHeader("Origin", "http://area51.mil:3333");
        http.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "DELETE");
        HttpResponse response = httpclient.execute(http);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertOriginResponse(false, new String[]{"http://area51.mil:3333"}, true, response);
        assertAllowCredentials(response, false);
        List<String> exposeHeadersValues
            = headerValues(response.getHeaders(CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS));
        // preflight never returns Expose-Headers
        assertEquals(Collections.emptyList(), exposeHeadersValues);
        List<String> allowedMethods
            = headerValues(response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS));
        assertEquals(Arrays.asList("DELETE PUT"), allowedMethods);
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }
    }

    @Test
    public void testAnnotatedLocalPreflightNoGo() throws Exception {
        configureAllowOrigins(true, null);
        String r = configClient.replacePath("/setAllowCredentials/false")
            .accept("text/plain").post(null, String.class);
        assertEquals("ok", r);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpOptions http = new HttpOptions("http://localhost:" + PORT + "/antest/delete");
        // this is the origin we expect to get.
        http.addHeader("Origin", "http://area51.mil:4444");
        http.addHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, "DELETE");
        HttpResponse response = httpclient.execute(http);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertOriginResponse(false, new String[]{"http://area51.mil:4444"}, false, response);
        // we could check that the others are also missing.
        if (httpclient instanceof Closeable) {
            ((Closeable)httpclient).close();
        }
    }

    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public static final int PORT = allocatePortAsInt(SpringServer.class);

        public SpringServer() {
            super("/jaxrs_cors", PORT);
        }
    }
}
