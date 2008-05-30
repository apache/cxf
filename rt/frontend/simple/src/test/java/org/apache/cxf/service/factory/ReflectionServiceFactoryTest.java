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
package org.apache.cxf.service.factory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.no_body_parts.NoBodyPartsImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.junit.Test;

public class ReflectionServiceFactoryTest extends AbstractSimpleFrontendTest {
    private ReflectionServiceFactoryBean serviceFactory;

    @Test
    public void testUnwrappedBuild() throws Exception {
        Service service = createService(false);
        
        ServiceInfo si = service.getServiceInfos().get(0);
        InterfaceInfo intf = si.getInterface();
        
        assertEquals(4, intf.getOperations().size());
        
        String ns = si.getName().getNamespaceURI();
        OperationInfo sayHelloOp = intf.getOperation(new QName(ns, "sayHello"));
        assertNotNull(sayHelloOp);
        
        assertEquals("sayHello", sayHelloOp.getInput().getName().getLocalPart());
        
        List<MessagePartInfo> messageParts = sayHelloOp.getInput().getMessageParts();
        assertEquals(0, messageParts.size());
        
        // test output
        messageParts = sayHelloOp.getOutput().getMessageParts();
        assertEquals(1, messageParts.size());
        assertEquals("sayHelloResponse", sayHelloOp.getOutput().getName().getLocalPart());
        
        MessagePartInfo mpi = messageParts.get(0);
        assertEquals("return", mpi.getName().getLocalPart());
        assertEquals(String.class, mpi.getTypeClass());

        
        OperationInfo op = si.getInterface().getOperation(new QName(ns, "echoWithExchange"));
        assertEquals(1, op.getInput().getMessageParts().size());
    }
    
    @Test
    public void testWrappedBuild() throws Exception {
        Service service = createService(true);
        
        ServiceInfo si = service.getServiceInfos().get(0);
        InterfaceInfo intf = si.getInterface();
        
        assertEquals(4, intf.getOperations().size());
        
        String ns = si.getName().getNamespaceURI();
        OperationInfo sayHelloOp = intf.getOperation(new QName(ns, "sayHello"));
        assertNotNull(sayHelloOp);
        
        assertEquals("sayHello", sayHelloOp.getInput().getName().getLocalPart());
        
        List<MessagePartInfo> messageParts = sayHelloOp.getInput().getMessageParts();
        assertEquals(1, messageParts.size());
        assertNotNull(messageParts.get(0).getXmlSchema());
        
        // test unwrapping
        assertTrue(sayHelloOp.isUnwrappedCapable());
        
        OperationInfo unwrappedOp = sayHelloOp.getUnwrappedOperation();
        assertEquals("sayHello", unwrappedOp.getInput().getName().getLocalPart());
        
        messageParts = unwrappedOp.getInput().getMessageParts();
        assertEquals(0, messageParts.size());
        
        // test output
        messageParts = sayHelloOp.getOutput().getMessageParts();
        assertEquals(1, messageParts.size());
        assertEquals("sayHelloResponse", sayHelloOp.getOutput().getName().getLocalPart());
        
        messageParts = unwrappedOp.getOutput().getMessageParts();
        assertEquals("sayHelloResponse", unwrappedOp.getOutput().getName().getLocalPart());
        assertEquals(1, messageParts.size());
        MessagePartInfo mpi = messageParts.get(0);
        assertEquals("return", mpi.getName().getLocalPart());
        assertEquals(String.class, mpi.getTypeClass());
    }

    private Service createService(boolean wrapped) throws JAXBException {
        serviceFactory = new ReflectionServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setServiceClass(HelloService.class);
        serviceFactory.setWrapped(wrapped);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("test", "test");
        serviceFactory.setProperties(props);
        
        return serviceFactory.create();        
    }
    
    @Test
    public void testServerFactoryBean() throws Exception {
        Service service = createService(true);
        assertEquals("test", service.get("test"));
        
        ServerFactoryBean svrBean = new ServerFactoryBean();
        svrBean.setAddress("http://localhost/Hello");
        svrBean.setServiceFactory(serviceFactory);
        svrBean.setServiceBean(new HelloServiceImpl());
        svrBean.setBus(getBus());
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("test", "test");
        serviceFactory.setProperties(props);
        svrBean.setProperties(props);
        
        Server server = svrBean.create();
        assertNotNull(server);
        Map<QName, Endpoint> eps = service.getEndpoints();
        assertEquals(1, eps.size());
        
        Endpoint ep = eps.values().iterator().next();
        EndpointInfo endpointInfo = ep.getEndpointInfo();
        
        assertEquals("test", ep.get("test"));
        
        SOAPAddress soapAddress = endpointInfo.getExtensor(SOAPAddress.class);
        assertNotNull(soapAddress);
        
        BindingInfo b = endpointInfo.getService().getBindings().iterator().next();
        
        assertTrue(b instanceof SoapBindingInfo);
        
        SoapBindingInfo sb = (SoapBindingInfo) b;
        assertEquals("HelloServiceSoapBinding", b.getName().getLocalPart());
        assertEquals("document", sb.getStyle());
        
        assertEquals(4, b.getOperations().size());
        
        BindingOperationInfo bop = b.getOperations().iterator().next();
        SoapOperationInfo sop = bop.getExtensor(SoapOperationInfo.class);
        assertNotNull(sop);
        assertEquals("", sop.getAction());
        assertEquals("document", sop.getStyle());
    }
    
    @Test(expected = ServiceConstructionException.class)
    public void testDocLiteralPartWithType() throws Exception {
        serviceFactory = new ReflectionServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setServiceClass(NoBodyPartsImpl.class);
        serviceFactory.getServiceConfigurations().add(0,
            new AbstractServiceConfiguration() {
                @Override
                public Boolean isWrapped() {
                    return Boolean.FALSE;
                }

                @Override
                public Boolean isWrapped(Method m) {
                    return Boolean.FALSE;
                }
            });
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
}
