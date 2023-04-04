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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;

import jakarta.activation.DataSource;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.SeBootstrap.Configuration;
import jakarta.ws.rs.SeBootstrap.Instance;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.core.Variant.VariantListBuilder;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.RuntimeDelegate;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.bootstrap.ConfigurationBuilderImpl;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource") // Responses built in this test don't need to be closed
public class ResponseImplTest {

    @Test
    public void testReadEntityWithNullOutMessage() {
        final String str = "ouch";

        Response response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                  .entity(str)
                  .build();
        assertEquals(str, response.readEntity(String.class));
    }

    @Test
    public void testReadBufferedStaxUtils() throws Exception {
        ResponseImpl r = new ResponseImpl(200);
        Source responseSource = readResponseSource(r);
        Document doc = StaxUtils.read(responseSource);
        assertEquals("Response", doc.getDocumentElement().getLocalName());
    }

    @Test
    public void testReadBufferedStaxSource() throws Exception {
        ResponseImpl r = new ResponseImpl(200);
        Source responseSource = readResponseSource(r);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        DOMResult res = new DOMResult();
        transformer.transform(responseSource, res);
        Document doc = (Document)res.getNode();
        assertEquals("Response", doc.getDocumentElement().getLocalName());
    }

    private Source readResponseSource(ResponseImpl r) {
        String content = "<Response "
            + " xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\""
            + " xmlns:ns2=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\">"
            + "<Result><Decision>Permit</Decision><Status><StatusCode"
            + " Value=\"urn:oasis:names:tc:xacml:1.0:status:ok\"/></Status></Result></Response>";


        MultivaluedMap<String, Object> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "text/xml");
        r.addMetadata(headers);
        r.setEntity(new ByteArrayInputStream(content.getBytes()), null);
        r.setOutMessage(createMessage());
        r.bufferEntity();
        return r.readEntity(Source.class);
    }

    private Message createMessage() {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        e.setOutMessage(new MessageImpl());
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(null);
        when(endpoint.get(Application.class.getName())).thenReturn(null);
        when(endpoint.size()).thenReturn(0);
        when(endpoint.isEmpty()).thenReturn(true);
        when(endpoint.get(ServerProviderFactory.class.getName())).thenReturn(factory);
        e.put(Endpoint.class, endpoint);
        return m;
    }

    @Test
    public void testResourceImpl() {
        String entity = "bar";
        ResponseImpl ri = new ResponseImpl(200, entity);
        assertEquals("Wrong status", ri.getStatus(), 200);
        assertSame("Wrong entity", entity, ri.getEntity());

        MetadataMap<String, Object> meta = new MetadataMap<>();
        ri.addMetadata(meta);
        ri.getMetadata();
        assertSame("Wrong metadata", meta, ri.getMetadata());
        assertSame("Wrong metadata", meta, ri.getHeaders());
    }

    @Test
    public void testGetHeaderStringUsingHeaderDelegate() throws Exception {
        StringBean bean = new StringBean("s3");
        RuntimeDelegate original = RuntimeDelegate.getInstance();
        RuntimeDelegate.setInstance(new StringBeanRuntimeDelegate(original));
        try {
            Response response = Response.ok().header(bean.get(), bean).build();
            String header = response.getHeaderString(bean.get());
            assertTrue(header.contains(bean.get()));
        } finally {
            RuntimeDelegate.setInstance(original);
            StringBeanRuntimeDelegate.assertNotStringBeanRuntimeDelegate();
        }
    }

    @Test
    public void testHasEntity() {
        assertTrue(new ResponseImpl(200, "").hasEntity());
        assertFalse(new ResponseImpl(200).hasEntity());
    }

    @Test
    public void testHasEntityWithEmptyStreamThatIsMarkSupported() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        when(entityStream.markSupported()).thenReturn(true);
        entityStream.mark(1);
        when(entityStream.read()).thenReturn(-1);
        entityStream.reset();
        assertFalse(new ResponseImpl(200, entityStream).hasEntity());
    }

    @Test
    public void testHasEntityWithNonEmptyStreamThatIsMarkSupported() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        when(entityStream.markSupported()).thenReturn(true);
        entityStream.mark(1);
        when(entityStream.read()).thenReturn(0);
        entityStream.reset();
        assertTrue(new ResponseImpl(200, entityStream).hasEntity());
    }

    @Test
    public void testHasEntityWithEmptyStreamThatIsNotMarkSupported() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        when(entityStream.markSupported()).thenReturn(false);
        when(entityStream.available()).thenReturn(0);
        when(entityStream.read()).thenReturn(-1);
        assertFalse(new ResponseImpl(200, entityStream).hasEntity());
    }

    @Test
    public void testHasEntityWithNonEmptyStreamThatIsNotMarkSupportedButIsAvailableSupported() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        when(entityStream.markSupported()).thenReturn(false);
        when(entityStream.available()).thenReturn(10);
        assertTrue(new ResponseImpl(200, entityStream).hasEntity());
    }

    @Test
    public void testHasEntityWithnNonEmptyStreamThatIsNotMarkSupportedNorAvailableSupported() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        when(entityStream.markSupported()).thenReturn(false);
        when(entityStream.available()).thenReturn(0);
        when(entityStream.read()).thenReturn(0);
        assertTrue(new ResponseImpl(200, entityStream).hasEntity());
        verify(entityStream, times(1)).markSupported();
        verify(entityStream, times(1)).available();
        verify(entityStream, times(1)).read();
    }

    @Test
    public void testGetEntityUnwrapped() {
        final Book book = new Book();
        Response r = Response.ok().entity(
            new GenericEntity<Book>(book) {
            }
        ).build();
        assertSame(book, r.getEntity());
    }

    @Test
    public void testGetEntity() {
        final Book book = new Book();
        Response r = Response.ok().entity(book).build();
        assertSame(book, r.getEntity());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetEntityAfterClose() {
        Response response = Response.ok("entity").build();
        response.close();
        response.getEntity();
    }

    @Test
    public void testStatuInfoForOKStatus() {
        StatusType si = new ResponseImpl(200, "").getStatusInfo();
        assertNotNull(si);
        assertEquals(200, si.getStatusCode());
        assertEquals(Status.Family.SUCCESSFUL, si.getFamily());
        assertEquals("OK", si.getReasonPhrase());
    }

    @Test
    public void testStatuInfoForClientErrorStatus() {
        StatusType si = new ResponseImpl(400, "").getStatusInfo();
        assertNotNull(si);
        assertEquals(400, si.getStatusCode());
        assertEquals(Status.Family.CLIENT_ERROR, si.getFamily());
        assertEquals("Bad Request", si.getReasonPhrase());
    }

    @Test
    public void testStatuInfoForClientErrorStatus2() {
        StatusType si = new ResponseImpl(499, "").getStatusInfo();
        assertNotNull(si);
        assertEquals(499, si.getStatusCode());
        assertEquals(Status.Family.CLIENT_ERROR, si.getFamily());
        assertEquals("", si.getReasonPhrase());
    }

    @Test
    public void testReasonPhrase() {
        int statusCode = 111;
        String reasonPhrase = "custom info";
        Response response = Response.status(statusCode, reasonPhrase).build();

        assertNotNull(response);
        assertEquals(statusCode, response.getStatus());
        assertEquals(reasonPhrase, response.getStatusInfo().getReasonPhrase());
    }

    @Test(expected = IllegalStateException.class)
    public void testHasEntityAfterClose() {
        Response r = new ResponseImpl(200, new ByteArrayInputStream("data".getBytes()));
        assertTrue(r.hasEntity());
        r.close();
        r.hasEntity();
    }


    @Test
    public void testBufferEntityNoEntity() {
        Response r = new ResponseImpl(200);
        assertFalse(r.bufferEntity());
    }

    @Test
    public void testGetHeaderString() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        ri.addMetadata(meta);
        assertNull(ri.getHeaderString("a"));
        meta.putSingle("a", "aValue");
        assertEquals("aValue", ri.getHeaderString("a"));
        meta.add("a", "aValue2");
        assertEquals("aValue,aValue2", ri.getHeaderString("a"));
    }

    @Test
    public void testGetHeaderStrings() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add("Set-Cookie", NewCookie.valueOf("a=b"));
        ri.addMetadata(meta);
        MultivaluedMap<String, String> headers = ri.getStringHeaders();
        assertEquals(1, headers.size());
        assertEquals("a=b;Version=1", headers.getFirst("Set-Cookie"));
    }

    @Test
    public void testGetCookiesWithEmptyValues() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add("Set-Cookie", NewCookie.valueOf("a="));
        meta.add("Set-Cookie", NewCookie.valueOf("c=\"\""));
        ri.addMetadata(meta);
        Map<String, NewCookie> cookies = ri.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("a=\"\";Version=1", cookies.get("a").toString());
        assertEquals("c=\"\";Version=1", cookies.get("c").toString());
        assertEquals("", cookies.get("a").getValue());
        assertEquals("", cookies.get("c").getValue());
    }
    
    @Test
    public void testGetCookies() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add("Set-Cookie", NewCookie.valueOf("a=b"));
        meta.add("Set-Cookie", NewCookie.valueOf("c=d"));
        ri.addMetadata(meta);
        Map<String, NewCookie> cookies = ri.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("a=b;Version=1", cookies.get("a").toString());
        assertEquals("c=d;Version=1", cookies.get("c").toString());
    }

    @Test
    public void testGetContentLength() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        ri.addMetadata(meta);
        assertEquals(-1, ri.getLength());
        meta.add("Content-Length", "10");
        assertEquals(10, ri.getLength());
    }

    @Test
    public void testGetDate() {
        doTestDate(HttpHeaders.DATE);
    }

    @Test
    public void testLastModified() {
        doTestDate(HttpHeaders.LAST_MODIFIED);
    }

    public void doTestDate(String dateHeader) {
        boolean date = HttpHeaders.DATE.equals(dateHeader);
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add(dateHeader, "Tue, 21 Oct 2008 17:00:00 GMT");
        ri.addMetadata(meta);
        assertEquals(HttpUtils.getHttpDate("Tue, 21 Oct 2008 17:00:00 GMT"),
                     date ? ri.getDate() : ri.getLastModified());
    }

    @Test
    public void testEntityTag() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add(HttpHeaders.ETAG, "1234");
        ri.addMetadata(meta);
        assertEquals(EntityTag.valueOf("\"1234\""), ri.getEntityTag());
    }

    @Test
    public void testLocation() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add(HttpHeaders.LOCATION, "http://localhost:8080");
        ri.addMetadata(meta);
        assertEquals(URI.create("http://localhost:8080"), ri.getLocation());
    }

    @Test
    public void testGetLanguage() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add(HttpHeaders.CONTENT_LANGUAGE, "en-US");
        ri.addMetadata(meta);
        assertEquals(Locale.US, ri.getLanguage());
    }

    @Test
    public void testGetMediaType() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        meta.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML);
        ri.addMetadata(meta);
        assertEquals(MediaType.TEXT_XML_TYPE, ri.getMediaType());
    }

    @Test
    public void testGetNoLinkBuilder() throws Exception {
        Response response = Response.ok().build();
        Builder builder = response.getLinkBuilder("anyrelation");
        assertNull(builder);
    }

    protected static List<Variant> getVariantList(List<String> encoding,
                                                  MediaType... mt) {
        return Variant.VariantListBuilder.newInstance()
            .mediaTypes(mt)
            .languages(new Locale("en", "US"), new Locale("en", "GB"), new Locale("zh", "CN"))
            .encodings(encoding.toArray(new String[]{}))
            .add()
            .build();
    }

    @Test
    public void testGetLinks() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<>();
        ri.addMetadata(meta);
        assertFalse(ri.hasLink("next"));
        assertNull(ri.getLink("next"));
        assertFalse(ri.hasLink("prev"));
        assertNull(ri.getLink("prev"));

        meta.add(HttpHeaders.LINK, "<http://localhost:8080/next;a=b>;rel=next");
        meta.add(HttpHeaders.LINK, "<http://prev>;rel=prev");

        assertTrue(ri.hasLink("next"));
        Link next = ri.getLink("next");
        assertNotNull(next);
        assertTrue(ri.hasLink("prev"));
        Link prev = ri.getLink("prev");
        assertNotNull(prev);

        Set<Link> links = ri.getLinks();
        assertTrue(links.contains(next));
        assertTrue(links.contains(prev));

        assertEquals("http://localhost:8080/next;a=b", next.getUri().toString());
        assertEquals("next", next.getRel());
        assertEquals("http://prev", prev.getUri().toString());
        assertEquals("prev", prev.getRel());
    }

    @Test
    public void testGetLinksNoRel() {
        try (ResponseImpl ri = new ResponseImpl(200)) {
            MetadataMap<String, Object> meta = new MetadataMap<>();
            ri.addMetadata(meta);
    
            Set<Link> links = ri.getLinks();
            assertTrue(links.isEmpty());
    
            meta.add(HttpHeaders.LINK, "<http://next>");
            meta.add(HttpHeaders.LINK, "<http://prev>");
    
            assertFalse(ri.hasLink("next"));
            Link next = ri.getLink("next");
            assertNull(next);
            assertFalse(ri.hasLink("prev"));
            Link prev = ri.getLink("prev");
            assertNull(prev);
    
            links = ri.getLinks();
            assertTrue(links.contains(Link.fromUri("http://next").build()));
            assertTrue(links.contains(Link.fromUri("http://prev").build()));
        }
    }

    @Test
    public void testGetLinksMultiple() {
        try (ResponseImpl ri = new ResponseImpl(200)) {
            MetadataMap<String, Object> meta = new MetadataMap<>();
            ri.addMetadata(meta);

            Set<Link> links = ri.getLinks();
            assertTrue(links.isEmpty());

            meta.add(HttpHeaders.LINK, "<http://next>;rel=\"next\",<http://prev>;rel=\"prev\"");

            assertTrue(ri.hasLink("next"));
            Link next = ri.getLink("next");
            assertNotNull(next);
            assertTrue(ri.hasLink("prev"));
            Link prev = ri.getLink("prev");
            assertNotNull(prev);

            links = ri.getLinks();
            assertTrue(links.contains(Link.fromUri("http://next").rel("next").build()));
            assertTrue(links.contains(Link.fromUri("http://prev").rel("prev").build()));
        }
    }
    

    @Test
    public void testGetMultipleWithSingleLink() {
        try (ResponseImpl ri = new ResponseImpl(200)) {
            MetadataMap<String, Object> meta = new MetadataMap<>();
            ri.addMetadata(meta);

            Set<Link> links = ri.getLinks();
            assertTrue(links.isEmpty());

            meta.add(HttpHeaders.LINK, "<http://next>;rel=\"next\",");

            assertTrue(ri.hasLink("next"));
            Link next = ri.getLink("next");
            assertNotNull(next);

            links = ri.getLinks();
            assertTrue(links.contains(Link.fromUri("http://next").rel("next").build()));
        }
    }

    @Test
    public void testGetLink() {
        try (ResponseImpl ri = new ResponseImpl(200)) {
            MetadataMap<String, Object> meta = new MetadataMap<>();
            ri.addMetadata(meta);

            Set<Link> links = ri.getLinks();
            assertTrue(links.isEmpty());

            meta.add(HttpHeaders.LINK, "<http://next>;rel=\"next\"");

            assertTrue(ri.hasLink("next"));
            Link next = ri.getLink("next");
            assertNotNull(next);

            links = ri.getLinks();
            assertTrue(links.contains(Link.fromUri("http://next").rel("next").build()));
        }
    }

    @Test
    public void testGetLinksMultipleMultiline() {
        try (ResponseImpl ri = new ResponseImpl(200)) {
            MetadataMap<String, Object> meta = new MetadataMap<>();
            ri.addMetadata(meta);
            
            final Message outMessage = createMessage();
            outMessage.put(Message.REQUEST_URI, "http://localhost");
            ri.setOutMessage(outMessage);

            Set<Link> links = ri.getLinks();
            assertTrue(links.isEmpty());

            meta.add(HttpHeaders.LINK, 
                "</TheBook/chapter2>;\n"
                + "         rel=\"prev\", \n"
                + "         </TheBook/chapter4>;\n"
                + "         rel=\"next\";");

            assertTrue(ri.hasLink("next"));
            Link next = ri.getLink("next");
            assertNotNull(next);
            assertTrue(ri.hasLink("prev"));
            Link prev = ri.getLink("prev");
            assertNotNull(prev);

            links = ri.getLinks();
            assertTrue(links.contains(Link.fromUri("http://localhost/TheBook/chapter4").rel("next").build()));
            assertTrue(links.contains(Link.fromUri("http://localhost/TheBook/chapter2").rel("prev").build()));
        }
    }

    @Test
    public void testReadEntityAndGetEntityAfter() {
        final String str = "ouch";

        final ResponseImpl response = new ResponseImpl(500, str);
        final Message outMessage = createMessage();
        outMessage.put(Message.REQUEST_URI, "http://localhost");
        response.setOutMessage(outMessage);

        final MultivaluedMap<String, Object> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "text/xml");
        response.addMetadata(headers);

        assertEquals(str, response.readEntity(String.class));
        assertThrows(IllegalStateException.class, () -> response.getEntity());
    }
    
    @Test
    public void testReadInputStream() {
        final String str = "ouch";

        final ResponseImpl response = new ResponseImpl(500, str);
        final Message outMessage = createMessage();
        outMessage.put(Message.REQUEST_URI, "http://localhost");
        response.setOutMessage(outMessage);

        final MultivaluedMap<String, Object> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "text/xml");
        response.addMetadata(headers);

        assertNotNull(response.readEntity(InputStream.class));
        assertNotNull(response.getEntity());
        
        response.close();
    }

    @Test
    public void testReadDataSource() throws IOException {
        final String str = "ouch";
        final ResponseImpl response = new ResponseImpl(500, str);
        final Message outMessage = createMessage();
        outMessage.put(Message.REQUEST_URI, "http://localhost");
        response.setOutMessage(outMessage);

        final MultivaluedMap<String, Object> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "text/xml");
        response.addMetadata(headers);

        final DataSource ds = response.readEntity(DataSource.class);
        assertNotNull(ds);
        try (Reader reader = new InputStreamReader(ds.getInputStream(), StandardCharsets.UTF_8)) {
            final CharBuffer buffer = CharBuffer.allocate(str.length());
            reader.read(buffer);
            assertEquals(str, buffer.flip().toString());
        }
        
        response.close();
    }
    
    @Test
    public void testReadEntityWithAnnotations() {
        final String str = "ouch";

        final ResponseImpl response = new ResponseImpl(500, str);
        final Message outMessage = createMessage();
        outMessage.put(Message.REQUEST_URI, "http://localhost");
        response.setOutMessage(outMessage);

        final MultivaluedMap<String, Object> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "text/xml");
        response.addMetadata(headers);

        final Annotation[] annotations = String.class.getAnnotations();
        assertEquals(str, response.readEntity(String.class, annotations));
        assertThrows(IllegalStateException.class, 
            () -> response.readEntity(Reader.class, annotations));
    }

    @Test
    public void testBufferAndReadInputStream() throws IOException {
        final String str = "ouch";

        try (ByteArrayInputStream out = new ByteArrayInputStream(str.getBytes())) {
            final ResponseImpl response = new ResponseImpl(500, out);
            final Message outMessage = createMessage();
            outMessage.put(Message.REQUEST_URI, "http://localhost");
            response.setOutMessage(outMessage);

            final MultivaluedMap<String, Object> headers = new MetadataMap<>();
            headers.putSingle("Content-Type", "text/rdf");
            response.addMetadata(headers);
            
            assertTrue(response.bufferEntity());
            assertNotNull(response.readEntity(InputStream.class));
            assertNotNull(response.getEntity());
            assertTrue(response.hasEntity());
    
            assertNotNull(response.readEntity(InputStream.class));
            assertNotNull(response.getEntity());
            assertTrue(response.hasEntity());
    
            response.close();
        }
    }
    
    @Test
    public void testBufferAndReadInputStreamWithException() throws IOException {
        final String str = "ouch";

        try (ByteArrayInputStream out = new ByteArrayInputStream(str.getBytes())) {
            final ResponseImpl response = new ResponseImpl(500, out);
            final Message outMessage = createMessage();
            outMessage.put(Message.REQUEST_URI, "http://localhost");
            response.setOutMessage(outMessage);

            ProviderFactory factory = ProviderFactory.getInstance(outMessage);
            factory.registerUserProvider(new FaultyMessageBodyReader<InputStream>());

            final MultivaluedMap<String, Object> headers = new MetadataMap<>();
            headers.putSingle("Content-Type", "text/rdf");
            response.addMetadata(headers);
            
            assertTrue(response.bufferEntity());
            assertThrows(ResponseProcessingException.class, () -> response.readEntity(InputStream.class));
            assertNotNull(response.getEntity());
            assertTrue(response.hasEntity());
    
            assertThrows(ResponseProcessingException.class, () -> response.readEntity(InputStream.class));
            assertNotNull(response.getEntity());
            assertTrue(response.hasEntity());
    
            response.close();
        }
    }
    
    @Provider
    @Consumes("text/rdf")
    public static class FaultyMessageBodyReader<T> implements MessageBodyReader<T> {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }
        
        @Override
        public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream) 
                    throws IOException, WebApplicationException {
            IOUtils.consume(entityStream);
            throw new IOException();
        }
    }
    
    public static class StringBean {
        private String header;

        public StringBean(String header) {
            super();
            this.header = header;
        }

        public String get() {
            return header;
        }

        public void set(String h) {
            this.header = h;
        }

        @Override
        public String toString() {
            return "StringBean. To get a value, use rather #get() method.";
        }
    }

    public static class StringBeanRuntimeDelegate extends RuntimeDelegate {
        private RuntimeDelegate original;
        public StringBeanRuntimeDelegate(RuntimeDelegate orig) {
            super();
            this.original = orig;
            assertNotStringBeanRuntimeDelegate(orig);
        }

        @Override
        public <T> T createEndpoint(Application arg0, Class<T> arg1)
            throws IllegalArgumentException, UnsupportedOperationException {
            return original.createEndpoint(arg0, arg1);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> arg0)
            throws IllegalArgumentException {
            if (arg0 == StringBean.class) {
                return (HeaderDelegate<T>) new StringBeanHeaderDelegate();
            }
            return original.createHeaderDelegate(arg0);
        }

        @Override
        public ResponseBuilder createResponseBuilder() {
            return original.createResponseBuilder();
        }

        @Override
        public UriBuilder createUriBuilder() {
            return original.createUriBuilder();
        }

        @Override
        public VariantListBuilder createVariantListBuilder() {
            return original.createVariantListBuilder();
        }

        public RuntimeDelegate getOriginal() {
            return original;
        }

        public static final void assertNotStringBeanRuntimeDelegate() {
            RuntimeDelegate delegate = RuntimeDelegate.getInstance();
            assertNotStringBeanRuntimeDelegate(delegate);
        }

        public static final void assertNotStringBeanRuntimeDelegate(RuntimeDelegate delegate) {
            if (delegate instanceof StringBeanRuntimeDelegate) {
                StringBeanRuntimeDelegate sbrd = (StringBeanRuntimeDelegate) delegate;
                if (sbrd.getOriginal() != null) {
                    RuntimeDelegate.setInstance(sbrd.getOriginal());
                    throw new RuntimeException(
                        "RuntimeDelegate.getInstance() is StringBeanRuntimeDelegate");
                }
            }
        }

        @Override
        public Builder createLinkBuilder() {
            return original.createLinkBuilder();
        }

        @Override
        public jakarta.ws.rs.SeBootstrap.Configuration.Builder createConfigurationBuilder() {
            return new ConfigurationBuilderImpl();
        }

        @Override
        public CompletionStage<Instance> bootstrap(Application application, Configuration configuration) {
            return original.bootstrap(application, configuration);
        }

        @Override
        public CompletionStage<Instance> bootstrap(Class<? extends Application> clazz, Configuration configuration) {
            return original.bootstrap(clazz, configuration);
        }

        @Override
        public EntityPart.Builder createEntityPartBuilder(String partName) throws IllegalArgumentException {
            return original.createEntityPartBuilder(partName);
        }
    }

    public static class StringBeanHeaderDelegate implements HeaderDelegate<StringBean> {

        @Override
        public StringBean fromString(String string) throws IllegalArgumentException {
            return new StringBean(string);
        }

        @Override
        public String toString(StringBean bean) throws IllegalArgumentException {
            return bean.get();
        }

    }
}
