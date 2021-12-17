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
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jms_mtom.JMSMTOMPortType;
import org.apache.cxf.jms_mtom.JMSMTOMService;
import org.apache.cxf.jms_mtom.JMSOutMTOMService;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class JMSTestMtom {
    private static EmbeddedJMSBrokerLauncher broker;
    private static Bus bus;

    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher();
        broker.startInProcess();
        bus = BusFactory.getDefaultBus();
        broker.updateWsdl(bus, "testutils/jms_test_mtom.wsdl");
        Object mtom = new JMSMTOMImpl();
        EndpointImpl ep = (EndpointImpl)Endpoint
            .publish("jms:jndi:dynamicQueues/test.cxf.jmstransport.queue&amp;receiveTimeout=10000", mtom);
        Binding binding = ep.getBinding();
        ((SOAPBinding)binding).setMTOMEnabled(true);
    }

    @AfterClass
    public static void stopServers() throws Exception {
        broker.stop();
    }

    @Test
    public void testMTOM() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_mtom", "JMSMTOMService");
        QName portName = new QName("http://cxf.apache.org/jms_mtom", "MTOMPort");

        URL wsdl = getWSDLURL("/wsdl/jms_test_mtom.wsdl");
        JMSMTOMService service = new JMSMTOMService(wsdl, serviceName);

        JMSMTOMPortType mtom = service.getPort(portName, JMSMTOMPortType.class);
        Binding binding = ((BindingProvider)mtom).getBinding();
        ((SOAPBinding)binding).setMTOMEnabled(true);

        Holder<String> name = new Holder<>("Sam");
        URL fileURL = this.getClass().getResource("/org/apache/cxf/systest/jms/JMSClientServerTest.class");
        Holder<DataHandler> handler1 = new Holder<>();
        handler1.value = new DataHandler(fileURL);
        int size = handler1.value.getInputStream().available();
        mtom.testDataHandler(name, handler1);

        byte[] bytes = IOUtils.readBytesFromStream(handler1.value.getInputStream());
        Assert.assertEquals("The response file is not same with the sent file.", size, bytes.length);
        ((Closeable)mtom).close();
    }


    @Test
    public void testOutMTOM() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_mtom", "JMSMTOMService");
        QName portName = new QName("http://cxf.apache.org/jms_mtom", "MTOMPort");

        URL wsdl = getWSDLURL("/wsdl/jms_test_mtom.wsdl");
        JMSOutMTOMService service = new JMSOutMTOMService(wsdl, serviceName);

        JMSMTOMPortType mtom = service.getPort(portName, JMSMTOMPortType.class);
        URL fileURL = this.getClass().getResource("/org/apache/cxf/systest/jms/JMSClientServerTest.class");
        DataHandler handler1 = new DataHandler(fileURL);
        int size = handler1.getInputStream().available();
        DataHandler ret = mtom.testOutMtom();

        byte[] bytes = IOUtils.readBytesFromStream(ret.getInputStream());
        Assert.assertEquals("The response file is not same with the original file.", size, bytes.length);
        ((Closeable)mtom).close();
    }

    public static URL getWSDLURL(String s) throws Exception {
        URL u = JMSTestMtom.class.getResource(s);
        if (u == null) {
            throw new IllegalArgumentException("WSDL classpath resource not found " + s);
        }
        String wsdlString = u.toString().intern();
        broker.updateWsdl(bus, wsdlString);
        return u;
    }
}
