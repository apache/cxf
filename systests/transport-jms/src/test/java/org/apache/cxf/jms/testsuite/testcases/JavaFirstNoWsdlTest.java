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
package org.apache.cxf.jms.testsuite.testcases;

import java.io.Closeable;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.systest.jms.Hello;
import org.apache.cxf.systest.jms.HelloImpl;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JavaFirstNoWsdlTest {
    private static final String SERVICE_ADDRESS = "jms:queue:test.cxf.jmstransport.queue3?receivetTimeOut=5000";
    private static Bus bus;
    private static ConnectionFactoryFeature cff;

    @BeforeClass
    public static void startServer() {
        bus = BusFactory.getDefaultBus();
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        PooledConnectionFactory cfp = new PooledConnectionFactory(cf);
        cff = new ConnectionFactoryFeature(cfp);

        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setBus(bus);
        svrFactory.getFeatures().add(cff);
        svrFactory.setServiceClass(Hello.class);
        svrFactory.setAddress(SERVICE_ADDRESS);
        svrFactory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        svrFactory.setServiceBean(new HelloImpl());
        svrFactory.create();
    }

    @AfterClass
    public static void stopServers() {
        bus.shutdown(false);
    }

    @Test
    public void testSpecNoWsdlService() throws Exception {
        specNoWsdlService(null);
    }

    @Test
    public void testSpecNoWsdlServiceWithDifferentMessageType() throws Exception {
        specNoWsdlService("text");
        specNoWsdlService("byte");
        specNoWsdlService("binary");
    }

    private void specNoWsdlService(String messageType) throws Exception {
        String address = SERVICE_ADDRESS + ((messageType != null) ? "&messageType=" + messageType : "");
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.getFeatures().add(cff);
        factory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        factory.setServiceClass(Hello.class);
        factory.setAddress(address);
        Hello client = (Hello)factory.create();
        String reply = client.sayHi(" HI");
        Assert.assertEquals("get HI", reply);
        ((Closeable)client).close();
    }

}
