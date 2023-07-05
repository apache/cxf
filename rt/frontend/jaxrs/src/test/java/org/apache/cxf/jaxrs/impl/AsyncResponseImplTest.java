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
package org.apache.cxf.jaxrs.impl;

import java.util.Date;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.AsyncResponse;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.Servlet3ContinuationProvider;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AsyncResponseImplTest {
    /**
     * According to the spec, subsequent calls to cancel the same AsyncResponse should
     * have the same behavior as the first call.
     */
    @Test
    public void testCancelBehavesTheSameWhenInvokedMultipleTimes() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        AsyncContext asyncCtx = mock(AsyncContext.class);
        Message msg = new MessageImpl();
        msg.setExchange(new ExchangeImpl());
        msg.put(ContinuationProvider.class.getName(), new Servlet3ContinuationProvider(req, resp, msg));

        when(req.startAsync()).thenReturn(asyncCtx);

        AsyncResponse impl = new AsyncResponseImpl(msg);

        // cancel the AsyncResponse for the first time
        assertTrue("Unexpectedly returned false when canceling the first time", impl.cancel());

        // check the state of the AsyncResponse
        assertTrue("AsyncResponse was canceled but is reporting that it was not canceled", impl.isCancelled());
        boolean isDone = impl.isDone();
        boolean isSuspended = impl.isSuspended();

        // cancel the AsyncResponse a second time
        assertTrue("Unexpectedly returned false when canceling the second time", impl.cancel());

        // verify that the state is the same as before the second cancel
        assertTrue("AsyncResponse was canceled (twice) but is reporting that it was not canceled", impl.isCancelled());
        assertEquals("AsynchResponse.isDone() returned a different response after canceling a second time",
                     isDone, impl.isDone());
        assertEquals("AsynchResponse.isSuspended() returned a different response after canceling a second time",
                     isSuspended, impl.isSuspended());
    }

    /**
     * Similar to testCancelBehavesTheSameWhenInvokedMultipleTimes, but using the cancel(int) signature.
     */
    @Test
    public void testCancelIntBehavesTheSameWhenInvokedMultipleTimes() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        AsyncContext asyncCtx = mock(AsyncContext.class);
        Message msg = new MessageImpl();
        msg.setExchange(new ExchangeImpl());
        msg.put(ContinuationProvider.class.getName(), new Servlet3ContinuationProvider(req, resp, msg));

        when(req.startAsync()).thenReturn(asyncCtx);

        AsyncResponse impl = new AsyncResponseImpl(msg);

        // cancel the AsyncResponse for the first time
        assertTrue("Unexpectedly returned false when canceling the first time", impl.cancel(10));

        // check the state of the AsyncResponse
        assertTrue("AsyncResponse was canceled but is reporting that it was not canceled", impl.isCancelled());
        boolean isDone = impl.isDone();
        boolean isSuspended = impl.isSuspended();

        // cancel the AsyncResponse a second time
        assertTrue("Unexpectedly returned false when canceling the second time", impl.cancel(25));

        // verify that the state is the same as before the second cancel
        assertTrue("AsyncResponse was canceled (twice) but is reporting that it was not canceled", impl.isCancelled());
        assertEquals("AsynchResponse.isDone() returned a different response after canceling a second time",
                     isDone, impl.isDone());
        assertEquals("AsynchResponse.isSuspended() returned a different response after canceling a second time",
                     isSuspended, impl.isSuspended());
    }

    /**
     * Similar to testCancelBehavesTheSameWhenInvokedMultipleTimes, but using the cancel(Date) signature.
     */
    @Test
    public void testCancelDateBehavesTheSameWhenInvokedMultipleTimes() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        AsyncContext asyncCtx = mock(AsyncContext.class);
        Message msg = new MessageImpl();
        msg.setExchange(new ExchangeImpl());
        msg.put(ContinuationProvider.class.getName(), new Servlet3ContinuationProvider(req, resp, msg));

        when(req.startAsync()).thenReturn(asyncCtx);

        AsyncResponse impl = new AsyncResponseImpl(msg);

        // cancel the AsyncResponse for the first time
        Date d = new Date(System.currentTimeMillis() + 60000);
        assertTrue("Unexpectedly returned false when canceling the first time", impl.cancel(d));

        // check the state of the AsyncResponse
        assertTrue("AsyncResponse was canceled but is reporting that it was not canceled", impl.isCancelled());
        boolean isDone = impl.isDone();
        boolean isSuspended = impl.isSuspended();

        // cancel the AsyncResponse a second time
        d = new Date(System.currentTimeMillis() + 120000);
        assertTrue("Unexpectedly returned false when canceling the second time", impl.cancel(d));

        // verify that the state is the same as before the second cancel
        assertTrue("AsyncResponse was canceled (twice) but is reporting that it was not canceled", impl.isCancelled());
        assertEquals("AsynchResponse.isDone() returned a different response after canceling a second time",
                     isDone, impl.isDone());
        assertEquals("AsynchResponse.isSuspended() returned a different response after canceling a second time",
                     isSuspended, impl.isSuspended());
    }
    
    /**
     * Test that creatinging an AsyncResponse with a null continuation throws
     * an IllegalArgumentException instead of a NullPointer Exception.
     */
    @Test
    public void testNullContinutaion() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        AsyncContext asyncCtx = mock(AsyncContext.class);
        Message msg = new MessageImpl();
        msg.setExchange(new ExchangeImpl());

        when(req.startAsync()).thenReturn(asyncCtx);

        AsyncResponse impl;
        try {
            impl = new AsyncResponseImpl(msg);
        } catch (IllegalArgumentException e) {
            assertEquals("Continuation not supported. " 
                             + "Please ensure that all servlets and servlet filters support async operations",
                         e.getMessage());
            return;
        }
        Assert.fail("Expected IllegalArgumentException, but instead got valid AsyncResponse, " + impl);
    }
}