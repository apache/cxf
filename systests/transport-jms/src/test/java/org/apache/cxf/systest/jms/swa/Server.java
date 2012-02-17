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

package org.apache.cxf.systest.jms.swa;

import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

public class Server extends AbstractBusTestServerBase {
    public static final String ADDRESS 
        = "jms:jndi:dynamicQueues/test.cxf.jmstransport.swa.queue"
        + "?jndiInitialContextFactory"
        + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
        + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:"
        + EmbeddedJMSBrokerLauncher.PORT;
    
    protected void run() {
        try {
            JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
            factory.setWsdlLocation("classpath:wsdl/swa-mime.wsdl");
            factory.setTransportId("http://cxf.apache.org/transports/jms");
            factory.setServiceName(new QName("http://cxf.apache.org/swa", "SwAService"));
            factory.setEndpointName(new QName("http://cxf.apache.org/swa", "SwAServiceHttpPort"));
            factory.setAddress(Server.ADDRESS);
            factory.setServiceBean(new SwAServiceImpl());
            factory.create().start();
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String args[]) {
        try {
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
