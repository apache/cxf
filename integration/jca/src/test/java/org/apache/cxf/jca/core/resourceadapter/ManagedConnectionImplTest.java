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
package org.apache.cxf.jca.core.resourceadapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ManagedConnectionImplTest {
    private DummyManagedConnectionImpl mc;

    @Before
    public void setUp() throws Exception {
        mc = new DummyManagedConnectionImpl(null, null, null);
    }

    @Test
    public void testGetSetLogWriter() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        PrintWriter writer = new PrintWriter(new StringWriter()) {
            @Override
            public void close() {
                super.close();
                closed.set(true);
            }
        };
        mc.setLogWriter(writer);
        assertSame(writer, mc.getLogWriter());

        mc.destroy();
        assertTrue(closed.get());
    }

    @Test
    public void testSetNullLogWriterOk() throws Exception {
        mc.setLogWriter(null);
    }

    @Test
    public void testRemoveConnectionEventListener() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);

        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        mc.sendEvent(event);
        verify(listener, times(1)).connectionErrorOccurred(isA(ConnectionEvent.class));

        mc.removeConnectionEventListener(listener);
        mc.sendEvent(event);
    }

    @Test
    public void testCleanupDoesNothing() throws Exception {
        mc.cleanup();
    }

    @Test
    public void testGetMetaData() throws Exception {
        mc.getMetaData();
    }

    @Test
    public void testGetSetSubject() {
        Subject s = new Subject();
        mc.setSubject(s);
        assertEquals("Got back what we set", s, mc.getSubject());
    }

    @Test
    public void testGetSetConnectionRequestInfo() {
        ConnectionRequestInfo ri = new ConnectionRequestInfo() {
        };

        mc.setConnectionRequestInfo(ri);
        assertEquals("Got back what we set", ri, mc.getConnectionRequestInfo());
    }

    @Test
    public void testClose() throws Exception {
        final Object o = new Object();
        ConnectionEventListener listener = mock(ConnectionEventListener.class);

        mc.addConnectionEventListener(listener);
        mc.close(o);

        verify(listener, times(1)).connectionClosed(isA(ConnectionEvent.class));
    }

    @Test
    public void testError() throws Exception {
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);

        mc.setLogWriter(null);
        mc.error(new Exception());

        verify(listener, times(1)).connectionErrorOccurred(isA(ConnectionEvent.class));
    }

    @Test
    public void testSendEventError() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        mc.sendEvent(event);

        verify(listener, times(1)).connectionErrorOccurred(isA(ConnectionEvent.class));
    }

    @Test
    public void testSendEventTxStarted() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.LOCAL_TRANSACTION_STARTED);
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);

        mc.sendEvent(event);
        verify(listener, times(1)).localTransactionStarted(isA(ConnectionEvent.class));
    }

    @Test
    public void testSendEventTxCommitted() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);

        mc.sendEvent(event);
        verify(listener, times(1)).localTransactionCommitted(isA(ConnectionEvent.class));
    }

    @Test
    public void testSendEventTxRolledBack() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);

        mc.sendEvent(event);

        verify(listener, times(1)).localTransactionRolledback(isA(ConnectionEvent.class));
    }
}