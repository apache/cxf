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

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.resources.BookStore;
import org.apache.cxf.jaxrs.resources.BookStoreNoAnnotations;
import org.apache.cxf.jaxrs.resources.BookStoreSubresourcesOnly;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;


public class JAXRSServerFactoryBeanTest extends Assert {

    @After
    public void tearDown() throws Exception {
        if (BusFactory.getDefaultBus(false) != null) {
            BusFactory.getDefaultBus(false).shutdown(true);
        }
    }
    
    @Test
    public void testServers() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/spring/servers.xml"});
        
        JAXRSServerFactoryBean sfb = (JAXRSServerFactoryBean)ctx.getBean("simple");
        assertEquals("Get a wrong address", "http://localhost:8080/rs", sfb.getAddress());
        assertNotNull("The resource classes should not be null", sfb.getResourceClasses());
        assertEquals("Get a wrong resource class", BookStore.class, sfb.getResourceClasses().get(0));
        
        sfb = (JAXRSServerFactoryBean)ctx.getBean("inlineServiceBeans");
        assertNotNull("The resource classes should not be null", sfb.getResourceClasses());
        assertEquals("Get a wrong resource class", BookStore.class, sfb.getResourceClasses().get(0));
        assertEquals("Get a wrong resource class", 
                     BookStoreSubresourcesOnly.class, sfb.getResourceClasses().get(1));
        
        sfb = (JAXRSServerFactoryBean)ctx.getBean("inlineProvider");
        assertNotNull("The provider should not be null", sfb.getProviders());
        assertEquals("Get a wrong provider size", sfb.getProviders().size(), 2);
        
        sfb = (JAXRSServerFactoryBean)ctx.getBean("moduleServer");
        assertNotNull("The resource classes should not be null", sfb.getResourceClasses());
        assertEquals("Get a wrong ResourceClasses size", 1, sfb.getResourceClasses().size());
        assertEquals("Get a wrong resource class", BookStoreNoAnnotations.class, 
                     sfb.getResourceClasses().get(0));
        
    }
}
