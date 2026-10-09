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

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Holder;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class HolderInInterceptorTest {

    private static final String NS = "urn:test";

    private Holder<Object> holder1 = new Holder<>();
    private Holder<Object> holder2 = new Holder<>();

    @Test
    public void testClientResponseWithAllParts() {
        MessageContentsList contents = new MessageContentsList(Arrays.asList("ret", "a", "b"));
        Message in = createClientInMessage(contents);

        new HolderInInterceptor().handleMessage(in);

        assertEquals("a", holder1.value);
        assertEquals("b", holder2.value);
        assertSame(holder1, contents.get(1));
        assertSame(holder2, contents.get(2));
    }

    @Test
    public void testClientResponseWithMissingPart() {
        // the server omitted the last out part defined in the WSDL
        MessageContentsList contents = new MessageContentsList(Arrays.asList("ret", "a"));
        Message in = createClientInMessage(contents);

        new HolderInInterceptor().handleMessage(in);

        assertEquals("a", holder1.value);
        assertNull(holder2.value);
        assertSame(holder1, contents.get(1));
        assertSame(holder2, contents.get(2));
    }

    @Test
    public void testClientResponseWithEmptyBody() {
        Message in = createClientInMessage(null);

        new HolderInInterceptor().handleMessage(in);

        assertNull(holder1.value);
        assertNull(holder2.value);
        List<?> contents = in.getContent(List.class);
        assertSame(holder1, contents.get(1));
        assertSame(holder2, contents.get(2));
    }

    private Message createClientInMessage(MessageContentsList contents) {
        OperationInfo op = new OperationInfo();
        MessageInfo output = op.createMessage(new QName(NS, "testResponse"), MessageInfo.Type.OUTPUT);
        op.setOutput("testResponse", output);
        addPart(output, "return", 0);
        addPart(output, "out1", 1);
        addPart(output, "out2", 2);

        Exchange exchange = new ExchangeImpl();
        exchange.put(BindingOperationInfo.class, new BindingOperationInfo(null, op));

        Message out = new MessageImpl();
        out.put(HolderInInterceptor.CLIENT_HOLDERS, Arrays.asList(holder1, holder2));
        exchange.setOutMessage(out);

        Message in = new MessageImpl();
        in.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        if (contents != null) {
            in.setContent(List.class, contents);
        }
        exchange.setInMessage(in);
        return in;
    }

    private static void addPart(MessageInfo mi, String name, int index) {
        MessagePartInfo part = mi.addMessagePart(new QName(NS, name));
        part.setIndex(index);
        part.setTypeClass(String.class);
    }
}
