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

package org.apache.cxf.jaxrs.model.wadl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ServerProviderFactoryTest {


    @Before
    public void setUp() {
        ServerProviderFactory.getInstance().clearProviders();
        AbstractResourceInfo.clearAllMaps();
    }


    @Test
    public void testCustomWadlHandler() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        assertEquals(1, pf.getPreMatchContainerRequestFilters().size());
        assertTrue(pf.getPreMatchContainerRequestFilters().get(0).getProvider() instanceof WadlGenerator);

        WadlGenerator wg = new WadlGenerator();
        pf.setUserProviders(Collections.singletonList(wg));
        assertEquals(1, pf.getPreMatchContainerRequestFilters().size());
        assertTrue(pf.getPreMatchContainerRequestFilters().get(0).getProvider() instanceof WadlGenerator);
        assertSame(wg, pf.getPreMatchContainerRequestFilters().get(0).getProvider());
    }

    @Test
    public void testCustomTestHandler() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        assertEquals(1, pf.getPreMatchContainerRequestFilters().size());
        assertTrue(pf.getPreMatchContainerRequestFilters().get(0).getProvider() instanceof WadlGenerator);

        TestHandler th = new TestHandler();
        pf.setUserProviders(Collections.singletonList(th));
        assertEquals(2, pf.getPreMatchContainerRequestFilters().size());
        assertTrue(pf.getPreMatchContainerRequestFilters().get(0).getProvider() instanceof WadlGenerator);
        assertSame(th, pf.getPreMatchContainerRequestFilters().get(1).getProvider());
    }

    @Test
    public void testCustomTestAndWadlHandler() {
        ServerProviderFactory pf = ServerProviderFactory.getInstance();
        assertEquals(1, pf.getPreMatchContainerRequestFilters().size());
        assertTrue(pf.getPreMatchContainerRequestFilters().get(0).getProvider() instanceof WadlGenerator);

        List<Object> providers = new ArrayList<>();
        WadlGenerator wg = new WadlGenerator();
        providers.add(wg);
        TestHandler th = new TestHandler();
        providers.add(th);
        pf.setUserProviders(providers);
        assertEquals(2, pf.getPreMatchContainerRequestFilters().size());
        assertSame(wg, pf.getPreMatchContainerRequestFilters().get(0).getProvider());
        assertSame(th, pf.getPreMatchContainerRequestFilters().get(1).getProvider());
    }

    @PreMatching
    private static final class TestHandler implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) {
            // complete
        }

    }


}