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
package org.apache.cxf.systest.mtom;

import java.io.InputStream;
import java.util.Collections;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.soap.SOAPBinding;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientMtomXopWithJMSTest {
    public static final QName MTOM_PORT = new QName("http://cxf.apache.org/mime", "TestMtomJMSPort");
    public static final QName MTOM_SERVICE = new QName("http://cxf.apache.org/mime", "TestMtomJMSService");

    private static Bus bus;
    private static ConnectionFactoryFeature cff;

    @BeforeClass
    public static void startServers() throws Exception {
        Object implementor = new TestMtomJMSImpl();
        bus = BusFactory.getDefaultBus();

        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        cff = new ConnectionFactoryFeature(cf);

        EndpointImpl ep = (EndpointImpl)Endpoint.create(implementor);
        ep.getFeatures().add(cff);
        ep.getInInterceptors().add(new TestMultipartMessageInterceptor());
        ep.getOutInterceptors().add(new TestAttachmentOutInterceptor());
        //ep.getInInterceptors().add(new LoggingInInterceptor());
        //ep.getOutInterceptors().add(new LoggingOutInterceptor());
        SOAPBinding jaxWsSoapBinding = (SOAPBinding)ep.getBinding();
        jaxWsSoapBinding.setMTOMEnabled(true);
        ep.publish();
    }

    @AfterClass
    public static void stopServers() throws Exception {
        bus.shutdown(false);
    }

    @Test
    public void testMtomXop() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true);
        InputStream pre = this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl");
        int fileSize = 0;
        for (int i = pre.read(); i != -1; i = pre.read()) {
            fileSize++;
        }
        Holder<DataHandler> param = new Holder<>();

        int count = 50;
        byte[] data = new byte[fileSize * count];
        for (int x = 0; x < count; x++) {
            this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl").read(data, fileSize * x, fileSize);
        }

        param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
        Holder<String> name = new Holder<>("call detail");
        mtomPort.testXop(name, param);

        // TODO Why should it fail here?
        // Assert.fail("Expect the exception here !");

        Assert.assertEquals("name unchanged", "return detail + call detail", name.value);
        Assert.assertNotNull(param.value);
        param.value.getInputStream().close();

    }

    private static <T> T createPort(QName serviceName, QName portName, Class<T> serviceEndpointInterface,
                                    boolean enableMTOM) throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceName(serviceName);
        factory.setServiceClass(serviceEndpointInterface);
        factory.setWsdlURL(ClientMtomXopTest.class.getResource("/wsdl/mtom_xop.wsdl").toExternalForm());
        factory.setFeatures(Collections.singletonList(cff));
        factory.getInInterceptors().add(new TestMultipartMessageInterceptor());
        factory.getOutInterceptors().add(new TestAttachmentOutInterceptor());
        @SuppressWarnings("unchecked")
        T proxy = (T)factory.create();
        BindingProvider bp = (BindingProvider)proxy;
        SOAPBinding binding = (SOAPBinding)bp.getBinding();
        binding.setMTOMEnabled(true);
        return proxy;
    }
}
