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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.LogicalHandler;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.handler.types.CString;
import org.apache.cxf.jaxws.handler.types.FullyQualifiedClassType;
import org.apache.cxf.jaxws.handler.types.ParamValueType;
import org.apache.cxf.jaxws.handler.types.PortComponentHandlerType;
import org.apache.cxf.jaxws.handler.types.XsdStringType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@SuppressWarnings("rawtypes")
public class HandlerChainBuilderTest {

    Handler[] allHandlers = {mock(LogicalHandler.class), mock(Handler.class),
                             mock(Handler.class), mock(LogicalHandler.class)};
    Handler[] logicalHandlers = {allHandlers[0], allHandlers[3]};
    Handler[] protocolHandlers = {allHandlers[1], allHandlers[2]};

    HandlerChainBuilder builder = new HandlerChainBuilder(mock(Bus.class));


    @Test
    public void testChainSorting() {
        List<Handler> sortedHandlerChain = builder.sortHandlers(Arrays.asList(allHandlers));
        assertSame(logicalHandlers[0], sortedHandlerChain.get(0));
        assertSame(logicalHandlers[1], sortedHandlerChain.get(1));
        assertSame(protocolHandlers[0], sortedHandlerChain.get(2));
        assertSame(protocolHandlers[1], sortedHandlerChain.get(3));
    }

    @Test
    public void testBuildHandlerChainFromConfiguration() {

        List<PortComponentHandlerType> hc = createHandlerChainType();
        List<Handler> chain = builder.buildHandlerChainFromConfiguration(hc);

        assertNotNull(chain);
        assertEquals(4, chain.size());
        assertEquals(TestLogicalHandler.class, chain.get(0).getClass());
        assertEquals(TestLogicalHandler.class, chain.get(1).getClass());
        assertEquals(TestProtocolHandler.class, chain.get(2).getClass());
        assertEquals(TestProtocolHandler.class, chain.get(3).getClass());

        TestLogicalHandler tlh = (TestLogicalHandler)chain.get(0);
        assertFalse(tlh.initCalled);
        assertNull(tlh.config);
    }

    @Test
    public void testBuilderCallsInit() {
        List<PortComponentHandlerType> hc = createHandlerChainType();
        hc.remove(3);
        hc.remove(2);
        hc.remove(1);

        PortComponentHandlerType h = hc.get(0);
        List<ParamValueType> params = h.getInitParam();

        ParamValueType p = new ParamValueType();
        CString pName = new CString();
        pName.setValue("foo");
        p.setParamName(pName);
        XsdStringType pValue = new XsdStringType();
        pValue.setValue("1");
        p.setParamValue(pValue);
        params.add(p);

        p = new ParamValueType();
        pName = new CString();
        pName.setValue("bar");
        p.setParamName(pName);
        pValue = new XsdStringType();
        pValue.setValue("2");
        p.setParamValue(pValue);
        params.add(p);

        List<Handler> chain = builder.buildHandlerChainFromConfiguration(hc);
        assertEquals(1, chain.size());
        TestLogicalHandler tlh = (TestLogicalHandler)chain.get(0);

        assertTrue(tlh.initCalled);
        Map cfg = tlh.config;
        assertNotNull(tlh.config);

        assertEquals(2, cfg.keySet().size());
        assertEquals("1", cfg.get("foo"));
        assertEquals("2", cfg.get("bar"));
    }

    @Test
    public void testBuilderCallsInitWithNoInitParamValues() {
        List<PortComponentHandlerType> hc = createHandlerChainType();
        hc.remove(3);
        hc.remove(2);
        hc.remove(1);

        PortComponentHandlerType h = hc.get(0);
        List<ParamValueType> params = h.getInitParam();

        ParamValueType p = new ParamValueType();
        CString pName = new CString();
        pName.setValue("foo");
        p.setParamName(pName);
        params.add(p);

        List<Handler> chain = builder.buildHandlerChainFromConfiguration(hc);
        assertEquals(1, chain.size());
        TestLogicalHandler tlh = (TestLogicalHandler)chain.get(0);

        assertTrue(tlh.initCalled);
        Map cfg = tlh.config;
        assertNotNull(tlh.config);
        assertEquals(1, cfg.keySet().size());
    }

    @Test
    public void testBuilderCannotLoadHandlerClass() {
        List<PortComponentHandlerType> hc = createHandlerChainType();
        hc.remove(3);
        hc.remove(2);
        hc.remove(1);
        FullyQualifiedClassType type = new FullyQualifiedClassType();
        type.setValue("no.such.class");
        hc.get(0).setHandlerClass(type);

        try {
            builder.buildHandlerChainFromConfiguration(hc);
            fail("did not get expected exception");
        } catch (WebServiceException ex) {
            // ex.printStackTrace();
            assertNotNull(ex.getCause());
            assertEquals(ClassNotFoundException.class, ex.getCause().getClass());
        }
    }

    private List<PortComponentHandlerType> createHandlerChainType() {
        List<PortComponentHandlerType> handlers = new ArrayList<>();

        PortComponentHandlerType h = new PortComponentHandlerType();
        CString name = new CString();
        name.setValue("lh1");
        h.setHandlerName(name);
        FullyQualifiedClassType type = new FullyQualifiedClassType();
        type.setValue(TestLogicalHandler.class.getName());
        h.setHandlerClass(type);
        handlers.add(h);

        h = new PortComponentHandlerType();
        name = new CString();
        name.setValue("ph1");
        h.setHandlerName(name);
        type = new FullyQualifiedClassType();
        type.setValue(TestProtocolHandler.class.getName());
        h.setHandlerClass(type);
        handlers.add(h);

        h = new PortComponentHandlerType();
        name = new CString();
        name.setValue("ph2");
        h.setHandlerName(name);
        type = new FullyQualifiedClassType();
        type.setValue(TestProtocolHandler.class.getName());
        h.setHandlerClass(type);
        handlers.add(h);

        h = new PortComponentHandlerType();
        name = new CString();
        name.setValue("lh2");
        h.setHandlerName(name);
        type = new FullyQualifiedClassType();
        type.setValue(TestLogicalHandler.class.getName());
        h.setHandlerClass(type);
        handlers.add(h);

        return handlers;
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
}
