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

package org.apache.cxf.binding.soap.interceptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SoapPreProtocolOutInterceptorTest extends Assert {
    private IMocksControl control;
    private SoapPreProtocolOutInterceptor interceptor;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        interceptor = new SoapPreProtocolOutInterceptor();
    }

    @Test
    public void testRequestorOutboundSoapAction() throws Exception {
        SoapMessage message = setUpMessage();
        interceptor.handleMessage(message);
        control.verify();

        Map<String, List<String>> reqHeaders = CastUtils.cast((Map)message.get(Message.PROTOCOL_HEADERS));
        assertNotNull(reqHeaders);
        List<String> soapaction = reqHeaders.get("soapaction");
        assertTrue(null != soapaction && soapaction.size() == 1);
        assertEquals("\"http://foo/bar/SEI/opReq\"", soapaction.get(0));
    }

    @Test
    public void testRequestorOutboundDispatchedSoapAction() throws Exception {
        SoapMessage message = setUpMessage();
        BindingOperationInfo dbop = setUpBindingOperationInfo("http://foo/bar/d",
                                                              "opDReq",
                                                              "opDResp",
                                                              SEI.class.getMethod("op", new Class[0]));
        SoapOperationInfo soi = new SoapOperationInfo();
        soi.setAction("http://foo/bar/d/SEI/opDReq");
        dbop.addExtensor(soi);

        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
        bop.setProperty("dispatchToOperation", dbop);

        interceptor.handleMessage(message);
        control.verify();

        Map<String, List<String>> reqHeaders = CastUtils.cast((Map)message.get(Message.PROTOCOL_HEADERS));
        assertNotNull(reqHeaders);
        List<String> soapaction = reqHeaders.get("soapaction");
        assertTrue(null != soapaction && soapaction.size() == 1);
        assertEquals("\"http://foo/bar/d/SEI/opDReq\"", soapaction.get(0));
    }

    private SoapMessage setUpMessage() throws Exception {
        
        SoapMessage message = new SoapMessage(new MessageImpl());
        Exchange exchange = new ExchangeImpl();
        BindingOperationInfo bop = setUpBindingOperationInfo("http://foo/bar",
                                                             "opReq",
                                                             "opResp",
                                                             SEI.class.getMethod("op", new Class[0]));
        SoapOperationInfo sop = new SoapOperationInfo();
        sop.setAction("http://foo/bar/SEI/opReq");
        bop.addExtensor(sop);
        exchange.put(BindingOperationInfo.class, bop);
        message.setExchange(exchange);
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        
        control.replay();
        return message;
    }
    
    private BindingOperationInfo setUpBindingOperationInfo(String nsuri, 
                                                           String opreq,
                                                           String opresp,
                                                           Method method) {
        ServiceInfo si = new ServiceInfo();
        InterfaceInfo iinf = new InterfaceInfo(si, 
                                               new QName(nsuri, method.getDeclaringClass().getSimpleName()));
        OperationInfo opInfo = iinf.addOperation(new QName(nsuri, method.getName()));
        opInfo.setProperty(Method.class.getName(), method);
        opInfo.setInput(opreq, opInfo.createMessage(new QName(nsuri, opreq), Type.INPUT));
        opInfo.setOutput(opresp, opInfo.createMessage(new QName(nsuri, opresp), Type.INPUT));
        
        BindingOperationInfo bindingOpInfo = new BindingOperationInfo(null, opInfo);
        
        return bindingOpInfo;
    }

    private interface SEI {
        String op();
    }
}
