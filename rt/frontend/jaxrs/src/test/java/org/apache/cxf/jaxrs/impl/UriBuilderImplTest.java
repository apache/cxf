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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.BookStore;
import org.apache.cxf.jaxrs.resources.UriBuilderWrongAnnotations;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import org.junit.Assert;
import org.junit.Test;

public class UriBuilderImplTest extends Assert {

    @Test(expected = IllegalArgumentException.class)
    public void testCtorNull() throws Exception {
        new UriBuilderImpl(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testPathStringNull() throws Exception {
        new UriBuilderImpl().path((String)null);
    }

    @Test
    public void testCtorAndBuild() throws Exception {
        URI uri = new URI("http://foo/bar/baz?query=1#fragment");
        URI newUri = new UriBuilderImpl(uri).build();
        assertEquals("URI is not built correctly", uri, newUri);
    }

    @Test
    public void testTrailingSlash() throws Exception {
        URI uri = new URI("http://bar/");
        URI newUri = new UriBuilderImpl(uri).build();
        assertEquals("URI is not built correctly", "http://bar/", newUri.toString());
    }
    
    @Test
    public void testPathTrailingSlash() throws Exception {
        URI uri = new URI("http://bar");
        URI newUri = new UriBuilderImpl(uri).path("/").build();
        assertEquals("URI is not built correctly", "http://bar/", newUri.toString());
    }
    
    @Test
    public void testPathTrailingSlash2() throws Exception {
        URI uri = new URI("http://bar");
        URI newUri = new UriBuilderImpl(uri).path("/").path("/").build();
        assertEquals("URI is not built correctly", "http://bar/", newUri.toString());
    }
    
    @Test
    public void testClone() throws Exception {
        URI uri = new URI("http://bar");
        URI newUri = new UriBuilderImpl(uri).clone().build();   
        assertEquals("URI is not built correctly", "http://bar", newUri.toString());
    }
    
    @Test
    public void testClonePctEncodedFromUri() throws Exception {
        URI uri = new URI("http://bar/foo%20");
        URI newUri = new UriBuilderImpl(uri).clone().buildFromEncoded();   
        assertEquals("URI is not built correctly", "http://bar/foo%20", newUri.toString());
    }
    
    @Test
    public void testClonePctEncoded() throws Exception {
        URI uri = new URI("http://bar");
        URI newUri = new UriBuilderImpl(uri)
            .path("{a}").path("{b}")
            .matrixParam("m", "m1 ", "m2+%20")
            .queryParam("q", "q1 ", "q2+q3%20").clone().buildFromEncoded("a+ ", "b%2B%20 ");   
        assertEquals("URI is not built correctly", 
                     "http://bar/a+%20/b%2B%20%20;m=m1%20;m=m2+%20?q=q1+&q=q2%2Bq3%20", 
                     newUri.toString());
    }
    
    @Test
    public void testEncodedPathQueryFromExistingURI() throws Exception {
        URI uri = new URI("http://bar/foo+%20%2B?q=a+b%20%2B");
        URI newUri = new UriBuilderImpl(uri).buildFromEncoded();   
        assertEquals("URI is not built correctly", 
                     "http://bar/foo+%20%2B?q=a%2Bb%20%2B", newUri.toString());
    }
    
    @Test
    public void testEncodedAddedQuery() throws Exception {
        URI uri = new URI("http://bar");
        URI newUri = new UriBuilderImpl(uri).queryParam("q", "a+b%20%2B").buildFromEncoded();   
        assertEquals("URI is not built correctly", "http://bar?q=a%2Bb%20%2B", newUri.toString());
    }
    
    @Test
    public void testSchemeSpecificPart() throws Exception {
        URI uri = new URI("http://bar");
        URI newUri = new UriBuilderImpl(uri).scheme("https").schemeSpecificPart("//localhost:8080/foo/bar")
            .build();
        assertEquals("URI is not built correctly", "https://localhost:8080/foo/bar", newUri.toString());
    }
    
    @Test
    public void testOpaqueSchemeSpecificPart() throws Exception {
        URI expectedUri = new URI("mailto:javanet@java.net.com");
        URI newUri = new UriBuilderImpl().scheme("mailto")
            .schemeSpecificPart("javanet@java.net.com").build();
        assertEquals("URI is not built correctly", expectedUri, newUri);
    }
    
    @Test
    public void testReplacePath() throws Exception {
        URI uri = new URI("http://foo/bar/baz;m1=m1value");
        URI newUri = new UriBuilderImpl(uri).replacePath("/newpath").build();
        assertEquals("URI is not built correctly", "http://foo/newpath", newUri.toString());
    }
    
    @Test
    public void testReplaceNullPath() throws Exception {
        URI uri = new URI("http://foo/bar/baz;m1=m1value");
        URI newUri = new UriBuilderImpl(uri).replacePath(null).build();
        assertEquals("URI is not built correctly", "http://foo", newUri.toString());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testUriNull() throws Exception {
        new UriBuilderImpl().uri(null);
    }

    @Test
    public void testUri() throws Exception {
        URI uri = new URI("http://foo/bar/baz?query=1#fragment");
        URI newUri = new UriBuilderImpl().uri(uri).build();
        assertEquals("URI is not built correctly", uri, newUri);
    }

    @Test
    public void testBuildValues() throws Exception {
        URI uri = new URI("http://zzz");
        URI newUri = new UriBuilderImpl(uri).path("/{b}/{a}/{b}").build("foo", "bar", "baz");
        assertEquals("URI is not built correctly", new URI("http://zzz/foo/bar/foo"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildMissingValues() throws Exception {
        URI uri = new URI("http://zzz");
        new UriBuilderImpl(uri).path("/{b}/{a}/{b}").build("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildMissingValues2() throws Exception {
        URI uri = new URI("http://zzz");
        new UriBuilderImpl(uri).path("/{b}").build();
    }

    @Test
    public void testBuildValueWithBrackets() throws Exception {
        URI uri = new URI("http://zzz");
        URI newUri = new UriBuilderImpl(uri).path("/{a}").build("{foo}");
        assertEquals("URI is not built correctly", new URI("http://zzz/%7Bfoo%7D"), newUri);
    }

    @Test
    public void testBuildValuesPct() throws Exception {
        URI uri = new URI("http://zzz");
        URI newUri = new UriBuilderImpl(uri).path("/{a}").build("foo%25/bar%");
        assertEquals("URI is not built correctly", new URI("http://zzz/foo%2525/bar%25"), newUri);
    }

    @Test
    public void testBuildValuesPctEncoded() throws Exception {
        URI uri = new URI("http://zzz");
        URI newUri = new UriBuilderImpl(uri).path("/{a}/{b}/{c}")
            .buildFromEncoded("foo%25", "bar%", "baz%20");
        assertEquals("URI is not built correctly", new URI("http://zzz/foo%25/bar%25/baz%20"), newUri);
    }

    @Test
    public void testBuildFromMapValues() throws Exception {
        URI uri = new URI("http://zzz");
        Map<String, String> map = new HashMap<String, String>();
        map.put("b", "foo");
        map.put("a", "bar");
        Map<String, String> immutable = Collections.unmodifiableMap(map);
        URI newUri = new UriBuilderImpl(uri).path("/{b}/{a}/{b}").buildFromMap(immutable);
        assertEquals("URI is not built correctly", new URI("http://zzz/foo/bar/foo"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildFromMapMissingValues() throws Exception {
        URI uri = new URI("http://zzz");
        Map<String, String> map = new HashMap<String, String>();
        map.put("b", "foo");
        Map<String, String> immutable = Collections.unmodifiableMap(map);
        new UriBuilderImpl(uri).path("/{b}/{a}/{b}").buildFromMap(immutable);
    }

    @Test
    public void testBuildFromMapValueWithBrackets() throws Exception {
        URI uri = new URI("http://zzz");
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "{foo}");
        Map<String, String> immutable = Collections.unmodifiableMap(map);
        URI newUri = new UriBuilderImpl(uri).path("/{a}").buildFromMap(immutable);
        assertEquals("URI is not built correctly", new URI("http://zzz/%7Bfoo%7D"), newUri);
    }

    @Test
    public void testBuildFromMapValuesPct() throws Exception {
        URI uri = new URI("http://zzz");
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "foo%25/bar%");
        Map<String, String> immutable = Collections.unmodifiableMap(map);
        URI newUri = new UriBuilderImpl(uri).path("/{a}").buildFromMap(immutable);
        assertEquals("URI is not built correctly", new URI("http://zzz/foo%2525/bar%25"), newUri);
    }

    @Test
    public void testBuildFromMapValuesPctEncoded() throws Exception {
        URI uri = new URI("http://zzz");
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "foo%25");
        map.put("b", "bar%");
        Map<String, String> immutable = Collections.unmodifiableMap(map);
        URI newUri = new UriBuilderImpl(uri).path("/{a}/{b}").buildFromEncodedMap(immutable);
        assertEquals("URI is not built correctly", new URI("http://zzz/foo%25/bar%25"), newUri);
    }

    @Test
    public void testAddPath() throws Exception {
        URI uri = new URI("http://foo/bar");
        URI newUri = new UriBuilderImpl().uri(uri).path("baz").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar/baz"), newUri);
        newUri = new UriBuilderImpl().uri(uri).path("baz").path("1").path("2").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar/baz/1/2"), newUri);
    }

    @Test
    public void testAddPathSlashes() throws Exception {
        URI uri = new URI("http://foo/");
        URI newUri = new UriBuilderImpl().uri(uri).path("/bar").path("baz/").path("/blah/").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar/baz/blah/"), newUri);
    }

    @Test
    public void testAddPathSlashes2() throws Exception {
        URI uri = new URI("http://foo/");
        URI newUri = new UriBuilderImpl().uri(uri).path("/bar///baz").path("blah//").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar/baz/blah/"), newUri);
    }

    @Test
    public void testAddPathSlashes3() throws Exception {
        URI uri = new URI("http://foo/");
        URI newUri = new UriBuilderImpl().uri(uri).path("/bar/").path("").path("baz").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar/baz"), newUri);
    }

    @Test
    public void testAddPathClass() throws Exception {
        URI uri = new URI("http://foo/");
        URI newUri = new UriBuilderImpl().uri(uri).path(BookStore.class).path("/").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bookstore/"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathClassNull() throws Exception {
        new UriBuilderImpl().path((Class)null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathClassNoAnnotation() throws Exception {
        new UriBuilderImpl().path(this.getClass()).build();
    }

    @Test
    public void testAddPathClassMethod() throws Exception {
        URI uri = new URI("http://foo/");
        URI newUri = new UriBuilderImpl().uri(uri).path(BookStore.class, "updateBook").path("bar").build();
        assertEquals("URI is not built correctly", new URI("http://foo/books/bar"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathClassMethodNull1() throws Exception {
        new UriBuilderImpl().path(null, "methName").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathClassMethodNull2() throws Exception {
        new UriBuilderImpl().path(BookStore.class, null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathClassMethodTooMany() throws Exception {
        new UriBuilderImpl().path(UriBuilderWrongAnnotations.class, "overloaded").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathClassMethodTooLess() throws Exception {
        new UriBuilderImpl().path(BookStore.class, "nonexistingMethod").build();
    }

    @Test
    public void testAddPathMethod() throws Exception {
        Method meth = BookStore.class.getMethod("updateBook", Book.class);
        URI uri = new URI("http://foo/");
        URI newUri = new UriBuilderImpl().uri(uri).path(meth).path("bar").build();
        assertEquals("URI is not built correctly", new URI("http://foo/books/bar"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathMethodNull() throws Exception {
        new UriBuilderImpl().path((Method)null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPathMethodNoAnnotation() throws Exception {
        Method noAnnot = BookStore.class.getMethod("getBook", String.class);
        new UriBuilderImpl().path(noAnnot).build();
    }

    @Test
    public void testSchemeHostPortQueryFragment() throws Exception {
        URI uri = new URI("http://foo:1234/bar?n1=v1&n2=v2#fragment");
        URI newUri = new UriBuilderImpl().scheme("http").host("foo").port(1234).path("bar").queryParam("n1",
                                                                                                       "v1")
            .queryParam("n2", "v2").fragment("fragment").build();
        compareURIs(uri, newUri);
    }

    @Test
    public void testReplaceQueryNull() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1&p2=v2");
        URI newUri = new UriBuilderImpl(uri).replaceQuery(null).build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
    }

    @Test
    public void testReplaceQueryEmpty() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1&p2=v2");
        URI newUri = new UriBuilderImpl(uri).replaceQuery("").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
    }

    @Test
    public void testReplaceQuery() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1");
        URI newUri = new UriBuilderImpl(uri).replaceQuery("p1=nv1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1"), newUri);
    }

    @Test
    public void testReplaceQuery2() throws Exception {
        URI uri = new URI("http://foo/bar");
        URI newUri = new UriBuilderImpl(uri).replaceQuery("p1=nv1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryParamNameNull() throws Exception {
        new UriBuilderImpl().queryParam(null, "baz");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryParamNullVal() throws Exception {
        new UriBuilderImpl().queryParam("foo", "bar", null, "baz");
    }

    @Test
    public void testQueryParamSameNameAndVal() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1");
        URI newUri = new UriBuilderImpl(uri).queryParam("p1", "v1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p1=v1"), newUri);
    }

    @Test
    public void testQueryParamVal() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1");
        URI newUri = new UriBuilderImpl(uri).queryParam("p2", "v2").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p2=v2"), newUri);
    }

    @Test
    public void testQueryParamSameNameDiffVal() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1");
        URI newUri = new UriBuilderImpl(uri).queryParam("p1", "v2").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p1=v2"), newUri);
    }

    @Test
    public void testQueryParamMultiVal() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1");
        URI newUri = new UriBuilderImpl(uri).queryParam("p1", "v2", "v3").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p1=v2&p1=v3"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceQueryParamNameNull() throws Exception {
        new UriBuilderImpl().replaceQueryParam(null, "baz");
    }

    @Test
    public void testReplaceQueryParamValNull() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1&p2=v2&p1=v3");
        URI newUri = new UriBuilderImpl(uri).replaceQueryParam("p1", (Object)null).build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p2=v2"), newUri);
    }

    @Test
    public void testReplaceQueryParamValEmpty() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1&p2=v2&p1=v3");
        URI newUri = new UriBuilderImpl(uri).replaceQueryParam("p1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p2=v2"), newUri);
    }

    @Test
    public void testReplaceQueryParamExisting() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1");
        URI newUri = new UriBuilderImpl(uri).replaceQueryParam("p1", "nv1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1"), newUri);
    }

    @Test
    public void testReplaceQueryParamExistingMulti() throws Exception {
        URI uri = new URI("http://foo/bar?p1=v1&p2=v2");
        URI newUri = new UriBuilderImpl(uri).replaceQueryParam("p1", "nv1", "nv2").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1&p1=nv2&p2=v2"), newUri);
    }

    @Test
    public void testReplaceMatrixNull() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
        URI newUri = new UriBuilderImpl(uri).replaceMatrix(null).build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
    }

    @Test
    public void testReplaceMatrixEmpty() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
        URI newUri = new UriBuilderImpl(uri).replaceMatrix("").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
    }

    @Test
    public void testReplaceMatrix() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
        URI newUri = new UriBuilderImpl(uri).replaceMatrix("p1=nv1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=nv1"), newUri);
    }

    @Test
    public void testReplaceMatrix2() throws Exception {
        URI uri = new URI("http://foo/bar/");
        URI newUri = new UriBuilderImpl(uri).replaceMatrix("p1=nv1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar/;p1=nv1"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMatrixParamNameNull() throws Exception {
        new UriBuilderImpl().matrixParam(null, "baz");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMatrixParamNullVal() throws Exception {
        new UriBuilderImpl().matrixParam("foo", "bar", null, "baz");
    }

    @Test
    public void testMatrixParamSameNameAndVal() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1");
        URI newUri = new UriBuilderImpl(uri).matrixParam("p1", "v1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p1=v1"), newUri);
    }

    @Test
    public void testMatrixParamNewNameAndVal() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1");
        URI newUri = new UriBuilderImpl(uri).matrixParam("p2", "v2").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p2=v2"), newUri);
    }

    @Test
    public void testMatrixParamSameNameDiffVal() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1");
        URI newUri = new UriBuilderImpl(uri).matrixParam("p1", "v2").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p1=v2"), newUri);
    }

    
    @Test
    public void testMatrixParamMultiSameNameNewVals() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1");
        URI newUri = new UriBuilderImpl(uri).matrixParam("p1", "v2", "v3").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p1=v2;p1=v3"), newUri);
    }
    
    @Test
    public void testPctEncodedMatrixParam() throws Exception {
        URI uri = new URI("http://foo/bar");
        URI newUri = new UriBuilderImpl(uri).matrixParam("p1", "v1%20").buildFromEncoded();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1%20"), newUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceMatrixParamNameNull() throws Exception {
        new UriBuilderImpl().replaceMatrixParam(null, "baz");
    }

    @Test
    public void testReplaceMatrixParamValNull() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1;p2=v2;p1=v3?noise=bazzz");
        URI newUri = new UriBuilderImpl(uri).replaceMatrixParam("p1", (Object)null).build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p2=v2?noise=bazzz"), newUri);
    }

    @Test
    public void testReplaceMatrixParamValEmpty() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1;p2=v2;p1=v3?noise=bazzz");
        URI newUri = new UriBuilderImpl(uri).replaceMatrixParam("p1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p2=v2?noise=bazzz"), newUri);
    }

