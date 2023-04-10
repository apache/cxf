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

package org.apache.cxf.bus.managers;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.EndpointResolver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndpointResolverRegistryImplTest {

    private EndpointResolverRegistryImpl registry;
    private EndpointResolver resolver1;
    private EndpointResolver resolver2;
    private EndpointReferenceType logical;
    private EndpointReferenceType physical;
    private EndpointReferenceType fresh;
    private QName serviceName;

    @Before
    public void setUp() {
        registry = new EndpointResolverRegistryImpl();
        resolver1 = mock(EndpointResolver.class);
        resolver2 = mock(EndpointResolver.class);
        logical = mock(EndpointReferenceType.class);
        physical = mock(EndpointReferenceType.class);
        fresh = mock(EndpointReferenceType.class);
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
    public void testRegister() {
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
        registry.register(resolver1);
        registry.register(resolver2);
        when(resolver1.resolve(logical)).thenReturn(physical);

        EndpointReferenceType resolved = registry.resolve(logical);
        assertSame("unexpected physical EPR", physical, resolved);

        resolver1.resolve(logical);
        when(resolver1.resolve(logical)).thenReturn(null);
        when(resolver2.resolve(logical)).thenReturn(physical);

        resolved = registry.resolve(logical);
        assertSame("unexpected physical EPR", physical, resolved);

        when(resolver1.resolve(logical)).thenReturn(null);
        when(resolver2.resolve(logical)).thenReturn(null);

        resolved = registry.resolve(logical);
        assertNull("unexpected physical EPR", resolved);
    }

    @Test
    public void testRenew() {
        registry.register(resolver1);
        registry.register(resolver2);
        when(resolver1.renew(logical, physical)).thenReturn(fresh);

        EndpointReferenceType renewed = registry.renew(logical, physical);
        assertSame("unexpected physical EPR", fresh, renewed);

        when(resolver1.renew(logical, physical)).thenReturn(null);
        when(resolver2.renew(logical, physical)).thenReturn(physical);

        renewed = registry.renew(logical, physical);
        assertSame("unexpected physical EPR", physical, renewed);

        when(resolver1.renew(logical, physical)).thenReturn(null);
        when(resolver2.renew(logical, physical)).thenReturn(null);

        renewed = registry.renew(logical, physical);
        assertNull("unexpected physical EPR", renewed);
    }

    @Test
    public void testMintFromServiceName() {
        registry.register(resolver1);
        registry.register(resolver2);
        when(resolver1.mint(serviceName)).thenReturn(logical);

        EndpointReferenceType minted = registry.mint(serviceName);
        assertSame("unexpected minted EPR", logical, minted);

        when(resolver1.mint(serviceName)).thenReturn(null);
        when(resolver2.mint(serviceName)).thenReturn(logical);

        minted = registry.mint(serviceName);
        assertSame("unexpected minted EPR", logical, minted);

        when(resolver1.mint(serviceName)).thenReturn(null);
        when(resolver2.mint(serviceName)).thenReturn(null);

        minted = registry.mint(serviceName);
        assertNull("unexpected minted EPR", minted);
    }

    @Test
    public void testMintFromPhysical() {
        registry.register(resolver1);
        registry.register(resolver2);
        when(resolver1.mint(physical)).thenReturn(logical);

        EndpointReferenceType minted = registry.mint(physical);
        assertSame("unexpected minted EPR", logical, minted);

        when(resolver1.mint(physical)).thenReturn(null);
        when(resolver2.mint(physical)).thenReturn(logical);

        minted = registry.mint(physical);
        assertSame("unexpected minted EPR", logical, minted);

        when(resolver1.mint(physical)).thenReturn(null);
        when(resolver2.mint(physical)).thenReturn(null);

        minted = registry.mint(physical);
        assertNull("unexpected minted EPR", minted);
    }
}