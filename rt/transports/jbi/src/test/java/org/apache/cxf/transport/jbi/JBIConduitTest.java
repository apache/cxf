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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

public class JBIConduitTest extends AbstractJBITest {
    static final Logger LOG = LogUtils.getLogger(JBIDestinationTest.class);

    
    @Test
    public void testPrepare() throws Exception {
        LOG.info("test prepare");
        JBIConduit conduit = setupJBIConduit(false, false);
        Message message = new MessageImpl();
        try {
            conduit.prepare(message);
        } catch (Exception ex) {
            ex.printStackTrace();            
        }
        assertNotNull(message.getContent(OutputStream.class));
        assertTrue(message.getContent(OutputStream.class) instanceof JBIConduitOutputStream);
    }
    
    @Test
    public void testSendOut() throws Exception {
        LOG.info("test send");
        JBIConduit conduit = setupJBIConduit(true, false); 
        Message message = new MessageImpl();

        Class<org.apache.hello_world_soap_http.Greeter> greeterCls 
            = org.apache.hello_world_soap_http.Greeter.class;
        message.put(Method.class.getName(), greeterCls.getMethod("sayHi"));
        
        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(false);
        message.setExchange(exchange);
        exchange.setInMessage(message);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        BindingMessageInfo bmi = control.createMock(BindingMessageInfo.class);
        EasyMock.expect(boi.getOutput()).andReturn(bmi);
        exchange.put(BindingOperationInfo.class, boi);
        MessageExchangeFactory factory = control.createMock(MessageExchangeFactory.class);
        EasyMock.expect(channel.createExchangeFactoryForService(
                        null)).andReturn(factory);
        InOut xchg = control.createMock(InOut.class);
        EasyMock.expect(factory.createInOutExchange()).andReturn(xchg);
        NormalizedMessage inMsg = control.createMock(NormalizedMessage.class);
        EasyMock.expect(xchg.createMessage()).andReturn(inMsg);
        NormalizedMessage outMsg = control.createMock(NormalizedMessage.class);
        EasyMock.expect(xchg.getOutMessage()).andReturn(outMsg);
        
        Source source = new StreamSource(new ByteArrayInputStream(
                            "<message>TestHelloWorld</message>".getBytes()));
        EasyMock.expect(outMsg.getContent()).andReturn(source);
        control.replay();
        try {
            conduit.prepare(message);
        } catch (IOException ex) {
            assertFalse("JMSConduit can't perpare to send out message", false);
            ex.printStackTrace();            
        }            
        OutputStream os = message.getContent(OutputStream.class);
        assertTrue("The OutputStream should not be null ", os != null);
        os.write("HelloWorld".getBytes());
        os.close();              
        InputStream is = inMessage.getContent(InputStream.class);
        assertNotNull(is);
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is, null);
        assertNotNull(reader);
        reader.nextTag();
         
        String reponse = reader.getElementText();
        assertEquals("The reponse date should be equals", reponse, "TestHelloWorld");
    }
}
