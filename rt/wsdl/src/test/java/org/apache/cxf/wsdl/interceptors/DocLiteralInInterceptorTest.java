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

package org.apache.cxf.wsdl.interceptors;

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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for testing DocLiteralInInterceptor to use Source Data Binding
 *
 */
public class DocLiteralInInterceptorTest {

    private static final String NS = "http://cxf.apache.org/wsdl-first/types";

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

        Service service = mock(Service.class);
        exchange.put(Service.class, service);
        when(service.getDataBinding()).thenReturn(new SourceDataBinding());
        when(service.size()).thenReturn(0);
        when(service.isEmpty()).thenReturn(true);

        Endpoint endpoint = mock(Endpoint.class);
        exchange.put(Endpoint.class, endpoint);

        OperationInfo operationInfo = new OperationInfo();
        operationInfo.setProperty("operation.is.synthetic", Boolean.TRUE);
        MessageInfo messageInfo = new MessageInfo(operationInfo, Type.INPUT,
                                                  new QName("http://foo.com", "bar"));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo1"), null));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo2"), null));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo3"), null));
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo4"), null));

        for (MessagePartInfo mpi : messageInfo.getMessageParts()) {
            mpi.setMessageContainer(messageInfo);
        }

        operationInfo.setInput("inputName", messageInfo);

        BindingOperationInfo boi = new BindingOperationInfo(null, operationInfo);
        exchange.put(BindingOperationInfo.class, boi);

        EndpointInfo endpointInfo = mock(EndpointInfo.class);
        BindingInfo binding = mock(BindingInfo.class);
        when(endpoint.getEndpointInfo()).thenReturn(endpointInfo);
        when(endpointInfo.getBinding()).thenReturn(binding);
        when(binding.getProperties()).thenReturn(new HashMap<String, Object>());
        when(endpointInfo.getProperties()).thenReturn(new HashMap<String, Object>());
        when(endpoint.size()).thenReturn(0);
        when(endpoint.isEmpty()).thenReturn(true);

        ServiceInfo serviceInfo = mock(ServiceInfo.class);
        when(endpointInfo.getService()).thenReturn(serviceInfo);

        when(serviceInfo.getName()).thenReturn(new QName("http://foo.com", "service"));
        InterfaceInfo interfaceInfo = mock(InterfaceInfo.class);
        when(serviceInfo.getInterface()).thenReturn(interfaceInfo);
        when(interfaceInfo.getName()).thenReturn(new QName("http://foo.com", "interface"));

        when(endpointInfo.getName()).thenReturn(new QName("http://foo.com", "endpoint"));
        when(endpointInfo.getProperty("URI", URI.class)).thenReturn(new URI("dummy"));

        List<OperationInfo> operations = new ArrayList<>();
        when(interfaceInfo.getOperations()).thenReturn(operations);

        m.setExchange(exchange);
        m.put(Message.SCHEMA_VALIDATION_ENABLED, false);
        m.setContent(XMLStreamReader.class, reader);

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

        Service service = mock(Service.class);
        exchange.put(Service.class, service);
        when(service.getDataBinding()).thenReturn(new SourceDataBinding());
        when(service.size()).thenReturn(0);
        when(service.isEmpty()).thenReturn(true);

        Endpoint endpoint = mock(Endpoint.class);
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

        ServiceInfo serviceInfo = mock(ServiceInfo.class);

        when(serviceInfo.getName()).thenReturn(new QName("http://foo.com", "service"));
        InterfaceInfo interfaceInfo = mock(InterfaceInfo.class);
        when(serviceInfo.getInterface()).thenReturn(interfaceInfo);
        when(interfaceInfo.getName()).thenReturn(new QName("http://foo.com", "interface"));

        BindingInfo bindingInfo = new BindingInfo(serviceInfo, "");
        BindingOperationInfo boi = new BindingOperationInfo(bindingInfo, operationInfoWrapper);
        exchange.put(BindingOperationInfo.class, boi);

        EndpointInfo endpointInfo = mock(EndpointInfo.class);
        BindingInfo binding = mock(BindingInfo.class);
        when(endpoint.getEndpointInfo()).thenReturn(endpointInfo);
        when(endpointInfo.getBinding()).thenReturn(binding);
        when(binding.getProperties()).thenReturn(new HashMap<String, Object>());
        when(endpointInfo.getProperties()).thenReturn(new HashMap<String, Object>());
        when(endpoint.size()).thenReturn(0);
        when(endpoint.isEmpty()).thenReturn(true);
        when(endpointInfo.getService()).thenReturn(serviceInfo);

        when(endpointInfo.getName()).thenReturn(new QName("http://foo.com", "endpoint"));
        when(endpointInfo.getProperty("URI", URI.class)).thenReturn(new URI("dummy"));

        List<OperationInfo> operations = new ArrayList<>();
        when(interfaceInfo.getOperations()).thenReturn(operations);

        m.setExchange(exchange);
        m.put(Message.SCHEMA_VALIDATION_ENABLED, false);
        m.setContent(XMLStreamReader.class, reader);

        new DocLiteralInInterceptor().handleMessage(m);

        MessageContentsList params = (MessageContentsList)m.getContent(List.class);

        // we expect a wrapped document
        assertEquals(1, params.size());

        Map<String, String> ns = new HashMap<>();
        ns.put("ns", NS);

        XPathUtils xu = new XPathUtils(ns);
        assertEquals("hello", xu.getValueString("//ns:GetPerson/ns:personId",
                                                ((DOMSource)params.get(0)).getNode().getFirstChild()));
        assertEquals("1234", xu.getValueString("//ns:GetPerson/ns:ssn",
                                               ((DOMSource)params.get(0)).getNode().getFirstChild()));

    }

}