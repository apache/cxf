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
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;
import javax.xml.namespace.QName;

import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConnectionFactoryImplTest extends Assert {
    
    ManagedConnectionFactory mockConnectionFactory;
    ConnectionManager mockConnectionManager;
    CXFConnectionRequestInfo param;
    ConnectionFactoryImpl cf;

    @Before
    public void setUp() throws Exception {
        mockConnectionFactory = EasyMock.createMock(ManagedConnectionFactory.class);
        mockConnectionManager = EasyMock.createMock(ConnectionManager.class);
        
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
        Reference ref = EasyMock.createMock(Reference.class);
        cf.setReference(ref);
        assertEquals("Got back what was set", ref, cf.getReference());
    }

    @Test
    public void testGetConnectionReturnsConnectionWithRightManager() throws Exception {
        EasyMock.reset(mockConnectionManager);
        
        CXFConnectionRequestInfo reqInfo = 
            new CXFConnectionRequestInfo(Runnable.class, 
                                            new URL("file:/tmp/foo"), 
                                            new QName(""), 
                                            new QName(""));
        
        mockConnectionManager.allocateConnection(EasyMock.eq(mockConnectionFactory),
                                                 EasyMock.eq(reqInfo));
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(mockConnectionManager);
         
        param.setWsdlLocation(new URL("file:/tmp/foo"));
        param.setServiceName(new QName(""));
        param.setPortName(new QName(""));
        Object o = cf.getConnection(param);
        assertNull("Got the result (the passed in ConnectionRequestInfo) from out mock manager",
                   o);
        EasyMock.verify(mockConnectionManager); 
    }

    @Test
    public void testGetConnectionWithNoPortReturnsConnectionWithRightManager() throws Exception {
        
        EasyMock.reset(mockConnectionManager);
        
        CXFConnectionRequestInfo reqInfo = 
            new CXFConnectionRequestInfo(Runnable.class, 
                                            new URL("file:/tmp/foo"), 
                                            new QName(""), 
                                            null);
        
        mockConnectionManager.allocateConnection(EasyMock.eq(mockConnectionFactory),
                                                 EasyMock.eq(reqInfo));
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(mockConnectionManager);
        
        param.setWsdlLocation(new URL("file:/tmp/foo"));
        param.setServiceName(new QName(""));
        Object o = cf.getConnection(param);
        
        EasyMock.verify(mockConnectionManager);
        
        assertNull("Got the result (the passed in ConnectionRequestInfo) from out mock manager",
                   o);
        
        
    }

    @Test
    public void testGetConnectionWithNoWsdlLocationReturnsConnectionWithRightManager() throws Exception {
        
        EasyMock.reset(mockConnectionManager);
        
        CXFConnectionRequestInfo reqInfo = 
            new CXFConnectionRequestInfo(Runnable.class, 
                                            null, 
                                            new QName(""), 
                                            new QName(""));
        
        mockConnectionManager.allocateConnection(EasyMock.eq(mockConnectionFactory),
                                                 EasyMock.eq(reqInfo));
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(mockConnectionManager);

        param.setServiceName(new QName(""));
        param.setPortName(new QName(""));
        Object o = cf.getConnection(param);
        EasyMock.verify(mockConnectionManager);
        
        assertNull("Got the result (the passed in ConnectionRequestInfo) from out mock manager",
                   o);
        
    }

    @Test
    public void testGetConnectionWithNoWsdlLocationAndNoPortReturnsConnectionWithRightManager()
        throws Exception {
        EasyMock.reset(mockConnectionManager);
        
        CXFConnectionRequestInfo reqInfo = 
            new CXFConnectionRequestInfo(Runnable.class, 
                                            null, 
                                            new QName(""), 
                                            null);
        
        mockConnectionManager.allocateConnection(EasyMock.eq(mockConnectionFactory),
                                                 EasyMock.eq(reqInfo));
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(mockConnectionManager);
        
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
            assertTrue(true);
        }
    }

    @Test
    public void testGetConnectionWithNoInterface() throws Exception {
        try {
            param.setInterface(null);
            cf.getConnection(param);
            fail("Expect exception of no interface here");
        } catch (ResourceException re) {
            assertTrue(true);
        }
    }
    
}
