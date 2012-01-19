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

import java.util.Collection;

import javax.jws.WebService;
import javax.wsdl.Definition;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebFault;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.WSDLDocumentation;
import org.apache.cxf.annotations.WSDLDocumentationCollection;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.service.Hello2;
import org.apache.cxf.jaxws.service.Hello3;
import org.apache.cxf.jaxws.service.HelloExcludeImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.junit.Test;

public class CodeFirstWSDLTest extends AbstractJaxWsTest {
    String address = "local://localhost:9000/Hello";
    
    private Definition createService(Class clazz) throws Exception {
        
        JaxWsImplementorInfo info = new JaxWsImplementorInfo(clazz);
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean(info);

        Bus bus = getBus();
        bean.setBus(bus);
        
        Service service = bean.create();

        InterfaceInfo i = service.getServiceInfos().get(0).getInterface();
        assertEquals(4, i.getOperations().size());

        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setBus(bus);
        svrFactory.setServiceFactory(bean);
        svrFactory.setServiceBean(clazz.newInstance());
        svrFactory.setAddress(address);
        svrFactory.create();
        
        Collection<BindingInfo> bindings = service.getServiceInfos().get(0).getBindings();
        assertEquals(1, bindings.size());
        
        ServiceWSDLBuilder wsdlBuilder = 
            new ServiceWSDLBuilder(bus, service.getServiceInfos().get(0));
        return wsdlBuilder.build();
    }
    
    @Test
    public void testWSDL1() throws Exception {
        Definition d = createService(Hello2.class);

        QName serviceName = new QName("http://service.jaxws.cxf.apache.org/", "Hello2Service");

        javax.wsdl.Service service = d.getService(serviceName);

        assertNotNull(service);

        QName portName = new QName("http://service.jaxws.cxf.apache.org/", "Hello2Port");

        javax.wsdl.Port port = service.getPort(portName.getLocalPart());

        assertNotNull(port);

        QName portTypeName = new QName("http://service.jaxws.cxf.apache.org/", "HelloInterface");

        javax.wsdl.PortType portType = d.getPortType(portTypeName);

        assertNotNull(portType);
        assertEquals(4, portType.getOperations().size());
    }

    @Test
    public void testWSDL2() throws Exception {
        Definition d = createService(Hello3.class);

        QName serviceName = new QName("http://mynamespace.com/", "MyService");

        javax.wsdl.Service service = d.getService(serviceName);

        assertNotNull(service);

        QName portName = new QName("http://mynamespace.com/", "MyPort");

        javax.wsdl.Port port = service.getPort(portName.getLocalPart());

        assertNotNull(port);

        QName portTypeName = new QName("http://service.jaxws.cxf.apache.org/", "HelloInterface");

        javax.wsdl.PortType portType = d.getPortType(portTypeName);

        assertNotNull(portType);
        assertEquals(4, portType.getOperations().size());
    }
    @Test
    public void testExcludeOnInterface() throws Exception {
        try {
            JaxWsImplementorInfo info = new JaxWsImplementorInfo(HelloExcludeImpl.class);
            ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean(info);

            Bus bus = getBus();
            bean.setBus(bus);
            
            bean.create();
            
            fail("WebMethod(exclude=true) is not allowed");
        } catch (JaxWsConfigurationException e) {
            assertTrue(e.getMessage().contains("WebMethod"));
        }
    }
    
    @Test
    public void testDocumentationOnSEI() throws Exception {
        //CXF-3093
        EndpointImpl ep = (EndpointImpl)Endpoint.publish("local://foo", new CXF3093Impl());
        ServiceWSDLBuilder wsdlBuilder = 
            new ServiceWSDLBuilder(bus, ep.getService().getServiceInfos().get(0));
        Definition def = wsdlBuilder.build();
        Document d = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter().getDocument(def);
        //org.apache.cxf.helpers.XMLUtils.printDOM(d);
        assertXPathEquals("//wsdl:definitions/wsdl:documentation", "My top level documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:portType/wsdl:documentation", "My portType documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:binding/wsdl:documentation", "My binding doc",
                          d.getDocumentElement());
        
        
        JaxwsServiceBuilder builder = new JaxwsServiceBuilder();
        builder.setServiceClass(CXF3093Impl.class);
        ServiceInfo serviceInfo = builder.createService();
        wsdlBuilder = new ServiceWSDLBuilder(bus, serviceInfo);
        
        def = wsdlBuilder.build();
        d = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter().getDocument(def);
        //org.apache.cxf.helpers.XMLUtils.printDOM(d);
        assertXPathEquals("//wsdl:definitions/wsdl:documentation", "My top level documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:portType/wsdl:documentation", "My portType documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:binding/wsdl:documentation", "My binding doc",
                          d.getDocumentElement());
    }

