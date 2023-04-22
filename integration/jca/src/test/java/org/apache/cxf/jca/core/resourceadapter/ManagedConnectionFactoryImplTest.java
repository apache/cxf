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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ResourceAdapterInternalException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ManagedConnectionFactoryImplTest {
    DummyManagedConnectionFactoryImpl mcf = new DummyManagedConnectionFactoryImpl();


    @Test
    public void testManagedConnectionFactoryImplInstanceOfResourceBean() throws Exception {
        assertNotNull("mcf is not null", mcf);
        assertTrue("ManagedConnectionFactoryImpl is ResourceBean", mcf instanceof ResourceBean);
    }

    @Test
    public void testMatchConnectionSameConnectioRequestInfoNotBound() throws Exception {
        Subject subject = null;
        Set<AbstractManagedConnectionImpl> connectionSet = new HashSet<>();
        ConnectionRequestInfo cri = new DummyConnectionRequestInfo();
        DummyManagedConnectionImpl con1 = new DummyManagedConnectionImpl(mcf, cri, subject);
        connectionSet.add(con1);

        ManagedConnection mcon = mcf.matchManagedConnections(connectionSet, subject, cri);
        assertEquals(con1, mcon);
    }

    @Test
    public void testMatchConnectionSameConnectioRequestInfoBound() throws Exception {
        Subject subject = null;
        Set<AbstractManagedConnectionImpl> connectionSet = new HashSet<>();
        ConnectionRequestInfo cri = new DummyConnectionRequestInfo();
        DummyManagedConnectionImpl con1 = new DummyManagedConnectionImpl(mcf, cri, subject);
        con1.setBound(true);
        connectionSet.add(con1);

        ManagedConnection mcon = mcf.matchManagedConnections(connectionSet, subject, cri);
        assertEquals(con1, mcon);
    }

    @Test
    public void testMatchConnectionDifferentConnectioRequestInfoNotBound() throws Exception {
        ConnectionRequestInfo cri1 = new DummyConnectionRequestInfo();
        ConnectionRequestInfo cri2 = new DummyConnectionRequestInfo();

        Subject subject = null;
        assertTrue("request info object are differnt", cri1 != cri2);

        Set<AbstractManagedConnectionImpl> connectionSet = new HashSet<>();
        DummyManagedConnectionImpl con1 = new DummyManagedConnectionImpl(mcf, cri1, subject);
        connectionSet.add(con1);

        ManagedConnection mcon = mcf.matchManagedConnections(connectionSet, subject, cri2);
        assertEquals("incorrect connection returned", con1, mcon);
    }

    @Test
    public void testMatchConnectionDifferentConnectioRequestInfoBound() throws Exception {
        ConnectionRequestInfo cri1 = new DummyConnectionRequestInfo();
        ConnectionRequestInfo cri2 = new DummyConnectionRequestInfo();

        Subject subject = null;

        assertTrue("request info object are differnt", cri1 != cri2);

        Set<AbstractManagedConnectionImpl> connectionSet = new HashSet<>();
        DummyManagedConnectionImpl con1 = new DummyManagedConnectionImpl(mcf, cri1, subject);
        con1.setBound(true);
        connectionSet.add(con1);

        ManagedConnection mcon = mcf.matchManagedConnections(connectionSet, subject, cri2);
        assertNull("should not get a match", mcon);
    }

    @Test
    public void testMatchConnectionInvalidatedWithSameConnectioRequestInfo() throws Exception {
        Subject subject = null;
        Set<AbstractManagedConnectionImpl> connectionSet = new HashSet<>();
        ConnectionRequestInfo cri = new DummyConnectionRequestInfo();

        DummyManagedConnectionImpl con1 = new DummyManagedConnectionImpl(mcf, cri, subject);
        con1.setBound(true);
        con1.setCon(connectionSet);
        connectionSet.add(con1);

        ManagedConnection mcon = mcf.matchManagedConnections(connectionSet, subject, cri);
        assertNull("Connection must be null", mcon);
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
        mcf.setLogWriter(writer);
        assertSame(writer, mcf.getLogWriter());

        mcf.close();
        assertFalse(closed.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNullLogWriter() throws Exception {
        mcf.setLogWriter(null);
    }
}

class DummyConnectionRequestInfo implements ConnectionRequestInfo {
}

class DummyManagedConnectionFactoryImpl extends AbstractManagedConnectionFactoryImpl {

    private static final long serialVersionUID = -218445259745278972L;

    DummyManagedConnectionFactoryImpl() {
    }

    DummyManagedConnectionFactoryImpl(Properties p) {
        super(p);
    }

    public Object createConnectionFactory(ConnectionManager connMgr) throws ResourceException {
        return null;
    }

    public Object createConnectionFactory() throws ResourceException {
        return null;
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connReqInfo)
        throws ResourceException {
        return null;
    }

    public void close() throws ResourceAdapterInternalException {
        // do nothing here
    }

    protected void validateReference(AbstractManagedConnectionImpl conn, Subject subject)
        throws ResourceAdapterInternalException {
        boolean valid = true;

        try {
            if (conn.getConnection(null, null) != null) {
                valid = false;
            }
        } catch (ResourceException ignored) {
            // do nothing here
        }

        if (!valid) {
            throw new ResourceAdapterInternalException("invalid");
        }
    }

    public void setResourceAdapter(jakarta.resource.spi.ResourceAdapter ra) {
    }

    public jakarta.resource.spi.ResourceAdapter getResourceAdapter() {
        return null;
    }
}