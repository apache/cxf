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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.ws.Endpoint;

import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

import demo.service.HelloWorld;
import demo.service.impl.HelloWorldImpl;


public final class ServerJMS {
    private static final String JMS_ENDPOINT_URI = "jms:queue:test.cxf.jmstransport.queue?timeToLive=1000"
                                  + "&jndiConnectionFactoryName=ConnectionFactory"
                                  + "&jndiInitialContextFactory"
                                  + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                                  + "&jndiURL=tcp://localhost:61616";

    private ServerJMS() {
        //
    }

    public static void main(String args[]) throws Exception {

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

    private static void launchAMQBroker() throws ClassNotFoundException, InstantiationException,
        IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        /*
         * The following make it easier to run this against something other than ActiveMQ. You will have
         * to get a JMS broker onto the right port of localhost.
         */
        Class<?> brokerClass = ServerJMS.class.getClassLoader()
            .loadClass("org.apache.activemq.broker.BrokerService");
        if (brokerClass == null) {
            System.err.println("ActiveMQ is not in the classpath, cannot launch broker.");
            return;
        }
        Object broker = brokerClass.newInstance();
        Method addConnectorMethod = brokerClass.getMethod("addConnector", String.class);
        addConnectorMethod.invoke(broker, "tcp://localhost:61616");
        Method setDataDirectory = brokerClass.getMethod("setDataDirectory", String.class);
        setDataDirectory.invoke(broker, "target/activemq-data");
        Method startMethod = brokerClass.getMethod("start");
        startMethod.invoke(broker);
    }

    private static void launchCxfApi() {
        Object implementor = new HelloWorldImpl();
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(HelloWorld.class);
        svrFactory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        svrFactory.setAddress(JMS_ENDPOINT_URI);
        svrFactory.setServiceBean(implementor);
        svrFactory.create();
    }

    private static void launchJaxwsApi() {
        Endpoint.publish(JMS_ENDPOINT_URI, new HelloWorldImpl());
    }
}
