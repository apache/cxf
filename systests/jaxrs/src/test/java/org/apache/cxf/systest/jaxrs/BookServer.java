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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.search.QueryContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.ext.search.sql.SQLPrinterVisitor;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.BinaryDataProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.message.Message;
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
}