    @Test
    public void testReplaceMatrixParamExisting() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1");
        URI newUri = new UriBuilderImpl(uri).replaceMatrixParam("p1", "nv1").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=nv1"), newUri);
    }

    @Test
    public void testReplaceMatrixParamExistingMulti() throws Exception {
        URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
        URI newUri = new UriBuilderImpl(uri).replaceMatrixParam("p1", "nv1", "nv2").build();
        assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=nv1;p1=nv2;p2=v2"), newUri);
    }

    @Test
    public void testMatrixNonFinalPathSegment() throws Exception {
        URI uri = new URI("http://blah/foo;p1=v1/bar");
        URI newUri = new UriBuilderImpl(uri).build();
        assertEquals("URI is not built correctly", new URI("http://blah/foo;p1=v1/bar"), newUri);
    }
    
    @Test
    public void testMatrixFinalPathSegment() throws Exception {
        URI uri = new URI("http://blah/foo;p1=v1/bar;p2=v2");
        URI newUri = new UriBuilderImpl(uri).build();
        assertEquals("URI is not built correctly", new URI("http://blah/foo;p1=v1/bar;p2=v2"), newUri);
    }
    
    @Test
    public void testAddPathWithMatrix() throws Exception {
        URI uri = new URI("http://blah/foo/bar;p1=v1");
        URI newUri = new UriBuilderImpl(uri).path("baz;p2=v2").build();
        assertEquals("URI is not built correctly", new URI("http://blah/foo/bar;p1=v1/baz;p2=v2"), newUri);
    }
    
    @Test
    public void testNonHttpSchemes() {
        String[] uris = {"ftp://ftp.is.co.za/rfc/rfc1808.txt",
                         "mailto:java-net@java.sun.com",
                         "news:comp.lang.java",
                         "urn:isbn:096139212y",
                         "ldap://[2001:db8::7]/c=GB?objectClass?one",
                         "telnet://194.1.2.17:81/",
                         "tel:+1-816-555-1212",
                         "foo://bar.com:8042/there/here?name=baz#brr"};

        int expectedCount = 0; 
         
        for (int i = 0; i < uris.length; i++) {
            URI uri = UriBuilder.fromUri(uris[i]).build();
            assertEquals("Strange", uri.toString(), uris[i]);
            expectedCount++;
        }
        assertEquals(8, expectedCount);
    }
    
    private void compareURIs(URI uri1, URI uri2) {
        
        assertEquals("Unexpected scheme", uri1.getScheme(), uri2.getScheme());
        assertEquals("Unexpected host", uri1.getHost(), uri2.getHost());
        assertEquals("Unexpected port", uri1.getPort(), uri2.getPort());
        assertEquals("Unexpected path", uri1.getPath(), uri2.getPath());
        assertEquals("Unexpected fragment", uri1.getFragment(), uri2.getFragment());
        
        MultivaluedMap<String, String> queries1 = 
            JAXRSUtils.getStructuredParams(uri1.getRawQuery(), "&", false);
        MultivaluedMap<String, String> queries2 = 
            JAXRSUtils.getStructuredParams(uri2.getRawQuery(), "&", false);
        assertEquals("Unexpected queries", queries1, queries2);
    }
}
