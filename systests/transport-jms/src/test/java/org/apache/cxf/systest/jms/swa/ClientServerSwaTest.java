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

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.Holder;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSConstants;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientServerSwaTest extends AbstractBusClientServerTestBase {
    public static final String ADDRESS
        = "jms:jndi:dynamicQueues/test.cxf.jmstransport.swa.queue"
            + "?jndiInitialContextFactory"
            + "=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=";

    static EmbeddedJMSBrokerLauncher broker;

    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            try {
                JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
                factory.setBus(getBus());
                factory.setWsdlLocation("classpath:/swa-mime_jms.wsdl");
                factory.setTransportId(SoapJMSConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID);
                factory.setServiceName(new QName("http://cxf.apache.org/swa", "SwAService"));
                factory.setEndpointName(new QName("http://cxf.apache.org/swa", "SwAServiceJMSPort"));
                factory.setAddress(ADDRESS + broker.getEncodedBrokerURL());
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
        broker = new EmbeddedJMSBrokerLauncher();
        System.setProperty("EmbeddedBrokerURL", broker.getBrokerURL());
        launchServer(broker);
        launchServer(new Server());
        createStaticBus();
    }
    @AfterClass
    public static void clearProperty() throws Exception {
        System.clearProperty("EmbeddedBrokerURL");
        stopAllServers();
    }
    @Test
    public void testSwa() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setWsdlLocation("classpath:/swa-mime_jms.wsdl");
        factory.setTransportId(SoapJMSConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID);
        factory.setServiceName(new QName("http://cxf.apache.org/swa", "SwAService"));
        factory.setEndpointName(new QName("http://cxf.apache.org/swa", "SwAServiceJMSPort"));
        factory.setAddress(ADDRESS + broker.getEncodedBrokerURL());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        SwAService port = factory.create(SwAService.class);


        Holder<String> textHolder = new Holder<>();
        Holder<DataHandler> data = new Holder<>();

        ByteArrayDataSource source = new ByteArrayDataSource("foobar".getBytes(), "application/octet-stream");
        DataHandler handler = new DataHandler(source);

        data.value = handler;

        textHolder.value = "Hi";

        port.echoData(textHolder, data);
        InputStream bis = null;
        bis = data.value.getDataSource().getInputStream();
        byte[] b = new byte[10];
        bis.read(b, 0, 10);
        String string = IOUtils.newStringFromBytes(b);
        assertEquals("testfoobar", string);
        assertEquals("Hi", textHolder.value);

        if (port instanceof Closeable) {
            ((Closeable)port).close();
        }
    }
}
