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
package org.apache.cxf.jaxrs.utils;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Providers;
import jakarta.xml.bind.JAXBContext;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.Customer.CustomerContext;
import org.apache.cxf.jaxrs.Customer.MyType;
import org.apache.cxf.jaxrs.Customer.Query;
import org.apache.cxf.jaxrs.Customer2;
import org.apache.cxf.jaxrs.CustomerApplication;
import org.apache.cxf.jaxrs.CustomerGender;
import org.apache.cxf.jaxrs.CustomerParameterHandler;
import org.apache.cxf.jaxrs.JAXBContextProvider;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.SimpleFactory;
import org.apache.cxf.jaxrs.Timezone;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.HttpServletRequestFilter;
import org.apache.cxf.jaxrs.impl.HttpServletResponseFilter;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.ResourceInfoImpl;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletConfig;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalUriInfo;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JAXRSUtilsTest {

    @Before
    public void setUp() {
    }
    @After
    public void tearDown() {
        AbstractResourceInfo.clearAllMaps();
    }

    @Test
    public void testFormParametersUTF8Encoding() throws Exception {
        JAXRSUtils.intersectMimeTypes("application/json", "application/json+v2");
        doTestFormParamsWithEncoding(StandardCharsets.UTF_8.name(), true);
        doTestFormParamsWithEncoding(StandardCharsets.UTF_8.name(), false);
    }

    @Test
    public void testFormParametersISO88591Encoding() throws Exception {
        doTestFormParamsWithEncoding("ISO-8859-1", true);
    }

    private void doTestFormParamsWithEncoding(String enc, boolean setEnc) throws Exception {
        Class<?>[] argType = {String.class, List.class};
        Method m = Customer.class.getMethod("testFormParam", argType);
        Message messageImpl = createMessage();
        String body = "p1=" + URLEncoder.encode("\u00E4\u00F6\u00FC", enc) + "&p2=2&p2=3";
        messageImpl.put(Message.REQUEST_URI, "/foo");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        String ct = MediaType.APPLICATION_FORM_URLENCODED;
        if (setEnc) {
            ct += ";charset=" + enc;
        }
        headers.putSingle("Content-Type", ct);
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals("2 form params should've been identified", 2, params.size());
        assertEquals("First Form Parameter not matched correctly",
                     "\u00E4\u00F6\u00FC", params.get(0));
        List<String> list = CastUtils.cast((List<?>)params.get(1));
        assertEquals(2, list.size());
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
    }

    @Test
    public void testSelectBetweenMultipleResourceClasses() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class,
                              org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        assertEquals(org.apache.cxf.jaxrs.resources.BookStore.class, firstResourceClass(resources, "/bookstore"));

        assertEquals(org.apache.cxf.jaxrs.resources.BookStore.class, firstResourceClass(resources, "/bookstore/"));

        assertEquals(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class,
                firstResourceClass(resources, "/bookstore/bar"));
    }

    private static Class<?> firstResourceClass(List<ClassResourceInfo> resources, String path) {
        return JAXRSUtils.selectResourceClass(resources, path, null).keySet().iterator().next().getResourceClass();
    }

    @Test
    public void testInjectCustomContext() throws Exception {
        final CustomerContext contextImpl = new CustomerContext() {

            public String get() {
                return "customerContext";
            }

        };
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        Customer customer = new Customer();
        sf.setServiceBeanObjects(customer);
        sf.setProvider(new ContextProvider<CustomerContext>() {
            public CustomerContext createContext(Message message) {
                return contextImpl;
            }
        });
        sf.setStart(false);
        Server s = sf.create();
        assertTrue(customer.getCustomerContext() instanceof ThreadLocalProxy<?>);
        invokeCustomerMethod(sf.getServiceFactory().getClassResourceInfo().get(0),
                             customer, s);
        CustomerContext context = customer.getCustomerContext();
        assertEquals("customerContext", context.get());
    }

    @Test
    public void testInjectApplicationInSingleton() throws Exception {
        CustomerApplication app = new CustomerApplication();
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        Customer customer = new Customer();
        sf.setServiceBeanObjects(customer);
        sf.setApplication(app);
        sf.setStart(false);
        Server server = sf.create();
        assertSame(app, customer.getApplication1());
        assertSame(app, customer.getApplication2());
        @SuppressWarnings("unchecked")
        ThreadLocalProxy<UriInfo> proxy = (ThreadLocalProxy<UriInfo>)app.getUriInfo();
        assertNotNull(proxy);
        invokeCustomerMethod(sf.getServiceFactory().getClassResourceInfo().get(0),
                             customer, server);
        assertSame(app, customer.getApplication2());
        assertTrue(proxy.get() instanceof UriInfo);
    }

    @Test
    public void testInjectApplicationInPerRequestResource() throws Exception {
        CustomerApplication app = new CustomerApplication();
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setServiceClass(Customer.class);
        sf.setApplication(app);
        sf.setStart(false);
        Server server = sf.create();

        @SuppressWarnings("unchecked")
        ThreadLocalProxy<UriInfo> proxy = (ThreadLocalProxy<UriInfo>)app.getUriInfo();
        assertNotNull(proxy);

        ClassResourceInfo cri = sf.getServiceFactory().getClassResourceInfo().get(0);

        Customer customer = (Customer)cri.getResourceProvider().getInstance(
             createMessage());

        assertNull(customer.getApplication1());
        assertNull(customer.getApplication2());

        invokeCustomerMethod(cri, customer, server);
        assertSame(app, customer.getApplication1());
        assertSame(app, customer.getApplication2());

        assertTrue(proxy.get() instanceof UriInfo);
    }

    private void invokeCustomerMethod(ClassResourceInfo cri,
        Customer customer, Server server) throws Exception {
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(
            Customer.class.getMethod("test", new Class[]{}));
        JAXRSInvoker invoker = new JAXRSInvoker();
        Exchange exc = new ExchangeImpl();
        exc.put(Endpoint.class, server.getEndpoint());
        Message inMessage = new MessageImpl();
        exc.setInMessage(inMessage);
        exc.put(OperationResourceInfo.class, ori);
        invoker.invoke(exc, Collections.emptyList(), customer);
    }

    @Test
    public void testSelectBetweenMultipleResourceClasses2() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResourceTemplate1.class,
                              org.apache.cxf.jaxrs.resources.TestResourceTemplate2.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        assertEquals(org.apache.cxf.jaxrs.resources.TestResourceTemplate1.class, firstResourceClass(resources, "/1"));

        assertEquals(org.apache.cxf.jaxrs.resources.TestResourceTemplate1.class, firstResourceClass(resources, "/1/"));

        assertEquals(org.apache.cxf.jaxrs.resources.TestResourceTemplate2.class,
                firstResourceClass(resources, "/1/foo"));

        assertEquals(org.apache.cxf.jaxrs.resources.TestResourceTemplate2.class,
                firstResourceClass(resources, "/1/foo/bar"));
    }

    @Test
    public void testSelectBetweenMultipleResourceClasses3() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResourceTemplate4.class,
                              org.apache.cxf.jaxrs.resources.TestResourceTemplate3.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        assertEquals(org.apache.cxf.jaxrs.resources.TestResourceTemplate3.class, firstResourceClass(resources, "/"));

        assertEquals(org.apache.cxf.jaxrs.resources.TestResourceTemplate4.class,
                firstResourceClass(resources, "/test"));
    }

    @Test
    public void testFindTargetResourceClass() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";

        //If acceptContentTypes does not specify a specific Mime type, the
        //method is declared with a most specific ProduceMime type is selected.
        OperationResourceInfo ori = findTargetResourceClass(resources, createMessage(),
             "/bookstore/1/books/123/", "GET", new MetadataMap<String, String>(), contentTypes,
             getTypes("application/json,application/xml;q=0.9"));
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethodToInvoke().getName());

        //test
        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/1/books/123",
             "GET", new MetadataMap<String, String>(), contentTypes, getTypes("application/json"));
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethodToInvoke().getName());

        //test
        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/1/books/123",
              "GET", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethodToInvoke().getName());

        //test
        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/1/books",
                      "GET", new MetadataMap<String, String>(), contentTypes,
                      getTypes("application/xml"));
        assertNotNull(ori);
        assertEquals("getBooks", ori.getMethodToInvoke().getName());

        //test find POST
        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/1/books",
                 "POST", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethodToInvoke().getName());

        //test find PUT
        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/1/books",
            "PUT", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));
        assertEquals("updateBook", ori.getMethodToInvoke().getName());

        //test find DELETE
        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/1/books/123",
             "DELETE", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethodToInvoke().getName());

    }

    private List<MediaType> getTypes(String types) {
        return JAXRSUtils.parseMediaTypes(types);
    }

    @Test
    public void testGetMediaTypes() {
        List<MediaType> types = JAXRSUtils.getMediaTypes(new String[]{"text/xml"});
        assertEquals(1, types.size());
        assertEquals(MediaType.TEXT_XML_TYPE, types.get(0));
    }
    @Test
    public void testGetMediaTypes2() {
        List<MediaType> types = JAXRSUtils.getMediaTypes(new String[]{"text/xml", "text/plain"});
        assertEquals(2, types.size());
        assertEquals(MediaType.TEXT_XML_TYPE, types.get(0));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, types.get(1));
    }
    @Test
    public void testGetMediaTypes3() {
        List<MediaType> types = JAXRSUtils.getMediaTypes(new String[]{"text/xml, text/plain"});
        assertEquals(2, types.size());
        assertEquals(MediaType.TEXT_XML_TYPE, types.get(0));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, types.get(1));
    }

    @Test
    public void testFindTargetResourceClassWithTemplates() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreTemplates.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";

        //If acceptContentTypes does not specify a specific Mime type, the
        //method is declared with a most specific ProduceMime type is selected.
        MetadataMap<String, String> values = new MetadataMap<>();
        OperationResourceInfo ori = findTargetResourceClass(resources, createMessage(), "/1/2/",
             "GET", values, contentTypes, getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("getBooks", ori.getMethodToInvoke().getName());
        assertEquals("Only id and final match groups should be there", 2, values.size());
        assertEquals("2 {id} values should've been picked up", 2, values.get("id").size());
        assertEquals("FINAL_MATCH_GROUP should've been picked up", 1,
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("First {id} is 1", "1", values.getFirst("id"));
        assertEquals("Second id is 2", "2", values.get("id").get(1));

        values = new MetadataMap<>();
        ori = findTargetResourceClass(resources, createMessage(), "/2",
             "POST", values, contentTypes, getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("updateBookStoreInfo", ori.getMethodToInvoke().getName());
        assertEquals("Only id and final match groups should be there", 2, values.size());
        assertEquals("Only single {id} should've been picked up", 1, values.get("id").size());
        assertEquals("FINAL_MATCH_GROUP should've been picked up", 1,
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("Only the first {id} should've been picked up", "2", values.getFirst("id"));

        values = new MetadataMap<>();
        ori = findTargetResourceClass(resources, createMessage(), "/3/4",
             "PUT", values, contentTypes, getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("updateBook", ori.getMethodToInvoke().getName());
        assertEquals("Only the first {id} should've been picked up", 3, values.size());
        assertEquals("Only the first {id} should've been picked up", 1, values.get("id").size());
        assertEquals("Only the first {id} should've been picked up", 1, values.get("bookId").size());
        assertEquals("Only the first {id} should've been picked up", 1,
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("Only the first {id} should've been picked up", "3", values.getFirst("id"));
        assertEquals("Only the first {id} should've been picked up", "4", values.getFirst("bookId"));
    }

    @Test
    public void testFindTargetResourceClassWithSubResource() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";

        OperationResourceInfo ori = findTargetResourceClass(resources,
               createMessage(), "/bookstore/books/sub/123", "GET", new MetadataMap<String, String>(), contentTypes,
               getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethodToInvoke().getName());

        ori = findTargetResourceClass(resources, createMessage(),
            "/bookstore/books/123/true/chapter/1", "GET", new MetadataMap<String, String>(), contentTypes,
            getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("getNewBook", ori.getMethodToInvoke().getName());

        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/books",
            "POST", new MetadataMap<String, String>(), contentTypes, getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethodToInvoke().getName());

        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/books",
             "PUT", new MetadataMap<String, String>(), contentTypes, getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("updateBook", ori.getMethodToInvoke().getName());

        ori = findTargetResourceClass(resources, createMessage(), "/bookstore/books/123",
            "DELETE", new MetadataMap<String, String>(), contentTypes, getTypes("*/*"));
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethodToInvoke().getName());
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype() throws Exception {
        List <MediaType> candidateList =
            JAXRSUtils.intersectMimeTypes("application/bar+xml", "application/*+xml");

        assertEquals(1, candidateList.size());
        assertEquals("application/bar+xml", candidateList.get(0).toString());
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype2() throws Exception {
        List <MediaType> candidateList =
            JAXRSUtils.intersectMimeTypes("application/bar+xml", "application/bar+xml");

        assertEquals(1, candidateList.size());
        assertEquals("application/bar+xml", candidateList.get(0).toString());
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype3() throws Exception {
        List <MediaType> candidateList =
            JAXRSUtils.intersectMimeTypes("application/*+xml", "application/bar+xml");

        assertEquals(1, candidateList.size());
        assertEquals("application/bar+xml", candidateList.get(0).toString());
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype4() throws Exception {
        List <MediaType> candidateList =
            JAXRSUtils.intersectMimeTypes("application/*+xml", "application/bar+json");

        assertEquals(0, candidateList.size());

    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype5() throws Exception {
        List <MediaType> candidateList =
            JAXRSUtils.intersectMimeTypes("application/bar+xml", "application/bar+*");

        assertEquals(1, candidateList.size());
        assertEquals("application/bar+xml", candidateList.get(0).toString());
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype6() throws Exception {
        Message m = new MessageImpl();
        m.put(JAXRSUtils.PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK, true);
        assertTrue(JAXRSUtils.compareCompositeSubtypes("application/bar+xml", "application/xml", m));
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype7() throws Exception {
        Message m = new MessageImpl();
        m.put(JAXRSUtils.PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK, true);
        assertTrue(JAXRSUtils.compareCompositeSubtypes("application/xml", "application/bar+xml", m));
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype8() throws Exception {
        Message m = new MessageImpl();
        m.put(JAXRSUtils.PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK, true);
        assertTrue(JAXRSUtils.compareCompositeSubtypes("application/xml+bar", "application/xml", m));
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype9() throws Exception {
        Message m = new MessageImpl();
        m.put(JAXRSUtils.PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK, true);
        assertTrue(JAXRSUtils.compareCompositeSubtypes("application/xml", "application/xml+bar", m));
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype10() throws Exception {
        Message m = new MessageImpl();
        m.put(JAXRSUtils.PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK, true);
        assertFalse(JAXRSUtils.compareCompositeSubtypes("application/v1+xml", "application/v2+xml", m));
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype11() throws Exception {
        Message m = new MessageImpl();
        m.put(JAXRSUtils.PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK, true);
        assertFalse(JAXRSUtils.compareCompositeSubtypes("application/v1+xml", "application/json", m));
    }

    @Test
    public void testIntersectMimeTypes() throws Exception {
        //test basic
        List<MediaType> methodMimeTypes = new ArrayList<>(
             JAXRSUtils.parseMediaTypes("application/mytype,application/xml,application/json"));

        List <MediaType> candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes,
                                                 MediaType.valueOf("application/json"));

        assertEquals(1, candidateList.size());
        assertEquals("application/json", candidateList.get(0).toString());

        //test basic
        methodMimeTypes = JAXRSUtils.parseMediaTypes(
            "application/mytype, application/json, application/xml");
        candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes,
                                                      MediaType.valueOf("application/json"));

        assertEquals(1, candidateList.size());
        assertEquals("application/json", candidateList.get(0).toString());

        //test accept wild card */*
        candidateList = JAXRSUtils.intersectMimeTypes(
            "application/mytype,application/json,application/xml", "*/*");

        assertEquals(3, candidateList.size());

        //test accept wild card application/*
        methodMimeTypes = JAXRSUtils.parseMediaTypes("text/html,text/xml,application/xml");
        MediaType acceptContentType = MediaType.valueOf("text/*");
        candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, acceptContentType);

        assertEquals(2, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("text/html".equals(type.toString())
                       || "text/xml".equals(type.toString()));
        }

        //test produce wild card */*
        candidateList = JAXRSUtils.intersectMimeTypes("*/*", "application/json");

        assertEquals(1, candidateList.size());
        assertEquals("application/json", candidateList.get(0).toString());

        //test produce wild card application/*
        candidateList = JAXRSUtils.intersectMimeTypes("application/*", "application/json");

        assertEquals(1, candidateList.size());
        assertEquals("application/json", candidateList.get(0).toString());

        //test produce wild card */*, accept wild card */*
        candidateList = JAXRSUtils.intersectMimeTypes("*/*", "*/*");

        assertEquals(1, candidateList.size());
        assertEquals("*/*", candidateList.get(0).toString());
    }

    @Test
    public void testIntersectMimeTypesTwoArray() throws Exception {
        //test basic
        List <MediaType> acceptedMimeTypes =
            JAXRSUtils.parseMediaTypes("application/mytype, application/xml, application/json");

        List <MediaType> candidateList =
            JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, JAXRSUtils.ALL_TYPES);

        assertEquals(3, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("application/mytype".equals(type.toString())
                       || "application/xml".equals(type.toString())
                       || "application/json".equals(type.toString()));
        }

        //test basic
        acceptedMimeTypes = Collections.singletonList(JAXRSUtils.ALL_TYPES);
        List<MediaType> providerMimeTypes =
            JAXRSUtils.parseMediaTypes("application/mytype, application/xml, application/json");

        candidateList = JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, providerMimeTypes, false);

        assertEquals(3, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("application/mytype".equals(type.toString())
                       || "application/xml".equals(type.toString())
                       || "application/json".equals(type.toString()));
        }

        //test empty
        acceptedMimeTypes = JAXRSUtils.parseMediaTypes("application/mytype,application/xml");

        candidateList = JAXRSUtils.intersectMimeTypes(acceptedMimeTypes,
                                                      MediaType.valueOf("application/json"));

        assertEquals(0, candidateList.size());
    }

    @Test
    public void testParseMediaTypes() throws Exception {
        List<MediaType> types = JAXRSUtils.parseMediaTypes("*");
        assertTrue(types.size() == 1
                   && types.get(0).equals(JAXRSUtils.ALL_TYPES));
        types = JAXRSUtils.parseMediaTypes("text/*");
        assertTrue(types.size() == 1 && types.get(0).equals(new MediaType("text", "*")));
        types = JAXRSUtils.parseMediaTypes("text/*,text/plain;q=.2,text/xml,TEXT/BAR");
        assertTrue(types.size() == 4
                   && "text/*".equals(types.get(0).toString())
                   && "text/plain;q=.2".equals(types.get(1).toString())
                   && "text/xml".equals(types.get(2).toString())
                   && "text/bar".equals(types.get(3).toString()));

    }

    private static List<MediaType> sortMediaTypes(String mediaTypes) {
        return JAXRSUtils.sortMediaTypes(mediaTypes, JAXRSUtils.MEDIA_TYPE_Q_PARAM);
    }

    private static List<MediaType> sortMediaTypes(List<MediaType> mediaTypes) {
        return JAXRSUtils.sortMediaTypes(mediaTypes, JAXRSUtils.MEDIA_TYPE_Q_PARAM);
    }

    private static int compareSortedMediaTypes(List<MediaType> mt1, List<MediaType> mt2) {
        return JAXRSUtils.compareSortedMediaTypes(mt1, mt2, JAXRSUtils.MEDIA_TYPE_Q_PARAM);
    }

    @Test
    public void testSortMediaTypes() throws Exception {
        List<MediaType> types =
            sortMediaTypes("text/*,text/plain;q=.2,text/xml,TEXT/BAR");
        assertTrue(types.size() == 4
                   && "text/xml".equals(types.get(0).toString())
                   && "text/bar".equals(types.get(1).toString())
                   && "text/plain;q=.2".equals(types.get(2).toString())
                   && "text/*".equals(types.get(3).toString()));
    }

    @Test
    public void testCompareMediaTypes() throws Exception {
        MediaType m1 = MediaType.valueOf("text/xml");
        MediaType m2 = MediaType.valueOf("text/*");
        assertTrue("text/xml is more specific than text/*",
                   JAXRSUtils.compareMediaTypes(m1, m2) < 0);
        assertTrue("text/* is less specific than text/*",
                   JAXRSUtils.compareMediaTypes(m2, m1) > 0);
        assertTrue("text/xml should be equal to itself",
                   JAXRSUtils.compareMediaTypes(m1, new MediaType("text", "xml")) == 0);
        assertTrue("text/* should be equal to itself",
                   JAXRSUtils.compareMediaTypes(m2, new MediaType("text", "*")) == 0);

        assertTrue("text/plain and text/xml are just two specific media types",
                   JAXRSUtils.compareMediaTypes(MediaType.valueOf("text/plain"), m1) == 0);
        assertTrue("text/xml and text/plain are just two specific media types",
                   JAXRSUtils.compareMediaTypes(m1, MediaType.valueOf("text/plain")) == 0);
        assertTrue("*/* is less specific than text/xml",
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m1) > 0);
        assertTrue("*/* is less specific than text/xml",
                   JAXRSUtils.compareMediaTypes(m1, JAXRSUtils.ALL_TYPES) < 0);
        assertTrue("*/* is less specific than text/*",
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m2) > 0);
        assertTrue("*/* is less specific than text/*",
                   JAXRSUtils.compareMediaTypes(m2, JAXRSUtils.ALL_TYPES) < 0);

        MediaType m3 = MediaType.valueOf("text/xml;q=0.2");
        assertTrue("text/xml should be more preferred than text/xml;q=0.2",
                   JAXRSUtils.compareMediaTypes(m1, m3) < 0);
        MediaType m4 = MediaType.valueOf("text/xml;q=.3");
        assertTrue("text/xml;q=.3 should be more preferred than text/xml;q=0.2",
                   JAXRSUtils.compareMediaTypes(m4, m3) < 0);

        assertTrue("text/xml;q=.3 should be more preferred than than text/xml;q=0.2",
                  JAXRSUtils.compareMediaTypes(m3, m4) > 0);
    }

    @Test
    public void testCompareSortedMediaTypes() throws Exception {
        MediaType m1 = MediaType.valueOf("text/xml");
        MediaType m2 = MediaType.valueOf("text/*");
        assertTrue("text/xml is more specific than text/*",
                   compareSortedMediaTypes(Collections.singletonList(m1),
                                                      Collections.singletonList(m2)) < 0);
        assertTrue("text/* is less specific than text/xml",
                   compareSortedMediaTypes(Collections.singletonList(m2),
                                                      Collections.singletonList(m1)) > 0);

        assertTrue("text/xml is the same as text/xml",
                   compareSortedMediaTypes(Collections.singletonList(m1),
                                                      Collections.singletonList(m1)) == 0);

        List<MediaType> sortedList1 = new ArrayList<>();
        sortedList1.add(m1);
        sortedList1.add(m2);

        List<MediaType> sortedList2 = new ArrayList<>();
        sortedList2.add(m1);
        sortedList2.add(m2);

        assertTrue("lists should be equal",
                   compareSortedMediaTypes(sortedList1, sortedList2) == 0);

        sortedList1.add(MediaType.WILDCARD_TYPE);
        assertTrue("first list should be less specific",
                   compareSortedMediaTypes(sortedList1, sortedList2) > 0);
        sortedList1.add(MediaType.WILDCARD_TYPE);
        assertTrue("second list should be more specific",
                   compareSortedMediaTypes(sortedList2, sortedList1) < 0);
    }

    @Test
    public void testAcceptTypesMatch() throws Exception {

        Method m = Customer.class.getMethod("test", new Class[]{});
        ClassResourceInfo cr = new ClassResourceInfo(Customer.class);

        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES,
                                             new MediaType("text", "xml"),
                                             new OperationResourceInfo(m, cr)));
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES,
                                             new MediaType("text", "*"),
                                             new OperationResourceInfo(m, cr)));
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES,
                                             new MediaType("*", "*"),
                                             new OperationResourceInfo(m, cr)));
        assertFalse("text/plain was matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES,
                                             new MediaType("text", "plain"),
                                             new OperationResourceInfo(m, cr)));
    }


    @Test
    public void testQueryParameters() throws Exception {
        Class<?>[] argType = {String.class, Integer.TYPE, String.class, String.class};
        Method m = Customer.class.getMethod("testQuery", argType);
        Message messageImpl = createMessage();

        messageImpl.put(Message.QUERY_STRING, "query=24&query2=");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(4, params.size());
        assertEquals("Query Parameter was not matched correctly", "24", params.get(0));
        assertEquals("Primitive Query Parameter was not matched correctly", 24, params.get(1));
        assertEquals("", params.get(2));
        assertNull(params.get(3));
    }

    @Test
    public void testQueryParametersIntegerArray() throws Exception {
        Class<?>[] argType = {Integer[].class};
        Method m = Customer.class.getMethod("testQueryIntegerArray", argType);
        Message messageImpl = createMessage();

        messageImpl.put(Message.QUERY_STRING, "query=1&query=2");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(1, params.size());
        Integer[] intValues = (Integer[])params.get(0);
        assertEquals(2, intValues.length);
        assertEquals(1, (int)intValues[0]);
        assertEquals(2, (int)intValues[1]);
    }

    @Test
    public void testQueryParametersIntArray() throws Exception {
        Class<?>[] argType = {int[].class};
        Method m = Customer.class.getMethod("testQueryIntArray", argType);
        Message messageImpl = createMessage();

        messageImpl.put(Message.QUERY_STRING, "query=1&query=2");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(1, params.size());
        int[] intValues = (int[])params.get(0);
        assertEquals(2, intValues.length);
        assertEquals(1, intValues[0]);
        assertEquals(2, intValues[1]);
    }

    @Test
    public void testQueryParametersIntegerArrayValueIsColection() throws Exception {
        Class<?>[] argType = {Integer[].class};
        Method m = Customer.class.getMethod("testQueryIntegerArray", argType);
        Message messageImpl = createMessage();
        messageImpl.put("parse.query.value.as.collection", true);
        messageImpl.put(Message.QUERY_STRING, "query=1&query=2,3");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(1, params.size());
        Integer[] intValues = (Integer[])params.get(0);
        assertEquals(3, intValues.length);
        assertEquals(1, (int)intValues[0]);
        assertEquals(2, (int)intValues[1]);
        assertEquals(3, (int)intValues[2]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQueryParamAsListWithDefaultValue() throws Exception {
        Class<?>[] argType = {List.class, List.class, List.class, Integer[].class,
            List.class, List.class, List.class, List.class, List.class};
        Method m = Customer.class.getMethod("testQueryAsList", argType);
        Message messageImpl = createMessage();
        ProviderFactory.getInstance(messageImpl)
            .registerUserProvider(new MyTypeParamConverterProvider());
        messageImpl.put(Message.QUERY_STRING,
                "query2=query2Value&query2=query2Value2&query3=1&query3=2&query4=");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(9, params.size());
        List<String> queryList = (List<String>)params.get(0);
        assertNotNull(queryList);
        assertEquals(1, queryList.size());
        assertEquals("default", queryList.get(0));

        List<String> queryList2 = (List<String>)params.get(1);
        assertNotNull(queryList2);
        assertEquals(2, queryList2.size());
        assertEquals("query2Value", queryList2.get(0));
        assertEquals("query2Value2", queryList2.get(1));

        List<Integer> queryList3 = (List<Integer>)params.get(2);
        assertNotNull(queryList3);
        assertEquals(2, queryList3.size());
        assertEquals(Integer.valueOf(1), queryList3.get(0));
        assertEquals(Integer.valueOf(2), queryList3.get(1));

        Integer[] queryList3Array = (Integer[])params.get(3);
        assertNotNull(queryList3Array);
        assertEquals(2, queryList3Array.length);
        assertEquals(Integer.valueOf(1), queryList3Array[0]);
        assertEquals(Integer.valueOf(2), queryList3Array[1]);

        List<String> queryList4 = (List<String>)params.get(4);
        assertNotNull(queryList4);
        assertEquals(1, queryList4.size());
        assertEquals("", queryList4.get(0));

        List<String> queryList5 = (List<String>)params.get(5);
        assertNotNull(queryList5);
        assertEquals(0, queryList5.size());

        List<MyType<Integer>> queryList6 = (List<MyType<Integer>>)params.get(6);
        assertNotNull(queryList6);
        assertEquals(2, queryList6.size());
        assertEquals(Integer.valueOf(1), queryList6.get(0).get());
        assertEquals(Integer.valueOf(2), queryList6.get(1).get());

        List<Long> queryList7 = (List<Long>)params.get(7);
        assertNotNull(queryList7);
        assertEquals(2, queryList7.size());
        assertEquals(1L, queryList7.get(0).longValue());
        assertEquals(2L, queryList7.get(1).longValue());

        List<Double> queryList8 = (List<Double>)params.get(8);
        assertNotNull(queryList8);
        assertEquals(2, queryList8.size());
        assertEquals(1., queryList8.get(0), 0.);
        assertEquals(2., queryList8.get(1), 0.);
    }

    @Test
    public void testCookieParameters() throws Exception {
        Class<?>[] argType = {String.class, Set.class, String.class, Set.class};
        Method m = Customer.class.getMethod("testCookieParam", argType);
        Message messageImpl = createMessage();
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.add("Cookie", "c1=c1Value");
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(4, params.size());
        assertEquals("c1Value", params.get(0));
        Set<Cookie> set1 = CastUtils.cast((Set<?>)params.get(1));
        assertEquals(1, set1.size());
        assertTrue(set1.contains(Cookie.valueOf("c1=c1Value")));
        assertEquals("c2Value", params.get(2));
        Set<Cookie> set2 = CastUtils.cast((Set<?>)params.get(3));
        assertTrue(set2.contains((Object)"c2Value"));
        assertEquals(1, set2.size());

    }

    @Test
    public void testMultipleCookieParameters() throws Exception {
        Class<?>[] argType = {String.class, String.class, Cookie.class};
        Method m = Customer.class.getMethod("testMultipleCookieParam", argType);
        Message messageImpl = createMessage();
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.add("Cookie", "c1=c1Value; c2=c2Value");
        headers.add("Cookie", "c3=c3Value");
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(3, params.size());
        assertEquals("c1Value", params.get(0));
        assertEquals("c2Value", params.get(1));
        assertEquals("c3Value", ((Cookie)params.get(2)).getValue());
    }

    @Test
    public void testFromStringParameters() throws Exception {
        Class<?>[] argType = {UUID.class, CustomerGender.class, CustomerGender.class};
        Method m = Customer.class.getMethod("testFromStringParam", argType);
        UUID u = UUID.randomUUID();
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "p1=" + u.toString() + "&p2=1&p3=2");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(3, params.size());
        assertEquals("Query UUID Parameter was not matched correctly",
                     u.toString(), params.get(0).toString());
        assertSame(CustomerGender.FEMALE, params.get(1));
        assertSame(CustomerGender.MALE, params.get(2));
    }

    @Test
    public void testFromValueEnum() throws Exception {
        Class<?>[] argType = {Timezone.class};
        Method m = Customer.class.getMethod("testFromValueParam", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "p1=Europe%2FLondon");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(1, params.size());
        assertSame("Timezone Parameter was not processed correctly",
                   Timezone.EUROPE_LONDON, params.get(0));
    }

    @Test
    public void testCustomerParameter() throws Exception {
        Message messageImpl = createMessage();
        ServerProviderFactory.getInstance(messageImpl).registerUserProvider(
            new CustomerParameterHandler());
        Class<?>[] argType = {Customer.class, Customer[].class, Customer2.class};
        Method m = Customer.class.getMethod("testCustomerParam", argType);

        messageImpl.put(Message.QUERY_STRING, "p1=Fred&p2=Barry&p3=Jack&p4=John");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(3, params.size());
        Customer c = (Customer)params.get(0);
        assertEquals("Fred", c.getName());
        Customer c2 = ((Customer[])params.get(1))[0];
        assertEquals("Barry", c2.getName());
        Customer2 c3 = (Customer2)params.get(2);
        assertEquals("Jack", c3.getName());

        try {
            messageImpl.put(Message.QUERY_STRING, "p3=noName");
            JAXRSUtils.processParameters(new OperationResourceInfo(m, null), null, messageImpl);
            fail("Customer2 constructor does not accept names starting with lower-case chars");
        } catch (Exception ex) {
            // expected
        }

    }

    @Test
    public void testLocaleParameter() throws Exception {
        Message messageImpl = createMessage();
        ProviderFactory.getInstance(messageImpl).registerUserProvider(
            new LocaleParameterHandler());
        Class<?>[] argType = {Locale.class};
        Method m = Customer.class.getMethod("testLocaleParam", argType);

        messageImpl.put(Message.QUERY_STRING, "p1=en_us");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(1, params.size());
        Locale l = (Locale)params.get(0);
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());
    }

    @Test
    public void testQueryParameter() throws Exception {
        Message messageImpl = createMessage();
        ProviderFactory.getInstance(messageImpl).registerUserProvider(
            new GenericObjectParameterHandler());
        Class<?>[] argType = {Query.class};
        Method m = Customer.class.getMethod("testGenericObjectParam", argType);

        messageImpl.put(Message.QUERY_STRING, "p1=thequery");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(1, params.size());
        @SuppressWarnings("unchecked")
        Query<String> query = (Query<String>)params.get(0);
        assertEquals("thequery", query.getEntity());
    }

    @Test
    public void testQueryParameterDefaultValue() throws Exception {
        Message messageImpl = createMessage();
        ProviderFactory.getInstance(messageImpl).registerUserProvider(
            new GenericObjectParameterHandler());
        Class<?>[] argType = {String.class, String.class};
        Method m = Customer.class.getMethod("testGenericObjectParamDefaultValue", argType);

        messageImpl.put(Message.QUERY_STRING, "p1=thequery&p2");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(2, params.size());
        String query = (String)params.get(0);
        assertEquals("thequery", query);
        query = (String)params.get(1);
        assertEquals("thequery", query);
    }

    @Test
    public void testArrayParamNoProvider() throws Exception {
        Message messageImpl = createMessage();
        Class<?>[] argType = {String[].class};
        Method m = Customer.class.getMethod("testCustomerParam2", argType);

        messageImpl.put(Message.QUERY_STRING, "p1=Fred&p1=Barry");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals(1, params.size());
        String[] values = (String[])params.get(0);
        assertEquals("Fred", values[0]);
        assertEquals("Barry", values[1]);
    }

    @Test
    public void testWrongType() throws Exception {
        Class<?>[] argType = {HashMap.class};
        Method m = Customer.class.getMethod("testWrongType", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "p1=1");
        try {
            JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                             new ClassResourceInfo(Customer.class)),
                                         null,
                                         messageImpl);
            fail("HashMap can not be handled as parameter");
        } catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals("Parameter Class java.util.HashMap has no constructor with "
                         + "single String parameter, static valueOf(String) or fromString(String) methods",
                         ex.getResponse().getEntity().toString());
        }

    }

    @Test
    public void testExceptionDuringConstruction() throws Exception {
        Class<?>[] argType = {CustomerGender.class};
        Method m = Customer.class.getMethod("testWrongType2", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "p1=3");
        try {
            JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                             new ClassResourceInfo(Customer.class)),
                                         null,
                                         messageImpl);
            fail("CustomerGender have no instance with name 3");
        } catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
        }

    }


    @Test
    public void testQueryParametersBean() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testQueryBean", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "a=aValue&b=123");

        Message complexMessageImpl = createMessage();
        complexMessageImpl.put(Message.QUERY_STRING, "c=1&a=A&b=123&c=2&c=3&"
                                + "d.c=4&d.a=B&d.b=456&d.c=5&d.c=6&"
                                + "e.c=41&e.a=B1&e.b=457&e.c=51&e.c=61&"
                                + "e.c=42&e.a=B2&e.b=458&e.c=52&e.c=62&"
                                + "d.d.c=7&d.d.a=C&d.d.b=789&d.d.c=8&d.d.c=9&"
                                + "d.e.c=71&d.e.a=C1&d.e.b=790&d.e.c=81&d.e.c=91&"
                                + "d.e.c=72&d.e.a=C2&d.e.b=791&d.e.c=82&d.e.c=92");

        verifyParametersBean(m, null, messageImpl, null, complexMessageImpl);
    }

    @Test
    public void testXmlAdapterBean() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testXmlAdapter", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "a=aValue");

        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals(1, params.size());

        Customer.CustomerBean bean = (Customer.CustomerBean)params.get(0);
        assertEquals("aValue", bean.getA());
    }

    @Test
    public void testXmlAdapterBean2() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testXmlAdapter2", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "a=aValue");

        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals(1, params.size());

        Customer.CustomerBean bean = (Customer.CustomerBean)params.get(0);
        assertEquals("aValue", bean.getA());
    }

    @Test
    public void testXmlAdapterBean3() throws Exception {
        Class<?>[] argType = {Customer.CustomerBeanInterface.class};
        Method m = Customer.class.getMethod("testXmlAdapter3", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "a=aValue");

        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals(1, params.size());

        Customer.CustomerBean bean = (Customer.CustomerBean)params.get(0);
        assertEquals("aValue", bean.getA());
    }
    @Test
    public void testXmlAdapterString() throws Exception {
        Method m = Customer.class.getMethod("testXmlAdapter4", String.class);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "a=3");

        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals(1, params.size());

        String ret = (String)params.get(0);
        assertEquals("Val: 3", ret);
    }


    @Test
    public void testPathParametersBean() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testPathBean", argType);

        MultivaluedMap<String, String> pathTemplates = new MetadataMap<>();
        pathTemplates.add("a", "aValue");
        pathTemplates.add("b", "123");

        MultivaluedMap<String, String> complexPathTemplates = new MetadataMap<>();
        complexPathTemplates.add("c", "1");
        complexPathTemplates.add("a", "A");
        complexPathTemplates.add("b", "123");
        complexPathTemplates.add("c", "2");
        complexPathTemplates.add("c", "3");

        complexPathTemplates.add("d.c", "4");
        complexPathTemplates.add("d.a", "B");
        complexPathTemplates.add("d.b", "456");
        complexPathTemplates.add("d.c", "5");
        complexPathTemplates.add("d.c", "6");

        complexPathTemplates.add("e.c", "41");
        complexPathTemplates.add("e.a", "B1");
        complexPathTemplates.add("e.b", "457");
        complexPathTemplates.add("e.c", "51");
        complexPathTemplates.add("e.c", "61");

        complexPathTemplates.add("e.c", "42");
        complexPathTemplates.add("e.a", "B2");
        complexPathTemplates.add("e.b", "458");
        complexPathTemplates.add("e.c", "52");
        complexPathTemplates.add("e.c", "62");

        complexPathTemplates.add("d.d.c", "7");
        complexPathTemplates.add("d.d.a", "C");
        complexPathTemplates.add("d.d.b", "789");
        complexPathTemplates.add("d.d.c", "8");
        complexPathTemplates.add("d.d.c", "9");

        complexPathTemplates.add("d.e.c", "71");
        complexPathTemplates.add("d.e.a", "C1");
        complexPathTemplates.add("d.e.b", "790");
        complexPathTemplates.add("d.e.c", "81");
        complexPathTemplates.add("d.e.c", "91");

        complexPathTemplates.add("d.e.c", "72");
        complexPathTemplates.add("d.e.a", "C2");
        complexPathTemplates.add("d.e.b", "791");
        complexPathTemplates.add("d.e.c", "82");
        complexPathTemplates.add("d.e.c", "92");

        verifyParametersBean(m, pathTemplates, createMessage(), complexPathTemplates, createMessage());
    }

    @Test
    public void testMatrixParametersBean() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testMatrixBean", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.REQUEST_URI, "/bar;a=aValue/baz;b=123");

        Message complexMessageImpl = createMessage();
        complexMessageImpl.put(Message.REQUEST_URI, "/bar;c=1/bar;a=A/bar;b=123/bar;c=2/bar;c=3"
                                + "/bar;d.c=4/bar;d.a=B/bar;d.b=456/bar;d.c=5/bar;d.c=6"
                                + "/bar;e.c=41/bar;e.a=B1/bar;e.b=457/bar;e.c=51/bar;e.c=61"
                                + "/bar;e.c=42/bar;e.a=B2/bar;e.b=458/bar;e.c=52/bar;e.c=62"
                                + "/bar;d.d.c=7/bar;d.d.a=C/bar;d.d.b=789/bar;d.d.c=8/bar;d.d.c=9"
                                + "/bar;d.e.c=71/bar;d.e.a=C1/bar;d.e.b=790/bar;d.e.c=81/bar;d.e.c=91"
                                + "/bar;d.e.c=72/bar;d.e.a=C2/bar;d.e.b=791/bar;d.e.c=82/bar;d.e.c=92");

        verifyParametersBean(m, null, messageImpl, null, complexMessageImpl);
    }

    @Test
    public void testFormParametersBeanWithBoolean() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testFormBean", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.REQUEST_URI, "/bar");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        String body = "a=aValue&b=123&cb=true";
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals("Bean should be created", 1, params.size());
        Customer.CustomerBean cb = (Customer.CustomerBean)params.get(0);
        assertNotNull(cb);

        assertEquals("aValue", cb.getA());
        assertEquals(Long.valueOf(123), cb.getB());
        assertTrue(cb.isCb());
    }

    @Test
    public void testFormParametersBean() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testFormBean", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.REQUEST_URI, "/bar");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        String body = "a=aValue&b=123&cb=true";
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        Message complexMessageImpl = createMessage();
        complexMessageImpl.put(Message.REQUEST_URI, "/bar");
        complexMessageImpl.put(Message.PROTOCOL_HEADERS, headers);
        body = "c=1&a=A&b=123&c=2&c=3&"
                                + "d.c=4&d.a=B&d.b=456&d.c=5&d.c=6&"
                                + "e.c=41&e.a=B1&e.b=457&e.c=51&e.c=61&"
                                + "e.c=42&e.a=B2&e.b=458&e.c=52&e.c=62&"
                                + "d.d.c=7&d.d.a=C&d.d.b=789&d.d.c=8&d.d.c=9&"
                                + "d.e.c=71&d.e.a=C1&d.e.b=790&d.e.c=81&d.e.c=91&"
                                + "d.e.c=72&d.e.a=C2&d.e.b=791&d.e.c=82&d.e.c=92";
        complexMessageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        verifyParametersBean(m, null, messageImpl, null, complexMessageImpl);
    }

    @Test
    public void testFormParametersBeanWithMap() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testFormBean", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.REQUEST_URI, "/bar");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        String body = "g.b=1&g.b=2";
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null,
                                                           messageImpl);
        assertEquals("Bean should be created", 1, params.size());
        Customer.CustomerBean cb = (Customer.CustomerBean)params.get(0);
        assertNotNull(cb);
        assertNotNull(cb.getG());
        List<String> values = cb.getG().get("b");
        assertEquals(2, values.size());
        assertEquals("1", values.get(0));
        assertEquals("2", values.get(1));

    }

    private void verifyParametersBean(Method m,
                                      MultivaluedMap<String, String> simpleValues,
                                      Message simpleMessageImpl,
                                      MultivaluedMap<String, String> complexValues,
                                      Message complexMessageImpl) throws Exception {
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           simpleValues,
                                                           simpleMessageImpl);
        assertEquals("Bean should be created", 1, params.size());
        Customer.CustomerBean cb = (Customer.CustomerBean)params.get(0);
        assertNotNull(cb);

        assertEquals("aValue", cb.getA());
        assertEquals(Long.valueOf(123), cb.getB());

        params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                  new ClassResourceInfo(Customer.class)),
                                              complexValues,
                                              complexMessageImpl);
        assertEquals("Bean should be created", 1, params.size());
        Customer.CustomerBean cb1 = (Customer.CustomerBean)params.get(0);
        assertNotNull(cb1);

        assertEquals("A", cb1.getA());
        assertEquals(Long.valueOf(123), cb1.getB());
        List<String> list1 = cb1.getC();
        assertEquals(3, list1.size());
        assertEquals("1", list1.get(0));
        assertEquals("2", list1.get(1));
        assertEquals("3", list1.get(2));

        Customer.CustomerBean cb2 = cb1.getD();
        assertNotNull(cb2);

        assertEquals("B", cb2.getA());
        assertEquals(Long.valueOf(456), cb2.getB());
        List<String> list2 = cb2.getC();
        assertEquals(3, list2.size());
        assertEquals("4", list2.get(0));
        assertEquals("5", list2.get(1));
        assertEquals("6", list2.get(2));

        List<Customer.CustomerBean> cb2List = cb1.e;
        assertEquals(2, cb2List.size());

        int idx = 1;
        for (Customer.CustomerBean cb2E : cb2List) {
            assertNotNull(cb2E);

            assertEquals("B" + idx, cb2E.getA());
            assertEquals(Long.valueOf(456 + idx), cb2E.getB());
            // ensure C was stripped properly since lists within lists are not supported
            assertNull(cb2E.getC());
            assertNull(cb2E.getD());
            assertNull(cb2E.e);

            idx++;
        }

        Customer.CustomerBean cb3 = cb2.getD();
        assertNotNull(cb3);

        assertEquals("C", cb3.getA());
        assertEquals(Long.valueOf(789), cb3.getB());
        List<String> list3 = cb3.getC();
        assertEquals(3, list3.size());
        assertEquals("7", list3.get(0));
        assertEquals("8", list3.get(1));
        assertEquals("9", list3.get(2));

        List<Customer.CustomerBean> cb3List = cb2.e;
        assertEquals(2, cb3List.size());

        idx = 1;
        for (Customer.CustomerBean cb3E : cb3List) {
            assertNotNull(cb3E);

            assertEquals("C" + idx, cb3E.getA());
            assertEquals(Long.valueOf(789 + idx), cb3E.getB());
            // ensure C was stripped properly since lists within lists are not supported
            assertNull(cb3E.getC());
            assertNull(cb3E.getD());
            assertNull(cb3E.e);

            idx++;
        }
    }

    @Test
    public void testMultipleQueryParameters() throws Exception {
        Class<?>[] argType = {String.class, String.class, Long.class,
                              Boolean.TYPE, char.class, String.class, Boolean.class, String.class};
        Method m = Customer.class.getMethod("testMultipleQuery", argType);
        Message messageImpl = createMessage();

        messageImpl.put(Message.QUERY_STRING,
                        "query=first&query2=second&query3=3&query4=true&query6=&query7=true&query8");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals("First Query Parameter of multiple was not matched correctly", "first",
                     params.get(0));
        assertEquals("Second Query Parameter of multiple was not matched correctly",
                     "second", params.get(1));
        assertEquals("Third Query Parameter of multiple was not matched correctly",
                    3L, params.get(2));
        assertSame("Fourth Query Parameter of multiple was not matched correctly",
                     Boolean.TRUE, params.get(3));
        assertEquals("Fifth Query Parameter of multiple was not matched correctly",
                     '\u0000', params.get(4));
        assertEquals("Sixth Query Parameter of multiple was not matched correctly",
                     "", params.get(5));
        assertSame("Seventh Query Parameter of multiple was not matched correctly",
                Boolean.TRUE, params.get(6));
        assertNull("Eighth Query Parameter of multiple was not matched correctly",
                params.get(7));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMatrixParameters() throws Exception {
        Class<?>[] argType = {String.class, String.class, String.class, String.class,
                              List.class, String.class};
        Method m = Customer.class.getMethod("testMatrixParam", argType);
        Message messageImpl = createMessage();

        messageImpl.put(Message.REQUEST_URI, "/foo;p4=0;p3=3/bar;p1=1;p2=/baz;p4=4;p4=5;p5");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals("5 Matrix params should've been identified", 6, params.size());

        assertEquals("First Matrix Parameter not matched correctly",
                     "1", params.get(0));
        assertEquals("Second Matrix Parameter was not matched correctly",
                     "", params.get(1));
        assertEquals("Third Matrix Parameter was not matched correctly",
                     "3", params.get(2));
        assertEquals("Fourth Matrix Parameter was not matched correctly",
                     "0", params.get(3));
        List<String> list = (List<String>)params.get(4);
        assertEquals(3, list.size());
        assertEquals("0", list.get(0));
        assertEquals("4", list.get(1));
        assertEquals("5", list.get(2));
        assertNull("Sixth Matrix Parameter was not matched correctly",
                     params.get(5));
    }

    @Test
    public void testMatrixAndPathSegmentParameters() throws Exception {
        Class<?>[] argType = {PathSegment.class, String.class};
        Method m = Customer.class.getMethod("testPathSegment", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.REQUEST_URI, "/bar%20foo;p4=0%201");
        MultivaluedMap<String, String> values = new MetadataMap<>();
        values.add("ps", "bar%20foo;p4=0%201");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           values,
                                                           messageImpl);
        assertEquals("2 params should've been identified", 2, params.size());

        PathSegment ps = (PathSegment)params.get(0);
        assertEquals("bar foo", ps.getPath());
        assertEquals(1, ps.getMatrixParameters().size());
        assertEquals("0 1", ps.getMatrixParameters().getFirst("p4"));
        assertEquals("bar foo", params.get(1));
    }

    @Test
    public void testFormParameters() throws Exception {
        doTestFormParameters(true);
    }

    @Test
    public void testFormParametersWithoutMediaType() throws Exception {
        doTestFormParameters(false);
    }

    @SuppressWarnings("unchecked")
    private void doTestFormParameters(boolean useMediaType) throws Exception {
        Class<?>[] argType = {String.class, List.class};
        Method m = Customer.class.getMethod("testFormParam", argType);
        Message messageImpl = createMessage();
        String body = "p1=hello%2bworld&p2=2&p2=3";
        messageImpl.put(Message.REQUEST_URI, "/foo");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        if (useMediaType) {
            headers.putSingle("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        }
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           null, messageImpl);
        assertEquals("2 form params should've been identified", 2, params.size());

        assertEquals("First Form Parameter not matched correctly",
                     "hello+world", params.get(0));
        List<String> list = (List<String>)params.get(1);
        assertEquals(2, list.size());
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFormParametersAndMap() throws Exception {
        Class<?>[] argType = {MultivaluedMap.class, String.class, List.class};
        Method m = Customer.class.getMethod("testMultivaluedMapAndFormParam", argType);
        final Message messageImpl = createMessage();
        String body = "p1=1&p2=2&p2=3";
        messageImpl.put(Message.REQUEST_URI, "/foo");
        messageImpl.put("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        ProviderFactory.getInstance(messageImpl).registerUserProvider(
            new FormEncodingProvider<Object>() {
                @Override
                protected void persistParamsOnMessage(MultivaluedMap<String, String> params) {
                    messageImpl.put(FormUtils.FORM_PARAM_MAP, params);
                }
            });

        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           new MetadataMap<String, String>(), messageImpl);
        assertEquals("3 params should've been identified", 3, params.size());

        MultivaluedMap<String, String> map = (MultivaluedMap<String, String>)params.get(0);
        assertEquals(2, map.size());
        assertEquals(1, map.get("p1").size());
        assertEquals("First map parameter not matched correctly",
                     "1", map.getFirst("p1"));
        assertEquals(2, map.get("p2").size());

        assertEquals("2", map.get("p2").get(0));
        assertEquals("3", map.get("p2").get(1));

        assertEquals("First Form Parameter not matched correctly",
                     "1", params.get(1));
        List<String> list = (List<String>)params.get(2);
        assertEquals(2, list.size());
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
    }

    @Test
    public void testEncodedFormParameters() throws Exception {
        Class<?>[] argType = {String.class, String.class};
        Method m = Customer.class.getMethod("testEncodedFormParams", argType);
        final Message messageImpl = createMessage();
        String body = "p1=yay&p2=%21";
        messageImpl.put(Message.REQUEST_URI, "/foo");
        messageImpl.put("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m,
                                                               new ClassResourceInfo(Customer.class)),
                                                           new MetadataMap<String, String>(), messageImpl);
        assertEquals("2 params should've been identified", 2, params.size());
        assertEquals("yay", (String)params.get(0));
        assertEquals("%21", (String)params.get(1)); // if decoded, this will return "!" instead of "%21"
    }

    private static Map<ClassResourceInfo, MultivaluedMap<String, String>> getMap(ClassResourceInfo cri) {
        return Collections.singletonMap(cri, new MetadataMap<String, String>());
    }

    @Test
    public void testSelectResourceMethod() throws Exception {
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class);
        OperationResourceInfo ori1 = new OperationResourceInfo(
                                         Customer.class.getMethod("getItAsXML", new Class[]{}),
                                         cri);
        ori1.setHttpMethod("GET");
        ori1.setURITemplate(new URITemplate("/"));
        OperationResourceInfo ori2 = new OperationResourceInfo(
                                         Customer.class.getMethod("getItPlain", new Class[]{}),
                                         cri);
        ori2.setHttpMethod("GET");
        ori2.setURITemplate(new URITemplate("/"));
        MethodDispatcher md = new MethodDispatcher();
        md.bind(ori1, Customer.class.getMethod("getItAsXML", new Class[]{}));
        md.bind(ori2, Customer.class.getMethod("getItPlain", new Class[]{}));
        cri.setMethodDispatcher(md);

        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(getMap(cri), createMessage(), "GET",
              new MetadataMap<String, String>(), "*/*", getTypes("text/plain"));

        assertSame(ori, ori2);

        ori = JAXRSUtils.findTargetMethod(getMap(cri), createMessage(), "GET", new MetadataMap<String, String>(),
                                              "*/*", getTypes("text/xml"));

        assertSame(ori, ori1);

        ori = JAXRSUtils.findTargetMethod(getMap(cri), createMessage(), "GET", new MetadataMap<String, String>(),
                                          "*/*",
                                          sortMediaTypes(getTypes("*/*;q=0.1,text/plain,text/xml;q=0.8")));

        assertSame(ori, ori2);
        ori = JAXRSUtils.findTargetMethod(getMap(cri), createMessage(), "GET", new MetadataMap<String, String>(),
                                          "*/*",
                                          sortMediaTypes(getTypes("*;q=0.1,text/plain,text/xml;q=0.9,x/y")));

        assertSame(ori, ori2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHttpContextParameters() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori =
            new OperationResourceInfo(
                Customer.class.getMethod("testParams",
                                         new Class[]{UriInfo.class,
                                                     HttpHeaders.class,
                                                     Request.class,
                                                     SecurityContext.class,
                                                     Providers.class,
                                                     String.class,
                                                     List.class}),
                cri);
        ori.setHttpMethod("GET");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.add("Foo", "bar, baz");

        Message m = createMessage();
        m.put("org.apache.cxf.http.header.split", "true");
        m.put(Message.PROTOCOL_HEADERS, headers);

        List<Object> params =
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("7 parameters expected", 7, params.size());
        assertSame(UriInfoImpl.class, params.get(0).getClass());
        assertSame(HttpHeadersImpl.class, params.get(1).getClass());
        assertSame(RequestImpl.class, params.get(2).getClass());
        assertSame(SecurityContextImpl.class, params.get(3).getClass());
        assertSame(ProvidersImpl.class, params.get(4).getClass());
        assertSame(String.class, params.get(5).getClass());
        assertEquals("Wrong header param", "bar", params.get(5));
        List<String> values = (List<String>)params.get(6);
        assertEquals("Wrong headers size", 2, values.size());
        assertEquals("Wrong 1st header param", "bar", values.get(0));
        assertEquals("Wrong 2nd header param", "baz", values.get(1));
    }

    @Test
    public void testHttpContextParametersFromInterface() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Method methodToInvoke =
            Customer.class.getMethod("setUriInfoContext",
                                     new Class[]{UriInfo.class});
        OperationResourceInfo ori =
            new OperationResourceInfo(methodToInvoke,
                AnnotationUtils.getAnnotatedMethod(Customer.class, methodToInvoke), cri);
        ori.setHttpMethod("GET");

        Message m = new MessageImpl();

        List<Object> params =
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("1 parameters expected", 1, params.size());
        assertSame(UriInfoImpl.class, params.get(0).getClass());
    }

    @Test
    public void testServletContextParameters() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori =
            new OperationResourceInfo(
                Customer.class.getMethod("testServletParams",
                                         new Class[]{HttpServletRequest.class,
                                                     HttpServletResponse.class,
                                                     ServletContext.class,
                                                     ServletConfig.class}),
                cri);
        ori.setHttpMethod("GET");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = new HttpServletResponseFilter(
                                           mock(HttpServletResponse.class), null);
        ServletContext context = mock(ServletContext.class);
        ServletConfig config = mock(ServletConfig.class);

        Message m = createMessage();
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, context);
        m.put(AbstractHTTPDestination.HTTP_CONFIG, config);

        List<Object> params =
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("4 parameters expected", 4, params.size());
        assertSame(request.getClass(), ((HttpServletRequestFilter)params.get(0)).getRequest().getClass());
        assertSame(response.getClass(), params.get(1).getClass());
        assertSame(context.getClass(), params.get(2).getClass());
        assertSame(config.getClass(), params.get(3).getClass());

    }

    @Test
    public void testPerRequestContextFields() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        cri.setResourceProvider(new PerRequestResourceProvider(Customer.class));
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethod("postConstruct",
                                                                                       new Class[]{}), cri);

        Customer c = new Customer();

        Message m = createMessage();
        m.put(Message.PROTOCOL_HEADERS, new HashMap<String, List<String>>());
        HttpServletResponse response = mock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);

        InjectionUtils.injectContextFields(c, ori.getClassResourceInfo(), m);
        assertSame(UriInfoImpl.class, c.getUriInfo2().getClass());
        assertSame(HttpHeadersImpl.class, c.getHeaders().getClass());
        assertSame(RequestImpl.class, c.getRequest().getClass());
        assertSame(SecurityContextImpl.class, c.getSecurityContext().getClass());
        assertSame(ProvidersImpl.class, c.getBodyWorkers().getClass());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSingletonContextFields() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        cri.setResourceProvider(new SingletonResourceProvider(c));

        Message m = createMessage();
        m.put(Message.PROTOCOL_HEADERS, new HashMap<String, List<String>>());
        ServletContext servletContextMock = mock(ServletContext.class);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, servletContextMock);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, httpRequest);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, httpResponse);

        InjectionUtils.injectContextProxies(cri, cri.getResourceProvider().getInstance(null));
        InjectionUtils.injectContextFields(c, cri, m);
        InjectionUtils.injectContextMethods(c, cri, m);
        assertSame(ThreadLocalUriInfo.class, c.getUriInfo2().getClass());
        assertSame(UriInfoImpl.class,
                   ((ThreadLocalProxy<UriInfo>)c.getUriInfo2()).get().getClass());
        assertSame(HttpHeadersImpl.class,
                   ((ThreadLocalProxy<HttpHeaders>)c.getHeaders()).get().getClass());
        assertSame(RequestImpl.class,
                   ((ThreadLocalProxy<Request>)c.getRequest()).get().getClass());
        assertSame(ResourceInfoImpl.class,
                   ((ThreadLocalProxy<ResourceInfo>)c.getResourceInfo()).get().getClass());
        assertSame(SecurityContextImpl.class,
                   ((ThreadLocalProxy<SecurityContext>)c.getSecurityContext()).get().getClass());
        assertSame(ProvidersImpl.class,
                   ((ThreadLocalProxy<Providers>)c.getBodyWorkers()).get().getClass());

        assertSame(servletContextMock,
                   ((ThreadLocalProxy<ServletContext>)c.getThreadLocalServletContext()).get());
        assertSame(servletContextMock,
                   ((ThreadLocalProxy<ServletContext>)c.getServletContext()).get());
        assertSame(servletContextMock,
                   ((ThreadLocalProxy<ServletContext>)c.getSuperServletContext()).get());
        HttpServletRequest currentReq =
            ((ThreadLocalProxy<HttpServletRequest>)c.getServletRequest()).get();
        assertSame(httpRequest,
                   ((HttpServletRequestFilter)currentReq).getRequest());
        HttpServletResponseFilter filter = (
            HttpServletResponseFilter)((ThreadLocalProxy<HttpServletResponse>)c.getServletResponse()).get();
        assertSame(httpResponse, filter.getResponse());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSingletonHttpResourceFields() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        cri.setResourceProvider(new SingletonResourceProvider(c));

        Message m = createMessage();
        ServletContext servletContextMock = mock(ServletContext.class);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, servletContextMock);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, httpRequest);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, httpResponse);
        InjectionUtils.injectContextProxies(cri, cri.getResourceProvider().getInstance(null));
        InjectionUtils.injectContextFields(c, cri, m);
        assertSame(servletContextMock,
                   ((ThreadLocalProxy<ServletContext>)c.getServletContextResource()).get());
        HttpServletRequest currentReq =
            ((ThreadLocalProxy<HttpServletRequest>)c.getServletRequestResource()).get();
        assertSame(httpRequest,
                   ((HttpServletRequestFilter)currentReq).getRequest());
        HttpServletResponseFilter filter = (
            HttpServletResponseFilter)((ThreadLocalProxy<HttpServletResponse>)c.getServletResponseResource())
                .get();
        assertSame(httpResponse, filter.getResponse());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testContextAnnotationOnMethod() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        cri.setResourceProvider(new SingletonResourceProvider(c));
        InjectionUtils.injectContextProxies(cri, cri.getResourceProvider().getInstance(null));

        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethods()[0],
                                                              cri);
        Message message = createMessage();
        InjectionUtils.injectContextMethods(c, ori.getClassResourceInfo(), message);
        assertNotNull(c.getUriInfo());
        assertSame(ThreadLocalUriInfo.class, c.getUriInfo().getClass());
        assertSame(UriInfoImpl.class,
                   ((ThreadLocalProxy<UriInfo>)c.getUriInfo()).get().getClass());
        assertSame(ThreadLocalServletConfig.class, c.getSuperServletConfig().getClass());
        assertSame(ThreadLocalHttpServletRequest.class, c.getHttpServletRequest().getClass());
    }

    @Test
    public void testParamAnnotationOnMethod() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethods()[0],
                                                              cri);
        Message m = createMessage();
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.add("AHeader2", "theAHeader2");
        m.put(Message.PROTOCOL_HEADERS, headers);
        m.put(Message.QUERY_STRING, "a_value=aValue&query2=b");
        JAXRSUtils.injectParameters(ori, c, m);
        assertEquals("aValue", c.getQueryParam());
        assertEquals("theAHeader2", c.getAHeader2());
    }

    @Test
    public void testParamAnnotationOnField() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethods()[0],
                                                              cri);
        Message m = createMessage();

        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.add("AHeader", "theAHeader");
        m.put(Message.PROTOCOL_HEADERS, headers);
        m.put(Message.QUERY_STRING, "b=bValue");
        JAXRSUtils.injectParameters(ori, c, m);
        assertEquals("bValue", c.getB());
        assertEquals("theAHeader", c.getAHeader());
    }

    @Test
    public void testDefaultValueOnField() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethods()[0],
                                                              cri);
        Message m = createMessage();

        m.put(Message.QUERY_STRING, "");
        JAXRSUtils.injectParameters(ori, c, m);
        assertEquals("bQuery", c.getB());
    }

    @Test
    public void testContextResolverParam() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori =
            new OperationResourceInfo(
                Customer.class.getMethod("testContextResolvers",
                                         new Class[]{ContextResolver.class}),
                                         cri);
        ori.setHttpMethod("GET");

        Message m = createMessage();
        ContextResolver<JAXBContext> cr = new JAXBContextProvider();
        ProviderFactory.getInstance(m).registerUserProvider(cr);

        m.put(Message.BASE_PATH, "/");
        List<Object> params =
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("1 parameters expected", 1, params.size());
        assertSame(cr.getClass(), params.get(0).getClass());
    }

    @Test
    public void testContextResolverFields() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        cri.setResourceProvider(new PerRequestResourceProvider(Customer.class));
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethod("postConstruct",
                                                                                       new Class[]{}), cri);

        Message m = createMessage();
        HttpServletResponse response = mock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        Customer c = new Customer();
        ContextResolver<JAXBContext> cr = new JAXBContextProvider();
        ProviderFactory.getInstance(m).registerUserProvider(cr);

        m.put(Message.BASE_PATH, "/");
        InjectionUtils.injectContextFields(c, ori.getClassResourceInfo(), m);
        assertSame(cr.getClass(), c.getContextResolver().getClass());
    }

    @Test
    public void testServletResourceFields() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        cri.setResourceProvider(new PerRequestResourceProvider(Customer.class));
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethod("postConstruct",
                                                                                       new Class[]{}),
                                                              cri);

        Customer c = new Customer();

        // Creating mocks for the servlet request, response and context
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletContext context = mock(ServletContext.class);

        Message m = createMessage();
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, context);

        InjectionUtils.injectContextFields(c, ori.getClassResourceInfo(), m);
        assertSame(request.getClass(),
                   ((HttpServletRequestFilter)c.getServletRequestResource()).getRequest().getClass());
        HttpServletResponseFilter filter = (HttpServletResponseFilter)c.getServletResponseResource();
        assertSame(response.getClass(), filter.getResponse().getClass());
        assertSame(context.getClass(), c.getServletContextResource().getClass());
        assertNotNull(c.getServletRequest());
        assertNotNull(c.getServletResponse());
        assertNotNull(c.getServletContext());
        assertNotNull(c.getServletRequestResource());
        assertNotNull(c.getServletResponseResource());
        assertNotNull(c.getServletContextResource());
        assertSame(request.getClass(),
                   ((HttpServletRequestFilter)c.getServletRequestResource()).getRequest().getClass());
        filter = (HttpServletResponseFilter)c.getServletResponse();
        assertSame(response.getClass(), filter.getResponse().getClass());
        assertSame(context.getClass(), c.getServletContext().getClass());
    }

    @Test
    public void testConversion() throws Exception {
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori =
            new OperationResourceInfo(
                Customer.class.getMethod("testConversion",
                                         new Class[]{PathSegmentImpl.class,
                                                     SimpleFactory.class}),
                cri);
        ori.setHttpMethod("GET");
        ori.setURITemplate(new URITemplate("{id1}/{id2}"));
        MultivaluedMap<String, String> values = new MetadataMap<>();
        values.putSingle("id1", "1");
        values.putSingle("id2", "2");

        Message m = createMessage();


        List<Object> params =
            JAXRSUtils.processParameters(ori, values, m);
        PathSegment ps = (PathSegment)params.get(0);
        assertEquals("1", ps.getPath());

        SimpleFactory sf = (SimpleFactory)params.get(1);
        assertEquals(2, sf.getId());
    }
    
    @Test
    public void testBeanParamsWithBooleanConverter() throws Exception {
        Class<?>[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testBeanParam", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.REQUEST_URI, "/bar");
        
        // The converter converts any Boolean to null
        ProviderFactory.getInstance(messageImpl)
            .registerUserProvider(new MyBoolParamConverterProvider());
        
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        String body = "value=true";
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        final ClassResourceInfo cri = new ClassResourceInfo(Customer.class);
        final MethodDispatcher md = new MethodDispatcher();
        
        final OperationResourceInfo ori = new OperationResourceInfo(m, cri);
        md.bind(ori, m);
        
        cri.setMethodDispatcher(md);
        cri.initBeanParamInfo(ServerProviderFactory.getInstance(messageImpl));

        List<Object> params = JAXRSUtils.processParameters(ori,
                                                           null,
                                                           messageImpl);
        assertEquals("Bean should be created", 1, params.size());
        Customer.CustomerBean cb = (Customer.CustomerBean)params.get(0);
        assertNotNull(cb);

        assertNull(cb.getBool());
    }

    private static OperationResourceInfo findTargetResourceClass(List<ClassResourceInfo> resources,
                                                                Message message,
                                                                String path,
                                                                String httpMethod,
                                                                MultivaluedMap<String, String> values,
                                                                String requestContentType,
                                                                List<MediaType> acceptContentTypes) {

        Map<ClassResourceInfo, MultivaluedMap<String, String>> mResources
            = JAXRSUtils.selectResourceClass(resources, path, new MessageImpl());

        if (mResources != null) {
            OperationResourceInfo ori = JAXRSUtils.findTargetMethod(mResources, message, httpMethod,
                                                   values, requestContentType, acceptContentTypes);
            if (ori != null) {
                return ori;
            }
        }

        return null;
    }

    private static Message createMessage() {
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = mockEndpoint();
        e.put(Endpoint.class, endpoint);
        return m;
    }

    private static Endpoint mockEndpoint() {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(null);
        when(endpoint.get(Application.class.getName())).thenReturn(null);
        when(endpoint.get("org.apache.cxf.jaxrs.comparator")).thenReturn(null);
        when(endpoint.size()).thenReturn(0);
        when(endpoint.isEmpty()).thenReturn(true);
        when(endpoint.get(ServerProviderFactory.class.getName()))
                .thenReturn(ServerProviderFactory.getInstance());
        return endpoint;
    }

    @SuppressWarnings("unchecked")
    static class MyBoolParamConverterProvider implements ParamConverterProvider {
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType.isAssignableFrom(Boolean.TYPE) || rawType.isAssignableFrom(Boolean.class)) {
                return (ParamConverter<T>) new ParamConverter<Boolean>() {
                    @Override
                    public Boolean fromString(String value) {
                        if (rawType.isAssignableFrom(Boolean.TYPE)) {
                            return true;
                        }
                        return null;
                    }
        
                    @Override
                    public String toString(Boolean value) {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            
            return null;
        }
    }
    
    
    static class MyTypeParamConverterProvider
        implements ParamConverterProvider, ParamConverter<MyType<Integer>> {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
                                                  Annotation[] annotations) {
            if (rawType == MyType.class) {
                Type type = ((ParameterizedType)genericType).getActualTypeArguments()[0];
                @SuppressWarnings("unchecked")
                ParamConverter<T> converter = (ParamConverter<T>)this;
                if (type == Integer.class) {
                    return converter;
                }
            }
            return null;
        }

        @Override
        public MyType<Integer> fromString(String value) {
            return new MyType<Integer>(Integer.valueOf(value));
        }

        @Override
        public String toString(MyType<Integer> value) {
            return null;
        }

    }
    private static final class LocaleParameterHandler implements ParamConverterProvider, ParamConverter<Locale> {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> cls, Type arg1, Annotation[] arg2) {
            if (cls == Locale.class) {
                return (ParamConverter<T>)this;
            }
            return null;
        }

        public Locale fromString(String s) {
            String[] values = s.split("_");
            return values.length == 2 ? new Locale(values[0], values[1]) : new Locale(s);
        }

        @Override
        public String toString(Locale arg0) throws IllegalArgumentException {
            return null;
        }

    }

    private static final class GenericObjectParameterHandler implements ParamConverterProvider,
        ParamConverter<Query<String>> {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> cls, Type arg1, Annotation[] arg2) {
            if (cls == Query.class) {
                return (ParamConverter<T>)this;
            }
            return null;
        }

        public Query<String> fromString(String s) {
            return new Query<String>(s);
        }

        @Override
        public String toString(Query<String> arg0) throws IllegalArgumentException {
            return null;
        }

    }


}
