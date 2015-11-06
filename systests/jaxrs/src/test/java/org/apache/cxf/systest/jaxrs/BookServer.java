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

package org.apache.cxf.systest.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.ext.search.QueryContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.ext.search.sql.SQLPrinterVisitor;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.BinaryDataProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.provider.StreamingResponseProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.jaxrs.BookStore.BookNotReturnedException;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
    
public class BookServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(BookServer.class);
     
    org.apache.cxf.endpoint.Server server;
    private Map< ? extends String, ? extends Object > properties;
    
    public BookServer() {
        this(Collections.< String, Object >emptyMap());
    }
    
    /**
     * Allow to specified custom contextual properties to be passed to factory bean
     */
    public BookServer(final Map< ? extends String, ? extends Object > properties) {
        this.properties = properties;
    }
    
    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        bus.setProperty(ExceptionMapper.class.getName(), new BusMapperExceptionMapper());
        setBus(bus);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStore.class, SimpleBookStore.class, BookStorePerRequest.class);
        List<Object> providers = new ArrayList<Object>();
        
        //default lifecycle is per-request, change it to singleton
        BinaryDataProvider<Object> p = new BinaryDataProvider<Object>();
        p.setProduceMediaTypes(Collections.singletonList("application/bar"));
        p.setEnableBuffering(true);
        p.setReportByteArraySize(true);
        providers.add(p);
        providers.add(new BookStore.PrimitiveIntArrayReaderWriter());
        providers.add(new BookStore.PrimitiveDoubleArrayReaderWriter());
        providers.add(new BookStore.StringArrayBodyReaderWriter());
        providers.add(new BookStore.StringListBodyReaderWriter());
        providers.add(new StreamingResponseProvider<Object>());
        providers.add(new ContentTypeModifyingMBW());
        JAXBElementProvider<?> jaxbProvider = new JAXBElementProvider<Object>();
        Map<String, String> jaxbElementClassMap = new HashMap<String, String>(); 
        jaxbElementClassMap.put(BookNoXmlRootElement.class.getName(), "BookNoXmlRootElement");
        jaxbProvider.setJaxbElementClassMap(jaxbElementClassMap);
        providers.add(jaxbProvider);
        providers.add(new FormatResponseHandler());
        providers.add(new GenericHandlerWriter());
        providers.add(new FaultyRequestHandler());
        providers.add(new SearchContextProvider());
        providers.add(new QueryContextProvider());
        providers.add(new BlockingRequestFilter());
        providers.add(new FaultyResponseFilter());
        providers.add(new BlockedExceptionMapper());
        providers.add(new ParamConverterImpl());
        sf.setProviders(providers);
        List<Interceptor<? extends Message>> inInts = new ArrayList<Interceptor<? extends Message>>();
        inInts.add(new CustomInFaultyInterceptor());
        inInts.add(new LoggingInInterceptor());
        
        sf.setInInterceptors(inInts);
        List<Interceptor<? extends Message>> outInts = new ArrayList<Interceptor<? extends Message>>();
        outInts.add(new CustomOutInterceptor());
        sf.setOutInterceptors(outInts);
        List<Interceptor<? extends Message>> outFaultInts = new ArrayList<Interceptor<? extends Message>>();
        outFaultInts.add(new CustomOutFaultInterceptor());
        sf.setOutFaultInterceptors(outFaultInts);
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setAddress("http://localhost:" + PORT + "/");

        sf.getProperties(true).put("org.apache.cxf.jaxrs.mediaTypeCheck.strict", true);
        sf.getProperties().put("search.visitor", new SQLPrinterVisitor<SearchBean>("books"));
        sf.getProperties().put("org.apache.cxf.http.header.split", true);
        sf.getProperties().put("default.content.type", "*/*");
        sf.getProperties().putAll(properties);
        server = sf.create();
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
    }
    
    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }

    public static void main(String[] args) {
        try {
            BookServer s = new BookServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
    
    private static class BusMapperExceptionMapper implements ExceptionMapper<BusMapperException> {

        public Response toResponse(BusMapperException exception) {
            return Response.serverError().type("text/plain;charset=utf-8").header("BusMapper", "the-mapper")
                .entity("BusMapperException").build();
        }
        
    }
    @PreMatching
    private static class BlockingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getPath().endsWith("/blockAndThrowException")) {
                requestContext.setProperty("blocked", Boolean.TRUE);
                requestContext.abortWith(Response.ok().build());
            }
        }
        
    }
    private static class FaultyResponseFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
            if (PropertyUtils.isTrue(requestContext.getProperty("blocked"))) {
                throw new BlockedException();
            }
        }
        
    }
    private static class BlockedExceptionMapper implements ExceptionMapper<BlockedException> {

        @Override
        public Response toResponse(BlockedException exception) {
            return Response.ok().build();
        }
        
        
    }
    @SuppressWarnings("serial")
    public static class BlockedException extends RuntimeException {
        
    }
    
    public static class ReplaceContentTypeInterceptor extends AbstractPhaseInterceptor<Message> {
        public ReplaceContentTypeInterceptor() {
            super(Phase.READ);
        }

        public void handleMessage(Message message) throws Fault {
            Map<String, List<String>> headers = 
                CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            headers.put(Message.CONTENT_TYPE, Collections.singletonList("text/plain"));
        }
    }

    public static class ReplaceStatusInterceptor extends AbstractPhaseInterceptor<Message> {
        public ReplaceStatusInterceptor() {
            super(Phase.READ);
        }

        public void handleMessage(Message message) throws Fault {
            message.getExchange().put(Message.RESPONSE_CODE, 200);
        }
    }
    
    public static class NotReturnedExceptionMapper implements ResponseExceptionMapper<BookNotReturnedException> {

        public BookNotReturnedException fromResponse(Response r) {
            String status = r.getHeaderString("Status");
            if ("notReturned".equals(status)) { 
                return new BookNotReturnedException(status);
            } else {
                return null;
            }
        }
        
    }
    
    public static class NotFoundExceptionMapper implements ResponseExceptionMapper<BookNotFoundFault> {

        public BookNotFoundFault fromResponse(Response r) {
            String status = r.getHeaderString("Status");
            if ("notFound".equals(status)) { 
                return new BookNotFoundFault(status);
            } else {
                return null;
            }
        }
        
    }
    public static class TestResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
            throws IOException {
            // TODO Auto-generated method stub
            
        }
        
    }
    public static class ParamConverterImpl implements ParamConverterProvider {

        @Context
        private Providers providers;
        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
                                                  Annotation[] annotations) {
            if (rawType == Book.class) {
                
                MessageBodyReader<Book> mbr = providers.getMessageBodyReader(Book.class, 
                                                                             Book.class, 
                                                                             annotations, 
                                                                             MediaType.APPLICATION_XML_TYPE);
                MessageBodyWriter<Book> mbw = providers.getMessageBodyWriter(Book.class, 
                                                                             Book.class, 
                                                                             annotations, 
                                                                             MediaType.APPLICATION_XML_TYPE);
                return (ParamConverter<T>)new XmlParamConverter(mbr, mbw);
            } else if (rawType == byte.class) {
                return (ParamConverter<T>)new ByteConverter();
            } else {
                return null;
            }
            
        }
        private static class ByteConverter implements ParamConverter<Byte> {
            @Override
            public Byte fromString(String t) {
                return new Byte(t); 
            }

            @Override
            public String toString(Byte b) {
                return b.toString();
            }
        }
        private static class XmlParamConverter implements ParamConverter<Book> {
            private MessageBodyReader<Book> mbr;
            private MessageBodyWriter<Book> mbw;
            XmlParamConverter(MessageBodyReader<Book> mbr, MessageBodyWriter<Book> mbw) {  
                this.mbr = mbr;
                this.mbw = mbw;
            }
            @Override
            public Book fromString(String value) {
                try {
                    return mbr.readFrom(Book.class, Book.class, 
                                        new Annotation[]{}, 
                                        MediaType.APPLICATION_XML_TYPE, 
                                        new MetadataMap<String, String>(), 
                                        new ByteArrayInputStream(value.getBytes()));
                } catch (IOException ex) {
                    throw new BadRequestException(ex);
                }
            }
            @Override
            public String toString(Book value) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    mbw.writeTo(value, Book.class, Book.class, 
                                new Annotation[]{}, 
                                MediaType.APPLICATION_XML_TYPE, 
                                new MetadataMap<String, Object>(),
                                bos);
                } catch (IOException ex) {
                    throw new BadRequestException(ex);
                }
                return bos.toString();
            }
        }
    }
}
