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
package org.apache.cxf.osgi.itests.soap;

import java.util.Collections;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class JmsTestActivator implements BundleActivator {
    private Server server;

    @Override
    public void start(BundleContext bc) throws Exception {
        ConnectionFactory connectionFactory = createConnectionFactory();
        server = publishService(connectionFactory);
    }

    private Server publishService(ConnectionFactory connectionFactory) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress("jms:queue:greeter");
        factory.setFeatures(Collections.singletonList(new ConnectionFactoryFeature(connectionFactory)));
        factory.setServiceBean(new GreeterImpl());
        return factory.create();
    }

    private ActiveMQConnectionFactory createConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory
            = new ActiveMQConnectionFactory("vm://JmsServiceTest");
        connectionFactory.setUser("karaf");
        connectionFactory.setPassword("karaf");
        return connectionFactory;
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        server.destroy();
    }
}
