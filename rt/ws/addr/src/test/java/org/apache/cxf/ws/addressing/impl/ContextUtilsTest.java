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

package org.apache.cxf.ws.addressing.impl;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.Extensible;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ContextUtilsTest extends Assert {
    private static final QName WSA_ACTION_QNAME = 
        new QName(JAXWSAConstants.NS_WSA, Names.WSAW_ACTION_NAME);
    private static final QName OLD_WSDL_WSA_ACTION_QNAME = 
        new QName(Names.WSA_NAMESPACE_WSDL_NAME_OLD, Names.WSAW_ACTION_NAME);
    
    private IMocksControl control;
        

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @Test
    public void testGetActionFromExtensible() {
        Map<QName, Object> attributes = new HashMap<QName, Object>();
        Extensible ext = control.createMock(Extensible.class);
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        attributes.put(WSA_ACTION_QNAME, "urn:foo:test:2");
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn("urn:foo:test:1");
        control.replay();
        
        String action = InternalContextUtils.getAction(ext);
        assertEquals("urn:foo:test:1", action);
        
        control.reset();
        attributes.clear();
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn(null);
        attributes.put(WSA_ACTION_QNAME, "urn:foo:test:2");
        control.replay();
        
        action = InternalContextUtils.getAction(ext);
        assertEquals("urn:foo:test:2", action);
        
        control.reset();
        attributes.clear();
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn(null);
        attributes.put(OLD_WSDL_WSA_ACTION_QNAME, "urn:foo:test:3");
        control.replay();
        
        action = InternalContextUtils.getAction(ext);
        assertEquals("urn:foo:test:3", action);
        
        control.reset();
        attributes.clear();
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn(null);
        control.replay();
        
        action = InternalContextUtils.getAction(ext);
        assertEquals(null, action);
    }
    
    @Test
    public void testGetActionFromMessage() {
        Message msg = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);

        QName mqname = new QName("http://foo.com", "bar");
        QName fqname = new QName("urn:foo:test:4", "fault");
        OperationInfo operationInfo = new OperationInfo();
        MessageInfo messageInfo = new MessageInfo(operationInfo, Type.OUTPUT, mqname); 
        messageInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "partInfo"), null));
        operationInfo.setOutput("outputName", messageInfo);
        FaultInfo faultInfo = new FaultInfo(fqname, mqname, operationInfo);
        operationInfo.addFault(faultInfo);
        BindingOperationInfo boi = new BindingOperationInfo(null, operationInfo);

        // test 1 : retrieving the normal action prop from the message
        EasyMock.expect(msg.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        EasyMock.expect(msg.get(ContextUtils.ACTION)).andReturn("urn:foo:test:1");
        control.replay();
        
        AttributedURIType action = InternalContextUtils.getAction(msg);
        assertNotNull(action);
        assertEquals("urn:foo:test:1", action.getValue());
        control.reset();

        // test 2 : retrieving the normal soap action prop from the message
        EasyMock.expect(msg.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        EasyMock.expect(msg.get(SoapBindingConstants.SOAP_ACTION)).andReturn("urn:foo:test:2");
        control.replay();
        
        action = InternalContextUtils.getAction(msg);
        assertNotNull(action);
        assertEquals("urn:foo:test:2", action.getValue());
        control.reset();

        // test 3 : retrieving the action prop from the message info
        EasyMock.expect(msg.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        messageInfo.setProperty(ContextUtils.ACTION, "urn:foo:test:3");
        control.replay();
        
        action = InternalContextUtils.getAction(msg);
        assertNotNull(action);
        assertEquals("urn:foo:test:3", action.getValue());
        control.reset();
        
        // test 4 : retrieving the action for a fault without message part
        SoapFault fault = new SoapFault("faulty service", new RuntimeException(), fqname);
        EasyMock.expect(msg.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(msg.getContent(Exception.class)).andReturn(fault).anyTimes();
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        control.replay();
        
        action = InternalContextUtils.getAction(msg);
        assertNull(action);
        control.reset();
        
        // test 5 : retrieving the action for a fault with matching message part
        faultInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com", "faultInfo"), null));
        faultInfo.getMessagePart(0).setTypeClass(RuntimeException.class);
        faultInfo.addExtensionAttribute(Names.WSAW_ACTION_QNAME, "urn:foo:test:4");
        EasyMock.expect(msg.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(msg.getContent(Exception.class)).andReturn(fault).anyTimes();
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        control.replay();
        
        action = InternalContextUtils.getAction(msg);
        assertNotNull(action);
        assertEquals("urn:foo:test:4", action.getValue());
        control.reset();

        // test 6 : retrieving the action for a ws-addr fault with matching message part
        fault = new SoapFault("Action Mismatch",
                              new QName(Names.WSA_NAMESPACE_NAME,
                                        Names.ACTION_MISMATCH_NAME));
        EasyMock.expect(msg.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(msg.getContent(Exception.class)).andReturn(fault).anyTimes();
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        control.replay();
        
        action = InternalContextUtils.getAction(msg);
        assertNotNull(action);
        assertEquals(Names.WSA_DEFAULT_FAULT_ACTION, action.getValue());
        control.reset();

        // test 7 : retrieve the action for a fault matching the fault class with the WebFault annotation
        fault = new SoapFault("faulty service", new TestFault(), Fault.FAULT_CODE_SERVER);
        faultInfo.addMessagePart(new MessagePartInfo(new QName("http://foo.com:7", "faultInfo"), null));
        faultInfo.getMessagePart(0).setTypeClass(Object.class);
        faultInfo.getMessagePart(0).setConcreteName(new QName("urn:foo:test:7", "testFault"));
        faultInfo.addExtensionAttribute(Names.WSAW_ACTION_QNAME, "urn:foo:test:7");
        EasyMock.expect(msg.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(msg.getContent(Exception.class)).andReturn(fault).anyTimes();
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        control.replay();
        
        action = InternalContextUtils.getAction(msg);
        assertNotNull(action);
        assertEquals("urn:foo:test:7", action.getValue());
    }
    
    @WebFault(name = "testFault", targetNamespace = "urn:foo:test:7")
    public class TestFault extends Exception {
    }
}
