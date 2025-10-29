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

package org.apache.cxf.jaxrs.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ProxyClassLoader;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.BookInterface;
import org.apache.cxf.jaxrs.resources.BookStore;
import org.apache.cxf.jaxrs.resources.BookStoreSubresourcesOnly;
import org.apache.cxf.jaxrs.resources.BookSuperClass;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.jaxrs.resources.SuperBookStore;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JAXRSClientFactoryBeanTest {

    @Test
    public void testCreateClient() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStore.class);
        assertTrue(bean.create() instanceof BookStore);
    }

    @Test
    public void testCreateClientCustomLoader() throws Exception {
        ProxyClassLoader loader = new ProxyClassLoader(BookStore.class.getClassLoader());
        loader.addLoader(BookStore.class.getClassLoader());

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStore.class);
        bean.setClassLoader(loader);
        BookStore client = (BookStore)bean.createWithValues(BookStore.class);
        assertNotNull(client);
        assertSame(client.getClass().getClassLoader(), loader);
        // tricky to test the loader has been used correctly with Maven
        // given that the system loader loads all the test classes

    }

    @Test
    public void testCreateClientWithUserResource() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        UserResource r = new UserResource();
        r.setName(BookStore.class.getName());
        r.setPath("/");
        UserOperation op = new UserOperation();
        op.setName("getDescription");
        op.setVerb("GET");
        r.setOperations(Collections.singletonList(op));
        bean.setModelBeans(r);
        assertTrue(bean.create() instanceof BookStore);
    }

    @Test
    public void testCreateClientWithTwoUserResources() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        UserResource r1 = new UserResource();
        r1.setName(BookStore.class.getName());
        r1.setPath("/store");
        UserOperation op = new UserOperation();
        op.setName("getDescription");
        op.setVerb("GET");
        r1.setOperations(Collections.singletonList(op));

        UserResource r2 = new UserResource();
        r2.setName(Book.class.getName());
        r2.setPath("/book");
        UserOperation op2 = new UserOperation();
        op2.setName("getName");
        op2.setVerb("GET");
        r2.setOperations(Collections.singletonList(op2));

        bean.setModelBeans(r1, r2);
        bean.setServiceClass(Book.class);
        assertTrue(bean.create() instanceof Book);
    }

    @Test
    public void testGetConduit() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStore.class);
        BookStore store = bean.create(BookStore.class);
        Conduit conduit = WebClient.getConfig(store).getConduit();
        assertTrue(conduit instanceof HTTPConduit);
    }

    @Test
    public void testTemplateInRootPathInherit() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class, 1, 2, 3);
        BookStoreSubresourcesOnly store2 = store.getItself();
        assertEquals("http://bar/bookstore/1/2/3/sub1",
                     WebClient.client(store2).getCurrentURI().toString());
    }

    @Test
    public void testTemplateInRootReplace() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class, 1, 2, 3);
        BookStoreSubresourcesOnly store2 = store.getItself2("4", "11", "33");
        assertEquals("http://bar/bookstore/11/2/33/sub2/4",
                     WebClient.client(store2).getCurrentURI().toString());
    }

    @Test
    public void testTemplateInRootReplaceEmpty() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store2 = store.getItself4("4", "11", "22", "33");
        assertEquals("http://bar/bookstore/11/22/33/sub2/4",
                     WebClient.client(store2).getCurrentURI().toString());
    }

    @Test
    public void testTemplateInRootAppend() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class, 1, 2, 3);
        BookStoreSubresourcesOnly store2 = store.getItself3("id4");
        assertEquals("http://bar/bookstore/1/2/3/id4/sub3",
                     WebClient.client(store2).getCurrentURI().toString());
    }

    @Test
    public void testAddLoggingToClient() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStoreSubresourcesOnly.class);
        TestFeature testFeature = new TestFeature();
        bean.setFeatures(Collections.singletonList(testFeature));

        BookStoreSubresourcesOnly store = bean.create(BookStoreSubresourcesOnly.class, 1, 2, 3);
        assertTrue("TestFeature wasn't initialized", testFeature.isInitialized());
        BookStoreSubresourcesOnly store2 = store.getItself3("id4");
        assertEquals("http://bar/bookstore/1/2/3/id4/sub3",
                     WebClient.client(store2).getCurrentURI().toString());
    }

    @Test
    public void testComplexProxy() throws Exception {
        IProductResource productResource = JAXRSClientFactory.create("http://localhost:9000",
                                                                     IProductResource.class);
        assertNotNull(productResource);
        IPartsResource parts = productResource.getParts();
        assertNotNull(parts);
        IProductResource productResourceElement = parts.elementAt("1");
        assertNotNull(productResourceElement);
    }
    
    @Test
    public void testBookAndBridgeMethods() throws Exception {
        SuperBookStore superBookResource = JAXRSClientFactory.create("http://localhost:9000",
                SuperBookStore.class);
        assertNotNull(superBookResource);
        
        Book book = ((BookSuperClass)superBookResource).getNewBook("id4", true);
        assertNotNull(book);
        
        SuperBook superBook = (SuperBook)superBookResource.getNewBook("id4", true);
        assertNotNull(superBook);
    }

    @Test
    public void testVoidResponseAcceptWildcard() throws Exception {
        String address = "local://store";
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setServiceBean(new BookStore());
        sf.setAddress(address);
        Server s = sf.create(); 

        BookStore store = JAXRSClientFactory.create(address, BookStore.class);
        store.addBook(new Book());

        s.stop();

        ResponseImpl response = (ResponseImpl) WebClient.client(store).getResponse();
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>) response.getOutMessage().get(Message.PROTOCOL_HEADERS));
        assertTrue(headers.get(HttpHeaders.ACCEPT).contains(MediaType.WILDCARD));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokePathNull() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookInterface.class);
        BookInterface store = bean.create(BookInterface.class);
        store.getBook(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokePathEmpty() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookInterface.class);
        BookInterface store = bean.create(BookInterface.class);
        store.getBook("");
    }

    @Test
    public void testInvokePathEmptyAllowed() throws Exception {
        Bus bus = BusFactory.newInstance().createBus();
        bus.setProperty("allow.empty.path.template.value", true);
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setBus(bus);
        bean.setAddress("http://bar");
        bean.setResourceClass(BookInterface.class);
        BookInterface store = bean.create(BookInterface.class);
        assertNotNull(store.getBook(""));
    }

    @Test
    public void testCreateClientFrom() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://bar");
        bean.setResourceClass(BookStore.class);

        final Client client = bean.create();
        final WebClient wc = WebClient.fromClient(client);
        assertThat(wc.getConfigurationReference(), is(not(nullValue())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(2L));

        client.close();
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));

        wc.close();
        assertThat(wc.getConfigurationReference(), is(nullValue()));
    }

    @Test
    public void testCreateClientFromAndInvoke() throws Exception {
        final SuperBookStore superBookResource = JAXRSClientFactory
            .create("http://localhost:9000", SuperBookStore.class);
        final Client client = (Client) superBookResource;
        final WebClient wc = WebClient.fromClient(client);

        final Book book = superBookResource.getNewBook("id4", true);
        assertNotNull(book);
        
        assertThat(wc.getConfigurationReference(), is(not(nullValue())));
        assertThat(wc.getConfigurationReference().refCount(), equalTo(2L));

        client.close();
        assertThat(wc.getConfigurationReference().refCount(), equalTo(1L));

        wc.close();
        assertThat(wc.getConfigurationReference(), is(nullValue()));

    }

    private final class TestFeature extends AbstractFeature {
        private TestInterceptor testInterceptor;

        @Override
        protected void initializeProvider(InterceptorProvider provider, Bus bus) {
            testInterceptor = new TestInterceptor();
            provider.getOutInterceptors().add(testInterceptor);
        }

        protected boolean isInitialized() {
            return testInterceptor.isInitialized();
        }
    }

    private class TestInterceptor extends AbstractPhaseInterceptor<Message> {
        private boolean isInitialized;

        TestInterceptor() {
            super(Phase.PRE_STREAM);
            isInitialized = true;
        }

        public void handleMessage(Message message) throws Fault {
        }

        protected boolean isInitialized() {
            return isInitialized;
        }

    }



    public interface IProductResource {
        @Path("/parts")
        IPartsResource getParts();
    }

    public interface IPartsResource {
        @Path("/{i}/")
        IProductResource elementAt(@PathParam("i") String i);
        String get();
    }

}