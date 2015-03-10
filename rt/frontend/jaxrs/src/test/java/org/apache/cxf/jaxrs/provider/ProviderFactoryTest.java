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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.validation.Schema;

import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.CustomerParameterHandler;
import org.apache.cxf.jaxrs.JAXBContextProvider;
import org.apache.cxf.jaxrs.JAXBContextProvider2;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.ProviderFactory.LegacyParamConverter;
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
        AbstractResourceInfo.clearAllMaps();
    }
    
    @Test
    public void testMultipleFactories() {
        assertNotSame(ProviderFactory.createInstance(BusFactory.newInstance().createBus()), 
                      ProviderFactory.getSharedInstance());
        assertSame(ProviderFactory.getSharedInstance(), ProviderFactory.getSharedInstance());
        assertNotSame(ProviderFactory.createInstance(BusFactory.newInstance().createBus()), 
                      ProviderFactory.createInstance(BusFactory.newInstance().createBus()));
    }
    
    @Test
    public void testCustomWadlHandler() {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        MessageBodyReader<Book> customJaxbReader = pf.createMessageBodyReader(Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, message);
        assertTrue(customJaxbReader instanceof JAXBElementProvider);
        
        JAXBElementProvider<Book> provider = (JAXBElementProvider<Book>)customJaxbReader;
        MessageContext mc = provider.getContext();
        assertNotNull(mc);
        UriInfo ui = mc.getUriInfo();
        MultivaluedMap<String, String> queries = ui.getQueryParameters();
        assertEquals(1, queries.size());
        List<String> uriQuery = queries.get("uri");
        assertEquals(1, uriQuery.size());
        assertEquals(property, uriQuery.get(0));
        
        MessageBodyReader<?> customJaxbReader2 = pf.createMessageBodyReader((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, message);
        assertSame(customJaxbReader, customJaxbReader2);
         
        MessageBodyWriter<?> customJaxbWriter = pf.createMessageBodyWriter((Class<?>)Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, message);
        assertSame(customJaxbReader, customJaxbWriter);
        
        MessageBodyReader<?> jaxbReader = ProviderFactory.getSharedInstance().createMessageBodyReader(
            (Class<?>)Book.class, null, null, MediaType.TEXT_XML_TYPE, message);
        assertTrue(jaxbReader instanceof JAXBElementProvider);
        assertNotSame(jaxbReader, customJaxbReader);
    }
    
    private void checkJaxbProvider(ProviderFactory pf) {
        int count = 0;
        for (Object provider : pf.getReadersWriters()) {
            if (((ProviderInfo<?>)provider).getProvider() instanceof JAXBElementProvider) {
                count++;
            }
        }
        assertEquals(1, count);
    }
    
    @Test
    public void testCustomJaxbProvider() {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        JAXBElementProvider<Book> provider = new JAXBElementProvider<Book>();
        pf.registerUserProvider(provider);
        MessageBodyReader<Book> customJaxbReader = pf.createMessageBodyReader(Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(customJaxbReader, provider);
        
        MessageBodyWriter<Book> customJaxbWriter = pf.createMessageBodyWriter(Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(customJaxbWriter, provider);
    }
    
    @Test
    public void testDataSourceReader() {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new DataSourceProvider<Object>());
        MessageBodyReader<DataSource> reader = pf.createMessageBodyReader(
              DataSource.class, null, null, 
              MediaType.valueOf("image/png"), new MessageImpl());
        assertTrue(reader instanceof DataSourceProvider);
        MessageBodyReader<DataHandler> reader2 = pf.createMessageBodyReader(
                          DataHandler.class, null, null, 
                          MediaType.valueOf("image/png"), new MessageImpl());
        assertSame(reader, reader2);
    }
    
    @Test
    public void testDataSourceWriter() {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new DataSourceProvider<Object>());
        MessageBodyWriter<DataSource> writer = pf.createMessageBodyWriter(
              DataSource.class, null, null, 
              MediaType.valueOf("image/png"), new MessageImpl());
        assertTrue(writer instanceof DataSourceProvider);
        MessageBodyWriter<DataHandler> writer2 = pf.createMessageBodyWriter(
                          DataHandler.class, null, null, 
                          MediaType.valueOf("image/png"), new MessageImpl());
        assertSame(writer, writer2);
    }
    
    @Test
    public void testNoDataSourceWriter() {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new DataSourceProvider<Object>());
        MessageBodyWriter<DataSource> writer = pf.createMessageBodyWriter(
              DataSource.class, null, null, 
              MediaType.valueOf("multipart/form-data"), new MessageImpl());
        assertFalse(writer instanceof DataSourceProvider);
    }
    
    
    @Test
    public void testSchemaLocations() {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.setSchemaLocations(Collections.singletonList("classpath:/test.xsd"));
        MessageBodyReader<Book> customJaxbReader = pf.createMessageBodyReader(Book.class, null, null, 
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(customJaxbReader instanceof JAXBElementProvider);
        MessageBodyReader<Book> jaxbReader = ProviderFactory.getSharedInstance().createMessageBodyReader(
            Book.class, null, null, MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(jaxbReader instanceof JAXBElementProvider);
        assertNotSame(jaxbReader, customJaxbReader);
        
        assertNull(((JAXBElementProvider<Book>)jaxbReader).getSchema());
        assertNotNull(((JAXBElementProvider<Book>)customJaxbReader).getSchema());
    }
    
    @Test
    public void testGetFactoryInboundMessage() {
        ProviderFactory factory = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        
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
    public void testExceptionMappersHierarchyWithGenerics() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        RuntimeExceptionMapper1 exMapper1 = new RuntimeExceptionMapper1(); 
        pf.registerUserProvider(exMapper1);
        RuntimeExceptionMapper2 exMapper2 = new RuntimeExceptionMapper2(); 
        pf.registerUserProvider(exMapper2);
        assertSame(exMapper1, pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
        Object webExMapper = pf.createExceptionMapper(WebApplicationException.class, new MessageImpl());
        assertSame(exMapper2, webExMapper);
    }
    
    @Test
    public void testMessageBodyHandlerHierarchy() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        List<Object> providers = new ArrayList<Object>();
        SuperBookReaderWriter2<SuperBook> superBookHandler = new SuperBookReaderWriter2<SuperBook>();
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
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new TestStringProvider());
        pf.registerUserProvider(new PrimitiveTextProvider<Object>());
        
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();

        assertTrue(indexOf(readers, TestStringProvider.class) 
                   < indexOf(readers, PrimitiveTextProvider.class));
        
        List<ProviderInfo<MessageBodyWriter<?>>> writers = pf.getMessageWriters();

        assertTrue(indexOf(writers, TestStringProvider.class) 
                   < indexOf(writers, PrimitiveTextProvider.class));
        
    }
    
    @Test
    public void testParameterHandlerProvider() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        ParameterHandler<Customer> h = new CustomerParameterHandler();
        pf.registerUserProvider(h);
        ParamConverter<Customer> h2 = pf.createParameterHandler(Customer.class, null);
        
        assertSame(((LegacyParamConverter<Customer>)h2).getHandler(), h);
    }
    
    @Test
    public void testGetStringProvider() throws Exception {
        verifyProvider(String.class, PrimitiveTextProvider.class, "text/plain");
    }
    
    @Test
    public void testGetBinaryProvider() throws Exception {
        verifyProvider(byte[].class, BinaryDataProvider.class, "*/*");
        verifyProvider(InputStream.class, BinaryDataProvider.class, "image/png");
        MessageBodyWriter<File> writer = ProviderFactory.createInstance(BusFactory.newInstance().createBus())
            .createMessageBodyWriter(File.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE, 
                                     new MessageImpl());
        assertTrue(BinaryDataProvider.class == writer.getClass());
    }
    
    @Test
    public void testGetComplexProvider() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new ComplexMessageBodyReader());
        Message m = new MessageImpl();
        Exchange ex = new ExchangeImpl();
        m.setExchange(ex);
        m.put(ProviderFactory.IGNORE_TYPE_VARIABLES, true);
        MessageBodyReader<Book> reader =
            pf.createMessageBodyReader(Book.class, Book.class, null, MediaType.APPLICATION_JSON_TYPE, 
                                       m);
        assertTrue(ComplexMessageBodyReader.class == reader.getClass());
    }
    
    @Test
    public void testGetComplexProvider2() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new ComplexMessageBodyReader());
        MessageBodyReader<Book> reader =
            pf.createMessageBodyReader(Book.class, Book.class, null, MediaType.APPLICATION_JSON_TYPE, 
                                       new MessageImpl());
        assertTrue(ComplexMessageBodyReader.class == reader.getClass());
    }
    
    private void verifyProvider(ProviderFactory pf, Class<?> type, Class<?> provider, String mediaType) 
        throws Exception {
        
        if (pf == null) {
            pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        }
        
        MediaType mType = MediaType.valueOf(mediaType);
        
        MessageBodyReader<?> reader = pf.createMessageBodyReader(type, type, null, mType, new MessageImpl());
        assertSame("Unexpected provider found", provider, reader.getClass());
    
        MessageBodyWriter<?> writer = pf.createMessageBodyWriter(type, type, null, mType, new MessageImpl());
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
    public void testGetStringProviderUsingProviderDeclaration() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new TestStringProvider());
        verifyProvider(pf, String.class, TestStringProvider.class, "text/html");
    }    
    
    @Test
    public void testRegisterCustomJSONEntityProvider() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new CustomJSONProvider());
        verifyProvider(pf, Book.class, CustomJSONProvider.class, 
                       "application/json");
    }
    
    @Test
    public void testComplexExceptionMapper() {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new SecurityExceptionMapper());
        ExceptionMapper<SecurityException> mapper = 
            pf.createExceptionMapper(SecurityException.class, new MessageImpl());
        assertTrue(mapper instanceof SecurityExceptionMapper);
        ExceptionMapper<Throwable> mapper2 = 
            pf.createExceptionMapper(Throwable.class, new MessageImpl());
        assertNull(mapper2);
    }
    
    @Test
    public void testRegisterCustomResolver() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new JAXBContextProvider());
        Message message = prepareMessage("*/*", null);
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, message);
        assertFalse(cr instanceof ProviderFactory.ContextResolverProxy);
        assertTrue("JAXBContext ContextProvider can not be found", 
                   cr instanceof JAXBContextProvider);
        
    }
    
    @Test
    public void testRegisterCustomResolver2() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new JAXBContextProvider());
        pf.registerUserProvider(new JAXBContextProvider2());
        Message message = prepareMessage("text/xml+b", null);
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, message);
        assertFalse(cr instanceof ProviderFactory.ContextResolverProxy);
        assertTrue("JAXBContext ContextProvider can not be found", 
                   cr instanceof JAXBContextProvider2);
        
    }
    
    @Test
    public void testNoCustomResolver() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new JAXBContextProvider());
        pf.registerUserProvider(new JAXBContextProvider2());
        Message message = prepareMessage("text/xml+c", null);
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, message);
        assertNull(cr);
    }
    
    @Test
    public void testCustomResolverOut() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new JAXBContextProvider());
        pf.registerUserProvider(new JAXBContextProvider2());
        Message message = prepareMessage("text/xml+c", "text/xml+a");
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, message);
        assertFalse(cr instanceof ProviderFactory.ContextResolverProxy);
        assertTrue("JAXBContext ContextProvider can not be found", 
                   cr instanceof JAXBContextProvider);
    }
    
    @Test
    public void testCustomResolverProxy() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new JAXBContextProvider());
        pf.registerUserProvider(new JAXBContextProvider2());
        Message message = prepareMessage("text/xml+*", null);
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, message);
        assertTrue(cr instanceof ProviderFactory.ContextResolverProxy);
        assertTrue(((ProviderFactory.ContextResolverProxy<?>)cr).getResolvers().get(0) 
                   instanceof JAXBContextProvider);
        assertTrue(((ProviderFactory.ContextResolverProxy<?>)cr).getResolvers().get(1) 
                   instanceof JAXBContextProvider2);
    }
    
    private Message prepareMessage(String contentType, String acceptType) {
        Message message = new MessageImpl();
        Map<String, List<String>> headers = new MetadataMap<String, String>();
        message.put(Message.PROTOCOL_HEADERS, headers);
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);
        if (acceptType != null) {
            headers.put("Accept", Collections.singletonList(acceptType));
            exchange.setOutMessage(new MessageImpl());
        } else {
            headers.put("Content-Type", Collections.singletonList(contentType));
        }
        message.setExchange(exchange);
        return message;
    }
    
    @Test
    public void testRegisterCustomEntityProvider() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        pf.registerUserProvider(new CustomWidgetProvider());
        
        verifyProvider(pf, org.apache.cxf.jaxrs.resources.Book.class, CustomWidgetProvider.class, 
                       "application/widget");
    }
    
    private int indexOf(List<? extends Object> providerInfos, Class<?> providerType) {
        int index = 0;
        for (Object pi : providerInfos) {
            Object p = ((ProviderInfo<?>)pi).getProvider();
            if (p.getClass().isAssignableFrom(providerType)) {
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
        JAXBElementProvider<?> provider = new JAXBElementProvider<Object>();
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
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
    private static class SuperBookReaderWriter2<T> 
        implements MessageBodyReader<T>, MessageBodyWriter<T> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, 
                                  MediaType mediaType) {
            return true;
        }

        public T readFrom(Class<T> arg0, Type arg1, Annotation[] arg2, MediaType arg3, 
                          MultivaluedMap<String, String> arg4, InputStream arg5) 
            throws IOException, WebApplicationException {
            // TODO Auto-generated method stub
            return null;
        }

        public long getSize(T t, Class<?> type, Type genericType, 
                            Annotation[] annotations, MediaType mediaType) {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isWriteable(Class<?> type, Type genericType, 
                                   Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        
        public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream)
            throws IOException, WebApplicationException {
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

        public Response toResponse(T arg0) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
        
    private static class ComplexMessageBodyReader extends ProviderBase<AClass> {
    }
    private abstract static class ProviderBase<A> implements
        MessageBodyReader<Object>, MessageBodyWriter<Object> {
        @Override
        public boolean isReadable(Class<?> cls, Type arg1, Annotation[] arg2, MediaType arg3) {
            return true;
        }

        @Override
        public Object readFrom(Class<Object> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
                               MultivaluedMap<String, String> arg4, InputStream arg5) throws IOException,
            WebApplicationException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void writeTo(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            // TODO Auto-generated method stub
            
        }      
    }
    public static class AClass {
    }
    
    private static class SecurityExceptionMapper 
        extends AbstractBadRequestExceptionMapper<SecurityException> {
    }
    private abstract static class AbstractBadRequestExceptionMapper<T extends Throwable> 
        implements ExceptionMapper<T> {
        @Override
        public Response toResponse(T exception) {
            return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }
    }
    
    @Test
    public void testBadCustomExceptionMappersHierarchyWithGenerics() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        BadExceptionMapperA badExceptionMapperA = new BadExceptionMapperA();
        pf.registerUserProvider(badExceptionMapperA);
        BadExceptionMapperB badExceptionMapperB = new BadExceptionMapperB();
        pf.registerUserProvider(badExceptionMapperB);
        Object mapperResponse1 = pf.createExceptionMapper(RuntimeExceptionA.class, new MessageImpl());
        assertSame(badExceptionMapperA, mapperResponse1);
        Object mapperResponse2 = pf.createExceptionMapper(RuntimeExceptionB.class, new MessageImpl());
        assertSame(badExceptionMapperB, mapperResponse2);
        Object mapperResponse3 = pf.createExceptionMapper(RuntimeExceptionAA.class, new MessageImpl());
        assertSame(badExceptionMapperA, mapperResponse3);
        Object mapperResponse4 = pf.createExceptionMapper(RuntimeExceptionBB.class, new MessageImpl());
        assertSame(badExceptionMapperB, mapperResponse4);
    }

    @Test
    public void testGoodExceptionMappersHierarchyWithGenerics() throws Exception {
        ProviderFactory pf = ProviderFactory.createInstance(BusFactory.newInstance().createBus());
        GoodRuntimeExceptionAMapper runtimeExceptionAMapper = new GoodRuntimeExceptionAMapper();
        pf.registerUserProvider(runtimeExceptionAMapper);
        GoodRuntimeExceptionBMapper runtimeExceptionBMapper = new GoodRuntimeExceptionBMapper();
        pf.registerUserProvider(runtimeExceptionBMapper);
        Object mapperResponse1 = pf.createExceptionMapper(RuntimeExceptionA.class, new MessageImpl());
        assertSame(runtimeExceptionAMapper, mapperResponse1);
        Object mapperResponse2 = pf.createExceptionMapper(RuntimeExceptionB.class, new MessageImpl());
        assertSame(runtimeExceptionBMapper, mapperResponse2);
        Object mapperResponse3 = pf.createExceptionMapper(RuntimeExceptionAA.class, new MessageImpl());
        assertSame(runtimeExceptionAMapper, mapperResponse3);
        Object mapperResponse4 = pf.createExceptionMapper(RuntimeExceptionBB.class, new MessageImpl());
        assertSame(runtimeExceptionBMapper, mapperResponse4);
    }
    private static class RuntimeExceptionA extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    private static class RuntimeExceptionAA extends RuntimeExceptionA {
        private static final long serialVersionUID = 1L;
    }
    private static class RuntimeExceptionB extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    private static class RuntimeExceptionBB extends RuntimeExceptionB {
        private static final long serialVersionUID = 1L;
    }
    private static class GoodRuntimeExceptionAMapper implements ExceptionMapper<RuntimeExceptionA> {

        @Override
        public Response toResponse(RuntimeExceptionA exception) {
            return null;
        }
    }
    private static class GoodRuntimeExceptionBMapper implements ExceptionMapper<RuntimeExceptionB> {

        @Override
        public Response toResponse(RuntimeExceptionB exception) {
            return null;
        }
    }
    public abstract static class BadParentExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {
    }
    public static class BadExceptionMapperA extends BadParentExceptionMapper<RuntimeExceptionA> {

        @Override
        public Response toResponse(RuntimeExceptionA exception) {
            return null;
        }
    }
    public static class BadExceptionMapperB extends BadParentExceptionMapper<RuntimeExceptionB> {

        @Override
        public Response toResponse(RuntimeExceptionB exception) {
            return null;
        }
    }
}
