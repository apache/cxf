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

package org.apache.cxf.jaxrs.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.validation.Schema;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.CustomerParameterHandler;
import org.apache.cxf.jaxrs.JAXBContextProvider;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ProviderFactoryTest extends Assert {

    
    @Before
    public void setUp() {
        ProviderFactory.getInstance().clearProviders();
    }
    
    @Test
    public void testMultipleFactories() {
        assertNotSame(ProviderFactory.getInstance(), ProviderFactory.getSharedInstance());
        assertSame(ProviderFactory.getSharedInstance(), ProviderFactory.getSharedInstance());
        assertNotSame(ProviderFactory.getInstance(), ProviderFactory.getInstance());
    }
    
    @Test
    public void testCustomWadlHandler() {
        ProviderFactory pf = ProviderFactory.getInstance();
        assertEquals(1, pf.getRequestHandlers().size());
        assertTrue(pf.getRequestHandlers().get(0).getProvider() instanceof WadlGenerator);
        
        WadlGenerator wg = new WadlGenerator();
        pf.setUserProviders(Collections.singletonList(wg));
        assertEquals(1, pf.getRequestHandlers().size());
        assertTrue(pf.getRequestHandlers().get(0).getProvider() instanceof WadlGenerator);
        assertSame(wg, pf.getRequestHandlers().get(0).getProvider());
    }
    
    @Test
    public void testCustomTestHandler() {
        ProviderFactory pf = ProviderFactory.getInstance();
        assertEquals(1, pf.getRequestHandlers().size());
        assertTrue(pf.getRequestHandlers().get(0).getProvider() instanceof WadlGenerator);
        
        TestHandler th = new TestHandler();
        pf.setUserProviders(Collections.singletonList(th));
        assertEquals(2, pf.getRequestHandlers().size());
        assertTrue(pf.getRequestHandlers().get(0).getProvider() instanceof WadlGenerator);
        assertSame(th, pf.getRequestHandlers().get(1).getProvider());
    }
    
    @Test
    public void testCustomTestAndWadlHandler() {
        ProviderFactory pf = ProviderFactory.getInstance();
        assertEquals(1, pf.getRequestHandlers().size());
        assertTrue(pf.getRequestHandlers().get(0).getProvider() instanceof WadlGenerator);
        
        List<Object> providers = new ArrayList<Object>();
        WadlGenerator wg = new WadlGenerator();
        providers.add(wg);
        TestHandler th = new TestHandler();
        providers.add(th);
        pf.setUserProviders(providers);
        assertEquals(2, pf.getRequestHandlers().size());
        assertSame(wg, pf.getRequestHandlers().get(0).getProvider());
        assertSame(th, pf.getRequestHandlers().get(1).getProvider());
    }
    
    @Test
    public void testDefaultJaxbProvider() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        doTestDefaultJaxbProviderCloned(pf, "http://localhost:8080/base/");
        checkJaxbProvider(pf);
    }
    
    @Test
    public void testDefaultJaxbProviderMultipleThreads() throws Exception {
        for (int i = 0; i < 100; i++) {
            doTestDefaultJaxbProviderClonedMultipleThreads();
        }
    }
    
    
    public void doTestDefaultJaxbProviderClonedMultipleThreads() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(50, 50, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(50);
        
        addThreads(executor, pf, startSignal, doneSignal, 50);
        
        startSignal.countDown();
        doneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals("Not all invocations have completed", 0, doneSignal.getCount());
        checkJaxbProvider(pf);
    }
    
    private void addThreads(ThreadPoolExecutor executor, ProviderFactory pf,
                            CountDownLatch startSignal, CountDownLatch doneSignal, int count) {
        
        for (int i = 1; i <= count; i++) {
            executor.execute(new TestRunnable(pf, startSignal, doneSignal, 
                                              "http://localhost:8080/base/" + i));
        }
    }
    
    private void doTestDefaultJaxbProviderCloned(ProviderFactory pf, String property) {
        Message message = new MessageImpl();
        message.put(Message.QUERY_STRING, "uri=" + property);
        MessageBodyReader customJaxbReader = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, message);
        assertTrue(customJaxbReader instanceof JAXBElementProvider);
        
        JAXBElementProvider provider = (JAXBElementProvider)customJaxbReader;
        MessageContext mc = provider.getContext();
        assertNotNull(mc);
        UriInfo ui = mc.getUriInfo();
        MultivaluedMap<String, String> queries = ui.getQueryParameters();
        assertEquals(1, queries.size());
        List<String> uriQuery = queries.get("uri");
        assertEquals(1, uriQuery.size());
        assertEquals(property, uriQuery.get(0));
        
        MessageBodyReader customJaxbReader2 = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, message);
        assertSame(customJaxbReader, customJaxbReader2);
         
        MessageBodyWriter customJaxbWriter = pf.createMessageBodyWriter((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, message);
        assertSame(customJaxbReader, customJaxbWriter);
        
        MessageBodyReader jaxbReader = ProviderFactory.getSharedInstance().createMessageBodyReader(
            (Class<?>)Book.class, null, null, MediaType.TEXT_XML_TYPE, message);
        assertTrue(jaxbReader instanceof JAXBElementProvider);
        assertNotSame(jaxbReader, customJaxbReader);
    }
    
    private void checkJaxbProvider(ProviderFactory pf) {
        int count = 0;
        for (Object provider : pf.getReadersWriters()) {
            if (((ProviderInfo)provider).getProvider() instanceof JAXBElementProvider) {
                count++;
            }
        }
        assertEquals(1, count);
    }
    
    @Test
    public void testCustomJaxbProvider() {
        ProviderFactory pf = ProviderFactory.getInstance();
        JAXBElementProvider provider = new JAXBElementProvider();
        pf.registerUserProvider(provider);
        MessageBodyReader customJaxbReader = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(customJaxbReader, provider);
        
        MessageBodyWriter customJaxbWriter = pf.createMessageBodyWriter((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(customJaxbWriter, provider);
    }
    
    @Test
    public void testCustomJsonProvider() {
        ProviderFactory pf = ProviderFactory.getInstance();
        JSONProvider provider = new JSONProvider();
        pf.registerUserProvider(provider);
        MessageBodyReader customJsonReader = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                               MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertSame(customJsonReader, provider);
        
        MessageBodyWriter customJsonWriter = pf.createMessageBodyWriter((Class<?>)Book.class, null, null, 
                                               MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertSame(customJsonWriter, provider);
    }
    
    @Test
    public void testDefaultJsonProviderCloned() {
        ProviderFactory pf = ProviderFactory.getInstance();
        MessageBodyReader customJsonReader = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertTrue(customJsonReader instanceof JSONProvider);
        
        MessageBodyReader customJsonReader2 = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertSame(customJsonReader, customJsonReader2);
        
        MessageBodyWriter customJsonWriter = pf.createMessageBodyWriter((Class<?>)Book.class, null, null, 
                                                MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertSame(customJsonReader, customJsonWriter);
        
        MessageBodyReader jsonReader = ProviderFactory.getSharedInstance().createMessageBodyReader(
            (Class<?>)Book.class, null, null, MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertTrue(jsonReader instanceof JSONProvider);
        assertNotSame(jsonReader, customJsonReader);
    }
    
    @Test
    public void testDataSourceReader() {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(new DataSourceProvider());
        MessageBodyReader reader = pf.createMessageBodyReader(
              (Class<?>)DataSource.class, null, null, 
              MediaType.valueOf("image/png"), new MessageImpl());
        assertTrue(reader instanceof DataSourceProvider);
        MessageBodyReader reader2 = pf.createMessageBodyReader(
                          (Class<?>)DataHandler.class, null, null, 
                          MediaType.valueOf("image/png"), new MessageImpl());
        assertSame(reader, reader2);
    }
    
    @Test
    public void testDataSourceWriter() {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(new DataSourceProvider());
        MessageBodyWriter writer = pf.createMessageBodyWriter(
              (Class<?>)DataSource.class, null, null, 
              MediaType.valueOf("image/png"), new MessageImpl());
        assertTrue(writer instanceof DataSourceProvider);
        MessageBodyWriter writer2 = pf.createMessageBodyWriter(
                          (Class<?>)DataHandler.class, null, null, 
                          MediaType.valueOf("image/png"), new MessageImpl());
        assertSame(writer, writer2);
    }
    
    @Test
    public void testNoDataSourceWriter() {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(new DataSourceProvider());
        MessageBodyWriter writer = pf.createMessageBodyWriter(
              (Class<?>)DataSource.class, null, null, 
              MediaType.valueOf("multipart/form-data"), new MessageImpl());
        assertFalse(writer instanceof DataSourceProvider);
    }
    
    
    @Test
    public void testSchemaLocations() {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.setSchemaLocations(Collections.singletonList("classpath:/test.xsd"));
        MessageBodyReader customJaxbReader = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(customJaxbReader instanceof JAXBElementProvider);
        MessageBodyReader jaxbReader = ProviderFactory.getSharedInstance().createMessageBodyReader(
            (Class<?>)Book.class, null, null, MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(jaxbReader instanceof JAXBElementProvider);
        assertNotSame(jaxbReader, customJaxbReader);
        
        assertNull(((JAXBElementProvider)jaxbReader).getSchema());
        assertNotNull(((JAXBElementProvider)customJaxbReader).getSchema());
        
        MessageBodyReader customJsonReader = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                 MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertTrue(customJsonReader instanceof JSONProvider);
        MessageBodyReader jsonReader = ProviderFactory.getSharedInstance().createMessageBodyReader(
            (Class<?>)Book.class, null, null, MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertTrue(jsonReader instanceof JSONProvider);
        assertNotSame(jsonReader, customJsonReader);
        assertNull(((JSONProvider)jsonReader).getSchema());
        assertNotNull(((JSONProvider)customJsonReader).getSchema());
    }
    
    @Test
    public void testGetFactoryInboundMessage() {
        ProviderFactory factory = ProviderFactory.getInstance();
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory);
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        assertSame(ProviderFactory.getInstance(m), factory);
    }
    
    @Test
    public void testDefaultUserExceptionMappers() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        ExceptionMapper<?> mapper = 
            pf.createExceptionMapper(WebApplicationException.class, new MessageImpl());
        assertNotNull(mapper);
        WebApplicationExceptionMapper wm = new WebApplicationExceptionMapper(); 
        pf.registerUserProvider(wm);
        ExceptionMapper<?> mapper2 = 
            pf.createExceptionMapper(WebApplicationException.class, new MessageImpl());
        assertNotSame(mapper, mapper2);
        assertSame(wm, mapper2);
    }
    
    @Test
    public void testExceptionMappersHierarchy1() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        WebApplicationExceptionMapper wm = new WebApplicationExceptionMapper(); 
        pf.registerUserProvider(wm);
        assertSame(wm, pf.createExceptionMapper(WebApplicationException.class, new MessageImpl()));
        assertNull(pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
        TestRuntimeExceptionMapper rm = new TestRuntimeExceptionMapper(); 
        pf.registerUserProvider(rm);
        assertSame(wm, pf.createExceptionMapper(WebApplicationException.class, new MessageImpl()));
        assertSame(rm, pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
    }
    
    @Test
    public void testExceptionMappersHierarchy2() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        
        TestRuntimeExceptionMapper rm = new TestRuntimeExceptionMapper(); 
        pf.registerUserProvider(rm);
        assertSame(rm, pf.createExceptionMapper(WebApplicationException.class, new MessageImpl()));
        assertSame(rm, pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
        
        WebApplicationExceptionMapper wm = new WebApplicationExceptionMapper(); 
        pf.registerUserProvider(wm);
        assertSame(wm, pf.createExceptionMapper(WebApplicationException.class, new MessageImpl()));
        assertSame(rm, pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
    }
    
    @Test
    @Ignore
    public void testExceptionMappersHierarchyWithGenerics() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        RuntimeExceptionMapper1 exMapper1 = new RuntimeExceptionMapper1(); 
        pf.registerUserProvider(exMapper1);
        RuntimeExceptionMapper2 exMapper2 = new RuntimeExceptionMapper2(); 
        pf.registerUserProvider(exMapper2);
        assertSame(exMapper1, pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
        assertSame(exMapper2, pf.createExceptionMapper(WebApplicationException.class, new MessageImpl()));
    }
    
    @Test
    public void testMessageBodyHandlerHierarchy() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        List<Object> providers = new ArrayList<Object>();
        BookReaderWriter bookHandler = new BookReaderWriter();
        providers.add(bookHandler);
        SuperBookReaderWriter superBookHandler = new SuperBookReaderWriter();
        providers.add(superBookHandler);
        pf.setUserProviders(providers);
        assertSame(bookHandler, 
                   pf.createMessageBodyReader(Book.class, Book.class, new Annotation[]{}, 
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
        assertSame(superBookHandler, 
                   pf.createMessageBodyReader(SuperBook.class, SuperBook.class, new Annotation[]{}, 
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
        assertSame(bookHandler, 
                   pf.createMessageBodyWriter(Book.class, Book.class, new Annotation[]{}, 
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
        assertSame(superBookHandler, 
                   pf.createMessageBodyWriter(SuperBook.class, SuperBook.class, new Annotation[]{}, 
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
    }
    
    @Test
    public void testMessageBodyWriterNoTypes() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        List<Object> providers = new ArrayList<Object>();
        SuperBookReaderWriter2 superBookHandler = new SuperBookReaderWriter2();
        providers.add(superBookHandler);
        pf.setUserProviders(providers);
        assertSame(superBookHandler, 
                   pf.createMessageBodyReader(SuperBook.class, SuperBook.class, new Annotation[]{}, 
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
        assertSame(superBookHandler, 
                   pf.createMessageBodyWriter(SuperBook.class, SuperBook.class, new Annotation[]{}, 
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
    }
    
    @Test
    public void testSortEntityProviders() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(new TestStringProvider());
        pf.registerUserProvider(new PrimitiveTextProvider());
        
        List<ProviderInfo<MessageBodyReader>> readers = pf.getMessageReaders();

        assertTrue(indexOf(readers, TestStringProvider.class) 
                   < indexOf(readers, PrimitiveTextProvider.class));
        
        List<ProviderInfo<MessageBodyWriter>> writers = pf.getMessageWriters();

        assertTrue(indexOf(writers, TestStringProvider.class) 
                   < indexOf(writers, PrimitiveTextProvider.class));
        
    }
    
    @Test
    public void testParameterHandlerProvider() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        ParameterHandler h = new CustomerParameterHandler();
        pf.registerUserProvider(h);
        ParameterHandler h2 = pf.createParameterHandler(Customer.class);
        assertSame(h2, h);
    }
    
    @Test
    public void testSortEntityProvidersWithConfig() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        JSONProvider json1 = new JSONProvider();
        json1.setConsumeMediaTypes(Collections.singletonList("application/json;q=0.9"));
        pf.registerUserProvider(json1);
        JSONProvider json2 = new JSONProvider();
        json2.setConsumeMediaTypes(Collections.singletonList("application/json"));
        json2.setProduceMediaTypes(Collections.singletonList("application/sbc;q=0.9"));
        pf.registerUserProvider(json2);
        
        List<ProviderInfo<MessageBodyReader>> readers = pf.getMessageReaders();

        assertTrue(indexOf(readers, json2) 
                   < indexOf(readers, json1));
        
        List<ProviderInfo<MessageBodyWriter>> writers = pf.getMessageWriters();

        assertTrue(indexOf(writers, json1) 
                   < indexOf(writers, json2));
        
    }
    
    @Test
    public void testGetStringProvider() throws Exception {
        verifyProvider(String.class, PrimitiveTextProvider.class, "text/plain");
    }
    
    @Test
    public void testGetBinaryProvider() throws Exception {
        verifyProvider(byte[].class, BinaryDataProvider.class, "*/*");
        verifyProvider(InputStream.class, BinaryDataProvider.class, "image/png");
        MessageBodyWriter writer = ProviderFactory.getInstance()
            .createMessageBodyWriter(File.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE, 
                                     new MessageImpl());
        assertTrue(BinaryDataProvider.class == writer.getClass());
    }
    
    private void verifyProvider(ProviderFactory pf, Class<?> type, Class<?> provider, String mediaType) 
        throws Exception {
        
        if (pf == null) {
            pf = ProviderFactory.getInstance();
        }
        
        MediaType mType = MediaType.valueOf(mediaType);
        
        MessageBodyReader reader = pf.createMessageBodyReader(type, type, null, mType, new MessageImpl());
        assertSame("Unexpected provider found", provider, reader.getClass());
    
        MessageBodyWriter writer = pf.createMessageBodyWriter(type, type, null, mType, new MessageImpl());
        assertTrue("Unexpected provider found", provider == writer.getClass());
    }
    
    
    private void verifyProvider(Class<?> type, Class<?> provider, String mediaType) 
        throws Exception {
        verifyProvider(null, type, provider, mediaType);
        
    }
       
    @Test
    public void testGetStringProviderWildCard() throws Exception {
        verifyProvider(String.class, PrimitiveTextProvider.class, "text/*");
    }
    
    @Test
    public void testGetAtomProvider() throws Exception {
        ProviderFactory factory = ProviderFactory.getInstance();
        factory.setUserProviders(
             Arrays.asList(
                  new Object[]{new AtomEntryProvider(), new AtomFeedProvider()}));
        verifyProvider(factory, Entry.class, AtomEntryProvider.class, "application/atom+xml");
        verifyProvider(factory, Feed.class, AtomFeedProvider.class, "application/atom+xml");
    }
    
    @Test
    public void testGetStringProviderUsingProviderDeclaration() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(new TestStringProvider());
        verifyProvider(pf, String.class, TestStringProvider.class, "text/html");
    }    
    
    @Test
    public void testGetJSONProviderConsumeMime() throws Exception {
        verifyProvider(org.apache.cxf.jaxrs.resources.Book.class, JSONProvider.class, 
                       "application/json");
    }
    
    @Test
    public void testRegisterCustomJSONEntityProvider() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(new CustomJSONProvider());
        verifyProvider(pf, Book.class, CustomJSONProvider.class, 
                       "application/json");
    }
    
    
    @Test
    public void testRegisterCustomResolver() throws Exception {
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(new JAXBContextProvider());
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, new MessageImpl());
        assertTrue("JAXBContext ContextProvider can not be found", 
                   cr instanceof JAXBContextProvider);
        
    }
    
    @Test
    public void testRegisterCustomEntityProvider() throws Exception {
        ProviderFactory pf = (ProviderFactory)ProviderFactory.getInstance();
        pf.registerUserProvider(new CustomWidgetProvider());
        
        verifyProvider(pf, org.apache.cxf.jaxrs.resources.Book.class, CustomWidgetProvider.class, 
                       "application/widget");
    }
    
    private int indexOf(List<? extends Object> providerInfos, Class providerType) {
        int index = 0;
        for (Object pi : providerInfos) {
            Object p = ((ProviderInfo)pi).getProvider();
            if (p.getClass().isAssignableFrom(providerType)) {
                break;
            }
            index++;
        }
        return index;
    }
    
    private int indexOf(List<? extends Object> providerInfos, Object provider) {
        int index = 0;
        for (Object pi : providerInfos) {
            if (((ProviderInfo)pi).getProvider() == provider) {
                break;
            }
            index++;
        }
        return index;
    }
    
    @Consumes("text/html")
    @Produces("text/html")
    private final class TestStringProvider 
        implements MessageBodyReader<String>, MessageBodyWriter<String>  {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType m) {
            return type == String.class;
        }
        
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                                   MediaType m) {
            return type == String.class;
        }
        
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, 
                            MediaType m) {
            return s.length();
        }

        public String readFrom(Class<String> clazz, Type genericType, Annotation[] annotations, 
                               MediaType m, MultivaluedMap<String, String> headers, InputStream is) {
            try {
                return IOUtils.toString(is);
            } catch (IOException e) {
                // TODO: better exception handling
            }
            return null;
        }

        public void writeTo(String obj, Class<?> clazz, Type genericType, Annotation[] annotations,  
            MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) {
            try {
                os.write(obj.getBytes());
            } catch (IOException e) {
                // TODO: better exception handling
            }
        }

    }
    
    @Consumes("application/json")
    @Produces("application/json")
    private final class CustomJSONProvider 
        implements MessageBodyReader<Book>, MessageBodyWriter<Book>  {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType m) {
            return type.getAnnotation(XmlRootElement.class) != null;
        }
        
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                                   MediaType m) {
            return type.getAnnotation(XmlRootElement.class) != null;
        }
        
        public long getSize(Book b, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType m) {
            return -1;
        }

        public Book readFrom(Class<Book> clazz, Type genericType, Annotation[] annotations, 
                               MediaType m, MultivaluedMap<String, String> headers, InputStream is) {    
            //Dummy
            return null;
        }

        public void writeTo(Book obj, Class<?> clazz, Type genericType, Annotation[] annotations,  
            MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) {
            //Dummy
        }

    }
    
    @Consumes("application/widget")
    @Produces("application/widget")
    private final class CustomWidgetProvider
        implements MessageBodyReader<Book>, MessageBodyWriter<Book>  {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType m) {
            return type.getAnnotation(XmlRootElement.class) != null;
        }
        
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                                   MediaType m) {
            return type.getAnnotation(XmlRootElement.class) != null;
        }
        
        public long getSize(Book s, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType m) {
            return -1;
        }


        public Book readFrom(Class<Book> clazz, Type genericType, Annotation[] annotations, 
                               MediaType m, MultivaluedMap<String, String> headers, InputStream is) {    
            //Dummy
            return null;
        }

        public void writeTo(Book obj, Class<?> clazz, Type genericType, Annotation[] annotations,  
            MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) {
            //Dummy
        }

    }
    
    @Test
    public void testSetSchemasFromClasspath() {
        JAXBElementProvider provider = new JAXBElementProvider();
        ProviderFactory pf = ProviderFactory.getInstance();
        pf.registerUserProvider(provider);
        
        List<String> locations = new ArrayList<String>();
        locations.add("classpath:/test.xsd");
        pf.setSchemaLocations(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from classpath", s);
    }
    
    private static class TestRuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

        public Response toResponse(RuntimeException exception) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    @Produces("application/xml")
    @Consumes("application/xml")
    private static class BookReaderWriter 
        implements MessageBodyReader<Book>, MessageBodyWriter<Book> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, 
                                  MediaType mediaType) {
            return true;
        }

        public Book readFrom(Class<Book> arg0, Type arg1, Annotation[] arg2, 
                             MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5) 
            throws IOException, WebApplicationException {
            // TODO Auto-generated method stub
            return null;
        }

        public long getSize(Book t, Class<?> type, Type genericType, Annotation[] annotations, 
                            MediaType mediaType) {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, 
                                   MediaType mediaType) {
            return true;
        }

        public void writeTo(Book arg0, Class<?> arg1, Type arg2, Annotation[] arg3, 
                            MediaType arg4, MultivaluedMap<String, Object> arg5, OutputStream arg6) 
            throws IOException, WebApplicationException {
            // TODO Auto-generated method stub
            
        }
    }
    
    @Produces("application/xml")
    @Consumes("application/xml")
    private static class SuperBookReaderWriter 
        implements MessageBodyReader<SuperBook>, MessageBodyWriter<SuperBook> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, 
                                  MediaType mediaType) {
            return true;
        }

        public SuperBook readFrom(Class<SuperBook> arg0, Type arg1, Annotation[] arg2, MediaType arg3, 
                                  MultivaluedMap<String, String> arg4, InputStream arg5) 
            throws IOException, WebApplicationException {
            // TODO Auto-generated method stub
            return null;
        }

        public long getSize(SuperBook t, Class<?> type, Type genericType, 
                            Annotation[] annotations, MediaType mediaType) {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isWriteable(Class<?> type, Type genericType, 
                                   Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        public void writeTo(SuperBook arg0, Class<?> arg1, Type arg2, 
                            Annotation[] arg3, MediaType arg4, MultivaluedMap<String, Object> arg5, 
                            OutputStream arg6) throws IOException, WebApplicationException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    @Produces("application/xml")
    @Consumes("application/xml")
    private static class SuperBookReaderWriter2 implements MessageBodyReader, MessageBodyWriter {

        public boolean isReadable(Class type, Type genericType, Annotation[] annotations, 
                                  MediaType mediaType) {
            return true;
        }

        public Object readFrom(Class arg0, Type arg1, Annotation[] arg2, MediaType arg3, 
                                  MultivaluedMap arg4, InputStream arg5) 
            throws IOException, WebApplicationException {
            // TODO Auto-generated method stub
            return null;
        }

        public long getSize(Object t, Class type, Type genericType, 
                            Annotation[] annotations, MediaType mediaType) {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isWriteable(Class type, Type genericType, 
                                   Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        
        public void writeTo(Object arg0, Class arg1, Type arg2, 
                            Annotation[] arg3, MediaType arg4, MultivaluedMap arg5, 
                            OutputStream arg6) throws IOException, WebApplicationException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private static class TestHandler implements RequestHandler {

        public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
            return null;
        }
        
    }
    
    @Ignore
    private class TestRunnable implements Runnable {

        private CountDownLatch startSignal;
        private CountDownLatch doneSignal;
        private ProviderFactory pf;
        private String property; 
        public TestRunnable(ProviderFactory pf,
                            CountDownLatch startSignal,
                            CountDownLatch doneSignal,
                            String property) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
            this.pf = pf;
            this.property = property;
        }
        
        public void run() {
            
            try {
                startSignal.await();
                ProviderFactoryTest.this.doTestDefaultJaxbProviderCloned(pf, property);
                doneSignal.countDown();
            } catch (Exception ex) {
                ex.printStackTrace();
                Assert.fail(ex.getMessage());
            } 
            
        }
        
    }
    
    private static class RuntimeExceptionMapper1 
        extends AbstractTestExceptionMapper<RuntimeException> {
        
    }
    
    private static class RuntimeExceptionMapper2 
        extends AbstractTestExceptionMapper<WebApplicationException> {
        
    }
    
    private static class AbstractTestExceptionMapper<T extends RuntimeException> 
        implements ExceptionMapper<T> {

        @Override
        public Response toResponse(T arg0) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
