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

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.jms.ConnectionFactory;
import jakarta.xml.ws.Endpoint;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;

import org.junit.After;
import org.junit.AfterClass;

/**
 * Base class for JMS tests that use the an in VM ConnectionFactory.
 *
 *  The idea is to start the bus and services in the @BeforeClass of the test.
 *  In each test method clients are created and marked for removal.
 *  The base class then makes sure that all clients are closed after each test method
 *  and that the bus is shut down after the whole test class.
 */
public abstract class AbstractVmJMSTest {
    protected static Bus bus;
    protected static ConnectionFactoryFeature cff;
    protected static ConnectionFactory cf;
    protected static ActiveMQServer broker;
    private List<Object> closeableResources = new ArrayList<>();

    public static void startBusAndJMS(Class<?> testClass) {
        String brokerURI = "vm://" + testClass.getName() + "?broker.persistent=false&broker.useJmx=false";
        startBusAndJMS(brokerURI);
        startBroker(brokerURI);
    }

    public static void startBusAndJMS(String brokerURI) {
        bus = BusFactory.getDefaultBus();
        cf = new ActiveMQConnectionFactory(brokerURI);
        cff = new ConnectionFactoryFeature(cf);
    }

    public static void startBroker(String brokerURI) {
        try {
            final Configuration config = new ConfigurationImpl()
                .setSecurityEnabled(false)
                .setPersistenceEnabled(false)
                .setJMXManagementEnabled(false)
                .addAcceptorConfiguration("#", brokerURI)
                .addAddressesSetting("#",
                    new AddressSettings()
                        .setMaxDeliveryAttempts(1)
                        .setRedeliveryDelay(1000L));
            config.setBrokerInstance(new File("./target"));
            
            broker = new ActiveMQServerImpl(config);
            broker.start();
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> T markForClose(T resource) {
        closeableResources.add(resource);
        return resource;
    }

    @After
    public void stopClosables() {
        for (Object proxy : closeableResources) {
            if (proxy instanceof Closeable) {
                try {
                    ((Closeable)proxy).close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        closeableResources.clear();
    }

    @AfterClass
    public static void stopBus() throws Exception {
        bus.shutdown(false);
        bus = null;
        cf = null;
        cff = null;
        if (broker != null) {
            broker.stop();
            broker = null;
        }
    }

    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        if (u == null) {
            throw new IllegalArgumentException("WSDL classpath resource not found " + s);
        }
        return u;
    }

    public static void publish(String address, Object impl) {
        EndpointImpl ep = (EndpointImpl)Endpoint.create(impl);
        ep.setBus(bus);
        ep.getFeatures().add(cff);
        ep.publish(address);
    }

    public static void publish(Object impl) {
        publish(null, impl);
    }

}
