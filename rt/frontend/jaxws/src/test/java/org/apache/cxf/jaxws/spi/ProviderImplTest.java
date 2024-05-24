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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.spi.Invoker;
import jakarta.xml.ws.spi.ServiceDelegate;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.hello_world_soap_http.GreeterImpl;

import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;



public class ProviderImplTest extends AbstractCXFTest {

    protected LocalTransportFactory localTransport;

    private final Invoker validInvoker = new Invoker() {
        @Override
        public void inject(WebServiceContext webServiceContext)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        }

        @Override
        public Object invoke(Method method, Object... objects)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return null;
        }
    };

    private final Invoker invalidInvoker = new Invoker() {
        @Override
        public void inject(WebServiceContext webServiceContext)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            throw new WebServiceException("Oops");
        }

        @Override
        public Object invoke(Method method, Object... objects)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return null;
        }
    };

    @Before
    public void setUpBus() throws Exception {
        super.setUpBus();

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
    public void testCreateW3CEpr() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org", "ServiceName");
        QName portName = new QName("http://cxf.apache.org", "PortName");
        ProviderImpl impl = new ProviderImpl();
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference("http://myaddress", serviceName,
                                                                      portName, null, "wsdlLoc",
                                                                      null);

        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult result = new StreamResult(sw);
        w3Epr.writeTo(result);
        String expected = "<wsdl:definitions";
        assertTrue("Embeded wsdl element is not generated", sw.toString().indexOf(expected) > -1);
        assertTrue("wsdlLocation attribute has the wrong value",
                   sw.toString().contains(":wsdlLocation=\"http://cxf.apache.org wsdlLoc\""));
    }

    @org.junit.Test
    public void testCreateW3CEprNoMetadata() throws Exception {
        ProviderImpl impl = new ProviderImpl();
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference(
                         "http://myaddress", null, null, null, null, null, null, null, null);

        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult result = new StreamResult(sw);
        w3Epr.writeTo(result);
        assertTrue("Address is expected", sw.toString().contains("Address"));
        assertFalse("empty Metadata element should be dropped", sw.toString().contains("Metadata"));
    }

    @org.junit.Test
    public void testCreateW3CEprNoMetadataEmptyCustomMetadata() throws Exception {
        ProviderImpl impl = new ProviderImpl();
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference(
            "http://myaddress", null, null, new ArrayList<>(), null, null);

        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult result = new StreamResult(sw);
        w3Epr.writeTo(result);
        assertTrue("Address is expected", sw.toString().contains("Address"));
        assertFalse("empty Metadata element should be dropped", sw.toString().contains("Metadata"));
    }

    @org.junit.Test
    public void testCreateW3CEprMetadataInterfaceNameOnly() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org", "IntfName");
        ProviderImpl impl = new ProviderImpl();
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference(
                         "http://myaddress", serviceName, null, null, null, null, null, null, null);

        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult result = new StreamResult(sw);
        w3Epr.writeTo(result);
        assertTrue("Address is expected", sw.toString().contains("Address"));
        assertTrue("Metadata element expected", sw.toString().contains("Metadata"));
        assertTrue("Interface element expected", sw.toString().contains("Interface"));
        assertFalse("ServiceName is unexpected", sw.toString().contains("ServiceName"));
    }

    @org.junit.Test
    public void testCreateW3CEprMetadataServiceNameOnly() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org", "ServiceName");
        ProviderImpl impl = new ProviderImpl();
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference(
                         "http://myaddress", null, serviceName, null, null, null, null, null, null);

        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult result = new StreamResult(sw);
        w3Epr.writeTo(result);
        assertTrue("Address is expected", sw.toString().contains("Address"));
        assertTrue("Metadata element expected", sw.toString().contains("Metadata"));
        assertFalse("Interface element unexpected", sw.toString().contains("Interface"));
        assertTrue("ServiceName is expected", sw.toString().contains("ServiceName"));
    }

    @org.junit.Test
    public void testCreateW3CEprMetadataMetadataOnly() throws Exception {
        ProviderImpl impl = new ProviderImpl();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element element = builder.newDocument().createElement("customMetadata");
        List<Element> metadata = new ArrayList<>();
        metadata.add(element);
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference(
                         "http://myaddress", null, null,
                         metadata, null, null);

        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult result = new StreamResult(sw);
        w3Epr.writeTo(result);
        assertTrue("Address is expected", sw.toString().contains("Address"));
        assertTrue("Metadata element expected", sw.toString().contains("Metadata"));
        assertTrue("Custom Metadata element expected", sw.toString().contains("customMetadata"));
        assertFalse("Interface element unexpected", sw.toString().contains("Interface"));
        assertFalse("ServiceName unexpected", sw.toString().contains("ServiceName"));
    }

    @org.junit.Test
    public void testCreateServiceDelegate() throws Exception {
        ProviderImpl provider = new ProviderImpl();
        URL url = getClass().getResource("/wsdl/default_partname_test.wsdl");
        QName qName = new QName("http://cxf.com/", "HelloService");
        Class cls = ServiceImpl.class;

        ServiceDelegate test = provider.createServiceDelegate(url, qName, cls);
        assertEquals(test.getServiceName(), qName);
        assertEquals(test.getWSDLDocumentLocation(), url);
    }

    @org.junit.Test
    public void testCreateServiceDelegateWebServiceFeatures() throws Exception {
        ProviderImpl provider = new ProviderImpl();
        URL url = getClass().getResource("/wsdl/default_partname_test.wsdl");
        QName qName = new QName("http://cxf.com/", "HelloService");
        Class cls = ServiceImpl.class;

        WebServiceFeature[] noFeatures = new WebServiceFeature[0];

        ServiceDelegate test = provider.createServiceDelegate(url, qName, cls, noFeatures);
        assertEquals(test.getServiceName(), qName);
        assertEquals(test.getWSDLDocumentLocation(), url);

        WebServiceFeature[] unknownFeature = new WebServiceFeature[1];
        WebServiceFeature webServiceFeature = new WebServiceFeature() {
            @Override
            public String getID() {
                return "12345";
            }
        };
        unknownFeature[0] = webServiceFeature;

        try {
            provider.createServiceDelegate(url, qName, cls, unknownFeature);
            fail();
        } catch (WebServiceException wse) {
            //expected
        }
    }

    @org.junit.Test
    public void testCreateEndpointImpl() throws Exception {
        ProviderImpl provider = new ProviderImpl();
        Bus bus = null;
        String bindingId = "bindingId";
        Object implementor = null;
        WebServiceFeature[] noFeatures = new WebServiceFeature[0];
        EndpointImpl endpoint = provider.createEndpointImpl(bus, bindingId, implementor, noFeatures);
        assertEquals(endpoint.getBus(), bus);
    }

    @org.junit.Test
    public void testCreateEndpoint() throws Exception {
        ProviderImpl provider = new ProviderImpl();
        String bindingId = "http://schemas.xmlsoap.org/wsdl/soap/http";
        Object implementor = new GreeterImpl();
        Endpoint endpoint = provider.createEndpoint(bindingId, implementor);
        assertEquals(endpoint.getImplementor(), implementor);

        try {
            provider.createEndpoint(bindingId, new InvalidImplementor());
            fail();
        } catch (WebServiceException wse) {
            //expected
        }

        WebServiceFeature[] noFeatures = new WebServiceFeature[0];
        endpoint = provider.createEndpoint(bindingId, implementor, noFeatures);
        assertEquals(endpoint.getImplementor(), implementor);

        try {
            provider.createEndpoint(bindingId, new InvalidImplementor(), noFeatures);
            fail();
        } catch (WebServiceException wse) {
            //expected
        }

        try {
            Invoker invoker = null;
            provider.createEndpoint(bindingId, InvalidImplementor.class, invoker, noFeatures);
            fail();
        } catch (WebServiceException wse) {
            //expected
        }

        try {
            endpoint = provider.createEndpoint(bindingId, GreeterImpl.class, validInvoker, noFeatures);
            assertEquals(endpoint.getBinding().getBindingID(), bindingId);
            provider.createEndpoint(bindingId, GreeterImpl.class, invalidInvoker, noFeatures);
            fail();
        } catch (WebServiceException wse) {
            //expected
        }
    }

    @org.junit.Test
    public void testCreateAndPublishEndpoint() throws Exception {
        ProviderImpl provider = new ProviderImpl();
        String url = "http://localhost:8080/test";
        GreeterImpl greeter = new GreeterImpl();

        Endpoint endpoint = provider.createAndPublishEndpoint(url, greeter);
        assertEquals(endpoint.getImplementor(), greeter);
    }

    @org.junit.Test
    public void testCreateAndPublishEndpointWithFeatures() throws Exception {
        ProviderImpl provider = new ProviderImpl();
        String url = "http://localhost:8080/test";
        GreeterImpl greeter = new GreeterImpl();
        WebServiceFeature[] noFeatures = new WebServiceFeature[0];

        Endpoint endpoint = provider.createAndPublishEndpoint(url, greeter, noFeatures);
        assertEquals(endpoint.getImplementor(), greeter);
    }

    @org.junit.Test
    public void testConvertToInternal() throws Exception {
        EndpointReference external = new EndpointReference() {
            @Override
            public void writeTo(Result result) {

            }
        };

        EndpointReferenceType endpointReferenceType = ProviderImpl.convertToInternal(external);
        assertEquals(null, endpointReferenceType);

        QName serviceName = new QName("http://cxf.apache.org", "ServiceName");
        QName portName = new QName("http://cxf.apache.org", "PortName");
        ProviderImpl impl = new ProviderImpl();
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference("http://myaddress", serviceName,
                portName, null, "wsdlLoc",
                null);
        endpointReferenceType = ProviderImpl.convertToInternal(w3Epr);
        assertEquals("http://myaddress", endpointReferenceType.getAddress().getValue());
    }


    @After
    public void tearDown() {
        BusFactory.setDefaultBus(null);
    }

    private final class InvalidImplementor implements jakarta.xml.ws.Provider {
        @Override
        public Object invoke(Object o) {
            return null;
        }

    }

}
