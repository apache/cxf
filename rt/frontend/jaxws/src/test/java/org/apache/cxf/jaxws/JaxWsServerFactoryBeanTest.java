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
package org.apache.cxf.jaxws;

import java.util.HashMap;
import java.util.Map;
import javax.wsdl.Definition;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.calculator.CalculatorImpl;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.javaee.DescriptionType;
import org.apache.cxf.jaxws.javaee.DisplayNameType;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.hello_world_doc_lit.GreeterImplDoc;
import org.junit.Test;

public class JaxWsServerFactoryBeanTest extends AbstractJaxWsTest {
    
    @Test
    public void testBean() {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setAddress("http://localhost:9000/test");
        sf.setServiceClass(Hello.class);
        sf.setStart(false);
        
        Server server = sf.create();
        assertNotNull(server);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testJaxbExtraClass() {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setAddress("http://localhost:9000/test");
        sf.setServiceClass(Hello.class);
        sf.setStart(false);
        Map props = sf.getProperties();
        if (props == null) {
            props = new HashMap<String, Object>();
        }
        props.put("jaxb.additionalContextClasses", 
                  new Class[] {DescriptionType.class, DisplayNameType.class});
        sf.setProperties(props);
        Server server = sf.create();
        assertNotNull(server);
        Class[] extraClass = ((JAXBDataBinding)sf.getServiceFactory().getDataBinding()).getExtraClass();
        assertEquals(extraClass.length, 2);
        assertEquals(extraClass[0], DescriptionType.class);
        assertEquals(extraClass[1], DisplayNameType.class);
    }
    
    @Test
    public void testBareGreeter() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setServiceClass(GreeterImplDoc.class);
        sf.setStart(false);
        
        Server server = sf.create();
        assertNotNull(server);
    }


    @Test
    public void testSimpleServiceClass() throws Exception {
        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setServiceClass(Hello.class);
        String address = "http://localhost:9001/jaxwstest";
        factory.setAddress(address);
        Server server = factory.create();
        Endpoint endpoint = server.getEndpoint();
        ServiceInfo service = endpoint.getEndpointInfo().getService();
        assertNotNull(service);

        Bus bus = factory.getBus();
        Definition def = new ServiceWSDLBuilder(bus, service).build();

        WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter();
        def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
        Document doc = wsdlWriter.getDocument(def);

        Map<String, String> ns = new HashMap<String, String>();
        ns.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        ns.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils xpather = new XPathUtils(ns);
        xpather.isExist("/wsdl:definitions/wsdl:binding/soap:binding",
                        doc,
                        XPathConstants.NODE);
        xpather.isExist("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='add']/soap:operation",
                        doc,
                        XPathConstants.NODE);
        xpather.isExist("/wsdl:definitions/wsdl:service/wsdl:port[@name='add']/soap:address[@location='"
                        + address + "']",
                        doc,
                        XPathConstants.NODE);
    }

    @Test
    public void testJaxwsServiceClass() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(CalculatorPortType.class);
        factory.setServiceBean(new CalculatorImpl());
        String address = "http://localhost:9001/jaxwstest";
        factory.setAddress(address);
        Server server = factory.create();
        Endpoint endpoint = server.getEndpoint();
        ServiceInfo service = endpoint.getEndpointInfo().getService();
        assertNotNull(service);

        Bus bus = factory.getBus();
        Definition def = new ServiceWSDLBuilder(bus, service).build();

        WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter();
        def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
        Document doc = wsdlWriter.getDocument(def);

        Map<String, String> ns = new HashMap<String, String>();
        ns.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        ns.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils xpather = new XPathUtils(ns);
        xpather.isExist("/wsdl:definitions/wsdl:binding/soap:binding",
                        doc,
                        XPathConstants.NODE);
        xpather.isExist("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='add']/soap:operation",
                        doc,
                        XPathConstants.NODE);
        xpather.isExist("/wsdl:definitions/wsdl:service/wsdl:port[@name='add']/soap:address[@location='"
                        + address + "']",
                        doc,
                        XPathConstants.NODE);
    }
    
    @Test
    public void testPostConstructCalled() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(Hello.class);
        Hello bean = new Hello();
        factory.setServiceBean(bean);
        String address = "http://localhost:9001/jaxwstest";
        factory.setAddress(address);
        factory.create();
        assertTrue("PostConstruct is not called", bean.isPostConstructCalled());
    }
    
    @Test
    public void testPostConstructBlocked() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(Hello.class);
        Hello bean = new Hello();
        factory.setServiceBean(bean);
        String address = "http://localhost:9001/jaxwstest";
        factory.setAddress(address);
        factory.setBlockPostConstruct(true);
        factory.create();
        assertFalse("PostConstruct is called", bean.isPostConstructCalled());
    }
}
