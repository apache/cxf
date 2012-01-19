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

package org.apache.cxf.binding.soap;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.junit.Assert;
import org.junit.Test;

public class SoapActionInterceptorTest extends Assert {

    
    @Test
    public void testSoapAction() throws Exception {
        SoapPreProtocolOutInterceptor i = new SoapPreProtocolOutInterceptor();
        
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.getExchange().setOutMessage(message);
        SoapBinding sb = new SoapBinding(null);
        message = sb.createMessage(message);
        assertNotNull(message);
        assertTrue(message instanceof SoapMessage);
        SoapMessage soapMessage = (SoapMessage) message;
        soapMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        assertEquals(Soap11.getInstance(), soapMessage.getVersion());
        (new SoapPreProtocolOutInterceptor()).handleMessage(soapMessage);
        Map<String, List<String>> reqHeaders = CastUtils.cast((Map)soapMessage.get(Message.PROTOCOL_HEADERS));
        assertNotNull(reqHeaders);
        assertEquals("\"\"", reqHeaders.get(SoapBindingConstants.SOAP_ACTION).get(0));

        sb.setSoapVersion(Soap12.getInstance());
        soapMessage.clear();
        soapMessage = (SoapMessage) sb.createMessage(soapMessage);
        soapMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        i.handleMessage(soapMessage);
        String ct = (String) soapMessage.get(Message.CONTENT_TYPE);
        assertEquals("application/soap+xml", ct);
        
        BindingOperationInfo bop = createBindingOperation();
 
        soapMessage.getExchange().put(BindingOperationInfo.class, bop);
        SoapOperationInfo soapInfo = new SoapOperationInfo();
        soapInfo.setAction("foo");
        bop.addExtensor(soapInfo);
        
        i.handleMessage(soapMessage);
        ct = (String) soapMessage.get(Message.CONTENT_TYPE);
        assertEquals("application/soap+xml; action=\"foo\"", ct);
    }

    private BindingOperationInfo createBindingOperation() {
        ServiceInfo s = new ServiceInfo();
        InterfaceInfo ii = s.createInterface(new QName("FooInterface"));
        s.setInterface(ii);
        ii.addOperation(new QName("fooOp"));
        
        BindingInfo b = new BindingInfo(s, "foo");
        return b.buildOperation(new QName("fooOp"), null, null);
    }

}
