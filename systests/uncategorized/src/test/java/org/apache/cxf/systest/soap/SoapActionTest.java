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

package org.apache.cxf.systest.soap;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_action.Greeter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SoapActionTest extends Assert {
    static final String PORT1 = TestUtil.getPortNumber(SoapActionTest.class, 1);
    static final String PORT2 = TestUtil.getPortNumber(SoapActionTest.class, 2);
    
    static Bus bus;
    static String add11 = "http://localhost:" + PORT1 + "/test11";
    static String add12 = "http://localhost:" + PORT2 + "/test12";


    @BeforeClass
    public static void createServers() throws Exception {
        bus = BusFactory.getDefaultBus();
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new SoapActionGreeterImpl());
        sf.setAddress(add11);
        sf.setBus(bus);
        sf.create();
        
        sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new SoapActionGreeterImpl());
        sf.setAddress(add12);
        sf.setBus(bus);
        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        sf.setBindingConfig(config);
        sf.create();
    }
    @AfterClass
    public static void shutdown() throws Exception {
        bus.shutdown(true);
    }
    

    @Test
    public void testEndpoint() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Greeter.class);
        pf.setAddress(add11);
        pf.setBus(bus);
        Greeter greeter = (Greeter) pf.create();
        
        assertEquals("sayHi", greeter.sayHi("test"));
        assertEquals("sayHi2", greeter.sayHi2("test"));        
    }
    
    @Test
    public void testSoap12Endpoint() throws Exception {

        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Greeter.class);
        pf.setAddress(add12);
        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        pf.setBindingConfig(config);
        pf.setBus(bus);
        
        Greeter greeter = (Greeter) pf.create();
        
        assertEquals("sayHi", greeter.sayHi("test"));
        assertEquals("sayHi2", greeter.sayHi2("test"));
    }
}
