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
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.FIStaxInInterceptor;
import org.apache.cxf.interceptor.FIStaxOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.transform.TransformInInterceptor;
import org.apache.cxf.interceptor.transform.TransformOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.jaxrs.jaxws.BookSoapService;
import org.apache.cxf.systest.jaxrs.jaxws.BookStoreJaxrsJaxws;
import org.apache.cxf.systest.jaxrs.jaxws.BookStoreSoapRestFastInfoset2;
import org.apache.cxf.systest.jaxrs.jaxws.BookStoreSoapRestFastInfoset3;
import org.apache.cxf.systest.jaxrs.jaxws.HelloWorld;
import org.apache.cxf.systest.jaxrs.jaxws.User;
import org.apache.cxf.systest.jaxrs.jaxws.UserImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSSoapBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerRestSoap.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerRestSoap.class, true));
    }

    @Test
    public void testHelloRest() throws Exception {
        String address = "http://localhost:" + PORT + "/test/services/hello-rest";

        HelloWorld service = JAXRSClientFactory.create(address, HelloWorld.class);
        useHelloService(service);
    }

    @Test
    public void testHelloSoap() throws Exception {
        final QName serviceName = new QName("http://hello.com", "HelloWorld");
        final QName portName = new QName("http://hello.com", "HelloWorldPort");
        final String address = "http://localhost:" + PORT + "/test/services/hello-soap";

        Service service = Service.create(serviceName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);

        HelloWorld hw = service.getPort(HelloWorld.class);

        useHelloService(hw);
    }

    @Test
    public void testHelloSoapCustomDataBindingJaxb() throws Exception {
        final String address = "http://localhost:" + PORT + "/test/services/hello-soap-databinding-jaxb";
        doTestHelloSoapCustomDataBinding(address);
    }

    @Test
    public void testHelloSoapCustomDataBindingJaxbXslt() throws Exception {
        final String address = "http://localhost:" + PORT + "/test/services/hello-soap-databinding-xslt";
        doTestHelloSoapCustomDataBinding(address);
    }

    private void doTestHelloSoapCustomDataBinding(String address) throws Exception {
        final QName serviceName = new QName("http://hello.com", "HelloWorld");
        final QName portName = new QName("http://hello.com", "HelloWorldPort");

        Service service = Service.create(serviceName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);

        HelloWorld hw = service.getPort(HelloWorld.class);

        Client cl = ClientProxy.getClient(hw);

        HTTPConduit http = (HTTPConduit) cl.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(0);
        httpClientPolicy.setReceiveTimeout(0);

        http.setClient(httpClientPolicy);

        User user = new UserImpl("Barry");
        User user2 = hw.echoUser(user);

        assertNotSame(user, user2);
        assertEquals("Barry", user2.getName());
    }

    private void useHelloService(HelloWorld service) {
        assertEquals("Hello Barry", service.sayHi("Barry"));
        assertEquals("Hello Fred", service.sayHiToUser(new UserImpl("Fred")));

        Map<Integer, User> users = service.getUsers();
        assertEquals(1, users.size());
        assertEquals("Fred", users.entrySet().iterator().next().getValue().getName());

        users = service.echoUsers(users);
        assertEquals(1, users.size());
        assertEquals("Fred", users.entrySet().iterator().next().getValue().getName());
    }

    @Test
    public void testGetAll() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/test/services/rest2/myRestService");

        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "text/plain");
        InputStream in = connect.getInputStream();

        assertEquals("0", getStringFromInputStream(in));

    }
    @Test
    public void testGetBookTransform() throws Exception {

        String address = "http://localhost:" + PORT
                         + "/test/v1/rest-transform/bookstore/books/123";
        WebClient client = WebClient.create(address);
        Response r = client.get();
        String str = getStringFromInputStream((InputStream)r.getEntity());
        assertTrue(str.contains("TheBook"));
    }

    @Test
    public void testPostBookTransform() throws Exception {

        String address = "http://localhost:" + PORT
                         + "/test/v1/rest-transform/bookstore/books";

        TransformOutInterceptor out = new TransformOutInterceptor();
        out.setOutTransformElements(
            Collections.singletonMap("{http://www.example.org/books}*",
                                     "{http://www.example.org/super-books}*"));

        TransformInInterceptor in = new TransformInInterceptor();
        Map<String, String> map = new HashMap<>();

        // If Book2 didn't have {http://www.example.org/books}Book
        // then we'd just do '"*" : "{http://www.example.org/books}*'
        // but given that we have TheBook being returned, we need
        map.put("TheBook", "{http://www.example.org/books}Book");
        map.put("id", "{http://www.example.org/books}id");
        in.setInTransformElements(map);

        WebClient client = WebClient.create(address);
        WebClient.getConfig(client).getInInterceptors().add(in);
        WebClient.getConfig(client).getOutInterceptors().add(out);
        Book2 book = client.type("application/xml").accept("text/xml").post(new Book2(), Book2.class);
        assertEquals(124L, book.getId());
    }

    @Test
    public void testPostBookTransformV2() throws Exception {

        String address = "http://localhost:" + PORT
                         + "/test/v2/rest-transform/bookstore/books";
        WebClient client = WebClient.create(address);
        Book book = client.type("application/xml").accept("text/xml").post(new Book(), Book.class);
        assertEquals(124L, book.getId());
    }


    @Test
    public void testGetBookFastinfoset() throws Exception {

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://localhost:" + PORT + "/test/services/rest3/bookstore/fastinfoset2");
        bean.getInInterceptors().add(new FIStaxInInterceptor());
        JAXBElementProvider<?> p = new JAXBElementProvider<>();
        p.setConsumeMediaTypes(Collections.singletonList("application/fastinfoset"));
        bean.setProvider(p);

        Map<String, Object> props = new HashMap<>();
        props.put(FIStaxInInterceptor.FI_GET_SUPPORTED, Boolean.TRUE);
        bean.setProperties(props);

        WebClient client = bean.createWebClient();
        Book b = client.accept("application/fastinfoset").get(Book.class);
        assertEquals("CXF2", b.getName());
        assertEquals(2L, b.getId());
    }

    @Test
    public void testPostGetBookFastinfoset() throws Exception {

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://localhost:" + PORT + "/test/services/rest3/bookstore/fastinfoset");
        bean.getOutInterceptors().add(new FIStaxOutInterceptor());
        bean.getInInterceptors().add(new FIStaxInInterceptor());
        JAXBElementProvider<?> p = new JAXBElementProvider<>();
        p.setConsumeMediaTypes(Collections.singletonList("application/fastinfoset"));
        p.setProduceMediaTypes(Collections.singletonList("application/fastinfoset"));
        bean.setProvider(p);

        Map<String, Object> props = new HashMap<>();
        props.put(FIStaxOutInterceptor.FI_ENABLED, Boolean.TRUE);
        bean.setProperties(props);

        WebClient client = bean.createWebClient();
        Book b = new Book("CXF", 1L);
        Book b2 = client.type("application/fastinfoset").accept("application/fastinfoset")
            .post(b, Book.class);
        assertEquals(b2.getName(), b.getName());
        assertEquals(b2.getId(), b.getId());
    }

    @Test
    public void testPostGetBookFastinfosetProxy() throws Exception {

        JAXBElementProvider<Object> p = new JAXBElementProvider<>();
        p.setConsumeMediaTypes(Collections.singletonList("application/fastinfoset"));
        p.setProduceMediaTypes(Collections.singletonList("application/fastinfoset"));

        BookStoreJaxrsJaxws client = JAXRSClientFactory.create(
                                  "http://localhost:" + PORT + "/test/services/rest4",
                                  BookStoreSoapRestFastInfoset2.class,
                                  Collections.singletonList(p));

        Book b = new Book("CXF", 1L);

        Book b2 = client.addFastinfoBook(b);

        assertEquals(b2.getName(), b.getName());
        assertEquals(b2.getId(), b.getId());

        checkFiInterceptors(WebClient.getConfig(client));
    }

    @Test
    public void testPostGetBookFastinfosetProxyInterceptors() throws Exception {

        JAXBElementProvider<Object> p = new JAXBElementProvider<>();
        p.setConsumeMediaTypes(Collections.singletonList("application/fastinfoset"));
        p.setProduceMediaTypes(Collections.singletonList("application/fastinfoset"));

        BookStoreJaxrsJaxws client = JAXRSClientFactory.create(
                                  "http://localhost:" + PORT + "/test/services/rest5",
                                  BookStoreSoapRestFastInfoset3.class,
                                  Collections.singletonList(p));

        Book b = new Book("CXF", 1L);

        // Just to make sure it is enforced
        Map<String, Object> props = WebClient.getConfig(client).getRequestContext();
        props.put(FIStaxOutInterceptor.FI_ENABLED, Boolean.TRUE);

        Book b2 = client.addFastinfoBook(b);

        assertEquals(b2.getName(), b.getName());
        assertEquals(b2.getId(), b.getId());

        checkFiInterceptors(WebClient.getConfig(client));

    }

    private void checkFiInterceptors(ClientConfiguration cfg) {
        int count = 0;
        for (Interceptor<?> in : cfg.getInInterceptors()) {
            if (in instanceof FIStaxInInterceptor) {
                count++;
                break;
            }
        }
        for (Interceptor<?> in : cfg.getOutInterceptors()) {
            if (in instanceof FIStaxOutInterceptor) {
                count++;
                break;
            }
        }
        assertEquals("In and Out FastInfoset interceptors are expected", 2, count);
    }

    @Test
    public void testGetBook123ServletResponse() throws Exception {

        InputStream in = getHttpInputStream("http://localhost:" + PORT + "/test/services/rest/bookstore/0");
        InputStream expected = getClass().getResourceAsStream("resources/expected_get_book123.txt");
        assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in)));

    }

    @Test
    public void testGetBook123() throws Exception {

        InputStream in = getHttpInputStream("http://localhost:" + PORT + "/test/services/rest/bookstore/123");

        InputStream expected = getClass().getResourceAsStream("resources/expected_get_book123.txt");
        assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in)));

    }

    @Test
    public void testGetBook123Client() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                                  BookStoreJaxrsJaxws.class);
        HTTPConduit conduit = (HTTPConduit)WebClient.getConfig(proxy).getConduit();

        Book b = proxy.getBook(Long.valueOf("123"));
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());

        HTTPConduit conduit2 = (HTTPConduit)WebClient.getConfig(proxy).getConduit();
        assertSame(conduit, conduit2);

        conduit.getClient().setAutoRedirect(true);
        b = proxy.getBook(Long.valueOf("123"));
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testGetBook123WebClient() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        WebClient client = WebClient.create(baseAddress);
        client.path("/bookstore/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = client.get(Book.class);
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testGetBook123XMLSource() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        WebClient client = WebClient.create(baseAddress);
        client.path("/bookstore/123").accept(MediaType.APPLICATION_XML_TYPE);
        XMLSource source = client.get(XMLSource.class);
        source.setBuffering();
        Book b = source.getNode("/Book", Book.class);
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
        b = source.getNode("/Book", Book.class);
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testNoBookWebClient() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        WebClient client = WebClient.create(baseAddress);
        client.path("/bookstore/books/0/subresource").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = client.get(Book.class);
        assertNull(b);
        assertEquals(204, client.getResponse().getStatus());
    }

    @Test
    public void testGetBook123WebClientResponse() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        WebClient client = WebClient.create(baseAddress);
        client.path("/bookstore/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = readBook((InputStream)client.get().getEntity());
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testGetBook356ClientException() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                          BookStoreJaxrsJaxws.class,
                                          Collections.singletonList(new TestResponseExceptionMapper()));

        try {
            proxy.getBook(356L);
            fail();
        } catch (BookNotFoundFault ex) {
            assertEquals("No Book with id 356 is available", ex.getMessage());
        }
    }

    @Test(expected = NotFoundException.class)
    public void testCheckBookClientException() {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                          BookStoreJaxrsJaxws.class,
                                          Collections.singletonList(new NotFoundResponseExceptionMapper()));
        proxy.checkBook(100L);
    }

    @Test
    public void testCheckBookClientErrorResponse() {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                          BookStoreJaxrsJaxws.class,
                                          Collections.singletonList(new DummyResponseExceptionMapper()));
        Response response = proxy.checkBook(100L);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testNoBook357WebClient() throws Exception {

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        Map<String, Object> properties = new HashMap<>();
        properties.put("org.apache.cxf.http.throw_io_exceptions", Boolean.TRUE);
        bean.setProperties(properties);
        bean.setAddress("http://localhost:" + PORT + "/test/services/rest/bookstore/356");
        WebClient wc = bean.createWebClient();
        Response response = wc.get();
        assertEquals(404, response.getStatus());
        String msg = IOUtils.readStringFromStream((InputStream)response.getEntity());
        assertEquals("No Book with id 356 is available", msg);

    }

    @Test
    public void testOtherInterceptorDrainingStream() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        bean.getInInterceptors().add(new TestStreamDrainInterptor());
        WebClient client = bean.createWebClient();
        client.path("/bookstore/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = client.get(Book.class);
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testGetBookSubresourceClient() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                                  BookStoreJaxrsJaxws.class);
        BookSubresource bs = proxy.getBookSubresource("125");
        Book b = bs.getTheBook();
        assertEquals(125, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testGetBookSubresourceClientWithContext() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                                  BookStoreJaxrsJaxws.class);
        BookSubresource bs = proxy.getBookSubresource("125");
        Book b = bs.getTheBookWithContext(null);
        assertEquals(125, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testGetBookSubresourceClientNoProduces() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                                  BookStoreJaxrsJaxws.class);
        BookSubresource bs = proxy.getBookSubresource("125");
        Book b = bs.getTheBookNoProduces();
        assertEquals(125, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testGetBookSubresourceParamExtensions() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                              BookStoreJaxrsJaxws.class);
        WebClient.getConfig(proxy).getOutInterceptors().add(new LoggingOutInterceptor());
        BookSubresource bs = proxy.getBookSubresource("139");
        Book bean = new Book("CXF Rocks", 139L);
        Book b = bs.getTheBook4(bean, bean, bean, bean);
        assertEquals(139, b.getId());
        assertEquals("CXF Rocks", b.getName());
    }

    @Test
    public void testGetBookSubresourceParamExtensions2() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                              BookStoreJaxrsJaxws.class);
        WebClient.getConfig(proxy).getOutInterceptors().add(new LoggingOutInterceptor());
        BookSubresource bs = proxy.getBookSubresource("139");
        BookBean bean = new BookBean("CXF Rocks", 139L);
        bean.getComments().put(1L, "Good");
        bean.getComments().put(2L, "Good");
        BookBean b = bs.getTheBookQueryBean(bean);
        assertEquals(139, b.getId());
        assertEquals("CXF Rocks", b.getName());
    }

    @Test
    public void testGetBookSubresourceParamOrder() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                              BookStoreJaxrsJaxws.class);
        BookSubresource bs = proxy.getBookSubresource("139");
        Book b = bs.getTheBook5("CXF", 555L);
        assertEquals(555, b.getId());
        assertEquals("CXF", b.getName());
    }

    @Test
    public void testAddOrderFormBean() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                              BookStoreJaxrsJaxws.class);

        WebClient.getConfig(proxy).getOutInterceptors().add(new LoggingOutInterceptor());
        WebClient.getConfig(proxy).getInInterceptors().add(new LoggingInInterceptor());

        BookSubresource bs = proxy.getBookSubresource("139");
        OrderBean order = new OrderBean();
        order.setId(123L);
        order.setWeight(100);
        order.setCustomerTitle(OrderBean.Title.MS);
        OrderBean order2 = bs.addOrder(order);
        assertEquals(Long.valueOf(123L), Long.valueOf(order2.getId()));
        assertEquals(OrderBean.Title.MS, order2.getCustomerTitle());
    }

    @Test
    public void testGetBookSubresourceWebClientParamExtensions() throws Exception {

        WebClient client = WebClient.create("http://localhost:" + PORT + "/test/services/rest");
        client.type(MediaType.TEXT_PLAIN_TYPE).accept(MediaType.APPLICATION_XML_TYPE);
        client.path("/bookstore/books/139/subresource4/139/CXF Rocks");
        Book bean = new Book("CXF Rocks", 139L);
        Form form = new Form();
        form.param("name", "CXF Rocks").param("id", Long.toString(139L));
        Book b = readBook((InputStream)client.matrix("", bean).query("", bean).form(form).getEntity());
        assertEquals(139, b.getId());
        assertEquals("CXF Rocks", b.getName());
    }

    @Test
    public void testGetBookSubresourceClient2() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                                  BookStoreJaxrsJaxws.class);
        doTestSubresource(proxy);
        BookStoreJaxrsJaxws proxy2 = proxy.getBookStore("number1");
        doTestSubresource(proxy2);
        BookStoreJaxrsJaxws proxy3 = proxy2.getBookStore("number1");
        doTestSubresource(proxy3);
    }

    @Test
    public void testGetBookSubresourceWebClientProxyBean() throws Exception {

        WebClient client = WebClient.create("http://localhost:" + PORT + "/test/services/rest");
        client.type(MediaType.TEXT_PLAIN_TYPE)
            .accept(MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE);
        BookStoreJaxrsJaxws proxy =
            JAXRSClientFactory.fromClient(client, BookStoreJaxrsJaxws.class, true);

        doTestSubresource(proxy);

        BookStoreJaxrsJaxws proxy2 = JAXRSClientFactory.fromClient(
            WebClient.client(proxy), BookStoreJaxrsJaxws.class);
        doTestSubresource(proxy2);

    }


    @Test
    public void testGetBookSubresourceWebClientProxy2() throws Exception {

        WebClient client = WebClient.create("http://localhost:" + PORT + "/test/services/rest/bookstore")
            .path("/books/378");
        client.type(MediaType.TEXT_PLAIN_TYPE).accept(MediaType.APPLICATION_XML_TYPE);
        BookSubresource proxy = JAXRSClientFactory.fromClient(client, BookSubresource.class);

        Book b = proxy.getTheBook2("CXF ", "in ", "Acti", "on ", "- 3", "7", "8");
        assertEquals(378, b.getId());
        assertEquals("CXF in Action - 378", b.getName());

    }

    private void doTestSubresource(BookStoreJaxrsJaxws proxy) throws Exception {
        BookSubresource bs = proxy.getBookSubresource("378");

        Book b = bs.getTheBook2("CXF ", "in ", "Acti", "on ", "- 3", "7", "8");
        assertEquals(378, b.getId());
        assertEquals("CXF in Action - 378", b.getName());

        WebClient.client(bs).reset().header("N4", "- 4");
        b = bs.getTheBook2("CXF ", "in ", "Acti", "on ", null, "7", "8");
        assertEquals(378, b.getId());
        assertEquals("CXF in Action - 478", b.getName());



    }

    @Test
    public void testGetBookWebClientForm() throws Exception {

        String baseAddress = "http://localhost:" + PORT
            + "/test/services/rest/bookstore/books/679/subresource3";
        WebClient wc = WebClient.create(baseAddress);
        MultivaluedMap<String, Object> map = new MetadataMap<>();
        map.putSingle("id", "679");
        map.add("name", "CXF in Action - ");
        map.add("name", "679");
        Book b = readBook((InputStream)wc.accept("application/xml")
                          .form(map).getEntity());
        assertEquals(679, b.getId());
        assertEquals("CXF in Action - 679", b.getName());
    }

    @Test
    public void testGetBookWebClientForm2() throws Exception {

        String baseAddress = "http://localhost:" + PORT
            + "/test/services/rest/bookstore/books/679/subresource3";
        WebClient wc = WebClient.create(baseAddress);
        Form f = new Form(new MetadataMap<String, String>());
        f.param("id", "679").param("name", "CXF in Action - ")
            .param("name", "679");
        Book b = readBook((InputStream)wc.accept("application/xml")
                          .form(f).getEntity());
        assertEquals(679, b.getId());
        assertEquals("CXF in Action - 679", b.getName());
    }

    @Test
    public void testGetBookSubresourceClientFormParam() throws Exception {

        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                                  BookStoreJaxrsJaxws.class);
        BookSubresource bs = proxy.getBookSubresource("679");
        List<String> parts = new ArrayList<>();
        parts.add("CXF in Action - ");
        parts.add(Integer.toString(679));
        Book b = bs.getTheBook3("679", parts);
        assertEquals(679, b.getId());
        assertEquals("CXF in Action - 679", b.getName());
    }

    @Test
    public void testAddGetBook123WebClient() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        WebClient client = WebClient.create(baseAddress);
        client.path("/bookstore/books").accept(MediaType.APPLICATION_XML_TYPE)
            .type(MediaType.APPLICATION_XML_TYPE);
        Book b = new Book();
        b.setId(124);
        b.setName("CXF in Action - 2");
        Book b2 = client.post(b, Book.class);
        assertNotSame(b, b2);
        assertEquals(124, b2.getId());
        assertEquals("CXF in Action - 2", b2.getName());
    }

    @Test
    public void testAddGetBook123Client() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        BookStoreJaxrsJaxws proxy = JAXRSClientFactory.create(baseAddress,
                                                                  BookStoreJaxrsJaxws.class);
        Book b = new Book();
        b.setId(124);
        b.setName("CXF in Action - 2");
        Book b2 = proxy.addBook(b);
        assertNotSame(b, b2);
        assertEquals(124, b2.getId());
        assertEquals("CXF in Action - 2", b2.getName());
    }

    @Test
    public void testAddGetBookRest() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/test/services/rest/bookstore/books";

        File input = new File(getClass().getResource("resources/add_book.txt").toURI());
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(endpointAddress);
        post.addHeader("Content-Type", "application/xml");
        post.setEntity(new FileEntity(input, ContentType.TEXT_XML));

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(200, response.getStatusLine().getStatusCode());

            InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");

            assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                         stripXmlInstructionIfNeeded(EntityUtils.toString(response.getEntity())));
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }

    }

    @Test
    public void testGetBookSoap() throws Exception {
        String wsdlAddress =
            "http://localhost:" + PORT + "/test/services/soap/bookservice?wsdl";
        URL wsdlUrl = new URL(wsdlAddress);
        BookSoapService service =
            new BookSoapService(wsdlUrl,
                                new QName("http://books.com", "BookService"));
        BookStoreJaxrsJaxws store = service.getBookPort();
        Book book = store.getBook(Long.valueOf(123));
        assertEquals("id is wrong", book.getId(), 123);
    }

    @Test
    public void testGetUnqualifiedBookSoap() throws Exception {

        String wsdlAddress =
            "http://localhost:" + PORT + "/test/services/soap-transform/bookservice?wsdl";
        BookSoapService service =
            new BookSoapService(new URL(wsdlAddress),
                                new QName("http://books.com", "BookService"));
        BookStoreJaxrsJaxws store = service.getBookPort();

        TransformOutInterceptor out = new TransformOutInterceptor();
        Map<String, String> mapOut = new HashMap<>();
        // Book content (id, name) is unqualified, thus the following works
        // because JAXB will report
        // - {http://jaxws.jaxrs.systest.cxf.apache.org/}Book
        // - id
        // - name
        // and only the qualified top-level Book tag gets matched by the following
        // mapping
        mapOut.put("{http://jaxws.jaxrs.systest.cxf.apache.org/}*", "*");
        out.setOutTransformElements(mapOut);

        TransformInInterceptor in = new TransformInInterceptor();
        Map<String, String> mapIn = new HashMap<>();
        // mapIn.put("*", "{http://jaxws.jaxrs.systest.cxf.apache.org/}*");
        // won't work for a case where a totally unqualified getBookResponse needs to be
        // qualified such that only the top-level getBookResponse is processed because of '*'.
        // Such a mapping would work nicely if we had say a package-info making both
        // Book id & name qualified; otherwise we need to choose what tag we need to qualify

        // mapIn.put("*", "{http://jaxws.jaxrs.systest.cxf.apache.org/}*");
        // works too if the schema validation is disabled

        mapIn.put("getBookResponse", "{http://jaxws.jaxrs.systest.cxf.apache.org/}getBookResponse");
        in.setInTransformElements(mapIn);

        Client cl = ClientProxy.getClient(store);
        ((HTTPConduit)cl.getConduit()).getClient().setReceiveTimeout(10000000);
        cl.getInInterceptors().add(in);
        cl.getOutInterceptors().add(out);

        Book book = store.getBook(Long.valueOf(123));
        assertEquals("id is wrong", book.getId(), 123);

    }

    @Test
    public void testServiceListingsAndWadl() throws Exception {
        String listings =
            getStringFromInputStream(getHttpInputStream("http://localhost:" + PORT + "/test/services"));
        assertNotNull(listings);
        assertTrue(listings.contains("http://localhost:" + PORT + "/test/services/soap/bookservice?wsdl"));
        assertFalse(listings.contains("http://localhost:" + PORT + "/test/services/soap/bookservice2?wsdl"));

        assertTrue(listings.contains("http://localhost:" + PORT + "/test/services/rest?_wadl"));
        assertEquals(200, WebClient.create(
            "http://localhost:" + PORT + "/test/services/rest?_wadl&type=xml").get().getStatus());
        assertTrue(listings.contains("http://localhost:" + PORT + "/test/services/rest2?_wadl"));
        assertEquals(200, WebClient.create(
            "http://localhost:" + PORT + "/test/services/rest2?_wadl&type=xml").get().getStatus());
        assertFalse(listings.contains("http://localhost:" + PORT + "/test/services/rest3?_wadl"));
        assertFalse(listings.contains("Atom Log Feed"));

        WebClient webClient =
            WebClient.create("http://localhost:" + PORT + "/test/services/rest3?_wadl");
        assertEquals(404, webClient.get().getStatus());
    }

    @Test
    public void testAddFeatureToClient() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        bean.setResourceClass(BookStoreJaxrsJaxws.class);
        TestFeature testFeature = new TestFeature();
        List<AbstractFeature> features = new ArrayList<>();
        features.add(testFeature);
        bean.setFeatures(features);
        BookStoreJaxrsJaxws proxy = (BookStoreJaxrsJaxws)bean.create();
        Book b = proxy.getBook(Long.valueOf("123"));
        assertTrue("Out Interceptor not invoked", testFeature.handleMessageOnOutInterceptorCalled());
        assertTrue("In Interceptor not invoked", testFeature.handleMessageOnInInterceptorCalled());
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testServerFaultInInterceptor() throws Exception {
        //testing faults created by server handled correctly

        //999 causes error code of 404, 404 has a different code path so need to test too
        serverFaultInInterceptorTest("999");
        //322 causes a checked exception to be thrown so need to
        serverFaultInInterceptorTest("322");
    }

    @Test
    public void testClientFaultOutInterceptor() throws Exception {
        //testing faults created by client out interceptor chain handled correctly
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        bean.setResourceClass(BookStoreJaxrsJaxws.class);
        final boolean addBadOutInterceptor = true;
        TestFeature testFeature = new TestFeature(addBadOutInterceptor);
        List<AbstractFeature> features = new ArrayList<>();
        features.add(testFeature);
        bean.setFeatures(features);
        BookStoreJaxrsJaxws proxy = (BookStoreJaxrsJaxws)bean.create();
        try {
            //321 is special case - causes error code of 525
            proxy.getBook(Long.valueOf("123"));
            fail("Method should have thrown an exception");
        } catch (Exception e) {
            assertTrue("Out Interceptor not invoked", testFeature.handleMessageOnOutInterceptorCalled());
            assertFalse("In Interceptor not invoked", testFeature.handleMessageOnInInterceptorCalled());
            assertTrue("Wrong exception caught",
                       "fault from bad interceptor".equals(e.getCause().getMessage()));
            assertTrue("Client In Fault In Interceptor was invoked",
                    testFeature.faultInInterceptorCalled());
        }
    }

    private void serverFaultInInterceptorTest(String param) {
        String baseAddress = "http://localhost:" + PORT + "/test/services/rest";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        bean.setResourceClass(BookStoreJaxrsJaxws.class);
        TestFeature testFeature = new TestFeature();
        List<AbstractFeature> features = new ArrayList<>();
        features.add(testFeature);
        bean.setFeatures(features);
        BookStoreJaxrsJaxws proxy = (BookStoreJaxrsJaxws)bean.create();
        WebClient.getConfig(proxy).getRequestContext().put("org.apache.cxf.transport.no_io_exceptions", false);
        try {
            //321 is special case - causes error code of 525
            proxy.getBook(Long.valueOf(param));
            fail("Method should have thrown an exception");
        } catch (Exception e) {
            assertTrue("Out Interceptor not invoked", testFeature.handleMessageOnOutInterceptorCalled());
            if ("322".equals(param)) {
                //In interceptors not called when checked exception thrown from server
                assertTrue("In Interceptor not invoked", testFeature.handleMessageOnInInterceptorCalled());
            } else {
                assertFalse("In Interceptor not invoked", testFeature.handleMessageOnInInterceptorCalled());
            }
            assertTrue("Client In Fault In Interceptor not invoked",
                    testFeature.faultInInterceptorCalled());
        }
    }

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

    private InputStream getHttpInputStream(String endpointAddress) throws Exception {
        URL url = new URL(endpointAddress);

        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/xml,text/plain");
        return connect.getInputStream();
    }

    private Book readBook(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }

    @Ignore
    public static class TestResponseExceptionMapper implements ResponseExceptionMapper<BookNotFoundFault> {

        public TestResponseExceptionMapper() {
        }

        public BookNotFoundFault fromResponse(Response r) {
            Object value = r.getMetadata().getFirst("BOOK-HEADER");
            if (value != null) {
                return new BookNotFoundFault(value.toString());
            }
            throw new WebApplicationException();
        }

    }

    @Ignore
    public static class NotFoundResponseExceptionMapper implements ResponseExceptionMapper<Exception> {

        public Exception fromResponse(Response r) {
            if (r.getStatus() == HttpStatus.SC_NOT_FOUND) {
                return new NotFoundException();
            }
            return null;
        }
    }

    @Ignore
    public static class DummyResponseExceptionMapper implements ResponseExceptionMapper<Exception> {

        public Exception fromResponse(Response r) {
            return null;
        }
    }

    @Ignore
    public class TestStreamDrainInterptor extends AbstractPhaseInterceptor<Message> {
        public TestStreamDrainInterptor() {
            super(Phase.RECEIVE);
        }

        public void handleMessage(Message message) throws Fault {
            InputStream is = message.getContent(InputStream.class);
            if (is == null) {
                return;
            }
            byte[] payload;
            try {
                // input stream will be closed by readBytesFromStream()
                payload = IOUtils.readBytesFromStream(is);
                assertNotNull("payload was null", payload);
                assertTrue("payload was EMPTY", payload.length > 0);
                message.setContent(InputStream.class, new ByteArrayInputStream(payload));
            } catch (Exception e) {
                String error = "Failed to read the stream properly due to " + e.getMessage();
                assertNotNull(error, e);
            }
        }

    }

    @Ignore
    public class TestFeature extends AbstractFeature {
        private TestOutInterceptor testOutInterceptor;
        private TestInInterceptor testInInterceptor;
        private TestFaultInInterceptor testFaultInInterceptor;
        private boolean addBadOutInterceptor;

        public TestFeature() {
        }

        public TestFeature(boolean addBadOutInterceptor) {
            this.addBadOutInterceptor = addBadOutInterceptor;
        }

        @Override
        protected void initializeProvider(InterceptorProvider provider, Bus bus) {
            testOutInterceptor = new TestOutInterceptor(addBadOutInterceptor);
            testInInterceptor = new TestInInterceptor();
            testFaultInInterceptor = new TestFaultInInterceptor();
            provider.getOutInterceptors().add(testOutInterceptor);
            provider.getInInterceptors().add(testInInterceptor);
            provider.getInFaultInterceptors().add(testFaultInInterceptor);


        }

        protected boolean handleMessageOnOutInterceptorCalled() {
            return testOutInterceptor.handleMessageCalled();
        }

        protected boolean handleMessageOnInInterceptorCalled() {
            return testInInterceptor.handleMessageCalled();
        }

        protected boolean faultInInterceptorCalled() {
            return testFaultInInterceptor.handleMessageCalled();
        }
    }

    @Ignore
    public class TestInInterceptor extends AbstractPhaseInterceptor<Message> {
        private boolean handleMessageCalled;

        public TestInInterceptor() {
            super(Phase.PRE_STREAM);
        }

        public void handleMessage(Message message) throws Fault {
            handleMessageCalled = true;
        }

        protected boolean handleMessageCalled() {
            return handleMessageCalled;
        }

    }

    @Ignore
    public class TestOutInterceptor extends AbstractPhaseInterceptor<Message> {
        private boolean handleMessageCalled;
        private boolean isBadOutInterceptor;


        public TestOutInterceptor(boolean isBadOutInterceptor) {
            super(Phase.PRE_MARSHAL);
            this.isBadOutInterceptor = isBadOutInterceptor;
        }

        public void handleMessage(Message message) throws Fault {
            handleMessageCalled = true;
            if (isBadOutInterceptor) {
                throw new Fault(new Exception("fault from bad interceptor"));
            }
        }

        protected boolean handleMessageCalled() {
            return handleMessageCalled;
        }

    }

    @Ignore
    public class TestFaultInInterceptor extends AbstractPhaseInterceptor<Message> {
        private boolean handleMessageCalled;
        public TestFaultInInterceptor() {
            super(Phase.PRE_STREAM);
        }

        public void handleMessage(Message message) throws Fault {
            handleMessageCalled = true;
        }

        protected boolean handleMessageCalled() {
            return handleMessageCalled;
        }

    }
    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }
}
