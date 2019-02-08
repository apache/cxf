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
package org.apache.cxf.systest.jms;

import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.interceptor.TibcoSoapActionInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.SOAPService2;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Simple test to check we can publish and call JMS services using the JAXWS API
 */
public class JaxWsAPITest extends AbstractVmJMSTest {

    @BeforeClass
    public static void startServer() {
        startBusAndJMS(JaxWsAPITest.class);
        publish(new GreeterImplDoc());
    }

    @Test
    public void testGreeterUsingJaxWSAPI() throws Exception {
        QName serviceName = new QName("http://apache.org/hello_world_doc_lit", "SOAPService2");
        QName portName = new QName("http://apache.org/hello_world_doc_lit", "SoapPort2");
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        SOAPService2 service = new SOAPService2(wsdl, serviceName);
        Greeter greeter = markForClose(service.getPort(portName, Greeter.class, cff));

        Client client = ClientProxy.getClient(greeter);
        client.getEndpoint().getOutInterceptors().add(new TibcoSoapActionInterceptor());
        greeter.greetMeOneWay("test String");

        String greeting = greeter.greetMe("Chris");
        Assert.assertEquals("Hello Chris", greeting);
    }

}
