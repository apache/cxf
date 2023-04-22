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
package org.apache.cxf.systest.jms.continuations;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldService;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.systest.jms.AbstractVmJMSTest;

import org.junit.BeforeClass;
import org.junit.Test;

public class ProviderJMSContinuationTest extends AbstractVmJMSTest {

    @BeforeClass
    public static void startServers() throws Exception {
        startBusAndJMS(ProviderJMSContinuationTest.class);
        Object implementor = new HWSoapMessageDocProvider();
        String address = "jms:queue:test.jmstransport.text?replyToQueueName=test.jmstransport.text.reply";
        EndpointImpl ep = (EndpointImpl)Endpoint.create(implementor);
        ep.getInInterceptors().add(new IncomingMessageCounterInterceptor());
        ep.setBus(bus);
        ep.getFeatures().add(cff);
        ep.publish(address);
    }

    @Test
    public void testProviderContinuation() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        URL wsdl = getWSDLURL("/org/apache/cxf/systest/jms/continuations/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);
        HelloWorldPortType greeter = markForClose(service.getPort(HelloWorldPortType.class, cff));
        greeter.greetMe("ffang");
    }

}

