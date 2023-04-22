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

package demo.server;

import java.io.File;
import java.util.Collections;

import jakarta.xml.ws.Endpoint;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

import demo.service.HelloWorld;
import demo.service.impl.HelloWorldImpl;


public final class ServerJMS {
    private static final String JMS_ENDPOINT_URI = "jms:queue:test.cxf.jmstransport.queue?timeToLive=1000"
                                  + "&jndiConnectionFactoryName=ConnectionFactory"
                                  + "&jndiInitialContextFactory"
                                  + "=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
                                  + "&jndiURL=tcp://localhost:61616";

    private ServerJMS() {
        //
    }

    public static void main(String[] args) throws Exception {

        boolean launchAmqBroker = false;
        boolean jaxws = false;

        for (String arg : args) {
            if ("-activemqbroker".equals(arg)) {
                launchAmqBroker = true;
            }
            if ("-jaxws".equals(arg)) {
                jaxws = true;
            }
        }

        if (launchAmqBroker) {
            launchAMQBroker();
        }

        if (jaxws) {
            launchJaxwsApi();
        } else {
            launchCxfApi();
        }

        System.out.println("Server ready... Press any key to exit");
        System.in.read();
        System.out.println("Server exiting");
        System.exit(0);
    }

    private static void launchAMQBroker() throws Exception {
        final Configuration config = new ConfigurationImpl();
        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(false);
        config.addAcceptorConfiguration("tcp", "tcp://localhost:61616");
        config.setBrokerInstance(new File("target/activemq-data"));
       
        final ActiveMQServer broker = new ActiveMQServerImpl(config);
        broker.start();
    }

    private static void launchCxfApi() {
        Object implementor = new HelloWorldImpl();
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(HelloWorld.class);
        svrFactory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        svrFactory.setAddress(JMS_ENDPOINT_URI);
        svrFactory.setServiceBean(implementor);
        svrFactory.setFeatures(Collections.singletonList(new LoggingFeature()));
        svrFactory.create();
    }

    private static void launchJaxwsApi() {
        Endpoint.publish(JMS_ENDPOINT_URI, new HelloWorldImpl(), new LoggingFeature());
    }
}
