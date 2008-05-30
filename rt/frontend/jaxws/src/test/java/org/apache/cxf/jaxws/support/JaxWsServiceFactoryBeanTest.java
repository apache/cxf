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

package org.apache.cxf.jaxws.support;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jws.WebService;
import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.mtom_xop.TestMtomImpl;
import org.apache.cxf.no_body_parts.NoBodyPartsImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.junit.Test;

public class JaxWsServiceFactoryBeanTest extends AbstractJaxWsTest {
    
    @Test
    public void testDocLiteralPartWithType() throws Exception {
        ReflectionServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setServiceClass(NoBodyPartsImpl.class);
        Service service = serviceFactory.create();
        ServiceInfo serviceInfo = 
            service.getServiceInfos().get(0);
        QName qname = new QName("urn:org:apache:cxf:no_body_parts/wsdl",
                                "operation1");
        MessageInfo mi = serviceInfo.getMessage(qname);
        qname = new QName("urn:org:apache:cxf:no_body_parts/wsdl",
            "mimeAttachment");
        MessagePartInfo mpi = mi.getMessagePart(qname);
        QName elementQName = mpi.getElementQName();
        XmlSchemaElement element = 
            serviceInfo.getXmlSchemaCollection().getElementByQName(elementQName);
        assertNotNull(element);
    }


    @Test
    public void testEndpoint() throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();

        URL resource = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(resource);
        bean.setWsdlURL(resource.toString());
        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(GreeterImpl.class);

        BeanInvoker invoker = new BeanInvoker(new GreeterImpl());
        bean.setInvoker(invoker);
        
        Service service = bean.create();

        String ns = "http://apache.org/hello_world_soap_http";
        assertEquals("SOAPService", service.getName().getLocalPart());
        assertEquals(ns, service.getName().getNamespaceURI());
        
        InterfaceInfo intf = service.getServiceInfos().get(0).getInterface();
        
        OperationInfo op = intf.getOperation(new QName(ns, "sayHi"));
        
        Class wrapper = (Class) op.getInput().getMessageParts().get(0).getTypeClass();
        assertNotNull(wrapper);
        
        wrapper = (Class) op.getOutput().getMessageParts().get(0).getTypeClass();
        assertNotNull(wrapper);
    
        assertEquals(invoker, service.getInvoker());
        
        op = intf.getOperation(new QName(ns, "testDocLitFault"));
        Collection<FaultInfo> faults = op.getFaults();
        assertEquals(2, faults.size());
        
        FaultInfo f = op.getFault(new QName(ns, "BadRecordLitFault"));
        assertNotNull(f);
        Class c = f.getProperty(Class.class.getName(), Class.class);
        assertNotNull(c);
        
