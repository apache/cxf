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

package org.apache.cxf.interceptor;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for testing DocLiteralInInterceptor to use Source Data Binding 
 * 
 */
public class DocLiteralInInterceptorTest extends Assert {
    
    private static final String NS = "http://cxf.apache.org/wsdl-first/types";
    protected IMocksControl control;
    
    @Before
    public void setUp() throws Exception {
        control = EasyMock.createNiceControl();
    }
    
    @After 
    public void tearDown() throws Exception {
        control.verify();
    }

    @Test
    public void testUnmarshalSourceData() throws Exception {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getClass()
            .getResourceAsStream("resources/multiPartDocLitBareReq.xml"));
        
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag());
        
        XMLStreamReader filteredReader = new PartialXMLStreamReader(reader, 
             new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body"));

        // advance the xml reader to the message parts
        StaxUtils.read(filteredReader);
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag());

        Message m = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        
        Service service = control.createMock(Service.class);
        exchange.put(Service.class, service);
        EasyMock.expect(service.getDataBinding()).andReturn(new SourceDataBinding());
        EasyMock.expect(service.size()).andReturn(0).anyTimes();
        
        Endpoint endpoint = control.createMock(Endpoint.class);
        exchange.put(Endpoint.class, endpoint);
        
        OperationInfo operationInfo = new OperationInfo();
        MessageInfo messageInfo = new MessageInfo(operationInfo, Type.INPUT, 
                                                  new QName("http://foo.com", "bar"));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo1"), null));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo2"), null));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo3"), null));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo4"), null));
        operationInfo.setInput("inputName", messageInfo);
        
        BindingOperationInfo boi = new BindingOperationInfo(null, operationInfo);
        exchange.put(BindingOperationInfo.class, boi);
        
        EndpointInfo endpointInfo = control.createMock(EndpointInfo.class);
        BindingInfo binding = control.createMock(BindingInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(endpointInfo).anyTimes();
        EasyMock.expect(endpointInfo.getBinding()).andReturn(binding).anyTimes();
        EasyMock.expect(binding.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        EasyMock.expect(endpointInfo.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        EasyMock.expect(endpoint.size()).andReturn(0).anyTimes();
        
        ServiceInfo serviceInfo = control.createMock(ServiceInfo.class);
        EasyMock.expect(endpointInfo.getService()).andReturn(serviceInfo).anyTimes();
        
        EasyMock.expect(serviceInfo.getName()).andReturn(new QName("http://foo.com", "service")).anyTimes();
        InterfaceInfo interfaceInfo = control.createMock(InterfaceInfo.class);
        EasyMock.expect(serviceInfo.getInterface()).andReturn(interfaceInfo).anyTimes();
        EasyMock.expect(interfaceInfo.getName())
            .andReturn(new QName("http://foo.com", "interface")).anyTimes();
        
        EasyMock.expect(endpointInfo.getName()).andReturn(new QName("http://foo.com", "endpoint")).anyTimes();
        EasyMock.expect(endpointInfo.getProperty("URI", URI.class)).andReturn(new URI("dummy")).anyTimes();
        
        List<OperationInfo> operations = new ArrayList<OperationInfo>();
        EasyMock.expect(interfaceInfo.getOperations()).andReturn(operations).anyTimes();
        
        m.setExchange(exchange);
        m.put(Message.SCHEMA_VALIDATION_ENABLED, false);
        m.setContent(XMLStreamReader.class, reader);

        control.replay();
        
        new DocLiteralInInterceptor().handleMessage(m);
        
        MessageContentsList params = (MessageContentsList)m.getContent(List.class);
        
        assertEquals(4, params.size());
        assertEquals("StringDefaultInputElem", 
                     ((DOMSource)params.get(0)).getNode().getFirstChild().getNodeName());
        assertEquals("IntParamInElem", 
                     ((DOMSource)params.get(1)).getNode().getFirstChild().getNodeName());        
    }
    
        
    @Test
    public void testUnmarshalSourceDataWrapped() throws Exception {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getClass()
            .getResourceAsStream("resources/docLitWrappedReq.xml"));

        assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag());

        XMLStreamReader filteredReader = new PartialXMLStreamReader(reader,
            new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body"));

        // advance the xml reader to the message parts
        StaxUtils.read(filteredReader);
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag());

        Message m = new MessageImpl();
        // request to keep the document as wrapped
        m.put(DocLiteralInInterceptor.KEEP_PARAMETERS_WRAPPER, true);
        Exchange exchange = new ExchangeImpl();

        Service service = control.createMock(Service.class);
        exchange.put(Service.class, service);
        EasyMock.expect(service.getDataBinding()).andReturn(new SourceDataBinding()).anyTimes();
        EasyMock.expect(service.size()).andReturn(0).anyTimes();

        Endpoint endpoint = control.createMock(Endpoint.class);
        exchange.put(Endpoint.class, endpoint);

        // wrapped
        OperationInfo operationInfo = new OperationInfo();
        MessageInfo messageInfo = new MessageInfo(operationInfo, Type.INPUT, new QName(NS, "foo"));
        messageInfo.addMessagePart(new MessagePartInfo(new QName(NS, "personId"), null));
        messageInfo.addMessagePart(new MessagePartInfo(new QName(NS, "ssn"), null));
        messageInfo.getMessagePart(0).setConcreteName(new QName(NS, "personId"));
        messageInfo.getMessagePart(1).setConcreteName(new QName(NS, "ssn"));
        operationInfo.setInput("inputName", messageInfo);

        // wrapper
        OperationInfo operationInfoWrapper = new OperationInfo();
        MessageInfo messageInfoWrapper = new MessageInfo(operationInfo, Type.INPUT, new QName(NS, "foo"));
        messageInfoWrapper.addMessagePart(new MessagePartInfo(new QName(NS, "GetPerson"), null));
        messageInfoWrapper.getMessagePart(0).setConcreteName(new QName(NS, "GetPerson"));
        operationInfoWrapper.setInput("inputName", messageInfoWrapper);
        operationInfoWrapper.setUnwrappedOperation(operationInfo);       

        ServiceInfo serviceInfo = control.createMock(ServiceInfo.class);

        EasyMock.expect(serviceInfo.getName()).andReturn(new QName("http://foo.com", "service")).anyTimes();
        InterfaceInfo interfaceInfo = control.createMock(InterfaceInfo.class);
        EasyMock.expect(serviceInfo.getInterface()).andReturn(interfaceInfo).anyTimes();
        EasyMock.expect(interfaceInfo.getName()).andReturn(new QName("http://foo.com", "interface"))
            .anyTimes();

        BindingInfo bindingInfo = new BindingInfo(serviceInfo, "");
        BindingOperationInfo boi = new BindingOperationInfo(bindingInfo, operationInfoWrapper);
        exchange.put(BindingOperationInfo.class, boi);

        EndpointInfo endpointInfo = control.createMock(EndpointInfo.class);
        BindingInfo binding = control.createMock(BindingInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(endpointInfo).anyTimes();
        EasyMock.expect(endpointInfo.getBinding()).andReturn(binding).anyTimes();
        EasyMock.expect(binding.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        EasyMock.expect(endpointInfo.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        EasyMock.expect(endpoint.size()).andReturn(0).anyTimes();
        EasyMock.expect(endpointInfo.getService()).andReturn(serviceInfo).anyTimes();

        EasyMock.expect(endpointInfo.getName()).andReturn(new QName("http://foo.com", "endpoint")).anyTimes();
        EasyMock.expect(endpointInfo.getProperty("URI", URI.class)).andReturn(new URI("dummy")).anyTimes();

        List<OperationInfo> operations = new ArrayList<OperationInfo>();
        EasyMock.expect(interfaceInfo.getOperations()).andReturn(operations).anyTimes();

        m.setExchange(exchange);
        m.put(Message.SCHEMA_VALIDATION_ENABLED, false);
        m.setContent(XMLStreamReader.class, reader);

        control.replay();

        new DocLiteralInInterceptor().handleMessage(m);

        MessageContentsList params = (MessageContentsList)m.getContent(List.class);

        // we expect a wrapped document
        assertEquals(1, params.size());
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("ns", NS);

        XPathUtils xu = new XPathUtils(ns);
        assertEquals("hello", xu.getValueString("//ns:GetPerson/ns:personId", 
                                                ((DOMSource)params.get(0)).getNode().getFirstChild()));
        assertEquals("1234", xu.getValueString("//ns:GetPerson/ns:ssn", 
                                               ((DOMSource)params.get(0)).getNode().getFirstChild()));

    }
    
}
