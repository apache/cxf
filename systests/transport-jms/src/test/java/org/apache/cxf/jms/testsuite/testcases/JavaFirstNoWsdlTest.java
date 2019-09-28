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

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.systest.jms.AbstractVmJMSTest;
import org.apache.cxf.systest.jms.Hello;
import org.apache.cxf.systest.jms.HelloImpl;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JavaFirstNoWsdlTest extends AbstractVmJMSTest {
    private static final String SERVICE_ADDRESS = "jms:queue:test.cxf.jmstransport.queue3?receivetTimeOut=5000";

    @BeforeClass
    public static void startServer() {
        startBusAndJMS(JavaFirstNoWsdlTest.class);
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setBus(bus);
        svrFactory.getFeatures().add(cff);
        svrFactory.setServiceClass(Hello.class);
        svrFactory.setAddress(SERVICE_ADDRESS);
        svrFactory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        svrFactory.setServiceBean(new HelloImpl());
        svrFactory.create();
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
        Hello client = (Hello)markForClose(factory.create());
        String reply = client.sayHi(" HI");
        Assert.assertEquals("get HI", reply);
    }

}
