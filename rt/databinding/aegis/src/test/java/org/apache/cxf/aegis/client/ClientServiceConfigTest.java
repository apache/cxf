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

package org.apache.cxf.aegis.client;

import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientServiceConfigTest extends AbstractAegisTest {
    EndpointImpl impl;

    @Before
    public void before() throws Exception {
        super.setUp();

        ReflectionServiceFactoryBean factory = new ReflectionServiceFactoryBean();
        factory.setInvoker(new BeanInvoker(new EchoImpl()));
        factory.setDataBinding(new AegisDatabinding());

        ServerFactoryBean svrFac = new ServerFactoryBean();
        svrFac.setAddress("local://Echo");
        svrFac.setServiceFactory(factory);
        svrFac.setServiceClass(Echo.class);
        svrFac.setBus(getBus());
        svrFac.create();

        Endpoint endpoint = Endpoint.create(new EchoImpl());
        impl = (EndpointImpl) endpoint;
        impl.setDataBinding(new AegisDatabinding());
        endpoint.publish("local://JaxWsEcho");
    }
    @After
    public void after() throws Exception {
        impl.close();
        impl = null;
    }

    @Test
    public void talkToJaxWsHolder() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setDataBinding(new AegisDatabinding());
        factory.setAddress("local://JaxWsEcho");
        Echo client = factory.create(Echo.class);
        Holder<String> sholder = new Holder<>();
        client.echo("Channa Doll", sholder);
        assertEquals("Channa Doll", sholder.value);
    }

    @Test
    public void ordinaryParamNameTest() throws Exception {
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        ReflectionServiceFactoryBean factory = new ReflectionServiceFactoryBean();
        proxyFac.setServiceFactory(factory);
        proxyFac.setDataBinding(new AegisDatabinding());

        proxyFac.setAddress("local://Echo");
        proxyFac.setBus(getBus());

        Echo echo = proxyFac.create(Echo.class);
        String boing = echo.simpleEcho("reflection");
        assertEquals("reflection", boing);
    }

}