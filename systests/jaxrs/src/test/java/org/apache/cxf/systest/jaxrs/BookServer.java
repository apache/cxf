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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
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
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.StreamingResponseProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.jaxrs.BookStore.BookNotReturnedException;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

public class BookServer extends AbstractServerTestServerBase {
    public static final String PORT = allocatePort(BookServer.class);

    private Map< ? extends String, ? extends Object > properties;

    public BookServer() {
        this(Collections.emptyMap());
    }

    /**
     * Allow to specified custom contextual properties to be passed to factory bean
     */
    public BookServer(final Map< ? extends String, ? extends Object > properties) {
        this.properties = properties;
    }

    @Override
    protected Server createServer(Bus bus) throws Exception {
        bus.setProperty(ExceptionMapper.class.getName(), new BusMapperExceptionMapper());
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);

        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStore.class, SimpleBookStore.class, BookStorePerRequest.class, BookStoreRegex.class);

        //default lifecycle is per-request, change it to singleton
        final BinaryDataProvider<Object> p = new BinaryDataProvider<>();
        p.setProduceMediaTypes(Collections.singletonList("application/bar"));
        p.setEnableBuffering(true);
        p.setReportByteArraySize(true);
        final JAXBElementProvider<?> jaxbProvider = new JAXBElementProvider<>();
        jaxbProvider.setJaxbElementClassMap(
            Collections.singletonMap(BookNoXmlRootElement.class.getName(), "BookNoXmlRootElement"));
        sf.setProviders(Arrays.asList(
            p,
            new BookStore.PrimitiveIntArrayReaderWriter(),
            new BookStore.PrimitiveDoubleArrayReaderWriter(),
            new BookStore.StringArrayBodyReaderWriter(),
            new BookStore.StringListBodyReaderWriter(),
            new StreamingResponseProvider<Object>(),
            new ContentTypeModifyingMBW(),
            jaxbProvider,
            new FormatResponseHandler(),
            new GenericHandlerWriter(),
            new FaultyRequestHandler(),
            new SearchContextProvider(),
            new QueryContextProvider(),
            new BlockingRequestFilter(),
            new FaultyResponseFilter(),
            new BlockedExceptionMapper(),
            new ParamConverterImpl()
        ));
        sf.setInInterceptors(Arrays.asList(
            new CustomInFaultyInterceptor(),
            new LoggingInInterceptor()
        ));
        sf.setOutInterceptors(Arrays.asList(
            new CustomOutInterceptor(),
            new LoggingOutInterceptor()
        ));
        sf.setOutFaultInterceptors(Arrays.asList(
            new CustomOutFaultInterceptor()
        ));
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setAddress("http://localhost:" + PORT + "/");

        sf.getProperties(true).put("org.apache.cxf.jaxrs.mediaTypeCheck.strict", true);
        sf.getProperties().put("search.visitor", new SQLPrinterVisitor<SearchBean>("books"));
        sf.getProperties().put("org.apache.cxf.http.header.split", true);
        sf.getProperties().put("default.content.type", "*/*");
        sf.getProperties().putAll(properties);
        return sf.create();
    }

    public static void main(String[] args) throws Exception {
        new BookServer().start();
    }

    private static final class BusMapperExceptionMapper implements ExceptionMapper<BusMapperException> {

        public Response toResponse(BusMapperException exception) {
            return Response.serverError().type("text/plain;charset=utf-8").header("BusMapper", "the-mapper")
                .entity("BusMapperException").build();
        }

    }
    @PreMatching
    private static final class BlockingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getPath().endsWith("/blockAndThrowException")) {
                requestContext.setProperty("blocked", Boolean.TRUE);
                requestContext.abortWith(Response.ok().build());
            }
        }

    }
    private static final class FaultyResponseFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
            if (PropertyUtils.isTrue(requestContext.getProperty("blocked"))) {
                throw new BlockedException();
            }
        }

    }
    private static final class BlockedExceptionMapper implements ExceptionMapper<BlockedException> {

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
            }
            return null;
        }

    }

    public static class NotFoundExceptionMapper implements ResponseExceptionMapper<BookNotFoundFault> {

        public BookNotFoundFault fromResponse(Response r) {
            String status = r.getHeaderString("Status");
            if ("notFound".equals(status)) {
                return new BookNotFoundFault(status);
            }
            return null;
        }

    }
    public static class TestResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
            throws IOException {
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
        private static final class ByteConverter implements ParamConverter<Byte> {
            @Override
            public Byte fromString(String t) {
                return Byte.valueOf(t);
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
