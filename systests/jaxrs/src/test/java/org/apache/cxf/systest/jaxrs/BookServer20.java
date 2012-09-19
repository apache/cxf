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

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.NameBinding;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
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
        
        providers.add(new PreMatchContainerRequestFilter());
        providers.add(new PostMatchContainerResponseFilter());
        providers.add(new PostMatchContainerResponseFilter2());
        providers.add(new CustomReaderInterceptor());
        providers.add(new CustomWriterInterceptor());
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
    private static class PreMatchContainerRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            context.getHeaders().add("BOOK", "123");
            
            UriInfo ui = context.getUriInfo();
            String path = ui.getPath(false);
            if ("wrongpath".equals(path)) {
                context.setRequestUri(URI.create("/bookstore/bookheaders/simple"));
            }
        }
        
    }
    
    public static class PostMatchContainerResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("Response", "OK");
        }
        
    }
    
    @CustomHeaderAdded
    public static class PostMatchContainerResponseFilter2 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("Custom", "custom");
            Book book = (Book)responseContext.getEntity();
            responseContext.setEntity(new Book(book.getName(), 1 + book.getId()), null, null);
        }
        
    }
    
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface CustomHeaderAdded { 
        
    }
    
    public static class CustomReaderInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException,
            WebApplicationException {
            context.getHeaders().add("ServerReaderInterceptor", "serverRead");
            return context.proceed();
        }
        
    }
    
    public static class CustomWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("ServerWriterInterceptor", "serverWrite");
            context.proceed();
        }
        
    }
}
