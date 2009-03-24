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

import java.util.List;
import java.util.Map;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.MessageContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class AnnotationHandlerChainBuilderTest extends Assert {

    @Before
    public void setUp() {
    }

    @Test
    public void testFindHandlerChainAnnotation() {
        HandlerTestImpl handlerTestImpl = new HandlerTestImpl();
        AnnotationHandlerChainBuilder chainBuilder = new AnnotationHandlerChainBuilder();
        List<Handler> handlers = chainBuilder
            .buildHandlerChainFromClass(handlerTestImpl.getClass(), 
                                        null, 
                                        null, 
                                        null);
        assertNotNull(handlers);
        assertEquals(9, handlers.size());
        assertEquals(TestLogicalHandler.class, handlers.get(0).getClass());
        assertEquals(TestLogicalHandler.class, handlers.get(1).getClass());
        assertEquals(TestLogicalHandler.class, handlers.get(2).getClass());
        assertEquals(TestLogicalHandler.class, handlers.get(3).getClass());
        assertEquals(TestLogicalHandler.class, handlers.get(4).getClass());
        assertEquals(TestLogicalHandler.class, handlers.get(5).getClass());
        assertEquals(TestLogicalHandler.class, handlers.get(6).getClass());
        assertEquals(TestProtocolHandler.class, handlers.get(7).getClass());
        assertEquals(TestProtocolHandler.class, handlers.get(8).getClass());
    }    
    
    @Test
    public void testFindHandlerChainAnnotationPerPortServiceBinding() {
        HandlerTestImpl handlerTestImpl = new HandlerTestImpl();
        AnnotationHandlerChainBuilder chainBuilder = new AnnotationHandlerChainBuilder();
        QName portQName = new QName("namespacedoesntsupportyet", "SoapPort1");
        QName serviceQName = new QName("namespacedoesntsupportyet", "SoapService1");
        String bindingID = "http://schemas.xmlsoap.org/wsdl/soap/http";
        List<Handler> handlers = chainBuilder
            .buildHandlerChainFromClass(handlerTestImpl.getClass(), portQName, serviceQName, bindingID);
        assertNotNull(handlers);
        assertEquals(5, handlers.size());
    }
    
    @Test
    public void testFindHandlerChainAnnotationPerPortServiceBindingNegative() {
        HandlerTestImpl handlerTestImpl = new HandlerTestImpl();
        AnnotationHandlerChainBuilder chainBuilder = new AnnotationHandlerChainBuilder();
        QName portQName = new QName("namespacedoesntsupportyet", "SoapPortUnknown");
        QName serviceQName = new QName("namespacedoesntsupportyet", "SoapServiceUnknown");
        String bindingID = "BindingUnknow";
        List<Handler> handlers = chainBuilder
            .buildHandlerChainFromClass(handlerTestImpl.getClass(), portQName, serviceQName, bindingID);
        assertNotNull(handlers);
        assertEquals(3, handlers.size());
    }
    
    @Test
    public void testFindHandlerChainAnnotationPerPortServiceBindingWildcard() {
        HandlerTestImpl handlerTestImpl = new HandlerTestImpl();
        AnnotationHandlerChainBuilder chainBuilder = new AnnotationHandlerChainBuilder();
        QName portQName = new QName("http://apache.org/handler_test", "SoapPortWildcard");
        QName serviceQName = new QName("http://apache.org/handler_test", "SoapServiceWildcard");
        String bindingID = "BindingUnknow";
        List<Handler> handlers = chainBuilder
            .buildHandlerChainFromClass(handlerTestImpl.getClass(), portQName, serviceQName, bindingID);
        assertNotNull(handlers);
        assertEquals(7, handlers.size());
    }
    
    public static class TestLogicalHandler implements LogicalHandler {
        Map config;
        boolean initCalled;

        public void close(MessageContext arg0) {
        }

        public boolean handleFault(MessageContext arg0) {
            return false;
        }

        public boolean handleMessage(MessageContext arg0) {
            return false;
        }

        public final void init(final Map map) {
            config = map;
            initCalled = true;
        }
    }

    public static class TestProtocolHandler implements Handler {

        public void close(MessageContext arg0) {
        }

        public boolean handleFault(MessageContext arg0) {
            return false;
        }

        public boolean handleMessage(MessageContext arg0) {
            return false;
        }
    }

    @WebService()
    @HandlerChain(file = "./handlers.xml", name = "TestHandlerChain")
    public class HandlerTestImpl {

    }

}
