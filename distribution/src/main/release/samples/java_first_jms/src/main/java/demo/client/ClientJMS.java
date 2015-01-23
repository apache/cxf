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

package demo.client;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

import demo.service.HelloWorld;

public final class ClientJMS {
    private static final String JMS_ENDPOINT_URI = "jms:queue:test.cxf.jmstransport.queue?timeToLive=1000"
                               + "&jndiConnectionFactoryName=ConnectionFactory" + "&jndiInitialContextFactory"
                               + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                               + "&jndiURL=tcp://localhost:61616";

    private static final QName SERVICE_QNAME =
        new QName("http://impl.service.demo/", "HelloWorldImplService");
    private static final QName PORT_QNAME =
        new QName("http://service.demo/", "HelloWorldPort");

    private ClientJMS() {
        //
    }

    public static void main(String[] args) throws Exception {
        boolean jaxws = false;
        for (String arg : args) {
            if ("-jaxws".equals(arg)) {
                jaxws = true;
            }
        }
        HelloWorld client = null;
        if (jaxws) {
            client = createClientJaxWs();
        } else {
            client = createClientCxf();
        }
        String reply = client.sayHi("HI");
        System.out.println(reply);
        System.exit(0);
    }

    private static HelloWorld createClientJaxWs() {
        Service service = Service.create(SERVICE_QNAME);
        // Add a port to the Service
        service.addPort(PORT_QNAME, JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID, JMS_ENDPOINT_URI);
        return service.getPort(HelloWorld.class);
    }

    private static HelloWorld createClientCxf() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        factory.setAddress(JMS_ENDPOINT_URI);
        HelloWorld client = factory.create(HelloWorld.class);
        return client;
    }
}
