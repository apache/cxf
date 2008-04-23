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
package org.apache.cxf.jaxws.provider;

import java.net.URL;

import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.xml.XMLBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.hello_world_soap_http.HWSoapMessageProvider;
import org.junit.Test;

public class ProviderServiceFactoryTest extends AbstractJaxWsTest {
    @Test
    public void testFromWSDL() throws Exception {
        URL resource = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(resource);

        JaxWsImplementorInfo implInfo = new JaxWsImplementorInfo(HWSoapMessageProvider.class);
        JaxWsServiceFactoryBean bean = new JaxWsServiceFactoryBean(implInfo);
        bean.setWsdlURL(resource.toString());

        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(HWSoapMessageProvider.class);

        Service service = bean.create();

        assertEquals("SOAPService", service.getName().getLocalPart());
        assertEquals("http://apache.org/hello_world_soap_http", service.getName().getNamespaceURI());

        InterfaceInfo intf = service.getServiceInfos().get(0).getInterface();
        assertNotNull(intf);

        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setBus(bus);
        svrFactory.setServiceFactory(bean);
        svrFactory.setStart(false);

        ServerImpl server = (ServerImpl)svrFactory.create();

        Endpoint endpoint = server.getEndpoint();
        Binding binding = endpoint.getBinding();
        assertTrue(binding instanceof SoapBinding);
        assertEquals(Boolean.TRUE, endpoint.getEndpointInfo().getBinding()
            .getProperty(AbstractBindingFactory.DATABINDING_DISABLED));
    }

    @Test
    public void testXMLBindingFromCode() throws Exception {
        JaxWsServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        bean.setServiceClass(DOMSourcePayloadProvider.class);
        bean.setBus(getBus());
        bean.setInvoker(new JAXWSMethodInvoker(new DOMSourcePayloadProvider()));
        
        Service service = bean.create();

        assertEquals("DOMSourcePayloadProviderService", service.getName().getLocalPart());

        InterfaceInfo intf = service.getServiceInfos().get(0).getInterface();
        assertNotNull(intf);

        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setBus(getBus());
        svrFactory.setServiceFactory(bean);
        String address = "http://localhost:9000/test";
        svrFactory.setAddress(address);
        svrFactory.setTransportId(LocalTransportFactory.TRANSPORT_ID);

        ServerImpl server = (ServerImpl)svrFactory.create();

        assertEquals(1, service.getServiceInfos().get(0).getEndpoints().size());

        Endpoint endpoint = server.getEndpoint();
        Binding binding = endpoint.getBinding();
        assertTrue(binding instanceof XMLBinding);
        
        Node res = invoke(address, LocalTransportFactory.TRANSPORT_ID, 
                          "/org/apache/cxf/jaxws/provider/sayHi.xml");
        
        addNamespace("j", "http://service.jaxws.cxf.apache.org/");
        assertValid("/j:sayHi", res);
    }

    @Test
    public void testSOAPBindingFromCode() throws Exception {
        JaxWsServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        bean.setServiceClass(SOAPSourcePayloadProvider.class);
        bean.setBus(getBus());
        bean.setInvoker(new JAXWSMethodInvoker(new SOAPSourcePayloadProvider()));
        
        Service service = bean.create();

        assertEquals("SOAPSourcePayloadProviderService", service.getName().getLocalPart());

        InterfaceInfo intf = service.getServiceInfos().get(0).getInterface();
        assertNotNull(intf);
        assertEquals(1, intf.getOperations().size());

        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setBus(getBus());
        svrFactory.setServiceFactory(bean);
        String address = "http://localhost:9000/test";
        svrFactory.setAddress(address);

        ServerImpl server = (ServerImpl)svrFactory.create();

        // See if our endpoint was created correctly
        assertEquals(1, service.getServiceInfos().get(0).getEndpoints().size());

        Endpoint endpoint = server.getEndpoint();
        Binding binding = endpoint.getBinding();
        assertTrue(binding instanceof SoapBinding);

        SoapBindingInfo sb = (SoapBindingInfo)endpoint.getEndpointInfo().getBinding();
        assertEquals("document", sb.getStyle());
        assertEquals(false, bean.isWrapped());
        assertEquals(Boolean.TRUE, sb.getProperty(AbstractBindingFactory.DATABINDING_DISABLED));

        assertEquals(1, sb.getOperations().size());
        Node res = invoke(address, LocalTransportFactory.TRANSPORT_ID, "/org/apache/cxf/jaxws/sayHi.xml");
        
        addNamespace("j", "http://service.jaxws.cxf.apache.org/");
        assertValid("/s:Envelope/s:Body/j:sayHi", res);
    }
    
    @Test
    public void testSAAJProviderCodeFirst() throws Exception {
        JaxWsServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        bean.setServiceClass(SAAJProvider.class);
        bean.setBus(getBus());
        bean.setInvoker(new JAXWSMethodInvoker(new SAAJProvider()));
        
        Service service = bean.create();

        assertEquals("SAAJProviderService", service.getName().getLocalPart());

        InterfaceInfo intf = service.getServiceInfos().get(0).getInterface();
        assertNotNull(intf);
        assertEquals(1, intf.getOperations().size());

        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setBus(getBus());
        svrFactory.setServiceFactory(bean);
        String address = "http://localhost:9000/test";
        svrFactory.setAddress(address);

        ServerImpl server = (ServerImpl)svrFactory.create();

        Endpoint endpoint = server.getEndpoint();
        Binding binding = endpoint.getBinding();
        assertTrue(binding instanceof SoapBinding);

        SoapBindingInfo sb = (SoapBindingInfo)endpoint.getEndpointInfo().getBinding();
        assertEquals("document", sb.getStyle());
        assertEquals(false, bean.isWrapped());
        assertEquals(Boolean.TRUE, sb.getProperty(AbstractBindingFactory.DATABINDING_DISABLED));

        assertEquals(1, sb.getOperations().size());
        Node res = invoke(address, LocalTransportFactory.TRANSPORT_ID, "/org/apache/cxf/jaxws/sayHi.xml");
        
        addNamespace("j", "http://service.jaxws.cxf.apache.org/");
        assertValid("/s:Envelope/s:Body/j:sayHi", res);
    }
    
    @Test
    public void testStreamSourceProviderCodeFirst() throws Exception {
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(StreamSourcePayloadProvider.class);
        svrFactory.setBus(getBus());
        svrFactory.setServiceBean(new StreamSourcePayloadProvider());
        String address = "http://localhost:9000/test";
        svrFactory.setAddress(address);
        svrFactory.setTransportId(LocalTransportFactory.TRANSPORT_ID);

        svrFactory.create();

        Node res = invoke(address, LocalTransportFactory.TRANSPORT_ID, "/org/apache/cxf/jaxws/sayHi.xml");
        
        addNamespace("j", "http://service.jaxws.cxf.apache.org/");
        assertValid("/s:Envelope/s:Body/j:sayHi", res);
    }
    

    @Test
    public void testSourceMessageProviderCodeFirst() throws Exception {
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(SourceMessageProvider.class);
        svrFactory.setBus(getBus());
        svrFactory.setServiceBean(new SourceMessageProvider());
        String address = "http://localhost:9000/test";
        svrFactory.setAddress(address);

        svrFactory.create();

        Node res = invoke(address, LocalTransportFactory.TRANSPORT_ID, "/org/apache/cxf/jaxws/sayHi.xml");
        
        addNamespace("j", "http://service.jaxws.cxf.apache.org/");
        assertValid("/s:Envelope/s:Body/j:sayHi", res);
    }
}
