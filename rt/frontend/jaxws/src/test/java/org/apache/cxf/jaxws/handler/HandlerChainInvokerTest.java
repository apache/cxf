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



import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.handler.logical.LogicalMessageContextImpl;
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HandlerChainInvokerTest extends Assert {

    private static final int HANDLER_COUNT = 4;

    HandlerChainInvoker invoker;
    Message message;
    LogicalMessageContext lmc;
    MessageContext pmc;

    TestLogicalHandler[] logicalHandlers = new TestLogicalHandler[HANDLER_COUNT];
    TestProtocolHandler[] protocolHandlers = new TestProtocolHandler[HANDLER_COUNT];

    @Before
    public void setUp() {
        AbstractHandlerBase.clear();

        List<Handler> handlers = new ArrayList<Handler>();
        for (int i = 0; i < logicalHandlers.length; i++) {
            logicalHandlers[i] = new TestLogicalHandler();
            handlers.add(logicalHandlers[i]);
        }
        for (int i = 0; i < protocolHandlers.length; i++) {
            protocolHandlers[i] = new TestProtocolHandler();
            handlers.add(protocolHandlers[i]);
        }

        invoker = new HandlerChainInvoker(handlers);
        
        message = new MessageImpl();
        Exchange e = new ExchangeImpl();
        message.setExchange(e);
        lmc = new LogicalMessageContextImpl(message);
        pmc = new WrappedMessageContext(message);      
/*        
        payload = new DOMSource();
        message.setContent(Source.class, payload);*/
        
    }

    @Test
    public void testInvokeEmptyHandlerChain() {
        invoker = new HandlerChainInvoker(new ArrayList<Handler>());
        assertTrue(invoker.invokeLogicalHandlers(false, lmc));
        assertTrue(invoker.invokeProtocolHandlers(false, pmc));
    }

    @Test
    public void testHandlerPartitioning() {

        assertEquals(HANDLER_COUNT, invoker.getLogicalHandlers().size());
        for (Handler h : invoker.getLogicalHandlers()) {
            assertTrue(h instanceof LogicalHandler);
        }

        assertEquals(HANDLER_COUNT, invoker.getProtocolHandlers().size());
        for (Handler h : invoker.getProtocolHandlers()) {
            assertTrue(!(h instanceof LogicalHandler));
        }

    }

    @Test
    public void testInvokeHandlersInbound() {

        invoker.setInbound();
        assertTrue(invoker.isInbound());
        checkProtocolHandlersInvoked(false);

        assertEquals(4, invoker.getInvokedHandlers().size());
        assertTrue(invoker.isInbound());

        checkLogicalHandlersInvoked(false, true);
        assertEquals(8, invoker.getInvokedHandlers().size());
        assertTrue(invoker.isInbound());

        assertFalse(invoker.isClosed());
        assertTrue(logicalHandlers[0].getInvokeOrderOfHandleMessage() > logicalHandlers[1]
            .getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage() > protocolHandlers[0]
            .getInvokeOrderOfHandleMessage());
        assertTrue(protocolHandlers[0].getInvokeOrderOfHandleMessage() > protocolHandlers[1]
            .getInvokeOrderOfHandleMessage());
    }

    @Test
    public void testLogicalHandlerReturnFalseOutboundResponseExpected() {

        assertEquals(0, logicalHandlers[0].getHandleMessageCount());
        assertEquals(0, logicalHandlers[1].getHandleMessageCount());
        assertEquals(0, logicalHandlers[2].getHandleMessageCount());

        assertTrue(invoker.isOutbound());

        // invoke the handlers.  when a handler returns false, processing
        // of handlers is stopped and message direction is reversed.
        logicalHandlers[1].setHandleMessageRet(false);
        
        boolean ret = invoker.invokeLogicalHandlers(false, lmc);

        assertEquals(false, ret);
        assertFalse(invoker.isClosed());
        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(0, logicalHandlers[2].getHandleMessageCount());
        assertTrue(invoker.isInbound());

        // the next time invokeHandler is invoked, the 'next' handler is invoked.
        // As message direction has been reversed this means the that the previous
        // one on the list is actually invoked.
        logicalHandlers[1].setHandleMessageRet(true);

        ret = invoker.invokeLogicalHandlers(false, lmc);
        assertTrue(ret);
        assertFalse(invoker.isClosed());
        assertEquals(2, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(0, logicalHandlers[2].getHandleMessageCount());
        assertTrue(invoker.isInbound());
    }

    @Test
    public void testLogicalHandlerInboundProcessingStoppedResponseExpected() {

        assertEquals(0, logicalHandlers[0].getHandleMessageCount());
        assertEquals(0, logicalHandlers[1].getHandleMessageCount());

        invoker.setInbound();

        logicalHandlers[1].setHandleMessageRet(false);
        boolean ret = invoker.invokeLogicalHandlers(false, lmc);
        assertFalse(invoker.isClosed());

        assertEquals(false, ret);
        assertEquals(0, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertTrue(invoker.isOutbound());
    }
    
    @Test
    public void testHandleMessageReturnsFalseOutbound() {
        protocolHandlers[2].setHandleMessageRet(false);

        assertTrue(invoker.isOutbound());
        
        boolean continueProcessing = true;
        invoker.setLogicalMessageContext(lmc);
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
        invoker.setProtocolMessageContext(pmc);
        continueProcessing = invoker.invokeProtocolHandlers(false, pmc);
        
        assertFalse((Boolean)pmc.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY));
        assertFalse((Boolean)lmc.get(LogicalMessageContext.MESSAGE_OUTBOUND_PROPERTY));
        assertTrue(invoker.isInbound());
        assertFalse(continueProcessing);

        protocolHandlers[2].setHandleMessageRet(true);
        invoker.setProtocolMessageContext(pmc);
        continueProcessing = invoker.invokeProtocolHandlers(false, pmc);
        invoker.setLogicalMessageContext(lmc);
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);

        
        assertEquals(2, logicalHandlers[0].getHandleMessageCount());
        assertEquals(2, logicalHandlers[1].getHandleMessageCount());
        assertEquals(2, logicalHandlers[2].getHandleMessageCount());
        assertEquals(2, logicalHandlers[3].getHandleMessageCount());
        assertEquals(2, protocolHandlers[0].getHandleMessageCount());
        assertEquals(2, protocolHandlers[1].getHandleMessageCount());
        assertEquals(1, protocolHandlers[2].getHandleMessageCount());
        assertEquals(0, protocolHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[3].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[2].getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[2].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[1].getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[0].getInvokeOrderOfHandleMessage());
        assertTrue(protocolHandlers[0].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[3].getInvokeOrderOfHandleMessage());
        assertTrue(protocolHandlers[2].getInvokeOrderOfHandleMessage()
                   < protocolHandlers[1].getInvokeOrderOfHandleMessage());
 
        assertEquals(0, logicalHandlers[0].getCloseCount());
        assertEquals(0, logicalHandlers[1].getCloseCount());
        assertEquals(0, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        
        assertEquals(0, protocolHandlers[0].getCloseCount());
        assertEquals(0, protocolHandlers[1].getCloseCount());
        assertEquals(0, protocolHandlers[2].getCloseCount());
        assertEquals(0, protocolHandlers[0].getHandleFaultCount());
        assertEquals(0, protocolHandlers[1].getHandleFaultCount());
        assertEquals(0, protocolHandlers[2].getHandleFaultCount());    
    }
    
    @Test
    public void testHandleMessageThrowsProtocolExceptionOutbound() {
        message = new SoapMessage(message);
        lmc = new LogicalMessageContextImpl(message);
        pmc = new WrappedMessageContext(message);      

        ProtocolException pe = new ProtocolException("banzai");
        protocolHandlers[2].setException(pe);
        
        invoker.setRequestor(true);
        assertTrue(invoker.isOutbound());
        
        boolean continueProcessing = true;
        invoker.setLogicalMessageContext(lmc);
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
        assertTrue(continueProcessing);
        
        //create an empty SOAP body for testing
        try {
            pmc = new SOAPMessageContextImpl(message);
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapMessage = factory.createMessage();      
            ((SOAPMessageContext)pmc).setMessage(soapMessage);            
        } catch (SOAPException e) {
            //do nothing
        }
        
        try {
            invoker.setProtocolMessageContext(pmc);
            continueProcessing = invoker.invokeProtocolHandlers(false, pmc);
            fail("did not get expected exception");
        } catch (ProtocolException e) {
            assertEquals("banzai", e.getMessage());
        }
        
        assertFalse((Boolean)pmc.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY));
        assertFalse((Boolean)lmc.get(LogicalMessageContext.MESSAGE_OUTBOUND_PROPERTY));
        assertTrue(invoker.isInbound());
        
        //the message is replaced by fault message
        Source responseMessage = lmc.getMessage().getPayload();
        //System.out.println(getSourceAsString(responseMessage));
        assertTrue(getSourceAsString(responseMessage).indexOf("banzai") > -1);

        //assertFalse(continueProcessing);

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(1, logicalHandlers[3].getHandleMessageCount());
        assertEquals(1, protocolHandlers[0].getHandleMessageCount());
        assertEquals(1, protocolHandlers[1].getHandleMessageCount());
        assertEquals(1, protocolHandlers[2].getHandleMessageCount());
        assertEquals(0, protocolHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[3].getInvokeOrderOfHandleMessage()
                   < protocolHandlers[0].getInvokeOrderOfHandleMessage());
        assertTrue(protocolHandlers[1].getInvokeOrderOfHandleMessage()
                   < protocolHandlers[2].getInvokeOrderOfHandleMessage());
 
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(1, logicalHandlers[2].getCloseCount());
        assertEquals(1, logicalHandlers[3].getCloseCount());
        
        assertEquals(1, protocolHandlers[0].getCloseCount());
        assertEquals(1, protocolHandlers[1].getCloseCount());
        assertEquals(1, protocolHandlers[2].getCloseCount());
        assertEquals(0, protocolHandlers[3].getCloseCount());
        
        assertTrue(protocolHandlers[2].getInvokeOrderOfClose()
                   < protocolHandlers[1].getInvokeOrderOfClose());   
        assertTrue(protocolHandlers[0].getInvokeOrderOfClose()
                   < logicalHandlers[3].getInvokeOrderOfClose());   
        
        assertEquals(1, logicalHandlers[0].getHandleFaultCount());
        assertEquals(1, logicalHandlers[1].getHandleFaultCount());
        assertEquals(1, logicalHandlers[2].getHandleFaultCount());
        assertEquals(1, logicalHandlers[3].getHandleFaultCount());      

        assertEquals(1, protocolHandlers[0].getHandleFaultCount());
        assertEquals(1, protocolHandlers[1].getHandleFaultCount());
        assertEquals(0, protocolHandlers[2].getHandleFaultCount());    
        assertEquals(0, protocolHandlers[3].getHandleFaultCount());    
        
        assertTrue(protocolHandlers[0].getInvokeOrderOfHandleFault()
                   < logicalHandlers[3].getInvokeOrderOfHandleFault());
        assertTrue(protocolHandlers[2].getInvokeOrderOfHandleFault()
                   < protocolHandlers[1].getInvokeOrderOfHandleFault());
    }
    
    @Test
    public void testHandleFaultReturnsFalseOutbound() {
        ProtocolException pe = new ProtocolException("banzai");
        protocolHandlers[2].setException(pe);
        protocolHandlers[0].setHandleFaultRet(false);
        
        invoker.setRequestor(true);
        assertTrue(invoker.isOutbound());
        
        boolean continueProcessing = true;
        invoker.setLogicalMessageContext(lmc);
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
        assertTrue(continueProcessing);
        
        try {
            invoker.setProtocolMessageContext(pmc);
            continueProcessing = invoker.invokeProtocolHandlers(false, pmc);
            fail("did not get expected exception");
        } catch (ProtocolException e) {
            assertEquals("banzai", e.getMessage());
        }
        
        assertFalse((Boolean)pmc.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY));
        assertFalse((Boolean)lmc.get(LogicalMessageContext.MESSAGE_OUTBOUND_PROPERTY));
        assertTrue(invoker.isInbound());
        //assertFalse(continueProcessing);

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(1, logicalHandlers[3].getHandleMessageCount());
        assertEquals(1, protocolHandlers[0].getHandleMessageCount());
        assertEquals(1, protocolHandlers[1].getHandleMessageCount());
        assertEquals(1, protocolHandlers[2].getHandleMessageCount());
        assertEquals(0, protocolHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[3].getInvokeOrderOfHandleMessage()
                   < protocolHandlers[0].getInvokeOrderOfHandleMessage());
        assertTrue(protocolHandlers[1].getInvokeOrderOfHandleMessage()
                   < protocolHandlers[2].getInvokeOrderOfHandleMessage());
 
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(1, logicalHandlers[2].getCloseCount());
        assertEquals(1, logicalHandlers[3].getCloseCount());
        
        assertEquals(1, protocolHandlers[0].getCloseCount());
        assertEquals(1, protocolHandlers[1].getCloseCount());
        assertEquals(1, protocolHandlers[2].getCloseCount());
        assertEquals(0, protocolHandlers[3].getCloseCount());
        
        assertTrue(protocolHandlers[2].getInvokeOrderOfClose()
                   < protocolHandlers[1].getInvokeOrderOfClose());   
        assertTrue(protocolHandlers[0].getInvokeOrderOfClose()
                   < logicalHandlers[3].getInvokeOrderOfClose());   
        
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[3].getHandleFaultCount());      

        assertEquals(1, protocolHandlers[0].getHandleFaultCount());
        assertEquals(1, protocolHandlers[1].getHandleFaultCount());
        assertEquals(0, protocolHandlers[2].getHandleFaultCount());    
        assertEquals(0, protocolHandlers[3].getHandleFaultCount());    
        
        assertTrue(protocolHandlers[2].getInvokeOrderOfHandleFault()
                   < protocolHandlers[1].getInvokeOrderOfHandleFault());
    }
   
    @Test
    public void testHandleMessageReturnsTrue() {
        assertFalse(invoker.faultRaised());

        logicalHandlers[0].setHandleMessageRet(true);
        logicalHandlers[1].setHandleMessageRet(true);
        logicalHandlers[2].setHandleMessageRet(true);
        logicalHandlers[3].setHandleMessageRet(true);

        boolean continueProcessing = true;
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
        
        assertTrue(continueProcessing);

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(1, logicalHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[0].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[1].getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[2].getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[2].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[3].getInvokeOrderOfHandleMessage());
        assertEquals(0, logicalHandlers[0].getCloseCount());
        assertEquals(0, logicalHandlers[1].getCloseCount());
        assertEquals(0, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[3].getCloseCount());
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[3].getHandleFaultCount());
    }
    
    //JAX-WS 9.3.2.1: 
    //Return false This indicates that normal message processing should cease. Subsequent actions 
    //depend on whether the message exchange pattern (MEP) in use requires a response to the 
    //message currently being processed or not: 
    //Response The message direction is reversed, the runtime invokes handleMessage on the next
    //handler or dispatches the message (see section 9.1.2.2) if there are no further handlers.
    @Test
    public void testHandleMessageReturnsFalseWithResponseExpected() {
        assertFalse(invoker.faultRaised());

        logicalHandlers[0].setHandleMessageRet(true);
        logicalHandlers[1].setHandleMessageRet(true);
        logicalHandlers[2].setHandleMessageRet(false);
        logicalHandlers[3].setHandleMessageRet(true);
        invoker.setResponseExpected(true);
        
        boolean continueProcessing = true;
        invoker.setLogicalMessageContext(lmc);
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
        
        assertFalse(continueProcessing);
        
        assertFalse((Boolean)lmc.get(LogicalMessageContext.MESSAGE_OUTBOUND_PROPERTY));

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());

        logicalHandlers[2].setHandleMessageRet(true);
        invoker.invokeLogicalHandlers(false, lmc);
        
        assertEquals(2, logicalHandlers[0].getHandleMessageCount());
        assertEquals(2, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[3].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[2].getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[2].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[1].getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[0].getInvokeOrderOfHandleMessage());
 
        assertEquals(0, logicalHandlers[0].getCloseCount());
        assertEquals(0, logicalHandlers[1].getCloseCount());
        assertEquals(0, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
    }
 
    //JAX-WS 9.3.2.1: 
    //Return false This indicates that normal message processing should cease. Subsequent actions 
    //depend on whether the message exchange pattern (MEP) in use requires a response to the 
    //message currently being processed or not: 
    //No response Normal message processing stops, close is called on each previously invoked handler
    //in the chain, the message is dispatched
    @Test
    public void testHandleMessageReturnsFalseWithNoResponseExpected() {
        assertFalse(invoker.faultRaised());

        logicalHandlers[0].setHandleMessageRet(true);
        logicalHandlers[1].setHandleMessageRet(true);
        logicalHandlers[2].setHandleMessageRet(false);
        logicalHandlers[3].setHandleMessageRet(true);
        invoker.setResponseExpected(false);
        
        boolean continueProcessing = true;
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
        
        assertFalse(continueProcessing);

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[0].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[1].getInvokeOrderOfHandleMessage());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[2].getInvokeOrderOfHandleMessage());
        
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(1, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[3].getCloseCount());

        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[3].getHandleFaultCount());
    }
    
    //JAX-WS 9.3.2.1:
    //Throw ProtocolException or a subclass This indicates that normal message processing should cease. 
    //Subsequent actions depend on whether the MEP in use requires a response to the message currently 
    //being processed or not:
    //Response Normal message processing stops, fault message processing starts. The message direction 
    //is reversed, if the message is not already a fault message then it is replaced with a fault message, 
    //and the runtime invokes handleFault on the next handler or dispatches the message (see 
    //section 9.1.2.2) if there are no further handlers.
    @Test
    public void testHandleMessageThrowsProtocolExceptionWithResponseExpected() {
        assertFalse(invoker.faultRaised());

        ProtocolException pe = new ProtocolException("banzai");
        logicalHandlers[2].setException(pe);
        
        invoker.setRequestor(true);

        //boolean continueProcessing = true;
        try {
            invoker.setLogicalMessageContext(lmc);
            invoker.invokeLogicalHandlers(false, lmc);
            fail("did not get expected exception");
        } catch (ProtocolException e) {
            assertEquals("banzai", e.getMessage());
        }
        
        assertTrue(invoker.faultRaised());
        //assertFalse(continueProcessing);
        //assertTrue(invoker.isClosed());
        assertSame(pe, invoker.getFault());
                
        assertFalse((Boolean)lmc.get(LogicalMessageContext.MESSAGE_OUTBOUND_PROPERTY));
         
        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[2].getInvokeOrderOfHandleMessage());
        
        assertEquals(1, logicalHandlers[0].getHandleFaultCount());
        assertEquals(1, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleFault()
                   < logicalHandlers[0].getInvokeOrderOfHandleFault());
        
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(1, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[3].getCloseCount());
/*        
        
        continueProcessing = invoker.invokeLogicalHandlers(false, lmc);

        assertFalse(continueProcessing);
        assertTrue(invoker.faultRaised());
        assertTrue(invoker.isClosed());
        assertSame(pe, invoker.getFault());

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());*/
    }

    //JAX-WS 9.3.2.1:
    //Throw ProtocolException or a subclass This indicates that normal message processing should cease. 
    //Subsequent actions depend on whether the MEP in use requires a response to the message currently 
    //being processed or not:
    //No response Normal message processing stops, close is called on each previously invoked handler 
    //in the chain, the exception is dispatched
    @Test
    public void testHandleMessageThrowsProtocolExceptionWithNoResponseExpected() {
        assertFalse(invoker.faultRaised());

        ProtocolException pe = new ProtocolException("banzai");
        logicalHandlers[2].setException(pe);
        invoker.setResponseExpected(false);
        invoker.setRequestor(true);

        //boolean continueProcessing = true;
        try {
            invoker.invokeLogicalHandlers(false, lmc);
            //don't fail.  TCK says this shouldn't be thrown.
            //fail("did not get expected exception");
        } catch (ProtocolException e) {
            assertEquals("banzai", e.getMessage());
        }
        assertTrue(invoker.faultRaised());
        //assertFalse(continueProcessing);
        //assertTrue(invoker.isClosed());
        assertSame(pe, invoker.getFault());

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[2].getInvokeOrderOfHandleMessage());
        
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[3].getHandleFaultCount());
        
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(1, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[3].getCloseCount());
        assertTrue(logicalHandlers[1].getInvokeOrderOfClose()
                   < logicalHandlers[0].getInvokeOrderOfClose());
    }
    
    //Throw any other runtime exception This indicates that normal message processing should cease. 
    //Subsequent actions depend on whether the MEP in use includes a response to the message currently being 
    //processed or not: 
    //Response Normal message processing stops, close is called on each previously invoked handler in 
    //the chain, the message direction is reversed, and the exception is dispatched
    @Test
    public void testHandleMessageThrowsRuntimeExceptionWithResponseExpected() {
        assertFalse(invoker.faultRaised());

        RuntimeException re = new RuntimeException("banzai");
        logicalHandlers[1].setException(re);
        invoker.setRequestor(true);

        //boolean continueProcessing = true;
        try {
            invoker.invokeLogicalHandlers(false, lmc);
            fail("did not get expected exception");
        } catch (RuntimeException e) {
            assertEquals("banzai", e.getMessage());
        }
        
        //assertTrue(invoker.faultRaised());
        //assertFalse(continueProcessing);
        assertTrue(invoker.isClosed());
        //assertSame(re, invoker.getFault());

        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(0, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[0].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[1].getInvokeOrderOfHandleMessage());
        
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[3].getHandleFaultCount());
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(0, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[3].getCloseCount());
    }

    @Test
    public void testFaultRaised() {

        assertFalse(invoker.faultRaised());

        invoker.setFault(new ProtocolException("test exception"));
        assertTrue(invoker.faultRaised());

        // reset
        invoker.setFault(null);
        assertFalse(invoker.faultRaised());

        invoker.setFault(true);
        assertTrue(invoker.faultRaised());

        // reset
        invoker.setFault(false);
        invoker.setFault(null);
        assertFalse(invoker.faultRaised());

        invoker.setFault(true);
        invoker.setFault(new ProtocolException("test exception"));
    }

    // JAXB spec 9.3.2.2: Throw ProtocolException or a subclass This indicates
    // that fault message processing should cease. Fault message processing
    // stops, close is called on each previously invoked handler in the chain, the
    // exception is dispatched
    @Test
    public void testHandleFaultThrowsProtocolException() {
        ProtocolException pe = new ProtocolException("banzai");
        ProtocolException pe2 = new ProtocolException("banzai2");
        // throw exception during handleFault processing
        logicalHandlers[2].setException(pe);
        logicalHandlers[1].setFaultException(pe2);
        invoker.setRequestor(true);

        boolean continueProcessing = false;
        try {
            continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
            fail("did not get expected exception");
        } catch (ProtocolException e) {
            assertEquals("banzai2", e.getMessage());
        }      
 
        assertFalse(continueProcessing);
        assertTrue(invoker.isClosed());
        
        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleMessage()
                   < logicalHandlers[2].getInvokeOrderOfHandleMessage());

        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(1, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());

        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(1, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[3].getCloseCount());
        assertTrue(logicalHandlers[2].getInvokeOrderOfClose()
                   < logicalHandlers[1].getInvokeOrderOfClose());
        assertTrue(logicalHandlers[1].getInvokeOrderOfClose()
                   < logicalHandlers[0].getInvokeOrderOfClose());
        
    }

    // JAXB spec 9.3.2.2: Throw any other runtime exception This indicates
    // that fault message processing should cease. Fault message processing stops,
    // close is called on each previously invoked handler in the chain, the exception is
    // dispatched
    @Test
    public void testHandleFaultThrowsRuntimeException() {
        ProtocolException pe = new ProtocolException("banzai");
        RuntimeException re = new RuntimeException("banzai");
        // throw exception during handleFault processing
        logicalHandlers[2].setException(pe);
        logicalHandlers[1].setFaultException(re);
        invoker.setRequestor(true);


        boolean continueProcessing = false;
        try {
            continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
            fail("did not get expected exception");
        } catch (RuntimeException e) {
            assertEquals("banzai", e.getMessage());
        } 
        
        assertFalse(continueProcessing);
        assertTrue(invoker.isClosed());
        
        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
 
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(1, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());
        assertEquals(1, logicalHandlers[2].getCloseCount());
        assertEquals(0, logicalHandlers[3].getCloseCount());
        assertTrue(logicalHandlers[2].getInvokeOrderOfClose()
                   < logicalHandlers[1].getInvokeOrderOfClose());
        assertTrue(logicalHandlers[1].getInvokeOrderOfClose()
                   < logicalHandlers[0].getInvokeOrderOfClose()); 
    }
    
    
    //JAXB spec 9.3.2.2: Return true This indicates that fault message processing 
    //should continue. The runtime invokes handle Fault on the next handler or dispatches 
    //the fault message (see section 9.1.2.2) if there are no further handlers.
    @Test
    public void testHandleFaultReturnsTrue() {
        ProtocolException pe = new ProtocolException("banzai");
        logicalHandlers[2].setException(pe);
        invoker.setRequestor(true);

        logicalHandlers[0].setHandleFaultRet(true);
        logicalHandlers[1].setHandleFaultRet(true);
        logicalHandlers[2].setHandleFaultRet(true);
        logicalHandlers[3].setHandleFaultRet(true);
        
        boolean continueProcessing = false;
        try {
            continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
            fail("did not get expected exception");
        } catch (RuntimeException e) {
            assertEquals("banzai", e.getMessage());
        }        
        
        assertFalse(continueProcessing);
        //assertTrue(invoker.isClosed());
        
        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(0, logicalHandlers[3].getHandleMessageCount());
        
        assertEquals(1, logicalHandlers[0].getHandleFaultCount());
        assertEquals(1, logicalHandlers[1].getHandleFaultCount());
        assertEquals(0, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[3].getHandleFaultCount());
        assertTrue(logicalHandlers[1].getInvokeOrderOfHandleFault()
                   < logicalHandlers[0].getInvokeOrderOfHandleFault());

        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());    
        assertEquals(1, logicalHandlers[2].getCloseCount());    
        assertEquals(0, logicalHandlers[3].getCloseCount());   
    }

    
    //JAXB spec 9.3.2.2: Return false This indicates that fault message processing 
    //should cease. Fault message processing stops, close is called on each previously invoked
    //handler in the chain, the fault message is dispatched
    @Test
    public void testHandleFaultReturnsFalse() {
        ProtocolException pe = new ProtocolException("banzai");
        logicalHandlers[3].setException(pe);
        invoker.setRequestor(true);

        logicalHandlers[0].setHandleFaultRet(true);
        logicalHandlers[1].setHandleFaultRet(true);
        logicalHandlers[2].setHandleFaultRet(false);
        logicalHandlers[3].setHandleFaultRet(true);
        
        boolean continueProcessing = false;
        try {
            continueProcessing = invoker.invokeLogicalHandlers(false, lmc);
            fail("did not get expected exception");
        } catch (RuntimeException e) {
            assertEquals("banzai", e.getMessage());
        }    
        
        assertFalse(continueProcessing);
        //assertTrue(invoker.isClosed());
        
        assertEquals(1, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(1, logicalHandlers[2].getHandleMessageCount());
        assertEquals(1, logicalHandlers[3].getHandleMessageCount());
        
        
        assertEquals(0, logicalHandlers[0].getHandleFaultCount());
        assertEquals(0, logicalHandlers[1].getHandleFaultCount());
        assertEquals(1, logicalHandlers[2].getHandleFaultCount());
        assertEquals(0, logicalHandlers[3].getHandleFaultCount());
 
        assertEquals(1, logicalHandlers[0].getCloseCount());
        assertEquals(1, logicalHandlers[1].getCloseCount());    
        assertEquals(1, logicalHandlers[2].getCloseCount());    
        assertEquals(1, logicalHandlers[3].getCloseCount());   
        
        assertTrue(logicalHandlers[3].getInvokeOrderOfClose()
                   < logicalHandlers[2].getInvokeOrderOfClose());
        assertTrue(logicalHandlers[2].getInvokeOrderOfClose()
                   < logicalHandlers[1].getInvokeOrderOfClose());
        assertTrue(logicalHandlers[1].getInvokeOrderOfClose()
                   < logicalHandlers[0].getInvokeOrderOfClose());
    }
    
    @Test
    public void testMEPComplete() {

        invoker.invokeLogicalHandlers(false, lmc);
        invoker.invokeProtocolHandlers(false, pmc);
        assertEquals(8, invoker.getInvokedHandlers().size());

        invoker.mepComplete(message);

        assertTrue("close not invoked on logicalHandlers", logicalHandlers[0].isCloseInvoked());
        assertTrue("close not invoked on logicalHandlers", logicalHandlers[1].isCloseInvoked());
        assertTrue("close not invoked on protocolHandlers", protocolHandlers[0].isCloseInvoked());
        assertTrue("close not invoked on protocolHandlers", protocolHandlers[1].isCloseInvoked());

        assertTrue("incorrect invocation order of close", protocolHandlers[1].getInvokeOrderOfClose()
                   < protocolHandlers[0].getInvokeOrderOfClose());
        assertTrue("incorrect invocation order of close", protocolHandlers[0].getInvokeOrderOfClose()
                   < logicalHandlers[1].getInvokeOrderOfClose());
        assertTrue("incorrect invocation order of close", logicalHandlers[1].getInvokeOrderOfClose()
                   < logicalHandlers[0].getInvokeOrderOfClose());
    }


    @Test
    public void testResponseExpectedDefault() {
        assertTrue(invoker.isResponseExpected());
    }

    /* test invoking logical handlers when processing has been aborted
     * with both protocol and logical handlers in invokedHandlers list.
     *
     */
    @Test
    public void testInvokedAlreadyInvokedMixed() {

        // simulate an invocation being aborted by a logical handler
        //
        logicalHandlers[1].setHandleMessageRet(false);
        invoker.setInbound();
        invoker.invokeProtocolHandlers(true, pmc);
        invoker.invokeLogicalHandlers(true, lmc);

        assertEquals(7, invoker.getInvokedHandlers().size());
//        assertTrue(!invoker.getInvokedHandlers().contains(logicalHandlers[1]));
        assertTrue(invoker.getInvokedHandlers().contains(protocolHandlers[0]));
        assertTrue(invoker.getInvokedHandlers().contains(protocolHandlers[1]));
        assertEquals(0, logicalHandlers[0].getHandleMessageCount());
        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        
        logicalHandlers[1].setHandleMessageRet(true);
        invoker.invokeLogicalHandlers(true, lmc);
        invoker.invokeProtocolHandlers(true, pmc);
        
        
        assertEquals(2, protocolHandlers[0].getHandleMessageCount());
        assertEquals(2, protocolHandlers[1].getHandleMessageCount());

        assertEquals(1, logicalHandlers[1].getHandleMessageCount());
        assertEquals(0, logicalHandlers[0].getHandleMessageCount());
        assertEquals(2, protocolHandlers[0].getHandleMessageCount());
        assertEquals(2, protocolHandlers[1].getHandleMessageCount());

    }

    /*public void testHandlerReturnFalse() {
        logicalHandlers[1].setHandleMessageRet(false);
        invoker.setInbound();
        doInvokeProtocolHandlers(true);
        invoker.invokeLogicalHandlers(true, lmc);

    }*/

    protected void checkLogicalHandlersInvoked(boolean outboundProperty, boolean requestorProperty) {

        invoker.invokeLogicalHandlers(requestorProperty, lmc);

        assertTrue("handler not invoked", logicalHandlers[0].isHandleMessageInvoked());
        assertTrue("handler not invoked", logicalHandlers[1].isHandleMessageInvoked());
        assertTrue(invoker.getInvokedHandlers().contains(logicalHandlers[0]));
        assertTrue(invoker.getInvokedHandlers().contains(logicalHandlers[1]));
    }

    protected void checkProtocolHandlersInvoked(boolean outboundProperty) {

        invoker.invokeProtocolHandlers(false, pmc);

        assertTrue("handler not invoked", protocolHandlers[0].isHandleMessageInvoked());
        assertTrue("handler not invoked", protocolHandlers[1].isHandleMessageInvoked());

        assertTrue(invoker.getInvokedHandlers().contains(protocolHandlers[0]));
        assertTrue(invoker.getInvokedHandlers().contains(protocolHandlers[1]));
    }
    
    private String getSourceAsString(Source s) {
        String result = "";

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            OutputStream out = new ByteArrayOutputStream();
            StreamResult streamResult = new StreamResult();
            streamResult.setOutputStream(out);
            transformer.transform(s, streamResult);
            return streamResult.getOutputStream().toString();
        } catch (Exception e) {
            //do nothing
        }
        return result;
    }
    
    static class TestProtocolHandler extends AbstractHandlerBase<SOAPMessageContext> {

    }

    static class TestLogicalHandler extends AbstractHandlerBase<LogicalMessageContextImpl>
        implements LogicalHandler<LogicalMessageContextImpl> {

    }

    static class AbstractHandlerBase<T extends MessageContext> implements Handler<T> {

        private static int sinvokedOrder;
        private static int sid;

        private int invokeOrderOfHandleMessage;
        private int invokeOrderOfHandleFault;
        private int invokeOrderOfClose;
        private final int id = ++sid;

        private int handleMessageInvoked;
        private int handleFaultInvoked;
        private boolean handleMessageRet = true;
        private boolean handleFaultRet = true;
        private RuntimeException exception;
        private RuntimeException faultException;

        private int closeInvoked;

        public void reset() {
            handleMessageInvoked = 0;
            handleFaultInvoked = 0;
            handleMessageRet = true;
        }

        public boolean handleMessage(T arg0) {
            invokeOrderOfHandleMessage = ++sinvokedOrder;
            handleMessageInvoked++;

            if (exception != null) {
                RuntimeException e = exception;
                exception = null;
                throw e;
            }

            return handleMessageRet;
        }

        public boolean handleFault(T arg0) {
            invokeOrderOfHandleFault = ++sinvokedOrder;
            handleFaultInvoked++;

            if (faultException != null) {
                throw faultException;
            }

            return handleFaultRet;
        }

        public void close(MessageContext arg0) {
            invokeOrderOfClose = ++sinvokedOrder;
            closeInvoked++;
        }


        public void init(Map<String, Object> arg0) {
            // TODO Auto-generated method stub
        }


        public void destroy() {
            // TODO Auto-generated method stub
        }

        public int getHandleMessageCount() {
            return handleMessageInvoked;
        }

        public int getHandleFaultCount() {
            return handleFaultInvoked;
        }

        public boolean isHandleMessageInvoked() {
            return handleMessageInvoked > 0;
        }

        public boolean isCloseInvoked() {
            return closeInvoked > 0;
        }

        public int getCloseCount() {
            return closeInvoked;
        }

        public int getInvokeOrderOfHandleMessage() {
            return invokeOrderOfHandleMessage;
        }
        
        public int getInvokeOrderOfHandleFault() {
            return invokeOrderOfHandleFault;
        }
        
        public int getInvokeOrderOfClose() {
            return invokeOrderOfClose;
        }  

        public void setHandleMessageRet(boolean ret) {
            handleMessageRet = ret;
        }

        public void setHandleFaultRet(boolean ret) {
            handleFaultRet = ret;
        }
        
        public String toString() {
            return "[" + super.toString() + " id: " + id + " invoke order: " + invokeOrderOfHandleMessage
                   + "]";
        }

        public void setException(RuntimeException rte) {
            exception = rte;
        }
        
        public void setFaultException(RuntimeException rte) {
            faultException = rte;
        }
        
        public static void clear() {
            sinvokedOrder = 0;
            sid = 0;
        }
    }

}
