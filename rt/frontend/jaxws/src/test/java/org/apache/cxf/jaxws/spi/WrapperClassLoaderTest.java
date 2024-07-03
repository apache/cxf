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

package org.apache.cxf.jaxws.spi;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.common.spi.GeneratedClassClassLoader;
import org.apache.cxf.jaxws.service.SayHi;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

import org.junit.Before;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

public class WrapperClassLoaderTest extends AbstractCXFTest {

    protected LocalTransportFactory localTransport;

    @Before
    public void setUpBus() throws Exception {
        super.setUpBus();

        GeneratedClassClassLoader.TypeHelperClassLoader mockClassLoader =
                Mockito.mock(GeneratedClassClassLoader.TypeHelperClassLoader.class);
        doReturn(SayHi.class).when(mockClassLoader).loadClass("org.apache.cxf.jaxws.service.jaxws_asm.SayHi");
        bus.setExtension(mockClassLoader, GeneratedClassClassLoader.TypeHelperClassLoader.class);

        SoapBindingFactory bindingFactory = new SoapBindingFactory();
        bindingFactory.setBus(bus);
        bus.getExtension(BindingFactoryManager.class)
                .registerBindingFactory("http://schemas.xmlsoap.org/wsdl/soap/", bindingFactory);
        bus.getExtension(BindingFactoryManager.class)
                .registerBindingFactory("http://schemas.xmlsoap.org/wsdl/soap/http", bindingFactory);

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);

        SoapTransportFactory soapDF = new SoapTransportFactory();
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/", soapDF);
        dfm.registerDestinationFactory(SoapBindingConstants.SOAP11_BINDING_ID, soapDF);
        dfm.registerDestinationFactory(SoapBindingConstants.SOAP12_BINDING_ID, soapDF);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/local", soapDF);

        localTransport = new LocalTransportFactory();
        localTransport.setUriPrefixes(new HashSet<>(Arrays.asList("http", "local")));
        dfm.registerDestinationFactory(LocalTransportFactory.TRANSPORT_ID, localTransport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http", localTransport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http/configuration", localTransport);

        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator(LocalTransportFactory.TRANSPORT_ID, localTransport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http", localTransport);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/http", localTransport);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/http/configuration",
                localTransport);
    }

    @org.junit.Test
    public void testWrapperClassLoaderWhenNoWrappedOperations() throws Exception {
        WrapperClassLoader wrapperClassLoader = new WrapperClassLoader(bus);
        JaxWsServiceFactoryBean factory = new JaxWsServiceFactoryBean();

        QName serviceName = new QName(
                "http://apache.org/hello_world_soap_http", "interfaceTest");
        ServiceInfo serviceInfo = new ServiceInfo();
        DescriptionInfo descriptionInfo = new DescriptionInfo();
        descriptionInfo.setName(serviceName);
        serviceInfo.setDescription(descriptionInfo);

        InterfaceInfo interfaceInfo = new InterfaceInfo(serviceInfo, serviceName);
        QName sayHi = new QName("urn:test:ns", "sayHi");
        interfaceInfo.addOperation(sayHi);

        Set<Class<?>> result = wrapperClassLoader.generate(factory, interfaceInfo, false);

        assertTrue(result.isEmpty());
    }

    @org.junit.Test
    public void testWrapperClassLoaderWithWrappedOperations() throws Exception {
        WrapperClassLoader wrapperClassLoader = new WrapperClassLoader(bus);
        JaxWsServiceFactoryBean factory = new JaxWsServiceFactoryBean();

        QName serviceName = new QName(
                "http://apache.org/hello_world_soap_http", "interfaceTest");
        ServiceInfo serviceInfo = new ServiceInfo();
        DescriptionInfo descriptionInfo = new DescriptionInfo();
        descriptionInfo.setName(serviceName);
        serviceInfo.setDescription(descriptionInfo);

        InterfaceInfo interfaceInfo = new InterfaceInfo(serviceInfo, serviceName);
        QName sayHi = new QName("urn:test:ns", "sayHi");
        OperationInfo operationInfo = interfaceInfo.addOperation(sayHi);

        //Add wrapped operation
        QName sayHello = new QName("urn:test:ns", "sayHello");
        OperationInfo wrappedOperation = new OperationInfo();
        wrappedOperation.setName(sayHello);
        MessageInfo wrappedInputInfo = new MessageInfo(wrappedOperation, MessageInfo.Type.INPUT, sayHello);
        wrappedOperation.setInput("sayHello", wrappedInputInfo);
        MessageInfo wrappedOutputInfo = new MessageInfo(wrappedOperation, MessageInfo.Type.OUTPUT, sayHello);
        wrappedOperation.setOutput("sayHello", wrappedOutputInfo);
        operationInfo.setUnwrappedOperation(wrappedOperation);

        //Setup method on operationInfo
        Method method = Mockito.mock(Method.class);
        doReturn(SayHi.class).when(method).getDeclaringClass();
        operationInfo.setProperty(ReflectionServiceFactoryBean.METHOD, method);

        //Setup Input and Output
        MessageInfo inputInfo = new MessageInfo(operationInfo, MessageInfo.Type.INPUT, sayHi);
        inputInfo.addMessagePart(sayHi);
        operationInfo.setInput("sayHi", inputInfo);
        MessageInfo outputInfo = new MessageInfo(operationInfo, MessageInfo.Type.OUTPUT, sayHi);
        outputInfo.addMessagePart(sayHi);
        operationInfo.setOutput("sayHi", outputInfo);

        Set<Class<?>> result = wrapperClassLoader.generate(factory, interfaceInfo, false);

        //Both Input and Output Messages will be found on Classpath
        //org.apache.cxf.jaxws.service.jaxws_asm.SayHi
        assertEquals(2, result.size());
    }
}
