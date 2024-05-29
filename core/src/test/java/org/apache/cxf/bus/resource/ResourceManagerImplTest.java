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

package org.apache.cxf.bus.resource;

import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class ResourceManagerImplTest {

    private Bus bus;

    private ResourceManagerImpl resourceManager;


    @Before
    public void setup() {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        bus = BusFactory.newInstance().createBus();
        resourceManager = new ResourceManagerImpl(bus);
    }

    @Test
    public void testOnFirstResolve() {
        List<ResourceResolver> beforeFirstResolve = resourceManager.getResourceResolvers();
        resourceManager.onFirstResolve();
        List<ResourceResolver> afterFirstResolve = resourceManager.getResourceResolvers();
        assertEquals("No additional resolvers added.", beforeFirstResolve.size(),
                afterFirstResolve.size());
    }

    @Test
    public void testSetResolvers() {
        List<ResourceResolver> initialResolvers = resourceManager.getResourceResolvers();
        ResourceResolver mockResourceResolver = Mockito.mock(ResourceResolver.class);
        List<ResourceResolver> mockResolvers = List.of(mockResourceResolver);
        resourceManager.setResolvers(mockResolvers);

        List<ResourceResolver> actual = resourceManager.getResourceResolvers();
        assertEquals("Contains our Mock ResourceResolver", 1, actual.size());

        resourceManager.setResolvers(initialResolvers);
        actual = resourceManager.getResourceResolvers();
        assertEquals("Initial resolvers should have been cleared by set call.",
                0, actual.size());
    }

    @Test
    public void testGetRegistrationType() {
        ResourceManagerImpl impl = new ResourceManagerImpl();
        assertEquals("ResourceManagerImpl is of type ResourceManager.class",
                ResourceManager.class, impl.getRegistrationType());
    }
}
