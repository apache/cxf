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

package demo.jms_greeter.broker;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

public final class EmbeddedBroker {
    private EmbeddedBroker() {
    }

    public static void main(String[] args) throws Exception {
        Configuration config = new ConfigurationImpl();
        Set<TransportConfiguration> transports = new HashSet<>();
        transports.add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
        transports.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
        config.setAcceptorConfigurations(transports); 
        config.setBrokerInstance(new File("target/artemis"));
        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(false);
        config.setJMXManagementEnabled(false);
        
        EmbeddedActiveMQ server = new EmbeddedActiveMQ();
        server.setConfiguration(config);
        server.start();
        try {
            System.out.println("JMS broker ready ...");
            Thread.sleep(125 * 60 * 1000);
        } finally {
            System.out.println("JMS broker exiting");
            server.stop();
        }
        System.exit(0);
    }
}

