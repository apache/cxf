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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.validation.Schema;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.CustomerParameterHandler;
import org.apache.cxf.jaxrs.JAXBContextProvider;
import org.apache.cxf.jaxrs.JAXBContextProvider2;
import org.apache.cxf.jaxrs.PriorityCustomerParameterHandler;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProviderFactoryTest {


    @Before
    public void setUp() {
        ServerProviderFactory.getInstance().clearProviders();
        AbstractResourceInfo.clearAllMaps();
    }

    @Test
    public void testMultipleFactories() {
        assertNotSame(ServerProviderFactory.getInstance(), ServerProviderFactory.getInstance());
    }

    @Test
    public void testRegisterInFeature() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        final Object provider = new WebApplicationExceptionMapper();
        pf.registerUserProvider((Feature) context -> {
            context.register(provider);
            return true;
        });
        ExceptionMapper<WebApplicationException> em =
            pf.createExceptionMapper(WebApplicationException.class, new MessageImpl());
        assertSame(provider, em);
    }

    @Test
    public void testRegisterFeatureInFeature() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        final Object provider = new WebApplicationExceptionMapper();
        pf.registerUserProvider((Feature) context -> {
            context.register((Feature) context2-> {
                context2.register(provider);
                return true;
            });
            return true;
        });
        ExceptionMapper<WebApplicationException> em =
            pf.createExceptionMapper(WebApplicationException.class, new MessageImpl());
        assertSame(provider, em);
    }

    @Test
    public void testRegisterMbrMbwProviderAsMbrOnly() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        JAXBElementProvider<Book> customProvider = new JAXBElementProvider<>();
        pf.registerUserProvider((Feature) context -> {
            context.register(customProvider, MessageBodyReader.class);
            return true;
        });
        MessageBodyReader<Book> reader = pf.createMessageBodyReader(Book.class, null, null,
                                                                    MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(reader, customProvider);

        MessageBodyWriter<Book> writer = pf.createMessageBodyWriter(Book.class, null, null,
                                                                    MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(writer instanceof JAXBElementProvider);
        assertNotSame(writer, customProvider);
    }

    @Test
    public void testRegisterMbrMbwProviderAsMbwOnly() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        JAXBElementProvider<Book> customProvider = new JAXBElementProvider<>();
        pf.registerUserProvider((Feature) context -> {
            context.register(customProvider, MessageBodyWriter.class);
            return true;
        });
        MessageBodyWriter<Book> writer = pf.createMessageBodyWriter(Book.class, null, null,
                                                                    MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(writer, customProvider);

        MessageBodyReader<Book> reader = pf.createMessageBodyReader(Book.class, null, null,
                                                                    MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(reader instanceof JAXBElementProvider);
        assertNotSame(reader, customProvider);
    }

    @Test
    public void testOrderOfProvidersWithSameProperties() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        WildcardReader reader1 = new WildcardReader();
        pf.registerUserProvider(reader1);
        WildcardReader2 reader2 = new WildcardReader2();
        pf.registerUserProvider(reader2);
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(11, readers.size());
        assertSame(reader1, readers.get(7).getProvider());
        assertSame(reader2, readers.get(8).getProvider());
    }

    @Test
    public void testCustomProviderSorting() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        Comparator<?> comp = new Comparator<ProviderInfo<?>>() {

            @Override
            public int compare(ProviderInfo<?> o1, ProviderInfo<?> o2) {
                Object provider1 = o1.getProvider();
                Object provider2 = o2.getProvider();
                if (provider1 instanceof StringTextProvider) {
                    return 1;
                } else if (provider2 instanceof StringTextProvider) {
                    return -1;
                } else {
                    return 0;
                }
            }

        };
        pf.setProviderComparator(comp);

        // writers
        List<ProviderInfo<MessageBodyWriter<?>>> writers = pf.getMessageWriters();
        assertEquals(10, writers.size());
        Object lastWriter = writers.get(9).getProvider();
        assertTrue(lastWriter instanceof StringTextProvider);
        //readers
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(9, readers.size());
        Object lastReader = readers.get(8).getProvider();
        assertTrue(lastReader instanceof StringTextProvider);
    }
    @Test
    public void testCustomProviderSorting2() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        Comparator<Object> comp = new Comparator<Object>() {

            @Override
            public int compare(Object provider1, Object provider2) {
                if (provider1 instanceof StringTextProvider) {
                    return 1;
                } else if (provider2 instanceof StringTextProvider) {
                    return -1;
                } else {
                    return 0;
                }
            }

        };
        pf.setProviderComparator(comp);

        // writers
        List<ProviderInfo<MessageBodyWriter<?>>> writers = pf.getMessageWriters();
        assertEquals(10, writers.size());
        Object lastWriter = writers.get(9).getProvider();
        assertTrue(lastWriter instanceof StringTextProvider);
        //readers
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(9, readers.size());
        Object lastReader = readers.get(8).getProvider();
        assertTrue(lastReader instanceof StringTextProvider);
    }
    @SuppressWarnings("rawtypes")
    @Test
    public void testCustomProviderSortingMBROnly() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        Comparator<ProviderInfo<MessageBodyReader>> comp =
            new Comparator<ProviderInfo<MessageBodyReader>>() {

            @Override
            public int compare(ProviderInfo<MessageBodyReader> o1, ProviderInfo<MessageBodyReader> o2) {
                MessageBodyReader<?> provider1 = o1.getProvider();
                MessageBodyReader<?> provider2 = o2.getProvider();
                if (provider1 instanceof StringTextProvider) {
                    return 1;
                } else if (provider2 instanceof StringTextProvider) {
                    return -1;
                } else {
                    return 0;
                }
            }

        };
        pf.setProviderComparator(comp);

        // writers
        List<ProviderInfo<MessageBodyWriter<?>>> writers = pf.getMessageWriters();
        assertEquals(10, writers.size());
        Object lastWriter = writers.get(8).getProvider();
        assertFalse(lastWriter instanceof StringTextProvider);
        //readers
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(9, readers.size());
        Object lastReader = readers.get(8).getProvider();
        assertTrue(lastReader instanceof StringTextProvider);
    }
    @Test
    public void testCustomProviderSortingMBWOnly() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        Comparator<ProviderInfo<MessageBodyWriter<?>>> comp =
            new Comparator<ProviderInfo<MessageBodyWriter<?>>>() {

            @Override
            public int compare(ProviderInfo<MessageBodyWriter<?>> o1, ProviderInfo<MessageBodyWriter<?>> o2) {
                MessageBodyWriter<?> provider1 = o1.getProvider();
                MessageBodyWriter<?> provider2 = o2.getProvider();
                if (provider1 instanceof StringTextProvider) {
                    return 1;
                } else if (provider2 instanceof StringTextProvider) {
                    return -1;
                } else {
                    return 0;
                }
            }

        };
        pf.setProviderComparator(comp);

        // writers
        List<ProviderInfo<MessageBodyWriter<?>>> writers = pf.getMessageWriters();
        assertEquals(10, writers.size());
        Object lastWriter = writers.get(9).getProvider();
        assertTrue(lastWriter instanceof StringTextProvider);
        //readers
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(9, readers.size());
        Object lastReader = readers.get(8).getProvider();
        assertFalse(lastReader instanceof StringTextProvider);
    }

    @Test
    public void testCustomProviderSortingWIOnly() {
        ProviderFactory pf = ServerProviderFactory.getInstance();

        pf.setUserProviders(
            Arrays.asList(
                new DWriterInterceptor(), new CWriterInterceptor(),
                new AWriterInterceptor(), new BWriterInterceptor()));

        Comparator<ProviderInfo<WriterInterceptor>> comp =
            new Comparator<ProviderInfo<WriterInterceptor>>() {

                @Override
                public int compare(
                    ProviderInfo<WriterInterceptor> o1,
                    ProviderInfo<WriterInterceptor> o2) {

                    WriterInterceptor provider1 = o1.getProvider();
                    WriterInterceptor provider2 = o2.getProvider();

                    return provider1.getClass().getName().compareTo(
                        provider2.getClass().getName());
                }

            };

        pf.setProviderComparator(comp);

        Collection<ProviderInfo<WriterInterceptor>> values =
            pf.writerInterceptors.values();

        assertEquals(4, values.size());

        Iterator<ProviderInfo<WriterInterceptor>> iterator = values.iterator();

        assertEquals(AWriterInterceptor.class, iterator.next().getProvider().getClass());
        assertEquals(BWriterInterceptor.class, iterator.next().getProvider().getClass());
        assertEquals(CWriterInterceptor.class, iterator.next().getProvider().getClass());
        assertEquals(DWriterInterceptor.class, iterator.next().getProvider().getClass());
    }

    private static final class AWriterInterceptor implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws
            IOException, WebApplicationException {

        }
    }

    private static final class BWriterInterceptor implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws
            IOException, WebApplicationException {

        }
    }

    private static final class CWriterInterceptor implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws
            IOException, WebApplicationException {

        }
    }

    private static final class DWriterInterceptor implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws
            IOException, WebApplicationException {

        }
    }

    @Test
    public void testCustomProviderSortingWithBus() {
        WildcardReader wc1 = new WildcardReader();
        WildcardReader2 wc2 = new WildcardReader2();
        Bus bus = BusFactory.newInstance().createBus();
        bus.setProperty(MessageBodyReader.class.getName(), wc1);
        ProviderFactory pf = ServerProviderFactory.createInstance(bus);
        pf.registerUserProvider(wc2);
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(11, readers.size());
        assertSame(wc2, readers.get(7).getProvider());
        assertSame(wc1, readers.get(8).getProvider());
    }
    @Test
    public void testCustomProviderSortingWithBus2() {
        WildcardReader wc1 = new WildcardReader();
        WildcardReader2 wc2 = new WildcardReader2();
        Bus bus = BusFactory.newInstance().createBus();
        bus.setProperty(MessageBodyReader.class.getName(), wc2);
        ProviderFactory pf = ServerProviderFactory.createInstance(bus);
        pf.registerUserProvider(wc1);
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(11, readers.size());
        assertSame(wc1, readers.get(7).getProvider());
        assertSame(wc2, readers.get(8).getProvider());
    }

    @Test
    public void testCustomJaxbProvider() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        JAXBElementProvider<Book> provider = new JAXBElementProvider<>();
        pf.registerUserProvider(provider);
        MessageBodyReader<Book> customJaxbReader = pf.createMessageBodyReader(Book.class, null, null,
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(customJaxbReader, provider);

        MessageBodyWriter<Book> customJaxbWriter = pf.createMessageBodyWriter(Book.class, null, null,
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertSame(customJaxbWriter, provider);
    }
    
    @Test
    public void testCustomProviderAndJaxbProvider() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        CustomJaxbProvider provider = new CustomJaxbProvider();
        pf.registerUserProvider(provider);
        
        MessageBodyReader<JAXBElement> customJaxbReader = pf.createMessageBodyReader(JAXBElement.class, 
            String.class, null, MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertThat(customJaxbReader, instanceOf(JAXBElementTypedProvider.class));

        MessageBodyWriter<JAXBElement> customJaxbWriter = pf.createMessageBodyWriter(JAXBElement.class, 
            String.class, null, MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertThat(customJaxbWriter, instanceOf(JAXBElementTypedProvider.class));
    }

    @Test
    public void testDataSourceReader() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
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
        ProviderFactory pf = ServerProviderFactory.getInstance();
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
    public void testSchemaLocations() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        MessageBodyReader<Book> jaxbReader = pf.createMessageBodyReader(Book.class, null, null,
                                                              MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(jaxbReader instanceof JAXBElementProvider);
        pf.setSchemaLocations(Collections.singletonList("classpath:/test.xsd"));
        MessageBodyReader<Book> customJaxbReader = pf.createMessageBodyReader(
            Book.class, null, null, MediaType.TEXT_XML_TYPE, new MessageImpl());
        assertTrue(jaxbReader instanceof JAXBElementProvider);
        assertSame(jaxbReader, customJaxbReader);
        assertNotNull(((JAXBElementProvider<Book>)customJaxbReader).getSchema());
    }

    @Test
    public void testGetFactoryInboundMessage() {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.get(ServerProviderFactory.class.getName())).thenReturn(factory);
        e.put(Endpoint.class, endpoint);
        assertSame(ProviderFactory.getInstance(m), factory);
    }

    @Test
    public void testDefaultUserExceptionMappers() throws Exception {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
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
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
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
        Message m = new MessageImpl();
        m.put("default.wae.mapper.least.specific", false);
        ServerProviderFactory pf = ServerProviderFactory.getInstance();

        TestRuntimeExceptionMapper rm = new TestRuntimeExceptionMapper();
        pf.registerUserProvider(rm);
        ExceptionMapper<WebApplicationException> em =
            pf.createExceptionMapper(WebApplicationException.class, m);
        assertTrue(em instanceof WebApplicationExceptionMapper);
        assertSame(rm, pf.createExceptionMapper(RuntimeException.class, m));

        WebApplicationExceptionMapper wm = new WebApplicationExceptionMapper();
        pf.registerUserProvider(wm);
        assertSame(wm, pf.createExceptionMapper(WebApplicationException.class, new MessageImpl()));
        assertSame(rm, pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
    }

    @Test
    public void testExceptionMappersHierarchy3() throws Exception {
        Message m = new MessageImpl();
        ServerProviderFactory pf = ServerProviderFactory.getInstance();

        TestRuntimeExceptionMapper rm = new TestRuntimeExceptionMapper();
        pf.registerUserProvider(rm);
        ExceptionMapper<WebApplicationException> em =
            pf.createExceptionMapper(WebApplicationException.class, m);
        assertSame(rm, em);
        assertSame(rm, pf.createExceptionMapper(RuntimeException.class, m));

        WebApplicationExceptionMapper wm = new WebApplicationExceptionMapper();
        pf.registerUserProvider(wm);
        assertSame(wm, pf.createExceptionMapper(WebApplicationException.class, m));
        assertSame(rm, pf.createExceptionMapper(RuntimeException.class, m));
    }
    @Test
    public void testExceptionMappersHierarchy4() throws Exception {
        Message m = new MessageImpl();
        m.put("default.wae.mapper.least.specific", true);
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        ExceptionMapper<WebApplicationException> em =
            pf.createExceptionMapper(WebApplicationException.class, m);
        assertTrue(em instanceof WebApplicationExceptionMapper);
    }
    @Test
    public void testExceptionMappersHierarchy5() throws Exception {
        Message m = new MessageImpl();
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        ExceptionMapper<WebApplicationException> em =
            pf.createExceptionMapper(WebApplicationException.class, m);
        assertTrue(em instanceof WebApplicationExceptionMapper);
    }

    @Test
    public void testExceptionMappersHierarchyWithGenerics() throws Exception {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        RuntimeExceptionMapper1 exMapper1 = new RuntimeExceptionMapper1();
        pf.registerUserProvider(exMapper1);
        RuntimeExceptionMapper2 exMapper2 = new RuntimeExceptionMapper2();
        pf.registerUserProvider(exMapper2);
        assertSame(exMapper1, pf.createExceptionMapper(RuntimeException.class, new MessageImpl()));
        Object webExMapper = pf.createExceptionMapper(WebApplicationException.class, new MessageImpl());
        assertSame(exMapper2, webExMapper);
    }

    @Test
    public void testMessageBodyReaderString() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        MessageBodyReader<String> mbr = pf.createMessageBodyReader(String.class, String.class, new Annotation[]{},
                                   MediaType.APPLICATION_XML_TYPE, new MessageImpl());
        assertTrue(mbr instanceof StringTextProvider);
    }
    @Test
    public void testMessageBodyReaderBoolean() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new CustomBooleanReader());
        MessageBodyReader<Boolean> mbr = pf.createMessageBodyReader(Boolean.class, Boolean.class, new Annotation[]{},
                                   MediaType.TEXT_PLAIN_TYPE, new MessageImpl());
        assertTrue(mbr instanceof PrimitiveTextProvider);
    }
    @Test
    public void testMessageBodyReaderBoolean2() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new CustomBooleanReader2());
        MessageBodyReader<Boolean> mbr = pf.createMessageBodyReader(Boolean.class, Boolean.class, new Annotation[]{},
                                   MediaType.TEXT_PLAIN_TYPE, new MessageImpl());
        assertTrue(mbr instanceof CustomBooleanReader2);
    }
    @Test
    public void testMessageBodyWriterString() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        MessageBodyWriter<String> mbr = pf.createMessageBodyWriter(String.class, String.class, new Annotation[]{},
                                   MediaType.APPLICATION_XML_TYPE, new MessageImpl());
        assertTrue(mbr instanceof StringTextProvider);
    }

    @Test
    public void testMessageBodyHandlerHierarchy() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
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
        ProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
        SuperBookReaderWriter2<SuperBook> superBookHandler = new SuperBookReaderWriter2<>();
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
        ProviderFactory pf = ServerProviderFactory.getInstance();
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
        ProviderFactory pf = ServerProviderFactory.getInstance();
        ParamConverterProvider h = new CustomerParameterHandler();
        pf.registerUserProvider(h);
        ParamConverter<Customer> h2 = pf.createParameterHandler(Customer.class, Customer.class, null,
                                                                new MessageImpl());
        assertSame(h2, h);
    }
    
    @Test
    public void testParameterHandlerProviderWithPriority() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        ParamConverterProvider h = new CustomerParameterHandler();
        ParamConverterProvider hp = new PriorityCustomerParameterHandler();
        pf.registerUserProvider(h);
        pf.registerUserProvider(hp);
        ParamConverter<Customer> h2 = pf.createParameterHandler(Customer.class, Customer.class, null,
                                                                new MessageImpl());
        assertSame(h2, hp);
    }

    @Test
    public void testCustomProviderSortingParamConverterProvider() {
        ParamConverterProvider h = new CustomerParameterHandler();
        ParamConverterProvider hp = new PriorityCustomerParameterHandler();
        
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.setUserProviders(Arrays.asList(h, hp));

        Comparator<ProviderInfo<ParamConverterProvider>> comp =
            new Comparator<ProviderInfo<ParamConverterProvider>>() {

                @Override
                public int compare(
                    ProviderInfo<ParamConverterProvider> o1,
                    ProviderInfo<ParamConverterProvider> o2) {

                    ParamConverterProvider provider1 = o1.getProvider();
                    ParamConverterProvider provider2 = o2.getProvider();

                    return provider1.getClass().getName().compareTo(
                        provider2.getClass().getName());
                }

            };

        pf.setProviderComparator(comp);

        ParamConverter<Customer> h2 = pf.createParameterHandler(Customer.class, Customer.class, null,
                new MessageImpl());
        assertSame(h2, h);
    }
    
    @Test
    public void testGetStringProvider() throws Exception {
        verifyProvider(String.class, StringTextProvider.class, "text/plain");
    }

    @Test
    public void testGetBinaryProvider() throws Exception {
        verifyProvider(byte[].class, BinaryDataProvider.class, "*/*");
        verifyProvider(InputStream.class, BinaryDataProvider.class, "image/png");
        MessageBodyWriter<File> writer = ServerProviderFactory.getInstance()
            .createMessageBodyWriter(File.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                                     new MessageImpl());
        assertTrue(BinaryDataProvider.class == writer.getClass());
    }

    @Test
    public void testGetComplexProvider() throws Exception {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
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
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new ComplexMessageBodyReader());
        MessageBodyReader<Book> reader =
            pf.createMessageBodyReader(Book.class, Book.class, null, MediaType.APPLICATION_JSON_TYPE,
                                       new MessageImpl());
        assertTrue(ComplexMessageBodyReader.class == reader.getClass());
    }

    private void verifyProvider(ProviderFactory pf, Class<?> type, Class<?> provider, String mediaType)
        throws Exception {

        if (pf == null) {
            pf = ServerProviderFactory.getInstance();
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
        verifyProvider(String.class, StringTextProvider.class, "text/*");
    }


    @Test
    public void testGetStringProviderUsingProviderDeclaration() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new TestStringProvider());
        verifyProvider(pf, String.class, TestStringProvider.class, "text/html");
    }

    @Test
    public void testRegisterCustomJSONEntityProvider() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new CustomJSONProvider());
        verifyProvider(pf, Book.class, CustomJSONProvider.class,
                       "application/json");
    }

    @Test
    public void testComplexExceptionMapper() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
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
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new JAXBContextProvider());
        Message message = prepareMessage("*/*", null);
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, message);
        assertFalse(cr instanceof ProviderFactory.ContextResolverProxy);
        assertTrue("JAXBContext ContextProvider can not be found",
                   cr instanceof JAXBContextProvider);

    }

    @Test
    public void testRegisterCustomResolver2() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
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
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new JAXBContextProvider());
        pf.registerUserProvider(new JAXBContextProvider2());
        Message message = prepareMessage("text/xml+c", null);
        ContextResolver<JAXBContext> cr = pf.createContextResolver(JAXBContext.class, message);
        assertNull(cr);
    }

    @Test
    public void testCustomResolverProxy() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
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
        Map<String, List<String>> headers = new MetadataMap<>();
        message.put(Message.PROTOCOL_HEADERS, headers);
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);
        if (acceptType != null) {
            headers.put("Accept", Collections.singletonList(acceptType));
            exchange.setOutMessage(new MessageImpl());
        } else {
            headers.put("Content-Type", Collections.singletonList(contentType));
        }
        message.put("Content-Type", contentType);
        message.setExchange(exchange);
        return message;
    }

    private Message prepareFaultMessage(String contentType, String acceptType) {
        Message message = new MessageImpl();
        Map<String, List<String>> headers = new MetadataMap<String, String>();
        message.put(Message.PROTOCOL_HEADERS, headers);
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(null);
        exchange.setInFaultMessage(message);
        if (acceptType != null) {
            headers.put("Accept", Collections.singletonList(acceptType));
            exchange.setOutMessage(new MessageImpl());
        } else {
            headers.put("Content-Type", Collections.singletonList(contentType));
        }
        message.put("Content-Type", contentType);
        message.setExchange(exchange);
        return message;
    }

    @Test
    public void testRegisterCustomEntityProvider() throws Exception {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new CustomWidgetProvider());

        verifyProvider(pf, org.apache.cxf.jaxrs.resources.Book.class, CustomWidgetProvider.class,
                       "application/widget");
    }

    @Test
    public void testCreateMessageBodyReaderInterceptor() {
        ServerProviderFactory spf = ServerProviderFactory.getInstance();
        final Message message = prepareMessage(MediaType.APPLICATION_XML, MediaType.APPLICATION_XML);

        List<ReaderInterceptor> interceptors =
            spf.createMessageBodyReaderInterceptor(Book.class, Book.class,
                                                   new Annotation[0], MediaType.APPLICATION_XML_TYPE,
                                                   message, true, null);
        assertSame(1, interceptors.size());
    }

    @Test
    public void testCreateMessageBodyReaderInterceptorWithFaultMessage() throws Exception {
        ServerProviderFactory spf = ServerProviderFactory.getInstance();
        final Message message = prepareFaultMessage(MediaType.APPLICATION_XML, MediaType.APPLICATION_XML);

        List<ReaderInterceptor> interceptors =
            spf.createMessageBodyReaderInterceptor(Book.class, Book.class,
                                                   new Annotation[0], MediaType.APPLICATION_XML_TYPE,
                                                   message, true, null);
        assertSame(1, interceptors.size());
    }

    @Test
    public void testCreateMessageBodyReaderInterceptorWithReaderInterceptor() throws Exception {
        ReaderInterceptor ri = readerInterceptorContext -> readerInterceptorContext.proceed();
        ProviderInfo<ReaderInterceptor> pi = new ProviderInfo<>(ri, null, true);

        ServerProviderFactory spf = ServerProviderFactory.getInstance();
        spf.readerInterceptors.put(new ProviderFactory.NameKey("org.apache.cxf.filter.binding", 1, ri.getClass()), pi);

        final Message message = prepareMessage(MediaType.APPLICATION_XML, MediaType.APPLICATION_XML);

        List<ReaderInterceptor> interceptors =
            spf.createMessageBodyReaderInterceptor(Book.class, Book.class,
                                                   new Annotation[0], MediaType.APPLICATION_XML_TYPE,
                                                   message, true, null);
        assertSame(2, interceptors.size());
    }

    @Test
    public void testCreateMessageBodyReaderInterceptorWithFaultMessageAndReaderInterceptor() throws Exception {
        ReaderInterceptor ri = readerInterceptorContext -> readerInterceptorContext.proceed();
        ProviderInfo<ReaderInterceptor> pi = new ProviderInfo<>(ri, null, true);

        ServerProviderFactory spf = ServerProviderFactory.getInstance();
        spf.readerInterceptors.put(new ProviderFactory.NameKey("org.apache.cxf.filter.binding", 1, ri.getClass()), pi);

        final Message message = prepareFaultMessage(MediaType.APPLICATION_XML, MediaType.APPLICATION_XML);
        List<ReaderInterceptor> interceptors =
            spf.createMessageBodyReaderInterceptor(Book.class, Book.class,
                                                   new Annotation[0], MediaType.APPLICATION_XML_TYPE,
                                                   message, true, null);
        assertSame(2, interceptors.size());
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
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        ProviderFactory pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(provider);

        List<String> locations = new ArrayList<>();
        locations.add("classpath:/test.xsd");
        pf.setSchemaLocations(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from classpath", s);
    }

    @Test
    public void testSortByPriority() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
        BookReaderWriter bookHandler = new BookReaderWriter();
        providers.add(bookHandler);
        HighPriorityBookReaderWriter highPriorityBookHandler = new HighPriorityBookReaderWriter();
        providers.add(highPriorityBookHandler);
        pf.setUserProviders(providers);
        assertSame(highPriorityBookHandler,
                   pf.createMessageBodyReader(Book.class, Book.class, new Annotation[]{},
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
        assertSame(highPriorityBookHandler,
                   pf.createMessageBodyWriter(Book.class, Book.class, new Annotation[]{},
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
    }

    @Test
    public void testSortByPriorityReversed() {
        // do the same thing again but add the providers in the reverse order to ensure add order
        // isn't responsible for our success so far.
        ProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
        HighPriorityBookReaderWriter highPriorityBookHandler = new HighPriorityBookReaderWriter();
        providers.add(highPriorityBookHandler);
        BookReaderWriter bookHandler = new BookReaderWriter();
        providers.add(bookHandler);
        pf.setUserProviders(providers);
        assertSame(highPriorityBookHandler,
                   pf.createMessageBodyReader(Book.class, Book.class, new Annotation[]{},
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
        assertSame(highPriorityBookHandler,
                   pf.createMessageBodyWriter(Book.class, Book.class, new Annotation[]{},
                                              MediaType.APPLICATION_XML_TYPE, new MessageImpl()));
    }

    @Test
    public void testSortContextResolverByPriority() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
        LowPriorityContextResolver lowResolver = new LowPriorityContextResolver();
        providers.add(lowResolver);
        HighPriorityContextResolver highResolver = new HighPriorityContextResolver();
        providers.add(highResolver);
        pf.setUserProviders(providers);
        Message m = new MessageImpl();
        assertEquals(highResolver.getContext(null),
                   pf.createContextResolver(String.class, m, MediaType.TEXT_PLAIN_TYPE).getContext(null));
    }

    @Test
    public void testSortContextResolverByPriorityReversed() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
        HighPriorityContextResolver highResolver = new HighPriorityContextResolver();
        providers.add(highResolver);
        LowPriorityContextResolver lowResolver = new LowPriorityContextResolver();
        providers.add(lowResolver);
        pf.setUserProviders(providers);
        Message m = new MessageImpl();
        assertEquals(highResolver.getContext(null),
                   pf.createContextResolver(String.class, m, MediaType.TEXT_PLAIN_TYPE).getContext(null));
    }

    @Test
    public void testSortExceptionMapperByPriority() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
        LowPriorityExceptionMapper lowMapper = new LowPriorityExceptionMapper();
        providers.add(lowMapper);
        HighPriorityExceptionMapper highMapper = new HighPriorityExceptionMapper();
        providers.add(highMapper);
        pf.setUserProviders(providers);
        Message m = new MessageImpl();
        assertEquals(Response.ok().build().getStatus(),
                     pf.createExceptionMapper(RuntimeException.class, m).toResponse(null).getStatus());
    }

    @Test
    public void testSortExceptionMapperByPriorityReversed() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        List<Object> providers = new ArrayList<>();
        HighPriorityExceptionMapper highMapper = new HighPriorityExceptionMapper();
        providers.add(highMapper);
        LowPriorityExceptionMapper lowMapper = new LowPriorityExceptionMapper();
        providers.add(lowMapper);
        pf.setUserProviders(providers);
        Message m = new MessageImpl();
        assertEquals(Response.ok().build().getStatus(),
                   pf.createExceptionMapper(RuntimeException.class, m).toResponse(null).getStatus());
    }

    @Priority(1001)
    private static final class HighPriorityExceptionMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            return Response.ok().build();
        }
    }

    @Priority(2001)
    private static final class LowPriorityExceptionMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            return Response.noContent().build();
        }
    }

    private static final class TestRuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

        public Response toResponse(RuntimeException exception) {
            return null;
        }

    }

    @Priority(100)
    private static final class LowPriorityContextResolver implements ContextResolver<String> {

        @Override
        public String getContext(Class<?> paramClass) {
            return "low";
        }
    }

    @Priority(1)
    private static final class HighPriorityContextResolver implements ContextResolver<String> {

        @Override
        public String getContext(Class<?> paramClass) {
            return "high";
        }
    }

    @Priority(Priorities.USER - 10)
    @Produces("application/xml")
    @Consumes("application/xml")
    private static final class HighPriorityBookReaderWriter
        implements MessageBodyReader<Book>, MessageBodyWriter<Book> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType mediaType) {
            return true;
        }

        public Book readFrom(Class<Book> arg0, Type arg1, Annotation[] arg2,
                             MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return null;
        }

        public long getSize(Book t, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType mediaType) {
            return 0;
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                                   MediaType mediaType) {
            return true;
        }

        public void writeTo(Book arg0, Class<?> arg1, Type arg2, Annotation[] arg3,
                            MediaType arg4, MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {

        }
    }

    @Produces("application/xml")
    @Consumes("application/xml")
    private static final class BookReaderWriter
        implements MessageBodyReader<Book>, MessageBodyWriter<Book> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType mediaType) {
            return true;
        }

        public Book readFrom(Class<Book> arg0, Type arg1, Annotation[] arg2,
                             MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return null;
        }

        public long getSize(Book t, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType mediaType) {
            return 0;
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                                   MediaType mediaType) {
            return true;
        }

        public void writeTo(Book arg0, Class<?> arg1, Type arg2, Annotation[] arg3,
                            MediaType arg4, MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {

        }
    }

    @Produces("application/xml")
    @Consumes("application/xml")
    private static final class SuperBookReaderWriter
        implements MessageBodyReader<SuperBook>, MessageBodyWriter<SuperBook> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType mediaType) {
            return true;
        }

        public SuperBook readFrom(Class<SuperBook> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
                                  MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return null;
        }

        public long getSize(SuperBook t, Class<?> type, Type genericType,
                            Annotation[] annotations, MediaType mediaType) {
            return 0;
        }

        public boolean isWriteable(Class<?> type, Type genericType,
                                   Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        public void writeTo(SuperBook arg0, Class<?> arg1, Type arg2,
                            Annotation[] arg3, MediaType arg4, MultivaluedMap<String, Object> arg5,
                            OutputStream arg6) throws IOException, WebApplicationException {

        }

    }

    @Produces("application/xml")
    @Consumes("application/xml")
    private static final class SuperBookReaderWriter2<T>
        implements MessageBodyReader<T>, MessageBodyWriter<T> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType mediaType) {
            return true;
        }

        public T readFrom(Class<T> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
                          MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return null;
        }

        public long getSize(T t, Class<?> type, Type genericType,
                            Annotation[] annotations, MediaType mediaType) {
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
        }

    }

    @Produces("*/*")
    @Consumes("*/*")
    private static class WildcardReader implements MessageBodyReader<Object> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType mediaType) {
            return true;
        }

        public Object readFrom(Class<Object> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
                                  MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return null;
        }

    }
    @Produces("*/*")
    @Consumes("*/*")
    private static final class WildcardReader2 implements MessageBodyReader<Object> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                                  MediaType mediaType) {
            return true;
        }

        public Object readFrom(Class<Object> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
                                  MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return null;
        }

    }

    private static final class RuntimeExceptionMapper1
        extends AbstractTestExceptionMapper<RuntimeException> {

    }

    private static final class RuntimeExceptionMapper2
        extends AbstractTestExceptionMapper<WebApplicationException> {

    }

    private static class AbstractTestExceptionMapper<T extends RuntimeException>
        implements ExceptionMapper<T> {

        public Response toResponse(T arg0) {
            return null;
        }

    }

    private static final class ComplexMessageBodyReader extends ProviderBase<AClass> {
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
            return null;
        }

        @Override
        public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return 0;
        }

        @Override
        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return false;
        }

        @Override
        public void writeTo(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {

        }
    }
    public static class AClass {
    }

    private static final class SecurityExceptionMapper
        extends AbstractBadRequestExceptionMapper<SecurityException> {
    }
    private static final class CustomWebApplicationExceptionMapper
        extends AbstractBadRequestExceptionMapper<WebApplicationException> {
    }
    private abstract static class AbstractBadRequestExceptionMapper<T extends Throwable>
        implements ExceptionMapper<T> {
        @Override
        public Response toResponse(T exception) {
            return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }
    }
    @Test
    public void testWebApplicationMapperWithGenerics() throws Exception {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        CustomWebApplicationExceptionMapper mapper = new CustomWebApplicationExceptionMapper();
        pf.registerUserProvider(mapper);
        Object mapperResponse = pf.createExceptionMapper(WebApplicationException.class, new MessageImpl());
        assertSame(mapperResponse, mapper);
    }

    @Test
    public void testBadCustomExceptionMappersHierarchyWithGenerics() throws Exception {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
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
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
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

    @Test
    public void testProvidersWithConstraints() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        
        @ConstrainedTo(RuntimeType.SERVER)
        class ServerWildcardReader extends WildcardReader {
            
        }
        
        @ConstrainedTo(RuntimeType.CLIENT)
        class ClientWildcardReader extends WildcardReader {
            
        }

        final ServerWildcardReader reader = new ServerWildcardReader();
        pf.registerUserProvider(reader);
        
        List<ProviderInfo<MessageBodyReader<?>>> readers = pf.getMessageReaders();
        assertEquals(10, readers.size());
        assertSame(reader, readers.get(7).getProvider());

        pf.registerUserProvider(new ClientWildcardReader());
        assertEquals(10, pf.getMessageReaders().size());
    }

    private static class RuntimeExceptionA extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    private static final class RuntimeExceptionAA extends RuntimeExceptionA {
        private static final long serialVersionUID = 1L;
    }
    private static class RuntimeExceptionB extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    private static final class RuntimeExceptionBB extends RuntimeExceptionB {
        private static final long serialVersionUID = 1L;
    }
    private static final class GoodRuntimeExceptionAMapper implements ExceptionMapper<RuntimeExceptionA> {

        @Override
        public Response toResponse(RuntimeExceptionA exception) {
            return null;
        }
    }
    private static final class GoodRuntimeExceptionBMapper implements ExceptionMapper<RuntimeExceptionB> {

        @Override
        public Response toResponse(RuntimeExceptionB exception) {
            return null;
        }
    }
    public abstract static class BadParentExceptionMapper<T extends Throwable> implements ExceptionMapper<T> { // NOPMD
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
    @Consumes("text/plain")
    public static class CustomBooleanReader2 extends CustomBooleanReader {

    }
    public static class CustomBooleanReader implements MessageBodyReader<Boolean> {
        @Override
        public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
            return type == Boolean.class;
        }
        @Override
        public Boolean readFrom(Class<Boolean> type,
                                 Type type1,
                                 Annotation[] antns,
                                 MediaType mt, MultivaluedMap<String, String> mm,
                                 InputStream in) throws IOException, WebApplicationException {
            return Boolean.TRUE;
        }
    }

}