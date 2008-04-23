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

import javax.resource.NotSupportedException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.security.auth.Subject;


import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ManagedConnectionImplTest extends Assert {
    private DummyManagedConnectionImpl mc;
    
    @Before
    public void setUp() throws Exception {
        mc = new DummyManagedConnectionImpl(null, null, null);
    }

    @Test
    public void testGetSetLogWriter() throws Exception {
        PrintWriter writer = EasyMock.createMock(PrintWriter.class);
        mc.setLogWriter(writer);
        assertTrue(mc.getLogWriter() == writer);
        writer.close();
        EasyMock.expectLastCall();
        EasyMock.replay(writer);        
        mc.destroy();
        EasyMock.verify(writer);

    }

    @Test
    public void testSetNullLogWriterOk() throws Exception {
        mc.setLogWriter(null);
    }

    @Test
    public void testRemoveConnectionEventListener() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);

        ConnectionEventListener listener = EasyMock.createMock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        listener.connectionErrorOccurred(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(listener);
        mc.sendEvent(event);
        EasyMock.verify(listener);

        mc.removeConnectionEventListener(listener);
        mc.sendEvent(event);

    }

    @Test
    public void testCleanupDoesNothing() throws Exception {
        mc.cleanup();
    }

    @Test
    public void testGetMetaData() throws Exception {
        try {
            mc.getMetaData();            
        } catch (NotSupportedException expected) {
            fail("Got the Exception here");
        }
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
        ConnectionEventListener listener = EasyMock.createMock(ConnectionEventListener.class);
        listener.connectionClosed(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(listener);
        mc.addConnectionEventListener(listener);
        mc.close(o);
        EasyMock.verify(listener);
    }

    @Test
    public void testError() throws Exception {
        ConnectionEventListener listener = EasyMock.createMock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        listener.connectionErrorOccurred(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(listener);
        mc.setLogWriter(null);
        mc.error(new Exception());
        EasyMock.verify(listener);
    }

    @Test
    public void testSendEventError() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
        ConnectionEventListener listener = EasyMock.createMock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        listener.connectionErrorOccurred(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(listener);
        mc.sendEvent(event);
        EasyMock.verify(listener);
    }

    @Test
    public void testSendEventTxStarted() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.LOCAL_TRANSACTION_STARTED);
        ConnectionEventListener listener = EasyMock.createMock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        listener.localTransactionStarted(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(listener);
        mc.sendEvent(event);
        EasyMock.verify(listener);
    }

    @Test
    public void testSendEventTxCommitted() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
        ConnectionEventListener listener = EasyMock.createMock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        listener.localTransactionCommitted(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(listener);
        mc.sendEvent(event);
        EasyMock.verify(listener);
    }

    @Test
    public void testSendEventTxRolledBack() throws Exception {
        ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
        ConnectionEventListener listener = EasyMock.createMock(ConnectionEventListener.class);
        mc.addConnectionEventListener(listener);
        EasyMock.reset(listener);
        listener.localTransactionRolledback(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(listener);
        mc.sendEvent(event);
    }
}
