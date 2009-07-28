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
import java.lang.reflect.UndeclaredThrowableException;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.axiom.attachments.utils.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.mime.types.XopStringType;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientMtomXopTest extends AbstractBusClientServerTestBase {

    public static final QName MTOM_PORT = new QName("http://cxf.apache.org/mime", "TestMtomPort");
    public static final QName MTOM_SERVICE = new QName("http://cxf.apache.org/mime", "TestMtomService");

    @BeforeClass
    public static void startServers() throws Exception {
        TestUtilities.setKeepAliveSystemProperty(false);
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @AfterClass
    public static void cleanup() {
        TestUtilities.recoverKeepAliveSystemProperty();
    }

    @Test
    public void testMtomXop() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true, true);
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
            
            ((BindingProvider)mtomPort).getRequestContext().put("schema-validation-enabled",
                                                                Boolean.TRUE);
            
            param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
            Holder<String> name = new Holder<String>("call detail");
            mtomPort.testXop(name, param);
            assertEquals("name unchanged", "return detail + call detail", name.value);
            assertNotNull(param.value);
            
            InputStream in = param.value.getInputStream();
            byte bytes[] = IOUtils.getStreamAsByteArray(in);
            assertEquals(data.length, bytes.length);
            in.close();

            param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
            name = new Holder<String>("call detail");
            mtomPort.testXop(name, param);
            assertEquals("name unchanged", "return detail + call detail", name.value);
            assertNotNull(param.value);
            
            in = param.value.getInputStream();
            bytes = IOUtils.getStreamAsByteArray(in);
            assertEquals(data.length, bytes.length);
            in.close();

        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @org.junit.Ignore // see CXF-1395
    @Test
    public void testMtoMString() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true, false);
        InputStream pre = this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl");
        long fileSize = 0;
        for (int i = pre.read(); i != -1; i = pre.read()) {
            fileSize++;
        }
        byte[] data = new byte[(int)fileSize];
        this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl").read(data);
        String stringValue = new String(data, "utf-8");
        XopStringType xsv = new XopStringType();
        xsv.setAttachinfo(stringValue);
        xsv.setName("eman");
        XopStringType r = mtomPort.testXopString(xsv);
        assertNotNull(r);
    }

    private static <T> T createPort(QName serviceName, QName portName, Class<T> serviceEndpointInterface,
                                    boolean enableMTOM, boolean installInterceptors) throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        ReflectionServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(bus);
        serviceFactory.setServiceName(serviceName);
        serviceFactory.setServiceClass(serviceEndpointInterface);
        serviceFactory.setWsdlURL(ClientMtomXopTest.class.getResource("/wsdl/mtom_xop.wsdl"));
        Service service = serviceFactory.create();
        EndpointInfo ei = service.getEndpointInfo(portName);
        JaxWsEndpointImpl jaxwsEndpoint = new JaxWsEndpointImpl(bus, service, ei);
        SOAPBinding jaxWsSoapBinding = new SOAPBindingImpl(ei.getBinding());
        jaxWsSoapBinding.setMTOMEnabled(enableMTOM);

        if (installInterceptors) {
            jaxwsEndpoint.getBinding().getInInterceptors().add(new TestMultipartMessageInterceptor());
            jaxwsEndpoint.getBinding().getOutInterceptors().add(new TestAttachmentOutInterceptor());
        }
        
        jaxwsEndpoint.getBinding().getInInterceptors().add(new LoggingInInterceptor());
        jaxwsEndpoint.getBinding().getOutInterceptors().add(new LoggingOutInterceptor());

        Client client = new ClientImpl(bus, jaxwsEndpoint);
        InvocationHandler ih = new JaxWsClientProxy(client, jaxwsEndpoint.getJaxwsBinding());
        Object obj = Proxy
            .newProxyInstance(serviceEndpointInterface.getClassLoader(),
                              new Class[] {serviceEndpointInterface, BindingProvider.class}, ih);
        return serviceEndpointInterface.cast(obj);
    }
}
