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
package org.apache.cxf.jaxrs.client.spring;

import java.util.List;

import javax.xml.namespace.QName;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.cache.CacheControlClientReaderInterceptor;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;



public class JAXRSClientFactoryBeanTest {

    @After
    public void tearDown() throws Exception {
        if (BusFactory.getDefaultBus(false) != null) {
            BusFactory.getDefaultBus(false).shutdown(true);
        }
    }

    @Test
    public void testClients() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/client/spring/clients.xml"});

        Object bean = ctx.getBean("client1.proxyFactory");
        assertNotNull(bean);

        bean = ctx.getBean("setHeaderClient.proxyFactory");
        assertNotNull(bean);
        JAXRSClientFactoryBean cfb = (JAXRSClientFactoryBean) bean;
        assertNotNull(cfb.getHeaders());
        assertEquals("Get a wrong map size", cfb.getHeaders().size(), 1);
        assertEquals("Get a wrong username", cfb.getUsername(), "username");
        assertEquals("Get a wrong password", cfb.getPassword(), "password");
        QName serviceQName = new QName("http://books.com", "BookService");
        assertEquals(serviceQName, cfb.getServiceName());
        assertEquals(serviceQName, cfb.getServiceFactory().getServiceName());

        bean = ctx.getBean("ModelClient.proxyFactory");
        assertNotNull(bean);
        cfb = (JAXRSClientFactoryBean) bean;
        assertNotNull(cfb.getHeaders());
        assertEquals("Get a wrong map size", cfb.getHeaders().size(), 1);
        assertEquals("Get a wrong username", cfb.getUsername(), "username");
        assertEquals("Get a wrong password", cfb.getPassword(), "password");
        
        bean = ctx.getBean("client2.proxyFactory");
        assertNotNull(bean);
        cfb = (JAXRSClientFactoryBean) bean;
        assertNotNull(cfb.getProperties());
        assertEquals("Get a wrong map size", cfb.getProperties().size(), 1);

        ctx.close();
    }
    
    @Test
    public void testClientProperties() throws Exception {
        try (ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/client/spring/clients.xml"})) {
            Client bean = (Client) ctx.getBean("client2");
            assertNotNull(bean);
            assertThat(bean.query("list", "1").query("list", "2").getCurrentURI().toString(),
                endsWith("?list=1,2"));
            
            bean = (Client) ctx.getBean("client1");
            assertNotNull(bean);
            assertThat(bean.query("list", "1").query("list", "2").getCurrentURI().toString(),
                endsWith("?list=1&list=2"));
        }
    }
    
    @Test
    public void testClientPropertiesWithState() throws Exception {
        try (ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/client/spring/clients.xml"})) {
            Client bean = (Client) ctx.getBean("client3");
            assertNotNull(bean);
            assertThat(bean.query("list", "1").query("list", "2").getCurrentURI().toString(),
                endsWith("?list=1,2"));
            
            bean = (Client) ctx.getBean("client1");
            assertNotNull(bean);
            assertThat(bean.query("list", "1").query("list", "2").getCurrentURI().toString(),
                endsWith("?list=1&list=2"));
        }
    }
    
    @Test
    public void testClientWithFeatures() throws Exception {
        try (ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/client/spring/clients.xml"})) {
            final Client bean = (Client) ctx.getBean("client4");
            assertNotNull(bean);
            
            final JAXRSClientFactoryBean cfb = (JAXRSClientFactoryBean)ctx.getBean("client4.proxyFactory");
            assertNotNull(bean);
            
            assertThat((List<Object>)cfb.getProviders(), 
                hasItem(instanceOf(CacheControlClientReaderInterceptor.class)));
            
            assertThat((List<Object>)cfb.getProviders(), 
                hasItem(instanceOf(SpringParameterHandler.class)));
        }
    }
    
    public static class SomeFeature implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            context.register(SpringParameterHandler.class);
            return true;
        }
    }
}