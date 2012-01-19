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

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.resources.BookStore;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class SpringResourceFactoryTest extends Assert {
    
    @After
    public void tearDown() throws Exception {
        if (BusFactory.getDefaultBus(false) != null) {
            BusFactory.getDefaultBus(false).shutdown(true);
        }
    }
    
    @Test
    public void testFactory() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/spring/servers2.xml"});
        verifyFactory(ctx, "sfactory1", true);
        verifyFactory(ctx, "sfactory2", false);
        
        Object serverBean = ctx.getBean("server1");
        assertNotNull(serverBean);
        JAXRSServerFactoryBean factoryBean = (JAXRSServerFactoryBean)serverBean;
        List<ClassResourceInfo> list = factoryBean.getServiceFactory().getClassResourceInfo();
        assertNotNull(list);
        assertEquals(4, list.size());
        assertSame(BookStore.class, list.get(0).getServiceClass());
        assertSame(BookStore.class, list.get(0).getResourceClass());
        assertSame(BookStore.class, list.get(1).getServiceClass());
        assertSame(BookStore.class, list.get(1).getResourceClass());
    }
    
    private void verifyFactory(ApplicationContext ctx, String factoryName, boolean isSingleton) 
        throws Exception {
        Object bean = ctx.getBean(factoryName);
        assertNotNull(bean);
        SpringResourceFactory sf = (SpringResourceFactory)bean;
        assertNotNull(sf.getApplicationContext());
        Constructor c = sf.getBeanConstructor();
        Constructor c2 = BookStore.class.getConstructor(new Class[]{});
                
        assertEquals(c.getParameterTypes().length, c2.getParameterTypes().length);
        assertEquals(isSingleton, sf.isSingleton());
    }
}
