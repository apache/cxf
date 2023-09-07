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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FaultTest {
    @Test
    public void logOnceForFaultsOccurringAfterLoggingOutPhase() throws IOException {

        Message message = new MessageImpl();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, outputStream);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEventSenderMock logEventSender = new LogEventSenderMock();
        LoggingOutInterceptor interceptor = new LoggingOutInterceptor(logEventSender);
        
        interceptor.handleMessage(message);
        OutputStream preFaultOut = message.getContent(OutputStream.class);
        
        // simulate fault happening after message is already handled in out chain
        interceptor.handleFault(message); // first we unwind
        interceptor.handleMessage(message); // then we handle in the fault chain
        
        byte[] payload = "TestMessage".getBytes(StandardCharsets.UTF_8);
        // simulate writing that is setup based on the pre-fault output stream on message
        // this is what happens when StaxOutInterceptor is in use
        // StaxOutInterceptor sets XmlStreamWriter content wrapping the OutputStream in the message at that time
        // it does not recreate XmlStreamWriter during out fault chain, as it is already set during out chain
        preFaultOut.write(payload);
        
        // Then close is called through close on Conduit, 
        // which means close is called on the OutputStream in message at the time of close 
        OutputStream postFaultOut = message.getContent(OutputStream.class);
        postFaultOut.close();
        
        assertEquals(1, logEventSender.getLogEvents().size());
        assertEquals("TestMessage", logEventSender.getLogEvents().get(0).getPayload());
    }
}
