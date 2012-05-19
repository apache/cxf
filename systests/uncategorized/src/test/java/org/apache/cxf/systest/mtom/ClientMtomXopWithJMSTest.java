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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.BeforeClass;
import org.junit.Test;

public class ClientMtomXopWithJMSTest extends AbstractBusClientServerTestBase {
    public static final String JMS_PORT = EmbeddedJMSBrokerLauncher.PORT;
    
    public static final QName MTOM_PORT = new QName("http://cxf.apache.org/mime", "TestMtomJMSPort");
    public static final QName MTOM_SERVICE = new QName("http://cxf.apache.org/mime", "TestMtomJMSService");

    public static class ServerWithJMS extends AbstractBusTestServerBase {
        EndpointImpl jaxep;
        protected void run() {
            Object implementor = new TestMtomJMSImpl();
            String address = "http://not.required.for.jms";
            try {
                Bus bus = BusFactory.getDefaultBus();
                setBus(bus);
                EmbeddedJMSBrokerLauncher.updateWsdlExtensors(bus, "testutils/mtom_xop.wsdl");
                
                jaxep = (EndpointImpl) javax.xml.ws.Endpoint.publish(address, implementor);
                Endpoint ep = jaxep.getServer().getEndpoint();
                ep.getInInterceptors().add(new TestMultipartMessageInterceptor());
                ep.getOutInterceptors().add(new TestAttachmentOutInterceptor());
                ep.getInInterceptors().add(new LoggingInInterceptor());
                ep.getOutInterceptors().add(new LoggingOutInterceptor());
                SOAPBinding jaxWsSoapBinding = (SOAPBinding) jaxep.getBinding();
                jaxWsSoapBinding.setMTOMEnabled(true);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public void tearDown() {
            jaxep.stop();
        }

    }

    @BeforeClass
    public static void startServers() throws Exception {
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("org.apache.activemq.default.directory.prefix") != null) {
            props.put("org.apache.activemq.default.directory.prefix",
                      System.getProperty("org.apache.activemq.default.directory.prefix"));
        }
        props.put("java.util.logging.config.file", 
                  System.getProperty("java.util.logging.config.file"));
        
        assertTrue("server did not launch correctly", 
                   launchServer(EmbeddedJMSBrokerLauncher.class, props, null));
        assertTrue("server did not launch correctly", launchServer(ServerWithJMS.class, true));
        createStaticBus();
    }
    
    
    @Test
    public void testMtomXop() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true);
        try {
            InputStream pre = this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl");
            int fileSize = 0;
            for (int i = pre.read(); i != -1; i = pre.read()) {
                fileSize++;
            }
            Holder<DataHandler> param = new Holder<DataHandler>();
            
            int count = 50;
            byte[] data = new byte[fileSize *  count];
            for (int x = 0; x < count; x++) {
                this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl").read(data, 
                                                                                fileSize * x,
                                                                                fileSize);
            }
            
            param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
            Holder<String> name = new Holder<String>("call detail");
            mtomPort.testXop(name, param);
            fail("Expect the exception here !");
            assertEquals("name unchanged", "return detail + call detail", name.value);
            assertNotNull(param.value);
            param.value.getInputStream().close();
            
        } catch (SOAPFaultException ex) {
            assertTrue("Expect the configuration exception here",
                       ex.getCause() instanceof org.apache.cxf.configuration.ConfigurationException);
        }
    }

    private static <T> T createPort(QName serviceName, 
                                    QName portName, 
                                    Class<T> serviceEndpointInterface,
                                    boolean enableMTOM)
        throws Exception {
        Bus bus = getStaticBus();
        ReflectionServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(bus);
        serviceFactory.setServiceName(serviceName);
        serviceFactory.setServiceClass(serviceEndpointInterface);
        serviceFactory.setWsdlURL(ClientMtomXopTest.class.getResource("/wsdl/mtom_xop.wsdl"));
        Service service = serviceFactory.create();
        EndpointInfo ei = service.getEndpointInfo(portName);
        JaxWsEndpointImpl jaxwsEndpoint = new JaxWsEndpointImpl(bus, service, ei);
        SOAPBinding jaxWsSoapBinding = new SOAPBindingImpl(ei.getBinding(), jaxwsEndpoint);
        jaxWsSoapBinding.setMTOMEnabled(enableMTOM);
        
        jaxwsEndpoint.getBinding().getInInterceptors().add(new TestMultipartMessageInterceptor());
        jaxwsEndpoint.getBinding().getOutInterceptors().add(new TestAttachmentOutInterceptor());
        
        Client client = new ClientImpl(bus, jaxwsEndpoint);
        InvocationHandler ih = new JaxWsClientProxy(client, jaxwsEndpoint.getJaxwsBinding());
        Object obj = Proxy.newProxyInstance(serviceEndpointInterface.getClassLoader(), new Class[] {
            serviceEndpointInterface, BindingProvider.class }, ih);
        return serviceEndpointInterface.cast(obj);
    }
}
