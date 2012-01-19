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

package org.apache.cxf.jaxws.handler;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import org.apache.cxf.jaxws.handler.logical.LogicalMessageContextImpl;
import org.apache.cxf.jaxws.handler.logical.LogicalMessageImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.handlers.types.AddNumbers;
import org.apache.handlers.types.ObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LogicalMessageImplTest extends Assert {
    AddNumbers req;
    List<Object> args;

    @Before
    public void setUp() {
        req = new AddNumbers();        
        req.setArg0(10);
        req.setArg1(20);
        args = new ArrayList<Object>();
        args.add(req);
    }

    @Test
    public void testGetPayloadOfJAXB() throws Exception {
        //using Dispatch
        JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
        Message message = new MessageImpl();
        Exchange e = new ExchangeImpl();
        message.setExchange(e);
        LogicalMessageContextImpl lmci = new LogicalMessageContextImpl(message);

        JAXBElement<AddNumbers> el = new ObjectFactory().createAddNumbers(req);
        
        LogicalMessageImpl lmi = new LogicalMessageImpl(lmci);
        lmi.setPayload(el, ctx);
        
        Object obj = lmi.getPayload(ctx);
        assertTrue(obj instanceof JAXBElement);
        JAXBElement<?> el2 = (JAXBElement)obj;
        assertTrue(el2.getValue() instanceof AddNumbers);
        AddNumbers resp = (AddNumbers)el2.getValue();
        assertEquals(req.getArg0(), resp.getArg0());        
        assertEquals(req.getArg1(), resp.getArg1());        
    }
}