        assertEquals(1, f.getMessageParts().size());
        MessagePartInfo mpi = f.getMessagePartByIndex(0);
        assertNotNull(mpi.getTypeClass());
    }
    
    @Test
    public void testHolder() throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();

        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(TestMtomImpl.class);

        Service service = bean.create();
        InterfaceInfo intf = service.getServiceInfos().get(0).getInterface();
        
        OperationInfo op = intf.getOperation(
            new QName("http://cxf.apache.org/mime", "testXop"));
        assertNotNull(op);
        
        
        Iterator<MessagePartInfo> itr = op.getInput().getMessageParts().iterator();
        assertTrue(itr.hasNext());
        MessagePartInfo part = itr.next();
        assertEquals("testXop", part.getElementQName().getLocalPart());
        
        op = op.getUnwrappedOperation();
        assertNotNull(op);
        
        // test setup of input parts
        itr = op.getInput().getMessageParts().iterator();
        assertTrue(itr.hasNext());
        part = itr.next();
        assertEquals("name", part.getName().getLocalPart());
        assertEquals(String.class, part.getTypeClass());
        
        /*
         * revisit, try to use other wsdl operation rewrite test in future 
        assertTrue(itr.hasNext());
        part = itr.next();
        assertEquals(Boolean.TRUE, part.getProperty(JaxWsServiceFactoryBean.MODE_INOUT));
        assertEquals(byte[].class, part.getTypeClass());
        
        assertFalse(itr.hasNext());
        
        // test output setup
        itr = op.getOutput().getMessageParts().iterator();

        assertTrue(itr.hasNext());
        part = itr.next();
        assertEquals(Boolean.TRUE, part.getProperty(JaxWsServiceFactoryBean.MODE_INOUT));
        */
    }
    
    @Test
    public void testWrappedDocLit() throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(org.apache.hello_world_doc_lit.Greeter.class);
        Service service = bean.create();
        
        ServiceInfo si = service.getServiceInfos().get(0);
        InterfaceInfo intf = si.getInterface();
        
        assertEquals(4, intf.getOperations().size());
        
        String ns = si.getName().getNamespaceURI();
        assertEquals("http://apache.org/hello_world_doc_lit", ns);
        OperationInfo greetMeOp = intf.getOperation(new QName(ns, "greetMe"));
        assertNotNull(greetMeOp);
        
        assertEquals("greetMe", greetMeOp.getInput().getName().getLocalPart());
        assertEquals("http://apache.org/hello_world_doc_lit", greetMeOp.getInput().getName()
            .getNamespaceURI());
       
        List<MessagePartInfo> messageParts = greetMeOp.getInput().getMessageParts();
        assertEquals(1, messageParts.size());
        
        MessagePartInfo inMessagePart = messageParts.get(0);
        assertEquals("http://apache.org/hello_world_doc_lit", inMessagePart.getName().getNamespaceURI());
        assertEquals("http://apache.org/hello_world_doc_lit/types", inMessagePart.getElementQName()
            .getNamespaceURI());
        
        
        // test output
        messageParts = greetMeOp.getOutput().getMessageParts();
        assertEquals(1, messageParts.size());
        assertEquals("greetMeResponse", greetMeOp.getOutput().getName().getLocalPart());
        
        MessagePartInfo outMessagePart = messageParts.get(0);
        //assertEquals("result", outMessagePart.getName().getLocalPart());
        assertEquals("http://apache.org/hello_world_doc_lit", outMessagePart.getName().getNamespaceURI());
        assertEquals("http://apache.org/hello_world_doc_lit/types", outMessagePart.getElementQName()
            .getNamespaceURI());

        
        OperationInfo greetMeOneWayOp = si.getInterface().getOperation(new QName(ns, "greetMeOneWay"));
        assertEquals(1, greetMeOneWayOp.getInput().getMessageParts().size());
        assertNull(greetMeOneWayOp.getOutput());
        
        Collection<SchemaInfo> schemas = si.getSchemas();
        assertEquals(1, schemas.size());
    }
    
    @Test
    public void testBareBug() throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(org.apache.cxf.test.TestInterfacePort.class);
        Service service = bean.create();
        ServiceInfo si = service.getServiceInfos().get(0);
        ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus, si);
        Definition def = builder.build();
        
        Document wsdl = WSDLFactory.newInstance().newWSDLWriter().getDocument(def);
        NodeList nodeList = assertValid("/wsdl:definitions/wsdl:types/xsd:schema" 
                                        + "[@targetNamespace='http://cxf.apache.org/" 
                                        + "org.apache.cxf.test.TestInterface/xsd']" 
                                        + "/xsd:element[@name='getMessage']", wsdl);
        assertEquals(1, nodeList.getLength());
        

        assertValid("/wsdl:definitions/wsdl:message[@name='setMessage']" 
                    + "/wsdl:part[@name = 'parameters'][@element='ns1:setMessage']" , wsdl);

        assertValid("/wsdl:definitions/wsdl:message[@name='echoCharResponse']" 
                    + "/wsdl:part[@name = 'y'][@element='ns1:charEl_y']" , wsdl);
        
        assertValid("/wsdl:definitions/wsdl:message[@name='echoCharResponse']" 
                    + "/wsdl:part[@name = 'return'][@element='ns1:charEl_return']" , wsdl);

        assertValid("/wsdl:definitions/wsdl:message[@name='echoCharResponse']" 
                    + "/wsdl:part[@name = 'z'][@element='ns1:charEl_z']" , wsdl);
        
        assertValid("/wsdl:definitions/wsdl:message[@name='echoChar']" 
                    + "/wsdl:part[@name = 'x'][@element='ns1:charEl_x']" , wsdl);
        
        assertValid("/wsdl:definitions/wsdl:message[@name='echoChar']" 
                    + "/wsdl:part[@name = 'y'][@element='ns1:charEl_y']" , wsdl);
    }
    
    @Test
    public void testMtomFeature() throws Exception {
        JaxWsServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        bean.setBus(getBus());
        bean.setServiceClass(GreeterImpl.class);
        bean.setWsdlURL(getClass().getResource("/wsdl/hello_world.wsdl"));
        bean.setWsFeatures(Arrays.asList(new WebServiceFeature[]{new MTOMFeature()}));
        Service service = bean.create();
        Endpoint endpoint = service.getEndpoints().values().iterator().next();
        assertTrue(endpoint instanceof JaxWsEndpointImpl);
        Binding binding = ((JaxWsEndpointImpl)endpoint).getJaxwsBinding();
        assertTrue(binding instanceof SOAPBinding);
        assertTrue(((SOAPBinding)binding).isMTOMEnabled());
    }   
    
    @Test
    public void testWebSeviceException() throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(WebServiceExceptionTestImpl.class);
        Service service = bean.create();
        ServiceInfo si = service.getServiceInfos().get(0);
        ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus, si);
        Definition def = builder.build();
        
        Document wsdl = WSDLFactory.newInstance().newWSDLWriter().getDocument(def);
        
        assertInvalid("/wsdl:definitions/wsdl:message[@name='WebServiceException']", wsdl);
    }
    
    @WebService
    public static class WebServiceExceptionTestImpl {
        public int echoInt(int i) throws WebServiceException {
            return i;
        }
        
    }
}
