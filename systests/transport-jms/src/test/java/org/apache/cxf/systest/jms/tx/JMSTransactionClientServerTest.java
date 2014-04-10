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
package org.apache.cxf.systest.jms.tx;

import java.util.Collections;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.systest.jms.AbstractVmJMSTest;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.hello_world_doc_lit.Greeter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class JMSTransactionClientServerTest extends AbstractVmJMSTest {
    private static final String SERVICE_ADDRESS = 
        "jms:queue:greeter.queue.tx?receivetTimeOut=5000&sessionTransacted=true";
    private static EndpointImpl endpoint;

    @BeforeClass
    public static void startServers() throws Exception {
        startBusAndJMS(JMSTransactionClientServerTest.class);

        endpoint = new EndpointImpl(bus, new GreeterImplWithTransaction());
        endpoint.setAddress(SERVICE_ADDRESS);
        endpoint.setFeatures(Collections.singletonList(cff));
        endpoint.publish();
    }

    @AfterClass
    public static void clearProperty() {
        endpoint.stop();
    }

    @Test
    public void testTransaction() throws Exception {
        Greeter greeter = createGreeterProxy();
        // Should be processed normally
        greeter.greetMeOneWay("Good guy");
        
        // Should cause rollback, redelivery and in the end the message should go to the dead letter queue
        greeter.greetMe("Bad guy");
    }

    private Greeter createGreeterProxy() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.getFeatures().add(cff);
        factory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        factory.setServiceClass(Greeter.class);
        factory.setAddress(SERVICE_ADDRESS);
        return (Greeter)markForClose(factory.create());
    }
}
