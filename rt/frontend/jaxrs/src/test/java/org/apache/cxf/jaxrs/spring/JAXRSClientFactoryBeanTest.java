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
package org.apache.cxf.jaxrs.spring;

import javax.xml.namespace.QName;

import junit.framework.Assert;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JAXRSClientFactoryBeanTest extends Assert {

    @After
    public void tearDown() throws Exception {
        if (BusFactory.getDefaultBus(false) != null) {
            BusFactory.getDefaultBus(false).shutdown(true);
        }
    }
    
    @Test
    public void testClients() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/spring/clients.xml"});
        
        Object bean = ctx.getBean("client1.proxyFactory");
        assertNotNull(bean);
        
        bean = ctx.getBean("setHeaderClient.proxyFactory");
        assertNotNull(bean);
        JAXRSClientFactoryBean cfb = (JAXRSClientFactoryBean) bean;
        assertNotNull(cfb.getHeaders());
        assertEquals("Get a wrong map size", cfb.getHeaders().size(), 1);
        assertEquals("Get a wrong username", cfb.getUsername(), "username");
        assertEquals("Get a wrong password", cfb.getPassword(), "password");
        assertEquals(new QName("http://books.com", "BookService"), 
                     cfb.getServiceName());
        
        bean = ctx.getBean("ModelClient.proxyFactory");
        assertNotNull(bean);
        cfb = (JAXRSClientFactoryBean) bean;
        assertNotNull(cfb.getHeaders());
        assertEquals("Get a wrong map size", cfb.getHeaders().size(), 1);
        assertEquals("Get a wrong username", cfb.getUsername(), "username");
        assertEquals("Get a wrong password", cfb.getPassword(), "password");
    }

}
