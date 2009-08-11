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
import java.util.List;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.AegisElementProvider;
import org.apache.cxf.jaxrs.provider.DataBindingJSONProvider;
import org.apache.cxf.sdo.SDODataBinding;
import org.apache.cxf.systest.jaxrs.sdo.SDOResource;
import org.apache.cxf.systest.jaxrs.sdo.Structure;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSDataBindingTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookDataBindingServer.class));
    }
    
    
    @Test
    public void testGetBookJAXB() throws Exception {
        WebClient client = WebClient.create("http://localhost:9080/databinding/jaxb/bookstore/books/123");
        Book book = client.accept("application/xml").get(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action", book.getName());
    }
    
    @Test
    @Ignore("Aegis Schema Validation gets in the way")
    public void testGetBookAegis() throws Exception {
        WebClient client = WebClient.create("http://localhost:9080/databinding/aegis/bookstore/books/123",
                                            Collections.singletonList(new AegisElementProvider()));
        Book book = client.accept("application/xml").get(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action", book.getName());
    }
    
    @Test
    public void testSDOStructure() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setDataBinding(new SDODataBinding());
        bean.setAddress("http://localhost:9080/databinding/sdo");
        bean.setResourceClass(SDOResource.class);
        SDOResource client = bean.create(SDOResource.class);
        Structure struct = client.getStructure();
        assertEquals("sdo", struct.getText());
        assertEquals(123.5, struct.getDbl(), 0.01);
        assertEquals(3, struct.getInt());
    }
    
    @Test
    public void testSDOStructureJSON() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        DataBinding db = new SDODataBinding();
        bean.setDataBinding(db);
        DataBindingJSONProvider provider = new DataBindingJSONProvider();
        provider.setNamespaceMap(Collections.singletonMap("http://apache.org/structure/types", "p0"));
        provider.setDataBinding(db);
        bean.setProvider(provider);
        bean.setAddress("http://localhost:9080/databinding/sdo");
        bean.setResourceClass(SDOResource.class);
        List<Interceptor> list = new ArrayList<Interceptor>();
        list.add(new LoggingInInterceptor());
        bean.setInInterceptors(list);
        SDOResource client = bean.create(SDOResource.class);
        WebClient.client(client).accept("application/json");
        Structure struct = client.getStructure();
        assertEquals("sdo", struct.getText());
        assertEquals(123.5, struct.getDbl(), 0.01);
        assertEquals(3, struct.getInt());
    }
    
}
