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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.http.AbstractRestTest;
import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.customer.Customer;
import org.apache.cxf.customer.Customers;
import org.apache.cxf.customer.bare.CustomerService;
import org.apache.cxf.customer.bare.GetCustomer;
import org.apache.cxf.customer.bare.GetCustomers;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.Test;

public class ClientTest extends AbstractRestTest {
    @Test
    public void testCreation() throws Exception {
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        bfm.registerBindingFactory(HttpBindingFactory.HTTP_BINDING_ID, new HttpBindingFactory());
        
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setServiceClass(CustomerService.class);
        sf.getServiceFactory().setWrapped(false);
        sf.setAddress("http://localhost:9002/foo/");
        sf.setServiceBean(new CustomerService());
        //sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("contextMatchStrategy", "stem");
        sf.setProperties(props);
        
        ServerImpl svr = (ServerImpl) sf.create();

        svr.getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
        
        ClientImpl client = new ClientImpl(getBus(), svr.getEndpoint());
        
        Object[] objects = client.invoke(new QName("http://cxf.apache.org/jra", "getCustomers"), 
                                         new GetCustomers());
        assertNotNull(objects);
        
        Customers c = (Customers) objects[0];
        Customer customer = c.getCustomer().iterator().next();
        assertEquals("Dan Diephouse", customer.getName());
        
        GetCustomer getCustomer = new GetCustomer();
        getCustomer.setId(customer.getId());
        objects = client.invoke(new QName("http://cxf.apache.org/jra", "getCustomer"), getCustomer);
        
        customer = (Customer) objects[0];
        assertEquals("Dan Diephouse", customer.getName());
//        
//        objects = client.invoke(new QName("http://cxf.apache.org/jra", "deleteCustomer"), 
//        customer.getId());
//        assertTrue(objects == null || objects.length == 0);
//        
        svr.destroy();
    }

}
