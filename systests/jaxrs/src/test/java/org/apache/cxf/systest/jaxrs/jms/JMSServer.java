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
package org.apache.cxf.systest.jaxrs.jms;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JMSServer extends AbstractBusTestServerBase {

    ClassPathXmlApplicationContext context;

    protected void run()  {
        context = new ClassPathXmlApplicationContext("org/apache/cxf/systest/jaxrs/jms/jms_server_config.xml");
        context.start();
    }

    public void tearDown() {
        context.getBean(JAXRSServerFactoryBean.class).getServer().destroy();
        context.close();
    }

    public static void main(String[] args) {
        try {
            final Configuration config = new ConfigurationImpl();
            config.setPersistenceEnabled(false);
            config.setSecurityEnabled(false);
            config.addAcceptorConfiguration("tcp", "tcp://localhost:61500");
           
            final ActiveMQServer broker = new ActiveMQServerImpl(config);
            broker.start();

            System.setProperty("testutil.ports.EmbeddedJMSBrokerLauncher", "61500");
            JMSServer s = new JMSServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