    @WebService(targetNamespace = "http://www.example.org/contract/DoubleIt")
    @WSDLDocumentationCollection({
        @WSDLDocumentation("My portType documentation"), 
        @WSDLDocumentation(value = "My top level documentation", 
                           placement = WSDLDocumentation.Placement.TOP), 
        @WSDLDocumentation(value = "My binding doc", placement = WSDLDocumentation.Placement.BINDING) 
    })
    public interface CXF3093PortType { 
        int doubleIt(int numberToDouble); 
    }
    
    @WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
                serviceName = "DoubleItService",
                portName = "DoubleItPort")
    public static class CXF3093Impl implements CXF3093PortType {
        public int doubleIt(int numberToDouble) {
            return numberToDouble * 2;
        }
        
    }

    
    @Test
    public void testOnlyRootElementOnFaultBean() throws Exception {
        //CXF-4016
        EndpointImpl ep = (EndpointImpl)Endpoint.publish("local://foo4016", new CXF4016Impl());
        ServiceWSDLBuilder wsdlBuilder = 
            new ServiceWSDLBuilder(bus, ep.getService().getServiceInfos().get(0));
        Definition def = wsdlBuilder.build();
        Document d = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter().getDocument(def);
        this.addNamespace("http://www.example.org/contract/DoubleIt", "tns");
        //org.apache.cxf.helpers.XMLUtils.printDOM(d);
        assertValid("//xsd:element[@ref='tns:CustomMessageBean']", d);
    }


    @WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
                serviceName = "DoubleItService",
                portName = "DoubleItPort")
    public static class CXF4016Impl {
        public int doubleIt(int numberToDouble) throws CustomException {
            return numberToDouble * 2;
        }
        
    } 
    @WebFault(name = "CustomException", targetNamespace = "http://www.example.org/contract/DoubleIt")
    public static class CustomException extends Exception {
        private static final long serialVersionUID = 1L;
        private CustomMessageBean faultInfo; 

        public CustomException(String msg) {
            super(msg);
        }
        public CustomException(String msg, CustomMessageBean b) {
            super(msg);
            faultInfo = b;
        }
        public CustomMessageBean getCustomMessage() {
            return faultInfo;
        }
        public void setCustomMessage(CustomMessageBean b) {
            faultInfo = b;
        }
    }
    
    @XmlRootElement(name = "CustomMessageBean")
    @XmlType(name = "", propOrder = { "myId", "msg" })
    public static class CustomMessageBean {
        String msg;
        int myId;
        
        public CustomMessageBean() {
        }

        public int getMyId() {
            return myId;
        }

        public void setMyId(int myId) {
            this.myId = myId;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
    
    
    @Test
    public void testDocumentationOnImpl() throws Exception {
        //CXF-3092
        EndpointImpl ep = (EndpointImpl)Endpoint.publish("local://foo", new CXF3092Impl());
        ServiceWSDLBuilder wsdlBuilder = 
            new ServiceWSDLBuilder(bus, ep.getService().getServiceInfos().get(0));
        Definition def = wsdlBuilder.build();
        Document d = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter().getDocument(def);
        //org.apache.cxf.helpers.XMLUtils.printDOM(d);
        assertXPathEquals("//wsdl:definitions/wsdl:documentation", "My top level documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:service/wsdl:documentation", "My Service documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:binding/wsdl:documentation", "My binding doc",
                          d.getDocumentElement());
        
        JaxwsServiceBuilder builder = new JaxwsServiceBuilder();
        builder.setServiceClass(CXF3092Impl.class);
        ServiceInfo serviceInfo = builder.createService();
        wsdlBuilder = new ServiceWSDLBuilder(bus, serviceInfo);
        
        def = wsdlBuilder.build();
        d = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter().getDocument(def);
        //org.apache.cxf.helpers.XMLUtils.printDOM(d);
        assertXPathEquals("//wsdl:definitions/wsdl:documentation", "My top level documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:service/wsdl:documentation", "My Service documentation",
                          d.getDocumentElement());
        assertXPathEquals("//wsdl:definitions/wsdl:binding/wsdl:documentation", "My binding doc",
                          d.getDocumentElement());
    }

    @WebService(targetNamespace = "http://www.example.org/contract/DoubleIt")
    public interface CXF3092PortType { 
        int doubleIt(int numberToDouble); 
    }
    
    @WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
                serviceName = "DoubleItService",
                portName = "DoubleItPort")
    @WSDLDocumentationCollection({
        @WSDLDocumentation("My Service documentation"), 
        @WSDLDocumentation(value = "My top level documentation", 
                           placement = WSDLDocumentation.Placement.TOP), 
        @WSDLDocumentation(value = "My binding doc", placement = WSDLDocumentation.Placement.BINDING) 
    })
    public static class CXF3092Impl implements CXF3092PortType {
        public int doubleIt(int numberToDouble) {
            return numberToDouble * 2;
        }
        
    }

}
