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

package org.apache.cxf.endpoint;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EndpointResolverRegistryImplTest extends Assert {

    private EndpointResolverRegistryImpl registry;
    private EndpointResolver resolver1;
    private EndpointResolver resolver2;
    private EndpointReferenceType logical;
    private EndpointReferenceType physical;
    private EndpointReferenceType fresh;
    private IMocksControl control;
    private QName serviceName;

    @Before
    public void setUp() {
        registry = new EndpointResolverRegistryImpl();
        control = EasyMock.createNiceControl();
        resolver1 = control.createMock(EndpointResolver.class);
        resolver2 = control.createMock(EndpointResolver.class);
        logical = control.createMock(EndpointReferenceType.class);
        physical = control.createMock(EndpointReferenceType.class);
        fresh = control.createMock(EndpointReferenceType.class);
        serviceName = new QName("namespace", "local");
    }

    @After
    public void tearDown() {
        resolver1 = null;
        resolver2 = null;
        logical = null;
        physical = null;
        serviceName = null;
    }

    @Test
    public void testInit() {
        assertNull("unexpected resolvers list", registry.getResolvers());
        Bus bus = control.createMock(Bus.class);
        registry.setBus(bus);
        bus.setExtension(registry, EndpointResolverRegistry.class);
        control.replay();
        
        registry.init();
        
        assertNotNull("expected resolvers list", registry.getResolvers());
        control.verify();
    }
    
    @Test
    public void testRegister() {
        registry.init();
        assertEquals("unexpected resolver count",
                     0,
                     registry.getResolvers().size());
        
        registry.register(resolver1);
        
        assertEquals("unexpected resolver count",
                     1,
                     registry.getResolvers().size());
        assertTrue("expected resolver to be registered",
                   registry.getResolvers().contains(resolver1));
        
        registry.unregister(resolver1);
        
        assertEquals("unexpected resolver count",
                     0,
                     registry.getResolvers().size());
        assertFalse("expected resolver to be registered",
                    registry.getResolvers().contains(resolver1));
        
        registry.register(resolver2);
        registry.register(resolver1);
        
        assertEquals("unexpected resolver count",
                     2,
                     registry.getResolvers().size());
        assertTrue("expected resolver to be registered",
                   registry.getResolvers().contains(resolver1));
        assertTrue("expected resolver to be registered",
                   registry.getResolvers().contains(resolver2));
        
        registry.unregister(resolver2);
        
        assertEquals("unexpected resolver count",
                     1,
                     registry.getResolvers().size());
        assertTrue("expected resolver to be registered",
                   registry.getResolvers().contains(resolver1));
        assertFalse("expected resolver to be registered",
                    registry.getResolvers().contains(resolver2));
    }
    
    @Test
    public void testResolve() {
        registry.init();
        registry.register(resolver1);
        registry.register(resolver2);
        resolver1.resolve(logical);
        EasyMock.expectLastCall().andReturn(physical);
        control.replay();
     
        EndpointReferenceType resolved = registry.resolve(logical);
        
        control.verify();
        assertSame("unexpected physical EPR", physical, resolved);
        
        control.reset();
        resolver1.resolve(logical);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.resolve(logical);
        EasyMock.expectLastCall().andReturn(physical);
        control.replay();
        
        resolved = registry.resolve(logical);
        
        control.verify();
        assertSame("unexpected physical EPR", physical, resolved);

        control.reset();
        resolver1.resolve(logical);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.resolve(logical);
        EasyMock.expectLastCall().andReturn(null);
        control.replay();

        resolved = registry.resolve(logical);

        control.verify();
        assertNull("unexpected physical EPR", resolved);
    }
    
    @Test
    public void testRenew() {
        registry.init();
        registry.register(resolver1);
        registry.register(resolver2);
        resolver1.renew(logical, physical);
        EasyMock.expectLastCall().andReturn(fresh);
        control.replay();
        
        EndpointReferenceType renewed = registry.renew(logical, physical);
        
        control.verify();
        assertSame("unexpected physical EPR", fresh, renewed);
        
        control.reset();
        resolver1.renew(logical, physical);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.renew(logical, physical);
        EasyMock.expectLastCall().andReturn(physical);
        control.replay();
        
        renewed = registry.renew(logical, physical);
        
        control.verify();
        assertSame("unexpected physical EPR", physical, renewed);

        control.reset();
        resolver1.renew(logical, physical);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.renew(logical, physical);
        EasyMock.expectLastCall().andReturn(null);
        control.replay();

        renewed = registry.renew(logical, physical);

        control.verify();
        assertNull("unexpected physical EPR", renewed);
    }
    
    @Test
    public void testMintFromServiceName() {
        registry.init();
        registry.register(resolver1);
        registry.register(resolver2);
        resolver1.mint(serviceName);
        EasyMock.expectLastCall().andReturn(logical);
        control.replay();
     
        EndpointReferenceType minted = registry.mint(serviceName);
        
        control.verify();
        assertSame("unexpected minted EPR", logical, minted);
        
        control.reset();
        resolver1.mint(serviceName);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.mint(serviceName);
        EasyMock.expectLastCall().andReturn(logical);
        control.replay();
        
        minted = registry.mint(serviceName);
        
        control.verify();
        assertSame("unexpected minted EPR", logical, minted);

        control.reset();
        resolver1.mint(serviceName);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.mint(serviceName);
        EasyMock.expectLastCall().andReturn(null);
        control.replay();

        minted = registry.mint(serviceName);

        control.verify();
        assertNull("unexpected minted EPR", minted);
    }
    
    @Test
    public void testMintFromPhysical() {
        registry.init();
        registry.register(resolver1);
        registry.register(resolver2);
        resolver1.mint(physical);
        EasyMock.expectLastCall().andReturn(logical);
        control.replay();
     
        EndpointReferenceType minted = registry.mint(physical);
        
        control.verify();
        assertSame("unexpected minted EPR", logical, minted);
        
        control.reset();
        resolver1.mint(physical);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.mint(physical);
        EasyMock.expectLastCall().andReturn(logical);
        control.replay();
        
        minted = registry.mint(physical);
        
        control.verify();
        assertSame("unexpected minted EPR", logical, minted);

        control.reset();
        resolver1.mint(physical);
        EasyMock.expectLastCall().andReturn(null);
        resolver2.mint(physical);
        EasyMock.expectLastCall().andReturn(null);
        control.replay();

        minted = registry.mint(physical);

        control.verify();
        assertNull("unexpected minted EPR", minted);
    }
}
