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
package org.apache.cxf.jaxws.interceptors;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Holder;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * CXF-7455: HolderInInterceptor should tolerate missing
 * message parts in SOAP responses from non-compliant servers.
 */
public class HolderInInterceptorTest {

    private static final String NS = "http://test";

    @Test
    public void testMissingOutputPartDoesNotThrow() {
        ServiceInfo si = new ServiceInfo();
        InterfaceInfo ii = new InterfaceInfo(si,
            new QName(NS, "testInterface"));
        OperationInfo op = ii.addOperation(
            new QName(NS, "testOp"));

        MessageInfo inMsg = op.createMessage(
            new QName(NS, "inputMsg"), MessageInfo.Type.INPUT);
        op.setInput("input", inMsg);
        MessageInfo outMsg = op.createMessage(
            new QName(NS, "outputMsg"), MessageInfo.Type.OUTPUT);
        op.setOutput("output", outMsg);

        MessagePartInfo retPart =
            outMsg.addMessagePart("return");
        retPart.setTypeClass(String.class);

        MessagePartInfo holderPart =
            outMsg.addMessagePart("holderParam");
        holderPart.setTypeClass(String.class);

        BindingOperationInfo bop =
            new BindingOperationInfo(null, op);

        // Response with only the return value (index 0).
        // The holder part at index 1 is missing.
        MessageContentsList inObjects =
            new MessageContentsList("returnValue");

        Message inMessage = new MessageImpl();
        inMessage.setContent(List.class, inObjects);
        inMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);

        List<Holder<?>> holders = new ArrayList<>();
        holders.add(new Holder<String>("original"));

        Message outMessage = new MessageImpl();
        outMessage.put(
            HolderInInterceptor.CLIENT_HOLDERS, holders);

        Exchange exchange = new ExchangeImpl();
        exchange.setOutMessage(outMessage);
        exchange.put(BindingOperationInfo.class, bop);
        inMessage.setExchange(exchange);

        // Before fix: IndexOutOfBoundsException (CXF-7455)
        HolderInInterceptor interceptor =
            new HolderInInterceptor();
        interceptor.handleMessage(inMessage);

        // Holder retains its original value since the
        // response part was missing
        @SuppressWarnings("unchecked")
        Holder<String> holder =
            (Holder<String>) holders.get(0);
        assertNotNull(holder);
        assertEquals("original", holder.value);
    }
}
