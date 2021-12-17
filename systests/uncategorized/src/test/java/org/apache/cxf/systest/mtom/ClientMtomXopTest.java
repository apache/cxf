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
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.mime.types.XopStringType;
import org.apache.cxf.mtom_xop.TestMtomImpl;
import org.apache.cxf.mtom_xop.TestMtomProviderImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClientMtomXopTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(ClientMtomXopTest.class);
    public static final QName MTOM_PORT = new QName("http://cxf.apache.org/mime", "TestMtomPort");
    public static final QName MTOM_PORT_PROVIDER = new QName("http://cxf.apache.org/mime", "TestMtomProviderPort");
    public static final QName MTOM_SERVICE = new QName("http://cxf.apache.org/mime", "TestMtomService");



    public static class Server extends AbstractBusTestServerBase {
        EndpointImpl jaxep;
        protected void run() {
            Object implementor = new TestMtomImpl();
            String address = "http://localhost:" + PORT + "/mime-test";
            String addressProvider = "http://localhost:" + PORT + "/mime-test-provider";
            try {
                jaxep = (EndpointImpl) jakarta.xml.ws.Endpoint.publish(address, implementor);
                Endpoint ep = jaxep.getServer().getEndpoint();
                ep.getInInterceptors().add(new TestMultipartMessageInterceptor());
                ep.getOutInterceptors().add(new TestAttachmentOutInterceptor());
                LoggingInInterceptor logIn = new LoggingInInterceptor();
                logIn.setLogBinary(false);
                logIn.setLogMultipart(true);
                LoggingOutInterceptor logOut = new LoggingOutInterceptor();
                logOut.setLogBinary(false);
                logOut.setLogMultipart(true);
                jaxep.getInInterceptors().add(logIn);
                jaxep.getOutInterceptors().add(logOut);
                SOAPBinding jaxWsSoapBinding = (SOAPBinding) jaxep.getBinding();
                jaxep.getProperties().put("schema-validation-enabled", "true");
                jaxWsSoapBinding.setMTOMEnabled(true);
                EndpointImpl endpoint =
                    (EndpointImpl)jakarta.xml.ws.Endpoint.publish(addressProvider, new TestMtomProviderImpl());
                endpoint.getProperties().put("schema-validation-enabled", "true");
                endpoint.getInInterceptors().add(logIn);
                endpoint.getOutInterceptors().add(logOut);
                

            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
        public void tearDown() {
            jaxep.stop();
            jaxep = null;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testMtomXop() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true, true);
        try {
            Holder<DataHandler> param = new Holder<>();
            Holder<String> name;
            byte[] bytes;
            InputStream in;

            InputStream pre = this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl");
            int fileSize = 0;
            for (int i = pre.read(); i != -1; i = pre.read()) {
                fileSize++;
            }

            int count = 50;
            byte[] data = new byte[fileSize *  count];
            for (int x = 0; x < count; x++) {
                this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl").read(data,
                                                                                fileSize * x,
                                                                                fileSize);
            }

            Object[] validationTypes = new Object[]{Boolean.TRUE, SchemaValidationType.IN, SchemaValidationType.BOTH};

            for (Object validationType : validationTypes) {
                ((BindingProvider)mtomPort).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED,
                                                                    validationType);

                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();

                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();
            }

            validationTypes = new Object[]{Boolean.FALSE, SchemaValidationType.OUT, SchemaValidationType.NONE};
            for (Object validationType : validationTypes) {
                ((BindingProvider)mtomPort).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED,
                                                                validationType);
                SAAJOutInterceptor saajOut = new SAAJOutInterceptor();
                SAAJInInterceptor saajIn = new SAAJInInterceptor();

                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();

                ClientProxy.getClient(mtomPort).getInInterceptors().add(saajIn);
                ClientProxy.getClient(mtomPort).getInInterceptors().add(saajOut);
                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();

                ClientProxy.getClient(mtomPort).getInInterceptors().remove(saajIn);
                ClientProxy.getClient(mtomPort).getInInterceptors().remove(saajOut);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } catch (Exception ex) {
            System.out.println(System.getProperties());
            throw ex;
        }
    }

    
    @Test
    public void testMtomWithValidationErrorOnServer() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true, true);
        try {
            Holder<DataHandler> param = new Holder<>();
            Holder<String> name;

            InputStream pre = this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl");
            int fileSize = 0;
            for (int i = pre.read(); i != -1; i = pre.read()) {
                fileSize++;
            }

            int count = 1;
            byte[] data = new byte[fileSize *  count];
            for (int x = 0; x < count; x++) {
                this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl").read(data,
                                                                                fileSize * x,
                                                                                fileSize);
            }

            
            param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
            //name length > 80 to break the schema 
            //will throw exception on server side
            name = new Holder<>("break schema");
            ClientProxy.getClient(mtomPort).getInInterceptors().add(new LoggingInInterceptor());
            ClientProxy.getClient(mtomPort).getOutInterceptors().add(new LoggingOutInterceptor());
            ((HTTPConduit)ClientProxy.getClient(mtomPort).getConduit()).getClient().setReceiveTimeout(60000);
            mtomPort.testXop(name, param);
            fail("should throw jakarta.xml.ws.soap.SOAPFaultException");
            
        } catch (jakarta.xml.ws.soap.SOAPFaultException  ex) {
            assertTrue(ex.getMessage().contains("cvc-maxLength-valid"));
        } catch (Exception ex) {
            fail("should throw jakarta.xml.ws.soap.SOAPFaultException");
        }
    }

    @Test
    public void testMtomXopProvider() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT_PROVIDER, TestMtom.class, true, true);
        try {
            Holder<DataHandler> param = new Holder<>();
            Holder<String> name;
            byte[] bytes;
            InputStream in;

            InputStream pre = this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl");
            int fileSize = 0;
            for (int i = pre.read(); i != -1; i = pre.read()) {
                fileSize++;
            }

            int count = 50;
            byte[] data = new byte[fileSize *  count];
            for (int x = 0; x < count; x++) {
                this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl").read(data,
                                                                                fileSize * x,
                                                                                fileSize);
            }

            Object[] validationTypes = new Object[]{Boolean.TRUE, SchemaValidationType.IN, SchemaValidationType.BOTH};

            for (Object validationType : validationTypes) {
                ((BindingProvider)mtomPort).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED,
                                                                    validationType);

                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();

                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();
            }

            validationTypes = new Object[]{Boolean.FALSE, SchemaValidationType.OUT, SchemaValidationType.NONE};
            for (Object validationType : validationTypes) {
                ((BindingProvider)mtomPort).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED,
                                                                validationType);
                SAAJOutInterceptor saajOut = new SAAJOutInterceptor();
                SAAJInInterceptor saajIn = new SAAJInInterceptor();

                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();

                ClientProxy.getClient(mtomPort).getInInterceptors().add(saajIn);
                ClientProxy.getClient(mtomPort).getInInterceptors().add(saajOut);
                param.value = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
                name = new Holder<>("call detail");
                mtomPort.testXop(name, param);
                assertEquals("name unchanged", "return detail + call detail", name.value);
                assertNotNull(param.value);

                in = param.value.getInputStream();
                bytes = IOUtils.readBytesFromStream(in);
                assertEquals(data.length, bytes.length);
                in.close();

                ClientProxy.getClient(mtomPort).getInInterceptors().remove(saajIn);
                ClientProxy.getClient(mtomPort).getInInterceptors().remove(saajOut);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } catch (Exception ex) {
            System.out.println(System.getProperties());
            throw ex;
        }
    }

    @Ignore("failed on jenkins CI")
    public void testMtomWithChineseFileName() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true, true);
        try {
            final Holder<DataHandler> param = new Holder<>();

            URL fileURL = getClass().getResource("/\u6d4b\u8bd5.bmp");
            assertNotNull(fileURL);

            Object[] validationTypes = new Object[]{Boolean.TRUE, SchemaValidationType.IN, SchemaValidationType.BOTH};
            for (Object validationType : validationTypes) {
                ((BindingProvider)mtomPort).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED,
                                                                    validationType);
                param.value = new DataHandler(fileURL);
                final Holder<String> name = new Holder<>("have name");
                mtomPort.testXop(name, param);

                assertEquals("can't get file name", "return detail   测试.bmp",
                    java.net.URLDecoder.decode(name.value, StandardCharsets.UTF_8.name()));
                assertNotNull(param.value);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } catch (Exception ex) {
            throw ex;
        }
    }
    
    @Test
    public void testMtomWithFileName() throws Exception {
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class, true, true);
        try {
            Holder<DataHandler> param = new Holder<>();
            Holder<String> name;

            URL fileURL = getClass().getClassLoader().getResource("me.bmp");

            Object[] validationTypes = new Object[]{Boolean.TRUE, SchemaValidationType.IN, SchemaValidationType.BOTH};
            for (Object validationType : validationTypes) {
                ((BindingProvider)mtomPort).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED,
                                                                    validationType);
                param.value = new DataHandler(fileURL);
                name = new Holder<>("have name");
                mtomPort.testXop(name, param);
                assertEquals("can't get file name", "return detail + me.bmp", name.value);
                assertNotNull(param.value);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } catch (Exception ex) {
            System.out.println(System.getProperties());
            throw ex;
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

    private <T> T createPort(QName serviceName, QName portName, Class<T> serviceEndpointInterface,
                                    boolean enableMTOM, boolean installInterceptors) throws Exception {
        ReflectionServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        Bus bus = getStaticBus();
        LoggingFeature lf = new LoggingFeature();
        lf.setPrettyLogging(false);
        lf.setLogBinary(false);
        lf.setLogMultipart(true);
        bus.getFeatures().add(lf);
        serviceFactory.setBus(bus);
        serviceFactory.setServiceName(serviceName);
        serviceFactory.setServiceClass(serviceEndpointInterface);
        serviceFactory.setWsdlURL(ClientMtomXopTest.class.getResource("/wsdl/mtom_xop.wsdl"));
        Service service = serviceFactory.create();
        EndpointInfo ei = service.getEndpointInfo(portName);
        JaxWsEndpointImpl jaxwsEndpoint = new JaxWsEndpointImpl(bus, service, ei);
        SOAPBinding jaxWsSoapBinding = new SOAPBindingImpl(ei.getBinding(), jaxwsEndpoint);
        jaxWsSoapBinding.setMTOMEnabled(enableMTOM);

        if (installInterceptors) {
            //jaxwsEndpoint.getBinding().getInInterceptors().add(new TestMultipartMessageInterceptor());
            jaxwsEndpoint.getBinding().getOutInterceptors().add(new TestAttachmentOutInterceptor());
        }

        jaxwsEndpoint.getBinding().getInInterceptors().add(new LoggingInInterceptor());
        jaxwsEndpoint.getBinding().getOutInterceptors().add(new LoggingOutInterceptor());

        Client client = new ClientImpl(bus, jaxwsEndpoint);
        InvocationHandler ih = new JaxWsClientProxy(client, jaxwsEndpoint.getJaxwsBinding());
        Object obj = Proxy
            .newProxyInstance(serviceEndpointInterface.getClassLoader(),
                              new Class[] {serviceEndpointInterface, BindingProvider.class}, ih);
        updateAddressPort(obj, PORT);
        return serviceEndpointInterface.cast(obj);
    }
}
