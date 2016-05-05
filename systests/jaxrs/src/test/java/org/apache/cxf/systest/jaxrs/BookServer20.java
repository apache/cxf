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
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NameBinding;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
    
public class BookServer20 extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(BookServer20.class);
 
    org.apache.cxf.endpoint.Server server; 
    
    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        setBus(bus);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStore.class);
        
        List<Object> providers = new ArrayList<Object>();
        
        providers.add(new PreMatchContainerRequestFilter2());
        providers.add(new PreMatchContainerRequestFilter());
        providers.add(new PostMatchContainerResponseFilter());
        providers.add(new PostMatchContainerResponseFilter3());
        providers.add(new PostMatchContainerResponseFilter2());
        providers.add(new CustomReaderBoundInterceptor());
        providers.add(new CustomReaderInterceptor());
        providers.add(new CustomWriterInterceptor());
        providers.add(new CustomDynamicFeature());
        providers.add(new PostMatchContainerRequestFilter());
        providers.add(new FaultyContainerRequestFilter());
        providers.add(new PreMatchReplaceStreamOrAddress());
        providers.add(new ServerTestFeature());
        providers.add(new JacksonJaxbJsonProvider());
        sf.setProviders(providers);
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setAddress("http://localhost:" + PORT + "/");
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
            BookServer20 s = new BookServer20();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
    
    @PreMatching
    @Priority(1)
    private static class PreMatchContainerRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            UriInfo ui = context.getUriInfo();
            String path = ui.getPath(false);
            
            if (context.getMethod().equals("POST") 
                && "bookstore/bookheaders/simple".equals(path) && !context.hasEntity()) {
                byte[] bytes = StringUtils.toBytesUTF8("<Book><name>Book</name><id>126</id></Book>");
                context.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, Integer.toString(bytes.length));
                context.getHeaders().putSingle("Content-Type", "application/xml");
                context.getHeaders().putSingle("EmptyRequestStreamDetected", "true");
                context.setEntityStream(new ByteArrayInputStream(bytes));
            }
            if ("true".equals(context.getProperty("DynamicPrematchingFilter"))) {
                throw new RuntimeException();
            }
            context.setProperty("FirstPrematchingFilter", "true");
            
            if ("wrongpath".equals(path)) {
                context.setRequestUri(URI.create("/bookstore/bookheaders/simple"));
            } else if ("throwException".equals(path)) {
                context.setProperty("filterexception", "prematch");
                throw new InternalServerErrorException(
                    Response.status(500).type("text/plain")
                        .entity("Prematch filter error").build());
            }
            
            MediaType mt = context.getMediaType();
            if (mt != null && mt.toString().equals("text/xml")) {
                String method = context.getMethod();
                if ("PUT".equals(method)) {
                    context.setMethod("POST");
                }
                context.getHeaders().putSingle("Content-Type", "application/xml");
            } else {
                String newMt = context.getHeaderString("newmediatype");
                if (newMt != null) {
                    context.getHeaders().putSingle("Content-Type", newMt);
                }
            }
            List<MediaType> acceptTypes = context.getAcceptableMediaTypes();
            if (acceptTypes.size() == 1 && acceptTypes.get(0).toString().equals("text/mistypedxml")) {
                context.getHeaders().putSingle("Accept", "text/xml");
            }
        }
        
    }
    
    @PreMatching
    @Priority(3)
    private static class PreMatchContainerRequestFilter2 implements ContainerRequestFilter {
        @Context
        private HttpServletRequest servletRequest;
        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (!"true".equals(context.getProperty("FirstPrematchingFilter"))
                || !"true".equals(context.getProperty("DynamicPrematchingFilter"))
                || !"true".equals(servletRequest.getAttribute("FirstPrematchingFilter"))
                || !"true".equals(servletRequest.getAttribute("DynamicPrematchingFilter"))) {
                throw new RuntimeException();
            }
            context.getHeaders().add("BOOK", "12");
        }
        
    }
    
    @PreMatching
    private static class PreMatchReplaceStreamOrAddress implements ContainerRequestFilter {
        @Context
        private UriInfo ui;
        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            String path = ui.getPath();
            if (path.endsWith("books/checkN")) {
                URI requestURI = URI.create(path.replace("N", "2"));
                context.setRequestUri(requestURI);
                
                String body = IOUtils.readStringFromStream(context.getEntityStream());
                if (!"s".equals(body)) {
                    throw new RuntimeException();
                }
                
                replaceStream(context);
            } else if (path.endsWith("books/check2")) {
                replaceStream(context);
            }
        }
        private void replaceStream(ContainerRequestContext context) {
            InputStream is = new ByteArrayInputStream("123".getBytes());
            context.setEntityStream(is);
        }
    }
    
        
    @PreMatching
    @Priority(2)
    private static class PreMatchDynamicContainerRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (!"true".equals(context.getProperty("FirstPrematchingFilter"))) {
                throw new RuntimeException();
            }
            context.setProperty("DynamicPrematchingFilter", "true");
        }
        
    }
    
    @CustomHeaderAdded
    private static class PostMatchContainerRequestFilter implements ContainerRequestFilter {
        @Context
        private UriInfo ui;
        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (ui.getQueryParameters().getFirst("throwException") != null) {
                context.setProperty("filterexception", "postmatch");
                throw new InternalServerErrorException(
                    Response.status(500).type("text/plain")
                        .entity("Postmatch filter error").build());
            }
            String value = context.getHeaders().getFirst("Book");
            if (value != null) {
                context.getHeaders().addFirst("Book", value + "3");
            }
        }
        
    }
    
    @Faulty
    @CustomHeaderAdded
    private static class FaultyContainerRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            throw new RuntimeException();
        }
        
    }
    
    @Priority(3)
    public static class PostMatchContainerResponseFilter implements ContainerResponseFilter {

        @Context
        private ResourceInfo rInfo;
        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            String ct = responseContext.getMediaType().toString();
            if (requestContext.getProperty("filterexception") != null) {
                if (!"text/plain".equals(ct)) {
                    throw new RuntimeException();
                }
                responseContext.getHeaders().putSingle("FilterException", 
                                                       requestContext.getProperty("filterexception"));
            }
            Object entity = responseContext.getEntity();
            Type entityType = responseContext.getEntityType();
            if (entity instanceof GenericHandler && InjectionUtils.getActualType(entityType) == Book.class) {
                ct += ";charset=ISO-8859-1";
                if ("getGenericBook2".equals(rInfo.getResourceMethod().getName())) {
                    Annotation[] anns = responseContext.getEntityAnnotations();
                    if (anns.length == 4 && anns[3].annotationType() == Context.class) {
                        responseContext.getHeaders().addFirst("Annotations", "OK");
                    }
                } else {
                    responseContext.setEntity(new Book("book", 124L));
                }
            } else {
                ct += ";charset=";
            }
            responseContext.getHeaders().putSingle("Content-Type", ct);
            responseContext.getHeaders().add("Response", "OK");
        }
        
    }
    
    @Priority(1)
    public static class PostMatchContainerResponseFilter2 implements ContainerResponseFilter {
        @Context
        private ResourceInfo ri;
        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            if (ri.getResourceMethod() != null
                && "addBook2".equals(ri.getResourceMethod().getName())) {
                return;
            }
            if (!responseContext.getHeaders().containsKey("Response")) {
                throw new RuntimeException();
            }
        
            if ((!responseContext.getHeaders().containsKey("DynamicResponse")
                || !responseContext.getHeaders().containsKey("DynamicResponse2"))
                && !"Prematch filter error".equals(responseContext.getEntity())) {
                throw new RuntimeException();
            }
            responseContext.getHeaders().add("Response2", "OK2");
            
        }
        
    }
    @Priority(4)
    @CustomHeaderAdded
    @PostMatchMode
    public static class PostMatchContainerResponseFilter3 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("Custom", "custom");
            if (!responseContext.getEntity().equals("Postmatch filter error")) {
                Book book = (Book)responseContext.getEntity();
                responseContext.setEntity(new Book(book.getName(), 1 + book.getId()), null, null);
            }
        }
        
    }
    
    public static class PostMatchDynamicContainerRequestResponseFilter 
        implements ContainerRequestFilter, ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            if (!responseContext.getHeaders().containsKey("Response")) {
                throw new RuntimeException();
            }
            responseContext.getHeaders().add("DynamicResponse2", "Dynamic2");
            
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            throw new RuntimeException();
            
        }
        
    }
    public static class PostMatchDynamicEchoBookFilter implements ContainerResponseFilter {
        private int supplement;
        public PostMatchDynamicEchoBookFilter(int supplement) {
            this.supplement = supplement;
        }
        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            Book book = (Book)responseContext.getEntity();
            responseContext.setEntity(new Book(book.getName(), book.getId() + supplement));
        }
    }
    
    @Priority(2)
    public static class PostMatchDynamicContainerResponseFilter 
        implements ContainerResponseFilter {
    
        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            if (!responseContext.getHeaders().containsKey("Response")) {
                throw new RuntimeException();
            }
            responseContext.getHeaders().add("DynamicResponse", "Dynamic");
            
        }
        
    }
    
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface CustomHeaderAdded { 
        
    }
    
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface CustomHeaderAddedAsync { 
        
    }
    
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface PostMatchMode { 
        
    }
    
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface Faulty { 
        
    }
    
    @Priority(1)
    public static class CustomReaderInterceptor implements ReaderInterceptor {
        @Context
        private ResourceInfo ri;
        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException,
            WebApplicationException {
            if (ri.getResourceClass() == BookStore.class) {
                context.getHeaders().add("ServerReaderInterceptor", "serverRead");
            }
            return context.proceed();
            
        }
        
    }
    
    @Priority(2)
    @CustomHeaderAddedAsync
    public static class CustomReaderBoundInterceptor implements ReaderInterceptor {
        @Context
        private ResourceInfo ri;
        @Context
        private UriInfo ui;
        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException,
            WebApplicationException {
            if (ri.getResourceClass() == BookStore.class) {
                String serverRead = context.getHeaders().getFirst("ServerReaderInterceptor");
                if (serverRead == null || !serverRead.equals("serverRead")) {
                    throw new RuntimeException();
                }
                if (ui.getPath().endsWith("/async")) {
                    context.getHeaders().putSingle("ServerReaderInterceptor", "serverReadAsync");
                }
            }
            return context.proceed();
            
        }
        
    }
    
    public static class CustomWriterInterceptor implements WriterInterceptor {

        @Context
        private HttpServletResponse response;
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("ServerWriterInterceptor", "serverWrite");
            context.getHeaders().putSingle("ServerWriterInterceptor2", "serverWrite2");
            response.addHeader("ServerWriterInterceptorHttpResponse", "serverWriteHttpResponse");
            String ct = context.getHeaders().getFirst("Content-Type").toString();
            if (!ct.endsWith("ISO-8859-1")) {
                ct += "us-ascii";
            }
            context.setMediaType(MediaType.valueOf(ct));
            context.proceed();
        }
        
    }
    
    public static class CustomDynamicFeature implements DynamicFeature {

        private static final ContainerResponseFilter RESPONSE_FILTER =
            new PostMatchDynamicContainerResponseFilter();
        
        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext configurable) {
            
            configurable.register(new PreMatchDynamicContainerRequestFilter());
            configurable.register(RESPONSE_FILTER);
            Map<Class<?>, Integer> contracts = new HashMap<Class<?>, Integer>();
            contracts.put(ContainerResponseFilter.class, 2);
            configurable.register(new PostMatchDynamicContainerRequestResponseFilter(), 
                                  contracts);
            Method m = resourceInfo.getResourceMethod();
            if ("echoBookElement".equals(m.getName())) {
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType == Book.class) {
                    configurable.register(new PostMatchDynamicEchoBookFilter(2));
                }
            }
        }
        
    }
    private static class ServerTestFeature implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(new GenericHandlerWriter());
            return true;
        }
        
    }
}
