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
package org.apache.cxf.ext.logging;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.junit.Assert;
import org.junit.Test;

public class DefaultLogEventMapperTest {

    @Test
    public void testRest() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(Message.REQUEST_URI, "test");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEvent event = mapper.map(message);
        Assert.assertEquals("GET[test]", event.getOperationName());
    }
    
    /**
     * Test for NPE described in CXF-6436
     */
    @Test
    public void testNullValues() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, null);
        message.put(Message.REQUEST_URI, null);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEvent event = mapper.map(message);
        Assert.assertEquals("", event.getOperationName());
    }
    
    @Test
    public void testSoap() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        ExchangeImpl exchange = new ExchangeImpl();
        ServiceInfo service = new ServiceInfo();
        BindingInfo info = new BindingInfo(service, "bindingId");
        SoapBinding value = new SoapBinding(info);
        exchange.put(Binding.class, value);
        OperationInfo opInfo = new OperationInfo();
        opInfo.setName(new QName("http://my", "Operation"));
        BindingOperationInfo boi = new BindingOperationInfo(info, opInfo);
        exchange.put(BindingOperationInfo.class, boi);
        message.setExchange(exchange);
        LogEvent event = mapper.map(message);
        Assert.assertEquals("{http://my}Operation", event.getOperationName());
    }

}
