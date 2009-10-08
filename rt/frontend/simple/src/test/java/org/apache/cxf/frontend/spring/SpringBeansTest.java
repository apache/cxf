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
package org.apache.cxf.frontend.spring;

import java.util.List;
import junit.framework.Assert;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.NullConduitSelector;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.service.factory.HelloService;
import org.apache.cxf.service.factory.HelloServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;



public class SpringBeansTest extends Assert {

    @Test
    public void testServers() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/frontend/spring/servers.xml"});

        ServerFactoryBean bean = (ServerFactoryBean) ctx.getBean("simple");
        assertNotNull(bean);

        if (!(bean.getServiceBean() instanceof HelloServiceImpl)) {
            fail("can't get the right serviceBean");
        }
        bean = (ServerFactoryBean) ctx.getBean("inlineImplementor");
        if (!(bean.getServiceBean() instanceof HelloServiceImpl)) {
            fail("can't get the right serviceBean");
        }

        bean = (ServerFactoryBean) ctx.getBean("inlineSoapBinding");
        assertNotNull(bean);

        BindingConfiguration bc = bean.getBindingConfig();
        assertTrue(bc instanceof SoapBindingConfiguration);
        SoapBindingConfiguration sbc = (SoapBindingConfiguration) bc;
        assertTrue(sbc.getVersion() instanceof Soap12);

        bean = (ServerFactoryBean) ctx.getBean("simpleWithBindingId");
        assertEquals("get the wrong BindingId",
                     bean.getBindingId(),
                     "http://cxf.apache.org/bindings/xformat");

        bean = (ServerFactoryBean) ctx.getBean("simpleWithWSDL");
        assertNotNull(bean);
        assertEquals(bean.getWsdlLocation(), "org/apache/cxf/frontend/spring/simple.wsdl");
    }

    @Test
    public void testClients() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/frontend/spring/clients.xml"});

        Object bean = ctx.getBean("client1.proxyFactory");
        assertNotNull(bean);

        ClientProxyFactoryBean cpfbean = (ClientProxyFactoryBean)bean;
        BindingConfiguration bc = cpfbean.getBindingConfig();
        assertTrue(bc instanceof SoapBindingConfiguration);
        SoapBindingConfiguration sbc = (SoapBindingConfiguration) bc;
        assertTrue(sbc.getVersion() instanceof Soap12);

        HelloService greeter = (HelloService) ctx.getBean("client1");
        assertNotNull(greeter);

        Client client = ClientProxy.getClient(greeter);
        assertNotNull("expected ConduitSelector", client.getConduitSelector());
        assertTrue("unexpected ConduitSelector",
                   client.getConduitSelector() instanceof NullConduitSelector);

        List<Interceptor> inInterceptors = client.getInInterceptors();
        boolean saaj = false;
        boolean logging = false;
        for (Interceptor<?> i : inInterceptors) {
            if (i instanceof SAAJInInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingInInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
        assertTrue(logging);

        saaj = false;
        logging = false;
        for (Interceptor<?> i : client.getOutInterceptors()) {
            if (i instanceof SAAJOutInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingOutInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
        assertTrue(logging);

        ClientProxyFactoryBean clientProxyFactoryBean =
            (ClientProxyFactoryBean) ctx.getBean("client2.proxyFactory");
        assertNotNull(clientProxyFactoryBean);
        assertEquals("get the wrong transportId", 
                     clientProxyFactoryBean.getTransportId(),
                     "http://cxf.apache.org/transports/local");

        assertEquals("get the wrong bindingId",
                     clientProxyFactoryBean.getBindingId(),
                     "http://cxf.apache.org/bindings/xformat");

        greeter = (HelloService) ctx.getBean("client2");        
        assertNotNull(greeter);
     

        greeter = (HelloService) ctx.getBean("client3");
        assertNotNull(greeter);

        client = ClientProxy.getClient(greeter);
        EndpointInfo epi = client.getEndpoint().getEndpointInfo();
        AuthorizationPolicy ap = epi.getExtensor(AuthorizationPolicy.class);
        assertNotNull("The AuthorizationPolicy instance should not be null", ap);
        assertEquals("Get the wrong username", ap.getUserName(), "testUser");
        assertEquals("Get the wrong password", ap.getPassword(), "password");
    }
}
