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
package org.apache.cxf.jca.cxf;



import java.io.Serializable;
import java.net.URL;

import javax.naming.Reference;
import javax.xml.namespace.QName;

import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConnectionFactoryImplTest {

    ManagedConnectionFactory mockConnectionFactory;
    ConnectionManager mockConnectionManager;
    CXFConnectionRequestInfo param;
    ConnectionFactoryImpl cf;

    @Before
    public void setUp() throws Exception {
        mockConnectionFactory = mock(ManagedConnectionFactory.class);
        mockConnectionManager = mock(ConnectionManager.class);

        param = new CXFConnectionRequestInfo();
        param.setInterface(Runnable.class);

        cf = new ConnectionFactoryImpl(mockConnectionFactory, mockConnectionManager);
    }

    @Test
    public void testInstanceOfSerializable() throws Exception {
        assertTrue("Instance of Serializable", cf instanceof Serializable);
    }

    @Test
    public void testInstanceOfReferencable() throws Exception {
        assertTrue("Instance of Referenceable", cf instanceof Referenceable);

        assertNull("No ref set", cf.getReference());
        Reference ref = new Reference("some.class");
        cf.setReference(ref);
        assertEquals("Got back what was set", ref, cf.getReference());
    }

    @Test
    public void testGetConnectionReturnsConnectionWithRightManager() throws Exception {
        CXFConnectionRequestInfo reqInfo =
            new CXFConnectionRequestInfo(Runnable.class,
                                            new URL("file:/tmp/foo"),
                                            new QName(""),
                                            new QName(""));

        when(mockConnectionManager.allocateConnection(eq(mockConnectionFactory),
                                                 eq(reqInfo))).thenReturn(null);

        param.setWsdlLocation(new URL("file:/tmp/foo"));
        param.setServiceName(new QName(""));
        param.setPortName(new QName(""));
        Object o = cf.getConnection(param);
        assertNull("Got the result (the passed in ConnectionRequestInfo) from out mock manager",
                   o);
    }

    @Test
    public void testGetConnectionWithNoPortReturnsConnectionWithRightManager() throws Exception {
        CXFConnectionRequestInfo reqInfo =
            new CXFConnectionRequestInfo(Runnable.class,
                                            new URL("file:/tmp/foo"),
                                            new QName(""),
                                            null);

        when(mockConnectionManager.allocateConnection(eq(mockConnectionFactory),
                                                 eq(reqInfo))).thenReturn(null);

        param.setWsdlLocation(new URL("file:/tmp/foo"));
        param.setServiceName(new QName(""));
        Object o = cf.getConnection(param);

        assertNull("Got the result (the passed in ConnectionRequestInfo) from out mock manager",
                   o);


    }

    @Test
    public void testGetConnectionWithNoWsdlLocationReturnsConnectionWithRightManager() throws Exception {
        CXFConnectionRequestInfo reqInfo =
            new CXFConnectionRequestInfo(Runnable.class,
                                            null,
                                            new QName(""),
                                            new QName(""));

        when(mockConnectionManager.allocateConnection(eq(mockConnectionFactory),
                                                 eq(reqInfo))).thenReturn(null);

        param.setServiceName(new QName(""));
        param.setPortName(new QName(""));
        Object o = cf.getConnection(param);

        assertNull("Got the result (the passed in ConnectionRequestInfo) from out mock manager",
                   o);

    }

    @Test
    public void testGetConnectionWithNoWsdlLocationAndNoPortReturnsConnectionWithRightManager()
        throws Exception {

        CXFConnectionRequestInfo reqInfo =
            new CXFConnectionRequestInfo(Runnable.class,
                                            null,
                                            new QName(""),
                                            null);

        when(mockConnectionManager.allocateConnection(eq(mockConnectionFactory),
                                                 eq(reqInfo))).thenReturn(null);

        cf = new ConnectionFactoryImpl(mockConnectionFactory, mockConnectionManager);
        param.setServiceName(new QName(""));
        Object o = cf.getConnection(param);
        assertNull("Got the result (the passed in ConnectionRequestInfo) from out mock manager",
                   o);


    }

    @Test
    public void testGetConnectionWithNonInterface() throws Exception {
        try {
            param.setInterface(Object.class);
            param.setWsdlLocation(new URL("file:/tmp/foo"));
            param.setServiceName(new QName(""));
            param.setPortName(new QName(""));
            cf.getConnection(param);
            fail("Expect exception on using of none interface class");
        } catch (ResourceException re) {
            // expected
        }
    }

    @Test
    public void testGetConnectionWithNoInterface() throws Exception {
        try {
            param.setInterface(null);
            cf.getConnection(param);
            fail("Expect exception of no interface here");
        } catch (ResourceException re) {
            // expected
        }
    }

}
