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

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.ServiceContractResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceContractResolverRegistryImplTest {
    private ServiceContractResolverRegistryImpl registry;
    private ServiceContractResolver resolver1;
    private ServiceContractResolver resolver2;
    private URI uri1;
    private URI uri2;
    private QName serviceName;

    @Before
    public void setUp() throws URISyntaxException {
        registry = new ServiceContractResolverRegistryImpl();
        resolver1 = mock(ServiceContractResolver.class);
        resolver2 = mock(ServiceContractResolver.class);
        uri1 = new URI("http://mock");
        uri2 = new URI("file:///foo/bar");

        serviceName = new QName("namespace", "local");
    }
    @After
    public void tearDown() {
        resolver1 = null;
        resolver2 = null;
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
    public void testGetContactLocation() {
        registry.register(resolver1);
        registry.register(resolver2);
        when(resolver1.getContractLocation(serviceName)).thenReturn(uri1);

        URI resolved = registry.getContractLocation(serviceName);
        assertSame("unexpected physical EPR", uri1, resolved);

        when(resolver1.getContractLocation(serviceName)).thenReturn(null);
        when(resolver2.getContractLocation(serviceName)).thenReturn(uri2);

        resolved = registry.getContractLocation(serviceName);
        assertSame("unexpected physical EPR", uri2, resolved);
        assertNotSame("unexpected physical EPR", uri1, resolved);

        when(resolver1.getContractLocation(serviceName)).thenReturn(null);
        when(resolver2.getContractLocation(serviceName)).thenReturn(null);

        resolved = registry.getContractLocation(serviceName);
        assertNull("unexpected physical EPR", resolved);
    }

}