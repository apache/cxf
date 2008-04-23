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
package org.apache.cxf.binding.http.bare;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.http.AbstractRestTest;
import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.binding.http.IriDecoderHelper;
import org.apache.cxf.binding.http.URIMapper;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.customer.bare.CustomerService;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.junit.Test;

public class BareServiceTest extends AbstractRestTest {

    @Test
    public void testCreation() throws Exception {
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        HttpBindingFactory factory = new HttpBindingFactory();
        factory.setBus(getBus());
        bfm.registerBindingFactory(HttpBindingFactory.HTTP_BINDING_ID, factory);

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setServiceClass(CustomerService.class);
        sf.getServiceFactory().setWrapped(false);
        sf.setAddress("http://localhost:9001/foo/");
        sf.setServiceBean(new CustomerService());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("contextMatchStrategy", "stem");
        sf.setProperties(props);

        ServerImpl svr = (ServerImpl) sf.create();

        URIMapper mapper = (URIMapper) svr.getEndpoint().getService().get(URIMapper.class.getName());
        assertNotNull(mapper);

        BindingOperationInfo bop = mapper.getOperation("/customers", "GET", null);
        assertNotNull(bop);
        assertEquals("getCustomers", bop.getName().getLocalPart());

        bop = mapper.getOperation("/customers", "POST", null);
        assertNotNull(bop);
        assertEquals("addCustomer", bop.getName().getLocalPart());

        bop = mapper.getOperation("/customers/123", "GET", null);
        assertNotNull(bop);
        assertEquals("getCustomer", bop.getName().getLocalPart());

        bop = mapper.getOperation("/customers/123", "PUT", null);
        assertNotNull(bop);
        assertEquals("updateCustomer", bop.getName().getLocalPart());

        bop = mapper.getOperation("/customers/details/123", "GET", null);
        assertNotNull(bop);
        assertEquals("getSomeDetails", bop.getName().getLocalPart());

        // TEST POST/GETs

        Document res = get("http://localhost:9001/foo/customers");
        assertNotNull(res);

        addNamespace("c", "http://cxf.apache.org/jra");
        assertValid("/c:customers", res);
        assertValid("/c:customers/c:customer/c:id[text()='123']", res);
        assertValid("/c:customers/c:customer/c:name[text()='Dan Diephouse']", res);

        res = get("http://localhost:9001/foo/customers/123");
        assertNotNull(res);

        assertValid("/c:customer", res);
        assertValid("/c:customer/c:id[text()='123']", res);
        assertValid("/c:customer/c:name[text()='Dan Diephouse']", res);

        // Try invalid customer
        res = get("http://localhost:9001/foo/customers/0", 500);
        assertNotNull(res);

        assertValid("//c:CustomerNotFoundFault", res);

        res = put("http://localhost:9001/foo/customers/123", "update.xml");
        assertNotNull(res);

        assertValid("/c:updateCustomer", res);

        res = post("http://localhost:9001/foo/customers", "add.xml");
        assertNotNull(res);

        assertValid("/c:addCustomer", res);

        // Get the updated document
        res = get("http://localhost:9001/foo/customers/123");
        assertNotNull(res);

        assertValid("/c:customer", res);
        assertValid("/c:customer/c:id[text()='123']", res);
        assertValid("/c:customer/c:name[text()='Danno Manno']", res);

        svr.stop();
    }

    @Test
    public void testSetContentType() throws Exception {
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        HttpBindingFactory factory = new HttpBindingFactory();
        factory.setBus(getBus());
        bfm.registerBindingFactory(HttpBindingFactory.HTTP_BINDING_ID, factory);

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setServiceClass(CustomerService.class);
        sf.getServiceFactory().setWrapped(false);
        sf.setAddress("http://localhost:9001/foo/");
        sf.setServiceBean(new CustomerService());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "text/plain");
        sf.setProperties(props);

        ServerImpl svr = (ServerImpl) sf.create();

        URL url = new URL("http://localhost:9001/foo/customers/123");
        HttpURLConnection c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod("GET");

        assertEquals("text/plain", c.getContentType());

        c.disconnect();

        url = new URL("http://localhost:9001/foo/customers/bleh");
        c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod("GET");

        String ct = c.getContentType();
        assertTrue(ct.startsWith("text/plain"));

        svr.stop();
    }

    @Test
    public void testGetOnBadUnwrappedParam() throws Exception {
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        HttpBindingFactory factory = new HttpBindingFactory();
        factory.setBus(getBus());
        bfm.registerBindingFactory(HttpBindingFactory.HTTP_BINDING_ID, factory);

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setServiceBean(new TestService());
        sf.getServiceFactory().setWrapped(false);
        sf.setAddress("http://localhost:9001/test/");

        ServerImpl svr = (ServerImpl) sf.create();

        URL url = new URL("http://localhost:9001/test/topics/123");
        HttpURLConnection c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod("GET");

        assertEquals("text/xml; charset=utf-8", c.getContentType());

        Document doc = DOMUtils.readXml(c.getErrorStream());
        Message msg = new Message("SIMPLE_TYPE", IriDecoderHelper.BUNDLE);
        assertValid("//*[text()='" + msg.toString() + "']", doc);

        svr.stop();
    }

    @Test
    public void testQueryParam() throws Exception {
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        HttpBindingFactory factory = new HttpBindingFactory();
        factory.setBus(getBus());
        bfm.registerBindingFactory(HttpBindingFactory.HTTP_BINDING_ID, factory);

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setServiceBean(new TestService());
        sf.getServiceFactory().setWrapped(false);
        sf.setAddress("http://localhost:9001/test/");

        ServerImpl svr = (ServerImpl) sf.create();

        URL url = new URL("http://localhost:9001/test/rest/foo");
        HttpURLConnection c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod("GET");

        assertEquals("text/xml; charset=utf-8", c.getContentType());

        Document doc = DOMUtils.readXml(c.getInputStream());
        addNamespace("b", "http://bare.http.binding.cxf.apache.org/");
        assertValid("//b:getDataResponse", doc);

        svr.stop();
    }
}
