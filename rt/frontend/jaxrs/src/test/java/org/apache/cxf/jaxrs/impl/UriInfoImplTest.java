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

package org.apache.cxf.jaxrs.impl;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodInvocationInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoStack;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UriInfoImplTest {
    @Test
    public void testResolve() {
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/", null), null);
        assertEquals("Wrong base path", "http://localhost:8080/baz/",
                     u.getBaseUri().toString());
        URI resolved = u.resolve(URI.create("a"));
        assertEquals("http://localhost:8080/baz/a", resolved.toString());
    }

    @Test
    public void testResolveNormalizeSimple() {
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", null), null);
        assertEquals("Wrong base path", "http://localhost:8080/baz",
                     u.getBaseUri().toString());
        URI resolved = u.resolve(URI.create("./a"));
        assertEquals("http://localhost:8080/a", resolved.toString());
    }

    @Test
    public void testRelativize() {
        UriInfoImpl u = new UriInfoImpl(
                                        mockMessage("http://localhost:8080/app/root", "/a/b/c"), null);
        assertEquals("Wrong Request Uri", "http://localhost:8080/app/root/a/b/c",
                     u.getRequestUri().toString());
        URI relativized = u.relativize(URI.create("http://localhost:8080/app/root/a/d/e"));
        assertEquals("../d/e", relativized.toString());
    }

    @Test
    public void testRelativizeAlreadyRelative() throws Exception {
        Message mockMessage = mockMessage("http://localhost:8080/app/root/",
            "/soup/");
        UriInfoImpl u = new UriInfoImpl(mockMessage, null);
        assertEquals("http://localhost:8080/app/root/soup/", u.getRequestUri()
                     .toString());
        URI x = URI.create("x/");
        assertEquals("http://localhost:8080/app/root/x/", u.resolve(x)
                     .toString());
        assertEquals("../x/", u.relativize(x).toString());
    }

    @Test
    public void testRelativizeNoCommonPrefix() throws Exception {
        Message mockMessage = mockMessage("http://localhost:8080/app/root/",
            "/soup");
        UriInfoImpl u = new UriInfoImpl(mockMessage, null);
        assertEquals("http://localhost:8080/app/root/soup", u.getRequestUri()
                     .toString());
        URI otherHost = URI.create("http://localhost:8081/app/root/x");
        assertEquals(otherHost, u.resolve(otherHost));

        // port/host is different!
        assertEquals(otherHost, u.relativize(otherHost));
    }

    @Test
    public void testRelativizeChild() throws Exception {
        /** From UriInfo.relativize() javadoc (2013-04-21):
         *
         * <br/><b>Request URI:</b> <tt>http://host:port/app/root/a/b/c</tt>
         * <br/><b>Supplied URI:</b> <tt>a/b/c/d/e</tt>
         * <br/><b>Returned URI:</b> <tt>d/e</tt>
         *
         * NOTE: Although the above is correct JAX-RS API-wise (as of 2013-04-21),
         * it is WRONG URI-wise (but correct API wise)
         * as the request URI is missing the trailing / -- if the request returned HTML at
         * that location, then resolving "d/e" would end up instead at /app/root/a/b/d/e
         * -- see URI.create("/app/root/a/b/c").resolve("d/e"). Therefore the below tests
         * use the slightly modified request URI http://example.com/app/root/a/b/c/ with a trailing /
         *
         * See the test testRelativizeSibling for a non-slash-ending request URI
         */
        Message mockMessage = mockMessage("http://example.com/app/root/",
            "/a/b/c/");
        UriInfoImpl u = new UriInfoImpl(mockMessage, null);
        assertEquals("http://example.com/app/root/a/b/c/", u.getRequestUri()
                     .toString());
        URI absolute = URI.create("http://example.com/app/root/a/b/c/d/e");
        assertEquals("d/e", u.relativize(absolute).toString());

        URI relativeToBase = URI.create("a/b/c/d/e");
        assertEquals("d/e", u.relativize(relativeToBase).toString());
    }

    @Test
    public void testRelativizeSibling() throws Exception {
        Message mockMessage = mockMessage("http://example.com/app/root/",
            "/a/b/c.html");
        UriInfoImpl u = new UriInfoImpl(mockMessage, null);
        // NOTE: No slash in the end!
        assertEquals("http://example.com/app/root/a/b/c.html", u
                     .getRequestUri().toString());
        URI absolute = URI.create("http://example.com/app/root/a/b/c.pdf");
        assertEquals("c.pdf", u.relativize(absolute).toString());

        URI relativeToBase = URI.create("a/b/c.pdf");
        assertEquals("c.pdf", u.relativize(relativeToBase).toString());
    }

    @Test
    public void testRelativizeGrandParent() throws Exception {
        Message mockMessage = mockMessage("http://example.com/app/root/",
            "/a/b/c/");
        UriInfoImpl u = new UriInfoImpl(mockMessage, null);
        // NOTE: All end with slashes (imagine they are folders)
        assertEquals("http://example.com/app/root/a/b/c/", u.getRequestUri()
                     .toString());
        URI absolute = URI.create("http://example.com/app/root/a/");
        // Need to go two levels up from /a/b/c/ to /a/
        assertEquals("../../", u.relativize(absolute).toString());

        URI relativeToBase = URI.create("a/");
        assertEquals("../../", u.relativize(relativeToBase).toString());
    }

    @Test
    public void testRelativizeCousin() throws Exception {
        Message mockMessage = mockMessage("http://example.com/app/root/",
            "/a/b/c/");
        UriInfoImpl u = new UriInfoImpl(mockMessage, null);
        // NOTE: All end with slashes (imagine they are folders)
        assertEquals("http://example.com/app/root/a/b/c/", u.getRequestUri()
                     .toString());
        URI absolute = URI.create("http://example.com/app/root/a/b2/c2/");
        // Need to go two levels up from /a/b/c/ to /a/
        assertEquals("../../b2/c2/", u.relativize(absolute).toString());

        URI relativeToBase = URI.create("a/b2/c2/");
        assertEquals("../../b2/c2/", u.relativize(relativeToBase).toString());
    }

    @Test
    public void testRelativizeOutsideBase() throws Exception {
        Message mockMessage = mockMessage("http://example.com/app/root/",
            "/a/b/c/");
        UriInfoImpl u = new UriInfoImpl(mockMessage, null);
        // NOTE: All end with slashes (imagine they are folders)
        assertEquals("http://example.com/app/root/a/b/c/", u.getRequestUri()
                     .toString());
        URI absolute = URI.create("http://example.com/otherapp/fred.txt");

        assertEquals("../../../../../otherapp/fred.txt", u.relativize(absolute)
                     .toString());

        URI relativeToBase = URI.create("../../otherapp/fred.txt");
        assertEquals("../../../../../otherapp/fred.txt",
                     u.relativize(relativeToBase).toString());
    }

    @Test
    public void testResolveNormalizeComplex() throws Exception {
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/1/2/3/", null), null);
        assertEquals("Wrong base path", "http://localhost:8080/baz/1/2/3/",
                     u.getBaseUri().toString());
        URI resolved = u.resolve(new URI("../../a"));
        assertEquals("http://localhost:8080/baz/1/a", resolved.toString());
    }

    @Test
    public void testGetAbsolutePath() {

        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz/bar",
                     u.getAbsolutePath().toString());

        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/", "/bar"),
                            null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz/bar",
                     u.getAbsolutePath().toString());

        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "bar"),
                            null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz/bar",
                     u.getAbsolutePath().toString());
    }

    @Test
    public void testGetPathSegments() {

        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080", "/bar/foo/x%2Fb"),
                                        null);
        List<PathSegment> segments = u.getPathSegments();
        assertEquals(3, segments.size());
        assertEquals("bar", segments.get(0).toString());
        assertEquals("foo", segments.get(1).toString());
        assertEquals("x/b", segments.get(2).toString());
    }

    @Test
    public void testGetEncodedPathSegments() {

        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080", "/bar/foo/x%2Fb"),
                                        null);
        List<PathSegment> segments = u.getPathSegments(false);
        assertEquals(3, segments.size());
        assertEquals("bar", segments.get(0).toString());
        assertEquals("foo", segments.get(1).toString());
        assertEquals("x%2Fb", segments.get(2).toString());
    }

    @Test
    public void testGetAbsolutePathWithEncodedChars() {

        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz%20foo", "/bar"),
                                        null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz%20foo/bar",
                     u.getAbsolutePath().toString());
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/%20foo", "/bar%20foo"),
                            null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz/%20foo/bar%20foo",
                     u.getAbsolutePath().toString());

    }

    @Test
    public void testGetQueryParameters() {
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("unexpected queries", 0, u.getQueryParameters().size());

        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar", "n=1%202"),
                            null);

        MultivaluedMap<String, String> qps = u.getQueryParameters(false);
        assertEquals("Number of queries is wrong", 1, qps.size());
        assertEquals("Wrong query value", qps.getFirst("n"), "1%202");

        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar",
            "N=0&n=1%202&n=3&b=2&a%2Eb=ab"),
                            null);

        qps = u.getQueryParameters();
        assertEquals("Number of queiries is wrong", 4, qps.size());
        assertEquals("Wrong query value", qps.get("N").get(0), "0");
        assertEquals("Wrong query value", qps.get("n").get(0), "1 2");
        assertEquals("Wrong query value", qps.get("n").get(1), "3");
        assertEquals("Wrong query value", qps.get("b").get(0), "2");
        assertEquals("Wrong query value", qps.get("a.b").get(0), "ab");

        Message m = mockMessage("http://localhost:8080/baz", "/bar",
                "N=0&n=1%202&n=3&&b=2&a%2Eb=ab");
        m.put("parse.query.value.as.collection", Boolean.TRUE);
        u = new UriInfoImpl(m, null);

        qps = u.getQueryParameters();
        assertEquals("Number of queries is wrong", 4, qps.size());
        assertEquals("Wrong query value", qps.get("N").get(0), "0");
        assertEquals("Wrong query value", qps.get("n").get(0), "1 2");
        assertEquals("Wrong query value", qps.get("n").get(1), "3");
        assertEquals("Wrong query value", qps.get("b").get(0), "2");
        assertEquals("Wrong query value", qps.get("a.b").get(0), "ab");
    }

    @Test
    public void testGetCaseinsensitiveQueryParameters() {
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("unexpected queries", 0, u.getQueryParameters().size());

        Message m = mockMessage("http://localhost:8080/baz", "/bar",
            "N=1%202&n=3&b=2&a%2Eb=ab");
        m.put("org.apache.cxf.http.case_insensitive_queries", "true");

        u = new UriInfoImpl(m, null);

        MultivaluedMap<String, String> qps = u.getQueryParameters();
        assertEquals("Number of queiries is wrong", 3, qps.size());
        assertEquals("Wrong query value", qps.get("n").get(0), "1 2");
        assertEquals("Wrong query value", qps.get("n").get(1), "3");
        assertEquals("Wrong query value", qps.get("b").get(0), "2");
        assertEquals("Wrong query value", qps.get("a.b").get(0), "ab");
    }

    @Test
    public void testGetRequestURI() {

        UriInfo u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/bar", "/foo", "n=1%202"),
                                    null);

        assertEquals("Wrong request uri", "http://localhost:8080/baz/bar/foo?n=1%202",
                     u.getRequestUri().toString());
    }

    @Test
    public void testGetRequestURIWithEncodedChars() {

        UriInfo u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/bar", "/foo/%20bar", "n=1%202"),
                                    null);

        assertEquals("Wrong request uri", "http://localhost:8080/baz/bar/foo/%20bar?n=1%202",
                     u.getRequestUri().toString());
    }

    @Test
    public void testGetTemplateParameters() {

        MultivaluedMap<String, String> values = new MetadataMap<>();
        new URITemplate("/bar").match("/baz", values);

        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        values);
        assertEquals("unexpected templates", 0, u.getPathParameters().size());

        values.clear();
        new URITemplate("/{id}").match("/bar%201", values);
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar%201"),
                            values);

        MultivaluedMap<String, String> tps = u.getPathParameters(false);
        assertEquals("Number of templates is wrong", 1, tps.size());
        assertEquals("Wrong template value", tps.getFirst("id"), "bar%201");

        values.clear();
        new URITemplate("/{id}/{baz}").match("/1%202/bar", values);
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/1%202/bar"),
                            values);

        tps = u.getPathParameters();
        assertEquals("Number of templates is wrong", 2, tps.size());
        assertEquals("Wrong template value", tps.getFirst("id"), "1 2");
        assertEquals("Wrong template value", tps.getFirst("baz"), "bar");

        // with suffix
        values.clear();
        new URITemplate("/bar").match("/bar", values);

        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                            values);
        assertEquals("unexpected templates", 0, u.getPathParameters().size());
    }

    @Test
    public void testGetBaseUri() {

        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", null), null);
        assertEquals("Wrong base path", "http://localhost:8080/baz",
                     u.getBaseUri().toString());
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/", null),
                            null);
        assertEquals("Wrong base path", "http://localhost:8080/baz/",
                     u.getBaseUri().toString());
    }

    @Test
    public void testGetPath() {

        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/bar/baz",
            "/baz"),
                                        null);
        assertEquals("Wrong path", "baz", u.getPath());

        u = new UriInfoImpl(mockMessage("http://localhost:8080/bar/baz",
            "/bar/baz"), null);
        assertEquals("Wrong path", "/", u.getPath());

        u = new UriInfoImpl(mockMessage("http://localhost:8080/bar/baz/",
            "/bar/baz/"), null);
        assertEquals("Wrong path", "/", u.getPath());

        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/baz/bar%201"),
                            null);
        assertEquals("Wrong path", "bar 1", u.getPath());

        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/baz/bar%201"),
                            null);
        assertEquals("Wrong path", "bar%201", u.getPath(false));


    }

    @Path("foo")
    public static class RootResource {

        @GET
        public Response get() {
            return null;
        }

        @GET
        @Path("bar")
        public Response getSubMethod() {
            return null;
        }

        @Path("sub")
        public SubResource getSubResourceLocator() {
            return new SubResource();
        }
    }

    public static class SubResource {
        @GET
        public Response getFromSub() {
            return null;
        }

        @GET
        @Path("subSub")
        public Response getFromSubSub() {
            return null;
        }
    }

    private static ClassResourceInfo getCri(Class<?> clazz, boolean setUriTemplate) {
        ClassResourceInfo cri = new ClassResourceInfo(clazz);
        Path path = AnnotationUtils.getClassAnnotation(clazz, Path.class);
        if (setUriTemplate) {
            cri.setURITemplate(URITemplate.createTemplate(path));
        }
        return cri;
    }

    private static OperationResourceInfo getOri(ClassResourceInfo cri, String methodName) throws Exception {
        Method method = cri.getResourceClass().getMethod(methodName);
        OperationResourceInfo ori = new OperationResourceInfo(method, cri);
        ori.setURITemplate(URITemplate.createTemplate(AnnotationUtils.getMethodAnnotation(method, Path.class)));
        return ori;
    }

    private static List<String> getMatchedURIs(UriInfo u) {
        return u.getMatchedURIs();
    }

    @Test
    public void testGetMatchedURIsRoot() throws Exception {
        System.out.println("testGetMatchedURIsRoot");
        Message m = mockMessage("http://localhost:8080/app", "/foo");
        OperationResourceInfoStack oriStack = new OperationResourceInfoStack();
        ClassResourceInfo cri = getCri(RootResource.class, true);
        OperationResourceInfo ori = getOri(cri, "get");

        MethodInvocationInfo miInfo = new MethodInvocationInfo(ori, RootResource.class, new ArrayList<String>());
        oriStack.push(miInfo);
        m.put(OperationResourceInfoStack.class, oriStack);

        UriInfoImpl u = new UriInfoImpl(m);
        List<String> matchedUris = getMatchedURIs(u);
        assertEquals(1, matchedUris.size());
        assertTrue(matchedUris.contains("foo"));
    }

    @Test
    public void testGetMatchedURIsRootSub() throws Exception {
        System.out.println("testGetMatchedURIsRootSub");
        Message m = mockMessage("http://localhost:8080/app", "/foo/bar");
        OperationResourceInfoStack oriStack = new OperationResourceInfoStack();
        ClassResourceInfo cri = getCri(RootResource.class, true);
        OperationResourceInfo ori = getOri(cri, "getSubMethod");

        MethodInvocationInfo miInfo = new MethodInvocationInfo(ori, RootResource.class, new ArrayList<String>());
        oriStack.push(miInfo);
        m.put(OperationResourceInfoStack.class, oriStack);

        UriInfoImpl u = new UriInfoImpl(m);
        List<String> matchedUris = getMatchedURIs(u);
        assertEquals(2, matchedUris.size());
        assertEquals("foo/bar", matchedUris.get(0));
        assertEquals("foo", matchedUris.get(1));
    }

    @Test
    public void testGetMatchedURIsSubResourceLocator() throws Exception {
        System.out.println("testGetMatchedURIsSubResourceLocator");
        Message m = mockMessage("http://localhost:8080/app", "/foo/sub");
        OperationResourceInfoStack oriStack = new OperationResourceInfoStack();
        ClassResourceInfo rootCri = getCri(RootResource.class, true);
        OperationResourceInfo rootOri = getOri(rootCri, "getSubResourceLocator");

        MethodInvocationInfo miInfo = new MethodInvocationInfo(rootOri, RootResource.class, new ArrayList<String>());
        oriStack.push(miInfo);

        ClassResourceInfo subCri = getCri(SubResource.class, false);
        OperationResourceInfo subOri = getOri(subCri, "getFromSub");

        miInfo = new MethodInvocationInfo(subOri, SubResource.class, new ArrayList<String>());
        oriStack.push(miInfo);
        m.put(OperationResourceInfoStack.class, oriStack);

        UriInfoImpl u = new UriInfoImpl(m);
        List<String> matchedUris = getMatchedURIs(u);
        assertEquals(2, matchedUris.size());
        assertEquals("foo/sub", matchedUris.get(0));
        assertEquals("foo", matchedUris.get(1));
    }

    @Test
    public void testGetMatchedURIsSubResourceLocatorSubPath() throws Exception {
        System.out.println("testGetMatchedURIsSubResourceLocatorSubPath");
        Message m = mockMessage("http://localhost:8080/app", "/foo/sub/subSub");
        OperationResourceInfoStack oriStack = new OperationResourceInfoStack();
        ClassResourceInfo rootCri = getCri(RootResource.class, true);
        OperationResourceInfo rootOri = getOri(rootCri, "getSubResourceLocator");

        MethodInvocationInfo miInfo = new MethodInvocationInfo(rootOri, RootResource.class, new ArrayList<String>());
        oriStack.push(miInfo);

        ClassResourceInfo subCri = getCri(SubResource.class, false);
        OperationResourceInfo subOri = getOri(subCri, "getFromSubSub");

        miInfo = new MethodInvocationInfo(subOri, SubResource.class, new ArrayList<String>());
        oriStack.push(miInfo);
        m.put(OperationResourceInfoStack.class, oriStack);

        UriInfoImpl u = new UriInfoImpl(m);
        List<String> matchedUris = getMatchedURIs(u);
        assertEquals(3, matchedUris.size());
        assertEquals("foo/sub/subSub", matchedUris.get(0));
        assertEquals("foo/sub", matchedUris.get(1));
        assertEquals("foo", matchedUris.get(2));
    }

    private Message mockMessage(String baseAddress, String pathInfo) {
        return mockMessage(baseAddress, pathInfo, null, null);
    }

    private Message mockMessage(String baseAddress, String pathInfo, String query) {
        return mockMessage(baseAddress, pathInfo, query, null);
    }

    private Message mockMessage(String baseAddress, String pathInfo,
                                String query, String fragment) {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        ServletDestination d = mock(ServletDestination.class);
        e.setDestination(d);
        EndpointInfo epr = new EndpointInfo();
        epr.setAddress(baseAddress);
        when(d.getEndpointInfo()).thenReturn(epr);
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        return m;
    }

}
