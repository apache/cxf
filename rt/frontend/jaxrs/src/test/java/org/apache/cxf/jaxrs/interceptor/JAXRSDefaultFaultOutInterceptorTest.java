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

package org.apache.cxf.jaxrs.interceptor;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JAXRSDefaultFaultOutInterceptorTest {

    @Test
    public void testFaultCauseNotLeakedByDefault() throws Exception {
        String out = writeFault(false);
        assertTrue(out, out.contains(">Application fault</"));
        assertFalse(out, out.contains(IllegalStateException.class.getName()));
        assertFalse(out, out.contains("internal detail"));
    }

    @Test
    public void testFaultCauseIncludedWhenEnabled() throws Exception {
        String out = writeFault(true);
        assertTrue(out, out.contains(">Application fault Caused by: internal detail</"));
        assertFalse(out, out.contains(IllegalStateException.class.getName()));
    }

    private static String writeFault(boolean causeEnabled) throws Exception {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.get(ServerProviderFactory.class.getName()))
            .thenReturn(ServerProviderFactory.getInstance());

        Message inMessage = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.put(Endpoint.class, endpoint);
        exchange.setInMessage(inMessage);
        inMessage.setExchange(exchange);

        Message outMessage = new MessageImpl();
        outMessage.setExchange(exchange);
        exchange.setOutFaultMessage(outMessage);
        if (causeEnabled) {
            outMessage.put(Message.EXCEPTION_MESSAGE_CAUSE_ENABLED, Boolean.TRUE);
        }

        Fault fault = new Fault(new org.apache.cxf.common.i18n.Message("Application fault",
                                    (ResourceBundle) null),
                                new IllegalStateException("internal detail"));
        outMessage.setContent(Exception.class, fault);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(bos);
        outMessage.setContent(XMLStreamWriter.class, writer);

        new JAXRSDefaultFaultOutInterceptor().handleMessage(outMessage);
        writer.flush();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
