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

import java.io.Closeable;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerSwaTest extends AbstractBusClientServerTestBase {
    public static final String ADDRESS 
        = "jms:jndi:dynamicQueues/test.cxf.jmstransport.swa.queue"
            + "?jndiInitialContextFactory"
            + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=";
    
    static EmbeddedJMSBrokerLauncher broker;
    
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            try {
                JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
                factory.setWsdlLocation("classpath:wsdl/swa-mime.wsdl");
                factory.setTransportId("http://cxf.apache.org/transports/jms");
                factory.setServiceName(new QName("http://cxf.apache.org/swa", "SwAService"));
                factory.setEndpointName(new QName("http://cxf.apache.org/swa", "SwAServiceHttpPort"));
                factory.setAddress(ADDRESS + broker.getBrokerURL());
                factory.setServiceBean(new SwAServiceImpl());
                factory.create().start();
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher("vm://ClientServerSwaTest");
        System.setProperty("EmbeddedBrokerURL", broker.getBrokerURL());
        launchServer(broker);
        launchServer(new Server());
        createStaticBus();
    }
    @AfterClass
    public static void clearProperty() {
        System.clearProperty("EmbeddedBrokerURL");
    }    
    @Test
    public void testSwa() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setWsdlLocation("classpath:wsdl/swa-mime.wsdl");
        factory.setTransportId("http://cxf.apache.org/transports/jms");
        factory.setServiceName(new QName("http://cxf.apache.org/swa", "SwAService"));
        factory.setEndpointName(new QName("http://cxf.apache.org/swa", "SwAServiceHttpPort"));
        factory.setAddress(ADDRESS + broker.getBrokerURL());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        SwAService port = factory.create(SwAService.class);
        
        
        Holder<String> textHolder = new Holder<String>();
        Holder<DataHandler> data = new Holder<DataHandler>();
        
        ByteArrayDataSource source = new ByteArrayDataSource("foobar".getBytes(), "application/octet-stream");
        DataHandler handler = new DataHandler(source);
        
        data.value = handler;
        
        textHolder.value = "Hi";

        port.echoData(textHolder, data);
        InputStream bis = null;
        bis = data.value.getDataSource().getInputStream();
        byte b[] = new byte[10];
        bis.read(b, 0, 10);
        String string = IOUtils.newStringFromBytes(b);
        assertEquals("testfoobar", string);
        assertEquals("Hi", textHolder.value);
        
        if (port instanceof Closeable) {
            ((Closeable)port).close();
        }
    }
}
