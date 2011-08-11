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

package org.apache.cxf.transport.jbi;

import java.util.logging.Logger;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;


import org.junit.Test;

public class JBIDestinationTest extends AbstractJBITest {
    static final Logger LOG = LogUtils.getLogger(JBIDestinationTest.class);
    @Test
    public void testDestination() throws Exception {
        LOG.info("JBI destination test");
    }
    
    @Test
    public void testOutputStreamSubstitutionDoesntCauseExceptionInDoClose() throws Exception {
        //Create enough of the object structure to get through the code.
        NormalizedMessage normalizedMessage = control.createMock(NormalizedMessage.class);
        channel = control.createMock(DeliveryChannel.class);
        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(false);
        Message message = new MessageImpl();
        message.setExchange(exchange);
        
        
        MessageExchange messageExchange = control.createMock(MessageExchange.class);
        EasyMock.expect(messageExchange.createMessage()).andReturn(normalizedMessage);
        message.put(MessageExchange.class, messageExchange);
        channel.send(messageExchange);
        EasyMock.replay(channel);
        
        JBIDestinationOutputStream jbiOS = new JBIDestinationOutputStream(message, null, channel);
        
        //Create array of more than what is in threshold in CachedOutputStream, 
        //though the threshold in CachedOutputStream should be made protected 
        //perhaps so it can be referenced here in case it ever changes.
        int targetLength = 64 * 1025;
        StringBuffer sb = new StringBuffer();
        sb.append("<root>");
        while (sb.length() < targetLength) {
            sb.append("<dummy>some xml</dummy>");
        }
        sb.append("</root>");
        byte[] testBytes = sb.toString().getBytes();
        
        jbiOS.write(testBytes);        
        jbiOS.doClose();
        
        //Verify send method was called.
        EasyMock.verify(channel);
    }
}
